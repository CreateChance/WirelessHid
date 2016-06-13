package com.baniel.wirelesshid;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.Button;

public class Key extends Button {

    private final KeyAttributes mKeyAttributes;
    private KeyListener mKeyListener;

    public static class KeyAttributes {
        public String keyFunction;
        public String mainLabel;
        public String shiftLabel;
        public byte keyCode;
    }

    public interface KeyListener {
        public void onKeyDown(String keyFunction, byte keyCode);

        public void onKeyUp(String keyFunction, byte keyCode);
    }

    public Key(Context context, KeyAttributes keyAttributes) {
        this(context, keyAttributes, null);
        setDefaultConfiguration();
    }

    public Key(Context context, KeyAttributes keyAttributes, AttributeSet attrs) {
        this(context, keyAttributes, attrs, android.R.attr.buttonStyleSmall);
    }

    public Key(Context context, KeyAttributes keyAttributes, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mKeyAttributes = keyAttributes;
        setText(mKeyAttributes.mainLabel);
    }

    private void setDefaultConfiguration() {
        setPadding(1, 1, 1, 1);
    }

    public void setShiftState(boolean shiftOn) {
        String label = null;
        if (shiftOn) {
            label = mKeyAttributes.shiftLabel;
        }
        else {
            label = mKeyAttributes.mainLabel;
        }

        if (label != null && label.length() > 0) {
            setText(label);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                dispatchOnKeyDown();
                break;
            case MotionEvent.ACTION_UP:
                dispatchOnKeyUp();
                break;
        }
        return super.onTouchEvent(event);
    }

    private void dispatchOnKeyDown() {
        if (mKeyListener != null) {
            mKeyListener.onKeyDown(mKeyAttributes.keyFunction, mKeyAttributes.keyCode);
        }
    }

    private void dispatchOnKeyUp() {
        if (mKeyListener != null) {
            mKeyListener.onKeyUp(mKeyAttributes.keyFunction, mKeyAttributes.keyCode);
        }
    }

    public void setKeyListener(KeyListener keyListener) {
        mKeyListener = keyListener;
    }
}
