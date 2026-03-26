package httpHandle.handlers;

import application.usecases.ImprimirFacturaUseCase;
import com.sun.net.httpserver.HttpExchange;
import domain.entities.Factura;
import httpHandle.PrinterConfig;
import httpHandle.util.DocumentParser;
import infrastructure.escpos.ImprimirFactura;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;

public class FacturaHandler extends BaseHandler {

    private final ImprimirFacturaUseCase useCase;

    public FacturaHandler(java.util.Map<String, httpHandle.PrinterConfig> printers) {
        super(printers);
        this.useCase = new ImprimirFacturaUseCase(new ImprimirFactura());
    }

    @Override
    protected void handleRequest(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String printerName = extractPrinterName(path, "/print/");

        PrinterConfig config = validarImpresora(exchange, printerName, "/print/");
        if (config == null) {
            return;
        }

        Factura factura = DocumentParser.parsear(exchange.getRequestBody(), Factura.class);
        
        if (factura == null) {
            sendResponse(exchange, Map.of("message", "Error al parsear el documento Factura"), 400);
            return;
        }

        LOGGER.log(Level.INFO, "Enviando datos a la impresora: {0} ({1}:{2})", 
            new Object[]{config.getNombre(), config.getIp(), config.getPuerto()});

        if (config.getCopias() == 0) {
            useCase.ejecutar(config, factura, false);
        }

        for (int i = 0; i < config.getCopias(); i++) {
            boolean esCopia = i > 0;
            useCase.ejecutar(config, factura, esCopia);
        }

        sendSuccess(exchange, "Trabajo enviado a la impresora '" + printerName + "' exitosamente.");
    }

    @Override
    protected boolean esMetodoValido(HttpExchange exchange) {
        return "POST".equalsIgnoreCase(exchange.getRequestMethod());
    }
}