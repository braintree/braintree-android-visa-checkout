package com.braintreepayments.api;

import com.visa.checkout.Profile;
import com.visa.checkout.Profile.CardBrand;
import com.visa.checkout.Profile.ProfileBuilder;
import com.visa.checkout.VisaCheckoutSdk;
import com.visa.checkout.VisaPaymentSummary;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static com.braintreepayments.api.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({"org.powermock.*", "org.mockito.*", "org.robolectric.*", "android.*", "androidx.*", "com.visa.checkout.Profile", "com.visa.checkout.Profile.ProfileBuilder"})
@PrepareForTest({ VisaPaymentSummary.class, TokenizationClient.class, VisaCheckoutSdk.class })
public class VisaCheckoutUnitTest {

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    private Configuration mConfigurationWithVisaCheckout;
    private VisaPaymentSummary visaPaymentSummary;

    @Before
    public void setup() throws Exception {
        JSONObject visaConfiguration = new JSONObject(stringFromFixture("configuration/with_visa_checkout.json"));
        mConfigurationWithVisaCheckout = Configuration.fromJson(visaConfiguration.toString());

        visaPaymentSummary = PowerMockito.mock(VisaPaymentSummary.class);
        when(visaPaymentSummary.getCallId()).thenReturn("stubbedCallId");
        when(visaPaymentSummary.getEncKey()).thenReturn("stubbedEncKey");
        when(visaPaymentSummary.getEncPaymentData()).thenReturn("stubbedEncPaymentData");
        PowerMockito.whenNew(VisaPaymentSummary.class).withAnyArguments().thenReturn(visaPaymentSummary);
    }

    @Test
    public void createProfileBuilder_whenNotEnabled_throwsConfigurationException() {
        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder().build();

        Configuration configuration = TestConfigurationBuilder.basicConfig();
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .build();

        VisaCheckout sut = new VisaCheckout(braintreeClient, tokenizationClient);

        VisaCheckoutCreateProfileBuilderCallback listener = mock(VisaCheckoutCreateProfileBuilderCallback.class);
        sut.createProfileBuilder(listener);

        ArgumentCaptor<ConfigurationException> captor = ArgumentCaptor.forClass(ConfigurationException.class);
        verify(listener, times(1)).onResult((ProfileBuilder) isNull(), captor.capture());

        ConfigurationException configurationException = captor.getValue();
        assertEquals("Visa Checkout is not enabled.", configurationException.getMessage());
    }

    // TODO: Investigate test failures after visa-checkout repo has been migrated to braintree-core
    @Test
    @Ignore("Ignoring until failures can be investigated further")
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
        VisaCheckout sut = new VisaCheckout(braintreeClient, tokenizationClient);

        sut.createProfileBuilder(new VisaCheckoutCreateProfileBuilderCallback() {
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
    @Ignore("Ignoring until failures can be investigated further")
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
        VisaCheckout sut = new VisaCheckout(braintreeClient, tokenizationClient);

        sut.createProfileBuilder(new VisaCheckoutCreateProfileBuilderCallback() {
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
        VisaCheckout sut = new VisaCheckout(braintreeClient, tokenizationClient);

        VisaCheckoutTokenizeCallback listener = mock(VisaCheckoutTokenizeCallback.class);
        sut.tokenize(sampleVisaPaymentSummary(), listener);

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
        VisaCheckout sut = new VisaCheckout(braintreeClient, tokenizationClient);

        VisaCheckoutTokenizeCallback listener = mock(VisaCheckoutTokenizeCallback.class);
        sut.tokenize(sampleVisaPaymentSummary(), listener);

        verify(braintreeClient).sendAnalyticsEvent("visacheckout.tokenize.succeeded");
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
        VisaCheckout sut = new VisaCheckout(braintreeClient, tokenizationClient);

        VisaCheckoutTokenizeCallback listener = mock(VisaCheckoutTokenizeCallback.class);
        sut.tokenize(sampleVisaPaymentSummary(), listener);

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
        VisaCheckout sut = new VisaCheckout(braintreeClient, tokenizationClient);

        VisaCheckoutTokenizeCallback listener = mock(VisaCheckoutTokenizeCallback.class);
        sut.tokenize(sampleVisaPaymentSummary(), listener);

        verify(braintreeClient).sendAnalyticsEvent("visacheckout.tokenize.failed");
    }

    private VisaPaymentSummary sampleVisaPaymentSummary() throws JSONException {
//        JSONObject summaryJson = new JSONObject()
//                .put("encPaymentData", "stubbedEncPaymentData")
//                .put("encKey", "stubbedEncKey")
//                .put("callid", "stubbedCallId");
//
//        Parcel in = Parcel.obtain();
//        in.writeString("SUCCESS");
//        in.writeString(summaryJson.toString());
//        in.setDataPosition(0);
//
//        return VisaPaymentSummary.CREATOR.createFromParcel(in);
        return visaPaymentSummary;
    }
}
