package dev.phantasm.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.phantasm.config.PhantasmConfigScreen;

public final class PhantasmModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return PhantasmConfigScreen::build;
    }
}
