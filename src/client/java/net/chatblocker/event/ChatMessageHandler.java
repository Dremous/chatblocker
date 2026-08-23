package net.chatblocker.event;

import java.util.UUID;
import net.chatblocker.config.ChatBlockConfig;
import net.chatblocker.filter.MessageFilter;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;

/**
 * 聊天消息过滤：当服务器广播的消息发送者是其他玩家且命中关键词时，
 * 返回 false 阻止该消息在本地聊天框显示。自己发送的消息豁免，不屏蔽。
 */
public final class ChatMessageHandler {

    private ChatMessageHandler() {
    }

    /** 注册过滤回调（Mod 入口调用一次） */
    public static void register() {
        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
            Minecraft client = Minecraft.getInstance();
            // 不在游戏中时放行
            if (client.player == null) {
                return true;
            }
            // 发送者信息缺失时放行（不误伤系统/插件消息）；sender 为 null 时 senderId 传 null
            UUID senderId = sender == null ? null : sender.getId();
            ChatBlockConfig config = ChatBlockConfig.INSTANCE;
            // 判定逻辑（屏蔽别人、自己豁免）见 MessageFilter
            boolean block = MessageFilter.shouldBlock(message.getString(), senderId,
                    client.player.getUUID(), config.isEnabled(), config.getKeywords());
            return !block;
        });
    }
}
