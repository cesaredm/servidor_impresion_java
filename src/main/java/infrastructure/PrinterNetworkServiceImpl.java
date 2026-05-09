/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */
package infrastructure;

import infrastructure.state.PrinterStateHolder;
import domain.PrinterNetworkService;
import domain.PrinterStatus;
import domain.entities.Printer;
import domain.entities.PrinterConfig;
import infrastructure.config.PrinterConfigProperties;

import java.net.InetAddress;
import java.net.Socket;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PrinterNetworkServiceImpl implements PrinterNetworkService {

    private static final Logger LOGGER = Logger.getLogger(PrinterNetworkServiceImpl.class.getName());
    private static final int PING_TIMEOUT = 1000;
    private static final int PRINTER_PORT = 9100;

    private final PrinterConfigProperties configProps;

    public PrinterNetworkServiceImpl() {
        this.configProps = PrinterConfigProperties.getInstance();
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
    public List<Printer> discover(String ipRange) {
        List<Printer> printers = new ArrayList<>();

        String startIp = ipRange.split("-")[0].trim();
        String endIp = ipRange.split("-")[1].trim();

        String baseIp = startIp.substring(0, startIp.lastIndexOf("."));
        int start = Integer.parseInt(startIp.substring(startIp.lastIndexOf(".") + 1));
        int end = Integer.parseInt(endIp.substring(endIp.lastIndexOf(".") + 1));

        for (int i = start; i <= end; i++) {
            String ip = baseIp + "." + i;
            
            boolean reachable = ping(ip);
            
            Printer printer = new Printer(ip, ip);
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

    @Override
    public void save(PrinterConfig config) {
        String name = config.getNombre();
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("El nombre de la impresora es requerido");
        }

        configProps.setProperty(name, "ip", config.getIp());
        configProps.setProperty(name, "port", String.valueOf(config.getPuerto()));
        configProps.setProperty(name, "copias", String.valueOf(config.getCopias()));
        configProps.setProperty(name, "papelSize", String.valueOf(config.getPapelSize()));
        configProps.setProperty(name, "tipoConexion", config.getTipoConexion());

        if (config.getLogo() != null && !config.getLogo().isEmpty()) {
            configProps.setProperty(name, "logo", config.getLogo());
        }

        configProps.saveProperties();
        configProps.loadProperties();
    }

    @Override
    public Optional<PrinterConfig> findByIp(String ipAddress) {
        for (PrinterConfig config : PrinterStateHolder.INSTANCE.getAllPrinters()) {
            if (ipAddress.equals(config.getIp())) {
                return Optional.of(config);
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<PrinterConfig> findByName(String name) {
        PrinterConfig config = PrinterStateHolder.INSTANCE.getPrinter(name);
        return Optional.ofNullable(config);
    }

    @Override
    public List<PrinterConfig> findAll() {
        return new ArrayList<>(PrinterStateHolder.INSTANCE.getAllPrinters());
    }

    @Override
    public void delete(String name) {
        configProps.removePrinter(name);
        configProps.saveProperties();
        configProps.loadProperties();
    }
}