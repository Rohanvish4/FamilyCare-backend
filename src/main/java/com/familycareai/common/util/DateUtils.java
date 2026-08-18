package com.familycareai.common.util;

import java.time.LocalDate;
import java.time.Period;

public class DateUtils {

    private DateUtils() {
    }

    public static int calculateAge(LocalDate dateOfBirth) {
        if (dateOfBirth == null) {
            return 0;
        }
        return Period.between(dateOfBirth, LocalDate.now()).getYears();
    }

    public static boolean isAdult(LocalDate dateOfBirth) {
        return calculateAge(dateOfBirth) >= 18;
    }
}
