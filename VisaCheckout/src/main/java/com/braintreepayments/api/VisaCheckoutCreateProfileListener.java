package com.braintreepayments.api;

import com.visa.checkout.Profile;

public interface VisaCheckoutCreateProfileListener {
    void onResult(Exception error, Profile.ProfileBuilder profileBuilder);
}
