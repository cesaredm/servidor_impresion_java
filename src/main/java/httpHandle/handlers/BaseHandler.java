package httpHandle.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import httpHandle.PrinterConfig;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class BaseHandler implements HttpHandler {

    protected static final Logger LOGGER = Logger.getLogger(BaseHandler.class.getName());

    protected Map<String, PrinterConfig> printers;

    public BaseHandler(Map<String, PrinterConfig> printers) {
        this.printers = printers;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            agregarCabecerasCors(exchange);
            
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            
            if (!esMetodoValido(exchange)) {
                sendResponse(exchange, Map.of("message", "Método no permitido"), 405);
                return;
            }

            handleRequest(exchange);
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al procesar solicitud: {0}", e.getMessage());
            sendResponse(exchange, Map.of("message", "Error inesperado: " + e.getMessage()), 500);
        }
    }

    protected abstract void handleRequest(HttpExchange exchange) throws IOException;

    protected abstract boolean esMetodoValido(HttpExchange exchange);

    protected PrinterConfig obtenerConfig(String printerName) {
        return printers.get(printerName);
    }

    protected PrinterConfig validarImpresora(HttpExchange exchange, String printerName, String ruta) throws IOException {
        if (printerName == null || printerName.isEmpty()) {
            sendResponse(exchange, Map.of("message", "Nombre de impresora no especificado en la URL. Use " + ruta + "{nombreImpresora}"), 400);
            return null;
        }

        PrinterConfig config = printers.get(printerName);
        if (config == null) {
            sendResponse(exchange, Map.of("message", "Impresora '" + printerName + "' no encontrada en la configuración."), 404);
            return null;
        }
        return config;
    }

    protected void agregarCabecerasCors(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Credentials", "true");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    protected void sendResponse(HttpExchange exchange, Map<String, Object> response, int statusCode) throws IOException {
        com.google.gson.Gson gson = new com.google.gson.Gson();
        String jsonResponse = gson.toJson(response);
        byte[] responseBytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    protected void sendSuccess(HttpExchange exchange, String message) throws IOException {
        exchange.getResponseHeaders().set("Connection", "keep-alive");
        exchange.getResponseHeaders().set("Keep-Alive", "timeout=3600, max=1000");
        sendResponse(exchange, Map.of("message", message), 200);
    }

    protected String extractPrinterName(String path, String ruta) {
        if (path != null && path.startsWith(ruta)) {
            return path.substring(ruta.length());
        }
        return null;
    }
}