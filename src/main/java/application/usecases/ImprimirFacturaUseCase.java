package application.usecases;

import domain.entities.Factura;
import domain.ports.out.ImpresoraPort;
import httpHandle.PrinterConfig;

public class ImprimirFacturaUseCase {

    private final ImpresoraPort<Factura> impresoraPort;

    public ImprimirFacturaUseCase(ImpresoraPort<Factura> impresoraPort) {
        this.impresoraPort = impresoraPort;
    }

    public String ejecutar(PrinterConfig config, Factura factura, boolean copias) {
        if (factura == null) {
            return "Factura no proporcionada";
        }
        return impresoraPort.imprimir(config, factura, copias);
    }
}