package com.bizlinks.helpdesk.service;

import java.util.List;

@FunctionalInterface
public interface XmlValidator {
    List<ValidationIssue> validate(String xml) throws Exception;

    record ValidationIssue(String code, String description) { }
}
