// SPDX-License-Identifier: MIT
package net.chatblocker.filter;

import java.util.Collection;
import java.util.UUID;

/**
 * 入站消息的完整屏蔽判定：关键词命中、自己消息豁免（在线按 UUID）、
 * 离线服务器回显豁免（发送者未知时按文本匹配）。
 * 纯 JDK 逻辑，不依赖 Minecraft 类，可单元测试。
 */
public final class InboundMessageFilter {

    private InboundMessageFilter() {
    }

    /**
     * 判断一条入站消息是否应被屏蔽。
     *
     * @param messageText   消息文本
     * @param senderId      发送者 UUID，可为 null（离线服务器聊天与系统消息无发送者信息）
     * @param selfId        本地玩家 UUID，可为 null（不在游戏中时不屏蔽）
     * @param enabled       屏蔽功能是否启用
     * @param keywords      屏蔽关键词集合
     * @param exemptOwnEcho 是否按文本豁免自己消息的回显
     * @param tracker       自己消息回显追踪器，可为 null（不豁免）
     * @return 应屏蔽返回 true，放行返回 false
     */
    public static boolean shouldBlock(String messageText, UUID senderId, UUID selfId,
                                      boolean enabled, Collection<String> keywords,
                                      boolean exemptOwnEcho, OwnEchoTracker tracker) {
        // 基础判定：功能开关、是否在游戏中、自己的消息按 UUID 豁免
        if (!MessageFilter.shouldBlock(messageText, senderId, selfId, enabled, keywords)) {
            return false;
        }
        // 只有发送者未知（离线服务器聊天/系统消息）时才用回显文本豁免；
        // 有发送者时说明能拿到 UUID，不能让他人复述自己的话绕过屏蔽。
        boolean ownEcho = exemptOwnEcho && senderId == null
                && tracker != null && tracker.matches(messageText);
        return !ownEcho;
    }
}
