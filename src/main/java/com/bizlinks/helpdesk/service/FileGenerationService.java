package com.bizlinks.helpdesk.service;

import com.bizlinks.helpdesk.model.DocumentRecord;
import com.bizlinks.helpdesk.model.FileStatus;
import com.bizlinks.helpdesk.model.GenerationRow;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.StandardCopyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FileGenerationService {
    private static final Logger LOG = LoggerFactory.getLogger(FileGenerationService.class);
    private final DocumentRepository repository;
    private final PdfProvider pdfProvider;

    public FileGenerationService(DocumentRepository repository, PdfProvider pdfProvider) {
        this.repository = repository;
        this.pdfProvider = pdfProvider;
    }

    public GenerationRow generate(DocumentRecord document, GenerationOptions options, Path output) {
        GenerationRow row = GenerationRow.pending(document);
        if (options.pdf()) {
            if ("RC".equals(document.documentType()) || "RA".equals(document.documentType())) row = row.withPdf(FileStatus.NOT_FOUND, "PDF no disponible para RC/RA");
            else {
                try { row = write(row, pdfProvider.getPdf(document), output.resolve(document.baseFileName() + ".pdf"), (current, value, detail) -> current.withPdf(value, detail)); }
                catch (Exception ex) { LOG.error("Error generando PDF para ID={} tipo={} documento={}", document.id(), document.documentType(), document.documentNumber(), ex); row = row.withPdf(FileStatus.ERROR, message(ex)); }
            }
        }
        if (options.ubl()) {
            try { row = write(row, repository.getUbl(document.id()), output.resolve(document.baseFileName() + ".zip"), (current, value, detail) -> current.withUbl(value, detail)); }
            catch (Exception ex) { LOG.error("Error recuperando UBL para ID={}", document.id(), ex); row = row.withUbl(FileStatus.ERROR, message(ex)); }
        }
        if (options.cdr()) {
            try { row = write(row, repository.getCdr(document.id()), output.resolve("R-" + document.baseFileName() + ".zip"), (current, value, detail) -> current.withCdr(value, detail)); }
            catch (Exception ex) { LOG.error("Error recuperando CDR para ID={}", document.id(), ex); row = row.withCdr(FileStatus.ERROR, message(ex)); }
        }
        if (options.xmlData()) {
            try { row = write(row, repository.getXmlData(document.id()), output.resolve("R-" + document.baseFileName() + ".xml"), (current, value, detail) -> current.withXml(value, detail)); }
            catch (Exception ex) { LOG.error("Error recuperando XML data para ID={}", document.id(), ex); row = row.withXml(FileStatus.ERROR, message(ex)); }
        }
        return row;
    }

    private GenerationRow write(GenerationRow row, byte[] content, Path target, RowUpdater updater) throws IOException {
        if (content == null || content.length == 0) return updater.update(row, FileStatus.NOT_FOUND, "Sin datos para " + target.getFileName());
        Files.createDirectories(target.getParent());
        Path temporary = Files.createTempFile(target.getParent(), "." + target.getFileName(), ".part");
        try {
            Files.write(temporary, content);
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temporary); }
        return updater.update(row, FileStatus.GENERATED, "");
    }

    private String message(Exception exception) {
        Throwable cause = exception;
        while (cause.getCause() != null) cause = cause.getCause();
        String value = cause.getMessage();
        return value == null || value.isBlank() ? cause.getClass().getSimpleName() : cause.getClass().getSimpleName() + ": " + value;
    }

    @FunctionalInterface private interface RowUpdater { GenerationRow update(GenerationRow row, FileStatus status, String detail); }
}
