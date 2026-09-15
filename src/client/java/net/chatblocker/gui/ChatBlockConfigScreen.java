// SPDX-License-Identifier: MIT
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
                .title(Component.translatable("chatblocker.config.title"))
                .category(ConfigCategory.createBuilder()
                        .name(Component.translatable("chatblocker.config.category"))
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("chatblocker.config.enabled"))
                                .description(OptionDescription.of(
                                        Component.translatable("chatblocker.config.enabled.desc")))
                                .binding(true, config::isEnabled, config::setEnabled)
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("chatblocker.config.exempt_own_echo"))
                                .description(OptionDescription.of(
                                        Component.translatable("chatblocker.config.exempt_own_echo.desc")))
                                .binding(true, config::isExemptOwnEcho, config::setExemptOwnEcho)
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .group(ListOption.<String>createBuilder()
                                .name(Component.translatable("chatblocker.config.keywords"))
                                .description(OptionDescription.of(
                                        Component.translatable("chatblocker.config.keywords.desc")))
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