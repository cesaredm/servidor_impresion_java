package httpHandle.handlers;

import application.usecases.ImprimirPagoUseCase;
import com.sun.net.httpserver.HttpExchange;
import domain.entities.Pago;
import domain.PrinterConfig;
import httpHandle.util.DocumentParser;
import infrastructure.escpos.ImprimirPago;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;

public class PagoHandler extends BaseHandler {

    private final ImprimirPagoUseCase useCase;

    public PagoHandler(Map<String, PrinterConfig> printers) {
        super(printers);
        this.useCase = new ImprimirPagoUseCase(new ImprimirPago());
    }

    @Override
    protected void handleRequest(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String printerName = extractPrinterName(path, "/pago/print/");

        PrinterConfig config = validarImpresora(exchange, printerName, "/pago/print/");
        if (config == null) {
            return;
        }

        Pago pago = DocumentParser.parsear(exchange.getRequestBody(), Pago.class);
        
        if (pago == null) {
            sendResponse(exchange, Map.of("message", "Error al parsear el documento Pago"), 400);
            return;
        }

        LOGGER.log(Level.INFO, "Enviando datos a la impresora: {0} ({1}:{2})", 
            new Object[]{config.getNombre(), config.getIp(), config.getPuerto()});

        if (config.getCopias() == 0) {
            useCase.ejecutar(config, pago, false);
        }

        for (int i = 0; i < config.getCopias(); i++) {
            useCase.ejecutar(config, pago, false);
        }

        sendSuccess(exchange, "Trabajo enviado a la impresora '" + config.getNombre() + "' exitosamente.");
    }

    @Override
    protected boolean esMetodoValido(HttpExchange exchange) {
        return "POST".equalsIgnoreCase(exchange.getRequestMethod());
    }
}