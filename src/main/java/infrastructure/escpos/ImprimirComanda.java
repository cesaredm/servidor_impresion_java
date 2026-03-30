package infrastructure.escpos;

import com.github.anastaciocintra.escpos.EscPos;
import domain.entities.Comanda;
import domain.entities.DatosGeneralesComanda;
import domain.entities.Detalles;
import domain.ports.out.ImpresoraPort;
import domain.PrinterConfig;
import java.io.IOException;
import java.net.ConnectException;
import java.util.List;
import java.util.logging.Level;

public class ImprimirComanda extends AjustesImpresion implements ImpresoraPort<Comanda> {

    private final EscposConnectionFactory connectionFactory;

    public ImprimirComanda() {
        this.connectionFactory = new EscposConnectionFactory();
    }

    @Override
    public String imprimir(PrinterConfig config, Comanda comanda, boolean copias) {
        List<Detalles> detalles = comanda.getDetalles();
        DatosGeneralesComanda datosGenerales = comanda.getDatosGenerales();
        papelAncho = config.getPapelSize();

        try (EscPos print = connectionFactory.crearConexion(config, config.getTipoConexion())) {
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

            if (!datosGenerales.getNota().equals("")) {
                print.writeLF("");
                print.write(campoColorMode, "Nota:");
                print.writeLF(" " + texto(datosGenerales.getNota()));
            }

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