/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package httpHandle;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
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
        Map<String, Object> response;

        try {
            URI url = exchange.getRequestURI();

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
            // Asumimos que el cuerpo es el texto/comandos a imprimir directamente
            InputStream requestBody = exchange.getRequestBody();
            byte[] printData = readAllBytes(requestBody); // Leer todo el cuerpo

            if (printData == null || printData.length == 0) {
                message = "No se recibieron datos para imprimir.";
                response = Map.of("message", message);
                statusCode = 400; // Bad Request
                sendResponse(exchange, response, statusCode);
                return;
            }

            // 5. Enviar los datos a la impresora
            LOGGER.log(Level.INFO, "Enviando {0} bytes a la impresora: {1} ({2}:{3})",
                    new Object[]{printData.length, config.getNombre(), config.getIp(), config.getPuerto()});

            //sendToPrinter(config, printData);
            Printescpos.printTcpIp(config);

            // 6. Enviar respuesta exitosa
            message = "Trabajo enviado a la impresora '" + printerName + "' exitosamente.";
            //response = Map.of("message", message);
            statusCode = 200; // OK
            LOGGER.info(message);

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

    private void sendToPrinter(PrinterConfig config, byte[] data) throws IOException {
        // Usamos try-with-resources para asegurar que el socket y los streams se cierren
        try (Socket socket = new Socket(config.getIp(), config.getPuerto()); OutputStream out = socket.getOutputStream()) {
            // Establecer un timeout de conexión y lectura podría ser buena idea
            socket.setSoTimeout(5000); // 5 segundos de timeout de lectura/escritura

            LOGGER.log(Level.FINE, "Conectado a {0}:{1}", new Object[]{config.getIp(), config.getPuerto()});

            // Enviar los datos directamente. Asume que 'data' ya contiene los comandos ESC/POS
            // o el texto formateado correctamente con la codificación esperada por la impresora.
            out.write(data);
            out.flush(); // Asegurar que todos los datos se envíen

            LOGGER.log(Level.FINE, "Datos enviados correctamente.");

            // CONSIDERACIÓN IMPORTANTE SOBRE ESC/POS:
            // Muchas impresoras ESC/POS requieren comandos específicos al final,
            // como un corte de papel (GS V 1) o un avance de línea (LF).
            // Si tus clientes envían solo el texto, podrías añadir esos comandos aquí:
            // Ejemplo: Añadir un salto de línea y corte parcial
            // byte[] LF = new byte[]{0x0A}; // Line Feed
            // byte[] PARTIAL_CUT = new byte[]{0x1D, 0x56, 0x42, 0x00}; // GS V m=66 n=0
            // out.write(LF);
            // out.write(PARTIAL_CUT);
            // out.flush();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al conectar o enviar datos a la impresora " + config.getNombre(), e);
            throw e; // Relanzar para que el handler sepa que hubo un error
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
