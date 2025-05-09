/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */
package com.cdsoft.printserver;

import java.util.logging.Level;
import com.sun.net.httpserver.HttpServer;
import httpHandle.ConfigHandler;
import httpHandle.PrintHandler;
import httpHandle.PrinterConfig;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonInitException;

public class PrintServer implements Daemon {

    private static final Logger LOGGER = Logger.getLogger(PrintServer.class.getName());
    private static final String USER_PATH = System.getProperty("user.home");
    private static final String CONFIG_FILE = "C:\\impresorasConfig\\printers.properties";
    private static final int SERVER_PORT = 8088;
    private static final Map<String, PrinterConfig> printers = new HashMap<>();
    private static HttpServer server;
    private static ExecutorService executor;

    @Override
    public void init(DaemonContext context) throws DaemonInitException {
        try {
            loadPrinterConfiguration();
        } catch (IOException e) {
            throw new DaemonInitException("Error al inicializar el servidor", e);
        }
    }

    @Override
    public void start() throws Exception {
        startServer();
    }

    @Override
    public void stop() throws Exception {
        if (server != null) {
            //server.stop(0);
            stopServer();
            LOGGER.info("Servidor detenido");
        }
    }

    @Override
    public void destroy() {
        if (server != null) {
            server.stop(0);
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
            String copias = properties.get("copias");
            String logo = properties.get("logo");

            if (ip != null && !ip.isEmpty() && portStr != null && !portStr.isEmpty()) {
                try {
                    int port = Integer.parseInt(portStr);
                    PrinterConfig config = new PrinterConfig(name, ip, logo, port, Integer.parseInt(copias));
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
        server.createContext("/print", new PrintHandler(printers));
        server.createContext("/impresoras", new PrintHandler(printers));
        server.createContext("/recargar", new ConfigHandler());
        server.createContext("/comanda/print", new PrintHandler(printers));
        executor = Executors.newCachedThreadPool();
        //server.setExecutor(Executors.newCachedThreadPool());
        server.setExecutor(executor);
        server.start();
        LOGGER.log(Level.INFO, "Servidor de impresión iniciado en el puerto {0}", SERVER_PORT);
        LOGGER.log(Level.INFO, "Esperando peticiones en http://localhost:{0}/print/{{nombre_impresora}}", SERVER_PORT);
    }

    public static void stopServer() {
        if (server != null) {
            LOGGER.log(Level.INFO, "Iniciando la detención del servidor de impresión");

            try {
                // Detener el servidor
                server.stop(0);
                LOGGER.log(Level.INFO, "Servidor detenido correctamente.");

                // Apagar el executor si está activo
                if (executor != null && !executor.isShutdown()) {
                    LOGGER.log(Level.INFO, "Cerrando el pool de hilos...");
                    executor.shutdown();

                    if (!executor.awaitTermination(15, TimeUnit.SECONDS)) {
                        LOGGER.log(Level.WARNING, "Fuerza el apagado del pool de hilos...");
                        executor.shutdownNow();

                        if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                            LOGGER.log(Level.SEVERE, "No se pudo detener el pool de hilos completamente.");
                        }
                    }
                }

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error al detener el servidor de impresión", e);
            } finally {
                // Salir de la JVM después de cerrar todo
                LOGGER.log(Level.INFO, "Detención completa. Saliendo del sistema...");
                System.exit(0);
            }
        } else {
            LOGGER.log(Level.WARNING, "El servidor ya está detenido o no se inició correctamente.");
        }
    }

    // Método main para pruebas manuales
    public static void main(String[] args) {
        PrintServer serverPrint = new PrintServer();
        try {
            serverPrint.init(null);
            serverPrint.start();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    stopServer();
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error al detener el servidor", e);
                }
            }));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al iniciar el servidor de impresión.", e);
        }
    }
}
