package application.usecases;

import domain.entities.Cotizacion;
import domain.ports.out.ImpresoraPort;
import domain.PrinterConfig;

public class ImprimirCotizacionUseCase {

    private final ImpresoraPort<Cotizacion> impresoraPort;

    public ImprimirCotizacionUseCase(ImpresoraPort<Cotizacion> impresoraPort) {
        this.impresoraPort = impresoraPort;
    }

    public String ejecutar(PrinterConfig config, Cotizacion cotizacion, boolean copias) {
        if (cotizacion == null) {
            return "Cotizacion no proporcionada";
        }
        return impresoraPort.imprimir(config, cotizacion, copias);
    }
}