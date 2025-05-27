package com.worksync.ai.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.ai.autoconfigure.vectorstore.elasticsearch.ElasticsearchVectorStoreAutoConfiguration;

@Configuration
@EnableAutoConfiguration(exclude = {ElasticsearchVectorStoreAutoConfiguration.class})
public class VectorStoreConfig {
    // This class disables Spring AI's vector store auto-configuration
} 