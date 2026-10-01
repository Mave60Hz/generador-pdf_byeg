package com.bizlinks.helpdesk.model;

public record GenerationRow(DocumentRecord document, FileStatus pdf, FileStatus ubl, FileStatus cdr, FileStatus xmlData, String detail) {
    public static GenerationRow pending(DocumentRecord document) {
        return new GenerationRow(document, FileStatus.PENDING, FileStatus.PENDING, FileStatus.PENDING, FileStatus.PENDING, "");
    }
    public GenerationRow withPdf(FileStatus value, String message) { return new GenerationRow(document, value, ubl, cdr, xmlData, append(message)); }
    public GenerationRow withUbl(FileStatus value, String message) { return new GenerationRow(document, pdf, value, cdr, xmlData, append(message)); }
    public GenerationRow withCdr(FileStatus value, String message) { return new GenerationRow(document, pdf, ubl, value, xmlData, append(message)); }
    public GenerationRow withXml(FileStatus value, String message) { return new GenerationRow(document, pdf, ubl, cdr, value, append(message)); }
    private String append(String message) { return detail.isBlank() ? message : detail + "; " + message; }
}
