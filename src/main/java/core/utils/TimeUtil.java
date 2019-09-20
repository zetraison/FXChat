package core.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class TimeUtil {

    public static String getCurrentTime() {
        return new SimpleDateFormat("HH:mm").format(Calendar.getInstance().getTime());
    }
}
