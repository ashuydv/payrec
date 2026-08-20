package com.payrecon.processing;

import com.payrecon.domain.PaymentType;
import com.payrecon.processing.strategy.BankTransferPaymentProcessor;
import com.payrecon.processing.strategy.CardPaymentProcessor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentProcessorFactoryTest {

    @Test
    void getProcessor_returnsCardProcessor_forCardType() {
        PaymentProcessorFactory factory = new PaymentProcessorFactory(
                List.of(new CardPaymentProcessor(), new BankTransferPaymentProcessor()));

        assertThat(factory.getProcessor(PaymentType.CARD)).isInstanceOf(CardPaymentProcessor.class);
    }

    @Test
    void getProcessor_returnsBankTransferProcessor_forBankTransferType() {
        PaymentProcessorFactory factory = new PaymentProcessorFactory(
                List.of(new CardPaymentProcessor(), new BankTransferPaymentProcessor()));

        assertThat(factory.getProcessor(PaymentType.BANK_TRANSFER)).isInstanceOf(BankTransferPaymentProcessor.class);
    }

    @Test
    void getProcessor_throwsIllegalArgument_whenNoProcessorRegisteredForType() {
        PaymentProcessorFactory factory = new PaymentProcessorFactory(List.of(new CardPaymentProcessor()));

        assertThatThrownBy(() -> factory.getProcessor(PaymentType.BANK_TRANSFER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BANK_TRANSFER");
    }
}
