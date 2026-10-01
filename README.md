# Help Desk Next

Nueva aplicación de escritorio separada del programa legado `help-desk/`, que se conserva intacto. La interfaz incluye generación/descarga de archivos, validación XML y una pestaña para probar explícitamente la conexión DB2 PROD. DBFE_old permanece aparcada; todos los flujos DB2 usan únicamente PROD.

## Funciones

- Lee el TXT legado en UTF-8, con una fila por documento y cuatro campos: `id,RUC,tipo,serie-número`.
- Genera PDF mediante el endpoint HTTPS de archivos y, como alternativa, los dos servicios Hessian.
- Recupera UBL, CDR y XML data desde DB2 usando el ID de la fila.
- Valida XML contra el servicio configurado y exporta los resultados como TXT.
- Muestra el estado por documento y por formato, con errores legibles y progreso.
- Registra eventos en consola y en `logs/help-desk.log`; conserva hasta 30 archivos diarios. Cambia la carpeta con `HELP_DESK_LOG_DIR`.
- Usa `SwingWorker`, límites de conexión/lectura y escritura temporal para evitar archivos de salida parciales.

## Requisitos

- JDK 17 o posterior.
- Maven 3.9 o posterior.
- Red/VPN con ruta TCP al servidor DB2 PROD `172.19.35.33:52000`.

## Configuración

Configura la única conexión activa en `src/main/resources/database.properties`. El archivo está excluido de Git para evitar publicar credenciales. Las propiedades `db2.old.*` están comentadas porque DBFE_old queda aparcada. El botón de prueba solo abre y cierra la sesión JDBC PROD; no descarga documentos ni llama servicios PDF/XML.

| Variable | Valor por defecto/uso |
| --- | --- |
| `db2.url` | `jdbc:db2://172.19.35.121:52000/DBFE` |
| `db2.driver` | `com.ibm.db2.jcc.DB2Driver` |
| `db2.username`, `db2.password` | Credenciales de la base principal; completar en `database.properties` |
| `db2.old.*` | Comentado; reservar para una fase posterior |
| Servicios PDF y validación XML | Aparcados; no participan en la interfaz actual |

La conexión DB2 PROD se configura estáticamente en el archivo anterior. Opcionalmente, puedes sobrescribir usuario/contraseña con variables de entorno:

```powershell
$env:DB2_USERNAME = "tu_usuario"
$env:DB2_PASSWORD = "tu_clave"
mvn package
java -jar target/help-desk-next-2.0.0-SNAPSHOT.jar
```

Desde esta carpeta, ejecuta `mvn package` y luego:

```powershell
java -jar target/help-desk-next-2.0.0-SNAPSHOT.jar
```

La causa registrada anteriormente (`Connection timed out`, `SQLSTATE=08001`, `ERRORCODE=-4499`) ocurre antes de autenticar. Si persiste en la prueba, verifica VPN, rutas de red y que firewall permita TCP a `172.19.35.121:52000`.

## Formato de entrada

```text
123456,20123456789,01,F001-00000123
123457,20123456789,07,F001-00000045
```

Las filas vacías y comentarios que comienzan con `#` se ignoran. Se validan los cuatro campos y el ID antes de procesar el lote.

## Notas de integración

- DB2 se consulta por ID en `PORTALPERU.TM_CE_DOCUMENTO`, con las columnas `ARCHIVOENVIADO`, `ARCHIVORESPUESTA` y `XMLDATA`, igual que el código existente.
- PDF se solicita con los parámetros RUC emisor, tipo de documento y número. Se intenta primero el endpoint de archivos codificado por `NcCrypt` y después los dos servicios Hessian. Los PDFs pequeños se aceptan; el programa anterior descartaba cualquier respuesta de 5000 bytes o menos.
- La codificación URL reproduce el formato 3DES `NcCrypt` del proyecto legado usando JCE estándar de Java, sin depender de RSA JSAFE.
- El validador espera el contrato JSON `Fact` y una respuesta en forma de arreglo con `code` y `description`. El mapeador `integrador-utils` es un requisito de ejecución para reproducir el XML->Fact heredado. Verifica el mapeo y el endpoint real antes de desplegarlo en producción.
- Los servicios Hessian usan por defecto las rutas internas `172.19.64.84:9080` que aparecen comentadas en la configuración original; se pueden sustituir con `PDF_HESSIAN_URL` y `PDF_HESSIAN_FALLBACK_URL`. El servicio de validación heredado sigue predeterminado a HTTP y puede cambiarse mediante `XML_VALIDATION_URL`.
- La generación por lote procesa un documento a la vez para limitar conexiones DB2 y presión sobre servicios remotos.
