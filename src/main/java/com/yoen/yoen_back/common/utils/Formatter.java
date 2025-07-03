package com.yoen.yoen_back.common.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Formatter {
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ISO_LOCAL_TIME;

    public static LocalDateTime getDateTime(String date) {
        return LocalDateTime.parse(date, dateTimeFormatter);
    }

    public static LocalDate getDate(String date) {
        return LocalDate.parse(date, dateFormatter);
    }

    public static LocalTime getTime(String time) {
        return LocalTime.parse(time, timeFormatter);
    }

}
