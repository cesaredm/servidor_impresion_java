package httpHandle;

import com.github.anastaciocintra.escpos.EscPos;
import com.github.anastaciocintra.escpos.EscPosConst;
import com.github.anastaciocintra.escpos.PrintModeStyle;
import com.github.anastaciocintra.escpos.Style;
import com.github.anastaciocintra.escpos.image.Bitonal;
import com.github.anastaciocintra.escpos.image.BitonalOrderedDither;
import com.github.anastaciocintra.escpos.image.CoffeeImageImpl;
import com.github.anastaciocintra.escpos.image.EscPosImage;
import com.github.anastaciocintra.escpos.image.RasterBitImageWrapper;
import com.github.anastaciocintra.output.PrinterOutputStream;
import com.github.anastaciocintra.output.TcpIpOutputStream;
import entities.Comanda;
import entities.DatosGenerales;
import entities.DatosGeneralesComanda;
import entities.Detalles;
import entities.Factura;
import entities.Tienda;
import entities.Totales;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.Image;

/**
 *
 * @author cesar
 */
public class Printescpos {

    private final static Logger LOGGER = Logger.getLogger(Printescpos.class.getName());
    private static EscPos print;
    static int papelAncho = 48;
    private final static int anchoTitulos = "Cant".length() + "Precio".length() + "Total".length();
    private final static DecimalFormat formatDecimal = new DecimalFormat("###,###,###,##0.00");
    static Style title = new Style()
            .setJustification(EscPosConst.Justification.Center)
            .setBold(true)
            .setFontSize(Style.FontSize._2, Style.FontSize._2)
            .setFontName(Style.FontName.Font_B);
    static PrintModeStyle campo = new PrintModeStyle().setBold(true);
    static PrintModeStyle bold = new PrintModeStyle().setBold(true);
    static PrintModeStyle nota = new PrintModeStyle().setBold(true).setJustification(EscPosConst.Justification.Center);

    public Printescpos() {
    }

    // funcion que retorna lista de impresoras disponibles en el dispositivo
    public static String[] listaImpresorasDisponibles() {
        String[] lista = PrinterOutputStream.getListPrintServicesNames();

        return lista;
    }

    public static void printTest(PrinterConfig printer) {
        try (TcpIpOutputStream outputStream = new TcpIpOutputStream(printer.getIp(), printer.getPuerto())) {
            EscPos print = new EscPos(outputStream);
            print.writeLF("Esto es un test");
            print.feed(2);
            print.writeLF("De impresion en la impresora " + printer.getNombre());
            print.feed(5);
            print.cut(EscPos.CutMode.FULL);
            print.close();
        } catch (ConnectException ex) {
            LOGGER.log(Level.SEVERE, null, "No se pudo conectar a la impresra, revise la configuracion " + ex);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, "Error en la impresion " + ex);
        } finally {

        }
    }
}
