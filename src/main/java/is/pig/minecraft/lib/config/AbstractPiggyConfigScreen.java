package is.pig.minecraft.lib.config;

import dev.isxander.yacl3.api.YetAnotherConfigLib;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Base class for Piggy Mods configuration screens.
 * Wraps YetAnotherConfigLib builder.
 */
public abstract class AbstractPiggyConfigScreen {
    private final String title;
    private final PiggyConfigManager<?> configManager;

    protected AbstractPiggyConfigScreen(String title, PiggyConfigManager<?> configManager) {
        this.title = title;
        this.configManager = configManager;
    }

    /**
     * Creates the screen.
     *
     * @param parent The parent screen.
     * @return The generated Screen.
     */
    public Screen create(Screen parent) {
        YetAnotherConfigLib.Builder builder = YetAnotherConfigLib.createBuilder()
                .title(Component.literal(title));

        addCategories(builder);

        return builder.save(configManager::save)
                .build()
                .generateScreen(parent);
    }

    /**
     * Subclasses must implement this to add their specific categories and options.
     *
     * @param builder The YACL builder.
     */
    protected abstract void addCategories(YetAnotherConfigLib.Builder builder);
}
