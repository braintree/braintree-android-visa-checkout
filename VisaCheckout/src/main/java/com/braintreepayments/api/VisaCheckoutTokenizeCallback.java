package com.braintreepayments.api;

import com.braintreepayments.api.models.PaymentMethodNonce;

public interface VisaCheckoutTokenizeCallback {
    void onResult(PaymentMethodNonce paymentMethodNonce, Exception error);
}
