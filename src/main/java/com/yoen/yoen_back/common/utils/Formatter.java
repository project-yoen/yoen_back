package com.yoen.yoen_back.common.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Formatter {
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ISO_LOCAL_TIME;

    public static LocalDateTime getDateTime(String dateTime) {
        if (dateTime == null) return null;
        return LocalDateTime.parse(dateTime, dateTimeFormatter);
    }

    public static LocalDate getDate(String date) {
        if (date == null) return null;
        return LocalDate.parse(date, dateFormatter);
    }

    public static LocalTime getTime(String time) {
        if (time == null) return null;
        return LocalTime.parse(time, timeFormatter);
    }

}
