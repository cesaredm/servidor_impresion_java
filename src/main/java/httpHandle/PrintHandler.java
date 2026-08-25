package httpHandle;

import httpHandle.handlers.ComandaHandler;
import httpHandle.handlers.CotizacionHandler;
import httpHandle.handlers.FacturaHandler;
import httpHandle.handlers.ImpresorasHandler;
import httpHandle.handlers.KardexHandler;
import httpHandle.handlers.NotFoundHandler;
import httpHandle.handlers.PagoHandler;
import httpHandle.handlers.TestHandler;
import java.io.IOException;
import java.util.logging.Logger;
import com.sun.net.httpserver.HttpExchange;
import httpHandle.handlers.PrinterNetworkHandler;

public class PrintHandler implements com.sun.net.httpserver.HttpHandler {

    private static final Logger LOGGER = Logger.getLogger(PrintHandler.class.getName());

    private final FacturaHandler facturaHandler;
    private final ComandaHandler comandaHandler;
    private final CotizacionHandler cotizacionHandler;
    private final PagoHandler pagoHandler;
    private final TestHandler testHandler;
    private final ImpresorasHandler impresorasHandler;
    private final KardexHandler kardexHandler;
    private final NotFoundHandler notFoundHandler;
		private final PrinterNetworkHandler printerNetworkHandler;

    public PrintHandler() {
        this.facturaHandler = new FacturaHandler();
        this.comandaHandler = new ComandaHandler();
        this.cotizacionHandler = new CotizacionHandler();
        this.pagoHandler = new PagoHandler();
        this.testHandler = new TestHandler();
        this.impresorasHandler = new ImpresorasHandler();
        this.kardexHandler = new KardexHandler();
				this.printerNetworkHandler = new PrinterNetworkHandler();
        this.notFoundHandler = new NotFoundHandler();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
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

        if (path.startsWith("/kardex/print/")) {
            kardexHandler.handle(exchange);
            return;
        }

        if (path.startsWith("/prueba/")) {
            testHandler.handle(exchange);
            return;
        }
				
				if(path.startsWith("/printers/")){
					printerNetworkHandler.handle(exchange);
					return;
				}

        notFoundHandler.handle(exchange);
    }
}
