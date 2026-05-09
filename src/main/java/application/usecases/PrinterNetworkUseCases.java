package application.usecases;

import domain.PrinterNetworkService;
import domain.PrinterStatus;
import domain.entities.Printer;
import domain.entities.PrinterConfig;
import infrastructure.PrinterNetworkServiceImpl;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PrinterNetworkUseCases {

    private static final Logger LOGGER = Logger.getLogger(PrinterNetworkUseCases.class.getName());
    private final PrinterNetworkService service;

    public PrinterNetworkUseCases(PrinterNetworkServiceImpl printerService) {
        this.service = printerService;
    }

    public List<Printer> discoverPrinters(String ipRange) {
        LOGGER.log(Level.INFO, "Descubriendo impresoras en rango: {0}", ipRange);
        List<Printer> printers = service.discover(ipRange);
        
        for (Printer printer : printers) {
            LOGGER.log(Level.INFO, "Impresora encontrada: {0} - {1}", new Object[]{printer.getIpAddress(), printer.getStatus()});
        }
        
        return printers;
    }

    public boolean pingPrinter(String ip) {
        LOGGER.log(Level.INFO, "Haciendo ping a: {0}", ip);
        boolean result = service.ping(ip);
        
        Optional<PrinterConfig> configOpt = service.findByIp(ip);
        if (configOpt.isPresent()) {
            PrinterConfig config = configOpt.get();
            Printer printer = toPrinter(config, result ? PrinterStatus.ONLINE : PrinterStatus.OFFLINE);
            service.save(toPrinterConfig(printer));
        }
        
        LOGGER.log(Level.INFO, "Ping a {0}: {1}", new Object[]{ip, result ? "OK" : "FAILED"});
        return result;
    }

    public void savePrinterConfig(Printer printer) {
        LOGGER.log(Level.INFO, "Guardando configuracion de impresora: {0}", printer.getIpAddress());
        
        boolean alive = service.ping(printer.getIpAddress());
        
        PrinterConfig config = toPrinterConfig(printer);
        config = new PrinterConfig(
            config.getNombre(),
            config.getIp(),
            config.getLogo(),
            config.getPuerto(),
            config.getCopias(),
            config.getPapelSize(),
            config.getTipoConexion()
        );
        
        service.save(config);
        LOGGER.log(Level.INFO, "Impresora guardada: {0}", config.getNombre());
    }

    public List<PrinterConfig> listPrinters() {
        LOGGER.info("Listando impresoras configuradas");
        return service.findAll();
    }

    public Optional<PrinterConfig> findByIp(String ip) {
        return service.findByIp(ip);
    }
    
    public Optional<PrinterConfig> findByName(String name) {
        return service.findByName(name);
    }

    public void deletePrinter(String name) {
        LOGGER.log(Level.INFO, "Eliminando impresora: {0}", name);
        service.delete(name);
    }

    private PrinterConfig toPrinterConfig(Printer printer) {
        return new PrinterConfig(
            printer.getName(),
            printer.getIpAddress(),
            null,
            printer.getPort(),
            1,
            48,
            "red"
        );
    }

    private Printer toPrinter(PrinterConfig config, PrinterStatus status) {
        return new Printer(
            config.getNombre(),
            config.getIp(),
            null,
            status,
            config.getPuerto()
        );
    }
}