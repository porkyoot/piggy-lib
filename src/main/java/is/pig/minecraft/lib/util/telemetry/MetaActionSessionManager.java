package is.pig.minecraft.lib.util.telemetry;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.client.Minecraft;

/**
 * Manages active MetaActionSessions.
 */
public class MetaActionSessionManager {
    private static final MetaActionSessionManager INSTANCE = new MetaActionSessionManager();
    
    // For now, we support one active "Global" session per thread or just one globally if appropriate.
    // Given the single-threaded nature of Minecraft client ticks, a single atomic reference is often enough.
    private final AtomicReference<MetaActionSession> currentSession = new AtomicReference<>(null);
    private final ConcurrentHashMap<String, MetaActionSession> namedSessions = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<MetaActionSession> monitoringSessions = new CopyOnWriteArrayList<>();

    private MetaActionSessionManager() {}

    public static MetaActionSessionManager getInstance() {
        return INSTANCE;
    }

    /**
     * Starts a new session and sets it as the current global session.
     */
    public MetaActionSession startSession(String name) {
        MetaActionSession session = new MetaActionSession(name);
        currentSession.set(session);
        namedSessions.put(name, session);
        return session;
    }

    public Optional<MetaActionSession> getCurrentSession() {
        return Optional.ofNullable(currentSession.get());
    }

    public Optional<MetaActionSession> getSession(String name) {
        return Optional.ofNullable(namedSessions.get(name));
    }

    protected void endSession(MetaActionSession session) {
        currentSession.compareAndSet(session, null);
        namedSessions.remove(session.getName(), session);
        monitoringSessions.remove(session);
    }
    
    public void registerMonitoring(MetaActionSession session) {
        if (!monitoringSessions.contains(session)) {
            monitoringSessions.add(session);
        }
    }

    public void tick(Minecraft client) {
        if (client.player == null) return;

        boolean died = client.player.isDeadOrDying() || client.player.getHealth() <= 0;
        
        for (MetaActionSession session : monitoringSessions) {
            if (died) {
                String forensics = String.format("Died@%s | Health:%.1f | LastPos:%s", 
                    is.pig.minecraft.lib.util.telemetry.formatter.FormatterUtils.formatVec3(client.player.position()),
                    client.player.getHealth(),
                    is.pig.minecraft.lib.util.telemetry.formatter.FormatterUtils.formatVec3(client.player.position()));
                session.fail("Delayed Death Forensics: " + forensics);
            } else {
                session.tickMonitor();
            }
        }
    }
    
    /**
     * Logs to the current active session if one exists.
     */
    public void log(org.slf4j.event.Level level, String message) {
        getCurrentSession().ifPresent(s -> s.log(level, message));
    }
}
