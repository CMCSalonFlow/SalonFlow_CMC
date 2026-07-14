package com.example.salonflow.search.service;

import com.example.salonflow.search.dto.BranchSearchRequest;
import com.example.salonflow.search.dto.BranchSearchResponse;

public interface BranchSearchQueryService {

    BranchSearchResponse search(
            BranchSearchRequest request
    );

}
