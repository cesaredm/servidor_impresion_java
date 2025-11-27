/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package httpHandle;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import domain.Impresora;
import entities.Comanda;
import entities.Cotizacion;
import entities.Factura;
import entities.Pago;
import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets; // O el charset que necesite tu impresora
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import infra.PrinterFactory;

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

    public PrinterConfig validacionImpresora(HttpExchange exchange, String ruta) {
        int statusCode = 500;
        String message = "";
        Map<String, Object> response; // para crear la respuesta
        try {
            URI requestURI = exchange.getRequestURI();
            String path = requestURI.getPath(); // Debería ser /print/nombreImpresora
            String printerName = extractPrinterName(path, ruta);

            if (printerName == null || printerName.isEmpty()) {
                message = "Nombre de impresora no especificado en la URL. Use /prueba/{nombreImpresora}";
                response = Map.of("message", message);
                statusCode = 400; // Bad Request
                sendResponse(exchange, response, statusCode);
                return null;
            }

            // 3. Buscar la configuración de la impresora
            PrinterConfig config = printers.get(printerName);
            if (config == null) {
                message = "Impresora -" + printerName + "- no encontrada en la configuración.";
                response = Map.of("message", message);
                statusCode = 404; // Not Found
                sendResponse(exchange, response, statusCode);
                return null;
            }
            return config;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
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
            //Obtener las impresoras instaladas en la maquina
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod()) && url.getPath().equals("/impresoras")) {
                statusCode = 200;
                response = Map.of("Impresoras", Printescpos.listaImpresorasDisponibles());
                sendResponse(exchange, response, statusCode);
                return;
            }
            // Verificar método (solo aceptamos POST) para la ruta /print
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod()) && url.getPath().startsWith("/print")) {
                message = "Método no permitido. Use POST.";
                response = Map.of("message", message);
                statusCode = 405; // Method Not Allowed

                sendResponse(exchange, response, statusCode);
                return;
            }
            // Imprimir factura
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod()) && url.getPath().startsWith("/print")) {
                // Extraer nombre de la impresora de la URL
                URI requestURI = exchange.getRequestURI();
                String path = requestURI.getPath(); // Debería ser /print/nombreImpresora
                String printerName = extractPrinterName(path, "/print/");

                if (printerName == null || printerName.isEmpty()) {
                    message = "Nombre de impresora no especificado en la URL. Use /print/{nombreImpresora}";
                    response = Map.of("message", message);
                    statusCode = 400; // Bad Request
                    sendResponse(exchange, response, statusCode);
                    return;
                }

                // Buscar la configuración de la impresora
                PrinterConfig config = printers.get(printerName);
                if (config == null) {
                    message = "Impresora '" + printerName + "' no encontrada en la configuración.";
                    response = Map.of("message", message);
                    statusCode = 404; // Not Found
                    sendResponse(exchange, response, statusCode);
                    return;
                }

                // Leer los datos a imprimir del cuerpo de la petición
                InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
                //creamos una una clase de tipo Factura a partir de el json recibido
                Factura factura = json.fromJson(reader, Factura.class);
                // Enviar los datos a la impresora
                LOGGER.log(Level.INFO, "Enviando datos a la impresora: {1} ({2}:{3})", new Object[]{config.getNombre(), config.getIp(), config.getPuerto()});
                // logica de imprimir la factura, con las copias o no
                Impresora impresoraTicket = (Impresora<Factura>) PrinterFactory.getPrinter(config.getTipoConexion(), "factura");

                if (config.getCopias() == 0) {
                    impresoraTicket.imprimir(config, factura, false);
                }

                for (int i = 0; i < config.getCopias(); i++) {
                    if (i == 0) {
                        impresoraTicket.imprimir(config, factura, false);
                    } else {
                        impresoraTicket.imprimir(config, factura, true);
                    }
                }

                // Enviar respuesta exitosa
                message = "Trabajo enviado a la impresora '" + printerName + "' exitosamente.";
                statusCode = 200; // OK
                LOGGER.info(message);
                response = Map.of("message", message);
                //Esto ayuda a evitar que el cliente corte la conexión tras cada impresión.
                exchange.getResponseHeaders().set("Connection", "keep-alive");
                // Establecer un tiempo de espera de 1 hora (3600 segundos) y un máximo de 1000 peticiones.
                exchange.getResponseHeaders().set("Keep-Alive", "timeout=3600, max=1000");
                sendResponse(exchange, response, statusCode);
                return;
            }

            // imprimir comanda
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod()) && url.getPath().startsWith("/comanda/print")) {
                // validar datos y configuracion de la impresora
                PrinterConfig config = validacionImpresora(exchange, "/comanda/print/");

                // Leer los datos a imprimir del cuerpo de la petición
                InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
                //creamos una una clase de tipo Factura a partir de el json recibido
                Comanda comanda = json.fromJson(reader, Comanda.class);
                // Enviar los datos a la impresora
                LOGGER.log(Level.INFO, "Enviando datos a la impresora: {1} ({2}:{3})", new Object[]{config.getNombre(), config.getIp(), config.getPuerto()});
                Impresora impresoraTicket = (Impresora<Comanda>) PrinterFactory.getPrinter(config.getTipoConexion(), "comanda");
                // logica de imprimir la factura, con las copias o no
                if (config.getCopias() == 0) {
                    impresoraTicket.imprimir(config, comanda, false);
                }

                for (int i = 0; i < config.getCopias(); i++) {
                    if (i == 0) {
                        impresoraTicket.imprimir(config, comanda, true);
                    } else {
                        impresoraTicket.imprimir(config, comanda, true);
                    }
                }

                // Enviar respuesta exitosa
                message = "Trabajo enviado a la impresora '" + config.getNombre() + "' exitosamente.";
                statusCode = 200; // OK
                LOGGER.info(message);
                response = Map.of("message", message);
                //Esto ayuda a evitar que el cliente corte la conexión tras cada impresión.
                exchange.getResponseHeaders().set("Connection", "keep-alive");
                // Establecer un tiempo de espera de 1 hora (3600 segundos) y un máximo de 1000 peticiones.
                exchange.getResponseHeaders().set("Keep-Alive", "timeout=3600, max=1000");
                sendResponse(exchange, response, statusCode);
                return;
            }
            // Imprimir cotizacion
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod()) && url.getPath().startsWith("/cotizacion/print")) {
                PrinterConfig config = validacionImpresora(exchange, "/cotizacion/print/");
                // Leer los datos a imprimir del cuerpo de la petición
                InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
                //creamos una una clase de tipo Factura a partir de el json recibido
                Cotizacion factura = json.fromJson(reader, Cotizacion.class);
                // Enviar los datos a la impresora
                LOGGER.log(Level.INFO, "Enviando datos a la impresora: {1} ({2}:{3})", new Object[]{config.getNombre(), config.getIp(), config.getPuerto()});
                // logica de imprimir la factura, con las copias o no
                Impresora impresoraTicket = (Impresora<Cotizacion>) PrinterFactory.getPrinter(config.getTipoConexion(), "cotizacion");

                if (config.getCopias() == 0) {
                    impresoraTicket.imprimir(config, factura, false);
                }

                for (int i = 0; i < config.getCopias(); i++) {
                    if (i == 0) {
                        impresoraTicket.imprimir(config, factura, false);
                    } else {
                        impresoraTicket.imprimir(config, factura, true);
                    }
                }

                // Enviar respuesta exitosa
                message = "Trabajo enviado a la impresora '" + config.getNombre() + "' exitosamente.";
                statusCode = 200; // OK
                LOGGER.info(message);
                response = Map.of("message", message);
                //Esto ayuda a evitar que el cliente corte la conexión tras cada impresión.
                exchange.getResponseHeaders().set("Connection", "keep-alive");
                // Establecer un tiempo de espera de 1 hora (3600 segundos) y un máximo de 1000 peticiones.
                exchange.getResponseHeaders().set("Keep-Alive", "timeout=3600, max=1000");
                sendResponse(exchange, response, statusCode);
                return;
            }
             // Imprimir baucher de pago
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod()) && url.getPath().startsWith("/pago/print")) {
                PrinterConfig config = validacionImpresora(exchange, "/pago/print/");
                // Leer los datos a imprimir del cuerpo de la petición
                InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
                //creamos una una clase de tipo Factura a partir de el json recibido
                Pago pago = json.fromJson(reader, Pago.class);
                // Enviar los datos a la impresora
                LOGGER.log(Level.INFO, "Enviando datos a la impresora: {1} ({2}:{3})", new Object[]{config.getNombre(), config.getIp(), config.getPuerto()});
                // logica de imprimir la factura, con las copias o no
                Impresora impresoraTicket = (Impresora<Pago>) PrinterFactory.getPrinter(config.getTipoConexion(), "pago");

                if (config.getCopias() == 0) {
                    impresoraTicket.imprimir(config, pago, false);
                }

                for (int i = 0; i < config.getCopias(); i++) {
                    if (i == 0) {
                        impresoraTicket.imprimir(config, pago, false);
                    } else {
                        impresoraTicket.imprimir(config, pago, false);
                    }
                }

                // Enviar respuesta exitosa
                message = "Trabajo enviado a la impresora '" + config.getNombre() + "' exitosamente.";
                statusCode = 200; // OK
                LOGGER.info(message);
                response = Map.of("message", message);
                //Esto ayuda a evitar que el cliente corte la conexión tras cada impresión.
                exchange.getResponseHeaders().set("Connection", "keep-alive");
                // Establecer un tiempo de espera de 1 hora (3600 segundos) y un máximo de 1000 peticiones.
                exchange.getResponseHeaders().set("Keep-Alive", "timeout=3600, max=1000");
                sendResponse(exchange, response, statusCode);
                return;
            }
            // imprimir test de impresion
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod()) && url.getPath().startsWith("/prueba")) {
                // validacion de datos y configuracion de impresora
                PrinterConfig config = validacionImpresora(exchange, "/prueba/");

                if (Objects.isNull(config)) {
                    return;
                }

                Impresora testImpresion = (Impresora<NullPointerException>) PrinterFactory.getPrinter(config.getTipoConexion(), "test");

                testImpresion.imprimir(config, null, false);

                message = "Trabajo de test enviado a la impresora '" + config.getNombre() + "' exitosamente.";
                //response = Map.of("message", message);
                statusCode = 200; // OK
                LOGGER.info(message);
                response = Map.of("message", message);
                sendResponse(exchange, response, statusCode);
                return;
            }
        } catch (IOException e) {
            message = "Error de E/S al procesar la impresión para '"
                    + extractPrinterName(exchange.getRequestURI().getPath(), "") + "': " + e.getMessage();
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

    private String extractPrinterName(String path, String ruta) {
        // Extraer de "/print/nombre" -> "nombre"
        if (path != null && path.startsWith(ruta)) {
            String name = path.substring(ruta.length());
            // Podrías decodificar URL si esperas caracteres especiales:
            // return java.net.URLDecoder.decode(name, StandardCharsets.UTF_8);
            return name;
        }
        return null;
    }

    // funcion para la preparacion de la informacion de respuesta
    private void sendResponse(HttpExchange exchange, Map<String, Object> response, int statusCode) throws IOException {
        // creamos json para la respuesta
        String jsonResponse = this.json.toJson(response);
        // Asegúrate de que la respuesta sea UTF-8 para compatibilidad
        byte[] responseBytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
        //exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }
}
