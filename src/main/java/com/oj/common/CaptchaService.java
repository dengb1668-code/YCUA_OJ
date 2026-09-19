package com.oj.common;

import com.oj.vo.CaptchaVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 登录图片验证码: Java2D 自绘 PNG + Redis 存储(5 分钟, 一次性)
 * <p>
 * 安全性: 验证码无论对错都会在使用后删除(防爆破/重放); 大小写不敏感。
 */
@Service
@RequiredArgsConstructor
public class CaptchaService {

    private static final String KEY_PREFIX = "oj:captcha:";
    private static final long TTL_MINUTES = 5;
    /** 字符集: 去掉易混淆的 0/O/1/l/I */
    private static final char[] CHARS = "23456789ABCDEFGHJKMNPQRSTUVWXYZ".toCharArray();
    private static final int LENGTH = 4;
    private static final int WIDTH = 130;
    private static final int HEIGHT = 48;

    private final StringRedisTemplate redisTemplate;
    private final SecureRandom random = new SecureRandom();

    /** 生成验证码: 返回 captchaId 与 base64 图片(data URL 前缀, 前端可直接作 img src) */
    public CaptchaVO generate() {
        StringBuilder code = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            code.append(CHARS[random.nextInt(CHARS.length)]);
        }
        String id = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(KEY_PREFIX + id, code.toString(), TTL_MINUTES, TimeUnit.MINUTES);
        return new CaptchaVO(id, "data:image/png;base64," + draw(code.toString()));
    }

    /** 校验验证码: 无论对错都删除(一次性); 错误/过期抛异常 */
    public void verify(String captchaId, String captchaCode) {
        String key = KEY_PREFIX + captchaId;
        String expected = redisTemplate.opsForValue().get(key);
        redisTemplate.delete(key);
        if (expected == null || captchaCode == null
                || !expected.equalsIgnoreCase(captchaCode.trim())) {
            throw new IllegalArgumentException("验证码错误或已过期");
        }
    }

    /** Java2D 绘制验证码图片: 浅灰底 + 随机旋转字符 + 干扰线 + 噪点 */
    private String draw(String code) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // 底色: 浅灰带轻微渐变感
            g.setColor(new Color(245, 247, 249));
            g.fillRect(0, 0, WIDTH, HEIGHT);

            // 干扰线
            g.setStroke(new BasicStroke(1.2f));
            for (int i = 0; i < 3; i++) {
                g.setColor(new Color(120 + random.nextInt(120), 120 + random.nextInt(120), 120 + random.nextInt(120)));
                g.drawLine(random.nextInt(WIDTH), random.nextInt(HEIGHT),
                        random.nextInt(WIDTH), random.nextInt(HEIGHT));
            }

            // 字符: 随机深色 + 随机旋转
            int charWidth = WIDTH / (LENGTH + 1);
            for (int i = 0; i < LENGTH; i++) {
                g.setFont(new Font("Arial", Font.BOLD, 30 + random.nextInt(6)));
                g.setColor(new Color(30 + random.nextInt(90), 30 + random.nextInt(90), 30 + random.nextInt(90)));
                double angle = Math.toRadians(random.nextInt(31) - 15); // ±15°
                int x = charWidth * i + charWidth / 2 + random.nextInt(6) - 3;
                int y = HEIGHT / 2 + 11 + random.nextInt(6) - 3;
                g.rotate(angle, x, y);
                g.drawString(String.valueOf(code.charAt(i)), x, y);
                g.rotate(-angle, x, y);
            }

            // 噪点
            for (int i = 0; i < 80; i++) {
                g.setColor(new Color(random.nextInt(200), random.nextInt(200), random.nextInt(200)));
                g.fillRect(random.nextInt(WIDTH), random.nextInt(HEIGHT), 1, 1);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException("验证码图片生成失败", e);
        } finally {
            g.dispose();
        }
    }
}
