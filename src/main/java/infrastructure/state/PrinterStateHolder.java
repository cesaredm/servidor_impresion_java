package infrastructure.state;

import infrastructure.config.PrinterConfigProperties;
import domain.entities.PrinterConfig;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * FUENTE DE VERDAD - MEMORIA (CACHÉ).
 *
 * Enum singleton que mantiene la configuración de impresoras en memoria
 * usando un ConcurrentHashMap para acceso thread-safe.
 *
 * Rol en la arquitectura:
 * - Actúa como observador (PrinterChangeListener) de PrinterConfigProperties
 * - Cuando PrinterConfigProperties.loadProperties() notifica cambios,
 *   este holder actualiza su ConcurrentHashMap interno
 * - Es la fuente de acceso rápido para consultas (findByName, findByIp, findAll)
 * - Sincronizado con el archivo físico a través del patrón Observer
 *
 * Thread-safety: ConcurrentHashMap permite operaciones concurrentes seguras
 * sin bloqueos globales.
 *
 * @see infrastructure.config.PrinterConfigProperties
 * @see infrastructure.state.PrinterChangeListener
 */
public enum PrinterStateHolder implements PrinterChangeListener {
    INSTANCE;

    private final ConcurrentMap<String, PrinterConfig> printers = new ConcurrentHashMap<>();

    PrinterStateHolder() {
        PrinterConfigProperties.getInstance().addListener(this);
    }

    @Override
    public void onPrintersReloaded() {
        printers.clear();
        printers.putAll(PrinterConfigProperties.getInstance().getAllPrinterConfigs());
    }

    public void init() {
        onPrintersReloaded();
    }

    public PrinterConfig getPrinter(String name) {
        return printers.get(name);
    }

    public Collection<PrinterConfig> getAllPrinters() {
        return printers.values();
    }

    public ConcurrentMap<String, PrinterConfig> getPrintersMap() {
        return printers;
    }
}
