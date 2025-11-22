package infra;

import com.github.anastaciocintra.escpos.EscPos;
import com.github.anastaciocintra.escpos.EscPosConst;
import com.github.anastaciocintra.escpos.image.Bitonal;
import com.github.anastaciocintra.escpos.image.BitonalOrderedDither;
import com.github.anastaciocintra.escpos.image.CoffeeImageImpl;
import com.github.anastaciocintra.escpos.image.EscPosImage;
import com.github.anastaciocintra.escpos.image.RasterBitImageWrapper;
import com.github.anastaciocintra.output.PrinterOutputStream;
import com.github.anastaciocintra.output.TcpIpOutputStream;
import domain.AjustesImpresion;
import static domain.AjustesImpresion.texto;
import domain.Impresora;
import entities.Cotizacion;
import entities.DatosGeneralesCotizacion;
import entities.DetallesCotizacion;
import entities.Tienda;
import entities.Totales;
import httpHandle.PrinterConfig;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.ConnectException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.print.PrintService;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author cesar
 */
public class ImpresionCotizacion extends AjustesImpresion implements Impresora<Cotizacion> {
    private EscPos imprimirCotizacion(PrinterConfig impresora) throws IOException{
        if(impresora.getTipoConexion().equals("red")){
            TcpIpOutputStream red = new TcpIpOutputStream(impresora.getIp(), impresora.getPuerto());
            return new EscPos(red);
        }
        
        if(impresora.getTipoConexion().equals("usb")){
            PrintService printService = PrinterOutputStream.getPrintServiceByName(impresora.getNombre());
            PrinterOutputStream usb = new PrinterOutputStream(printService);
            return new EscPos(usb);
        }
        return null;
    }

    @Override
    public String imprimir(PrinterConfig impresora, Cotizacion cotizacion, boolean copias) {
      List<DetallesCotizacion> detalles = cotizacion.getDetalles();
        Tienda tienda = cotizacion.getTienda();
        DatosGeneralesCotizacion datosGenerales = cotizacion.getDatosGenerales();
        Totales totales = cotizacion.getTotales();
        papelAncho = impresora.getPapelSize();
        //Crear conexion hacia la impresora
        try {
             EscPos print = imprimirCotizacion(impresora);
            // para que acepte los acentos
            //print.setCharacterCodeTable(EscPos.CharacterCodeTable.CP437_USA_Standard_Europe);
            //print.setPrinterCharacterTable(2);
            //print.setCharsetName("UTF-8");
            //String urlImagen = "https://api.cdsoft.net/uploads/logos/1745804389513-CDsoft.png";
            String urlImagen = "https://api.cdsoft.net/uploads/logos/" + tienda.getLogo();
            BufferedImage bufferedImage;
            try {
                if (impresora.getLogo() != null) {
                    bufferedImage = obtenerImagenLocal(impresora.getLogo());
                } else {
                    bufferedImage = obtenerImagenDeUrl(urlImagen);
                }

                //por ejemplo, 384 píxeles para una impresora de 58 mm y 576 píxeles para una de 80 mm
                BufferedImage resizeImage = resizeImage(bufferedImage, 290); //tamano de ancho en pixeles
                //sendImageInChunks(print, resizeImage, 24);
                // Crear algoritmo bitonal
                //Bitonal algorithm = new BitonalThreshold(127);
                Bitonal algorithm = new BitonalOrderedDither(3, 3, 120, 170);

                // Crear EscPosImage a partir de la imagen descargada
                EscPosImage escposImage = new EscPosImage(new CoffeeImageImpl(resizeImage), algorithm);

                /*
                Configurar el wrapper de imagen 
                existen varios wrappers - RasterImageWrapper = para imagenes complejas con degradados etc
                BitImageWrapper = para imagenes simples de blanco y negro
                 */
                RasterBitImageWrapper imageWrapper = new RasterBitImageWrapper().setJustification(EscPosConst.Justification.Center);
                //BitImageWrapper imageWrapper = new BitImageWrapper().setJustification(EscPosConst.Justification.Center);
                print.write(imageWrapper, escposImage).feed(1);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "No se encontro el logo, por lo tanto no l imprimira");
            }

            //nombre de la tienda
            if (tienda.getNombre() != null && !tienda.getNombre().isEmpty()) {
                print.writeLF(title, tienda.getNombre());
            }

            print.feed(1);
                
            print.writeLF(subTitle, "Proforma");
            
            print.feed(1);
            // Ruc de la tienda
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
            // Detalles
            print.writeLF("*".repeat(papelAncho));
            for (DetallesCotizacion detalle : detalles) {
                print.writeLF(texto(detalle.getNombre()));
                print.write(formatDecimal.format(detalle.getCantidad()));
                print.write(" x ");
                print.write(formatDecimal.format(detalle.getPrecio()));
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
            // Totales
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
            if (totales.getCordobasRecibidos() > 0 || totales.getDolaresRecibidos() > 0) {
                print.writeLF(tituloConLineaPunteada(" Cambio ", papelAncho));
                print.write(campo, "Recibio C$");
                print.write(espacio(papelAncho, "Recibio C$".length(), espacioCantidades(totales.getCordobasRecibidos())));
                print.writeLF(bold, formatDecimal.format(totales.getCordobasRecibidos()));
                print.write(campo, "Recibio $");
                print.write(espacio(papelAncho, "Recibio $".length(), espacioCantidades(totales.getDolaresRecibidos())));
                print.writeLF(bold, formatDecimal.format(totales.getDolaresRecibidos()));
                print.write(campo, "Cambio C$");
                print.write(espacio(papelAncho, "Cambio C$".length(), espacioCantidades(totales.getCambio())));
                print.writeLF(bold, formatDecimal.format(totales.getCambio()));
            }
            print.writeLF("-".repeat(papelAncho));
            print.writeLF(nota, tienda.getNota());
            print.feed(1);
            if (copias) {
                print.writeLF("Copia");
            }
            print.feed(4);
            print.cut(EscPos.CutMode.FULL);
            print.close();
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
