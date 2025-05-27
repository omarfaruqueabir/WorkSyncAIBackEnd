package com.worksync.ai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchCustomConversions;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.time.Duration;
import java.time.LocalDateTime;

@Configuration
//@EnableElasticsearchRepositories(basePackages = "com.worksync.ai.repository")
public class ElasticsearchConfig extends ElasticsearchConfiguration {

    @Value("${spring.elasticsearch.uris}")
    private String elasticsearchUrl;

    @Value("${spring.elasticsearch.connection-timeout:5s}")
    private String connectionTimeout;

    @Value("${spring.elasticsearch.socket-timeout:10s}")
    private String socketTimeout;

    @Value("${spring.elasticsearch.username:}")
    private String username;

    @Value("${spring.elasticsearch.password:}")
    private String password;

    @Override
    public ClientConfiguration clientConfiguration() {
        var builder = ClientConfiguration.builder()
            .connectedTo(elasticsearchUrl.replace("http://", ""))
            .withConnectTimeout(Duration.parse("PT" + connectionTimeout))
            .withSocketTimeout(Duration.parse("PT" + socketTimeout));

        // Add authentication if credentials are provided
        if (username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
            return builder.withBasicAuth(username, password).build();
        }

        return builder.build();
    }

    @Bean
    public ElasticsearchCustomConversions elasticsearchCustomConversions() {
        return new ElasticsearchCustomConversions(
            Arrays.asList(new LocalDateTimeToStringConverter(), new StringToLocalDateTimeConverter())
        );
    }

    @WritingConverter
    static class LocalDateTimeToStringConverter implements Converter<LocalDateTime, String> {
        @Override
        public String convert(LocalDateTime source) {
            return source.format(DateTimeFormatter.ISO_DATE_TIME);
        }
    }

    @ReadingConverter
    static class StringToLocalDateTimeConverter implements Converter<String, LocalDateTime> {
        @Override
        public LocalDateTime convert(String source) {
            return LocalDateTime.parse(source, DateTimeFormatter.ISO_DATE_TIME);
        }
    }

    @Bean
    public IndexNameProvider indexNameProvider() {
        return new IndexNameProvider();
    }

    public static class IndexNameProvider {
        private static final String INDEX_PREFIX = "log-";
        private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        public String getIndexName() {
            return INDEX_PREFIX + LocalDate.now().format(DATE_FORMATTER);
        }
    }
} 