package com.bizlinks.helpdesk.service;

import com.bizlinks.helpdesk.config.AppConfig;
import com.bizlinks.helpdesk.model.DocumentRecord;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CompositePdfProvider implements PdfProvider {
    private static final Logger LOG = LoggerFactory.getLogger(CompositePdfProvider.class);
    private final AppConfig config;
    private final PdfProvider hessian;

    public CompositePdfProvider(AppConfig config) {
        this.config = config;
        this.hessian = new HessianPdfProvider(config.pdfHessianUrl(), config.pdfHessianFallbackUrl());
    }

    @Override public byte[] getPdf(DocumentRecord document) throws Exception {
        Exception fileFailure = null;
        try {
            byte[] result = download(document);
            if (result != null && result.length > 0) return result;
            LOG.info("Endpoint de archivos no devolvió PDF para tipo={} documento={}; probando Hessian",
                    document.documentType(), document.documentNumber());
        } catch (Exception ex) {
            fileFailure = ex;
            LOG.warn("Falló endpoint de archivos para tipo={} documento={}: {}",
                    document.documentType(), document.documentNumber(), ex.toString());
        }
        try {
            byte[] result = hessian.getPdf(document);
            if (result == null || result.length == 0) LOG.info("Hessian no encontró PDF para tipo={} documento={}", document.documentType(), document.documentNumber());
            else LOG.info("PDF recibido desde Hessian para tipo={} documento={}, bytes={}", document.documentType(), document.documentNumber(), result.length);
            return result;
        }
        catch (Exception ex) {
            if (fileFailure != null) ex.addSuppressed(fileFailure);
            LOG.error("Fallaron las rutas PDF para tipo={} documento={}", document.documentType(), document.documentNumber(), ex);
            throw ex;
        }
    }

    private byte[] download(DocumentRecord document) throws Exception {
        String payload = "a1=PDF&d1=" + document.issuerTaxId() + "&d3=" + document.documentNumber() + "&d2=" + document.documentType();
        String encrypted = LegacyNcCrypt.encryptPassword(payload);
        URI uri = URI.create(config.pdfFileBaseUrl() + encrypted.replace("+", "%2B"));
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        connection.setConnectTimeout(config.connectTimeoutMillis());
        connection.setReadTimeout(config.readTimeoutMillis());
        connection.setRequestMethod("GET");
        try {
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) throw new IllegalStateException("Servicio de archivos devolvió HTTP " + status);
            try (InputStream in = connection.getInputStream()) { return in.readAllBytes(); }
        } finally { connection.disconnect(); }
    }
}
