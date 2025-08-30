/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package domain;

import com.github.anastaciocintra.escpos.EscPos;
import com.github.anastaciocintra.escpos.EscPosConst;
import com.github.anastaciocintra.escpos.PrintModeStyle;
import com.github.anastaciocintra.escpos.Style;
import httpHandle.Printescpos;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 *
 * @author cesar
 */
public abstract class AjustesImpresion {

    protected final static Logger LOGGER = Logger.getLogger(Printescpos.class.getName());
    protected EscPos print;
    protected int papelAncho = 48;
    protected final int anchoTitulos = "Cant".length() + "Precio".length() + "Total".length();
    protected final DecimalFormat formatDecimal = new DecimalFormat("###,###,###,##0.00");
    protected Style title = new Style()
            .setJustification(EscPosConst.Justification.Center)
            .setBold(true)
            .setFontSize(Style.FontSize._2, Style.FontSize._2)
            .setFontName(Style.FontName.Font_B);
    protected PrintModeStyle campo = new PrintModeStyle().setBold(true);
    protected PrintModeStyle bold = new PrintModeStyle().setBold(true);
    protected PrintModeStyle nota = new PrintModeStyle().setBold(true).setJustification(EscPosConst.Justification.Center);

    // funcion encargada de contar el numero de caracteres de las cantidades agrgandole el formato decimal
    public int espacioCantidades(float value) {
        return formatDecimal.format(value).length();
    }
    
    public static String texto(String value) {
        /*return value.replace("á", "\u00E1") // Código Unicode para "á"
             .replace("é", "\u00E9") // Código Unicode para "é"
             .replace("í", "\u00ED") // Código Unicode para "í"
             .replace("ó", "\u00F3") // Código Unicode para "ó"
             .replace("ú", "\u00FA") // Código Unicode para "ú"
             .replace("ñ", "\u00F1"); // Código Unicode para "ñ"*/
        return value.replace("á", "a") // Código Unicode para "á"
                .replace("é", "e") // Código Unicode para "é"
                .replace("í", "i") // Código Unicode para "í"
                .replace("ó", "o") // Código Unicode para "ó"
                .replace("ú", "u") // Código Unicode para "ú"
                .replace("ñ", "n"); // Código Unicode para "ñ"
    }

    /*
        @Params ancho = 48, campo = logitud del nombre del campo, resto  = longitud de contenido a la derecha
        return espacios
        funcion encargada de calcular el espacio dinamico de dos columnas en forma between
     */
    public String espacio(int ancho, int campo, int resto) {
        int espacios = ancho - campo - resto;
        if (espacios < 0) {
            espacios = 0;
        }
        return " ".repeat(espacios);
    }

    /*
        funcion encargada de dar espacio dinamico a tres columnas
     */
    public String espacioTresColumnas(int ancho, int caracterres) {
        // el ancho normalmente es de 48 caracteres -> dinamico
        // retorna los espacios
        int espacios = (ancho - caracterres) / 2;
        if (espacios < 0) {
            espacios = 0;
        }
        return " ".repeat(espacios);
    }

    /*
        funcion encargada de realizar la peticion de el logo al servidor de cdsoft
        esto obtine el logo de la tienda
     */
    public BufferedImage obtenerImagenDeUrl(String urlImagen) throws IOException {
        URL url = new URL(urlImagen);
        HttpURLConnection conexion = (HttpURLConnection) url.openConnection();

        try {
            conexion.setRequestMethod("GET"); // O el método que necesites (POST, etc.)
            conexion.connect();

            int codigoRespuesta = conexion.getResponseCode();
            if (codigoRespuesta == HttpURLConnection.HTTP_OK) {
                InputStream inputStream = conexion.getInputStream();
                return ImageIO.read(inputStream);
            } else {
                throw new IOException("Error al obtener la imagen. Código de respuesta: " + codigoRespuesta);
            }
        } finally {
            conexion.disconnect();
        }
    }
    
    /*
        @Params imageUrl = direccion de la imagen , outputFilePath = donde se guardara la imagen
        Funcion para descargar una imagen
     */
    public static void downloadImage(String imageUrl, String outputFilePath) throws IOException {
        // Crear el objeto URL a partir de la URL de la imagen
        URL url = new URL(imageUrl);

        // Descargar la imagen desde la URL
        BufferedImage image = ImageIO.read(url);

        // Verificar si la imagen fue descargada correctamente
        if (image != null) {
            // Crear un archivo en la ruta especificada y guardar la imagen
            File outputFile = new File(outputFilePath);
            // Guardar la imagen como archivo PNG (puedes cambiar el formato si lo deseas)
            ImageIO.write(image, "PNG", outputFile);
            System.out.println("Imagen guardada exitosamente en: " + outputFile.getAbsolutePath());
        } else {
            System.err.println("No se pudo descargar la imagen desde la URL.");
        }
    }

    //funcion para obtener la imagen o logo local desde la carpeta de impresorasConfig
    public BufferedImage obtenerImagenLocal(String urlImage) throws IOException {
        try {
            BufferedImage image = ImageIO.read(new File("C:/impresorasConfig/" + urlImage));
            return image;
        } finally {

        }
    }

    /*
        @Params originalImage = bufferedImage de la imagen a redimencionar, width = ancho en pixeles 
        cambiar tamano de imagen para impresion, respetando relacion aspecto
     */
    public BufferedImage resizeImage(BufferedImage originalImage, int width) {
        // Calcular el alto manteniendo la relación de aspecto
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();
        int height = (originalHeight * width) / originalWidth;

        Image temp = originalImage.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        BufferedImage resizedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = resizedImage.createGraphics();
        g2d.drawImage(temp, 0, 0, null);
        g2d.dispose();

        return resizedImage;
    }

}
