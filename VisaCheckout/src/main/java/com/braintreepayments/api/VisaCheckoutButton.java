package com.braintreepayments.api;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

public class VisaCheckoutButton extends FrameLayout {

    public VisaCheckoutButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView();
    }

    public VisaCheckoutButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public VisaCheckoutButton(Context context) {
        super(context);
        initView();
    }

    private void initView() {
        inflate(getContext(), R.layout.visa_checkout_button_wrapper, this);
    }
}