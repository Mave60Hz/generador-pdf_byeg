package com.bizlinks.helpdesk.service;

import com.bizlinks.helpdesk.model.DocumentRecord;
import com.caucho.hessian.client.HessianProxyFactory;

import java.net.URL;

public final class HessianPdfProvider implements PdfProvider {
    public interface PdfService {
        byte[] obtenerPDF(String issuerTaxId, String documentType, String documentNumber);
    }
    private final String primaryUrl;
    private final String fallbackUrl;

    public HessianPdfProvider(String primaryUrl, String fallbackUrl) {
        this.primaryUrl = primaryUrl;
        this.fallbackUrl = fallbackUrl;
    }

    @Override public byte[] getPdf(DocumentRecord document) throws Exception {
        Exception firstFailure = null;
        for (String endpoint : new String[]{primaryUrl, fallbackUrl}) {
            try {
                if (endpoint == null || endpoint.isBlank()) continue;
                PdfService service = (PdfService) new HessianProxyFactory().create(PdfService.class, new URL(endpoint).toExternalForm());
                byte[] pdf = service.obtenerPDF(document.issuerTaxId(), document.documentType(), document.documentNumber());
                if (pdf != null && pdf.length > 0) return pdf;
            } catch (Exception ex) {
                if (firstFailure == null) firstFailure = ex;
                else firstFailure.addSuppressed(ex);
            }
        }
        if (firstFailure != null) throw firstFailure;
        return null;
    }
}
