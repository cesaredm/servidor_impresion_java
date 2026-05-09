package domain;

import domain.entities.Printer;
import domain.entities.PrinterConfig;
import java.util.List;
import java.util.Optional;

public interface PrinterNetworkService {
    List<Printer> discover(String ipRange);
    
    boolean ping(String ipAddress);
    
    void save(PrinterConfig config);
    
    Optional<PrinterConfig> findByIp(String ipAddress);
    
    List<PrinterConfig> findAll();
    
    Optional<PrinterConfig> findByName(String name);
    
    void delete(String name);
}