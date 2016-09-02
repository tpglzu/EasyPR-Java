package org.tpglzu.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by tangpg on 2016/08/26.
 */
public class DateUtils{
    public static String format(Date date,String format) {
        String dateString = null;
        if (null != date) {
            SimpleDateFormat simpleDateFormat =new SimpleDateFormat(format);
            dateString = simpleDateFormat.format(date);
        }

        return dateString;
    }

    public static String formatToSec(Date date){
        return format(date,"yyyyMMdd_HHmmss");
    }

    public static String getTimeStamp(){
        return String.valueOf(new Date().getTime());
    }
}
