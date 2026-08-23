package net.chatblock;

import net.chatblock.config.ChatBlockConfig;
import net.chatblock.event.ChatMessageHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

/** Mod 客户端入口：加载配置并注册事件监听。 */
public class ChatBlockMod implements ClientModInitializer {

    /** Mod ID */
    public static final String MOD_ID = "chatblock";

    @Override
    public void onInitializeClient() {
        // 配置文件位于 .minecraft/config/chatblock.json
        ChatBlockConfig.INSTANCE.setFilePath(
                FabricLoader.getInstance().getConfigDir().resolve("chatblock.json"));
        ChatBlockConfig.INSTANCE.loadFromDisk();

        // 注册消息过滤事件
        ChatMessageHandler.register();
    }
}
