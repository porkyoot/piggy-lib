package is.pig.minecraft.lib.util.telemetry.formatter;

import is.pig.minecraft.api.FallPredictionResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FormatterTest {

    @Test
    void testFormatDouble() {
        assertEquals("123.46", FormatterUtils.formatDouble(123.4567));
        assertEquals("0.00", FormatterUtils.formatDouble(0.0));
        assertEquals("-1.20", FormatterUtils.formatDouble(-1.2));
    }

    @Test
    void testFormatVec3() {
        Vec3 vec = new Vec3(1.234, 5.678, -9.012);
        assertEquals("(1.23, 5.68, -9.01)", FormatterUtils.formatVec3(vec));
        assertEquals("null", FormatterUtils.formatVec3(null));
    }

    @Test
    void testFormatBlockPos() {
        BlockPos pos = new BlockPos(10, 20, 30);
        assertEquals("(10, 20, 30)", FormatterUtils.formatBlockPos(pos));
        assertEquals("null", FormatterUtils.formatBlockPos(null));
    }

    @Test
    void testFormatTrajectory() {
        FallPredictionResult prediction = new FallPredictionResult(
            new is.pig.minecraft.api.BlockPos(1, 2, 3),
            new is.pig.minecraft.api.Vec3(1.1, 2.2, 3.3),
            10,
            15.5f,
            20.0f,
            true
        );
        String formatted = PiggyTelemetryFormatter.formatTrajectory(prediction, null);
        assertTrue(formatted.contains("[Trajectory|"));
        assertTrue(formatted.contains("Fatal:true"));
        assertTrue(formatted.contains("ImpactTicks:10"));
        assertTrue(formatted.contains("ImpactPos:(1, 2, 3)"));
    }
}
