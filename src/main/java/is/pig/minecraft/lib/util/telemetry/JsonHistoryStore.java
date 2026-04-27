package is.pig.minecraft.lib.util.telemetry;

import com.google.gson.reflect.TypeToken;
import is.pig.minecraft.lib.util.telemetry.formatter.PiggyTelemetryFormatter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/**
 * A reusable component for persisting structured events to specialized JSON history files.
 * Extends the generic AbstractHistoryStore.
 */
public class JsonHistoryStore extends AbstractHistoryStore<JsonHistoryStore.HistoryEntryRecord> {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public JsonHistoryStore(String fileName, Predicate<StructuredEvent> filter) {
        super(fileName, filter, new TypeToken<CopyOnWriteArrayList<HistoryEntryRecord>>(){}.getType());
    }

    @Override
    protected HistoryEntryRecord mapEvent(StructuredEventDispatcher.EnrichedEventView view) {
        StructuredEvent event = view.parent();
        return new HistoryEntryRecord(
            LocalDateTime.now().format(TIME_FORMATTER),
            event.getEventKey(),
            PiggyTelemetryFormatter.formatNarrative(event),
            view.enrichedData()
        );
    }

    /**
     * A record representing a single horizontal entry in the JSON history.
     */
    public record HistoryEntryRecord(
        String timestamp,
        String eventKey,
        String narrative,
        java.util.Map<String, Object> data
    ) {}
}
