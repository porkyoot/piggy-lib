package is.pig.minecraft.lib.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.function.BiConsumer;

/**
 * Universal, generic configuration manager for Piggy Mods.
 * Handles loading and saving of configuration instances using GSON.
 *
 * @param <T> The type of the configuration class.
 */
public abstract class PiggyConfigManager<T> {
    private final Logger logger;
    private final File configFile;
    private final Class<T> configClass;
    private final Gson gson;

    protected PiggyConfigManager(String fileName, Class<T> configClass, String loggerName) {
        this(fileName, configClass, loggerName, null);
    }

    protected PiggyConfigManager(String fileName, Class<T> configClass, String loggerName, GsonBuilder customBuilder) {
        this.logger = LoggerFactory.getLogger(loggerName);
        this.configFile = FabricLoader.getInstance().getConfigDir().resolve(fileName).toFile();
        this.configClass = configClass;
        
        GsonBuilder builder = customBuilder != null ? customBuilder : new GsonBuilder();
        builder.setPrettyPrinting()
                .registerTypeAdapter(Color.class, new ColorTypeAdapter())
                .registerTypeHierarchyAdapter(BiConsumer.class, new TypeAdapter<BiConsumer<?, ?>>() {
                    @Override
                    public void write(JsonWriter out, BiConsumer<?, ?> value) throws IOException {
                        out.beginObject().endObject();
                    }

                    @Override
                    public BiConsumer<?, ?> read(JsonReader in) throws IOException {
                        in.skipValue();
                        return null;
                    }
                })
                .setExclusionStrategies(new com.google.gson.ExclusionStrategy() {
                    @Override
                    public boolean shouldSkipField(com.google.gson.FieldAttributes f) {
                        return f.getName().equals("syncListeners");
                    }

                    @Override
                    public boolean shouldSkipClass(Class<?> clazz) {
                        return false;
                    }
                });
                
        this.gson = builder.create();
    }


    protected abstract T getConfigInstance();
    protected abstract void setConfigInstance(T instance);

    public void load() {
        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                T loaded = gson.fromJson(reader, configClass);
                if (loaded != null) {
                    setConfigInstance(loaded);
                    logger.info("Configuration loaded successfully: {}", configFile.getName());
                }
            } catch (com.google.gson.JsonSyntaxException | com.google.gson.JsonIOException e) {
                logger.error("Failed to parse configuration file: {}", configFile.getAbsolutePath(), e);
                throw new RuntimeException(
                        "Piggy Config Error: The configuration file '" + configFile.getName()
                                + "' is malformed. Please fix the syntax or delete the file to regenerate it. Details: "
                                + e.getMessage(),
                        e);
            } catch (IOException e) {
                logger.error("Failed to load configuration: {}", configFile.getName(), e);
            }
        } else {
            save(); // Create default
        }
    }

    public void save() {
        try (FileWriter writer = new FileWriter(configFile)) {
            gson.toJson(getConfigInstance(), writer);
        } catch (IOException e) {
            logger.error("Failed to save configuration: {}", configFile.getName(), e);
        }
    }

    private static class ColorTypeAdapter extends TypeAdapter<Color> {
        @Override
        public void write(JsonWriter out, Color value) throws IOException {
            out.beginObject();
            out.name("red").value(value.getRed());
            out.name("green").value(value.getGreen());
            out.name("blue").value(value.getBlue());
            out.name("alpha").value(value.getAlpha());
            out.endObject();
        }

        @Override
        public Color read(JsonReader in) throws IOException {
            in.beginObject();
            int r = 0, g = 0, b = 0, a = 255;
            while (in.hasNext()) {
                switch (in.nextName()) {
                    case "red" -> r = in.nextInt();
                    case "green" -> g = in.nextInt();
                    case "blue" -> b = in.nextInt();
                    case "alpha" -> a = in.nextInt();
                    default -> in.skipValue();
                }
            }
            in.endObject();
            return new Color(r, g, b, a);
        }
    }
}
