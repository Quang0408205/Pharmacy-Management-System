package com.pharmacy.demo.config;

import java.time.Clock;
import java.time.ZoneId;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

@Configuration
public class MongoConfig {

    @Bean
    MongoCustomConversions mongoCustomConversions() {
        return new MongoCustomConversions(List.of(
                MongoConverters.CodedEnumWriter.INSTANCE,
                MongoConverters.CodedEnumReaderFactory.INSTANCE,
                MongoConverters.LocalDateWriter.INSTANCE,
                MongoConverters.LocalDateReader.INSTANCE));
    }

    @Bean
    MongoTransactionManager transactionManager(MongoDatabaseFactory databaseFactory) {
        return new MongoTransactionManager(databaseFactory);
    }

    @Bean
    Clock clock(AppProperties properties) {
        return Clock.system(ZoneId.of(properties.timezone()));
    }
}
