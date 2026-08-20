package com.payrecon.service;

import com.payrecon.dto.CreateMerchantRequest;
import com.payrecon.dto.MerchantResponse;
import com.payrecon.dto.PageResponse;
import com.payrecon.dto.UpdateMerchantRequest;
import org.springframework.data.domain.Pageable;

public interface MerchantService {

    MerchantResponse createMerchant(CreateMerchantRequest request);

    MerchantResponse getMerchant(Long id);

    PageResponse<MerchantResponse> listMerchants(Pageable pageable);

    MerchantResponse updateMerchant(Long id, UpdateMerchantRequest request);
}
