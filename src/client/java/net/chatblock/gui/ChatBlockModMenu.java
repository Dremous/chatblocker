package net.chatblock.gui;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** ModMenu 集成：在 Mod 列表中为本 Mod 提供配置按钮。 */
public class ChatBlockModMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ChatBlockConfigScreen::create;
    }
}
