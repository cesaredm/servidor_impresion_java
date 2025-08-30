
package infra;

import com.github.anastaciocintra.escpos.EscPos;
import com.github.anastaciocintra.output.PrinterOutputStream;
import domain.AjustesImpresion;
import domain.Impresora;
import entities.Comanda;
import entities.DatosGeneralesComanda;
import entities.Detalles;
import httpHandle.PrinterConfig;
import httpHandle.Printescpos;
import java.io.IOException;
import java.net.ConnectException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.print.PrintService;

/**
 *
 * @author cesar
 */
public class ImpresionComandaUsb extends AjustesImpresion implements Impresora<Comanda> {

    @Override
    public String imprimir(PrinterConfig printer, Comanda comanda, boolean copias) {
         List<Detalles> detalles = comanda.getDetalles();
        DatosGeneralesComanda datosGenerales = comanda.getDatosGenerales();
        papelAncho = printer.getPapelSize();
        //Crear conexion hacia impresora mediante ip
        PrintService printService = PrinterOutputStream.getPrintServiceByName(printer.getNombre());
        try (PrinterOutputStream outputStream = new PrinterOutputStream(printService)) {
            print = new EscPos(outputStream);
            print.writeLF(title, "Pedido");
            print.write(campo, "Id: ");
            print.writeLF(datosGenerales.getId());
            print.write(campo, "Fecha: ");
            print.writeLF(datosGenerales.getFecha());
            print.write(campo, "Atendido por: # ");
            print.write(String.valueOf(datosGenerales.getEmpleado()));
            print.writeLF(" " + datosGenerales.getUsuario());
            print.write(campo, "Comprador: ");
            print.writeLF(texto(datosGenerales.getComprador()));
            print.writeLF("-".repeat(papelAncho));
            print.write(bold, "Cant");
            print.write(espacioTresColumnas(papelAncho, anchoTitulos));
            print.write(bold, "Precio");
            print.write(espacioTresColumnas(papelAncho, anchoTitulos));
            print.writeLF(bold, "Total");
            print.writeLF("-".repeat(papelAncho));
            for (Detalles detalle : detalles) {
                print.writeLF(texto(detalle.getDescripcion()));
                print.write(formatDecimal.format(detalle.getCantidadProducto()));
                print.write(" x ");
                print.write(formatDecimal.format(detalle.getPrecioProducto()));
                print.write(" = ");
                print.writeLF(formatDecimal.format(detalle.getImporte()));
                if (detalle.getDescuento() > 0) {
                    print.write("Desc - ");
                    print.write(formatDecimal.format(detalle.getDescuento()) + " ");
                    print.write("PO: ");
                    print.writeLF(formatDecimal.format(detalle.getPrecioVenta()));
                }
                print.writeLF("-".repeat(papelAncho));
            }
            print.feed(4);
            print.cut(EscPos.CutMode.FULL);
            return "exito";
        } catch (ConnectException ex) {
            LOGGER.log(Level.SEVERE, null, "No se pudo conectar a la impresra, revise la configuracion " + ex);
            return "Fallo la impresion";
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, "Error en la impresion " + ex);
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
