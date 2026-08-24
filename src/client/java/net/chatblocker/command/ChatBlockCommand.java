package net.chatblocker.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.chatblocker.config.ChatBlockConfig;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

import java.util.List;

/** 客户端命令 /chatblocker add|remove|list|reload，用于维护屏蔽关键词。 */
public final class ChatBlockCommand {

    private ChatBlockCommand() {
    }

    /** 注册命令树（Mod 入口调用一次） */
    public static void register() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) ->
				dispatcher.register(ClientCommands.literal("chatblocker")
						.then(ClientCommands.literal("add")
								.then(ClientCommands.argument("keyword", StringArgumentType.greedyString())
										.executes(ChatBlockCommand::executeAdd)))
						.then(ClientCommands.literal("remove")
								.then(ClientCommands.argument("keyword", StringArgumentType.greedyString())
										.executes(ChatBlockCommand::executeRemove)))
						.then(ClientCommands.literal("list")
								.executes(ChatBlockCommand::executeList))
						.then(ClientCommands.literal("reload")
								.executes(ChatBlockCommand::executeReload))));
    }

    /** /chatblocker add <关键词>：添加关键词并立即写盘（忽略大小写去重） */
    private static int executeAdd(CommandContext<FabricClientCommandSource> context) {
        String keyword = StringArgumentType.getString(context, "keyword").trim();
        FabricClientCommandSource source = context.getSource();
        ChatBlockConfig config = ChatBlockConfig.INSTANCE;

        if (keyword.isEmpty()) {
            source.sendFeedback(Component.literal("§c关键词不能为空"));
            return 0;
        }
        if (!config.addKeyword(keyword)) {
            source.sendFeedback(Component.literal("§e关键词已存在（不区分大小写）：" + keyword));
            return 0;
        }
        config.saveToDisk();
        source.sendFeedback(Component.literal("§a已添加关键词：" + keyword));
        return 1;
    }

    /** /chatblocker remove <关键词>：移除关键词并立即写盘（忽略大小写） */
    private static int executeRemove(CommandContext<FabricClientCommandSource> context) {
        String keyword = StringArgumentType.getString(context, "keyword").trim();
        FabricClientCommandSource source = context.getSource();
        ChatBlockConfig config = ChatBlockConfig.INSTANCE;

        if (!config.removeKeyword(keyword)) {
            source.sendFeedback(Component.literal("§e关键词不存在：" + keyword));
            return 0;
        }
        config.saveToDisk();
        source.sendFeedback(Component.literal("§a已移除关键词：" + keyword));
        return 1;
    }

    /** /chatblocker list：列出当前全部关键词 */
    private static int executeList(CommandContext<FabricClientCommandSource> context) {
        FabricClientCommandSource source = context.getSource();
        List<String> keywords = ChatBlockConfig.INSTANCE.getKeywords();

        if (keywords.isEmpty()) {
            source.sendFeedback(Component.literal("当前没有屏蔽关键词"));
        } else {
            source.sendFeedback(Component.literal(
                    "当前屏蔽关键词（" + keywords.size() + " 个）：" + String.join("、", keywords)));
        }
        return 1;
    }

    /** /chatblocker reload：从磁盘重新加载配置 */
    private static int executeReload(CommandContext<FabricClientCommandSource> context) {
        FabricClientCommandSource source = context.getSource();
        boolean success = ChatBlockConfig.INSTANCE.loadFromDisk();

        if (success) {
            source.sendFeedback(Component.literal("§a已重新加载配置"));
            return 1;
        }
        source.sendFeedback(Component.literal("§c配置文件不存在或损坏，已保留当前配置"));
        return 0;
    }
}
