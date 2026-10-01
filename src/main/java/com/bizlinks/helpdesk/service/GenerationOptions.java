package com.bizlinks.helpdesk.service;

public record GenerationOptions(boolean pdf, boolean ubl, boolean cdr, boolean xmlData) {
    public boolean anySelected() { return pdf || ubl || cdr || xmlData; }
}
