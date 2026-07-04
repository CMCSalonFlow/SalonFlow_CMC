package com.example.salonflow.search.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BranchSearchResponse {

    private List<BranchSearchItem> items;

    /**
     * Search After
     */
    private String nextCursor;

    private Long total;

}