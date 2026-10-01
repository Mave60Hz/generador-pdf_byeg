package com.bizlinks.helpdesk.service;

import com.bizlinks.helpdesk.config.AppConfig.DatabaseConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Db2DocumentRepository implements DocumentRepository {
    private static final Logger LOG = LoggerFactory.getLogger(Db2DocumentRepository.class);
    private final DatabaseConfig config;

    public Db2DocumentRepository(DatabaseConfig config) { this.config = config; }

    public String testConnection() throws Exception {
        if (config.username().isBlank() || config.password().isBlank()) {
            throw new IllegalStateException("Completa db2.username y db2.password en database.properties.");
        }
        Class.forName(config.driver());
        DriverManager.setLoginTimeout(12);
        try (Connection connection = DriverManager.getConnection(config.url(), config.username(), config.password())) {
            var metadata = connection.getMetaData();
            return metadata.getDatabaseProductName() + " " + metadata.getDatabaseProductVersion();
        }
    }

    private byte[] read(long id, String column) throws Exception {
        if (!column.matches("ARCHIVOENVIADO|ARCHIVORESPUESTA|XMLDATA")) throw new IllegalArgumentException("Columna inválida");
        if (config.username().isBlank() || config.password().isBlank()) throw new IllegalStateException("Configura usuario y contraseña DB2 mediante variables de entorno.");
        Class.forName(config.driver());
        String sql = "SELECT " + column + " FROM PORTALPERU.TM_CE_DOCUMENTO WHERE ID = ? FOR READ ONLY";
        LOG.debug("Consultando DB2 para ID={} columna={}", id, column);
        try (Connection connection = DriverManager.getConnection(config.url(), config.username(), config.password());
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet result = statement.executeQuery()) {
                byte[] data = result.next() ? result.getBytes(1) : null;
                LOG.debug("Consulta DB2 para ID={} columna={} resultado={}", id, column,
                        data == null ? "sin datos" : data.length + " bytes");
                return data;
            }
        }
    }

    @Override public byte[] getUbl(long id) throws Exception { return read(id, "ARCHIVOENVIADO"); }
    @Override public byte[] getCdr(long id) throws Exception { return read(id, "ARCHIVORESPUESTA"); }
    @Override public byte[] getXmlData(long id) throws Exception { return read(id, "XMLDATA"); }
}
