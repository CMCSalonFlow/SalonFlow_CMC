package com.example.salonflow.search.dto;

import java.util.List;

public class BranchSearchResponse {

    private List<BranchSearchItem> items;
    private String nextCursor;
    private Long total;

    public List<BranchSearchItem> getItems() {
        return items;
    }

    public void setItems(List<BranchSearchItem> items) {
        this.items = items;
    }

    public String getNextCursor() {
        return nextCursor;
    }

    public void setNextCursor(String nextCursor) {
        this.nextCursor = nextCursor;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }
}
