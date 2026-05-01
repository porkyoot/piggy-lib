package is.pig.minecraft.lib.util.telemetry;
import is.pig.minecraft.api.*;

import is.pig.minecraft.lib.I18n;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Registry mapping physical {@link StructuredEvent} types to humanizing functions.
 * Translators convert structured data into readable log messages using {@link I18n}.
 */
@Deprecated(forRemoval = true)
public final class EventTranslatorRegistry {

    private static final EventTranslatorRegistry INSTANCE = new EventTranslatorRegistry();

    private final Map<Class<? extends StructuredEvent>, BiFunction<StructuredEvent, I18n, String>> translators = new HashMap<>();

    private EventTranslatorRegistry() {}

    /**
     * @return the singleton instance of the registry.
     */
    public static EventTranslatorRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Registers a new translator for a specific event class.
     *
     * @param eventClass the class of the structured event
     * @param translator the function to humanize the event
     * @param <T> the type of {@link StructuredEvent}
     */
    public <T extends StructuredEvent> void register(Class<T> eventClass, BiFunction<T, I18n, String> translator) {
        // Safe cast: T is a subtype of StructuredEvent, and BiFunction is being used as such.
        // The translator expects T, which StructuredEvent will be when this class matches.
        translators.put(eventClass, (event, i18n) -> translator.apply(eventClass.cast(event), i18n));
    }

    /**
     * Retrieves the translator for a given event class.
     *
     * @param eventClass the class of the structured event
     * @return the bi-function to humanize the event, or null if not registered
     */
    public BiFunction<StructuredEvent, I18n, String> getTranslator(Class<? extends StructuredEvent> eventClass) {
        return translators.get(eventClass);
    }
}
