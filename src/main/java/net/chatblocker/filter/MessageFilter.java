package net.chatblocker.filter;

import java.util.Collection;
import java.util.Objects;
import java.util.UUID;

/**
 * 聊天屏蔽判定：屏蔽别人发送的命中关键词的消息，自己发送的消息豁免。
 * 纯逻辑，不依赖 Minecraft 类，可单元测试。
 */
public final class MessageFilter {

    private MessageFilter() {
    }

    /**
     * 判断一条聊天消息是否应被屏蔽（不在本地聊天框显示）。
     *
     * @param messageText 消息文本
     * @param senderId    发送者 UUID，可为 null（离线服务器聊天与系统消息无发送者信息）
     * @param selfId      本地玩家 UUID，可为 null（不在游戏中时不屏蔽）
     * @param enabled     屏蔽功能是否启用
     * @param keywords    屏蔽关键词集合
     * @return 应屏蔽返回 true，放行返回 false
     */
    public static boolean shouldBlock(String messageText, UUID senderId, UUID selfId,
                                      boolean enabled, Collection<String> keywords) {
        // 功能关闭时不屏蔽
        if (!enabled) {
            return false;
        }
        // 本地玩家信息缺失（不在游戏中）时不屏蔽
        if (selfId == null) {
            return false;
        }
        // 自己发送的消息豁免，不屏蔽
        if (Objects.equals(senderId, selfId)) {
            return false;
        }
        // 别人的消息或匿名消息（离线服务器/系统消息，senderId 为 null）命中关键词则屏蔽
        return KeywordFilter.containsBlockedKeyword(messageText, keywords);
    }
}
