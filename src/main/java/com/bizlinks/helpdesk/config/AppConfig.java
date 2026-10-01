package com.bizlinks.helpdesk.config;

import java.util.Optional;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record AppConfig(
        DatabaseConfig primaryDatabase,
        DatabaseConfig legacyDatabase,
        String pdfFileBaseUrl,
        String pdfHessianUrl,
        String pdfHessianFallbackUrl,
        String validationUrl,
        int connectTimeoutMillis,
        int readTimeoutMillis) {
    private static final Logger LOG = LoggerFactory.getLogger(AppConfig.class);
    public static final String PROD_DB2_URL = "jdbc:db2://172.19.35.121:52000/DBFE";

    public static AppConfig fromEnvironment() {
        Properties database = new Properties();
        try (InputStream stream = AppConfig.class.getClassLoader().getResourceAsStream("database.properties")) {
            if (stream == null) throw new IllegalStateException("No se encontró database.properties en el classpath.");
            database.load(stream);
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo leer database.properties.", ex);
        }
        LOG.info("Configuración DB2 PROD cargada: {}", database.getProperty("db2.url", PROD_DB2_URL));
        return new AppConfig(
                database(database, "db2", PROD_DB2_URL),
                // TODO: Reactivar DBFE_old cuando se solicite trabajar con la base alterna.
                database(database, "db2.old", "jdbc:db2://172.19.35.89:52000/DBFE"),
                env("PDF_FILE_URL", "https://integrador.bizlinks.com.pe/sfeperufiles/files/"),
                // Endpoints internos de la configuración original; las rutas testing respondieron 404.
                env("PDF_HESSIAN_URL", "http://172.19.64.84:9080/pdfgen-mod1-war/remoting/GeneracionPDF"),
                env("PDF_HESSIAN_FALLBACK_URL", "http://172.19.64.84:9080/pdfgen-mod3-war/remoting/GeneracionPDF"),
                env("XML_VALIDATION_URL", "http://ec2-54-244-165-155.us-west-2.compute.amazonaws.com:9084/validate"),
                integer("HTTP_CONNECT_TIMEOUT_MS", 10_000),
                integer("HTTP_READ_TIMEOUT_MS", 60_000));
    }

    private static DatabaseConfig database(Properties properties, String prefix, String defaultUrl) {
        String envPrefix = prefix.toUpperCase().replace('.', '_');
        return new DatabaseConfig(env(envPrefix + "_URL", properties.getProperty(prefix + ".url", defaultUrl)),
                env(envPrefix + "_DRIVER", properties.getProperty(prefix + ".driver", "com.ibm.db2.jcc.DB2Driver")),
                env(envPrefix + "_USERNAME", properties.getProperty(prefix + ".username", "")),
                env(envPrefix + "_PASSWORD", properties.getProperty(prefix + ".password", "")));
    }

    private static String env(String key, String fallback) {
        return Optional.ofNullable(System.getenv(key)).filter(v -> !v.isBlank()).orElse(fallback);
    }

    private static int integer(String key, int fallback) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) return fallback;
        try { return Integer.parseInt(value); }
        catch (NumberFormatException ex) { throw new IllegalArgumentException(key + " debe ser un entero", ex); }
    }

    public record DatabaseConfig(String url, String driver, String username, String password) { }
}
