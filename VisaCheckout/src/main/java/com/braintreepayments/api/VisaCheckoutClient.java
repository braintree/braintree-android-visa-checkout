package com.braintreepayments.api;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.Nullable;

import com.visa.checkout.Environment;
import com.visa.checkout.Profile;
import com.visa.checkout.VisaPaymentSummary;

import java.util.List;

public class VisaCheckoutClient {

    private BraintreeClient braintreeClient;
    private TokenizationClient tokenizationClient;

    VisaCheckoutClient(BraintreeClient braintreeClient, TokenizationClient tokenizationClient) {
        this.braintreeClient = braintreeClient;
        this.tokenizationClient = tokenizationClient;
    }

    public void createProfileBuilder(Context context, final VisaCheckoutCreateProfileBuilderCallback listener) {
        braintreeClient.getConfiguration(new ConfigurationCallback() {
            @Override
            public void onResult(@Nullable Configuration configuration, @Nullable Exception e) {
                VisaCheckoutConfiguration visaCheckoutConfiguration = configuration.getVisaCheckout();
                boolean enabledAndSdkAvailable = isVisaCheckoutSDKAvailable() && configuration
                        .getVisaCheckout().isEnabled();

                if (!enabledAndSdkAvailable) {
                    listener.onResult(null, new ConfigurationException("Visa Checkout is not enabled."));
                    return;
                }

                String merchantApiKey = visaCheckoutConfiguration.getApiKey();
                List<String> acceptedCardBrands = visaCheckoutConfiguration.getAcceptedCardBrands();
                String environment = Environment.SANDBOX;

                if ("production".equals(configuration.getEnvironment())) {
                    environment = Environment.PRODUCTION;
                }

                Profile.ProfileBuilder profileBuilder = new Profile.ProfileBuilder(merchantApiKey, environment);
                profileBuilder.setCardBrands(acceptedCardBrands.toArray(new String[acceptedCardBrands.size()]));
                profileBuilder.setDataLevel(Profile.DataLevel.FULL);
                profileBuilder.setExternalClientId(visaCheckoutConfiguration.getExternalClientId());

                listener.onResult(profileBuilder, null);
            }
        });
    }

    public void tokenize(final Context context, VisaPaymentSummary visaPaymentSummary, final VisaCheckoutTokenizeCallback listener) {
        tokenizationClient.tokenize(new VisaCheckoutBuilder(visaPaymentSummary), new PaymentMethodNonceCallback() {
            @Override
            public void success(PaymentMethodNonce paymentMethodNonce) {
                listener.onResult(paymentMethodNonce, null);
                braintreeClient.sendAnalyticsEvent("visacheckout.tokenize.succeeded");
            }

            @Override
            public void failure(Exception e) {
                listener.onResult(null, e);
                braintreeClient.sendAnalyticsEvent("visacheckout.tokenize.failed");
            }
        });
    }

    void onActivityResult(Context context, int resultCode, Intent data, VisaCheckoutOnActivityResultCallback listener) {

    }

    static boolean isVisaCheckoutSDKAvailable() {
        try {
            Class.forName("com.visa.checkout.VisaCheckoutSdk");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
