package infrastructure.escpos;

import com.github.anastaciocintra.escpos.EscPos;
import com.github.anastaciocintra.output.PrinterOutputStream;
import com.github.anastaciocintra.output.TcpIpOutputStream;
import domain.PrinterConfig;
import java.io.IOException;
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
        TcpIpOutputStream tcp = new TcpIpOutputStream(config.getIp(), config.getPuerto());
        return new EscPos(tcp);
    }

    private EscPos crearConexionUsb(PrinterConfig config) throws IOException {
        PrintService printService = PrinterOutputStream.getPrintServiceByName(config.getNombre());
        if (printService == null) {
            throw new IOException("Impresora USB no encontrada: " + config.getNombre());
        }
        PrinterOutputStream usb = new PrinterOutputStream(printService);
        return new EscPos(usb);
    }
}