package net.chatblocker.gui;

import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.ListOption;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import net.chatblocker.config.ChatBlockConfig;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/** YACL 配置界面工厂：每次调用生成新的配置 Screen 实例。 */
public final class ChatBlockConfigScreen {

    private ChatBlockConfigScreen() {
    }

    /** 基于当前配置生成配置界面；parent 为关闭界面后返回的屏幕 */
    public static Screen create(Screen parent) {
        ChatBlockConfig config = ChatBlockConfig.INSTANCE;

        return YetAnotherConfigLib.createBuilder()
                .title(Component.literal("ChatBlocker 设置"))
                .category(ConfigCategory.createBuilder()
                        .name(Component.literal("屏蔽设置"))
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.literal("启用屏蔽"))
                                .description(OptionDescription.of(
                                        Component.literal("关闭后不再过滤任何消息。修改立即生效，保存用于持久化")))
                                .binding(true, config::isEnabled, config::setEnabled)
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.literal("豁免自己消息的回显"))
                                .description(OptionDescription.of(
                                        Component.literal("离线服务器聊天无发送者信息，按文本匹配豁免自己刚发出的消息回显；别人在数秒内发送完全相同文本时可能被误豁免")))
                                .binding(true, config::isExemptOwnEcho, config::setExemptOwnEcho)
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .group(ListOption.<String>createBuilder()
                                .name(Component.literal("屏蔽关键词"))
                                .description(OptionDescription.of(
                                        Component.literal("别人发送的消息包含任一关键词时，自己不显示该消息（不区分大小写）")))
                                .binding(List.of(), config::getKeywords, config::setKeywords)
                                .controller(StringControllerBuilder::create)
                                .initial("")
                                .build())
                        .build())
                // 点击保存按钮时写盘
                .save(config::saveToDisk)
                .build()
                .generateScreen(parent);
    }
}
