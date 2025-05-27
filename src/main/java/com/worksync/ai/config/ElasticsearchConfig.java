package com.worksync.ai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchCustomConversions;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.time.Duration;

@Configuration
@EnableElasticsearchRepositories(basePackages = "com.worksync.ai.repository")
public class ElasticsearchConfig extends ElasticsearchConfiguration {

    @Value("${spring.elasticsearch.uris}")
    private String[] elasticsearchUris;

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
            .connectedTo(elasticsearchUris)
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
        return new ElasticsearchCustomConversions(Collections.emptyList());
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