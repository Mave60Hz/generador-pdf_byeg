package com.bizlinks.helpdesk.model;

public record DocumentRecord(long id, String issuerTaxId, String documentType, String documentNumber) {
    public DocumentRecord {
        if (id <= 0) throw new IllegalArgumentException("El ID debe ser positivo");
        if (issuerTaxId == null || issuerTaxId.isBlank()) throw new IllegalArgumentException("Falta el RUC emisor");
        if (documentType == null || documentType.isBlank()) throw new IllegalArgumentException("Falta el tipo de documento");
        if (documentNumber == null || documentNumber.isBlank()) throw new IllegalArgumentException("Falta el número de documento");
    }

    public String baseFileName() {
        if ("RC".equals(documentType) || "RA".equals(documentType)) return issuerTaxId + "-" + documentNumber;
        return issuerTaxId + "-" + documentType + "-" + documentNumber;
    }
}
