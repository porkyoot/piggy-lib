package is.pig.minecraft.lib.ui;

import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Reusable YACL option builder for "No Cheating Mode" toggle.
 * Provides standardized confirmation dialog and warning messages.
 */
public class NoCheatingModeOption {

    /**
     * Creates a "No Cheating Mode" option with confirmation dialog.
     * 
     * @param parent       The parent screen to return to after confirmation
     * @param getter       Function to get the current value
     * @param setter       Function to set the new value
     * @param saveCallback Callback to save the configuration
     * @return Configured YACL Option
     */
    public static Option<Boolean> create(
            Screen parent,
            Supplier<Boolean> getter,
            Consumer<Boolean> setter,
            Runnable saveCallback) {

        return Option.<Boolean>createBuilder()
                .name(Component.literal("No Cheating Mode"))
                .description(OptionDescription.of(
                        Component.literal("Prevents usage of cheat features in Survival mode."),
                        Component.literal(""),
                        Component.literal("§cWARNING: Disabling this allows cheats on servers!"),
                        Component.literal("§cThis MIGHT result in a BAN on some servers.")))
                .binding(
                        true,
                        getter,
                        newValue -> {
                            boolean currentValue = getter.get();
                            if (!newValue && currentValue) {
                                // User is attempting to disable it - show confirmation
                                Minecraft client = Minecraft.getInstance();
                                client.setScreen(new ConfirmScreen(
                                        (confirmed) -> {
                                            if (confirmed) {
                                                setter.accept(false);
                                                saveCallback.run();
                                            } else {
                                                setter.accept(true); // Revert
                                                saveCallback.run();
                                            }
                                            // Re-open config screen
                                            // Note: We can't directly call the factory here
                                            // The parent screen should handle this
                                            client.setScreen(parent);
                                        },
                                        Component.literal("Disable No Cheating Mode?"),
                                        Component.literal(
                                                "§cWARNING: Disabling this allows the use of cheat features in Survival mode.\n"
                                                        +
                                                        "Using these on servers is detectable and MIGHT RESULT IN A BAN.\n\n"
                                                        +
                                                        "Are you sure you want to continue?"),
                                        Component.literal("Yes, I understand the risks"),
                                        Component.literal("Cancel")));
                            } else {
                                setter.accept(newValue);
                            }
                        })
                .controller(TickBoxControllerBuilder::create)
                .build();
    }
}
