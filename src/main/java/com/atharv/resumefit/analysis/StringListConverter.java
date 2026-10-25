package com.atharv.resumefit.analysis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.List;

/** Stores a List of strings as a JSON array in a TEXT column. */
@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> TYPE = new TypeReference<>() {
    };

    @Override
    public String convertToDatabaseColumn(List<String> list) {
        try {
            return MAPPER.writeValueAsString(list == null ? List.of() : list);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not save list", e);
        }
    }

    @Override
    public List<String> convertToEntityAttribute(String json) {
        try {
            return json == null || json.isBlank() ? List.of() : MAPPER.readValue(json, TYPE);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not read list", e);
        }
    }
}
