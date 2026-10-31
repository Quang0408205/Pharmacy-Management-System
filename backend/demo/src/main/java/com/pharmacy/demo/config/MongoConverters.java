package com.pharmacy.demo.config;

import com.pharmacy.demo.enums.CodedEnum;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;

public final class MongoConverters {

    private MongoConverters() {}

    @WritingConverter
    public enum CodedEnumWriter implements Converter<CodedEnum, String> {
        INSTANCE;

        @Override
        public String convert(CodedEnum source) {
            return source.getDbCode();
        }
    }

    @ReadingConverter
    public enum CodedEnumReaderFactory implements ConverterFactory<String, CodedEnum> {
        INSTANCE;

        @Override
        public <T extends CodedEnum> Converter<String, T> getConverter(Class<T> targetType) {
            return source -> {
                for (T constant : targetType.getEnumConstants()) {
                    if (constant.getDbCode().equals(source)) {
                        return constant;
                    }
                }
                throw new IllegalArgumentException("Unknown code " + source + " for " + targetType.getSimpleName());
            };
        }
    }

    @WritingConverter
    public enum LocalDateWriter implements Converter<LocalDate, Date> {
        INSTANCE;

        @Override
        public Date convert(LocalDate source) {
            return Date.from(source.atStartOfDay().toInstant(ZoneOffset.UTC));
        }
    }

    @ReadingConverter
    public enum LocalDateReader implements Converter<Date, LocalDate> {
        INSTANCE;

        @Override
        public LocalDate convert(Date source) {
            return source.toInstant().atOffset(ZoneOffset.UTC).toLocalDate();
        }
    }
}
