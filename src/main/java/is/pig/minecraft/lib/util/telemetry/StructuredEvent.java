package is.pig.minecraft.lib.util.telemetry;

import java.util.Map;

/**
 * An interface for telemetry entries that have a machine-readable key and structured data.
 * Structured events are designed to be humanized via the {@link EventTranslatorRegistry}.
 */
public interface StructuredEvent extends TelemetryEntry {

    /**
     * Gets the unique event key for this event type.
     * This key is typically used for I18n lookup (e.g., "event.piggy.item_action").
     *
     * @return the event key
     */
    String getEventKey();

    /**
     * Gets a map of key-value pairs representing the structured data of this event.
     * This data is used by translators to build human-readable messages.
     *
     * @return the event data map
     */
    Map<String, Object> getEventData();

    /**
     * Gets an icon (typically a Unicode emoji) representing the category of this event.
     * This icon is prepended to the narrative in the Narrative Engine.
     *
     * @return the category icon string
     */
    default String getCategoryIcon() {
        return "🔹";
    }

    /**
     * Indicates if this event is notable and should be reported as a high-signal admin alert.
     * Notable events typically involve malicious intent (Arson, Threats, Moderation).
     *
     * @return true if notable, false otherwise
     */
    default boolean isNotable() {
        return false;
    }

    /**
     * Indicates if this event represents a system failure or an unsuccessful meta-action.
     * Failed actions (MLG failures, Sorting errors) are reported to the user for awareness.
     *
     * @return true if failure, false otherwise
     */
    default boolean isFailure() {
        return false;
    }
}
