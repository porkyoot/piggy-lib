package is.pig.minecraft.lib.legacy;

import is.pig.minecraft.lib.api.IPlayerController;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.ClickType;

public class LegacyPlayerController implements IPlayerController {

    @Override
    public void clickInventorySlot(int syncId, int slotId, int button, String clickType) {
        Minecraft client = Minecraft.getInstance();
        if (client.gameMode != null && client.player != null) {
            ClickType type = ClickType.valueOf(clickType);
            client.gameMode.handleInventoryMouseClick(syncId, slotId, button, type, client.player);
        }
    }

    @Override
    public void attackEntity(int entityId) {
        Minecraft client = Minecraft.getInstance();
        if (client.gameMode != null && client.player != null && client.level != null) {
            Entity target = client.level.getEntity(entityId);
            if (target != null) {
                client.gameMode.attack(client.player, target);
                client.player.swing(InteractionHand.MAIN_HAND);
            }
        }
    }

    @Override
    public void startBreakingBlock(int x, int y, int z, String face) {
        Minecraft client = Minecraft.getInstance();
        if (client.gameMode != null) {
            BlockPos pos = new BlockPos(x, y, z);
            Direction dir = Direction.valueOf(face);
            client.gameMode.startDestroyBlock(pos, dir);
        }
    }

    @Override
    public void setCameraRotation(float yaw, float pitch) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            client.player.setYRot(yaw);
            client.player.setXRot(pitch);
        }
    }
}
