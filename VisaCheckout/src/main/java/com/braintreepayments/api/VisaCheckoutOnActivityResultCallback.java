package com.braintreepayments.api;

import com.braintreepayments.api.models.PaymentMethodNonce;

public interface VisaCheckoutOnActivityResultCallback {
    void onResult(PaymentMethodNonce paymentMethodNonce, Exception error);
}
