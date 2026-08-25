package infrastructure.escpos;

import com.github.anastaciocintra.escpos.EscPos;
import com.github.anastaciocintra.escpos.EscPosConst;
import com.github.anastaciocintra.escpos.image.Bitonal;
import com.github.anastaciocintra.escpos.image.BitonalOrderedDither;
import com.github.anastaciocintra.escpos.image.CoffeeImageImpl;
import com.github.anastaciocintra.escpos.image.EscPosImage;
import com.github.anastaciocintra.escpos.image.RasterBitImageWrapper;
import domain.entities.DatosGeneralesKardex;
import domain.entities.PrinterConfig;
import domain.exceptions.ImpresoraNoConectadaException;
import domain.exceptions.ImpresoraNoInstaladaException;
import domain.ports.out.ImpresoraPort;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.xhtmlrenderer.swing.Java2DRenderer;

public class ImprimirKardexHtml extends AjustesImpresion implements ImpresoraPort<DatosGeneralesKardex> {
    private static final Logger LOGGER = Logger.getLogger(ImprimirKardexHtml.class.getName());
    private final EscposConnectionFactory connectionFactory = new EscposConnectionFactory();

    @Override
    public String imprimir(PrinterConfig config, DatosGeneralesKardex datos, boolean copias) {
        File tempFile = null;
        try (EscPos escpos = connectionFactory.crearConexion(config, config.getTipoConexion())) {
            papelAncho = config.getPapelSize() * 12;
            tempFile = File.createTempFile("kardex_", ".html");
            try (FileWriter writer = new FileWriter(tempFile)) {
                writer.write(generarHtmlKardex(datos));
            }

            String fileUrl = tempFile.toURI().toURL().toExternalForm();
            Java2DRenderer renderer = new Java2DRenderer(fileUrl, tempFile.getParent(), papelAncho);
            BufferedImage imagen = recortarEspacioBlanco(renderer.getImage());

            Bitonal algorithm = new BitonalOrderedDither(3, 3, 128, 170);
            EscPosImage escposImage = new EscPosImage(new CoffeeImageImpl(imagen), algorithm);
            RasterBitImageWrapper wrapper = new RasterBitImageWrapper()
                    .setJustification(EscPosConst.Justification.Center);
            escpos.write(wrapper, escposImage);
            escpos.feed(4);
            escpos.cut(EscPos.CutMode.FULL);
						escpos.close();
            return "Exito";
        } catch (ImpresoraNoInstaladaException ex) {
            LOGGER.log(Level.SEVERE, "Impresora no instalada: {0}", ex.getNombreImpresora());
            return ex.getUserMessage();
        } catch (ImpresoraNoConectadaException ex) {
            LOGGER.log(Level.SEVERE, "No se pudo conectar a la impresora: {0}:{1}",
                    new Object[]{ex.getIp(), ex.getPuerto()});
            return ex.getUserMessage();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Error en la impresion de Kardex: {0}", ex.getMessage());
            return "Error de comunicación con la impresora: " + ex.getMessage();
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error al imprimir comprobante de Kardex", ex);
            return "Error: " + ex.getMessage();
        } finally {
            if (tempFile != null && tempFile.exists() && !tempFile.delete()) {
                tempFile.deleteOnExit();
            }
        }
    }

    private String generarHtmlKardex(DatosGeneralesKardex datos) {
        String colaborador = !vacio(datos.getNombreEmpleado())
                ? datos.getNombreEmpleado()
                : !vacio(datos.getEmpleado()) ? datos.getEmpleado() : "No disponible";

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n")
                .append("<html><head><meta charset='UTF-8'/>\n")
                .append("<style>\n")
                .append(getCssStyles())
                .append("</style></head>\n<body>\n")
                .append("<div class='ticket-kardex'>\n");

        sb.append("  <header class='ticket-kardex__header'>\n")
                .append("    <div class='ticket-kardex__eyebrow'>INVENTARIO / KARDEX</div>\n")
                .append("    <h1 class='ticket-kardex__title'>COMPROBANTE DE MOVIMIENTO</h1>\n")
                .append("  </header>\n");

        sb.append("  <div class='ticket-kardex__movement'>\n")
                .append("    <table class='ticket-kardex__row-table'>\n")
                .append("      <tr>\n")
                .append("        <td class='ticket-kardex__movement-label'>TIPO DE MOVIMIENTO</td>\n")
                .append("        <td class='ticket-kardex__movement-value'>")
                .append(escape(datos.getTipoMovimiento()))
                .append("</td>\n      </tr>\n    </table>\n")
                .append("    <div class='ticket-kardex__product'>")
                .append(escape(datos.getProducto()))
                .append("</div>\n  </div>\n");

        sb.append("  <table class='ticket-kardex__summary'>\n    <tr>\n")
                .append("      <td>\n        <div class='ticket-kardex__summary-item'>\n")
                .append("          <div class='ticket-kardex__label'>CANTIDAD</div>\n")
                .append("          <strong class='ticket-kardex__summary-value'>")
                .append(formatDecimal.format(datos.getCantidad()))
                .append("</strong>\n        </div>\n      </td>\n")
                .append("      <td>\n        <div class='ticket-kardex__summary-item'>\n")
                .append("          <div class='ticket-kardex__label'>LUGAR</div>\n")
                .append("          <strong class='ticket-kardex__place-value'>")
                .append(escape(datos.getLugar()))
                .append("</strong>\n        </div>\n      </td>\n")
                .append("    </tr>\n  </table>\n");

        sb.append("  <table class='ticket-kardex__details'>\n")
                .append(detail("FECHA", datos.getFecha()))
                .append(detail("VENCIMIENTO", vacio(datos.getFechaVencimiento()) ? "No aplica" : datos.getFechaVencimiento()))
                .append(detail("COLABORADOR", colaborador))
                .append("  </table>\n");

        sb.append("  <div class='ticket-kardex__note'>\n")
                .append("    <strong class='ticket-kardex__note-title'>Nota</strong>\n    <span>")
                .append(escape(vacio(datos.getNota()) ? "Sin nota registrada" : datos.getNota()))
                .append("</span>\n  </div>\n</div>\n</body></html>");
        return sb.toString();
    }

    private String detail(String label, String value) {
        String valueClass = "FECHA".equals(label)
                ? "ticket-kardex__detail-value ticket-kardex__date-value"
                : "ticket-kardex__detail-value";
        return "    <tr class='ticket-kardex__detail-row'>\n"
                + "      <td class='ticket-kardex__label'>" + label + "</td>\n"
                + "      <td class='" + valueClass + "'>" + escape(value) + "</td>\n"
                + "    </tr>\n";
    }

    private String getCssStyles() {
        return """
                * {
                    font-family: Arial, sans-serif;
                    font-size: 29px;
                    margin: 0;
                    padding: 0;
                }

                body,
                .ticket-kardex,
                .ticket-kardex__header,
                .ticket-kardex__movement,
                .ticket-kardex__note {
                    display: block;
                }

                .ticket-kardex__header {
                    text-align: center;
                    margin-bottom: 14px;
                }

                .ticket-kardex__eyebrow {
                    font-size: 22px;
                    letter-spacing: 3px;
                }

                .ticket-kardex__title {
                    width: 90%;
                    font-size: 34px;
                    line-height: 38px;
                    font-weight: bold;
                    margin: 6px auto 0;
                }

                .ticket-kardex__movement {
                    border: 2px solid black;
                    border-radius: 12px;
                    padding: 13px 16px;
                    margin-bottom: 14px;
                }

                .ticket-kardex__row-table,
                .ticket-kardex__details,
                .ticket-kardex__summary {
                    width: 100%;
                    border-collapse: collapse;
                    table-layout: fixed;
                }

                .ticket-kardex__movement-label {
                    width: 48%;
                    font-size: 22px;
                    white-space: nowrap;
                    text-align: left;
                    vertical-align: top;
                }

                .ticket-kardex__label {
                    width: 48%;
                    font-size: 22px;
                    text-align: left;
                    vertical-align: top;
                }

                .ticket-kardex__movement-value,
                .ticket-kardex__detail-value {
                    width: 52%;
                    text-align: right;
                    vertical-align: top;
                    font-weight: bold;
                    word-wrap: break-word;
                }

                .ticket-kardex__movement-value {
                    text-transform: uppercase;
                }

                .ticket-kardex__date-value {
                    white-space: nowrap;
                    font-size: 23px;
                }

                .ticket-kardex__product {
                    font-size: 31px;
                    font-weight: bold;
                    margin-top: 10px;
                    word-wrap: break-word;
                }

                .ticket-kardex__summary {
                    margin-bottom: 12px;
                }

                .ticket-kardex__summary td {
                    width: 50%;
                    padding: 0 5px;
                    vertical-align: top;
                }

                .ticket-kardex__summary td:first-child {
                    padding-left: 0;
                }

                .ticket-kardex__summary td:last-child {
                    padding-right: 0;
                }

                .ticket-kardex__summary-item {
                    text-align: left;
                    border: 2px solid black;
                    border-radius: 10px;
                    padding: 12px 14px;
                    min-height: 92px;
                }

                .ticket-kardex__summary-value,
                .ticket-kardex__place-value {
                    display: block;
                    font-size: 34px;
                    margin-top: 5px;
                    word-wrap: break-word;
                }

                .ticket-kardex__details {
                    border-top: 2px dotted black;
                    border-bottom: 2px dotted black;
                    padding: 12px 0 7px;
                }

                .ticket-kardex__detail-row td {
                    padding-bottom: 7px;
                }

                .ticket-kardex__note {
                    border: 2px solid black;
                    border-radius: 10px;
                    padding: 13px 16px;
                    margin-top: 14px;
                    word-wrap: break-word;
                }

                .ticket-kardex__note-title {
                    display: block;
                    margin-bottom: 5px;
                }
                """;
    }

    private String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private boolean vacio(String value) {
        return value == null || value.isBlank();
    }

    private BufferedImage recortarEspacioBlanco(BufferedImage imagen) {
        int minX = imagen.getWidth();
        int minY = imagen.getHeight();
        int maxX = -1;
        int maxY = -1;

        for (int y = 0; y < imagen.getHeight(); y++) {
            for (int x = 0; x < imagen.getWidth(); x++) {
                int rgb = imagen.getRGB(x, y);
                int alpha = (rgb >>> 24) & 0xff;
                int rojo = (rgb >>> 16) & 0xff;
                int verde = (rgb >>> 8) & 0xff;
                int azul = rgb & 0xff;

                if (alpha > 0 && (rojo < 245 || verde < 245 || azul < 245)) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }

        if (maxX < 0) {
            return imagen;
        }

        int margen = 8;
        int inicioX = Math.max(0, minX - margen);
        int inicioY = Math.max(0, minY - margen);
        int finX = Math.min(imagen.getWidth(), maxX + margen + 1);
        int finY = Math.min(imagen.getHeight(), maxY + margen + 1);
        return imagen.getSubimage(inicioX, inicioY, finX - inicioX, finY - inicioY);
    }

}
