package com.smartflow.smestocksensebackend.dto.response;

import com.smartflow.smestocksensebackend.entity.Partner;

public record PartnerDropdownResponse(
        Long id,
        String tenDoiTac,
        String loaiDoiTac
) {

    public static PartnerDropdownResponse from(Partner partner) {
        return new PartnerDropdownResponse(
                partner.getId(),
                partner.getName(),
                partner.getType().name()
        );
    }
}
