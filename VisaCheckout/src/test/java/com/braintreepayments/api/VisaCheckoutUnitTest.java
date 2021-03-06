package com.braintreepayments.api;

import android.os.Parcel;
import androidx.appcompat.app.AppCompatActivity;

import com.braintreepayments.api.exceptions.ConfigurationException;
import com.braintreepayments.api.interfaces.BraintreeErrorListener;
import com.braintreepayments.api.interfaces.BraintreeResponseListener;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCallback;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PaymentMethodBuilder;
import com.braintreepayments.api.models.VisaCheckoutConfiguration;
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
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static com.braintreepayments.api.test.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*", "androidx.*", "org.json.*",
        "com.visa.*" })
@PrepareForTest({ TokenizationClient.class, VisaCheckoutConfiguration.class, VisaCheckout.class })
public class VisaCheckoutUnitTest {

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    private Configuration mConfigurationWithVisaCheckout;
    private BraintreeFragment mBraintreeFragment;
    private TestActivity mActivity;

    @Before
    public void setup() throws JSONException {
        JSONObject visaConfiguration = new JSONObject(stringFromFixture("configuration/with_visa_checkout.json"));
        mConfigurationWithVisaCheckout = Configuration.fromJson(visaConfiguration.toString());

        mActivity = Robolectric.setupActivity(TestActivity.class);

        mBraintreeFragment = new MockFragmentBuilder()
                .activity(mActivity)
                .configuration(mConfigurationWithVisaCheckout)
                .build();

        mBraintreeFragment.addListener(new BraintreeErrorListener() {
            @Override
            public void onError(Exception error) {
                throw new RuntimeException(error);
            }
        });
    }

    @Test
    public void createProfileBuilder_whenNotEnabled_throwsConfigurationException() {
        BraintreeFragment braintreeFragment = new MockFragmentBuilder()
                .build();

        VisaCheckout.createProfileBuilder(braintreeFragment, null);

        ArgumentCaptor<ConfigurationException> argumentCaptor = ArgumentCaptor.forClass(ConfigurationException.class);
        verify(braintreeFragment, times(1)).postCallback(argumentCaptor.capture());

        ConfigurationException configurationException = argumentCaptor.getValue();
        assertEquals("Visa Checkout is not enabled.", configurationException.getMessage());
    }

    @Test
    public void createProfileBuilder_whenProduction_usesProductionConfig() throws Exception {
        final CountDownLatch lock = new CountDownLatch(1);
        BraintreeFragment braintreeFragment = new MockFragmentBuilder()
                .configuration(new TestConfigurationBuilder()
                        .environment("production")
                        .visaCheckout(new TestConfigurationBuilder.TestVisaCheckoutConfigurationBuilder()
                                .apikey("gwApiKey")
                                .supportedCardTypes(CardBrand.VISA, CardBrand.MASTERCARD)
                                .externalClientId("gwExternalClientId"))
                        .build())
                .build();

        VisaCheckout.createProfileBuilder(braintreeFragment, new BraintreeResponseListener<ProfileBuilder>() {
            @Override
            public void onResponse(ProfileBuilder profileBuilder) {
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
        BraintreeFragment braintreeFragment = new MockFragmentBuilder()
                .configuration(new TestConfigurationBuilder()
                        .environment("environment")
                        .visaCheckout(new TestConfigurationBuilder.TestVisaCheckoutConfigurationBuilder()
                                .apikey("gwApiKey")
                                .supportedCardTypes(CardBrand.VISA, CardBrand.MASTERCARD)
                                .externalClientId("gwExternalClientId"))
                        .build())
                .build();

        VisaCheckout.createProfileBuilder(braintreeFragment, new BraintreeResponseListener<ProfileBuilder>() {
            @Override
            public void onResponse(ProfileBuilder profileBuilder) {
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
        mockStatic(TokenizationClient.class);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                PaymentMethodNonceCallback paymentMethodNonceCallback = (PaymentMethodNonceCallback)invocation
                        .getArguments()[2];

                paymentMethodNonceCallback.success(VisaCheckoutNonce.fromJson(
                        stringFromFixture("payment_methods/visa_checkout_response.json")));
                return null;
            }
        }).when(TokenizationClient.class, "tokenize", any(BraintreeFragment.class), any(PaymentMethodBuilder.class),
                any(PaymentMethodNonceCallback.class));
        ArgumentCaptor paymentMethodNonceCaptor = ArgumentCaptor.forClass(VisaCheckoutNonce.class);

        VisaCheckout.tokenize(mBraintreeFragment, sampleVisaPaymentSummary());

        verify(mBraintreeFragment).postCallback((VisaCheckoutNonce) paymentMethodNonceCaptor.capture());
        assertEquals("123456-12345-12345-a-adfa", ((VisaCheckoutNonce) paymentMethodNonceCaptor.getValue())
                .getNonce());
    }

    @Test
    public void tokenize_whenSuccessful_sendsAnalyticEvent() throws Exception {
        mockStatic(TokenizationClient.class);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                PaymentMethodNonceCallback paymentMethodNonceCallback = (PaymentMethodNonceCallback)invocation
                        .getArguments()[2];

                paymentMethodNonceCallback.success(VisaCheckoutNonce.fromJson(
                        stringFromFixture("payment_methods/visa_checkout_response.json")));
                return null;
            }
        }).when(TokenizationClient.class, "tokenize", any(BraintreeFragment.class), any(PaymentMethodBuilder.class),
                any(PaymentMethodNonceCallback.class));
        ArgumentCaptor paymentMethodNonceCaptor = ArgumentCaptor.forClass(VisaCheckoutNonce.class);

        VisaCheckout.tokenize(mBraintreeFragment, sampleVisaPaymentSummary());

        verify(mBraintreeFragment).postCallback((VisaCheckoutNonce) paymentMethodNonceCaptor.capture());
        verify(mBraintreeFragment).sendAnalyticsEvent(eq("visacheckout.tokenize.succeeded"));
    }

    @Test
    public void tokenize_whenFailure_postsException() throws Exception {
        mockStatic(TokenizationClient.class);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                PaymentMethodNonceCallback paymentMethodNonceCallback = (PaymentMethodNonceCallback)invocation
                        .getArguments()[2];

                paymentMethodNonceCallback.failure(new Exception("Mock Failure"));
                return null;
            }
        }).when(TokenizationClient.class, "tokenize", any(BraintreeFragment.class), any(PaymentMethodBuilder.class),
                any(PaymentMethodNonceCallback.class));
        ArgumentCaptor exceptionCaptor = ArgumentCaptor.forClass(Exception.class);

        VisaCheckout.tokenize(mBraintreeFragment, sampleVisaPaymentSummary());

        verify(mBraintreeFragment).postCallback((Exception) exceptionCaptor.capture());
        assertEquals("Mock Failure", ((Exception)exceptionCaptor.getValue()).getMessage());
    }

    @Test
    public void tokenize_whenFailure_sendsAnalyticEvent() throws Exception {
        mockStatic(TokenizationClient.class);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                PaymentMethodNonceCallback paymentMethodNonceCallback = (PaymentMethodNonceCallback)invocation
                        .getArguments()[2];

                paymentMethodNonceCallback.failure(new Exception("Mock Failure"));
                return null;
            }
        }).when(TokenizationClient.class, "tokenize", any(BraintreeFragment.class), any(PaymentMethodBuilder.class),
                any(PaymentMethodNonceCallback.class));
        ArgumentCaptor exceptionCaptor = ArgumentCaptor.forClass(Exception.class);

        VisaCheckout.tokenize(mBraintreeFragment, sampleVisaPaymentSummary());

        verify(mBraintreeFragment).postCallback((Exception) exceptionCaptor.capture());
        verify(mBraintreeFragment).sendAnalyticsEvent(eq("visacheckout.tokenize.failed"));
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

        return VisaPaymentSummary.CREATOR.createFromParcel(in);
    }
}
