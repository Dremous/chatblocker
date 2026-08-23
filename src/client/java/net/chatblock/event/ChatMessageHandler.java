package net.chatblock.event;

import net.chatblock.config.ChatBlockConfig;
import net.chatblock.filter.KeywordFilter;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;

/**
 * 聊天消息过滤：当服务器广播的消息发送者是自己且命中关键词时，
 * 返回 false 阻止该消息在本地聊天框显示。其他玩家的客户端不受影响。
 */
public final class ChatMessageHandler {

    private ChatMessageHandler() {
    }

    /** 注册过滤回调（Mod 入口调用一次） */
    public static void register() {
        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
            ChatBlockConfig config = ChatBlockConfig.INSTANCE;
            // 屏蔽功能关闭时放行
            if (!config.isEnabled()) {
                return true;
            }
            Minecraft client = Minecraft.getInstance();
            // 不在游戏中或发送者信息缺失时放行（不误伤系统/插件消息）
            if (client.player == null || sender == null) {
                return true;
            }
            // 只过滤自己发送的消息，别人的消息正常显示
            if (!sender.getId().equals(client.player.getUUID())) {
                return true;
            }
            // 命中关键词则阻止显示
            return !KeywordFilter.containsBlockedKeyword(message.getString(), config.getKeywords());
        });
    }
}
