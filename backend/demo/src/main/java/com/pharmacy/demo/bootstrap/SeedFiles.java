package com.pharmacy.demo.bootstrap;

import com.pharmacy.demo.config.AppProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class SeedFiles {

    private final AppProperties properties;
    private final ObjectMapper objectMapper;

    public List<JsonNode> read(String fileName) {
        Path path = Path.of(properties.seed().dataDirectory())
                .resolve(fileName)
                .toAbsolutePath()
                .normalize();
        if (!Files.isRegularFile(path)) {
            throw new IllegalStateException("Seed file not found: " + path);
        }
        JsonNode root = objectMapper.readTree(path.toFile());
        List<JsonNode> items = new ArrayList<>();
        root.forEach(items::add);
        return items;
    }

    public static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.stringValue();
    }

    public static Integer integer(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.intValue();
    }

    public static Long number(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.longValue();
    }
}
