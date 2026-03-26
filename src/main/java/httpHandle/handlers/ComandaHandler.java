package httpHandle.handlers;

import application.usecases.ImprimirComandaUseCase;
import com.sun.net.httpserver.HttpExchange;
import domain.entities.Comanda;
import httpHandle.PrinterConfig;
import httpHandle.util.DocumentParser;
import infrastructure.escpos.ImprimirComanda;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;

public class ComandaHandler extends BaseHandler {

    private final ImprimirComandaUseCase useCase;

    public ComandaHandler(java.util.Map<String, httpHandle.PrinterConfig> printers) {
        super(printers);
        this.useCase = new ImprimirComandaUseCase(new ImprimirComanda());
    }

    @Override
    protected void handleRequest(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String printerName = extractPrinterName(path, "/comanda/print/");

        PrinterConfig config = validarImpresora(exchange, printerName, "/comanda/print/");
        if (config == null) {
            return;
        }

        Comanda comanda = DocumentParser.parsear(exchange.getRequestBody(), Comanda.class);
        
        if (comanda == null) {
            sendResponse(exchange, Map.of("message", "Error al parsear el documento Comanda"), 400);
            return;
        }

        LOGGER.log(Level.INFO, "Enviando datos a la impresora: {0} ({1}:{2})", 
            new Object[]{config.getNombre(), config.getIp(), config.getPuerto()});

        if (config.getCopias() == 0) {
            useCase.ejecutar(config, comanda, false);
        }

        for (int i = 0; i < config.getCopias(); i++) {
            useCase.ejecutar(config, comanda, true);
        }

        sendSuccess(exchange, "Trabajo enviado a la impresora '" + config.getNombre() + "' exitosamente.");
    }

    @Override
    protected boolean esMetodoValido(HttpExchange exchange) {
        return "POST".equalsIgnoreCase(exchange.getRequestMethod());
    }
}