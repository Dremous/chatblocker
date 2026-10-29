// SPDX-License-Identifier: MIT
package net.chatblocker.event;

import java.util.Locale;
import java.util.UUID;
import net.chatblocker.config.ChatBlockConfig;
import net.chatblocker.filter.InboundMessageFilter;
import net.chatblocker.filter.OwnEchoTracker;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
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
        // 记录会产生聊天回显的命令参数（/say、/msg 等），避免自己的回显被自己屏蔽
        ClientSendMessageEvents.COMMAND.register(ChatMessageHandler::recordCommandEcho);
        // 断开连接时清空记录，避免换服后旧回显误豁免
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> OWN_ECHO_TRACKER.clear());

        // 玩家聊天消息：发送者为其他玩家或 null（离线服务器无签名聊天）时按关键词判定
        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
            Minecraft client = Minecraft.getInstance();
            // 不在游戏中时放行
            if (client.player == null) {
                return true;
            }
            // 离线服务器聊天消息 sender 为 null，此时 senderId 传 null（匿名路径）
            // 注意：authlib 7 的 GameProfile 是 Record，getId() 已改名为 id()
            UUID senderId = sender == null ? null : sender.id();
            ChatBlockConfig config = ChatBlockConfig.INSTANCE;
            // 判定逻辑（屏蔽别人/匿名消息、自己豁免）见 InboundMessageFilter
            return !InboundMessageFilter.shouldBlock(message.getString(), senderId,
                    client.player.getUUID(), config.isEnabled(), config.getKeywords(),
                    config.isExemptOwnEcho(), OWN_ECHO_TRACKER);
        });

        // 系统消息：服务器公告、插件消息、/say 等，无发送者信息，命中关键词即屏蔽
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            // action bar（覆盖层）消息放行，仅过滤聊天栏系统消息
            if (overlay) {
                return true;
            }
            Minecraft client = Minecraft.getInstance();
            // 不在游戏中时放行
            if (client.player == null) {
                return true;
            }
            ChatBlockConfig config = ChatBlockConfig.INSTANCE;
            return !InboundMessageFilter.shouldBlock(message.getString(), null,
                    client.player.getUUID(), config.isEnabled(), config.getKeywords(),
                    config.isExemptOwnEcho(), OWN_ECHO_TRACKER);
        });
    }

    /**
     * 记录会产生聊天回显的命令参数。
     * 只记录消息正文，不记录整条命令，避免 "/gamemode creative" 之类造成大面积误豁免。
     *
     * @param command 命令文本（不含前导斜杠）
     */
    private static void recordCommandEcho(String command) {
        if (command == null || command.isBlank()) {
            return;
        }
        String trimmed = command.trim();
        int firstSpace = trimmed.indexOf(' ');
        if (firstSpace <= 0) {
            return;
        }
        String head = trimmed.substring(0, firstSpace).toLowerCase(Locale.ROOT);
        String rest = trimmed.substring(firstSpace + 1).trim();
        if (rest.isEmpty()) {
            return;
        }
        switch (head) {
            // 整段参数就是消息正文
            case "say", "me", "r" -> OWN_ECHO_TRACKER.record(rest);
            // 第一个参数是目标玩家，其余才是消息正文
            case "msg", "tell", "w" -> {
                int secondSpace = rest.indexOf(' ');
                if (secondSpace > 0) {
                    OWN_ECHO_TRACKER.record(rest.substring(secondSpace + 1));
                }
            }
            default -> {
                // 其他命令不产生聊天回显，无需记录
            }
        }
    }
}
