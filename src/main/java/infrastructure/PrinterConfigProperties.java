/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */ // Suponiendo que NETBEANS mantiene esto, pero el usuario pidió no eliminar código. 
// 2025-08-05: Migración a Singleton para unificar acceso a printers.properties.

package infrastructure;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import domain.entities.PrinterConfig;
import java.util.logging.Logger;

/**
 * Singleton responsable de la carga, lectura y escritura centralizada
 * del archivo de configuración printers.properties.
 * Gestiona el formato: [nombre].key = valor
 */ 
public class PrinterConfigProperties {

    private static final Logger LOGGER = Logger.getLogger(PrinterConfigProperties.class.getName());
    private static final String CONFIG_FILE = "C:\\impresorasConfig\\printers.properties";
    
    private static PrinterConfigProperties instance;
    private Properties properties;

    private PrinterConfigProperties() {
        loadProperties();
    }

    public static synchronized PrinterConfigProperties getInstance() {
        if (instance == null) {
            instance = new PrinterConfigProperties();
        }
        return instance;
    }

    /**
     * Carga las propiedades desde disco.
     * Si el archivo no existe, inicializa un Properties vacío.
     */
    public synchronized void loadProperties() {
        properties = new Properties();
        File file = new File(CONFIG_FILE);
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                properties.load(fis);
                LOGGER.log(Level.INFO, "Configuración cargada desde: " + CONFIG_FILE);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Error al cargar properties: " + e.getMessage());
            }
        } else {
            LOGGER.log(Level.WARNING, "Archivo de configuración no encontrado: " + CONFIG_FILE);
        }
    }

    /**
     * Guarda el estado actual de las propiedades a disco.
     */
    public synchronized void saveProperties() {
        File file = new File(CONFIG_FILE);
        try {
            file.getParentFile().mkdirs(); // Asegura que la carpeta exista
            try (FileOutputStream fos = new FileOutputStream(file)) {
                properties.store(fos, "Printers Configuration - Actualizado por PrinterConfigProperties");
                LOGGER.log(Level.INFO, "Configuración guardada en: " + CONFIG_FILE);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al guardar properties: " + e.getMessage());
        }
    }

    /**
     * Obtiene un valor específico para una impresora.
     * @param printerName Nombre de la impresora (ej: "cocina")
     * @param key Clave de la propiedad (ej: "ip", "port")
     * @return El valor de la propiedad, o null si no existe.
     */
    public String getProperty(String printerName, String key) {
        if (properties == null) return null;
        return properties.getProperty(printerName + "." + key);
    }

    /**
     * Establece un valor para una propiedad de una impresora.
     * @param printerName Nombre de la impresora.
     * @param key Clave de la propiedad.
     * @param value Valor a establecer.
     */
    public void setProperty(String printerName, String key, String value) {
        if (properties != null) {
            properties.setProperty(printerName + "." + key, value);
        }
    }

    /**
     * Elimina toda la información de una impresora.
     * @param printerName Nombre de la impresora a eliminar.
     */
    public void removePrinter(String printerName) {
        if (properties == null) return;
        Set<String> keysToRemove = new HashSet<>();
        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith(printerName + ".")) {
                keysToRemove.add(key);
            }
        }
        for (String key : keysToRemove) {
            properties.remove(key);
        }
    }

    /**
     * Obtiene los nombres de todas las impresoras configuradas.
     * @return Un Set con los nombres de las impresoras.
     */
		public Set<String> getAllPrinterNames() {
        Set<String> names = new HashSet<>();
        if (properties != null) {
            for (String key : properties.stringPropertyNames()) {
                int dotIndex = key.lastIndexOf('.');
                if (dotIndex > 0) {
                    names.add(key.substring(0, dotIndex));
                }
            }
        }
        return names;
    }
    
    /**
     * Obtiene todas las propiedades en bruto.
     * Útil para lecturas masivas o compatibilidad.
     */
    public Properties getRawProperties() {
        if (properties == null) {
            loadProperties();
        }
        return properties;
    }

    /**
     * Obtiene la configuración de todas las impresoras como objetos PrinterConfig.
     * Centraliza la lógica de parseo, validación y construcción.
     * @return Un mapa con el nombre de la impresora y su configuración.
     */
    public Map<String, PrinterConfig> getAllPrinterConfigs() {
        Map<String, PrinterConfig> printers = new HashMap<>();
        if (properties == null) return printers;

        // Agrupar propiedades por nombre de impresora
        Map<String, Map<String, String>> printerPropsByName = new HashMap<>();
        for (String key : properties.stringPropertyNames()) {
            String[] parts = key.split("\\.", 2);
            if (parts.length == 2) {
                String printerName = parts[0];
                String propName = parts[1];
                printerPropsByName
                    .computeIfAbsent(printerName, k -> new HashMap<>())
                    .put(propName, properties.getProperty(key));
            }
        }

        // Crear objetos PrinterConfig
        for (Map.Entry<String, Map<String, String>> entry : printerPropsByName.entrySet()) {
            String name = entry.getKey();
            Map<String, String> props = entry.getValue();
            
            String ip = props.get("ip");
            String portStr = props.get("port");
            String copias = props.get("copias");
            String logo = props.get("logo");
            String papelSize = props.get("papelSize");
            String tipoConexion = props.get("tipoConexion");
            
            if (papelSize == null) papelSize = "48";
            if (tipoConexion == null) tipoConexion = "red";

            if (ip != null && !ip.isEmpty() && portStr != null && !portStr.isEmpty() && copias != null) {
                try {
                    int port = Integer.parseInt(portStr);
                    PrinterConfig config = new PrinterConfig(
                        name, ip, logo, port, 
                        Integer.parseInt(copias), 
                        Integer.parseInt(papelSize), 
                        tipoConexion
                    );
                    printers.put(name, config);
                } catch (NumberFormatException e) {
                    LOGGER.log(Level.WARNING, "Puerto inválido para la impresora {0}: {1}", new Object[]{name, portStr});
                }
            } else {
                LOGGER.log(Level.WARNING, "Configuración incompleta para la impresora: {0}", name);
            }
        }

        return printers;
    }
}