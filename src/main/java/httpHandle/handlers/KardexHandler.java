package httpHandle.handlers;

import application.usecases.ImprimirKardexUseCase;
import com.sun.net.httpserver.HttpExchange;
import domain.entities.DatosGeneralesKardex;
import domain.entities.MovimientoKardex;
import domain.entities.PrinterConfig;
import httpHandle.util.DocumentParser;
import infrastructure.escpos.ImprimirKardexHtml;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;

public class KardexHandler extends BaseHandler {

	private final ImprimirKardexUseCase useCase;

	public KardexHandler() {
		this.useCase = new ImprimirKardexUseCase(new ImprimirKardexHtml());
	}

	@Override
	protected void handleRequest(HttpExchange exchange) throws IOException {
		String path = exchange.getRequestURI().getPath();
		String printerName = extractPrinterName(path, "/kardex/print/");
		PrinterConfig config = validarImpresora(exchange, printerName, "/kardex/print/");
		if (config == null) {
			return;
		}

        MovimientoKardex movimiento = DocumentParser.parsear(exchange.getRequestBody(), MovimientoKardex.class);
        if (movimiento == null || movimiento.getDatosGenerales() == null) {
            sendResponse(exchange, Map.of("message", "Error al parsear los datos de Kardex"), 400);
            return;
        }
        DatosGeneralesKardex datos = movimiento.getDatosGenerales();

		if (vacio(datos.getProducto()) || vacio(datos.getTipoMovimiento()) || vacio(datos.getLugar())) {
			sendResponse(exchange, Map.of("message",
				"Faltan datos requeridos: producto, tipoMovimiento y lugar"), 400);
			return;
		}

		LOGGER.log(Level.INFO, "Enviando movimiento de Kardex a la impresora: {0} ({1}:{2})",
			new Object[]{config.getNombre(), config.getIp(), config.getPuerto()});

		String resultado = useCase.ejecutar(config, datos, false);
		if (!"Exito".equals(resultado)) {
			sendResponse(exchange, Map.of("message", resultado), 500);
			return;
		}

		sendSuccess(exchange, "Trabajo enviado a la impresora '" + config.getNombre() + "' exitosamente.");
	}

	@Override
	protected boolean esMetodoValido(HttpExchange exchange) {
		return "POST".equalsIgnoreCase(exchange.getRequestMethod());
	}

	private boolean vacio(String valor) {
		return valor == null || valor.isBlank();
	}

}
