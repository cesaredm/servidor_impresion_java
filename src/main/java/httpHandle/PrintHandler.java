package httpHandle;

import httpHandle.handlers.ComandaHandler;
import httpHandle.handlers.CotizacionHandler;
import httpHandle.handlers.FacturaHandler;
import httpHandle.handlers.ImpresorasHandler;
import httpHandle.handlers.NotFoundHandler;
import httpHandle.handlers.PagoHandler;
import httpHandle.handlers.TestHandler;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

public class PrintHandler implements com.sun.net.httpserver.HttpHandler {

    private static final Logger LOGGER = Logger.getLogger(PrintHandler.class.getName());
    private final Map<String, PrinterConfig> printers;

    private final FacturaHandler facturaHandler;
    private final ComandaHandler comandaHandler;
    private final CotizacionHandler cotizacionHandler;
    private final PagoHandler pagoHandler;
    private final TestHandler testHandler;
    private final ImpresorasHandler impresorasHandler;
    private final NotFoundHandler notFoundHandler;

    public PrintHandler(Map<String, PrinterConfig> printers) {
        this.printers = printers;
        this.facturaHandler = new FacturaHandler(printers);
        this.comandaHandler = new ComandaHandler(printers);
        this.cotizacionHandler = new CotizacionHandler(printers);
        this.pagoHandler = new PagoHandler(printers);
        this.testHandler = new TestHandler(printers);
        this.impresorasHandler = new ImpresorasHandler(printers);
        this.notFoundHandler = new NotFoundHandler(printers);
    }

    @Override
    public void handle(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();

        if (path.equals("/impresoras")) {
            impresorasHandler.handle(exchange);
            return;
        }

        if (path.startsWith("/print/")) {
            facturaHandler.handle(exchange);
            return;
        }

        if (path.startsWith("/comanda/print/")) {
            comandaHandler.handle(exchange);
            return;
        }

        if (path.startsWith("/cotizacion/print/")) {
            cotizacionHandler.handle(exchange);
            return;
        }

        if (path.startsWith("/pago/print/")) {
            pagoHandler.handle(exchange);
            return;
        }

        if (path.startsWith("/prueba/")) {
            testHandler.handle(exchange);
            return;
        }

        notFoundHandler.handle(exchange);
    }
}