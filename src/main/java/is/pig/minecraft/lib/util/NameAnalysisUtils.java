package is.pig.minecraft.lib.util;

public class NameAnalysisUtils {

    /**
     * Extracts the "Material" from a registry name (e.g., "acacia_boat" ->
     * "acacia").
     * Usually the prefix before the last underscore.
     */
    public static String extractMaterial(String registryName) {
        if (registryName == null || registryName.isEmpty())
            return "";

        // Remove namespace if present
        String path = registryName;
        if (registryName.contains(":")) {
            path = registryName.split(":")[1];
        }

        int lastUnderscore = path.lastIndexOf('_');
        if (lastUnderscore > 0) {
            return path.substring(0, lastUnderscore);
        }
        return path; // Fallback to full name if no underscore
    }

    /**
     * Extracts the "Type" from a registry name (e.g., "acacia_boat" -> "boat").
     * Usually the suffix after the last underscore.
     */
    public static String extractType(String registryName) {
        if (registryName == null || registryName.isEmpty())
            return "";

        // Remove namespace if present
        String path = registryName;
        if (registryName.contains(":")) {
            path = registryName.split(":")[1];
        }

        int lastUnderscore = path.lastIndexOf('_');
        if (lastUnderscore >= 0 && lastUnderscore < path.length() - 1) {
            return path.substring(lastUnderscore + 1);
        }
        return path; // Fallback to full name if no underscore
    }
}
