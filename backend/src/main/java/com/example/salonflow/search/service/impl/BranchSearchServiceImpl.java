package com.example.salonflow.search.service.impl;

import com.example.salonflow.entity.Branch;
import com.example.salonflow.entity.SalonService;
import com.example.salonflow.repository.BranchRepository;
import com.example.salonflow.repository.ServiceRepository;
import com.example.salonflow.search.document.BranchSearchDocument;
import com.example.salonflow.search.mapper.BranchSearchMapper;
import com.example.salonflow.search.repository.BranchSearchRepository;
import com.example.salonflow.search.service.BranchSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BranchSearchServiceImpl implements BranchSearchService {

    private final BranchRepository branchRepository;

    private final ServiceRepository serviceRepository;

    private final BranchSearchRepository searchRepository;

    private final BranchSearchMapper mapper;

    @Override
    public void indexBranch(Long branchId) {

        Branch branch = branchRepository.findById(branchId)
                .orElseThrow();

        List<SalonService> services = serviceRepository.findByBranchIdAndIsActiveTrue(branchId);

        BranchSearchDocument document = mapper.toDocument(branch, services);

        searchRepository.save(document);
    }

    @Override
    public void deleteBranch(Long branchId) {

        searchRepository.deleteById(branchId);

    }

    @Override
    public void reindexAll() {

        searchRepository.deleteAll();

        List<Branch> branches = branchRepository.findAll();

        for (Branch branch : branches) {

            indexBranch(branch.getId());

        }

    }

}