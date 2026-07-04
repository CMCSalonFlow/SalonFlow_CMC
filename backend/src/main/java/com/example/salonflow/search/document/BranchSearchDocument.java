package com.example.salonflow.search.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "branch_search")
public class BranchSearchDocument {

    /**
     * Elasticsearch _id
     */
    @Id
    private Long branchId;

    private Long salonId;

    @MultiField(
            mainField = @Field(type = FieldType.Text),
            otherFields = {
                    @InnerField(
                            suffix = "keyword",
                            type = FieldType.Keyword
                    )
            }
    )
    private String salonName;

    @MultiField(
            mainField = @Field(type = FieldType.Text),
            otherFields = {
                    @InnerField(
                            suffix = "keyword",
                            type = FieldType.Keyword
                    )
            }
    )
    private String branchName;

    @Field(type = FieldType.Text)
    private String address;

    /**
     * Chưa dùng geo_point ở giai đoạn này
     */
    @Field(type = FieldType.Double)
    private Double latitude;

    @Field(type = FieldType.Double)
    private Double longitude;

    /**
     * Filter theo serviceId
     */
    @Field(type = FieldType.Long)
    private List<Long> serviceIds;

    /**
     * Full text search
     */
    @Field(type = FieldType.Keyword)
    private List<String> services;

    @Field(type = FieldType.Double)
    private BigDecimal minPrice;

    @Field(type = FieldType.Double)
    private BigDecimal maxPrice;

    /**
     * Chưa có Review
     */
    @Field(type = FieldType.Double)
    @Builder.Default
    private Double averageRating = 0d;

    @Field(type = FieldType.Boolean)
    private Boolean active;
}