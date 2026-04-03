package is.pig.minecraft.lib.inventory.sort;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class BurstControllerTest {

    @Test
    public void testAIMDIncreases() {
        BurstController controller = new BurstController();
        int initial = controller.getCurrentWindow(); // 50
        
        controller.reportSuccess();
        assertEquals(initial + 5, controller.getCurrentWindow());
        
        // Rapid growth to max
        for (int i = 0; i < 40; i++) controller.reportSuccess();
        assertEquals(200, controller.getCurrentWindow());
    }

    @Test
    public void testAIMDDecreases() {
        BurstController controller = new BurstController();
        int initial = controller.getCurrentWindow(); // 50
        
        controller.reportDesync();
        assertEquals(initial / 2, controller.getCurrentWindow());
        
        // Shrink to min
        for (int i = 0; i < 10; i++) controller.reportDesync();
        assertEquals(1, controller.getCurrentWindow());
    }

    @Test
    public void testGetNextBurst() {
        BurstController controller = new BurstController();
        // Window is 50
        List<Move> moves = new ArrayList<>();
        for (int i = 0; i < 100; i++) moves.add(new Move.LeftClick(i));
        
        List<Move> burst = controller.getNextBurst(moves);
        assertEquals(50, burst.size());
        assertEquals(0, burst.get(0).slotIndex());
        assertEquals(49, burst.get(49).slotIndex());
        
        // Small list
        List<Move> small = moves.subList(0, 10);
        assertEquals(10, controller.getNextBurst(small).size());
    }
}
