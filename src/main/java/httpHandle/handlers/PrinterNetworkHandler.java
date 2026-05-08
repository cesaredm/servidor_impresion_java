package httpHandle.handlers;
/*
GET	/printers	Listar impresoras guardadas
GET	/printers/discover?range=192.168.0.1-192.168.0.255	Escanear red
GET	/printers/{ip}/ping	Hacer ping a una IP
GET /printers/ping?ip=192.168.1.105
POST	/printers	Guardar impresora (ipAddress=...)
DELETE	/printers/{ip}	Eliminar impresora
*/
import application.usecases.PrinterNetworkUseCases;
import com.sun.net.httpserver.HttpExchange;
import domain.entities.PrinterConfig;
import domain.entities.Printer;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PrinterNetworkHandler extends BaseHandler {

    private static final Logger LOGGER = Logger.getLogger(PrinterNetworkHandler.class.getName());
    private final PrinterNetworkUseCases useCases;

    public PrinterNetworkHandler(Map<String, PrinterConfig> printers) {
        super(printers);
        this.useCases = new PrinterNetworkUseCases();
    }

    @Override
    protected void handleRequest(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String[] pathParts = path.split("/");

        LOGGER.info("PrinterNetwork: " + exchange.getRequestMethod() + " " + path);

        try {
            switch (exchange.getRequestMethod()) {
                case "GET" -> handleGet(exchange, pathParts);
                case "POST" -> handlePost(exchange, pathParts);
                case "DELETE" -> handleDelete(exchange, pathParts);
                default -> sendResponse(exchange, Map.of("message", "Método no permitido"), 405);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error en PrinterNetworkHandler", e);
            sendResponse(exchange, Map.of("message", "Internal Server Error: " + e.getMessage()), 500);
        }
    }

    @Override
    protected boolean esMetodoValido(HttpExchange exchange) {
        String method = exchange.getRequestMethod();
        return "GET".equalsIgnoreCase(method) 
                || "POST".equalsIgnoreCase(method)
                || "DELETE".equalsIgnoreCase(method);
    }

    private void handleGet(HttpExchange exchange, String[] pathParts) throws IOException {
        String path = exchange.getRequestURI().getPath();

        if (path.equals("/printers")) {
            List<Printer> printers = useCases.listPrinters();
            Map<String, Object> response = new HashMap<>();
            response.put("printers", printers);
            sendResponse(exchange, response, 200);
            return;
        }

        if (path.startsWith("/printers/discover")) {
            String query = exchange.getRequestURI().getQuery();
            String range = extractQueryParam(query, "range");
            
            if (range == null || range.isEmpty()) {
                sendResponse(exchange, Map.of("message", "Parametro 'range' requerido (ej: 192.168.0.1-192.168.0.255)"), 400);
                return;
            }
            
            List<Printer> printers = useCases.discoverPrinters(range);
            Map<String, Object> response = new HashMap<>();
            response.put("printers", printers);
            response.put("count", printers.size());
            sendResponse(exchange, response, 200);
            return;
        }

        // Ruta: /printers/ping?ip=192.168.1.105
        if (path.equals("/printers/ping")) {
            String query = exchange.getRequestURI().getQuery();
            String ip = extractQueryParam(query, "ip");
            
            if (ip == null || ip.isEmpty()) {
                sendResponse(exchange, Map.of("message", "Parametro 'ip' requerido"), 400);
                return;
            }
            
            boolean result = useCases.pingPrinter(ip);
            Map<String, Object> response = new HashMap<>();
            response.put("ip", ip);
            response.put("accesible", result);
            sendResponse(exchange, response, 200);
            return;
        }

        sendResponse(exchange, Map.of("message", "Endpoint no encontrado"), 404);
    }

    private void handlePost(HttpExchange exchange, String[] pathParts) throws IOException {
        String path = exchange.getRequestURI().getPath();

        if (path.equals("/printers")) {
            String body = readBody(exchange);
            Printer printer = parsePrinter(body);
            
            if (printer == null || printer.getIpAddress() == null) {
                sendResponse(exchange, Map.of("message", "Datos de impresora inválidos"), 400);
                return;
            }
            
            useCases.savePrinterConfig(printer);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("printer", printer);
            sendResponse(exchange, response, 201);
            return;
        }

        sendResponse(exchange, Map.of("message", "Endpoint no encontrado"), 404);
    }

    private void handleDelete(HttpExchange exchange, String[] pathParts) throws IOException {
        if (pathParts.length >= 3) {
            String ip = pathParts[2];
            useCases.deletePrinter(ip);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Impresora eliminada: " + ip);
            sendResponse(exchange, response, 200);
            return;
        }

        sendResponse(exchange, Map.of("message", "Endpoint no encontrado"), 404);
    }

    private String extractQueryParam(String query, String param) {
        if (query == null) return null;
        for (String pair : query.split("&")) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2 && keyValue[0].equals(param)) {
                return keyValue[1];
            }
        }
        return null;
    }

    private Printer parsePrinter(String body) {
        try {
            Map<String, String> params = new HashMap<>();
            for (String pair : body.split("&")) {
                String[] kv = pair.split("=");
                if (kv.length == 2) {
                    params.put(kv[0], kv[1]);
                }
            }

            if (params.containsKey("ipAddress")) {
                String name = params.containsKey("name") ? params.get("name") : params.get("ipAddress").replace(".", "_");
                Printer printer = new Printer(
                    name,
                    params.get("ipAddress")
                );
                
                if (params.containsKey("hostName")) {
                    printer.setHostName(params.get("hostName"));
                }
                if (params.containsKey("port")) {
                    try {
                        printer.setPort(Integer.parseInt(params.get("port")));
                    } catch (NumberFormatException e) {
                        printer.setPort(9100);
                    }
                }
                
                return printer;
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error al parsear printer: " + e.getMessage());
        }
        return null;
    }

    private String readBody(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes());
    }
}