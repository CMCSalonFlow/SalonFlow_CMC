package com.example.salonflow.search.initializer;

import com.example.salonflow.search.document.BranchSearchDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Component;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
@Slf4j
@Component
@RequiredArgsConstructor
public class BranchSearchInitializer {

    private final ElasticsearchOperations elasticsearchOperations;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeIndex() {

        IndexOperations indexOperations =
                elasticsearchOperations.indexOps(BranchSearchDocument.class);

        if (!indexOperations.exists()) {

            indexOperations.create();

            indexOperations.putMapping();

            log.info("Created Elasticsearch index: branch_search");

        } else {

            log.info("Elasticsearch index already exists: branch_search");

        }
    }
}