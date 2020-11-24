package com.braintreepayments.api;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.Nullable;

import com.braintreepayments.api.exceptions.ConfigurationException;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCallback;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.models.VisaCheckoutBuilder;
import com.braintreepayments.api.models.VisaCheckoutConfiguration;
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

    public void createProfileBuilder(Context context, final VisaCheckoutCreateProfileListener listener) {
        braintreeClient.getConfiguration(context, new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(@Nullable Exception e, @Nullable Configuration configuration) {
                VisaCheckoutConfiguration visaCheckoutConfiguration = configuration.getVisaCheckout();
                boolean enabledAndSdkAvailable = isVisaCheckoutSDKAvailable() && configuration
                        .getVisaCheckout().isEnabled();

                if (!enabledAndSdkAvailable) {
                    listener.onResult(new ConfigurationException("Visa Checkout is not enabled."), null);
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

                listener.onResult(null, profileBuilder);
            }
        });
    }

    public void tokenize(final Context context, VisaPaymentSummary visaPaymentSummary, final VisaCheckoutTokenizeListener listener) {
        tokenizationClient.tokenize(context, new VisaCheckoutBuilder(visaPaymentSummary), new PaymentMethodNonceCallback() {
            @Override
            public void success(PaymentMethodNonce paymentMethodNonce) {
                listener.onResult(null, paymentMethodNonce);
                braintreeClient.sendAnalyticsEvent(context, "visacheckout.tokenize.succeeded");
            }

            @Override
            public void failure(Exception e) {
                listener.onResult(e, null);
                braintreeClient.sendAnalyticsEvent(context, "visacheckout.tokenize.failed");
            }
        });
    }

    void onActivityResult(Context context, int resultCode, Intent data, VisaCheckoutActivityResultListener listener) {

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
