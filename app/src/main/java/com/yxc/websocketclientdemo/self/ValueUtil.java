package com.yxc.websocketclientdemo.self;

import android.text.TextUtils;

/**
 * @author luo
 */
public class ValueUtil {

    public static String getString(Object name, String defValue) {
        return name != null ? String.valueOf(name) : defValue;
    }

    public static int getInt(String name, int defValue) {
        return !TextUtils.isEmpty(name) ? Integer.parseInt(name) : defValue;
    }

    public static int getInt(Object name, int defValue) {
        return name != null ? (int) name : defValue;
    }

    public static boolean getBool(Object name, boolean defValue) {
        return name != null ? (boolean) name : defValue;
    }

    public static String getStringFormat(String name) {
        return !TextUtils.isEmpty(name) ? name : "";
    }
}
