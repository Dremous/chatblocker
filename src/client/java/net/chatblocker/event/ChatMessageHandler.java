package net.chatblocker.event;

import java.util.UUID;
import net.chatblocker.config.ChatBlockConfig;
import net.chatblocker.filter.MessageFilter;
import net.chatblocker.filter.OwnEchoTracker;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.Minecraft;

/**
 * 聊天消息过滤：当服务器广播的消息命中关键词时，返回 false 阻止该消息在本地聊天框显示。
 * 覆盖两个通道：玩家聊天（ALLOW_CHAT，自己发送的消息豁免）与系统消息（ALLOW_GAME，
 * 服务器公告/插件消息/离线服务器聊天等，无发送者信息）。
 * 离线服务器消息无发送者信息，通过 OwnEchoTracker 按文本匹配豁免自己消息的回显。
 */
public final class ChatMessageHandler {

    /** 自己消息回显追踪器（记录出站原文，回显包含原文时豁免） */
    private static final OwnEchoTracker OWN_ECHO_TRACKER = new OwnEchoTracker();

    private ChatMessageHandler() {
    }

    /** 注册过滤回调（Mod 入口调用一次） */
    public static void register() {
        // 记录自己发出的聊天原文，用于离线服务器回显豁免
        ClientSendMessageEvents.CHAT.register(OWN_ECHO_TRACKER::record);

        // 玩家聊天消息：发送者为其他玩家或 null（离线服务器无签名聊天）时按关键词判定
        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
            Minecraft client = Minecraft.getInstance();
            // 不在游戏中时放行
            if (client.player == null) {
                return true;
            }
            // 离线服务器聊天消息 sender 为 null，此时 senderId 传 null（匿名路径）
            UUID senderId = sender == null ? null : sender.getId();
            ChatBlockConfig config = ChatBlockConfig.INSTANCE;
            String text = message.getString();
            // 判定逻辑（屏蔽别人/匿名消息、自己豁免）见 MessageFilter
            boolean block = MessageFilter.shouldBlock(text, senderId,
                    client.player.getUUID(), config.isEnabled(), config.getKeywords());
            // 匿名消息命中关键词时，若为自己消息的回显则豁免（离线服务器场景）
            if (block && senderId == null && config.isExemptOwnEcho() && OWN_ECHO_TRACKER.matches(text)) {
                return true;
            }
            return !block;
        });

        // 系统消息：服务器公告、插件消息、/say 等，无发送者信息，命中关键词即屏蔽
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            Minecraft client = Minecraft.getInstance();
            // 不在游戏中时放行
            if (client.player == null) {
                return true;
            }
            ChatBlockConfig config = ChatBlockConfig.INSTANCE;
            String text = message.getString();
            boolean block = MessageFilter.shouldBlock(text, null,
                    client.player.getUUID(), config.isEnabled(), config.getKeywords());
            // 系统消息命中关键词时，若为自己消息的回显则豁免（部分服务器将聊天转为系统消息回显）
            if (block && config.isExemptOwnEcho() && OWN_ECHO_TRACKER.matches(text)) {
                return true;
            }
            return !block;
        });
    }
}
