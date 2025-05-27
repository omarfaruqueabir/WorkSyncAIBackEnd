package com.worksync.ai.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;

@Configuration
public class ElasticsearchMappingConfig {

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @PostConstruct
    public void setupMapping() {
        String mapping = """
            {
              "properties": {
                "timestamp": {
                  "type": "date",
                  "format": "strict_date_optional_time||epoch_millis||uuuu-MM-dd'T'HH:mm:ss.SSSSSS"
                }
              }
            }
            """;

        Document mappingDocument = Document.parse(mapping);
        String indexName = "worksync-events";

        // Create or update the index with proper mapping
        if (!elasticsearchOperations.indexOps(IndexCoordinates.of(indexName)).exists()) {
            elasticsearchOperations.indexOps(IndexCoordinates.of(indexName))
                .create();
        }
        
        elasticsearchOperations.indexOps(IndexCoordinates.of(indexName))
            .putMapping(mappingDocument);
    }
} 