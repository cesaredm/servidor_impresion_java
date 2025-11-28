/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
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
import entities.DatosGeneralesCotizacion;
import entities.DetallesCotizacion;
import entities.Pago;
import entities.Tienda;
import entities.Totales;
import httpHandle.PrinterConfig;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.ConnectException;
import java.util.List;
import java.util.logging.Level;
import javax.print.PrintService;

/**
 *
 * @author cesar
 */
public class ImpresionPago  extends AjustesImpresion implements Impresora<Pago> {
    private EscPos imprimirPago(PrinterConfig impresora) throws IOException{
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
    public String imprimir(PrinterConfig impresora, Pago data, boolean copias) {
        Tienda tienda = data.getTienda();
        papelAncho = impresora.getPapelSize();
        
        try {
            EscPos print = imprimirPago(impresora);
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
                
            print.writeLF(subTitle, "Comprobante de pago");
            
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
            print.writeLF(data.getFecha());
            print.write(campo, "Forma de pago: ");
            print.writeLF(data.getFormaPago());
            print.write(campo, "Cliente: ");
            print.writeLF(texto(data.getCliente()));
            print.writeLF(campo, "ID: #");
            print.writeLF("-".repeat(papelAncho));
            print.write(campo, "Saldo cordobas");
            print.write(espacio(papelAncho, "Saldo cordobas".length(), espacioCantidades(data.getSaldoAnteriorCordobas())));
            print.writeLF(bold, formatDecimal.format(data.getSaldoAnteriorCordobas()));
            
            print.write(campo, "Saldo dolares");
            print.write(espacio(papelAncho, "Saldo dolares".length(), espacioCantidades(data.getSaldoAnteriorDolares())));
            print.writeLF(bold, formatDecimal.format(data.getSaldoAnteriorDolares()));
            print.writeLF("-".repeat(papelAncho));
            print.write(campo, "Monto");
            print.write(espacio(papelAncho, "Monto".length(), espacioCantidades(data.getMonto())));
            print.writeLF(bold, formatDecimal.format(data.getMonto()));
            
            print.write(campo, "Moneda");
            print.write(espacio(papelAncho, "Moneda".length(), data.getMoneda().length()));
            print.writeLF(bold, data.getMoneda());
            print.writeLF("-".repeat(papelAncho));
            print.write(campo, "Nuevo saldo C$");
            print.write(espacio(papelAncho, "Nuevo saldo C$".length(), espacioCantidades(data.getNuevoSaldoCordobas())));
            print.writeLF(bold, formatDecimal.format(data.getNuevoSaldoCordobas()));
            
            print.write(campo, "Nuevo saldo $");
            print.write(espacio(papelAncho, "Nuevo saldo $".length(), espacioCantidades(data.getNuevoSaldoDolares())));
            print.writeLF(bold, formatDecimal.format(data.getNuevoSaldoDolares()));
            
            print.writeLF("-".repeat(papelAncho));
            print.writeLF(nota, tienda.getNota());
            print.feed(1);
            if (copias) {
                print.writeLF("Copia");
            }
            print.feed(4);
            print.cut(EscPos.CutMode.FULL);
            print.close();
            return "";
        }catch (ConnectException ex) {
            LOGGER.log(Level.SEVERE, null, "No se pudo conectar a la impresra, revise la configuracion " + ex);
            return "Fallo la impresion";
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, "Error en la impresion " + ex);
            return "Error en la impresion";
        }
    }
}
