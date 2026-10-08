package com.cmpe172.game_coaching_booking.dto;

import java.util.List;

public record PageResult<T>(List<T> items, int page, int size, long totalItems) {

    public int totalPages() {
        return totalItems == 0 ? 1 : (int) Math.ceil((double) totalItems / size);
    }

    public int getTotalPages() {
        return totalPages();
    }
}
