package is.pig.minecraft.lib.util.telemetry;

import is.pig.minecraft.lib.config.PiggyClientConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.event.Level;

import static org.junit.jupiter.api.Assertions.*;

public class TelemetryTest {
    private static class DummyConfig extends PiggyClientConfig<DummyConfig> {
        @Override public void save() {}
    }

    @BeforeEach
    void setup() throws Exception {
        java.lang.reflect.Method setInstance = PiggyClientConfig.class.getDeclaredMethod("setGlobalInstance", PiggyClientConfig.class);
        setInstance.setAccessible(true);
        DummyConfig config = new DummyConfig();
        config.setMaxLogBufferSize(100);
        setInstance.invoke(null, config);
    }

    @Test
    void testRollingBuffer() {
        MetaActionSession session = new MetaActionSession("TestSession");
        
        // Default max buffer is 100
        for (int i = 0; i < 150; i++) {
            session.info("Entry " + i);
        }
        
        assertEquals(100, session.getEntries().size(), "Buffer size should be capped at 100");
        TelemetryEntry entry = session.getEntries().get(0);
        assertTrue(entry instanceof LogEntry);
        assertEquals("Entry 50", ((LogEntry) entry).message(), "Oldest entries should be dropped");
    }

    @Test
    void testSuccessLifecycle() {
        MetaActionSession session = new MetaActionSession("TestSession");
        session.info("Test entry");
        
        session.succeed();
        
        assertEquals(SessionStatus.SUCCEEDED, session.getStatus());
        assertTrue(session.getEntries().isEmpty(), "Buffer should be cleared on success");
    }

    @Test
    void testFailureLifecycle() {
        MetaActionSession session = new MetaActionSession("TestSession");
        session.info("Test entry");
        
        // We can't easily test FileOutputEngine.dump() here because it depends on Minecraft.
        // But we can test the status change.
        session.fail("Test failure");
        
        assertEquals(SessionStatus.FAILED, session.getStatus());
        assertEquals("Test failure", session.getFailureReason());
    }
}
