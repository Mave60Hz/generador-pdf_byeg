package com.bizlinks.helpdesk.service;

import com.bizlinks.helpdesk.model.DocumentRecord;

@FunctionalInterface
public interface PdfProvider {
    byte[] getPdf(DocumentRecord document) throws Exception;
}
