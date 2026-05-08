package domain;

import domain.entities.Printer;
import java.util.List;
import java.util.Optional;

public interface PrinterNetworkService {
    List<Printer> discover(String ipRange);
    
    boolean ping(String ipAddress);
    
    void save(Printer printer);
    
    Optional<Printer> findByIp(String ipAddress);
    
    List<Printer> findAll();
    
    void delete(String ipAddress);
}