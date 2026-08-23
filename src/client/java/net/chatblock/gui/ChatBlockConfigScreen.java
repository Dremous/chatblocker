package net.chatblock.gui;

import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.ListOption;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import net.chatblock.config.ChatBlockConfig;
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
                .title(Component.literal("ChatBlock 设置"))
                .category(ConfigCategory.createBuilder()
                        .name(Component.literal("屏蔽设置"))
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.literal("启用屏蔽"))
                                .description(OptionDescription.of(
                                        Component.literal("关闭后不再过滤任何消息")))
                                .binding(true, config::isEnabled, config::setEnabled)
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .group(ListOption.<String>createBuilder()
                                .name(Component.literal("屏蔽关键词"))
                                .description(OptionDescription.of(
                                        Component.literal("消息包含任一关键词时，自己不显示该消息（不区分大小写）")))
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
