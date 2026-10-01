package com.bizlinks.helpdesk.model;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class DocumentInputReader {
    private DocumentInputReader() { }

    public static List<DocumentRecord> read(Path path) throws IOException {
        List<DocumentRecord> documents = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index).trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] fields = line.split(",", -1);
            try {
                if (fields.length != 4) throw new IllegalArgumentException("se esperaban 4 campos separados por coma");
                String type = fields[2].trim();
                if (type.matches("\\d") ) type = "0" + type;
                documents.add(new DocumentRecord(Long.parseLong(fields[0].trim()), fields[1].trim(), type, fields[3].trim()));
            } catch (RuntimeException ex) {
                errors.add("Línea " + (index + 1) + ": " + ex.getMessage());
            }
        }
        if (!errors.isEmpty()) throw new IllegalArgumentException(String.join("\n", errors));
        if (documents.isEmpty()) throw new IllegalArgumentException("El archivo no contiene documentos.");
        return List.copyOf(documents);
    }
}
