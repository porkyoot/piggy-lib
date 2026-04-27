package is.pig.minecraft.lib.util.telemetry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/**
 * Generic base class for event persistence.
 * Handles loading and saving of a list of type T to a JSON file.
 *
 * @param <T> The type of entries stored.
 */
public abstract class AbstractHistoryStore<T> {
    protected static final Logger LOGGER = LoggerFactory.getLogger("PiggyHistory");
    
    private final File historyFile;
    private final Predicate<StructuredEvent> filter;
    private final Type listType;
    private final Gson gson;
    
    protected List<T> history = new CopyOnWriteArrayList<>();

    protected AbstractHistoryStore(String fileName, Predicate<StructuredEvent> filter, Type listType) {
        File configDir = FabricLoader.getInstance().getConfigDir().resolve("piggy").resolve("history").toFile();
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        
        this.historyFile = new File(configDir, fileName);
        this.filter = filter;
        this.listType = listType;
        this.gson = createGsonBuilder().setPrettyPrinting().create();
        load();
    }

    /**
     * Allows subclasses to customize the GsonBuilder.
     */
    protected GsonBuilder createGsonBuilder() {
        return new GsonBuilder();
    }

    public boolean test(StructuredEvent event) {
        return filter.test(event);
    }

    public void register(String key) {
        PiggyEventDispatcher.getInstance().registerStore(key, this);
    }


    public void onEvent(StructuredEventDispatcher.EnrichedEventView view) {
        if (test(view.parent())) {
            T entry = mapEvent(view);
            if (entry != null) {
                addEntry(entry);
            }
        }
    }

    /**
     * Maps an EnrichedEventView to the stored type T.
     */
    protected abstract T mapEvent(StructuredEventDispatcher.EnrichedEventView view);


    protected synchronized void addEntry(T entry) {
        history.add(entry);
        save();
    }

    private void load() {
        if (historyFile.exists()) {
            try (FileReader reader = new FileReader(historyFile)) {
                List<T> loaded = gson.fromJson(reader, listType);
                if (loaded != null) {
                    history = new CopyOnWriteArrayList<>(loaded);
                }
            } catch (Exception e) {
                LOGGER.error("Failed to load history from " + historyFile, e);
            }
        }
    }

    private synchronized void save() {
        File tempFile = new File(historyFile.getParentFile(), historyFile.getName() + ".tmp");
        try (FileWriter writer = new FileWriter(tempFile)) {
            gson.toJson(history, writer);
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

    public List<T> getHistory() {
        return Collections.unmodifiableList(history);
    }

    public synchronized void clear() {
        history.clear();
        save();
    }
}
