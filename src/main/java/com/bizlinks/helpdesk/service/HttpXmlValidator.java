package com.bizlinks.helpdesk.service;

import com.bizlinks.helpdesk.config.AppConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HttpXmlValidator implements XmlValidator {
    private static final Logger LOG = LoggerFactory.getLogger(HttpXmlValidator.class);
    private final AppConfig config;
    private final HttpClient client;
    private final ObjectMapper mapper = new ObjectMapper();

    public HttpXmlValidator(AppConfig config) {
        this.config = config;
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(config.connectTimeoutMillis())).build();
    }

    @Override public List<ValidationIssue> validate(String xml) throws Exception {
        JsonNode document = LegacyValidationPayload.toFact(xml);
        HttpRequest request = HttpRequest.newBuilder(URI.create(config.validationUrl()))
                .timeout(Duration.ofMillis(config.readTimeoutMillis()))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(document)))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        LOG.info("Servicio de validación respondió HTTP {}", response.statusCode());
        if (response.statusCode() < 200 || response.statusCode() >= 300) throw new IllegalStateException("Validador devolvió HTTP " + response.statusCode() + ": " + response.body());
        JsonNode results = mapper.readTree(response.body());
        List<ValidationIssue> issues = new ArrayList<>();
        if (!results.isArray()) throw new IllegalStateException("Respuesta inesperada del validador: se esperaba una lista JSON.");
        for (JsonNode result : results) issues.add(new ValidationIssue(result.path("code").asText(""), result.path("description").asText("")));
        return List.copyOf(issues);
    }
}
