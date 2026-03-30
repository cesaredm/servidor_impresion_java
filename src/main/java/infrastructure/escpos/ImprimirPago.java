package infrastructure.escpos;

import com.github.anastaciocintra.escpos.EscPos;
import com.github.anastaciocintra.escpos.EscPosConst;
import com.github.anastaciocintra.escpos.image.Bitonal;
import com.github.anastaciocintra.escpos.image.BitonalOrderedDither;
import com.github.anastaciocintra.escpos.image.CoffeeImageImpl;
import com.github.anastaciocintra.escpos.image.EscPosImage;
import com.github.anastaciocintra.escpos.image.RasterBitImageWrapper;
import static infrastructure.escpos.AjustesImpresion.texto;
import domain.entities.Pago;
import domain.entities.Tienda;
import domain.ports.out.ImpresoraPort;
import domain.PrinterConfig;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.ConnectException;
import java.util.logging.Level;

public class ImprimirPago extends AjustesImpresion implements ImpresoraPort<Pago> {

    private final EscposConnectionFactory connectionFactory;

    public ImprimirPago() {
        this.connectionFactory = new EscposConnectionFactory();
    }

    @Override
    public String imprimir(PrinterConfig config, Pago data, boolean copias) {
        Tienda tienda = data.getTienda();
        papelAncho = config.getPapelSize();

        try (EscPos print = connectionFactory.crearConexion(config, config.getTipoConexion())) {
            String urlImagen = "https://api.cdsoft.net/uploads/logos/" + tienda.getLogo();
            BufferedImage bufferedImage;
            try {
                if (config.getLogo() != null) {
                    bufferedImage = obtenerImagenLocal(config.getLogo());
                } else {
                    bufferedImage = obtenerImagenDeUrl(urlImagen);
                }

                BufferedImage resizeImage = resizeImage(bufferedImage, 290);
                Bitonal algorithm = new BitonalOrderedDither(3, 3, 120, 170);
                EscPosImage escposImage = new EscPosImage(new CoffeeImageImpl(resizeImage), algorithm);

                RasterBitImageWrapper imageWrapper = new RasterBitImageWrapper().setJustification(EscPosConst.Justification.Center);
                print.write(imageWrapper, escposImage).feed(1);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "No se encontro el logo, por lo tanto no lo imprimira");
            }

            if (tienda.getNombre() != null && !tienda.getNombre().isEmpty()) {
                print.writeLF(title, tienda.getNombre());
            }

            print.feed(1);
            print.writeLF(subTitle, "Comprobante de pago");
            print.feed(1);

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