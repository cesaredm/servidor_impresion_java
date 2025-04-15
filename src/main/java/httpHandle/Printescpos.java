package httpHandle;

import com.github.anastaciocintra.escpos.EscPos;
import com.github.anastaciocintra.output.PrinterOutputStream;

/**
 *
 * @author cesar
 */
public class Printescpos {

    EscPos escpos;

    public Printescpos() {
    }

    public static String[] listaImpresorasDisponibles(){
        String[] lista = PrinterOutputStream.getListPrintServicesNames();
        
        return lista;
    }
}
