package net.chatblocker;

import net.chatblocker.command.ChatBlockCommand;
import net.chatblocker.config.ChatBlockConfig;
import net.chatblocker.event.ChatMessageHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

/** Mod 客户端入口：加载配置并注册事件监听。 */
public class ChatBlockMod implements ClientModInitializer {

    /** Mod ID */
    public static final String MOD_ID = "chatblocker";

    @Override
    public void onInitializeClient() {
        // 配置文件位于 .minecraft/config/chatblocker.json
        ChatBlockConfig.INSTANCE.setFilePath(
                FabricLoader.getInstance().getConfigDir().resolve("chatblocker.json"));
        ChatBlockConfig.INSTANCE.loadFromDisk();

        // 注册消息过滤事件
        ChatMessageHandler.register();

        // 注册客户端命令
        ChatBlockCommand.register();
    }
}
