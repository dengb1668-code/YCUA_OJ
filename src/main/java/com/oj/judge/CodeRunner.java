package com.oj.judge;

import com.oj.enums.Language;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * 代码编译/运行器: 封装"临时目录 -> 写源码 -> 编译 -> 运行"的通用流程,
 * 供真实判题(JudgeService)与自定义测试(CustomTestService)共用。
 * <p>
 * 安全警告: 当前直接在宿主机执行用户代码, 仅适合本地开发。
 * 部署上线前必须替换为沙箱(如 nsjail/isolate)或独立判题机。
 */
@Slf4j
@Component
public class CodeRunner {

    /** 运行输出最大采集量, 超出截断 */
    private static final int MAX_OUTPUT = 256 * 1024;
    /** 编译错误最大采集量(编译器报错可能非常长, 只保留开头部分) */
    private static final int MAX_COMPILE_ERROR = 4 * 1024;
    /** 本机 g++ 的 C++17 是否存在 filesystem 编译 bug(首次发现后缓存, 之后直接 C++14, 省一半编译时间) */
    private static volatile boolean cpp17Broken = false;
    /** C++ 预编译头目录: 预编译 bits/stdc++.h, 把每次 10+ 秒的编译降到 1-2 秒 */
    private static final Path PCH_DIR = Path.of(System.getProperty("user.home"), ".oj", "gch");
    private static volatile boolean pchAttempted = false;
    private static volatile boolean pchReady = false;

    /** 一次运行的结果 */
    public record RunResult(String stdout, String stderr, int exitCode, boolean timedOut, long elapsedMillis) {
        public static RunResult timeout(long elapsedMillis) {
            return new RunResult("", "", -1, true, elapsedMillis);
        }
    }

    /** 检查本机是否有所选语言的工具链, 无则返回提示信息 */
    public String checkToolchain(Language language) {
        return switch (language) {
            case CPP, C -> hasTool("g++") || hasTool("gcc") ? null : "未检测到 g++/gcc 编译器, 请安装 MinGW-w64";
            case JAVA -> hasTool("javac") ? null : "未检测到 JDK";
            case PYTHON3 -> hasTool("python3") || hasTool("python") ? null : "未检测到 Python";
            case JAVASCRIPT -> hasTool("node") ? null : "未检测到 Node.js";
            case GO -> hasTool("go") ? null : "未检测到 Go 工具链";
        };
    }

    public Path createTempDir() throws IOException {
        return Files.createTempDirectory("oj-run-");
    }

    public String sourceFileName(Language language) {
        return switch (language) {
            case CPP -> "main.cpp";
            case C -> "main.c";
            case JAVA -> "Main.java";
            case PYTHON3 -> "main.py";
            case GO -> "main.go";
            case JAVASCRIPT -> "main.js";
        };
    }

    public void writeSource(Path dir, Language language, String code) throws IOException {
        Files.writeString(dir.resolve(sourceFileName(language)), code, StandardCharsets.UTF_8);
    }

    /** 编译, 全部尝试失败时返回错误信息; 无需编译/成功返回 null */
    public String compile(Path dir, Language language) throws IOException, InterruptedException {
        if (language == Language.CPP) {
            ensureCppPch();
        }

        String lastError = null;
        for (String[] command : buildAttempts(language)) {
            CompileResult result = runCompile(command, dir);
            if (result.exitCode() == 0) {
                return null;
            }
            lastError = result.output();
            // PCH 与当前头文件不匹配时报错, 去掉 -I 用普通方式重试一次
            if (language == Language.CPP && result.output() != null && result.output().contains(".gch")) {
                CompileResult plain = runCompile(removePchInclude(command), dir);
                if (plain.exitCode() == 0) {
                    return null;
                }
                lastError = plain.output();
            }
        }

        // 首次检测到 filesystem 头文件报错(即本机工具链 bug, 而非用户代码错误), 缓存结果避免每次双倍编译
        if (language == Language.CPP && !cpp17Broken && lastError != null && lastError.contains("fs_path.h")) {
            cpp17Broken = true;
            log.info("检测到本机 g++ 的 C++17 filesystem 编译 bug, 后续编译将直接使用 C++14");
        }
        return lastError;
    }

    /** 按语言和已知工具链缺陷生成编译命令列表(依次尝试) */
    private List<String[]> buildAttempts(Language language) {
        String src = sourceFileName(language);
        if (language == Language.CPP) {
            if (cpp17Broken) {
                // 已知 C++17 损坏, 直接 C++14(预编译头可用时带上 -I)
                return Collections.singletonList(cppCommand("-std=c++14", src, pchReady));
            }
            return List.of(
                    cppCommand("-std=c++17", src, false),
                    cppCommand("-std=c++14", src, pchReady));
        }
        return switch (language) {
            case C -> Collections.singletonList(new String[]{"gcc", "-O2", src, "-o", "main.exe"});
            case JAVA -> Collections.singletonList(new String[]{"javac", src});
            case GO -> Collections.singletonList(new String[]{"go", "build", "-o", "main.exe", src});
            default -> List.of(); // 解释型语言无需编译
        };
    }

    private String[] cppCommand(String std, String src, boolean withPch) {
        if (withPch) {
            return new String[]{"g++", "-O2", std, "-I", PCH_DIR.toString(), src, "-o", "main.exe"};
        }
        return new String[]{"g++", "-O2", std, src, "-o", "main.exe"};
    }

    private String[] removePchInclude(String[] command) {
        return Arrays.stream(command)
                .filter(a -> !a.equals("-I") && !a.equals(PCH_DIR.toString()))
                .toArray(String[]::new);
    }

    /** 执行一次编译 */
    private CompileResult runCompile(String[] command, Path dir) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(dir.toFile());
        pb.redirectErrorStream(true);
        Process process = pb.start();
        String output = readLimited(process.getInputStream(), MAX_COMPILE_ERROR);
        if (!process.waitFor(30, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            return new CompileResult(-1, "编译超时");
        }
        return new CompileResult(process.exitValue(), output);
    }

    private record CompileResult(int exitCode, String output) {
    }

    /**
     * 预编译 bits/stdc++.h(每个 JVM 只尝试一次):
     * 本机 g++ 每次全量编译标准库要 10+ 秒, 预编译后降到 1-2 秒
     */
    private synchronized void ensureCppPch() {
        if (pchAttempted) {
            return;
        }
        pchAttempted = true;
        try {
            Path header = findStdcHeader();
            if (header == null) {
                log.warn("未找到 bits/stdc++.h, 跳过预编译头构建");
                return;
            }
            Path gchDir = PCH_DIR.resolve("bits");
            Files.createDirectories(gchDir);
            ProcessBuilder pb = new ProcessBuilder("g++", "-O2", "-std=c++14",
                    "-x", "c++-header", header.toString(), "-o", gchDir.resolve("stdc++.h.gch").toString());
            pb.redirectErrorStream(true);
            Process process = pb.start();
            String output = readLimited(process.getInputStream(), MAX_COMPILE_ERROR);
            if (!process.waitFor(180, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                log.warn("预编译头构建超时");
                return;
            }
            if (process.exitValue() == 0) {
                pchReady = true;
                log.info("C++ 预编译头构建完成: {}", gchDir.resolve("stdc++.h.gch"));
            } else {
                log.warn("预编译头构建失败, 回退普通编译: {}", output);
            }
        } catch (Exception e) {
            log.warn("预编译头构建异常, 回退普通编译", e);
        }
    }

    /** 通过 g++ -v 输出解析 include 搜索路径, 定位 bits/stdc++.h */
    private Path findStdcHeader() throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("g++", "-E", "-x", "c++", "-", "-v");
        Process process = pb.start();
        process.getOutputStream().close();
        String stderr = readLimited(process.getErrorStream(), 64 * 1024);
        process.waitFor(10, TimeUnit.SECONDS);

        boolean inSearchList = false;
        for (String line : stderr.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("#include")) {
                inSearchList = trimmed.contains("search starts here");
                continue;
            }
            if (trimmed.equals("End of search list.")) {
                inSearchList = false;
                continue;
            }
            if (inSearchList && !trimmed.isEmpty()) {
                Path candidate = Path.of(trimmed).resolve("bits/stdc++.h");
                if (Files.exists(candidate)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    /** 运行程序, 输入经 stdin 注入, 超过 timeoutMillis 强制终止 */
    public RunResult run(Path dir, Language language, String input, long timeoutMillis) throws IOException, InterruptedException {
        ProcessBuilder pb = switch (language) {
            case CPP, C, GO -> new ProcessBuilder(dir.resolve("main.exe").toString());
            case JAVA -> new ProcessBuilder("java", "-cp", dir.toString(), "Main");
            case PYTHON3 -> new ProcessBuilder(pythonCommand(), "main.py");
            case JAVASCRIPT -> new ProcessBuilder("node", "main.js");
        };
        pb.directory(dir.toFile());

        long start = System.nanoTime();
        Process process = pb.start();

        // 输出可能较大, 用两个线程读取避免管道阻塞
        final String[] stdout = {""};
        final String[] stderr = {""};
        Thread outThread = new Thread(() -> stdout[0] = readLimited(process.getInputStream(), MAX_OUTPUT));
        Thread errThread = new Thread(() -> stderr[0] = readLimited(process.getErrorStream(), MAX_OUTPUT));
        outThread.start();
        errThread.start();

        try (OutputStream stdin = process.getOutputStream()) {
            stdin.write(input == null ? new byte[0] : input.getBytes(StandardCharsets.UTF_8));
        }

        boolean finished = process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS);
        long elapsed = (System.nanoTime() - start) / 1_000_000;
        if (!finished) {
            // 必须先杀子进程树再杀主进程: Linux 上只杀主进程会留下孤儿进程(fork 炸弹杀不干净)
            process.descendants().forEach(ProcessHandle::destroyForcibly);
            process.destroyForcibly();
            outThread.join(1000);
            errThread.join(1000);
            return RunResult.timeout(elapsed);
        }
        outThread.join();
        errThread.join();
        return new RunResult(stdout[0], stderr[0], process.exitValue(), false, elapsed);
    }

    /** 递归删除临时目录, 失败忽略 */
    public void deleteQuietly(Path dir) {
        if (dir == null) {
            return;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                }
            });
        } catch (IOException ignored) {
        }
    }

    /** Linux 上默认只有 python3(Ubuntu/Debian 无 python 命令), 探测可用命令, Windows 回退 python */
    private String pythonCommand() {
        return hasTool("python3") ? "python3" : "python";
    }

    private boolean hasTool(String command) {
        try {
            Process p = new ProcessBuilder(command, "--version").start();
            p.getInputStream().close();
            p.getErrorStream().close();
            return p.waitFor(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 读取输出直到 EOF(内存中只保留前 maxBytes):
     * 必须持续读到底, 否则子进程会因管道缓冲区写满而阻塞, 拖到 waitFor 超时
     */
    private String readLimited(InputStream in, int maxBytes) {
        try (ByteArrayOutputStream buf = new ByteArrayOutputStream(Math.min(maxBytes, 8192))) {
            byte[] chunk = new byte[8192];
            int total = 0;
            int n;
            while ((n = in.read(chunk)) != -1) {
                total += n;
                if (buf.size() < maxBytes) {
                    int keep = Math.min(n, maxBytes - buf.size());
                    buf.write(chunk, 0, keep);
                }
            }
            String s = buf.toString(StandardCharsets.UTF_8);
            return total > maxBytes ? s + "\n...(输出过长已截断)" : s;
        } catch (IOException e) {
            return "";
        }
    }
}
