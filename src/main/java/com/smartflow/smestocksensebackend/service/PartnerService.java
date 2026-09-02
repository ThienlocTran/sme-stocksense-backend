package com.smartflow.smestocksensebackend.service;

import com.smartflow.smestocksensebackend.dto.request.CreatePartnerRequest;
import com.smartflow.smestocksensebackend.dto.request.UpdatePartnerRequest;
import com.smartflow.smestocksensebackend.dto.response.PartnerDropdownResponse;
import com.smartflow.smestocksensebackend.dto.response.PartnerResponse;

import java.util.List;

public interface PartnerService {

    List<PartnerResponse> getPartners(String keyword, String loaiDoiTac, String trangThai);

    PartnerResponse createPartner(CreatePartnerRequest request);

    PartnerResponse updatePartner(Long id, UpdatePartnerRequest request);

    List<PartnerDropdownResponse> getActiveSuppliers();

    List<PartnerDropdownResponse> getActiveCustomers();
}
