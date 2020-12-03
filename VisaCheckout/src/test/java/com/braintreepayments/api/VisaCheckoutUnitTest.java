package com.braintreepayments.api;

import android.os.Parcel;

import com.braintreepayments.api.exceptions.ConfigurationException;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.VisaCheckoutNonce;
import com.braintreepayments.api.test.TestActivity;
import com.braintreepayments.api.test.TestConfigurationBuilder;
import com.visa.checkout.Profile;
import com.visa.checkout.Profile.CardBrand;
import com.visa.checkout.Profile.ProfileBuilder;
import com.visa.checkout.VisaPaymentSummary;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static com.braintreepayments.api.test.FixturesHelper.stringFromFixture;
import static com.visa.checkout.VisaPaymentHelper.createPaymentSummary;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
public class VisaCheckoutUnitTest {

    private Configuration mConfigurationWithVisaCheckout;
    private TestActivity mActivity;

    @Before
    public void setup() throws JSONException {

        JSONObject visaConfiguration = new JSONObject(stringFromFixture("configuration/with_visa_checkout.json"));
        mConfigurationWithVisaCheckout = Configuration.fromJson(visaConfiguration.toString());

        mActivity = Robolectric.setupActivity(TestActivity.class);
    }

    @Test
    public void createProfileBuilder_whenNotEnabled_throwsConfigurationException() {
        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder().build();

        Configuration configuration = TestConfigurationBuilder.basicConfig();
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .build();

        VisaCheckoutClient sut = new VisaCheckoutClient(braintreeClient, tokenizationClient);

        VisaCheckoutCreateProfileBuilderCallback listener = mock(VisaCheckoutCreateProfileBuilderCallback.class);
        sut.createProfileBuilder(null, listener);

        ArgumentCaptor<ConfigurationException> captor = ArgumentCaptor.forClass(ConfigurationException.class);
        verify(listener, times(1)).onResult(null, captor.capture());

        ConfigurationException configurationException = captor.getValue();
        assertEquals("Visa Checkout is not enabled.", configurationException.getMessage());
    }

    @Test
    public void createProfileBuilder_whenProduction_usesProductionConfig() throws Exception {
        final CountDownLatch lock = new CountDownLatch(1);

        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder().build();
        String configString = new TestConfigurationBuilder()
                .environment("production")
                .visaCheckout(new TestConfigurationBuilder.TestVisaCheckoutConfigurationBuilder()
                        .apikey("gwApiKey")
                        .supportedCardTypes(CardBrand.VISA, CardBrand.MASTERCARD)
                        .externalClientId("gwExternalClientId"))
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(Configuration.fromJson(configString))
                .build();
        VisaCheckoutClient sut = new VisaCheckoutClient(braintreeClient, tokenizationClient);

        sut.createProfileBuilder(mActivity, new VisaCheckoutCreateProfileBuilderCallback() {
            @Override
            public void onResult(ProfileBuilder profileBuilder, Exception error) {
                List<String> expectedCardBrands = Arrays.asList(CardBrand.VISA, CardBrand.MASTERCARD);
                Profile profile = profileBuilder.build();
                assertNotNull(profile);
                lock.countDown();
            }
        });

        lock.await();
    }

    @Test
    public void createProfileBuilder_whenNotProduction_usesSandboxConfig() throws Exception {
        final CountDownLatch lock = new CountDownLatch(1);

        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder().build();
        String configString = new TestConfigurationBuilder()
                .environment("environment")
                .visaCheckout(new TestConfigurationBuilder.TestVisaCheckoutConfigurationBuilder()
                        .apikey("gwApiKey")
                        .supportedCardTypes(CardBrand.VISA, CardBrand.MASTERCARD)
                        .externalClientId("gwExternalClientId"))
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(Configuration.fromJson(configString))
                .build();
        VisaCheckoutClient sut = new VisaCheckoutClient(braintreeClient, tokenizationClient);

        sut.createProfileBuilder(mActivity, new VisaCheckoutCreateProfileBuilderCallback() {
            @Override
            public void onResult(ProfileBuilder profileBuilder, Exception error) {
                List<String> expectedCardBrands = Arrays.asList(CardBrand.VISA, CardBrand.MASTERCARD);
                Profile profile = profileBuilder.build();
                assertNotNull(profile);
                lock.countDown();
            }
        });

        lock.await();
    }

    @Test
    public void tokenize_whenSuccessful_postsVisaPaymentMethodNonce() throws Exception {
        VisaCheckoutNonce visaCheckoutNonce =
            VisaCheckoutNonce.fromJson(stringFromFixture("payment_methods/visa_checkout_response.json"));

        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder()
                .successNonce(visaCheckoutNonce)
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(mConfigurationWithVisaCheckout)
                .build();
        VisaCheckoutClient sut = new VisaCheckoutClient(braintreeClient, tokenizationClient);

        VisaCheckoutTokenizeCallback listener = mock(VisaCheckoutTokenizeCallback.class);
        sut.tokenize(mActivity, sampleVisaPaymentSummary(), listener);

        verify(listener).onResult(visaCheckoutNonce, null);
    }

    @Test
    public void tokenize_whenSuccessful_sendsAnalyticEvent() throws Exception {
        VisaCheckoutNonce visaCheckoutNonce =
                VisaCheckoutNonce.fromJson(stringFromFixture("payment_methods/visa_checkout_response.json"));

        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder()
                .successNonce(visaCheckoutNonce)
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(mConfigurationWithVisaCheckout)
                .build();
        VisaCheckoutClient sut = new VisaCheckoutClient(braintreeClient, tokenizationClient);

        VisaCheckoutTokenizeCallback listener = mock(VisaCheckoutTokenizeCallback.class);
        sut.tokenize(mActivity, sampleVisaPaymentSummary(), listener);

        verify(braintreeClient).sendAnalyticsEvent(mActivity, "visacheckout.tokenize.succeeded");
    }

    @Test
    public void tokenize_whenFailure_postsException() throws Exception {
        Exception tokenizeError = new Exception("Mock Failure");
        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder()
                .error(tokenizeError)
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(mConfigurationWithVisaCheckout)
                .build();
        VisaCheckoutClient sut = new VisaCheckoutClient(braintreeClient, tokenizationClient);

        VisaCheckoutTokenizeCallback listener = mock(VisaCheckoutTokenizeCallback.class);
        sut.tokenize(mActivity, sampleVisaPaymentSummary(), listener);

        verify(listener).onResult(null, tokenizeError);
    }

    @Test
    public void tokenize_whenFailure_sendsAnalyticEvent() throws Exception {
        Exception tokenizeError = new Exception("Mock Failure");
        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder()
                .error(tokenizeError)
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(mConfigurationWithVisaCheckout)
                .build();
        VisaCheckoutClient sut = new VisaCheckoutClient(braintreeClient, tokenizationClient);

        VisaCheckoutTokenizeCallback listener = mock(VisaCheckoutTokenizeCallback.class);
        sut.tokenize(mActivity, sampleVisaPaymentSummary(), listener);

        verify(braintreeClient).sendAnalyticsEvent(mActivity, "visacheckout.tokenize.failed");
    }

    private VisaPaymentSummary sampleVisaPaymentSummary() throws JSONException {
        JSONObject summaryJson = new JSONObject()
                .put("encPaymentData", "stubbedEncPaymentData")
                .put("encKey", "stubbedEncKey")
                .put("callid", "stubbedCallId");

        Parcel in = Parcel.obtain();
        in.writeString("SUCCESS");
        in.writeString(summaryJson.toString());
        in.setDataPosition(0);

        return createPaymentSummary(in);
    }
}
