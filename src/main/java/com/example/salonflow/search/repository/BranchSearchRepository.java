package com.example.salonflow.search.repository;

import com.example.salonflow.search.document.BranchSearchDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface BranchSearchRepository
        extends ElasticsearchRepository<BranchSearchDocument, Long> {

}