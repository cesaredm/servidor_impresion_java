package httpHandle;

import com.github.anastaciocintra.escpos.EscPos;
import com.github.anastaciocintra.output.PrinterOutputStream;
import com.github.anastaciocintra.output.TcpIpOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author cesar
 */
public class Printescpos {

    private final static Logger LOGGER = Logger.getLogger(Printescpos.class.getName());
    private static EscPos print;

    public Printescpos() {
    }

    public static String[] listaImpresorasDisponibles() {
        String[] lista = PrinterOutputStream.getListPrintServicesNames();

        return lista;
    }

    public static String printTcpIp(PrinterConfig printer) {
        try (TcpIpOutputStream outputStream = new TcpIpOutputStream(printer.getIp(), printer.getPuerto())) {
            print = new EscPos(outputStream);
            print.write("hola Mundo");
            print.info();
            return "Exito en la impresion";
        } catch (ConnectException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            return "Fallo la impresion";
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            return "Error en la impresion";
        }
    }
}
