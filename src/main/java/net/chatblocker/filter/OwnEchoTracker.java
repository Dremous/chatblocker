package net.chatblocker.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 自己消息回显追踪器：记录自己刚发出的聊天文本，用于在离线服务器（消息无发送者信息）
 * 上识别服务器回显的自己消息并豁免屏蔽。纯 JDK 实现，可单元测试。
 */
public final class OwnEchoTracker {

    /** 默认存活时间：10 秒（覆盖高延迟服务器回显） */
    public static final long DEFAULT_TTL_MILLIS = 10_000L;

    /** 条目存活时间（毫秒），测试可注入短 TTL */
    private final long ttlMillis;

    /** 已记录文本（小写规范化），与记录时间戳一一对应 */
    private final List<String> texts = new ArrayList<>();
    private final List<Long> timestamps = new ArrayList<>();

    /** 构造追踪器，使用默认存活时间 */
    public OwnEchoTracker() {
        this(DEFAULT_TTL_MILLIS);
    }

    /** 构造追踪器并指定条目存活时间（毫秒） */
    public OwnEchoTracker(long ttlMillis) {
        this.ttlMillis = ttlMillis;
    }

    /**
     * 记录自己刚发出的聊天原文（空白文本忽略）。
     *
     * @param text 出站聊天文本
     */
    public void record(String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        // 先清理过期条目，避免列表无限增长
        cleanupExpired();
        texts.add(text.toLowerCase(Locale.ROOT));
        timestamps.add(System.currentTimeMillis());
    }

    /**
     * 判断消息文本是否为自己消息的回显（包含任一未过期原文，不区分大小写）。
     * 服务器回显 = 聊天装饰 + 原文，因此用包含匹配。
     *
     * @param messageText 接收到的消息文本
     * @return 判定为自己的回显返回 true
     */
    public boolean matches(String messageText) {
        if (messageText == null || messageText.isEmpty()) {
            return false;
        }
        cleanupExpired();
        String normalized = messageText.toLowerCase(Locale.ROOT);
        for (String text : texts) {
            if (normalized.contains(text)) {
                return true;
            }
        }
        return false;
    }

    /** 懒清理：移除已过期的条目（客户端主线程调用，无并发） */
    private void cleanupExpired() {
        long now = System.currentTimeMillis();
        int aliveCount = 0;
        for (int i = 0; i < texts.size(); i++) {
            if (now - timestamps.get(i) <= ttlMillis) {
                // 存活条目前移，保持 texts 与 timestamps 对齐
                texts.set(aliveCount, texts.get(i));
                timestamps.set(aliveCount, timestamps.get(i));
                aliveCount++;
            }
        }
        if (aliveCount < texts.size()) {
            texts.subList(aliveCount, texts.size()).clear();
            timestamps.subList(aliveCount, timestamps.size()).clear();
        }
    }
}
