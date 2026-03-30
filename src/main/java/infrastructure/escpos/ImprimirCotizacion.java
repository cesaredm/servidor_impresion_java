package infrastructure.escpos;

import com.github.anastaciocintra.escpos.EscPos;
import com.github.anastaciocintra.escpos.EscPosConst;
import com.github.anastaciocintra.escpos.image.Bitonal;
import com.github.anastaciocintra.escpos.image.BitonalOrderedDither;
import com.github.anastaciocintra.escpos.image.CoffeeImageImpl;
import com.github.anastaciocintra.escpos.image.EscPosImage;
import com.github.anastaciocintra.escpos.image.RasterBitImageWrapper;
import static infrastructure.escpos.AjustesImpresion.texto;
import domain.entities.Cotizacion;
import domain.entities.DatosGeneralesCotizacion;
import domain.entities.Detalles;
import domain.entities.Tienda;
import domain.entities.Totales;
import domain.ports.out.ImpresoraPort;
import domain.PrinterConfig;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.ConnectException;
import java.util.List;
import java.util.logging.Level;

public class ImprimirCotizacion extends AjustesImpresion implements ImpresoraPort<Cotizacion> {

    private final EscposConnectionFactory connectionFactory;

    public ImprimirCotizacion() {
        this.connectionFactory = new EscposConnectionFactory();
    }

    @Override
    public String imprimir(PrinterConfig config, Cotizacion cotizacion, boolean copias) {
        List<Detalles> detalles = cotizacion.getDetalles();
        Tienda tienda = cotizacion.getTienda();
        DatosGeneralesCotizacion datosGenerales = cotizacion.getDatosGenerales();
        Totales totales = cotizacion.getTotales();
        papelAncho = config.getPapelSize();

        try (EscPos print = connectionFactory.crearConexion(config, config.getTipoConexion())) {
            String urlImagen = "https://api.cdsoft.net/uploads/logos/" + tienda.getLogo();
            BufferedImage bufferedImage;
            try {
                if (config.getLogo() != null) {
                    bufferedImage = obtenerImagenLocal(config.getLogo());
                } else {
                    bufferedImage = obtenerImagenDeUrl(urlImagen);
                }

                BufferedImage resizeImage = resizeImage(bufferedImage, 290);
                Bitonal algorithm = new BitonalOrderedDither(3, 3, 120, 170);
                EscPosImage escposImage = new EscPosImage(new CoffeeImageImpl(resizeImage), algorithm);

                RasterBitImageWrapper imageWrapper = new RasterBitImageWrapper().setJustification(EscPosConst.Justification.Center);
                print.write(imageWrapper, escposImage).feed(1);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "No se encontro el logo, por lo tanto no lo imprimira");
            }

            if (tienda.getNombre() != null && !tienda.getNombre().isEmpty()) {
                print.writeLF(title, tienda.getNombre());
            }

            print.feed(1);
            print.writeLF(subTitle, "Proforma");
            print.feed(1);

            if (tienda.getRut() != null && !tienda.getRut().isEmpty()) {
                print.write(campo, "N. ruc: ");
                print.writeLF(tienda.getRut());
            }

            print.write(campo, texto("Lugar: "));
            print.writeLF(tienda.getDireccion());
            print.write(campo, texto("Celular: "));
            print.writeLF(tienda.getTelefono());
            print.write(campo, "Fecha: ");
            print.writeLF(datosGenerales.getFecha());
            print.write(campo, "Atendido por : Cajero #");
            print.writeLF(String.valueOf(datosGenerales.getEmpleado()));
            print.write(campo, "N. Proforma: #");
            print.writeLF(String.valueOf(datosGenerales.getNumeroCorrelativo()));
            print.write(campo, "Comprador: ");
            print.writeLF(texto(datosGenerales.getCliente()));
            print.writeLF("*".repeat(papelAncho));
            print.write(bold, "Cant");
            print.write(espacioTresColumnas(papelAncho, anchoTitulos));
            print.write(bold, "Precio");
            print.write(espacioTresColumnas(papelAncho, anchoTitulos));
            print.writeLF(bold, "Total");

            print.writeLF("*".repeat(papelAncho));
            for (Detalles detalle : detalles) {
                print.writeLF(texto(detalle.getDescripcion()));
                print.write(formatDecimal.format(detalle.getCantidadProducto()));
                print.write(" x ");
                print.write(formatDecimal.format(detalle.getPrecioProducto()));
                print.write(" = ");
                print.write(detalle.getMonedaVenta().equals("Dolar") ? " $ " : " C$ ");
                print.writeLF(formatDecimal.format(detalle.getImporte()));
                if (detalle.getDescuento() > 0) {
                    print.write("Desc - ");
                    print.write(formatDecimal.format(detalle.getDescuento()) + " ");
                    print.write(campo, "PO: ");
                    print.writeLF(formatDecimal.format(detalle.getPrecioVenta()));
                }
                print.writeLF("-".repeat(papelAncho));
            }

            print.write(campo, "Sub C$");
            print.write(espacio(papelAncho, "Sub C$".length(), espacioCantidades(totales.getSubTotalCordobas())));
            print.writeLF(bold, formatDecimal.format(totales.getSubTotalCordobas()));
            print.write(campo, "Sub $");
            print.write(espacio(papelAncho, "Sub $".length(), espacioCantidades(totales.getSubTotalDolares())));
            print.writeLF(bold, formatDecimal.format(totales.getSubTotalDolares()));
            print.write(campo, "Desc C$");
            print.write(espacio(papelAncho, "Desc C$".length(), espacioCantidades(totales.getDescuentoCordobas())));
            print.writeLF(bold, formatDecimal.format(totales.getDescuentoCordobas()));
            print.write(campo, "Desc $");
            print.write(espacio(papelAncho, "Desc $".length(), espacioCantidades(totales.getDescuentoDolares())));
            print.writeLF(bold, formatDecimal.format(totales.getDescuentoDolares()));
            print.write(campo, "Total C$");
            print.write(espacio(papelAncho, "Total C$".length(), espacioCantidades(totales.getTotalCordobas())));
            print.writeLF(bold, formatDecimal.format(totales.getTotalCordobas()));
            print.write(campo, "Total $");
            print.write(espacio(papelAncho, "Total $".length(), espacioCantidades(totales.getTotalDolares())));
            print.writeLF(bold, formatDecimal.format(totales.getTotalDolares()));

            if (totales.getGlobalCordobas() > 0 && totales.getGlobalDolares() > 0) {
                print.writeLF(tituloConLineaPunteada(" Globales ", papelAncho));
                print.writeLF(bold, "C$ " + formatDecimal.format(totales.getGlobalCordobas()) + " -  $ " + formatDecimal.format(totales.getGlobalDolares()));
            }

            print.writeLF("-".repeat(papelAncho));
            print.writeLF(nota, tienda.getNota());
            print.feed(1);
            if (copias) {
                print.writeLF("Copia");
            }
            print.feed(4);
            print.cut(EscPos.CutMode.FULL);
            return "Exito";

        } catch (ConnectException ex) {
            LOGGER.log(Level.SEVERE, null, "No se pudo conectar a la impresra, revise la configuracion " + ex);
            return "Fallo la impresion";
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, "Error en la impresion " + ex);
            return "Error en la impresion";
        }
    }
}