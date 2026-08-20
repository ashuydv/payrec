package com.payrecon.controller;

import com.payrecon.dto.CreateMerchantRequest;
import com.payrecon.dto.MerchantResponse;
import com.payrecon.dto.PageResponse;
import com.payrecon.dto.UpdateMerchantRequest;
import com.payrecon.service.MerchantService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/merchants")
public class MerchantController {

    private final MerchantService merchantService;

    public MerchantController(MerchantService merchantService) {
        this.merchantService = merchantService;
    }

    @PostMapping
    public ResponseEntity<MerchantResponse> createMerchant(@Valid @RequestBody CreateMerchantRequest request) {
        MerchantResponse response = merchantService.createMerchant(request);
        return ResponseEntity.created(URI.create("/api/merchants/" + response.id())).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MerchantResponse> getMerchant(@PathVariable Long id) {
        return ResponseEntity.ok(merchantService.getMerchant(id));
    }

    @GetMapping
    public ResponseEntity<PageResponse<MerchantResponse>> listMerchants(
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(merchantService.listMerchants(pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MerchantResponse> updateMerchant(
            @PathVariable Long id, @Valid @RequestBody UpdateMerchantRequest request) {
        return ResponseEntity.ok(merchantService.updateMerchant(id, request));
    }
}
