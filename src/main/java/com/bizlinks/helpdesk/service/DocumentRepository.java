package com.bizlinks.helpdesk.service;

public interface DocumentRepository {
    byte[] getUbl(long id) throws Exception;
    byte[] getCdr(long id) throws Exception;
    byte[] getXmlData(long id) throws Exception;
}
