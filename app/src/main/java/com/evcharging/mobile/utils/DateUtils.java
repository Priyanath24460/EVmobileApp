package com.evcharging.mobile.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateUtils {

    public static String formatDateTime(Date date) {
        if (date == null) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
        return sdf.format(date);
    }

    public static String formatDate(Date date) {
        if (date == null) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        return sdf.format(date);
    }

    public static String formatTime(Date date) {
        if (date == null) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(date);
    }

    public static boolean isWithinNext7Days(Date date) {
        if (date == null) return false;

        Date now = new Date();
        Date sevenDaysLater = new Date(now.getTime() + (7 * 24 * 60 * 60 * 1000));

        return date.after(now) && date.before(sevenDaysLater);
    }
}
