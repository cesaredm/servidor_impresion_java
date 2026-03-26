package application.usecases;

import domain.entities.Pago;
import domain.ports.out.ImpresoraPort;
import httpHandle.PrinterConfig;

public class ImprimirPagoUseCase {

    private final ImpresoraPort<Pago> impresoraPort;

    public ImprimirPagoUseCase(ImpresoraPort<Pago> impresoraPort) {
        this.impresoraPort = impresoraPort;
    }

    public String ejecutar(PrinterConfig config, Pago pago, boolean copias) {
        if (pago == null) {
            return "Pago no proporcionado";
        }
        return impresoraPort.imprimir(config, pago, copias);
    }
}