package httpHandle.handlers;

import application.usecases.TestImpresionUseCase;
import com.sun.net.httpserver.HttpExchange;
import httpHandle.PrinterConfig;
import infrastructure.escpos.ImprimirTest;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;

public class TestHandler extends BaseHandler {

    private final TestImpresionUseCase useCase;

    public TestHandler(java.util.Map<String, httpHandle.PrinterConfig> printers) {
        super(printers);
        this.useCase = new TestImpresionUseCase(new ImprimirTest());
    }

    @Override
    protected void handleRequest(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String printerName = extractPrinterName(path, "/prueba/");

        PrinterConfig config = validarImpresora(exchange, printerName, "/prueba/");
        if (config == null) {
            return;
        }

        LOGGER.log(Level.INFO, "Enviando test a la impresora: {0} ({1}:{2})", 
            new Object[]{config.getNombre(), config.getIp(), config.getPuerto()});

        useCase.ejecutar(config);

        sendSuccess(exchange, "Trabajo de test enviado a la impresora '" + config.getNombre() + "' exitosamente.");
    }

    @Override
    protected boolean esMetodoValido(HttpExchange exchange) {
        return "GET".equalsIgnoreCase(exchange.getRequestMethod());
    }
}