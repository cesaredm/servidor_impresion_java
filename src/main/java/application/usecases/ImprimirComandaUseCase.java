package application.usecases;

import domain.entities.Comanda;
import domain.ports.out.ImpresoraPort;
import domain.PrinterConfig;

public class ImprimirComandaUseCase {

    private final ImpresoraPort<Comanda> impresoraPort;

    public ImprimirComandaUseCase(ImpresoraPort<Comanda> impresoraPort) {
        this.impresoraPort = impresoraPort;
    }

    public String ejecutar(PrinterConfig config, Comanda comanda, boolean copias) {
        if (comanda == null) {
            return "Comanda no proporcionada";
        }
        return impresoraPort.imprimir(config, comanda, copias);
    }
}