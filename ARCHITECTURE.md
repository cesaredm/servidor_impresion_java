# Arquitectura del Proyecto

## Visión General

Este proyecto sigue los principios de **Clean Architecture** y **Hexagonal Architecture** (Ports and Adapters) para separar la lógica de negocio de las implementaciones externas.

## Estructura de Paquetes

```
src/main/java/
├── domain/                      # Capa de Dominio
│   ├── PrinterConfig.java       # Entidad de configuración de impresoras
│   ├── entities/                # Entidades del dominio
│   │   ├── Factura.java
│   │   ├── Comanda.java
│   │   ├── Pago.java
│   │   ├── Cotizacion.java
│   │   ├── Tienda.java
│   │   ├── DatosGenerales.java
│   │   ├── DatosGeneralesComanda.java
│   │   ├── DatosGeneralesCotizacion.java
│   │   ├── Detalles.java
│   │   └── Totales.java
│   └── ports/
│       └── out/
│           └── ImpresoraPort.java  # Interfaz/Contrato del dominio
│
├── application/                 # Capa de Aplicación
│   └── usecases/               # Casos de uso
│       ├── ImprimirFacturaUseCase.java
│       ├── ImprimirComandaUseCase.java
│       ├── ImprimirPagoUseCase.java
│       ├── ImprimirCotizacionUseCase.java
│       └── TestImpresionUseCase.java
│
├── infrastructure/              # Capa de Infraestructura
│   ├── escpos/                 # Implementación ESCPOS
│   │   ├── AjustesImpresion.java         # Estilos y utilidades de impresión
│   │   ├── EscposConnectionFactory.java  # Factory para conexiones USB/Red
│   │   ├── ConfiguracionesImpresion.java # Utilidades de impresoras
│   │   ├── ImprimirFactura.java          # Implementación para facturas
│   │   ├── ImprimirComanda.java          # Implementación para comandas
│   │   ├── ImprimirPago.java             # Implementación para pagos
│   │   ├── ImprimirCotizacion.java       # Implementación para cotizaciones
│   │   └── ImprimirTest.java             # Implementación para test
│   └── Mdns.java               # Servicio mDNS para descubrimiento
│
├── httpHandle/                 # Capa de Interfaz HTTP
│   ├── PrintHandler.java       # Enrutador principal
│   └── handlers/               # Handlers HTTP
│       ├── BaseHandler.java            # Clase base con lógica común
│       ├── FacturaHandler.java         # Handler para facturas
│       ├── ComandaHandler.java        # Handler para comandas
│       ├── PagoHandler.java           # Handler para pagos
│       ├── CotizacionHandler.java    # Handler para cotizaciones
│       ├── TestHandler.java          # Handler para test
│       ├── ImpresorasHandler.java   # Handler para listar impresoras
│       ├── NotFoundHandler.java     # Handler para rutas no encontradas
│       └── ConfigHandler.java       # Handler para recargar configuración
│
├── util/
│   └── DocumentParser.java     # Utilidad para parsear JSON
│
└── PrintServer.java           # Punto de entrada del servidor
```

## Flujo de una Petición

### Ejemplo: Imprimir Factura

```
1. Cliente HTTP
   │
   ▼
2. PrintHandler (enruta según la URL)
   │
   ▼
3. FacturaHandler (parsea JSON → valida config)
   │
   ▼
4. ImprimirFacturaUseCase (orquesta la impresión)
   │
   ▼
5. ImprimirFactura (implementa ImpresoraPort)
   │
   ▼
6. EscposConnectionFactory (crea conexión USB o Red)
   │
   ▼
7. EscPos (impresión real con escpos-coffee)
```

## Capas Explicadas

### 1. Domain (Dominio)

**Responsabilidad:** Reglas de negocio y entidades del dominio.

**Contenido:**
- `PrinterConfig` - Configuración de impresoras
- Entidades (Factura, Comanda, Pago, etc.)
- `ImpresoraPort` - Interfaz que define el contrato de impresión

**Nota:** Esta capa no depende de ninguna otra. Define "qué" se hace, no "cómo".

### 2. Application (Aplicación)

**Responsabilidad:** Casos de uso que orquestan la lógica de negocio.

**Contenido:**
- Use Cases (ImprimirFacturaUseCase, etc.)

**Características:**
- Reciben la implementación de `ImpresoraPort` por inyección de dependencias
- No saben cómo se implementa la impresión, solo usan el contrato

### 3. Infrastructure (Infraestructura)

**Responsabilidad:** Implementaciones externas y detalles técnicos.

**Contenido:**
- Implementaciones de `ImpresoraPort` (ImprimirFactura, etc.)
- `EscposConnectionFactory` - Crea conexiones USB o Red
- `AjustesImpresion` - Estilos y utilidades de impresión

**Características:**
- Usa la librería `escpos-coffee` de anastaciocintra
- Maneja la conexión física a las impresoras

### 4. HTTP Handle (Interfaz)

**Responsabilidad:** Recibir y responder peticiones HTTP.

**Contenido:**
- `PrintHandler` - Enruta las peticiones al handler correcto
- Handlers específicos para cada tipo de documento

**Patrones:**
- Cada handler extiende `BaseHandler` para lógica común (CORS, validaciones, respuestas)
- Usa `DocumentParser` para parsear JSON de forma reutilizable

## Cómo Agregar un Nuevo Tipo de Documento

### Pasos:

1. **Crear la entidad en `domain/entities/`** (si no existe)

2. **Crear la implementación en `infrastructure/escpos/`**
   ```java
   public class ImprimirNuevoDocumento extends AjustesImpresion 
       implements ImpresoraPort<NuevoDocumento> {
       
       @Override
       public String imprimir(PrinterConfig config, NuevoDocumento doc, boolean copias) {
           // Lógica de impresión
       }
   }
   ```

3. **Crear el Use Case en `application/usecases/`**
   ```java
   public class ImprimirNuevoDocumentoUseCase {
       private final ImpresoraPort<NuevoDocumento> impresoraPort;
       
       public ImprimirNuevoDocumentoUseCase(ImpresoraPort<NuevoDocumento> impresoraPort) {
           this.impresoraPort = impresoraPort;
       }
       
       public String ejecutar(PrinterConfig config, NuevoDocumento doc, boolean copias) {
           return impresoraPort.imprimir(config, doc, copias);
       }
   }
   ```

4. **Crear el Handler en `httpHandle/handlers/`**
   ```java
   public class NuevoDocumentoHandler extends BaseHandler {
       private final ImprimirNuevoDocumentoUseCase useCase;
       
       public NuevoDocumentoHandler(Map<String, PrinterConfig> printers) {
           super(printers);
           this.useCase = new ImprimirNuevoDocumentoUseCase(new ImprimirNuevoDocumento());
       }
       
       @Override
       protected void handleRequest(HttpExchange exchange) {
           // Parsear documento, validar, imprimir
       }
       
       @Override
       protected boolean esMetodoValido(HttpExchange exchange) {
           return "POST".equalsIgnoreCase(exchange.getRequestMethod());
       }
   }
   ```

5. **Registrar la ruta en `PrintHandler.java`**
   ```java
   if (path.startsWith("/nuevo/print/")) {
       nuevoDocumentoHandler.handle(exchange);
       return;
   }
   ```

## Tecnologías Usadas

- **Java 23** - Lenguaje de programación
- **escpos-coffee** - Librería para impresión ESC/POS
- **Gson** - Parseo de JSON
- **Apache Commons Daemon** - Servicio Windows
- **HttpServer** - Servidor HTTP embebido de Java

## Convenciones

- **Paquetes:** Lowercase (domain, application, infrastructure, httpHandle)
- **Clases:** PascalCase (ImprimirFactura, FacturaHandler)
- **Interfaces:** Nombre terminated en Port (ImpresoraPort)
- **Use Cases:** Nombre terminated en UseCase (ImprimirFacturaUseCase)
- **Handlers:** Nombre terminated en Handler (FacturaHandler)

---

Para información sobre configuración y uso, ver [README.md](../README.md)
