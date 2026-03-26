package httpHandle.handlers;

import com.sun.net.httpserver.HttpExchange;
import httpHandle.PrinterConfig;
import java.io.IOException;
import java.util.Map;

public class NotFoundHandler extends BaseHandler {

    public NotFoundHandler(java.util.Map<String, PrinterConfig> printers) {
        super(printers);
    }

    @Override
    protected void handleRequest(HttpExchange exchange) throws IOException {
        sendResponse(exchange, Map.of("message", "Ruta no encontrada"), 404);
    }

    @Override
    protected boolean esMetodoValido(HttpExchange exchange) {
        return true;
    }
}