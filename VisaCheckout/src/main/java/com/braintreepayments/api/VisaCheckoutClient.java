package com.braintreepayments.api;

import android.content.Context;
import android.content.Intent;

import com.visa.checkout.VisaPaymentSummary;

public class VisaCheckoutClient {

    VisaCheckoutClient(BraintreeClient braintreeClient) {

    }

    public void createProfileBuilder(Context context, VisaCheckoutCreateProfileListener listener) {

    }

    public void tokenize(Context context, VisaPaymentSummary visaPaymentSummary, VisaCheckoutTokenizeListener listener) {

    }

    void onActivityResult(Context context, int resultCode, Intent data, VisaCheckoutActivityResultListener listener) {

    }
}
