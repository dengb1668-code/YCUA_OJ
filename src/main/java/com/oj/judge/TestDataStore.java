package com.oj.judge;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.yaml.snakeyaml.Yaml;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 测试点文件存储: 每个题目的测试点存放于
 * {oj.testdata.dir}/{problemId}/1.in、1.out、2.in、2.out ...
 * 每个测试点的计分配置(分值/独立时限/独立内存)存放于同目录 meta.json。
 * <p>
 * 洛谷式约定:
 *  - 满分默认 100, 未指定分值时均分(余数给最后一个测试点);
 *  - 每点 timeLimit(ms)/memoryLimit(MB) 可为 null, 判题时回退用题目默认值;
 *  - zip 上传要求文件在根目录、文件名恰一段数字、.in/.out 成对, 支持洛谷 config.yml。
 * <p>
 * 供三处使用: 判题(JudgeService 一次性读入内存)、Web 端管理 API(TestDataService)、题目详情(testCaseCount)。
 * 注意 Web 与评测机进程共享同一目录, 判题侧把测试点全部读入内存后再跑, 不与 Web 的写操作竞争;
 * 写侧对 Windows 文件锁做小次数重试; zip 替换采用 tmp 目录 + 原子换名, 校验失败不破坏旧数据。
 */
@Slf4j
@Component
public class TestDataStore {

    /** 单个题目测试点数量上限 */
    public static final int MAX_CASES = 100;
    /** 单个测试点文件(解压后)上限 */
    private static final int MAX_ENTRY_BYTES = 2 * 1024 * 1024;
    /** zip 内全部测试点解压后总上限 */
    private static final int MAX_TOTAL_BYTES = 32 * 1024 * 1024;
    /** zip 条目数上限(防 zip 炸弹) */
    private static final int MAX_ZIP_ENTRIES = 500;
    /** 洛谷命名规则: 文件名恰好一段连续数字 + .in/.out */
    private static final Pattern LUOGU_NAME = Pattern.compile("^[^0-9]*(\\d+)[^0-9]*\\.(in|out)$");
    private static final Pattern INDEX_PATTERN = Pattern.compile("(\\d+)");

    private final Path root;
    private final ObjectMapper objectMapper;

    public TestDataStore(@Value("${oj.testdata.dir:./oj-testdata}") String dir, ObjectMapper objectMapper) {
        this.root = Path.of(dir).toAbsolutePath().normalize();
        this.objectMapper = objectMapper;
    }

    /** 测试点计分配置 */
    public record PointMeta(int index, int score, Integer timeLimit, Integer memoryLimit) {
    }

    /** 单个测试点(判题用): 序号 + 输入 + 期望输出 + 计分配置 */
    public record CaseData(int index, String input, String output, int score, Integer timeLimit, Integer memoryLimit) {
    }

    /** 测试点元信息(管理页列表用) */
    public record CaseInfo(int index, long inputSize, long outputSize, int score, Integer timeLimit, Integer memoryLimit) {
    }

    /** 题目测试点目录 */
    private Path problemDir(Long problemId) {
        return root.resolve(String.valueOf(problemId));
    }

    private Path inputFile(Long problemId, int index) {
        return problemDir(problemId).resolve(index + ".in");
    }

    private Path outputFile(Long problemId, int index) {
        return problemDir(problemId).resolve(index + ".out");
    }

    private Path metaFile(Long problemId) {
        return problemDir(problemId).resolve("meta.json");
    }

    /** 列出 .in/.out 成对存在的测试点序号(升序); 目录不存在返回空列表 */
    public List<Integer> listIndexes(Long problemId) throws IOException {
        Path dir = problemDir(problemId);
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        Set<Integer> inIdx = new HashSet<>();
        Set<Integer> outIdx = new HashSet<>();
        List<Path> files;
        try (Stream<Path> stream = Files.list(dir)) {
            files = stream.toList();
        }
        for (Path p : files) {
            String name = p.getFileName().toString().toLowerCase(Locale.ROOT);
            Integer idx = indexFromName(name);
            if (idx == null) {
                continue;
            }
            if (name.endsWith(".in")) {
                inIdx.add(idx);
            } else if (name.endsWith(".out")) {
                outIdx.add(idx);
            }
        }
        inIdx.retainAll(outIdx);
        return inIdx.stream().sorted().toList();
    }

    /** 读取全部测试点到内存(判题用), 目录不存在返回空列表; IOException 重试一次 */
    public List<CaseData> loadAll(Long problemId) throws IOException {
        List<Integer> indexes = listIndexes(problemId);
        if (indexes.isEmpty()) {
            return List.of();
        }
        try {
            return readCases(problemId, indexes);
        } catch (IOException e) {
            log.warn("读取测试点失败, 重试一次: problemId={}", problemId, e);
            return readCases(problemId, indexes);
        }
    }

    /** 读取单个测试点内容(管理页编辑用) */
    public CaseData load(Long problemId, int index) throws IOException {
        Path in = inputFile(problemId, index);
        Path out = outputFile(problemId, index);
        if (!Files.exists(in) || !Files.exists(out)) {
            throw new IllegalArgumentException("测试点不存在: " + index);
        }
        PointMeta meta = metaFor(problemId, index);
        return new CaseData(index, read(in), read(out), meta.score(), meta.timeLimit(), meta.memoryLimit());
    }

    /** 列出测试点元信息(管理页列表用) */
    public List<CaseInfo> listInfo(Long problemId) throws IOException {
        List<Integer> indexes = listIndexes(problemId);
        List<CaseInfo> result = new ArrayList<>(indexes.size());
        for (int idx : indexes) {
            PointMeta meta = metaFor(problemId, idx);
            result.add(new CaseInfo(idx, Files.size(inputFile(problemId, idx)), Files.size(outputFile(problemId, idx)),
                    meta.score(), meta.timeLimit(), meta.memoryLimit()));
        }
        return result;
    }

    /** 全部测试点分值总和(满分) */
    public int totalScore(Long problemId) throws IOException {
        List<Integer> indexes = listIndexes(problemId);
        int total = 0;
        for (int idx : indexes) {
            total += metaFor(problemId, idx).score();
        }
        return total;
    }

    private List<CaseData> readCases(Long problemId, List<Integer> indexes) throws IOException {
        List<CaseData> result = new ArrayList<>(indexes.size());
        for (int idx : indexes) {
            PointMeta meta = metaFor(problemId, idx);
            result.add(new CaseData(idx, read(inputFile(problemId, idx)), read(outputFile(problemId, idx)),
                    meta.score(), meta.timeLimit(), meta.memoryLimit()));
        }
        return result;
    }

    // ---------------- meta.json ----------------

    /** 读取某点的计分配置; meta.json 缺失或没有该点时按均分默认值 */
    private PointMeta metaFor(Long problemId, int index) throws IOException {
        List<PointMeta> metas = readMeta(problemId);
        return metas.stream().filter(m -> m.index() == index)
                .findFirst().orElseGet(() -> defaultMeta(List.of(index)).get(0));
    }

    /** 读取 meta.json 并与现有测试点对齐(文件被手动增删时兜底); 缺失时返回均分默认值 */
    private List<PointMeta> readMeta(Long problemId) throws IOException {
        List<Integer> indexes = listIndexes(problemId);
        if (indexes.isEmpty()) {
            return List.of();
        }
        Path file = metaFile(problemId);
        List<PointMeta> metas = null;
        if (Files.exists(file)) {
            metas = objectMapper.readValue(Files.readString(file, StandardCharsets.UTF_8),
                    new TypeReference<List<PointMeta>>() {
                    });
        }
        if (metas == null || metas.isEmpty()) {
            return defaultMeta(indexes);
        }
        // 与现有文件对齐: 只保留仍存在的序号, 缺失的序号补默认均分值
        Set<Integer> existing = new HashSet<>(indexes);
        List<PointMeta> aligned = metas.stream()
                .filter(m -> existing.contains(m.index()))
                .map(m -> m.score() > 0 ? m : new PointMeta(m.index(), 1, m.timeLimit(), m.memoryLimit()))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        for (int idx : indexes) {
            if (aligned.stream().noneMatch(m -> m.index() == idx)) {
                aligned.add(defaultMeta(List.of(idx)).get(0));
            }
        }
        aligned.sort(Comparator.comparingInt(PointMeta::index));
        return aligned;
    }

    /** 满分 100 均分: 余数给最后一个测试点 */
    private List<PointMeta> defaultMeta(List<Integer> indexes) {
        int n = indexes.size();
        int base = n == 0 ? 0 : 100 / n;
        int rem = n == 0 ? 0 : 100 % n;
        List<PointMeta> result = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            result.add(new PointMeta(indexes.get(i), base + (i == n - 1 ? rem : 0), null, null));
        }
        return result;
    }

    private void writeMeta(Long problemId, List<PointMeta> metas) throws IOException {
        Files.createDirectories(problemDir(problemId));
        Files.writeString(metaFile(problemId), objectMapper.writeValueAsString(metas), StandardCharsets.UTF_8);
    }

    // ---------------- 增删改 ----------------

    /**
     * 追加测试点, 返回新序号。
     * score 为 null 时全部测试点重新均分 100(保持满分恒 100 的洛谷体验);
     * timeLimit/memoryLimit 为 null 表示用题目默认。
     */
    public int addCase(Long problemId, String input, String output, Integer score, Integer timeLimit, Integer memoryLimit)
            throws IOException {
        List<Integer> indexes = listIndexes(problemId);
        int next = indexes.stream().max(Integer::compareTo).orElse(0) + 1;
        // 先读旧配置(此时新测试点文件还未写入, readMeta 不会为其生成默认条目)
        List<PointMeta> oldMetas = readMeta(problemId);
        write(inputFile(problemId, next), input);
        write(outputFile(problemId, next), output);

        List<PointMeta> metas;
        if (score == null) {
            // 未指定分值: 全部点(含新点)重新均分 100, 保留各点已有的独立时限/内存
            List<Integer> all = new ArrayList<>(indexes);
            all.add(next);
            metas = new ArrayList<>();
            for (PointMeta fresh : defaultMeta(all)) {
                PointMeta old = oldMetas.stream().filter(m -> m.index() == fresh.index()).findFirst().orElse(null);
                if (fresh.index() == next) {
                    // 新点用传入的独立时限/内存
                    metas.add(new PointMeta(next, fresh.score(), timeLimit, memoryLimit));
                } else {
                    metas.add(new PointMeta(fresh.index(), fresh.score(),
                            old != null ? old.timeLimit() : null,
                            old != null ? old.memoryLimit() : null));
                }
            }
        } else {
            metas = new ArrayList<>(oldMetas);
            metas.add(new PointMeta(next, score, timeLimit, memoryLimit));
        }
        writeMeta(problemId, metas);
        return next;
    }

    /** 覆盖更新某个测试点的数据(不动配置) */
    public void updateCase(Long problemId, int index, String input, String output) throws IOException {
        requireIndex(problemId, index);
        write(inputFile(problemId, index), input);
        write(outputFile(problemId, index), output);
    }

    /**
     * 更新某个测试点的计分配置:
     * score 为 null 保留原分值; timeLimit/memoryLimit 为 null 表示清除(用题目默认)
     */
    public void updateConfig(Long problemId, int index, Integer score, Integer timeLimit, Integer memoryLimit)
            throws IOException {
        requireIndex(problemId, index);
        List<PointMeta> metas = readMeta(problemId);
        for (int i = 0; i < metas.size(); i++) {
            PointMeta m = metas.get(i);
            if (m.index() == index) {
                metas.set(i, new PointMeta(index,
                        score != null ? score : m.score(),
                        timeLimit,
                        memoryLimit));
                writeMeta(problemId, metas);
                return;
            }
        }
        metas.add(new PointMeta(index, score != null ? score : 1, timeLimit, memoryLimit));
        writeMeta(problemId, metas);
    }

    /** 删除某个测试点: 文件与配置一起重编号前移 */
    public void deleteCase(Long problemId, int index) throws IOException {
        requireIndex(problemId, index);
        // 删除前先读配置(与旧文件状态对齐), 之后按删除结果重编号
        List<PointMeta> metas = readMeta(problemId);
        deleteWithRetry(inputFile(problemId, index));
        deleteWithRetry(outputFile(problemId, index));
        List<Integer> indexes = listIndexes(problemId);
        for (int i : indexes.stream().filter(i -> i > index).sorted().toList()) {
            moveWithRetry(inputFile(problemId, i), inputFile(problemId, i - 1));
            moveWithRetry(outputFile(problemId, i), outputFile(problemId, i - 1));
        }
        // meta 同步: 删掉被删序号, 大于它的序号 -1
        List<PointMeta> updated = new ArrayList<>();
        for (PointMeta m : metas) {
            if (m.index() == index) {
                continue;
            }
            updated.add(m.index() > index
                    ? new PointMeta(m.index() - 1, m.score(), m.timeLimit(), m.memoryLimit())
                    : m);
        }
        writeMeta(problemId, updated);
    }

    private void requireIndex(Long problemId, int index) throws IOException {
        if (index <= 0 || !listIndexes(problemId).contains(index)) {
            throw new IllegalArgumentException("测试点不存在: " + index);
        }
    }

    /**
     * 解析 zip 并替换该题全部测试点(洛谷式严格规则):
     *  - 数据文件必须位于 zip 根目录, 文件名恰一段数字 + .in/.out, 成对出现;
     *  - config.yml(可选, 洛谷格式): 键=文件名, 字段 timeLimit(ms)/memoryLimit(kb)/score,
     *    subtaskId 暂不支持(报错), isPretest 忽略;
     *  - __MACOSX/.DS_Store/点开头文件忽略, 其他任何条目报错;
     *  - 分值: config.yml 指定的用指定值, 未指定的均分剩余(100 - Σ指定), 总分必须 > 0;
     *  - 全部校验通过后写临时目录再原子换名, 失败不破坏旧数据。
     */
    public int replaceAllFromZip(Long problemId, MultipartFile zip) {
        Map<Integer, String> inputs = new HashMap<>();
        Map<Integer, String> outputs = new HashMap<>();
        Map<Integer, Map<String, Object>> configByIndex = new HashMap<>();
        long totalBytes = 0;
        int entryCount = 0;

        try (ZipInputStream zin = new ZipInputStream(zip.getInputStream())) {
            ZipEntry entry;
            while ((entry = zin.getNextEntry()) != null) {
                if (++entryCount > MAX_ZIP_ENTRIES) {
                    throw new IllegalArgumentException("zip 条目过多(超过 " + MAX_ZIP_ENTRIES + ")");
                }
                String name = entry.getName().replace('\\', '/');
                if (entry.isDirectory()) {
                    continue;
                }
                if (isIgnored(name)) {
                    continue;
                }
                if (name.contains("/")) {
                    throw new IllegalArgumentException("测试点文件必须位于 zip 根目录: " + name);
                }
                if (name.equalsIgnoreCase("config.yml")) {
                    parseConfig(zin, name, configByIndex);
                    continue;
                }
                Matcher m = LUOGU_NAME.matcher(name);
                if (!m.matches()) {
                    throw new IllegalArgumentException(
                            "文件名不符合洛谷规则(恰好一段数字 + .in/.out, 如 1.in/1.out): " + name);
                }
                int idx = Integer.parseInt(m.group(1));
                boolean isInput = m.group(2).equalsIgnoreCase("in");
                byte[] bytes = readEntryLimited(zin, name);
                totalBytes += bytes.length;
                if (totalBytes > MAX_TOTAL_BYTES) {
                    throw new IllegalArgumentException("zip 解压后总大小超过 32MB");
                }
                String content = stripBom(new String(bytes, StandardCharsets.UTF_8));
                Map<Integer, String> target = isInput ? inputs : outputs;
                if (target.put(idx, content) != null) {
                    throw new IllegalArgumentException("测试点 " + idx + " 存在多个 ." + (isInput ? "in" : "out") + " 文件");
                }
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("zip 读取失败: " + e.getMessage(), e);
        }

        if (inputs.isEmpty()) {
            throw new IllegalArgumentException("zip 中没有找到测试点文件");
        }
        if (!inputs.keySet().equals(outputs.keySet())) {
            Set<Integer> missingOut = new TreeSet<>(inputs.keySet());
            missingOut.removeAll(outputs.keySet());
            Set<Integer> missingIn = new TreeSet<>(outputs.keySet());
            missingIn.removeAll(inputs.keySet());
            StringBuilder msg = new StringBuilder("测试点输入输出未配对: ");
            if (!missingOut.isEmpty()) {
                msg.append("缺少 .out: ").append(missingOut).append(" ");
            }
            if (!missingIn.isEmpty()) {
                msg.append("缺少 .in: ").append(missingIn);
            }
            throw new IllegalArgumentException(msg.toString().trim());
        }
        if (inputs.size() > MAX_CASES) {
            throw new IllegalArgumentException("测试点数量超过上限 " + MAX_CASES);
        }

        // 分值分配: config.yml 指定的用指定值, 未指定的均分剩余; 无任何指定则全部均分 100
        List<Integer> sorted = inputs.keySet().stream().sorted().toList();
        int specifiedScore = 0;
        int unspecified = 0;
        for (int idx : sorted) {
            Object s = configByIndex.getOrDefault(idx, Map.of()).get("score");
            if (s instanceof Number num && num.intValue() > 0) {
                specifiedScore += num.intValue();
            } else {
                unspecified++;
            }
        }
        if (unspecified == 0 && specifiedScore <= 0) {
            throw new IllegalArgumentException("测试点分值总和必须大于 0");
        }
        int remaining = Math.max(0, 100 - specifiedScore);
        int base = unspecified == 0 ? 0 : remaining / unspecified;
        int rem = unspecified == 0 ? 0 : remaining % unspecified;
        int[] unspecifiedSeen = {0};

        List<PointMeta> metas = new ArrayList<>();
        int n = 1;
        for (int idx : sorted) {
            Map<String, Object> cfg = configByIndex.getOrDefault(idx, Map.of());
            Object s = cfg.get("score");
            int score;
            if (s instanceof Number num && num.intValue() > 0) {
                score = num.intValue();
            } else {
                score = base + (++unspecifiedSeen[0] == unspecified ? rem : 0);
            }
            Integer timeLimit = toIntOrNull(cfg.get("timeLimit"));
            Integer memoryLimitKb = toIntOrNull(cfg.get("memoryLimit"));
            metas.add(new PointMeta(n, score, timeLimit, memoryLimitKb == null ? null : Math.max(1, memoryLimitKb / 1024)));
            n++;
        }

        // 全部校验通过, 写临时目录 -> 旧目录让位 -> 原子换名; 失败回滚
        Path dir = problemDir(problemId);
        Path tmp = root.resolve(problemId + ".tmp-" + System.nanoTime());
        Path bak = root.resolve(problemId + ".bak-" + System.nanoTime());
        try {
            Files.createDirectories(tmp);
            int i = 1;
            for (int idx : sorted) {
                write(tmp.resolve(i + ".in"), inputs.get(idx));
                write(tmp.resolve(i + ".out"), outputs.get(idx));
                i++;
            }
            Files.writeString(tmp.resolve("meta.json"), objectMapper.writeValueAsString(metas), StandardCharsets.UTF_8);
            if (Files.exists(dir)) {
                moveWithRetry(dir, bak);
            }
            try {
                moveWithRetry(tmp, dir);
            } catch (IOException e) {
                if (Files.exists(bak)) {
                    moveWithRetry(bak, dir); // 回滚旧数据
                }
                throw e;
            }
            if (Files.exists(bak)) {
                deleteDirQuietly(bak);
            }
            return sorted.size();
        } catch (IOException e) {
            deleteDirQuietly(tmp);
            throw new IllegalStateException("测试点写入失败: " + e.getMessage(), e);
        }
    }

    /** 解析洛谷 config.yml: 键=文件名(in/out 均可), 映射到测试点序号 */
    @SuppressWarnings("unchecked")
    private void parseConfig(ZipInputStream zin, String entryName, Map<Integer, Map<String, Object>> configByIndex)
            throws IOException {
        byte[] bytes = readEntryLimited(zin, entryName);
        String text = stripBom(new String(bytes, StandardCharsets.UTF_8));
        Map<String, Object> raw;
        try {
            raw = new Yaml().load(text);
        } catch (Exception e) {
            throw new IllegalArgumentException("config.yml 解析失败(YAML 语法错误): " + e.getMessage());
        }
        if (raw == null) {
            return;
        }
        for (Map.Entry<String, Object> e : raw.entrySet()) {
            String fileName = e.getKey().replace('\\', '/');
            if (fileName.contains("/")) {
                throw new IllegalArgumentException("config.yml 的键必须是文件名(不能带目录): " + e.getKey());
            }
            Matcher m = LUOGU_NAME.matcher(fileName);
            if (!m.matches()) {
                throw new IllegalArgumentException("config.yml 中存在无法识别的键(应为 N.in/N.out): " + e.getKey());
            }
            int idx = Integer.parseInt(m.group(1));
            if (!(e.getValue() instanceof Map<?, ?> valueMap)) {
                throw new IllegalArgumentException("config.yml 键 " + e.getKey() + " 的值必须是配置项映射");
            }
            Map<String, Object> cfg = new HashMap<>();
            for (Map.Entry<?, ?> fe : valueMap.entrySet()) {
                String field = String.valueOf(fe.getKey());
                switch (field) {
                    case "timeLimit", "memoryLimit", "score" -> cfg.put(field, fe.getValue());
                    case "subtaskId" ->
                            throw new IllegalArgumentException("config.yml 包含 subtaskId, 暂不支持子任务");
                    case "isPretest" -> { /* CF 概念, 忽略 */ }
                    default -> log.warn("config.yml 忽略未知字段: {}={}", field, fe.getValue());
                }
            }
            configByIndex.put(idx, cfg);
        }
    }

    private Integer toIntOrNull(Object o) {
        return o instanceof Number num ? num.intValue() : null;
    }

    /** 从文件名提取序号(首个数字), 无数字返回 null */
    private Integer indexFromName(String name) {
        Matcher m = INDEX_PATTERN.matcher(name);
        return m.find() ? Integer.valueOf(m.group(1)) : null;
    }

    /** 忽略 mac 打包产物/隐藏文件 */
    private boolean isIgnored(String entryName) {
        for (String seg : entryName.split("/")) {
            if (seg.equals("__MACOSX")) {
                return true;
            }
        }
        String base = entryName.substring(entryName.lastIndexOf('/') + 1);
        return base.equals(".DS_Store") || base.startsWith(".");
    }

    /** 读取单个 zip 条目, 超过上限报错(不信任 ZipEntry.getSize) */
    private byte[] readEntryLimited(ZipInputStream zin, String name) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int n;
        while ((n = zin.read(chunk)) != -1) {
            buf.write(chunk, 0, n);
            if (buf.size() > MAX_ENTRY_BYTES) {
                throw new IllegalArgumentException("测试点文件过大(超过 2MB): " + name);
            }
        }
        return buf.toByteArray();
    }

    /** 读文件: UTF-8 + 去掉 Windows 编辑器常见的 BOM */
    private String read(Path path) throws IOException {
        return stripBom(Files.readString(path, StandardCharsets.UTF_8));
    }

    private String stripBom(String s) {
        return s != null && s.startsWith("\uFEFF") ? s.substring(1) : s;
    }

    /** 写文件: UTF-8 */
    private void write(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, content == null ? "" : content, StandardCharsets.UTF_8);
    }

    /** Windows 下目标可能短暂被占用, 移动失败小睡重试 */
    private void moveWithRetry(Path from, Path to) throws IOException {
        IOException last = null;
        for (int i = 0; i < 5; i++) {
            try {
                Files.move(from, to);
                return;
            } catch (IOException e) {
                last = e;
                sleepQuietly();
            }
        }
        throw last;
    }

    private void deleteWithRetry(Path path) throws IOException {
        IOException last = null;
        for (int i = 0; i < 5; i++) {
            try {
                Files.deleteIfExists(path);
                return;
            } catch (IOException e) {
                last = e;
                sleepQuietly();
            }
        }
        throw last;
    }

    /** 递归删除目录, 失败忽略(清理 .bak/.tmp 用) */
    private void deleteDirQuietly(Path dir) {
        if (dir == null || !Files.exists(dir)) {
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

    private void sleepQuietly() {
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
