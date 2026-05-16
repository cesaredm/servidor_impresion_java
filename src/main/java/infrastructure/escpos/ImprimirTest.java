package infrastructure.escpos;

import com.github.anastaciocintra.escpos.EscPos;
import domain.ports.out.ImpresoraPort;
import domain.entities.PrinterConfig;
import domain.exceptions.ImpresoraNoConectadaException;
import domain.exceptions.ImpresoraNoInstaladaException;
import java.io.IOException;
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
        } catch (ImpresoraNoInstaladaException ex) {
            LOGGER.log(Level.SEVERE, "Impresora no instalada: {0}", ex.getNombreImpresora());
            return ex.getUserMessage();
        } catch (ImpresoraNoConectadaException ex) {
            LOGGER.log(Level.SEVERE, "No se pudo conectar a la impresora: {0}:{1}", 
                new Object[]{ex.getIp(), ex.getPuerto()});
            return ex.getUserMessage();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Error en la impresion: {0}", ex.getMessage());
            return "Error de comunicación con la impresora: " + ex.getMessage();
        }
    }
}