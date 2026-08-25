package application.usecases;

import domain.entities.DatosGeneralesKardex;
import domain.entities.PrinterConfig;
import domain.ports.out.ImpresoraPort;

public class ImprimirKardexUseCase {
    private final ImpresoraPort<DatosGeneralesKardex> impresoraPort;

    public ImprimirKardexUseCase(ImpresoraPort<DatosGeneralesKardex> impresoraPort) {
        this.impresoraPort = impresoraPort;
    }

    public String ejecutar(PrinterConfig config, DatosGeneralesKardex datos, boolean copias) {
        if (datos == null) {
            return "Datos de Kardex no proporcionados";
        }
        return impresoraPort.imprimir(config, datos, copias);
    }
}
