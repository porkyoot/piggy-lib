package is.pig.minecraft.lib.util.telemetry;

import is.pig.minecraft.lib.util.PiggyLog;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

/**
 * Handles persistent output of failed telemetry sessions.
 */
public class FileOutputEngine {
    private static final PiggyLog LOGGER = new PiggyLog("piggy-lib", "TelemetryOutput");
    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    public static void dump(MetaActionSession session) {
        // Construct the log directory in run/logs/piggy/
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            LOGGER.error("Minecraft instance is null. Skipping telemetry dump.");
            return;
        }
        File gameDir = mc.gameDirectory;
        File logsDir = new File(gameDir, "piggy_lib_dumps");
        
        if (!logsDir.exists() && !logsDir.mkdirs()) {
            LOGGER.error("Failed to create telemetry logs directory: {}", logsDir.getAbsolutePath());
            return;
        }

        String timestamp = LocalDateTime.now().format(FILE_DATE_FORMAT);
        String fileName = String.format("piggy_debug_%s_%s.log", session.getName().replace(" ", "_"), timestamp);
        File logFile = new File(logsDir, fileName);

        try (PrintWriter writer = new PrintWriter(new FileWriter(logFile))) {
            writer.println("=== Piggy Meta Action Telemetry Dump ===");
            writer.println("Session: " + session.getName());
            writer.println("ID: " + session.getSessionId());
            writer.println("Status: " + session.getStatus());
            writer.println("Failure Reason: " + (session.getFailureReason() != null ? session.getFailureReason() : "Unknown"));
            writer.println("Priority: " + session.isPriority());
            writer.println();
            writer.println(is.pig.minecraft.lib.util.perf.PerfMonitor.getInstance().getFormattedGlobalState());
            writer.println("=========================================");
            writer.write("--- LOG START ---\n");
            for (TelemetryEntry entry : session.getEntries()) {
                writer.write(entry.formatted() + "\n");
            }
            writer.write("--- LOG END ---\n");
            writer.println("=== End of Dump ===");
            LOGGER.info("Telemetry dumped to: {}", logFile.getName());
            sendChatNotification(session, logFile);
        } catch (IOException e) {
            LOGGER.error("Failed to write telemetry dump for session " + session.getName(), e);
        }
    }

    private static void sendChatNotification(MetaActionSession session, File logFile) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        String sessionName = session.getName().replace("_", " ");
        String outcome = session.getFailureReason() != null ? session.getFailureReason() : "unknown error";
        String logPath = logFile.getAbsolutePath();

        String informativeMessage = String.format("Piggy Lib: %s failed. Reason: %s. A detailed forensic report was generated for analysis. ",
            sessionName, outcome);

        LOGGER.info(informativeMessage + "View the log at: {}", logPath);

        Component msg = Component.literal("Piggy Lib: ")
            .withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true))
            .append(Component.literal(sessionName + " failed. Reason: " + outcome + ". ")
                .withStyle(ChatFormatting.RED))
            .append(Component.literal(logPath)
                .withStyle(Style.EMPTY
                    .withColor(ChatFormatting.AQUA)
                    .withUnderlined(true)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, logPath))));

        client.player.sendSystemMessage(msg);
    }
}
