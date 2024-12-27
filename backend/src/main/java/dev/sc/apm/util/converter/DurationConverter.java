package dev.sc.apm.util.converter;

import jakarta.persistence.AttributeConverter;

import java.time.Duration;

public class DurationConverter implements AttributeConverter<Duration, String> {
    @Override
    public String convertToDatabaseColumn(Duration duration) {
        return duration == null ? null : duration.toString(); // ISO-8601
    }

    @Override
    public Duration convertToEntityAttribute(String dbData) {
        return dbData == null ? null : Duration.parse(dbData);
    }
}
