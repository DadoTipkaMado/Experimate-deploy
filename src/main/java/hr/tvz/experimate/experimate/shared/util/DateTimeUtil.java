package hr.tvz.experimate.experimate.shared.util;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class DateTimeUtil {

    public static LocalDateTime getStartOfDay(LocalDate localDate){
        return localDate.atStartOfDay();
    }

    public static LocalDateTime getEndOfDay(LocalDate localDate){
        return localDate.atTime(23, 59, 59);
    }
}
