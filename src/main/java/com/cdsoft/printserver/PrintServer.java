/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */
package com.cdsoft.printserver;

import com.sun.net.httpserver.HttpServer;
import domain.entities.PrinterConfig;
import httpHandle.PrintHandler;
import httpHandle.handlers.ConfigHandler;
import httpHandle.handlers.PrinterNetworkHandler;
import infra.Mdns;
import infrastructure.PrinterConfigProperties;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonInitException;

public class PrintServer implements Daemon {

    private static final Logger LOGGER = Logger.getLogger(PrintServer.class.getName());
    
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
        Mdns.iniciarMDNS();
    }

    @Override
    public void stop() throws Exception {
        if (server != null) {
            stopServer();
            Mdns.detenerMdns();
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
        // ------------------------------------------------------------------
        // CÓDIGO ANTERIOR (comentado para referencia)
        // ------------------------------------------------------------------
        // Se muevió la lógica de parseo al Singleton infrastructure.PrinterConfigProperties
        // ------------------------------------------------------------------

        // NUEVA IMPLEMENTACIÓN: Usar el Singleton centralizado
        PrinterConfigProperties configProps = PrinterConfigProperties.getInstance();
        
        // Recargar desde disco para obtener la versión más reciente
        configProps.loadProperties();
        
        // Delegar la carga y construcción de objetos al Singleton
        printers.putAll(configProps.getAllPrinterConfigs());

        if (printers.isEmpty()) {
            LOGGER.log(Level.WARNING, "No se cargó ninguna configuración de impresora válida.");
        }
    }

    // Mantener el resto del código original sin cambios...
    public static void startServer() throws IOException, NoSuchAlgorithmException {
        
        HttpServer server = HttpServer.create(new InetSocketAddress(SERVER_PORT), 0);
        
        // ... rutas ...
        server.createContext("/print", new PrintHandler(printers));
        server.createContext("/impresoras", new PrintHandler(printers));
        server.createContext("/recargar", new ConfigHandler(printers));
        server.createContext("/comanda/print", new PrintHandler(printers));
        server.createContext("/cotizacion/print", new PrintHandler(printers));
        server.createContext("/pago/print", new PrintHandler(printers));
        server.createContext("/prueba", new PrintHandler(printers));
        server.createContext("/printers", new PrinterNetworkHandler(printers));
        server.createContext("/printers/discover", new PrinterNetworkHandler(printers));
        server.createContext("/printers/ping", new PrinterNetworkHandler(printers));

        executor = new ThreadPoolExecutor(
                2,
                10,
                60L, TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>()
        );
        server.setExecutor(executor);
        server.start();
        LOGGER.log(Level.INFO, "Servidor de impresión iniciado en el puerto {0}", SERVER_PORT);
        LOGGER.log(Level.INFO, "Esperando peticiones en http://localhost:{0}/print/{{nombre_impresora}}", SERVER_PORT);
    }

    public static void stopServer() {
        if (server != null) {
            LOGGER.log(Level.INFO, "Iniciando la detención del servidor de impresión");

            try {
                server.stop(1);
                LOGGER.log(Level.INFO, "Servidor detenido correctamente.");

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
            } catch (InterruptedException e) {
                LOGGER.log(Level.WARNING, "La interrupción en el hilo principal forzó el cierre del pool.", e);
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            } finally {
                LOGGER.log(Level.INFO, "Detención completa. Saliendo del sistema...");
                System.exit(0);
            }
        } else {
            LOGGER.log(Level.WARNING, "El servidor ya está detenido o no se inició correctamente.");
        }
    }

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