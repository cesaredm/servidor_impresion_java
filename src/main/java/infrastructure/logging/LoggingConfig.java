package infrastructure.logging;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Configura el logging de la aplicacion para que escriba en un archivo propio
 * con rotacion, sin depender de la consola ni de los pipes de procrun.
 *
 * <p>Como el servicio corre en {@code StartMode=exe}, procrun no recibe el
 * stdout/stderr del proceso hijo (Launch4j), por eso los
 * {@code stdOutput/stdError.log} quedan vacios. Este FileHandler escribe
 * directo a disco: {@code <logdir>\printserver.%g.log}.</p>
 *
 * <p>Directorio (en orden de prioridad):
 * {@code -Dprintserver.logdir}, env {@code PRINTSERVER_LOG_DIR},
 * {@code <user.dir>\log} (en produccion es {@code {app}\log}),
 * con fallback a la carpeta de datos de cada SO (ver AppDirectories).</p>
 */
public final class LoggingConfig {

    private static final String LOG_FILE_PATTERN = "printserver.%g.log";
    private static final int FILE_LIMIT_BYTES = 2 * 1024 * 1024;
    private static final int FILE_COUNT = 5;

    private static volatile boolean configured = false;

    private LoggingConfig() {
    }

    public static synchronized void setup() {
        if (configured) {
            return;
        }
        configured = true;
        Level level = resolveLevel();
        try {
            Path logDir = resolveLogDir();
            Files.createDirectories(logDir);
            FileHandler fileHandler = new FileHandler(
                    logDir.resolve(LOG_FILE_PATTERN).toString(),
                    FILE_LIMIT_BYTES, FILE_COUNT, true);
            fileHandler.setFormatter(new SimpleFormatter());
            fileHandler.setLevel(level);

            Logger root = Logger.getLogger("");
            root.setLevel(level);
            for (Handler handler : root.getHandlers()) {
                handler.setLevel(level);
            }
            // Se conserva el ConsoleHandler para no cambiar el flujo en dev (NetBeans).
            root.addHandler(fileHandler);
            Logger.getLogger(LoggingConfig.class.getName())
                    .log(Level.INFO, "Log en archivo: {0} (nivel {1})",
                            new Object[]{logDir.resolve("printserver.0.log"), level});
        } catch (IOException | SecurityException e) {
            Logger.getLogger(LoggingConfig.class.getName())
                    .log(Level.WARNING, "No se pudo crear el log en archivo, se sigue solo con consola: {0}",
                            e.getMessage());
        }
    }

    private static Path resolveLogDir() {
        String dir = System.getProperty("printserver.logdir");
        if (dir == null || dir.isBlank()) {
            dir = System.getenv("PRINTSERVER_LOG_DIR");
        }
        if (dir == null || dir.isBlank()) {
            dir = System.getProperty("user.dir") + java.io.File.separator + "log";
        }
        Path path = Paths.get(dir);
        if (!Files.isDirectory(path)) {
            try {
                Files.createDirectories(path);
            } catch (IOException | SecurityException e) {
                path = infrastructure.config.AppDirectories.fallbackLogDir();
            }
        }
        return path;
    }

    private static Level resolveLevel() {
        String level = System.getProperty("printserver.loglevel");
        if (level == null || level.isBlank()) {
            level = System.getenv("PRINTSERVER_LOG_LEVEL");
        }
        if (level != null && !level.isBlank()) {
            try {
                return Level.parse(level.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                // Nivel invalido: se usa INFO por defecto.
            }
        }
        return Level.INFO;
    }
}
