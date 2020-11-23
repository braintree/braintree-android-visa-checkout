package com.braintreepayments.api;

import com.braintreepayments.api.models.PaymentMethodNonce;

public interface VisaCheckoutActivityResultListener {
    void onResult(Exception error, PaymentMethodNonce paymentMethodNonce);
}
