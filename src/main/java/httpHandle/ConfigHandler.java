package httpHandle;
import com.cdsoft.printserver.PrintServer;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 *
 * @author cesar
 */
public class ConfigHandler implements HttpHandler{
    Map<String,Object> response;
    final Gson json = new Gson();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            URI url = exchange.getRequestURI();
            
            // Configurar los encabezados CORS
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Credentials", "true");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");

            // Manejar las solicitudes OPTIONS (preflight)
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1); // Respuesta vacía para OPTIONS
                return;
            }
            
            if(!"GET".equalsIgnoreCase(exchange.getRequestMethod()) && url.getPath().equals("/recargar")){
                response= Map.of("message", "Metodo no permitido solo GET.");
                sendResponse(exchange, response, 400);
            }
            if("GET".equalsIgnoreCase(exchange.getRequestMethod()) && url.getPath().equals("/recargar")){
                PrintServer.loadPrinterConfiguration();
                response= Map.of("message", "Configuraciones de impresoras actualizado");
                sendResponse(exchange, response, 200);
            }
        } catch (Exception e) {
        } finally {
        }
    }
    
    private void sendResponse(HttpExchange exchange, Map<String, Object> response, int statusCode) throws IOException {
        // Asegúrate de que la respuesta sea UTF-8 para compatibilidad
        //byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        String jsonResponse = this.json.toJson(response);
        byte[] responseBytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
        //exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }
    
}
