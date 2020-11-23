package com.braintreepayments.api;

import com.braintreepayments.api.models.PaymentMethodNonce;

public interface VisaCheckoutTokenizeListener {
    void onResult(Exception error, PaymentMethodNonce paymentMethodNonce);
}
