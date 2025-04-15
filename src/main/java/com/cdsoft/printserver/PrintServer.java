/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.cdsoft.printserver;
import java.util.logging.Level;

/**
 *
 * @author cesar
 */
import com.sun.net.httpserver.HttpServer;
import httpHandle.PrintHandler;
import httpHandle.PrinterConfig;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author cesar
 */

public class PrintServer {

    private static final Logger LOGGER = Logger.getLogger(PrintServer.class.getName());
    private static final String CONFIG_FILE = "C:\\Users\\cesar\\Documents\\printers.properties";
    private static final int SERVER_PORT = 8088; // Puerto en el que escuchará el servidor
    private static final Map<String, PrinterConfig> printers = new HashMap<>();

    public static void main(String[] args) {
        try {
            loadPrinterConfiguration();
            startServer();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al iniciar el servidor de impresión.", e);
        }
    }

    public static void loadPrinterConfiguration() throws IOException {
        Properties props = new Properties();
        try (InputStream input = new FileInputStream(CONFIG_FILE)) {
            props.load(input);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "No se pudo cargar el archivo de configuración: " + CONFIG_FILE, e);
            throw e; // Relanzar para detener el inicio si no hay config
        }

        // Agrupar propiedades por nombre de impresora
        Map<String, Map<String, String>> printerPropsByName = new HashMap<>();
        for (String key : props.stringPropertyNames()) {
            String[] parts = key.split("\\.", 2);
            if (parts.length == 2) {
                String printerName = parts[0];
                String propName = parts[1];
                /*
                    Cuando procesamos la primera propiedad de una impresora (digamos "cocina.ip"), printerName es "cocina".
                    computeIfAbsent busca "cocina" en printerPropsByName. Como es la primera vez, no la encuentra.
                    Ejecuta k -> new HashMap<>(), lo que crea un nuevo HashMap vacío.
                    Inserta ("cocina", nuevoHashMapVacio) en printerPropsByName.
                    Devuelve nuevoHashMapVacio.
                
                    Esto significa: "En el mapa interno que computeIfAbsent me devolvió, añade la propiedad actual".

                    Siguiendo el ejemplo de "cocina.ip": computeIfAbsent devolvió el nuevoHashMapVacio. La llamada .put("ip", "192.168.1.100") se ejecuta sobre ese nuevo mapa. Ahora el mapa interno para "cocina" contiene {"ip": "192.168.1.100"}.
                    Cuando luego procesamos "cocina.port", printerName es "cocina" de nuevo.
                    computeIfAbsent busca "cocina". Esta vez sí la encuentra.
                    Devuelve el mapa interno que ya existe (el que ahora contiene {"ip": "192.168.1.100"}).
                    La llamada encadenada .put("port", "9100") se ejecuta sobre ese mapa existente. Ahora el mapa interno para "cocina" contiene {"ip": "192.168.1.100", "port": "9100"}.
                */
                printerPropsByName.computeIfAbsent(printerName, k -> new HashMap<>())
                                 .put(propName, props.getProperty(key));
            }
        }

        // Crear objetos PrinterConfig
        printers.clear(); // Limpiar configuraciones previas si se recarga
        for (Map.Entry<String, Map<String, String>> entry : printerPropsByName.entrySet()) {
            String name = entry.getKey();
            Map<String, String> properties = entry.getValue();
            String ip = properties.get("ip");
            String portStr = properties.get("port");

            if (ip != null && !ip.isEmpty() && portStr != null && !portStr.isEmpty()) {
                try {
                    int port = Integer.parseInt(portStr);
                    PrinterConfig config = new PrinterConfig(name, ip, port);
                    printers.put(name, config);
                    LOGGER.log(Level.INFO, "Impresora cargada: {0}", config.toString());
                } catch (NumberFormatException e) {
                    LOGGER.log(Level.WARNING, "Puerto inválido para la impresora {0}: {1}", new Object[]{name, portStr});
                }
            } else {
                 LOGGER.log(Level.WARNING, "Configuración incompleta para la impresora: {0}", name);
            }
        }

        if (printers.isEmpty()) {
            LOGGER.log(Level.WARNING, "No se cargó ninguna configuración de impresora válida.");
        }
    }

    public static void startServer() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(SERVER_PORT), 0);

        // Crear contexto para las peticiones de impresión
        // La URL será http://<IP_SERVIDOR>:<SERVER_PORT>/print/{nombreImpresora}
        server.createContext("/print", new PrintHandler(printers));
        server.createContext("/impresoras", new PrintHandler(printers));

        // Usar un pool de hilos para manejar peticiones concurrentes
        server.setExecutor(Executors.newCachedThreadPool());

        server.start();
        LOGGER.log(Level.INFO, "Servidor de impresión iniciado en el puerto {0}", SERVER_PORT);
        LOGGER.log(Level.INFO, "Esperando peticiones en http://localhost:{0}/print/{{nombre_impresora}}", SERVER_PORT);
    }

    // Opcional: Método para recargar la configuración sin reiniciar el servidor
    public static void reloadConfiguration() {
        LOGGER.info("Recargando configuración de impresoras...");
        try {
            loadPrinterConfiguration();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al recargar la configuración.", e);
        }
    }
}
