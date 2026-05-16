package infrastructure.escpos;

import com.github.anastaciocintra.escpos.EscPos;
import com.github.anastaciocintra.output.PrinterOutputStream;
import com.github.anastaciocintra.output.TcpIpOutputStream;
import domain.entities.PrinterConfig;
import domain.exceptions.ImpresoraNoConectadaException;
import domain.exceptions.ImpresoraNoInstaladaException;
import java.io.IOException;
import java.net.ConnectException;
import javax.print.PrintService;

public class EscposConnectionFactory {

    public EscPos crearConexion(PrinterConfig config, String tipoConexion) throws IOException {
        if ("red".equalsIgnoreCase(tipoConexion)) {
            return crearConexionRed(config);
        } else {
            return crearConexionUsb(config);
        }
    }

    private EscPos crearConexionRed(PrinterConfig config) throws IOException {
        try {
            TcpIpOutputStream tcp = new TcpIpOutputStream(config.getIp(), config.getPuerto());
            return new EscPos(tcp);
        } catch (ConnectException e) {
            throw new ImpresoraNoConectadaException(config.getIp(), config.getPuerto());
        }
    }

    private EscPos crearConexionUsb(PrinterConfig config) throws IOException {
        PrintService printService = PrinterOutputStream.getPrintServiceByName(config.getNombre());
        if (printService == null) {
            throw new ImpresoraNoInstaladaException(config.getNombre());
        }
        PrinterOutputStream usb = new PrinterOutputStream(printService);
        return new EscPos(usb);
    }
}