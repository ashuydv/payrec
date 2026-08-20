package com.payrecon.service.impl;

import com.payrecon.domain.Merchant;
import com.payrecon.dto.CreateMerchantRequest;
import com.payrecon.dto.MerchantResponse;
import com.payrecon.dto.PageResponse;
import com.payrecon.dto.UpdateMerchantRequest;
import com.payrecon.exception.ResourceNotFoundException;
import com.payrecon.repository.MerchantRepository;
import com.payrecon.service.MerchantService;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MerchantServiceImpl implements MerchantService {

    private final MerchantRepository merchantRepository;

    public MerchantServiceImpl(MerchantRepository merchantRepository) {
        this.merchantRepository = merchantRepository;
    }

    @Override
    @Transactional
    public MerchantResponse createMerchant(CreateMerchantRequest request) {
        Merchant merchant = new Merchant(request.name(), request.settlementAccount());
        return MerchantResponse.from(merchantRepository.save(merchant));
    }

    @Override
    @Transactional(readOnly = true)
    public MerchantResponse getMerchant(Long id) {
        return merchantRepository.findById(id)
                .map(MerchantResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant not found: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<MerchantResponse> listMerchants(Pageable pageable) {
        return PageResponse.from(merchantRepository.findAll(pageable).map(MerchantResponse::from));
    }

    @Override
    @Transactional
    public MerchantResponse updateMerchant(Long id, UpdateMerchantRequest request) {
        Merchant merchant = merchantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant not found: " + id));

        merchant.setName(request.name());
        merchant.setSettlementAccount(request.settlementAccount());
        return MerchantResponse.from(merchant);
    }
}
