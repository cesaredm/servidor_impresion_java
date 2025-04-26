package httpHandle;

import com.github.anastaciocintra.escpos.EscPos;
import com.github.anastaciocintra.escpos.EscPosConst;
import com.github.anastaciocintra.escpos.PrintModeStyle;
import com.github.anastaciocintra.escpos.image.BitImageWrapper;
import com.github.anastaciocintra.escpos.image.Bitonal;
import com.github.anastaciocintra.escpos.image.BitonalThreshold;
import com.github.anastaciocintra.escpos.image.CoffeeImageImpl;
import com.github.anastaciocintra.escpos.image.EscPosImage;
import com.github.anastaciocintra.escpos.image.RasterBitImageWrapper;
import com.github.anastaciocintra.output.PrinterOutputStream;
import com.github.anastaciocintra.output.TcpIpOutputStream;
import entities.DatosGenerales;
import entities.Detalles;
import entities.Factura;
import entities.Tienda;
import entities.Totales;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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

/**
 *
 * @author cesar
 */
public class Printescpos {

    private final static Logger LOGGER = Logger.getLogger(Printescpos.class.getName());
    private static EscPos print;
    final static int papelAncho = 48;
    final static int anchoTitulos = "Cant".length() + "Precio".length() + "Total".length();
    final static int papelAncho58mm = 38;
    private final static DecimalFormat formatDecimal = new DecimalFormat("###,###,###,##0.00");
    static PrintModeStyle title = new PrintModeStyle().setFontSize(true, true).setJustification(EscPosConst.Justification.Center);
    static PrintModeStyle campo = new PrintModeStyle().setBold(true);
    static PrintModeStyle bold = new PrintModeStyle().setBold(true);
    static PrintModeStyle nota = new PrintModeStyle().setBold(true).setJustification(EscPosConst.Justification.Center);
    static byte[] printData;

    public Printescpos() {
    }

    public static String[] listaImpresorasDisponibles() {
        String[] lista = PrinterOutputStream.getListPrintServicesNames();

        return lista;
    }

    public static int espacioCantidades(float value) {
        return formatDecimal.format(value).length();
    }

    public static String espacio(int ancho, int campo, int resto) {
        int espacios = ancho - campo - resto;
        if (espacios < 0) {
            espacios = 0;
        }
        return " ".repeat(espacios);
    }

    public static String espacioTresColumnas(int ancho, int caracterres) {
        int espacios = (ancho - caracterres) / 2;
        if (espacios < 0) {
            espacios = 0;
        }
        return " ".repeat(espacios);
    }

    public static byte[] obtenerImagenDeUrl(String urlImagen) throws IOException {
        //1738198497071-CDsoft.png
        URL url = new URL(urlImagen);
        HttpURLConnection conexion = (HttpURLConnection) url.openConnection();

        try {
            conexion.setRequestMethod("GET"); // O el método que necesites (POST, etc.)
            conexion.connect();

            int codigoRespuesta = conexion.getResponseCode();
            if (codigoRespuesta == HttpURLConnection.HTTP_OK) {
                InputStream inputStream = conexion.getInputStream();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                return outputStream.toByteArray();
            } else {
                throw new IOException("Error al obtener la imagen. Código de respuesta: " + codigoRespuesta);
            }
        } finally {
            conexion.disconnect();
        }
    }

    // Helper para leer todos los bytes del InputStream
    private static byte[] readAllBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[1024];
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        return buffer.toByteArray();
    }

    public static String printTcpIp(PrinterConfig printer, byte data[]) {
        try (TcpIpOutputStream outputStream = new TcpIpOutputStream(printer.getIp(), printer.getPuerto())) {
            PrintModeStyle style = new PrintModeStyle();
            PrintModeStyle title = new PrintModeStyle().setFontSize(true, true).setJustification(EscPosConst.Justification.Center);
            PrintModeStyle total = style.setJustification(EscPosConst.Justification.Right);
            print = new EscPos(outputStream);
            print.writeLF("hola Mundo");
            print.writeLF("Hola Danny ....");
            print.write(title, "Titulo");
            print.write(data, 0, data.length - 1);
            print.feed(5);
            print.cut(EscPos.CutMode.FULL);
            return "Exito en la impresion";
        } catch (ConnectException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            return "Fallo la impresion";
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            return "Error en la impresion";
        }
    }

    public static String printTcpIp(PrinterConfig printer, Factura factura, boolean copia) {
        List<Detalles> detalles = factura.getDetalles();
        Tienda tienda = factura.getTienda();
        DatosGenerales datosGenerales = factura.getDatosGenerales();
        Totales totales = factura.getTotales();
        try (TcpIpOutputStream outputStream = new TcpIpOutputStream(printer.getIp(), printer.getPuerto())) {
            print = new EscPos(outputStream);

            /*String urlLogo = "https://api.cdsoft.net/uploads/logos/1738198497071-CDsoft.png"; // Reemplaza con la URL real
            byte[] logoBytes = obtenerImagenDeUrl(urlLogo);
            // Convertir los bytes de la imagen a BufferedImage
            InputStream in = new ByteArrayInputStream(logoBytes);
            BufferedImage bufferedImage = ImageIO.read(in);
            Bitonal algorithm = new BitonalThreshold(127);
            EscPosImage escposImage = new EscPosImage(new CoffeeImageImpl(bufferedImage), algorithm);
            // Procesar la imagen para escpos (ejemplo básico)
            //RasterBitImageWrapper imageWrapper = new RasterBitImageWrapper();
            // this wrapper uses esc/pos sequence: "ESC '*'"
            BitImageWrapper imageWrapper = new BitImageWrapper();
            print.write(imageWrapper, escposImage);*/
            print.writeLF(title, tienda.getNombre());
            print.feed(1);
            print.write(campo, "N. ruc: ");
            print.writeLF(tienda.getRut());
            print.write(campo, "Dirección: ");
            print.writeLF(tienda.getDireccion());
            print.write(campo, "Teléfono: ");
            print.writeLF(tienda.getTelefono());
            print.write(campo, "Fecha: ");
            print.writeLF(datosGenerales.getFecha());
            print.write(campo, "Tipo venta: ");
            print.writeLF(datosGenerales.getTipoVenta());
            print.write(campo, "Atendido por : Cajero #");
            print.writeLF(String.valueOf(datosGenerales.getEmpleado()));
            print.write(campo, "N. factura: #");
            print.writeLF(String.valueOf(datosGenerales.getFactura()));
            print.write(campo, "Comprador: ");
            print.writeLF(datosGenerales.getComprador());
            //print.writeLF(bold, "------------------------------------------------");
            print.writeLF("*".repeat(papelAncho));
            print.write(bold, "Cant");
            print.write(espacioTresColumnas(papelAncho, anchoTitulos));
            print.write(bold, "Precio");
            print.write(espacioTresColumnas(papelAncho, anchoTitulos));
            print.writeLF(bold, "Total");
            print.writeLF("*".repeat(papelAncho));
            //print.writeLF(bold, "------------------------------------------------");
            for (Detalles detalle : detalles) {
                print.writeLF(detalle.getDescripcion());
                print.write(formatDecimal.format(detalle.getCantidadProducto()));
                print.write(" x ");
                //print.write(espacioTresColumnas(papelAncho, espaciadoDetalles));
                print.write(formatDecimal.format(detalle.getPrecioProducto()));
                //print.write(espacioTresColumnas(papelAncho, espaciadoDetalles));
                print.write(" = ");
                print.writeLF(formatDecimal.format(detalle.getImporte()));
                if (detalle.getDescuento() > 0) {
                    print.write("Desc - ");
                    print.write(formatDecimal.format(detalle.getDescuento()) + " ");
                    print.write("PO: ");
                    print.writeLF(formatDecimal.format(detalle.getPrecioVenta()));
                }
                print.writeLF("------------------------------------------------");
            }
            print.write(campo, "Sub C$");
            print.write(espacio(papelAncho, "Sub C$".length(), espacioCantidades(totales.getSubTotalCordobas())));
            print.writeLF(formatDecimal.format(totales.getSubTotalCordobas()));
            print.write(campo, "Sub $");
            print.write(espacio(papelAncho, "Sub $".length(), espacioCantidades(totales.getSubTotalDolares())));
            print.writeLF(formatDecimal.format(totales.getSubTotalDolares()));
            print.write(campo, "Desc C$");
            print.write(espacio(papelAncho, "Desc C$".length(), espacioCantidades(totales.getDescuentoCordobas())));
            print.writeLF(formatDecimal.format(totales.getDescuentoCordobas()));
            print.write(campo, "Desc $");
            print.write(espacio(papelAncho, "Desc $".length(), espacioCantidades(totales.getDescuentoDolares())));
            print.writeLF(formatDecimal.format(totales.getDescuentoDolares()));
            print.write(campo, "Total C$");
            print.write(espacio(papelAncho, "Total C$".length(), espacioCantidades(totales.getTotalCordobas())));
            print.writeLF(formatDecimal.format(totales.getTotalCordobas()));
            print.write(campo, "Total $");
            print.write(espacio(papelAncho, "Total $".length(), espacioCantidades(totales.getTotalDolares())));
            print.writeLF(formatDecimal.format(totales.getTotalDolares()));
            print.writeLF("-------------------- Cambio --------------------");
            print.write(campo, "Recib C$");
            print.write(espacio(papelAncho, "Recib C$".length(), espacioCantidades(totales.getCordobasRecibidos())));
            print.writeLF(formatDecimal.format(totales.getCordobasRecibidos()));
            print.write(campo, "Recib $");
            print.write(espacio(papelAncho, "Recib $".length(), espacioCantidades(totales.getDolaresRecibidos())));
            print.writeLF(formatDecimal.format(totales.getDolaresRecibidos()));
            print.write(campo, "Cambio");
            print.write(espacio(papelAncho, "Cambio".length(), espacioCantidades(totales.getCambio())));
            print.writeLF(formatDecimal.format(totales.getCambio()));
            print.writeLF("------------------------------------------------");
            print.writeLF(nota, tienda.getNota());
            print.feed(1);
            if (copia) {
                print.writeLF("Copia");
            }
            print.feed(4);
            print.cut(EscPos.CutMode.FULL);
            return "Exito en la impresion";
        } catch (ConnectException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            return "Fallo la impresion";
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            return "Error en la impresion";
        } finally {
            try {
                print.close();
            } catch (IOException ex) {
                Logger.getLogger(Printescpos.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }
}
