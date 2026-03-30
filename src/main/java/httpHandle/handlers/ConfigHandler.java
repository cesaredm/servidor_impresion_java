package httpHandle.handlers;

import com.cdsoft.printserver.PrintServer;
import com.sun.net.httpserver.HttpExchange;
import domain.PrinterConfig;
import java.io.IOException;
import java.util.Map;

public class ConfigHandler extends BaseHandler {

    public ConfigHandler(java.util.Map<String, PrinterConfig> printers) {
        super(printers);
    }

    @Override
    protected void handleRequest(HttpExchange exchange) throws IOException {
        PrintServer.loadPrinterConfiguration();
        sendResponse(exchange, Map.of("message", "Configuraciones de impresoras actualizado"), 200);
    }

    @Override
    protected boolean esMetodoValido(HttpExchange exchange) {
        return "GET".equalsIgnoreCase(exchange.getRequestMethod());
    }
}
