package httpHandle.util;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DocumentParser {

    private static final Logger LOGGER = Logger.getLogger(DocumentParser.class.getName());
    private static final Gson GSON = new Gson();

    private DocumentParser() {
    }

    public static <T> T parsear(InputStream json, Class<T> tipo) {
        if (json == null) {
            LOGGER.log(Level.WARNING, "InputStream es nulo");
            return null;
        }

        try (InputStreamReader reader = new InputStreamReader(json, StandardCharsets.UTF_8)) {
            T documento = GSON.fromJson(reader, tipo);
            
            if (documento == null) {
                LOGGER.log(Level.WARNING, "El documento parseado es nulo para tipo: {0}", tipo.getName());
            }
            
            return documento;
            
        } catch (JsonSyntaxException e) {
            LOGGER.log(Level.SEVERE, "Error de sintaxis JSON: {0}", e.getMessage());
            return null;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al parsear documento: {0}", e.getMessage());
            return null;
        }
    }
}