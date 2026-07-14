package com.example.salonflow.search.service;

public interface BranchSearchService {

    void indexBranch(Long branchId);

    void deleteBranch(Long branchId);

    void reindexAll();

}