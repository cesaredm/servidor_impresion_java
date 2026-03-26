package infrastructure.escpos;

import com.github.anastaciocintra.escpos.EscPos;
import domain.ports.out.ImpresoraPort;
import httpHandle.PrinterConfig;
import java.io.IOException;
import java.net.ConnectException;
import java.util.logging.Level;

public class ImprimirTest extends AjustesImpresion implements ImpresoraPort<Object> {

    private final EscposConnectionFactory connectionFactory;

    public ImprimirTest() {
        this.connectionFactory = new EscposConnectionFactory();
    }

    @Override
    public String imprimir(PrinterConfig config, Object data, boolean copias) {
        try (EscPos print = connectionFactory.crearConexion(config, config.getTipoConexion())) {
            print.writeLF("Esto es un test");
            print.feed(2);
            print.writeLF("De impresion en la impresora " + config.getNombre());
            print.feed(5);
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