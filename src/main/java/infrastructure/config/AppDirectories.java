package infrastructure.config;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Resuelve las rutas de datos de la aplicacion segun el sistema operativo.
 *
 * <p>Antes estas rutas estaban fijas a Windows ({@code C:\impresorasConfig}).
 * Ahora se resuelven por SO para que el mismo jar corra en
 * Windows, Linux y macOS:</p>
 * <ul>
 *   <li>Windows: {@code C:\impresorasConfig} (sin cambios, caso 90%)</li>
 *   <li>Linux: {@code /etc/printserver} (convencion para servicios del sistema)</li>
 *   <li>macOS: {@code /Library/Application Support/PrintServer} (convencion Apple)</li>
 * </ul>
 *
 * <p>Override (tiene prioridad sobre todo, util para dev o rutas custom):<br>
 * {@code -Dprintserver.configdir=RUTA} o variable de entorno
 * {@code PRINTSERVER_CONFIG_DIR}.</p>
 */
public final class AppDirectories {

    private AppDirectories() {
    }

    /** Carpeta base de datos (equivale al historico {@code impresorasConfig}). */
    public static Path configDir() {
        String override = System.getProperty("printserver.configdir");
        if (override == null || override.isBlank()) {
            override = System.getenv("PRINTSERVER_CONFIG_DIR");
        }
        if (override != null && !override.isBlank()) {
            return Paths.get(override.trim());
        }
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            return Paths.get("C:\\impresorasConfig");
        }
        if (os.contains("mac")) {
            return Paths.get("/Library/Application Support/PrintServer");
        }
        return Paths.get("/etc/printserver");
    }

    /** Archivo {@code printers.properties} (configuracion de impresoras). */
    public static Path configFile() {
        return configDir().resolve("printers.properties");
    }

    /** Logo local dentro de la carpeta de datos. */
    public static Path logoFile(String fileName) {
        return configDir().resolve(fileName);
    }

    /** Carpeta de logs de emergencia (cuando el log principal no se puede crear). */
    public static Path fallbackLogDir() {
        return configDir().resolve("logs");
    }
}
