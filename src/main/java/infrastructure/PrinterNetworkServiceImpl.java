/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */
package infrastructure;

import domain.PrinterNetworkService;
import domain.PrinterStatus;
import domain.entities.Printer;
import infrastructure.PrinterConfigProperties;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PrinterNetworkServiceImpl implements PrinterNetworkService {

    private static final Logger LOGGER = Logger.getLogger(PrinterNetworkServiceImpl.class.getName());
    private static final int PING_TIMEOUT = 1000;
    private static final int PRINTER_PORT = 9100;

    // NUEVA IMPLEMENTACIÓN: Referencia única al Singleton centralizado
    private final PrinterConfigProperties configProps;

    public PrinterNetworkServiceImpl() {
        this.configProps = PrinterConfigProperties.getInstance();
    }

    @Override
    public List<Printer> discover(String ipRange) {
        List<Printer> printers = new ArrayList<>();

        String startIp = ipRange.split("-")[0].trim();
        String endIp = ipRange.split("-")[1].trim();

        String baseIp = startIp.substring(0, startIp.lastIndexOf("."));
        int start = Integer.parseInt(startIp.substring(startIp.lastIndexOf(".") + 1));
        int end = Integer.parseInt(endIp.substring(endIp.lastIndexOf(".") + 1));

        for (int i = start; i <= end; i++) {
            String ip = baseIp + "." + i;
            Printer printer = findByIp(ip).orElse(null);

            if (printer == null) {
                boolean reachable = ping(ip);
                printer = new Printer(UUID.randomUUID().toString(), ip);
                printer.setStatus(reachable ? PrinterStatus.ONLINE : PrinterStatus.OFFLINE);

                if (reachable) {
                    try {
                        String hostName = InetAddress.getByName(ip).getHostName();
                        if (!hostName.equals(ip)) {
                            printer.setHostName(hostName);
                        }
                    } catch (Exception e) {
                        LOGGER.log(Level.FINE, "No se pudo resolver hostname para: " + ip);
                    }
                }
            }

            if (printer.getStatus() == PrinterStatus.ONLINE) {
                printers.add(printer);
            }
        }

        return printers;
    }

    @Override
    public boolean ping(String ipAddress) {
        try {
            InetAddress address = InetAddress.getByName(ipAddress);
            boolean reachable = address.isReachable(PING_TIMEOUT);

            if (!reachable) {
                reachable = checkPrinterPort(ipAddress);
            }

            return reachable;
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Error al hacer ping a: " + ipAddress);
            return false;
        }
    }

    private boolean checkPrinterPort(String ipAddress) {
        try (Socket socket = new Socket()) {
            socket.connect(new java.net.InetSocketAddress(ipAddress, PRINTER_PORT), PING_TIMEOUT);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void save(Printer printer) {
        String name = printer.getName();
        if (name == null || name.isEmpty()) {
            name = printer.getIpAddress().replace(".", "_");
            printer.setName(name);
        }

        configProps.setProperty(name, "ip", printer.getIpAddress());
        configProps.setProperty(name, "port", String.valueOf(printer.getPort()));
        configProps.setProperty(name, "status", printer.getStatus().name());
        
        if (printer.getHostName() != null && !printer.getHostName().isEmpty()) {
            configProps.setProperty(name, "hostname", printer.getHostName());
        }

        // Guarda cambios a disco mediante el Singleton
        configProps.saveProperties();
    }

    @Override
    public Optional<Printer> findByIp(String ipAddress) {
        for (String printerName : configProps.getAllPrinterNames()) {
            String storedIp = configProps.getProperty(printerName, "ip");
            if (ipAddress.equals(storedIp)) {
                return Optional.of(buildPrinter(printerName));
            }
        }
        return Optional.empty();
    }

    public Optional<Printer> findByName(String name) {
        String ip = configProps.getProperty(name, "ip");
        if (ip != null) {
            return Optional.of(buildPrinter(name));
        }
        return Optional.empty();
    }

    private Printer buildPrinter(String name) {
        String ip = configProps.getProperty(name, "ip");
        String portStr = configProps.getProperty(name, "port");
        String hostName = configProps.getProperty(name, "hostname");
        String statusStr = configProps.getProperty(name, "status");

        if (ip == null) {
            return null;
        }

        PrinterStatus status;
        try {
            status = PrinterStatus.valueOf(statusStr != null ? statusStr : "UNKNOWN");
        } catch (Exception e) {
            status = PrinterStatus.UNKNOWN;
        }

        int port = 9100;
        if (portStr != null) {
            try {
                port = Integer.parseInt(portStr);
            } catch (Exception e) {
                port = 9100;
            }
        }

        Printer printer = new Printer(name, ip);
        printer.setHostName(hostName);
        printer.setStatus(status);
        printer.setPort(port);

        return printer;
    }

    @Override
    public List<Printer> findAll() {
        List<Printer> printers = new ArrayList<>();

        for (String printerName : configProps.getAllPrinterNames()) {
            try {
                Printer printer = buildPrinter(printerName);
                if (printer != null) {
                    printers.add(printer);
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error al cargar impresora: " + printerName);
            }
        }

        return printers;
    }

    @Override
    public void delete(String name) {
        configProps.removePrinter(name);
        configProps.saveProperties();
    }
}