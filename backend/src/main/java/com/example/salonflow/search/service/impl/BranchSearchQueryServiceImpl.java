package com.example.salonflow.search.service.impl;

import com.example.salonflow.search.document.BranchSearchDocument;
import com.example.salonflow.search.dto.BranchSearchItem;
import com.example.salonflow.search.dto.BranchSearchRequest;
import com.example.salonflow.search.dto.BranchSearchResponse;
import com.example.salonflow.search.service.BranchSearchQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders.*;

@Service
@RequiredArgsConstructor
public class BranchSearchQueryServiceImpl
        implements BranchSearchQueryService {

    private final ElasticsearchOperations elasticsearchOperations;

    @Override
    public BranchSearchResponse search(BranchSearchRequest request) {

        List<co.elastic.clients.elasticsearch._types.query_dsl.Query> filters =
                new ArrayList<>();

        /*
         * active = true
         */
        filters.add(
                term(t -> t
                        .field("active")
                        .value(true))
                        ._toQuery()
        );

        /*
         * keyword
         */
        if (request.getQ() != null && !request.getQ().isBlank()) {

            filters.add(

                    multiMatch(mm -> mm
                            .query(request.getQ())
                            .fields(
                                    "salonName",
                                    "branchName",
                                    "address",
                                    "services"
                            ))
                            ._toQuery()
            );

        }

        /*
         * service
         */
        if (request.getServiceId() != null) {

            filters.add(
                    term(t -> t
                            .field("serviceIds")
                            .value(request.getServiceId()))
                            ._toQuery()
            );

        }

        /*
         * price >=
         */
        if (request.getPriceMin() != null) {

            filters.add(
                    range(r -> r
                            .number(n -> n
                                    .field("minPrice")
                                    .gte(request.getPriceMin().doubleValue())))
                            ._toQuery()
            );

        }

        /*
         * price <=
         */
        if (request.getPriceMax() != null) {

            filters.add(
                    range(r -> r
                            .number(n -> n
                                    .field("maxPrice")
                                    .lte(request.getPriceMax().doubleValue())))
                            ._toQuery()
            );

        }

        NativeQuery query = NativeQuery.builder()

                .withQuery(

                        bool(b -> b.filter(filters))

                )

                .withPageable(

                        PageRequest.of(
                                0,
                                request.getSize()
                        )

                )

                .build();

        SearchHits<BranchSearchDocument> hits =
                elasticsearchOperations.search(
                        query,
                        BranchSearchDocument.class
                );

        List<BranchSearchItem> items =
                hits.getSearchHits()
                        .stream()
                        .map(this::toItem)
                        .toList();

        return BranchSearchResponse.builder()
                .items(items)
                .total(hits.getTotalHits())
                .nextCursor(null)
                .build();

    }

    private BranchSearchItem toItem(
            SearchHit<BranchSearchDocument> hit
    ) {

        BranchSearchDocument doc = hit.getContent();

        return BranchSearchItem.builder()
                .branchId(doc.getBranchId())
                .salonId(doc.getSalonId())
                .salonName(doc.getSalonName())
                .branchName(doc.getBranchName())
                .address(doc.getAddress())
                .latitude(doc.getLatitude())
                .longitude(doc.getLongitude())
                .minPrice(doc.getMinPrice())
                .maxPrice(doc.getMaxPrice())
                .rating(doc.getAverageRating())
                .distance(null)
                .build();

    }

}