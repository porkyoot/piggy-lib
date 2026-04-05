package is.pig.minecraft.lib.action.telemetry;

import is.pig.minecraft.lib.action.IAction;
import is.pig.minecraft.lib.config.PiggyClientConfig;
import is.pig.minecraft.lib.util.PiggyLog;
import is.pig.minecraft.lib.util.telemetry.MetaActionSessionManager;
import net.minecraft.client.Minecraft;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Continuous forensic logger for all Piggy Mod actions.
 * Dumps every action lifecycle event and result into logs/piggy/piggy_actions.log
 * when enabled in the config.
 */
public class ActionForensics {
    private static final ActionForensics INSTANCE = new ActionForensics();
    private static final PiggyLog LOGGER = new PiggyLog("piggy-lib", "ActionForensics");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private BufferedWriter writer;
    private File currentLogFile;
    private boolean lastStateWasEnabled = false;

    private ActionForensics() {}

    public static ActionForensics getInstance() {
        return INSTANCE;
    }

    /**
     * Logs an action event.
     * @param event The category of the event (e.g., "ENQUEUED", "EXECUTING", "VERIFIED").
     * @param action The action being logged.
     * @param details Additional context or "consequences" of the action.
     */
    public synchronized void log(String event, String source, String name, String details) {
        boolean isEnabled = PiggyClientConfig.getInstance() != null && 
                            PiggyClientConfig.getInstance().isFullActionDebug();

        if (!isEnabled && MetaActionSessionManager.getInstance().getCurrentSession().isEmpty()) {
            if (lastStateWasEnabled) {
                closeWriter();
                lastStateWasEnabled = false;
            }
            return;
        }

        lastStateWasEnabled = true;
        ensureWriterOpen();

        if (writer != null) {
            try {
                String timestamp = LocalDateTime.now().format(TIME_FORMAT);
                
                // Telemetry enrichment
                var perf = is.pig.minecraft.lib.util.perf.PerfMonitor.getInstance();
                double tps = perf.getServerTps();
                double mspt = perf.getClientMspt();
                double cps = perf.getCps();
                
                String posStr = "n/a";
                long tick = -1;
                Minecraft mc = Minecraft.getInstance();
                if (mc != null && mc.player != null) {
                    var p = mc.player;
                    posStr = String.format("(%.1f, %.1f, %.1f)", p.getX(), p.getY(), p.getZ());
                    if (mc.level != null) {
                        tick = mc.level.getGameTime();
                    }
                }

                String telemetryStr = String.format("Tick:%d TPS:%.1f MSPT:%.1f CPS:%.1f Pos:%s", tick, tps, mspt, cps, posStr);
                
                String line = String.format("[%s] [%-10s] [%-15s] %-25s | %-75s | %s", 
                    timestamp, 
                    event, 
                    source, 
                    name, 
                    telemetryStr,
                    details != null ? details : "");
                writer.write(line);
                writer.newLine();
                writer.flush(); // Ensure real-time flushing
            } catch (IOException e) {
                LOGGER.error("Failed to write to action forensic log", e);
            }
        }
    }

    public synchronized void log(String event, IAction action, String details) {
        Minecraft mc = Minecraft.getInstance();
        String actionTelemetry = action.getTelemetry(mc);
        String finalDetails = (details != null ? details : "") + (actionTelemetry != null ? " | " + actionTelemetry : "");
        log(event, action.getSourceMod(), action.getName(), finalDetails);
    }

    private void ensureWriterOpen() {
        if (writer != null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.gameDirectory == null) return;

        File logsDir = new File(mc.gameDirectory, "logs/piggy");
        if (!logsDir.exists() && !logsDir.mkdirs()) {
            LOGGER.error("Failed to create action forensics directory: {}", logsDir.getAbsolutePath());
            return;
        }

        currentLogFile = new File(logsDir, "piggy_actions.log");
        try {
            // Append mode ensure we don't wipe previous logs in the same session if someone toggles it
            writer = new BufferedWriter(new FileWriter(currentLogFile, true));
            String startMsg = "--- ACTION DEBUG SESSION STARTED: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + " ---";
            writer.write("\n" + startMsg + "\n");
            writer.flush();
            LOGGER.info("[ActionForensics] Recording debug actions to: {}", currentLogFile.getAbsolutePath());
        } catch (IOException e) {
            LOGGER.error("Failed to open action forensic log file", e);
        }
    }

    private void closeWriter() {
        if (writer != null) {
            try {
                writer.write("--- ACTION DEBUG SESSION PAUSED ---\n");
                writer.close();
            } catch (IOException ignored) {}
            writer = null;
        }
    }
}
