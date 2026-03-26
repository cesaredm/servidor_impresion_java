package httpHandle.handlers;

import com.sun.net.httpserver.HttpExchange;
import httpHandle.Printescpos;
import java.io.IOException;
import java.util.Map;

public class ImpresorasHandler extends BaseHandler {

    public ImpresorasHandler(java.util.Map<String, httpHandle.PrinterConfig> printers) {
        super(printers);
    }

    @Override
    protected void handleRequest(HttpExchange exchange) throws IOException {
        sendResponse(exchange, 
            Map.of("Impresoras", Printescpos.listaImpresorasDisponibles()), 
            200);
    }

    @Override
    protected boolean esMetodoValido(HttpExchange exchange) {
        return "GET".equalsIgnoreCase(exchange.getRequestMethod());
    }
}