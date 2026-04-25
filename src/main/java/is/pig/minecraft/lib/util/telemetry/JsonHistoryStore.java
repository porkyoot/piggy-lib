package is.pig.minecraft.lib.util.telemetry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import is.pig.minecraft.lib.util.telemetry.formatter.PiggyTelemetryFormatter;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/**
 * A reusable component for persisting structured events to specialized JSON history files.
 */
public class JsonHistoryStore {
    private static final Logger LOGGER = LoggerFactory.getLogger("PiggyHistory");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final File historyFile;
    private final Predicate<StructuredEvent> filter;
    private List<HistoryEntryRecord> history = new CopyOnWriteArrayList<>();

    public JsonHistoryStore(String fileName, Predicate<StructuredEvent> filter) {
        File configDir = FabricLoader.getInstance().getConfigDir().resolve("piggy").resolve("history").toFile();
        if (!configDir.exists()) configDir.mkdirs();
        
        this.historyFile = new File(configDir, fileName);
        this.filter = filter;
        load();
    }

    /**
     * Registers this store as a listener to the global event dispatcher.
     */
    public void register() {
        StructuredEventDispatcher.getInstance().registerListener(this::onEvent);
    }

    private void onEvent(StructuredEventDispatcher.EnrichedEventView view) {
        if (filter.test(view.parent())) {
            addEntry(view);
        }
    }

    private synchronized void addEntry(StructuredEventDispatcher.EnrichedEventView view) {
        StructuredEvent event = view.parent();
        
        HistoryEntryRecord entry = new HistoryEntryRecord(
            LocalDateTime.now().format(TIME_FORMATTER),
            event.getEventKey(),
            PiggyTelemetryFormatter.formatNarrative(event),
            view.enrichedData()
        );
        
        history.add(entry);
        save();
    }

    private void load() {
        if (historyFile.exists()) {
            try (FileReader reader = new FileReader(historyFile)) {
                Type listType = new TypeToken<CopyOnWriteArrayList<HistoryEntryRecord>>(){}.getType();
                List<HistoryEntryRecord> loaded = GSON.fromJson(reader, listType);
                if (loaded != null) history = new CopyOnWriteArrayList<>(loaded);
            } catch (Exception e) {
                LOGGER.error("Failed to load history from " + historyFile, e);
            }
        }
    }

    private synchronized void save() {
        File tempFile = new File(historyFile.getParentFile(), historyFile.getName() + ".tmp");
        try (FileWriter writer = new FileWriter(tempFile)) {
            GSON.toJson(history, writer);
            writer.flush();
            writer.close();
            
            if (historyFile.exists() && !historyFile.delete()) {
                LOGGER.warn("Failed to delete old history file: " + historyFile);
            }
            
            if (!tempFile.renameTo(historyFile)) {
                LOGGER.error("Failed to rename temp history file to " + historyFile);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to save history to " + historyFile, e);
        }
    }

    public List<HistoryEntryRecord> getHistory() {
        return Collections.unmodifiableList(history);
    }

    public synchronized void clear() {
        history.clear();
        save();
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
