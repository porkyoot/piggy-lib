package is.pig.minecraft.lib.legacy;

import is.pig.minecraft.lib.api.IScreenManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public class LegacyScreenManager implements IScreenManager {

    @Override
    public void openScreen(Object screenInstance) {
        Minecraft client = Minecraft.getInstance();
        if (screenInstance instanceof Screen screen) {
            client.setScreen(screen);
        }
    }

    @Override
    public int getMouseX() {
        Minecraft client = Minecraft.getInstance();
        return (int) (client.mouseHandler.xpos() * client.getWindow().getGuiScaledWidth() / client.getWindow().getWidth());
    }

    @Override
    public int getMouseY() {
        Minecraft client = Minecraft.getInstance();
        return (int) (client.mouseHandler.ypos() * client.getWindow().getGuiScaledHeight() / client.getWindow().getHeight());
    }
}
