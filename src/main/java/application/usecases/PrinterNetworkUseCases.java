package application.usecases;

import domain.PrinterNetworkService;
import domain.PrinterStatus;
import domain.entities.Printer;
import infrastructure.PrinterNetworkServiceImpl;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PrinterNetworkUseCases {

    private static final Logger LOGGER = Logger.getLogger(PrinterNetworkUseCases.class.getName());
    private final PrinterNetworkService service;

    public PrinterNetworkUseCases() {
        this.service = new PrinterNetworkServiceImpl();
    }

    public List<Printer> discoverPrinters(String ipRange) {
        LOGGER.log(Level.INFO, "Descubriendo impresoras en rango: {0}", ipRange);
        List<Printer> printers = service.discover(ipRange);
        
        for (Printer printer : printers) {
            //service.save(printer);
            LOGGER.log(Level.INFO, "Impresora encontrada: {0} - {1}", new Object[]{printer.getIpAddress(), printer.getStatus()});
        }
        
        return printers;
    }

    public boolean pingPrinter(String ip) {
        LOGGER.log(Level.INFO, "Haciendo ping a: {0}", ip);
        boolean result = service.ping(ip);
        
        Optional<Printer> printer = service.findByIp(ip);
        if (printer.isPresent()) {
            printer.get().setStatus(result ? PrinterStatus.ONLINE : PrinterStatus.OFFLINE);
            service.save(printer.get());
        }
        
        LOGGER.log(Level.INFO, "Ping a {0}: {1}", new Object[]{ip, result ? "OK" : "FAILED"});
        return result;
    }

    public void savePrinterConfig(Printer printer) {
        LOGGER.log(Level.INFO, "Guardando configuracion de impresora: {0}", printer.getIpAddress());
        
        boolean alive = service.ping(printer.getIpAddress());
        printer.setStatus(alive ? PrinterStatus.ONLINE : PrinterStatus.OFFLINE);
        
        service.save(printer);
        LOGGER.log(Level.INFO, "Impresora guardada: {0} - {1}", new Object[]{printer.getIpAddress(), printer.getStatus()});
    }

    public List<Printer> listPrinters() {
        LOGGER.info("Listando impresoras configuradas");
        return service.findAll();
    }

    public Optional<Printer> findByIp(String ip) {
        return service.findByIp(ip);
    }

    public void deletePrinter(String ip) {
        LOGGER.log(Level.INFO, "Eliminando impresora: {0}", ip);
        service.delete(ip);
    }
}