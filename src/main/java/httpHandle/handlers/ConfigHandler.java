package httpHandle.handlers;

import com.sun.net.httpserver.HttpExchange;
import infrastructure.config.PrinterConfigProperties;
import java.io.IOException;
import java.util.Map;

public class ConfigHandler extends BaseHandler {

    public ConfigHandler() {
    }

    @Override
    protected void handleRequest(HttpExchange exchange) throws IOException {
        //PrintServer.loadPrinterConfiguration();
				PrinterConfigProperties.getInstance().loadProperties();
        sendResponse(exchange, Map.of("message", "Configuraciones de impresoras actualizado"), 200);
    }

    @Override
    protected boolean esMetodoValido(HttpExchange exchange) {
        return "GET".equalsIgnoreCase(exchange.getRequestMethod());
    }
}
