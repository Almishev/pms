package com.hotel.pms.model.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class RestaurantChargesView {
    private BigDecimal restaurantCharge = BigDecimal.ZERO;
    private List<RestaurantChargeLineDto> lines = new ArrayList<>();
}
