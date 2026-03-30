package application.usecases;

import domain.ports.out.ImpresoraPort;
import domain.PrinterConfig;

public class TestImpresionUseCase {

    private final ImpresoraPort<Object> impresoraPort;

    public TestImpresionUseCase(ImpresoraPort<Object> impresoraPort) {
        this.impresoraPort = impresoraPort;
    }

    public String ejecutar(PrinterConfig config) {
        if (config == null) {
            return "Configuración de impresora no proporcionada";
        }
        return impresoraPort.imprimir(config, null, false);
    }
}