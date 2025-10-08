package com.evcharging.mobile.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateUtils {

    public static String formatDateTime(Date date) {
        if (date == null) return "";
        SimpleDateFormat formatter = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
        return formatter.format(date);
    }

    public static boolean isWithinNext7Days(Date date) {
        if (date == null) return false;
        
        Calendar now = Calendar.getInstance();
        Calendar future = Calendar.getInstance();
        future.add(Calendar.DAY_OF_MONTH, 7);
        
        return date.after(now.getTime()) && date.before(future.getTime());
    }
}