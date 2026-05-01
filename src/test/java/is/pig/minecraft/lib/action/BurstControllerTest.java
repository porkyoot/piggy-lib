package is.pig.minecraft.lib.action;

import is.pig.minecraft.inventory.sorting.Move;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class BurstControllerTest {

    @Test
    public void testAIMDIncreases() {
        BurstController controller = new BurstController();
        int initial = controller.getCurrentWindow(); // 100
        
        controller.reportSuccess(0); // 0 ping
        assertEquals(initial + 10, controller.getCurrentWindow());
        
        // Rapid growth to max
        for (int i = 0; i < 50; i++) controller.reportSuccess(0);
        assertEquals(500, controller.getCurrentWindow());
    }

    @Test
    public void testAIMDDecreases() {
        BurstController controller = new BurstController();
        int initial = controller.getCurrentWindow(); // 100
        
        controller.reportDesync();
        assertEquals(initial / 2, controller.getCurrentWindow());
        
        // Shrink to min
        for (int i = 0; i < 10; i++) controller.reportDesync();
        assertEquals(1, controller.getCurrentWindow());
    }

    @Test
    public void testGetNextBurst() {
        BurstController controller = new BurstController();
        // Window is 100
        List<Move> moves = new ArrayList<>();
        for (int i = 0; i < 200; i++) moves.add(new Move(i, Move.MoveType.PICKUP_ALL));
        
        List<Move> burst = controller.getNextBurst(moves);
        assertEquals(100, burst.size());
        assertEquals(0, burst.get(0).slotIndex());
        assertEquals(99, burst.get(99).slotIndex());
        
        // Small list
        List<Move> small = moves.subList(0, 10);
        assertEquals(10, controller.getNextBurst(small).size());
    }
}
