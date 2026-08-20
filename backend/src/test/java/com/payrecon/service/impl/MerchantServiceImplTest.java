package com.payrecon.service.impl;

import com.payrecon.domain.Merchant;
import com.payrecon.dto.CreateMerchantRequest;
import com.payrecon.dto.MerchantResponse;
import com.payrecon.dto.UpdateMerchantRequest;
import com.payrecon.exception.ResourceNotFoundException;
import com.payrecon.repository.MerchantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MerchantServiceImplTest {

    @Mock
    private MerchantRepository merchantRepository;

    private MerchantServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MerchantServiceImpl(merchantRepository);
    }

    @Test
    void createMerchant_savesAndReturnsResponse() {
        CreateMerchantRequest request = new CreateMerchantRequest("Acme Co", "ACC-001");
        Merchant saved = new Merchant(request.name(), request.settlementAccount());
        setId(saved, 1L);
        when(merchantRepository.save(org.mockito.ArgumentMatchers.any(Merchant.class))).thenReturn(saved);

        MerchantResponse response = service.createMerchant(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Acme Co");
        assertThat(response.settlementAccount()).isEqualTo("ACC-001");
    }

    @Test
    void getMerchant_throwsNotFound_whenMissing() {
        when(merchantRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMerchant(404L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getMerchant_returnsResponse_whenPresent() {
        Merchant merchant = new Merchant("Acme Co", "ACC-001");
        setId(merchant, 1L);
        when(merchantRepository.findById(1L)).thenReturn(Optional.of(merchant));

        MerchantResponse response = service.getMerchant(1L);

        assertThat(response.id()).isEqualTo(1L);
    }

    @Test
    void updateMerchant_updatesNameAndSettlementAccount() {
        Merchant merchant = new Merchant("Old Name", "ACC-OLD");
        setId(merchant, 1L);
        when(merchantRepository.findById(1L)).thenReturn(Optional.of(merchant));

        MerchantResponse response = service.updateMerchant(1L, new UpdateMerchantRequest("New Name", "ACC-NEW"));

        assertThat(response.name()).isEqualTo("New Name");
        assertThat(response.settlementAccount()).isEqualTo("ACC-NEW");
    }

    @Test
    void updateMerchant_throwsNotFound_whenMissing() {
        when(merchantRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateMerchant(404L, new UpdateMerchantRequest("x", "y")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private static void setId(Object entity, Long id) {
        try {
            Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
