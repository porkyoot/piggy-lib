package is.pig.minecraft.lib.util.telemetry;

import is.pig.minecraft.lib.I18n;
import is.pig.minecraft.lib.util.PiggyLog;
import is.pig.minecraft.lib.util.telemetry.formatter.PiggyTelemetryFormatter;
import org.junit.jupiter.api.Test;
import org.slf4j.event.Level;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class StructuredEventTest {

    private static class TestEvent implements StructuredEvent {
        @Override public String getEventKey() { return "test.event"; }
        @Override public Map<String, Object> getEventData() { return Map.of("foo", "bar"); }
        @Override public long timestamp() { return 0; }
        @Override public long tick() { return 0; }
        @Override public Level level() { return Level.INFO; }
        @Override public double tps() { return 20.0; }
        @Override public double mspt() { return 10.0; }
        @Override public double cps() { return 0.0; }
        @Override public String pos() { return "0,0,0"; }
        @Override public String formatted() { return "TestEvent"; }
    }

    @Test
    void testRegistryAndTranslation() {
        EventTranslatorRegistry registry = EventTranslatorRegistry.getInstance();
        
        // Register a translator
        registry.register(TestEvent.class, (event, i18n) -> {
            return "Event " + event.getEventKey() + " with data " + event.getEventData().get("foo");
        });

        TestEvent event = new TestEvent();
        var translator = registry.getTranslator(TestEvent.class);
        
        assertNotNull(translator);
        String result = translator.apply(event, I18n.getInstance());
        assertEquals("Event test.event with data bar", result);
    }

    @Test
    void testPiggyLogIntegration() {
        PiggyLog log = new PiggyLog("test");
        TestEvent event = new TestEvent();
        
        // Register event translator
        EventTranslatorRegistry.getInstance().register(TestEvent.class, (e, i) -> "Humanized " + e.getEventData().get("foo"));
        log.logEvent(event);
    }

    @Test
    void testNarrativeEngineAndDualView() {
        EventTranslatorRegistry.getInstance().register(TestEvent.class, (e, i) -> "Hello Narrative");
        TestEvent event = new TestEvent();
        
        String narrative = PiggyTelemetryFormatter.formatNarrative(event);
        assertEquals("[🔹] Hello Narrative", narrative);
        
        LogEntry entry = new LogEntry(
            123L, 456L, Level.INFO, 20.0, 10.0, 0.0, "0,0,0",
            "Technical Details",
            narrative
        );
        
        String formatted = entry.formatted();
        assertTrue(formatted.contains("Technical Details | Hello Narrative"), "Formatted log should contain both technical and narrative parts separated by |");
        assertTrue(formatted.contains("[123] [Tick:456]"), "Formatted log should contain timestamp and tick");
    }
}
