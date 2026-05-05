package infrastructure.escpos;

import com.github.anastaciocintra.escpos.EscPos;
import com.github.anastaciocintra.escpos.EscPosConst;
import com.github.anastaciocintra.escpos.image.Bitonal;
import com.github.anastaciocintra.escpos.image.BitonalOrderedDither;
import com.github.anastaciocintra.escpos.image.CoffeeImageImpl;
import com.github.anastaciocintra.escpos.image.EscPosImage;
import com.github.anastaciocintra.escpos.image.RasterBitImageWrapper;
import domain.PrinterConfig;
import domain.entities.DatosGenerales;
import domain.entities.Detalles;
import domain.entities.Factura;
import domain.entities.Tienda;
import domain.entities.Totales;
import domain.ports.out.ImpresoraPort;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.text.DecimalFormat;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.xhtmlrenderer.swing.Java2DRenderer;

public class ImprimirFacturaHtml extends AjustesImpresion implements ImpresoraPort<Factura> {

    private final EscposConnectionFactory connectionFactory;
    private final DecimalFormat formatDecimal = new DecimalFormat("###,###,###,##0.00");
    private static final Logger LOGGER = Logger.getLogger(ImprimirFacturaHtml.class.getName());

    private static final int ANCHO_IMAGEN = 576;

    public ImprimirFacturaHtml() {
        this.connectionFactory = new EscposConnectionFactory();
    }

    @Override
    public String imprimir(PrinterConfig config, Factura factura, boolean copias) {
        try (EscPos escpos = connectionFactory.crearConexion(config, config.getTipoConexion())) {

            String htmlCompleto = generarHtmlFactura(factura);
            String urlImagen = "https://api.cdsoft.net/uploads/logos/" + factura.getTienda().getLogo();
            BufferedImage bufferedImage;

            if (config.getLogo() != null) {
                bufferedImage = obtenerImagenLocal(config.getLogo());
            } else {
                bufferedImage = obtenerImagenDeUrl(urlImagen);
            }

            File tempFile = File.createTempFile("factura_", ".html");

            try (FileWriter writer = new FileWriter(tempFile)) {
                writer.write(htmlCompleto);
                LOGGER.info("HTML guardado en: " + tempFile.getAbsolutePath());
            }

            String fileUrl = tempFile.toURI().toURL().toExternalForm();
            String baseUrl = tempFile.getParent();

            Java2DRenderer renderer = new Java2DRenderer(fileUrl, baseUrl, ANCHO_IMAGEN);
            BufferedImage imagen = renderer.getImage();

            Bitonal algorithm = new BitonalOrderedDither();
            EscPosImage escposImage = new EscPosImage(new CoffeeImageImpl(imagen), algorithm);

            RasterBitImageWrapper imageWrapper = new RasterBitImageWrapper()
                    .setJustification(EscPosConst.Justification.Center);

            BufferedImage resizeImage = resizeImage(bufferedImage, 290);
            Bitonal algorithmLogo = new BitonalOrderedDither(3, 3, 120, 170);
            EscPosImage escposImageLogo = new EscPosImage(new CoffeeImageImpl(resizeImage), algorithmLogo);

            RasterBitImageWrapper imageWrapperLogo = new RasterBitImageWrapper().setJustification(EscPosConst.Justification.Center);
            escpos.write(imageWrapperLogo, escposImageLogo).feed(1);
            escpos.write(imageWrapper, escposImage);
            escpos.feed(4);
            escpos.cut(EscPos.CutMode.FULL);

            tempFile.deleteOnExit();

            return "Exito";

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al imprimir factura HTML", e);
            return "Error: " + e.getMessage();
        }
    }

    private String generarHtmlFactura(Factura factura) {
        Tienda tienda = factura.getTienda();
        DatosGenerales datos = factura.getDatosGenerales();
        Totales totales = factura.getTotales();
        List<Detalles> detalles = factura.getDetalles();

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>");
        sb.append("<html><head><meta charset='UTF-8' />");
        sb.append("<style>");
        sb.append(getCssStyles());
        sb.append("</style></head><body>");

        // Header
        sb.append("<header class='header'>");
        sb.append("<h1 class='title-bold'>").append(nullSafe(tienda.getNombre())).append("</h1>");
        sb.append("<div class='header-info'>");
        sb.append("<p>Nº Ruc: ").append(nullSafe(tienda.getRut())).append("</p>");
        sb.append("<p>").append(nullSafe(tienda.getDireccion())).append("</p>");
        sb.append("<p>Teléfono: ").append(nullSafe(tienda.getTelefono())).append("</p>");
        sb.append("</div>");
        sb.append("</header>");

        // Info Venta
        sb.append("<section class='section-info'>");
        sb.append("<div class='row'><span class='label'>FECHA:</span><span>").append(nullSafe(datos.getFecha())).append("</span></div>");
        sb.append("<div class='row'><span class='label'>FACTURA #:</span><span>").append(datos.getFactura()).append("</span></div>");
        sb.append("<div class='row'><span class='label'>CLIENTE:</span><span class='bold'>").append(nullSafe(datos.getCliente())).append("</span></div>");
        sb.append("<div class='row'><span class='label'>VENTA:</span><span class='bold'>").append(nullSafe(datos.getTipoVenta())).append("</span></div>");
        sb.append("<div class='row'><span class='label'>COMPRADOR:</span><span class='bold'>").append(nullSafe(datos.getComprador())).append("</span></div>");
        sb.append("<div class='row'><span class='label'>ATENDIDO:</span><span class='bold'>Cajero# ").append(datos.getEmpleado()).append("</span></div>");
        sb.append("</section>");

        // Tabla de detalles
        sb.append("<table>");
        sb.append("<thead>");

        sb.append("<tr class=''>");
        sb.append("<th class='border-doble-top' colspan='3' />");
        sb.append("</tr>");

        sb.append("<tr class='table-header'>");
        sb.append("<th class='quantity'>Cant.</th>");
        sb.append("<th class='price'>Precio</th>");
        sb.append("<th class='importe'>Total</th>");
        sb.append("</tr>");

        sb.append("<tr class=''>");
        sb.append("<th class='border-doble-top' colspan='3' />");
        sb.append("</tr>");

        sb.append("</thead>");
        sb.append("<tbody>");

        for (Detalles detalle : detalles) {
            sb.append("<tr class='row-item'>");
            sb.append("<td class='description' colspan='3'>").append(nullSafe(detalle.getDescripcion())).append("</td>");
            sb.append("</tr>");
            sb.append("<tr class='row-item'>");
            sb.append("<td class='quantity'>").append(formatDecimal.format(detalle.getCantidadProducto())).append("</td>");
            sb.append("<td class='price'>").append(formatDecimal.format(detalle.getPrecioProducto())).append("</td>");
            sb.append("<td class='importe'>");
            sb.append(detalle.getMonedaVenta().equals("Dolar") ? "$ " : "C$ ");
            sb.append(formatDecimal.format(detalle.getImporte()));
            sb.append("</td>");
            sb.append("</tr>");
            if (detalle.getDescuento() > 0) {
                sb.append("<tr><td class='description border-item' colspan='3'>Desc: ").append(formatDecimal.format(detalle.getDescuento())).append("</td></tr>");
            } else {
                sb.append("<tr><td class='border-item' colspan='3' /></tr>");
            }
        }

        sb.append("</tbody>");
        sb.append("</table>");

        // Totales
        sb.append("<div class='border-doble-top'></div>");
        sb.append("<table>");
        sb.append("<tr class='row-item'><td class='label'>Sub</td><td class='value'>").append("C$ ").append(formatDecimal.format(totales.getSubTotalCordobas())).append("</td></tr>");
        sb.append("<tr class='row-item'><td class='label'>Sub</td><td class='value'>").append("$ ").append(formatDecimal.format(totales.getSubTotalDolares())).append("</td></tr>");
        sb.append("<tr class='row-item'><td class='label'>Desc</td><td class='value'>").append("- C$ ").append(formatDecimal.format(totales.getDescuentoCordobas())).append("</td></tr>");
        sb.append("<tr class='row-item'><td class='label'>Desc</td><td class='value'>").append("- $ ").append(formatDecimal.format(totales.getDescuentoDolares())).append("</td></tr>");
        sb.append("<tr class='row-item totales-row'><td class='label totales'>Total</td><td class='value totales'>").append("C$ ").append(formatDecimal.format(totales.getTotalCordobas())).append("</td></tr>");
        sb.append("<tr class='row-item totales-row'><td class='label totales'>Total</td><td class='value totales'>").append("$ ").append(formatDecimal.format(totales.getTotalDolares())).append("</td></tr>");
        sb.append("</table>");
        sb.append("<div class='border-doble-top'></div>");
        // Globales
        if (totales.getGlobalCordobas() > 0 && totales.getGlobalDolares() > 0) {
            sb.append("<div class='border-globales centered globales'>");
            sb.append("<p class='bold'>Globales: </p>");
            sb.append("<p class='bold'>C$ ").append(formatDecimal.format(totales.getGlobalCordobas()));
            sb.append(" - $ ").append(formatDecimal.format(totales.getGlobalDolares()));
            sb.append("</p>");
            sb.append("</div>");
        }

        // Cambio
        if (totales.getDolaresRecibidos() > 0 || totales.getCordobasRecibidos() > 0) {
            sb.append("<div class='border-doble-top'></div>");
            sb.append("<table>");
            sb.append("<tr class='row-item'><td class='label'>Recibido C$:</td><td class='value'>").append(formatDecimal.format(totales.getCordobasRecibidos())).append("</td></tr>");
            sb.append("<tr class='row-item'><td class='label'>Recibido $:</td><td class='value'>").append(formatDecimal.format(totales.getDolaresRecibidos())).append("</td></tr>");
            sb.append("<tr class='row-item totales-row'><td class='label totales'>Cambio C$:</td><td class='value totales'>").append(formatDecimal.format(totales.getCambio())).append("</td></tr>");
            sb.append("</table>");
        }

        // Nota y mensaje final
        sb.append("<div class='nota'>");
        sb.append("<span class='bold'>Nota: </span>").append("nota de prueba pra ver como queda");
        sb.append("</div>");
        sb.append("<div class='border-doble-top'></div>");
        sb.append("<div class='centered'>").append(nullSafe(tienda.getNota())).append("</div>");

        sb.append("</body></html>");
        return sb.toString();
    }

    private String getCssStyles() {
        return "/* CSS 2.1 Compatible */\n"
                + "\n"
                + "* {\n"
                + "  font-size: 28px;\n"
                + "  font-family: Arial, sans-serif;\n"
                + "  margin: 0;\n"
                + "  padding: 0;\n"
                + "  box-sizing: border-box;\n"
                + "}\n"
                + "\n"
                + "/*----------------------------------------*/\n"
                + "\n"
                + ".header {\n"
                + "    text-align: center;\n"
                + "    margin-bottom: 15px;\n"
                + "    display: block;\n"
                + "}\n"
                + "\n"
                + ".header img {\n"
                + "    width: 128px;\n"
                + "    display: inline-block;\n"
                + "}\n"
                + "\n"
                + ".bold {\n"
                + "    font-weight: bold;\n"
                + "}\n"
                + "\n"
                + ".title-bold {\n"
                + "    font-size: 36px;\n"
                + "    font-weight: bold;\n"
                + "    margin: 0;\n"
                + "    display: block;\n"
                + "}\n"
                + "\n"
                + ".header-info p {\n"
                + "    margin: 2px 0;\n"
                + "    font-size: 28px;\n"
                + "}\n"
                + "\n"
                + ".section-info {\n"
                + "    margin-bottom: 10px;\n"
                + "    display: block;\n"
                + "    clear: both;\n"
                + "}\n"
                + "\n"
                + "/* Reemplazo de Flexbox por Floats */\n"
                + ".row, .row-item, .table-header {\n"
                + "    width: 100%;\n"
                + "    margin-bottom: 2px;\n"
                + "    display: block;\n"
                + "    clear: both;\n"
                + "    overflow: hidden;\n"
                + "}\n"
                + "\n"
                + ".row-item td.label {\n"
                + "    float: left;\n"
                + "    text-align: left;\n"
                + "    width: 150px;\n"
                + "}\n"
                + "\n"
                + ".row-item td.value {\n"
                + "    float: right;\n"
                + "    text-align: right;\n"
                + "    width: 426px;\n"
                + "}\n"
                + "\n"
                + ".row-item.totales-row td.label,\n"
                + ".row-item.totales-row td.value {\n"
                + "    font-weight: bold;\n"
                + "    font-size: 32px;\n"
                + "}\n"
                + "\n"
                + "/* En CSS 2.1 justify-content: space-between se hace con floats opuestos */\n"
                + ".row span:first-child, .label {\n"
                + "    float: left;\n"
                + "    width: 27mm;\n"
                + "    text-align: left;\n"
                + "}\n"
                + "\n"
                + ".row span:last-child {\n"
                + "    float: right;\n"
                + "    text-align: right;\n"
                + "    word-wrap: break-word;\n"
                + "}\n"
                + "\n"
                + "/*----------------------------------------*/\n"
                + "\n"
                + ".border-globales {\n"
                + "    border: 3px dotted black;\n"
                + "}\n"
                + "\n"
                + ".centered {\n"
                + "    text-align: center;\n"
                + "    display: block;\n"
                + "}\n"
                + "\n"
                + "/* Reemplazo de Grid para centrar */\n"
                + ".centered img {\n"
                + "    margin-left: auto;\n"
                + "    margin-right: auto;\n"
                + "    display: block;\n"
                + "}\n"
                + "\n"
                + ".both_border {\n"
                + "    border-top: 2px solid black;\n"
                + "    border-bottom: 2px solid black;\n"
                + "}\n"
                + "\n"
                + ".border-item {\n"
                + "    border-bottom: 1px solid black;\n"
                + "}\n"
                + "\n"
                + "/* Estilos de Tabla Tradicional */\n"
                + "table {\n"
                + "    width: 100%;\n"
                + "    border-collapse: collapse;\n"
                + "}\n"
                + "\n"
                + "td.description,\n"
                + "th.description {\n"
                + "    width: 100%;\n"
                + "    text-align: left;\n"
                + "    padding: 4px;\n"
                + "}\n"
                + "\n"
                + "td.quantity,\n"
                + "th.quantity {\n"
                + "    width: 150px;\n"
                + "    text-align: center;\n"
                + "}\n"
                + "\n"
                + "td.price,\n"
                + "th.price {\n"
                + "    width: 200px;\n"
                + "    text-align: right;\n"
                + "}\n"
                + "\n"
                + "td.importe,\n"
                + "th.importe {\n"
                + "    width: 227px;\n"
                + "    text-align: right;\n"
                + "}\n"
                + "\n"
                + "td.totales {\n"
                + "    font-size: 32px;\n"
                + "    font-weight: bold;\n"
                + "}\n"
                + "globales {\n"
                + "    margin-top: 15px;\n"
                + "    font-size: 30px;\n"
                + "    text-align: center;\n"
                + "}\n"
                + "globales p{\n"
                + "    font-weight: bold;\n"
                + "}\n"
                + "\n"
                + ".border-doble-top {\n"
                + "    border-top: 2px solid black;\n"
                + "}\n"
                + "\n"
                + ".border-doble-bottom {\n"
                + "    border-bottom: 2px solid black;\n"
                + "}\n"
                + "\n"
                + ".nota {\n"
                + "    border-bottom: 1px solid black;\n"
                + "    padding: 10px 0;\n"
                + "    display: block;\n"
                + "    clear: both;\n"
                + "}\n"
                + "small {\n"
                + "    font-size: 10px;\n"
                + "}\n";
    }

    private String nullSafe(String valor) {
        return valor != null ? valor : "";
    }
}
