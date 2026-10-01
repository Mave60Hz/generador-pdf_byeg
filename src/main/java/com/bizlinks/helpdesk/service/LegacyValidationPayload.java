package com.bizlinks.helpdesk.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/** Adapter for the existing Bizlinks XML-to-Fact contract. It expects the private integrador-utils jar at runtime. */
final class LegacyValidationPayload {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private LegacyValidationPayload() { }

    static JsonNode toFact(String xml) throws Exception {
        try {
            Class<?> commandFactoryType = Class.forName("com.bizlinks.integrador.marshaller.impl.CommandFactoryImpl");
            Object commandFactory = commandFactoryType.getConstructor().newInstance();
            Object action = commandFactoryType.getMethod("unMarshal", String.class).invoke(commandFactory, xml);
            Object documents = action.getClass().getMethod("getCommonDocuments").invoke(action);
            Object clientDocument = ((java.util.List<?>) documents).get(0);
            String documentType = clientDocument.getClass().getMethod("getTipoDocumento").invoke(clientDocument).toString();
            Object document = mapDocument(clientDocument, documentType);
            ObjectNode fact = MAPPER.createObjectNode();
            fact.set("document", MAPPER.valueToTree(document));
            JsonNode issuer = fact.path("document").path("emisor");
            ObjectNode companyId = MAPPER.createObjectNode();
            companyId.put("tipoDocumento", issuer.path("tipoDocumento").asText());
            companyId.put("numeroDocumento", issuer.path("numeroDocumento").asText());
            ObjectNode company = MAPPER.createObjectNode();
            company.set("id", companyId);
            ObjectNode properties = MAPPER.createObjectNode();
            properties.put("tiempoValido", 86400000);
            properties.put("inDiasExcepcion", "1");
            company.set("properties", properties);
            fact.set("issuer", company);
            return fact;
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException("La validación requiere integrador-utils en el classpath de ejecución; instala el artefacto privado Bizlinks y vuelve a iniciar la aplicación.", ex);
        }
    }

    private static Object mapDocument(Object clientDocument, String sunatType) throws Exception {
        String target;
        switch (sunatType) {
            case "01": target = "com.bizlinks.integrador.data.beans.v2_1.Invoice"; break;
            case "03": target = "com.bizlinks.integrador.data.beans.v2_1.Voucher"; break;
            case "07": target = "com.bizlinks.integrador.data.beans.v2_1.CreditNote"; break;
            case "08": target = "com.bizlinks.integrador.data.beans.v2_1.DebitNote"; break;
            case "RC": target = "com.bizlinks.integrador.data.beans.v2_1.RC"; break;
            case "RA": target = "com.bizlinks.integrador.data.beans.v2_1.RA"; break;
            case "RR": target = "com.bizlinks.integrador.data.beans.v2_1.RR"; break;
            default: throw new IllegalArgumentException("Tipo de documento no soportado: " + sunatType);
        }
        Class<?> mapperType = Class.forName("com.bizlinks.integrador.mapper.impl.BeanMapperImpl");
        Object mapper = mapperType.getConstructor().newInstance();
        for (var method : mapperType.getMethods()) {
            if (method.getName().equals("mapDocument") && method.getParameterCount() == 2
                    && method.getParameterTypes()[1] == Class.class
                    && method.getParameterTypes()[0].isAssignableFrom(clientDocument.getClass())) {
                return method.invoke(mapper, clientDocument, Class.forName(target));
            }
        }
        throw new NoSuchMethodException("BeanMapperImpl.mapDocument(document, Class<T>)");
    }
}
