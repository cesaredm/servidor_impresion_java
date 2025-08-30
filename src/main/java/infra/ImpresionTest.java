/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package infra;

import com.github.anastaciocintra.escpos.EscPos;
import com.github.anastaciocintra.output.PrinterOutputStream;
import com.github.anastaciocintra.output.TcpIpOutputStream;
import domain.AjustesImpresion;
import domain.Impresora;
import httpHandle.PrinterConfig;
import java.io.IOException;
import java.net.ConnectException;
import java.util.logging.Level;
import javax.print.PrintService;

/**
 *
 * @author cesar
 */
public class ImpresionTest extends AjustesImpresion implements Impresora<NullPointerException> {

    private EscPos ImprimirTest(PrinterConfig impresora) throws IOException{
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
    public String imprimir(PrinterConfig printer, NullPointerException data, boolean copias) {
        try {
            EscPos print = ImprimirTest(printer);
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
        return "";
    }

}
