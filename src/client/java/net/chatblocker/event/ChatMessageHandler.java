package net.chatblocker.event;

import java.util.UUID;
import net.chatblocker.config.ChatBlockConfig;
import net.chatblocker.filter.MessageFilter;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;

/**
 * 聊天消息过滤：当服务器广播的消息命中关键词时，返回 false 阻止该消息在本地聊天框显示。
 * 覆盖两个通道：玩家聊天（ALLOW_CHAT，自己发送的消息豁免）与系统消息（ALLOW_GAME，
 * 服务器公告/插件消息/离线服务器聊天等，无发送者信息，无法豁免自己）。
 */
public final class ChatMessageHandler {

    private ChatMessageHandler() {
    }

    /** 注册过滤回调（Mod 入口调用一次） */
    public static void register() {
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
            // 判定逻辑（屏蔽别人/匿名消息、自己豁免）见 MessageFilter
            boolean block = MessageFilter.shouldBlock(message.getString(), senderId,
                    client.player.getUUID(), config.isEnabled(), config.getKeywords());
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
            boolean block = MessageFilter.shouldBlock(message.getString(), null,
                    client.player.getUUID(), config.isEnabled(), config.getKeywords());
            return !block;
        });
    }
}
