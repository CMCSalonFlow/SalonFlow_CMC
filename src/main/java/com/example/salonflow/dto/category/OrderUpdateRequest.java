package com.example.salonflow.dto.category;

import lombok.Data;
import java.util.List;

@Data
public class OrderUpdateRequest {
    private List<Long> order;
}