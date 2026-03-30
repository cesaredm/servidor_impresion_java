package infrastructure.escpos;

import com.github.anastaciocintra.output.PrinterOutputStream;
/**
 *
 * @author cesar
 */
public class ConfiguracionesImpresion extends AjustesImpresion {

    public ConfiguracionesImpresion() {
    }

    // funcion que retorna lista de impresoras disponibles en el dispositivo
    public static String[] listaImpresorasDisponibles() {
        String[] lista = PrinterOutputStream.getListPrintServicesNames();

        return lista;
    }
}
