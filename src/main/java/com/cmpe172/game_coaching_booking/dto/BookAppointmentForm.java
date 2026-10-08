package com.cmpe172.game_coaching_booking.dto;

import jakarta.validation.constraints.Positive;

public class BookAppointmentForm {

    @Positive(message = "A valid slot is required")
    private int slotId;

    public int getSlotId() {
        return slotId;
    }

    public void setSlotId(int slotId) {
        this.slotId = slotId;
    }
}
