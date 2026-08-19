package com.example.salonflow.search.initializer;

import com.example.salonflow.search.document.BranchSearchDocument;
import com.example.salonflow.search.service.BranchSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BranchSearchInitializer {

    private final ElasticsearchOperations elasticsearchOperations;
    private final BranchSearchService branchSearchService;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeIndex() {

        try {

            log.info("=== initializeIndex() called ===");

            IndexOperations indexOperations =
                    elasticsearchOperations.indexOps(BranchSearchDocument.class);

            boolean exists = indexOperations.exists();
            log.info("Index exists: {}", exists);

            if (!exists) {

                boolean created = indexOperations.create();
                log.info("Index created: {}", created);

                boolean mapped = indexOperations.putMapping();
                log.info("Mapping created: {}", mapped);

            } else {

                log.info("Index already exists.");

            }

            log.info("Reindexing all branches to populate new search fields in Elasticsearch...");
            branchSearchService.reindexAll();
            log.info("Reindexing completed successfully.");

        } catch (Exception e) {

            log.error("Failed to initialize Elasticsearch index", e);

        }
    }
}