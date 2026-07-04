package com.example.salonflow.search.service.impl;

import com.example.salonflow.exception.BadRequestException;
import com.example.salonflow.search.document.BranchSearchDocument;
import com.example.salonflow.search.dto.BranchSearchItem;
import com.example.salonflow.search.dto.BranchSearchRequest;
import com.example.salonflow.search.dto.BranchSearchResponse;
import com.example.salonflow.search.service.BranchSearchQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders.bool;
import static co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders.match;
import static co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders.matchAll;
import static co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders.multiMatch;
import static co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders.range;
import static co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders.term;

import co.elastic.clients.elasticsearch._types.DistanceUnit;
import co.elastic.clients.elasticsearch._types.GeoDistanceSort;
import co.elastic.clients.elasticsearch._types.GeoDistanceType;
import co.elastic.clients.elasticsearch._types.GeoLocation;
import co.elastic.clients.elasticsearch._types.LatLonGeoLocation;
import co.elastic.clients.elasticsearch._types.ScoreSort;
import co.elastic.clients.elasticsearch._types.SortMode;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;

@Service
@RequiredArgsConstructor
public class BranchSearchQueryServiceImpl implements BranchSearchQueryService {

    private static final String CURSOR_PREFIX = "branch-search:";

    private final ElasticsearchOperations elasticsearchOperations;

    @Override
    public BranchSearchResponse search(BranchSearchRequest request) {

        List<co.elastic.clients.elasticsearch._types.query_dsl.Query> filters = new ArrayList<>();

        filters.add(term(t -> t.field("active").value(true)));

        if (request.getService() != null && !request.getService().isBlank()) {
            filters.add(match(m -> m.field("services").query(request.getService())));
        }

        if (request.getServiceId() != null) {
            filters.add(term(t -> t.field("serviceIds").value(request.getServiceId())));
        }

        if (request.getPriceMin() != null) {
            filters.add(
                    range(r -> r.number(n -> n.field("minPrice").gte(request.getPriceMin().doubleValue())))
            );
        }

        if (request.getPriceMax() != null) {
            filters.add(
                    range(r -> r.number(n -> n.field("maxPrice").lte(request.getPriceMax().doubleValue())))
            );
        }

        if (request.getRatingMin() != null) {
            filters.add(
                    range(r -> r.number(n -> n.field("averageRating").gte(request.getRatingMin())))
            );
        }

        boolean hasQ = request.getQ() != null && !request.getQ().isBlank();
        boolean hasLocation = request.getLatitude() != null && request.getLongitude() != null;

        NativeQueryBuilder builder = NativeQuery.builder()
                .withQuery(
                        hasQ
                                ? bool(b -> b
                                        .must(multiMatch(mm -> mm
                                                .query(request.getQ())
                                                .fields(
                                                        "salonName^4",
                                                        "branchName^3",
                                                        "address^2",
                                                        "services^3"
                                                )))
                                        .filter(filters))
                                : bool(b -> b
                                        .must(matchAll(ma -> ma))
                                        .filter(filters))
                )
                .withSort(buildSorts(hasQ, hasLocation, request.getLatitude(), request.getLongitude()))
                .withTrackScores(true)
                .withPageable(PageRequest.of(0, request.getSize() + 1));

        if (request.getCursor() != null && !request.getCursor().isBlank()) {
            builder.withSearchAfter(decodeCursor(request.getCursor(), hasQ, hasLocation));
        }

        SearchHits<BranchSearchDocument> hits = elasticsearchOperations.search(builder.build(), BranchSearchDocument.class);

        List<SearchHit<BranchSearchDocument>> searchHits = hits.getSearchHits();
        boolean hasNext = searchHits.size() > request.getSize();
        if (hasNext) {
            searchHits = searchHits.subList(0, request.getSize());
        }

        List<BranchSearchItem> items = searchHits.stream()
                .map(hit -> toItem(hit, request.getLatitude(), request.getLongitude(), hasQ))
                .toList();

        String nextCursor = null;
        if (hasNext && !searchHits.isEmpty()) {
            nextCursor = encodeCursor(searchHits.get(searchHits.size() - 1), hasQ, hasLocation);
        }

        BranchSearchResponse response = new BranchSearchResponse();
        response.setItems(items);
        response.setTotal(hits.getTotalHits());
        response.setNextCursor(nextCursor);
        return response;
    }

    private BranchSearchItem toItem(
            SearchHit<BranchSearchDocument> hit,
            Double latitude,
            Double longitude,
            boolean hasQ
    ) {
        BranchSearchDocument doc = hit.getContent();

        Double distance = null;
        if (latitude != null && longitude != null) {
            int distanceIndex = hasQ ? 1 : 0;
            List<Object> sortValues = hit.getSortValues();
            if (sortValues.size() > distanceIndex) {
                distance = toDouble(sortValues.get(distanceIndex));
            }
        }

        BranchSearchItem item = new BranchSearchItem();
        item.setBranchId(doc.getBranchId());
        item.setSalonId(doc.getSalonId());
        item.setSalonName(doc.getSalonName());
        item.setBranchName(doc.getBranchName());
        item.setAddress(doc.getAddress());
        item.setLatitude(doc.getLatitude());
        item.setLongitude(doc.getLongitude());
        item.setMinPrice(doc.getMinPrice());
        item.setMaxPrice(doc.getMaxPrice());
        item.setRating(doc.getAverageRating());
        item.setDistance(distance);
        return item;
    }

    private List<SortOptions> buildSorts(
            boolean hasQ,
            boolean hasLocation,
            Double latitude,
            Double longitude
    ) {
        List<SortOptions> sorts = new ArrayList<>();

        if (hasQ) {
            sorts.add(
                    SortOptions.of(s -> s.score(
                            ScoreSort.of(score -> score.order(SortOrder.Desc)))
                    )
            );
        }

        if (hasLocation) {
            sorts.add(
                    SortOptions.of(s -> s.geoDistance(
                            GeoDistanceSort.of(geo -> geo
                                    .field("location")
                                    .location(
                                            GeoLocation.of(gl -> gl.latlon(
                                                    LatLonGeoLocation.of(ll -> ll
                                                            .lat(latitude)
                                                            .lon(longitude))
                                            ))
                                    )
                                    .unit(DistanceUnit.Kilometers)
                                    .mode(SortMode.Min)
                                    .distanceType(GeoDistanceType.Arc)
                                    .order(SortOrder.Asc)
                                    .ignoreUnmapped(true)))
                    )
            );
        }

        sorts.add(
                SortOptions.of(s -> s.field(f -> f
                        .field("branchId")
                        .order(SortOrder.Asc)))
        );

        return sorts;
    }

    private List<Object> decodeCursor(
            String cursor,
            boolean hasQ,
            boolean hasLocation
    ) {
        try {
            String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            CursorPayload payload = CursorPayload.fromEncoded(raw);

            List<Object> values = new ArrayList<>();
            if (hasQ) {
                values.add(payload.score());
            }
            if (hasLocation) {
                values.add(payload.distance());
            }
            values.add(payload.branchId());
            return values;
        } catch (Exception ex) {
            throw new BadRequestException("Invalid search cursor", ex);
        }
    }

    private String encodeCursor(
            SearchHit<BranchSearchDocument> hit,
            boolean hasQ,
            boolean hasLocation
    ) {
        List<Object> sortValues = hit.getSortValues();
        int index = 0;

        Double score = null;
        Double distance = null;

        if (hasQ) {
            score = toDouble(sortValues.get(index++));
        }
        if (hasLocation) {
            distance = toDouble(sortValues.get(index++));
        }

        Long branchId = toLong(sortValues.get(index));
        CursorPayload payload = new CursorPayload(score, distance, branchId);
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.toEncoded().getBytes(StandardCharsets.UTF_8));
    }

    private Double toDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return value == null ? null : Double.parseDouble(value.toString());
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return value == null ? null : Long.parseLong(value.toString());
    }

    private record CursorPayload(Double score, Double distance, Long branchId) {

        private static final String PREFIX = "branch-search:";

        static CursorPayload fromEncoded(String raw) {
            if (!raw.startsWith(PREFIX)) {
                throw new IllegalArgumentException("Invalid cursor prefix");
            }

            String[] parts = raw.substring(PREFIX.length()).split("\\|", -1);
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid cursor format");
            }

            return new CursorPayload(
                    parts[0].isEmpty() ? null : Double.valueOf(parts[0]),
                    parts[1].isEmpty() ? null : Double.valueOf(parts[1]),
                    parts[2].isEmpty() ? null : Long.valueOf(parts[2])
            );
        }

        String toEncoded() {
            return PREFIX
                    + (score == null ? "" : score)
                    + "|"
                    + (distance == null ? "" : distance)
                    + "|"
                    + (branchId == null ? "" : branchId);
        }
    }
}
