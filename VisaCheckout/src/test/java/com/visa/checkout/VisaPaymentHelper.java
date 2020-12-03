package com.visa.checkout;

import android.os.Parcel;

import org.json.JSONObject;

public class VisaPaymentHelper {

    public static VisaPaymentSummary createPaymentSummary(Parcel in) {
        return new VisaPaymentSummary(in);
    }
}
