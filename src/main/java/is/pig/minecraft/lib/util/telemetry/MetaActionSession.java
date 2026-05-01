package is.pig.minecraft.lib.util.telemetry;
import is.pig.minecraft.api.*;

import is.pig.minecraft.lib.config.PiggyClientConfig;
import org.slf4j.event.Level;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Captures detailed telemetry during a meta-action (e.g., MLG, Sorting).
 * Uses a rolling memory buffer to minimize I/O and memory overhead.
 */
public class MetaActionSession {
    private final UUID sessionId;
    private final String name;
    private final long startTime;
    private final Deque<TelemetryEntry> buffer = new ArrayDeque<>(5000);
    private SessionStatus status = SessionStatus.ACTIVE;
    private String failureReason = null;
    private boolean priority = false;
    private long lastTickCount = 0;
    private int monitorTicks = 0;
    private final List<LogEnricher> enrichers = new ArrayList<>();

    public MetaActionSession(String name) {
        this.sessionId = UUID.randomUUID();
        this.name = name;
        this.startTime = System.currentTimeMillis();
        this.enrichers.add(new StandardEnvironmentEnricher());
    }

    /**
     * Adds a custom enricher to this session.
     * @param enricher the enricher to add
     */
    public void addEnricher(LogEnricher enricher) {
        this.enrichers.add(enricher);
    }

    /**
     * Records a message into the rolling buffer with full telemetry verbosity.
     */
    public synchronized void log(Level level, String message) {
        log(level, message, null);
    }

    private synchronized void log(Level level, String message, String narrative) {
        if (status != SessionStatus.ACTIVE && status != SessionStatus.MONITORING) return;

        long currentTick = getCurrentTick();
        Map<String, Object> data = new HashMap<>();
        enrichers.forEach(e -> e.enrich(data));

        buffer.add(new LogEntry(
            System.currentTimeMillis(), 
            currentTick, 
            level, 
            (double) data.getOrDefault("tps", 20.0), 
            (double) data.getOrDefault("mspt", 50.0), 
            (double) data.getOrDefault("cps", 0.0), 
            (String) data.getOrDefault("pos", "n/a"), 
            message,
            narrative));
        
        if (buffer.size() > PiggyClientConfig.getInstance().getMaxLogBufferSize()) {
            buffer.removeFirst();
        }
    }

    /**
     * Records a structured micro-action into the rolling buffer with full telemetry verbosity.
     */
    public synchronized void logAction(String why, String how, String outcome) {
        if (status != SessionStatus.ACTIVE && status != SessionStatus.MONITORING) return;

        long currentTick = getCurrentTick();
        Map<String, Object> data = new HashMap<>();
        enrichers.forEach(e -> e.enrich(data));

        buffer.add(new ActionAnatomyEntry(
            System.currentTimeMillis(), 
            currentTick, 
            Level.INFO, 
            (double) data.getOrDefault("tps", 20.0), 
            (double) data.getOrDefault("mspt", 50.0), 
            (double) data.getOrDefault("cps", 0.0), 
            (String) data.getOrDefault("pos", "n/a"), 
            why, 
            how, 
            outcome));

        if (buffer.size() > PiggyClientConfig.getInstance().getMaxLogBufferSize()) {
            buffer.removeFirst();
        }
    }

    private long getCurrentTick() {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc != null && mc.level != null) {
            return mc.level.getGameTime();
        }
        return lastTickCount;
    }

    public synchronized void setLastTick(long tick) {
        this.lastTickCount = tick;
    }

    public void info(String message) { log(Level.INFO, message); }
    public void warn(String message) { log(Level.WARN, message); }
    public void error(String message) { log(Level.ERROR, message); }
    public void debug(String message) { log(Level.DEBUG, message); }

    /**
     * Humanizes and records a structured event into the session buffer after global enrichment.
     * @param event the structured event to log
     */
    public synchronized void logEvent(StructuredEvent event) {
        StructuredEventDispatcher.getInstance().dispatch(event);
    }
    
    /**
     * Records an enriched structured event into the session buffer.
     * @param view the enriched event view from the dispatcher
     */
    public synchronized void logEnrichedEvent(is.pig.minecraft.api.EnrichedEventView view) {
        StructuredEvent event = view.parent();
        String technical = event.getEventKey() + " " + view.enrichedData();
        String narrative = is.pig.minecraft.lib.util.telemetry.formatter.PiggyTelemetryFormatter.formatNarrative(event);
        this.log(event.level(), technical, narrative);
    }
    
    /**
     * Moves the session into a monitoring state for a specified number of ticks.
     * If the player dies during this period, the session will be failed.
     */
    public synchronized void monitor(int ticks) {
        if (this.status == SessionStatus.ACTIVE) {
            this.status = SessionStatus.MONITORING;
            this.monitorTicks = ticks;
            log(Level.INFO, "Session moved to MONITORING for " + ticks + " ticks");
            MetaActionSessionManager.getInstance().registerMonitoring(this);
        }
    }

    public synchronized void tickMonitor() {
        if (this.status == SessionStatus.MONITORING) {
            this.monitorTicks--;
            if (this.monitorTicks <= 0) {
                succeed();
            }
        }
    }

    /**
     * Marks the session as successful. Telemetry will be discarded.
     */
    public synchronized void succeed() {
        if (this.status == SessionStatus.ACTIVE || this.status == SessionStatus.MONITORING) {
            this.status = SessionStatus.SUCCEEDED;
            discard();
        }
    }

    /**
     * Marks the session as failed and triggers a dump to disk.
     */
    public synchronized void fail(String reason) {
        if (this.status == SessionStatus.ACTIVE || this.status == SessionStatus.MONITORING) {
            this.status = SessionStatus.FAILED;
            this.failureReason = reason;
            log(Level.ERROR, "Session failed: " + reason);
            dump();
        }
    }

    public synchronized void discard() {
        buffer.clear();
        MetaActionSessionManager.getInstance().endSession(this);
    }

    private void dump() {
        FileOutputEngine.dump(this);
        MetaActionSessionManager.getInstance().endSession(this);
    }

    // Getters
    public UUID getSessionId() { return sessionId; }
    public String getName() { return name; }
    public long getStartTime() { return startTime; }
    public List<TelemetryEntry> getEntries() { return Collections.unmodifiableList(new ArrayList<>(buffer)); }
    public SessionStatus getStatus() { return status; }
    public String getFailureReason() { return failureReason; }
    
    public boolean isPriority() { return priority; }
    public void setPriority(boolean priority) { this.priority = priority; }
}
