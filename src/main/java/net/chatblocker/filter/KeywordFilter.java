package net.chatblocker.filter;

import java.util.Collection;
import java.util.Locale;

/** 聊天关键词过滤器：包含匹配、不区分大小写。纯逻辑，不依赖 Minecraft 类。 */
public final class KeywordFilter {

    private KeywordFilter() {
    }

    /**
     * 判断消息是否命中任一屏蔽关键词。
     *
     * @param message  待检测的消息文本
     * @param keywords 屏蔽关键词集合
     * @return 命中返回 true；消息为空、关键词为空或全部无效时返回 false
     */
    public static boolean containsBlockedKeyword(String message, Collection<String> keywords) {
        // 消息为空或没有有效关键词时直接放行
        if (message == null || message.isBlank() || keywords == null || keywords.isEmpty()) {
            return false;
        }
        // 统一转小写实现大小写不敏感
        String normalized = message.toLowerCase(Locale.ROOT);
        for (String keyword : keywords) {
            // 跳过空白与 null 关键词
            if (keyword == null || keyword.isBlank()) {
                continue;
            }
            if (normalized.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}
