package httpHandle.handlers;

import application.usecases.ImprimirCotizacionUseCase;
import com.sun.net.httpserver.HttpExchange;
import domain.entities.Cotizacion;
import domain.PrinterConfig;
import httpHandle.util.DocumentParser;
import infrastructure.escpos.ImprimirCotizacion;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;

public class CotizacionHandler extends BaseHandler {

    private final ImprimirCotizacionUseCase useCase;

    public CotizacionHandler(Map<String, PrinterConfig> printers) {
        super(printers);
        this.useCase = new ImprimirCotizacionUseCase(new ImprimirCotizacion());
    }

    @Override
    protected void handleRequest(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String printerName = extractPrinterName(path, "/cotizacion/print/");

        PrinterConfig config = validarImpresora(exchange, printerName, "/cotizacion/print/");
        if (config == null) {
            return;
        }

        Cotizacion cotizacion = DocumentParser.parsear(exchange.getRequestBody(), Cotizacion.class);
        
        if (cotizacion == null) {
            sendResponse(exchange, Map.of("message", "Error al parsear el documento Cotizacion"), 400);
            return;
        }

        LOGGER.log(Level.INFO, "Enviando datos a la impresora: {0} ({1}:{2})", 
            new Object[]{config.getNombre(), config.getIp(), config.getPuerto()});

        if (config.getCopias() == 0) {
            useCase.ejecutar(config, cotizacion, false);
        }

        for (int i = 0; i < config.getCopias(); i++) {
            boolean esCopia = i > 0;
            useCase.ejecutar(config, cotizacion, esCopia);
        }

        sendSuccess(exchange, "Trabajo enviado a la impresora '" + config.getNombre() + "' exitosamente.");
    }

    @Override
    protected boolean esMetodoValido(HttpExchange exchange) {
        return "POST".equalsIgnoreCase(exchange.getRequestMethod());
    }
}