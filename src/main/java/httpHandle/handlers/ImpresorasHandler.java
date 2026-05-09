package httpHandle.handlers;

import com.sun.net.httpserver.HttpExchange;
import infrastructure.escpos.ConfiguracionesImpresion;
import java.io.IOException;
import java.util.Map;
import domain.entities.PrinterConfig;

public class ImpresorasHandler extends BaseHandler {

    public ImpresorasHandler() {
        super();
    }

    @Override
    protected void handleRequest(HttpExchange exchange) throws IOException {
        sendResponse(exchange, 
            Map.of("Impresoras", ConfiguracionesImpresion.listaImpresorasDisponibles()), 
            200);
    }

    @Override
    protected boolean esMetodoValido(HttpExchange exchange) {
        return "GET".equalsIgnoreCase(exchange.getRequestMethod());
    }
}