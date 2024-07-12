package com.yxc.websocketclientdemo.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.ViewTreeObserver;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.lifecycle.Lifecycle;

public final class ViewUtil {

    private ViewUtil() {
    }



    public static void setTopMargin(@NonNull View view, int margin) {
        ((ViewGroup.MarginLayoutParams) view.getLayoutParams()).topMargin = margin;
        view.requestLayout();
    }

    public static void setBottomMargin(@NonNull View view, int margin) {
        ((ViewGroup.MarginLayoutParams) view.getLayoutParams()).bottomMargin = margin;
        view.requestLayout();
    }

    public static int getWidth(@NonNull View view) {
        return view.getLayoutParams().width;
    }

    public static void setPaddingTop(@NonNull View view, int padding) {
        view.setPadding(view.getPaddingLeft(), padding, view.getPaddingRight(), view.getPaddingBottom());
    }

    public static void setPaddingBottom(@NonNull View view, int padding) {
        view.setPadding(view.getPaddingLeft(), view.getPaddingTop(), view.getPaddingRight(), padding);
    }

    public static void setPadding(@NonNull View view, int padding) {
        view.setPadding(padding, padding, padding, padding);
    }

    public static boolean isPointInsideView(@NonNull View view, float x, float y) {
        int[] location = new int[2];

        view.getLocationOnScreen(location);

        int viewX = location[0];
        int viewY = location[1];

        return x > viewX && x < viewX + view.getWidth() &&
                y > viewY && y < viewY + view.getHeight();
    }

    public static int getStatusBarHeight(@NonNull View view) {
        int result = 0;
        int resourceId = view.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = view.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    public static int getNavigationBarHeight(@NonNull View view) {
        int result = 0;
        int resourceId = view.getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = view.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    public static void hideKeyboard(@NonNull Context context, @NonNull View view) {
        InputMethodManager inputManager = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    /**
     * Enables or disables a view and all child views recursively.
     */
    public static void setEnabledRecursive(@NonNull View view, boolean enabled) {
        view.setEnabled(enabled);
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                setEnabledRecursive(viewGroup.getChildAt(i), enabled);
            }
        }
    }
}