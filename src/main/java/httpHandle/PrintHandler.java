/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package httpHandle;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import entities.Comanda;
import entities.Factura;
import java.io.*;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets; // O el charset que necesite tu impresora
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author cesar
 */
public class PrintHandler implements HttpHandler {

    private static final Logger LOGGER = Logger.getLogger(PrintHandler.class.getName());
    private final Map<String, PrinterConfig> printers;
    private final Gson json = new Gson();

    public PrintHandler(Map<String, PrinterConfig> printers) {
        this.printers = printers;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        //String response = "Error interno del servidor.";
        int statusCode = 500;
        String message = "";
        Map<String, Object> response; // para crear la respuesta

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

            if ("GET".equalsIgnoreCase(exchange.getRequestMethod()) && url.getPath().equals("/impresoras")) {
                statusCode = 200;
                response = Map.of("Impresoras", Printescpos.listaImpresorasDisponibles());
                sendResponse(exchange, response, statusCode);
                return;
            }

            // 1. Verificar método (solo aceptamos POST)
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                message = "Método no permitido. Use POST.";
                response = Map.of("message", message);
                statusCode = 405; // Method Not Allowed

                sendResponse(exchange, response, statusCode);
                return;
            }
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod()) && url.getPath().startsWith("/print")) {
                // 2. Extraer nombre de la impresora de la URL
                URI requestURI = exchange.getRequestURI();
                String path = requestURI.getPath(); // Debería ser /print/nombreImpresora
                String printerName = extractPrinterName(path);

                if (printerName == null || printerName.isEmpty()) {
                    message = "Nombre de impresora no especificado en la URL. Use /print/{nombreImpresora}";
                    response = Map.of("message", message);
                    statusCode = 400; // Bad Request
                    sendResponse(exchange, response, statusCode);
                    return;
                }

                // 3. Buscar la configuración de la impresora
                PrinterConfig config = printers.get(printerName);
                if (config == null) {
                    message = "Impresora '" + printerName + "' no encontrada en la configuración.";
                    response = Map.of("message", message);
                    statusCode = 404; // Not Found
                    sendResponse(exchange, response, statusCode);
                    return;
                }

                // 4. Leer los datos a imprimir del cuerpo de la petición
                InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
                //creamos una una clase de tipo Factura a partir de el json recibido
                Factura factura = json.fromJson(reader, Factura.class);
                // 5. Enviar los datos a la impresora
                LOGGER.log(Level.INFO, "Enviando datos a la impresora: {1} ({2}:{3})", new Object[]{config.getNombre(), config.getIp(), config.getPuerto()});

                // 6. logica de imprimir la factura, con las copias o no
                for (int i = 0; i < config.getCopias(); i++) {
                    if (i == 0) {
                        Printescpos.printTcpIp(config, factura, false);
                    } else {
                        Printescpos.printTcpIp(config, factura, true);
                    }
                }

                if (config.getCopias() == 0) {
                   Printescpos.printTcpIp(config, factura, false);
                }
                
                // 6. Enviar respuesta exitosa
                message = "Trabajo enviado a la impresora '" + printerName + "' exitosamente.";
                //response = Map.of("message", message);
                statusCode = 200; // OK
                LOGGER.info(message);
                response = Map.of("message", message);
                sendResponse(exchange, response, statusCode);
                return;
            }
            if("POST".equalsIgnoreCase(exchange.getRequestMethod()) && url.getPath().startsWith("/comanda/print")){
                // 2. Extraer nombre de la impresora de la URL
                URI requestURI = exchange.getRequestURI();
                String path = requestURI.getPath(); // Debería ser /print/nombreImpresora
                //String printerName = extractPrinterName(path);
                String printerName = path.substring("/comanda/print/".length());

                if (printerName == null || printerName.isEmpty()) {
                    message = "Nombre de impresora no especificado en la URL. Use /comanda/print/{nombreImpresora}";
                    response = Map.of("message", message);
                    statusCode = 400; // Bad Request
                    sendResponse(exchange, response, statusCode);
                    return;
                }

                // 3. Buscar la configuración de la impresora
                PrinterConfig config = printers.get(printerName);
                if (config == null) {
                    message = "Impresora '" + printerName + "' no encontrada en la configuración.";
                    response = Map.of("message", message);
                    statusCode = 404; // Not Found
                    sendResponse(exchange, response, statusCode);
                    return;
                }

                // 4. Leer los datos a imprimir del cuerpo de la petición
                InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
                //creamos una una clase de tipo Factura a partir de el json recibido
                Comanda comanda = json.fromJson(reader, Comanda.class);
                // 5. Enviar los datos a la impresora
                LOGGER.log(Level.INFO, "Enviando datos a la impresora: {1} ({2}:{3})", new Object[]{config.getNombre(), config.getIp(), config.getPuerto()});

                // 6. logica de imprimir la factura, con las copias o no
                for (int i = 0; i < config.getCopias(); i++) {
                    if (i == 0) {
                        Printescpos.printComandaTcpIp(config, comanda, false);
                    } else {
                        Printescpos.printComandaTcpIp(config, comanda, true);
                    }
                }

                if (config.getCopias() == 0) {
                   Printescpos.printComandaTcpIp(config, comanda, false);
                }
                
                // 6. Enviar respuesta exitosa
                message = "Trabajo enviado a la impresora '" + printerName + "' exitosamente.";
                //response = Map.of("message", message);
                statusCode = 200; // OK
                LOGGER.info(message);
                response = Map.of("message", message);
                sendResponse(exchange, response, statusCode);
                return;
            }
        } catch (IOException e) {
            message = "Error de E/S al procesar la impresión para '"
                    + extractPrinterName(exchange.getRequestURI().getPath()) + "': " + e.getMessage();
            statusCode = 500; // Internal Server Error
            LOGGER.log(Level.SEVERE, message, e);
        } catch (Exception e) {
            message = "Error inesperado: " + e.getMessage();
            statusCode = 500;
            LOGGER.log(Level.SEVERE, message, e);
        } finally {
            // Asegurarse de enviar siempre una respuesta al cliente
            response = Map.of("message", message);
            sendResponse(exchange, response, statusCode);
        }
    }

    private String extractPrinterName(String path) {
        // Extraer de "/print/nombre" -> "nombre"
        if (path != null && path.startsWith("/print/")) {
            String name = path.substring("/print/".length());
            // Podrías decodificar URL si esperas caracteres especiales:
            // return java.net.URLDecoder.decode(name, StandardCharsets.UTF_8);
            return name;
        }
        return null;
    }

    // Helper para leer todos los bytes del InputStream
    private byte[] readAllBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[1024];
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        return buffer.toByteArray();
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
