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

	public ImprimirFacturaHtml() {
		this.connectionFactory = new EscposConnectionFactory();
	}

	@Override
	public String imprimir(PrinterConfig config, Factura factura, boolean copias) {
		try (EscPos escpos = connectionFactory.crearConexion(config, config.getTipoConexion())) {

			BufferedImage bufferedImage;
			papelAncho = config.getPapelSize() * 12;

			/* Generar documento HTML*/
			String htmlCompleto = generarHtmlFactura(factura);

			/* Obtener logo bien desde online o local */
			String urlImagen = "https://api.cdsoft.net/uploads/logos/" + factura.getTienda().getLogo();
			if (config.getLogo() != null) {
				bufferedImage = obtenerImagenLocal(config.getLogo());
			} else {
				bufferedImage = obtenerImagenDeUrl(urlImagen);
			}

			/* Creamos el archivo temporal en /user/AppData/Local/temp/ */
			File tempFile = File.createTempFile("factura_", ".html");
			try (FileWriter writer = new FileWriter(tempFile)) {
				writer.write(htmlCompleto);
				LOGGER.info("HTML guardado en: " + tempFile.getAbsolutePath());
			}

			String fileUrl = tempFile.toURI().toURL().toExternalForm();
			String baseUrl = tempFile.getParent();

			/* Renderizamos el documente a imagen */
			Java2DRenderer renderer = new Java2DRenderer(fileUrl, baseUrl, papelAncho);
			BufferedImage imagen = renderer.getImage();

			/* Eliminamos el documento creado, para evitar acumulacion de documentos en disco */
			tempFile.delete();

			/*
				BitonalOrderedDither(3, 3, 128, 170)
				parametro 120 es de 0 negro a 255 blanco - actualmente 128 es un color neutro bien para impresoras termicas
				parametro 170 y 128 son la profundida de bits afecta los como se ven los colores intermedios
			 */
			Bitonal algorithm = new BitonalOrderedDither(3, 3, 128, 170);
			Bitonal algorithmLogo = new BitonalOrderedDither(3, 3, 50, 200);
			BufferedImage resizeImage = resizeImage(bufferedImage, 290);

			EscPosImage escposImage = new EscPosImage(new CoffeeImageImpl(imagen), algorithm);
			EscPosImage escposImageLogo = new EscPosImage(new CoffeeImageImpl(resizeImage), algorithmLogo);

			RasterBitImageWrapper facturaWrapper = new RasterBitImageWrapper().setJustification(EscPosConst.Justification.Center);
			RasterBitImageWrapper imageWrapperLogo = new RasterBitImageWrapper().setJustification(EscPosConst.Justification.Center);

			escpos.write(imageWrapperLogo, escposImageLogo).feed(1);
			escpos.write(facturaWrapper, escposImage);
			escpos.feed(4);
			escpos.cut(EscPos.CutMode.FULL);

			// Cuando se cierre el servicio eliminara los documentos temporales creados anteriormene
			// tempFile.deleteOnExit();
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

		// HEADER
		sb.append("<header class='header'>");
		sb.append("<h1 class='title-bold'>").append(nullSafe(tienda.getNombre())).append("</h1>");
		sb.append("<div class='header-info'>");
		sb.append("<p>Nº Ruc: ").append(nullSafe(tienda.getRut())).append("</p>");
		sb.append("<p>").append(nullSafe(tienda.getDireccion())).append("</p>");
		sb.append("<p>Teléfono: ").append(nullSafe(tienda.getTelefono())).append("</p>");
		sb.append("</div>");
		sb.append("</header>");

		// INFO VENTA
		sb.append("<section class='section-info'>");
		sb.append("<div class='row'><span class='label'>FECHA:</span><span class='value'>").append(nullSafe(datos.getFecha())).append("</span></div>");
		sb.append("<div class='row'><span class='label'>FACTURA:</span><span class='value'># ").append(datos.getFactura()).append("</span></div>");
		if (!nullSafe(datos.getCliente()).equals("")) {
			sb.append("<div class='row'><span class='label'>CLIENTE:</span><span class='bold value'>").append(nullSafe(datos.getCliente())).append("</span></div>");
		}
		sb.append("<div class='row'><span class='label'>VENTA:</span><span class='bold value'>").append(nullSafe(datos.getTipoVenta())).append("</span></div>");
		sb.append("<div class='row'><span class='label'>COMPRADOR:</span><span class='bold value'>").append(nullSafe(datos.getComprador())).append("</span></div>");
		sb.append("<div class='row'><span class='label'>ATENDIDO:</span><span class='bold value'>Cajero# ").append(datos.getEmpleado()).append("</span></div>");
		sb.append("</section>");

		// TABLA DE DETALLES
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
				sb.append("<tr class='row-item'><td class='quantity'>Desc: </td>");
				sb.append("<td class='price'>").append(formatDecimal.format(detalle.getDescuento())).append("</td>");
				sb.append("<td class='importe'>").append(formatDecimal.format(detalle.getPrecioVenta())).append("</td></tr>");
				sb.append("<tr><td class='border-item' colspan='3' /></tr>");
			} else {
				sb.append("<tr><td class='border-item' colspan='3' /></tr>");
			}
		}

		sb.append("</tbody>");
		sb.append("</table>");

		// TOTALES
		sb.append("<div class='border-doble-top'></div>");
		sb.append("<table>");

		if (totales.getDescuentoCordobas() > 0) {
			sb.append("<tr class='row-item'><td class='label'>Sub</td><td class='value bold'>").append("C$ ").append(formatDecimal.format(totales.getSubTotalCordobas())).append("</td></tr>");
		}
		if (totales.getDescuentoDolares() > 0) {
			sb.append("<tr class='row-item'><td class='label'>Sub</td><td class='value bold'>").append("$ ").append(formatDecimal.format(totales.getSubTotalDolares())).append("</td></tr>");
		}
		if (totales.getDescuentoCordobas() > 0) {
			sb.append("<tr class='row-item'><td class='label'>Desc</td><td class='value bold'>").append("- C$ ").append(formatDecimal.format(totales.getDescuentoCordobas())).append("</td></tr>");
		}
		if (totales.getDescuentoDolares() > 0) {
			sb.append("<tr class='row-item'><td class='label'>Desc</td><td class='value bold'>").append("- $ ").append(formatDecimal.format(totales.getDescuentoDolares())).append("</td></tr>");
		}
		if (totales.getTotalCordobas() > 0) {
			sb.append("<tr class='row-item totales-row'><td class='label totales'>Total</td><td class='value totales'>").append("C$ ").append(formatDecimal.format(totales.getTotalCordobas())).append("</td></tr>");
		}
		if (totales.getTotalDolares() > 0) {
			sb.append("<tr class='row-item totales-row'><td class='label totales'>Total</td><td class='value totales'>").append("$ ").append(formatDecimal.format(totales.getTotalDolares())).append("</td></tr>");
		}

		sb.append("</table>");

		// GLOBALES
		if (totales.getGlobalCordobas() > 0 && totales.getGlobalDolares() > 0) {
			sb.append("<div class='border-doble-top'></div>");
			sb.append("<div class='border-globales centered globales'>");
			sb.append("<p class='bold'>Globales: </p>");
			sb.append("<p class='bold'>C$ ").append(formatDecimal.format(totales.getGlobalCordobas()));
			sb.append(" - $ ").append(formatDecimal.format(totales.getGlobalDolares()));
			sb.append("</p>");
			sb.append("</div>");
		}

		// CAMBIO
		if (totales.getDolaresRecibidos() > 0 || totales.getCordobasRecibidos() > 0) {
			sb.append("<div class='border-doble-top'></div>");
			sb.append("<table>");
			sb.append("<tr class='row-item'><td class='label'>Recibido C$</td><td class='value'>").append(formatDecimal.format(totales.getCordobasRecibidos())).append("</td></tr>");
			sb.append("<tr class='row-item'><td class='label'>Recibido $</td><td class='value'>").append(formatDecimal.format(totales.getDolaresRecibidos())).append("</td></tr>");
			sb.append("<tr class='row-item totales-row'><td class='label bold'>Cambio C$</td><td class='value bold'>").append(formatDecimal.format(totales.getCambio())).append("</td></tr>");
			sb.append("</table>");
		}

		if (!nullSafe(factura.getDatosGenerales().getAnotaciones()).equals("")) {
			sb.append("<div class='border-doble-top'></div>");
			sb.append("<div class='nota'>");
			sb.append("<span class='bold'>Nota: </span>").append(nullSafe(factura.getDatosGenerales().getAnotaciones()));
			sb.append("</div>");
		}
		sb.append("<div class='border-doble-top'></div>");
		sb.append("<div class='centered'>").append(nullSafe(tienda.getNota())).append("</div>");

		sb.append("</body></html>");
		return sb.toString();
	}

	private String getCssStyles() {
		return """
					 /* CSS 2.1 Compatible */
					 
					 * {
					   font-size: 29px;
					   font-family: Arial, sans-serif;
					   margin: 0;
					   padding: 0;
					   box-sizing: border-box;
					 }
					 
					 .header {
					     text-align: center;
					     margin-bottom: 15px;
					     display: block;
					 }
					 
					 .bold {
					     font-weight: bold;
					 }
					 
					 .title-bold {
					     font-size: 36px;
					     font-weight: bold;
					     margin: 0;
					     display: block;
					 }
					 
					 .header-info p {
					     margin: 2px 0;
					     font-size: 28px;
					 }
					 
					 .section-info {
					     margin-bottom: 10px;
					     display: block;
					     clear: both;
					 }
					 
					 /* Reemplazo de Flexbox por Floats */
					 .row, .row-item, .table-header {
					     width: 100%;
					     margin-bottom: 2px;
					     display: block;
					     clear: both;
					     overflow: hidden;
					 }
					 
					 .row-item td.label {
					     float: left;
					     text-align: left;
					     width: 170px;
					 }
					 
					 .row-item td.value {
					     float: right;
					     text-align: right;
					     width: 406px;
					 }
					 
					 .row-item.totales-row td.label,
					 .row-item.totales-row td.value {
					     font-weight: bold;
					     font-size: 32px;
					 }
					 
					 /* En CSS 2.1 justify-content: space-between se hace con floats opuestos */
					 .row span:first-child, .label {
					     float: left;
					     width: 27mm;
					     text-align: left;
					 }
         
           .row span:last-child, .value {
               float: right;
               text-align: right;
               word-wrap: break-word;
           }
					 
					 /*----------------------------------------*/
					 
					 .border-globales {
					     border: 3px dotted black;
					 }
					 
					 .centered {
					     text-align: center;
					     display: block;
					 }
					 
					 /* Reemplazo de Grid para centrar */
					 .centered img {
					     margin-left: auto;
					     margin-right: auto;
					     display: block;
					 }
					 
					 .both_border {
					     border-top: 2px solid black;
					     border-bottom: 2px solid black;
					 }
					 
					 /* Estilos de Tabla Tradicional */
					 table {
					     width: 100%;
					     border-collapse: collapse;
					 }
					 
					 table tr td {
					     font-size: 30px;
					 }
					 td.description,
					 th.description {
					     width: 100%;
					     text-align: left;
					     padding: 4px;
					 }
					 
					 td.quantity,
					 th.quantity {
					     width: 150px;
					     text-align: center;
					 }
					 
					 td.price,
					 th.price {
					     width: 200px;
					     text-align: right;
					 }
					 
					 td.importe,
					 th.importe {
					     width: 227px;
					     text-align: right;
					 }
					 
					 td.totales {
					     font-size: 32px;
					     font-weight: bold;
					 }

					 .globales {
					     margin-top: 10px;
					     margin-bottom: 10px;
					     font-size: 30px;
					     text-align: center;
					 }

					 .globales p{
					     font-weight: bold;
					 }
					 
					 .border-doble-top {
					     border-top: 2px solid black;
					 }
					 
					 .border-doble-bottom {
					     border-bottom: 2px solid black;
					 }
					 
					 .border-item {
					     border-bottom: 1px solid black;
					 }
					 
					 .nota {
					     border-bottom: 1px solid black;
					     padding: 10px 0;
					     display: block;
					     clear: both;
					 }

					 small {
					     font-size: 10px;
					 }
					 """;
	}

	private String nullSafe(String valor) {
		return valor != null ? valor : "";
	}
}
