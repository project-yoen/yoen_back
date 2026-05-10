package com.yoen.yoen_back.common.utils;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FormatterTest {

    // ISO_LOCAL_DATE_TIME 형식의 문자열이 LocalDateTime으로 정확히 변환되는지 확인한다.
    // Formatter는 날짜/시간 파싱을 여러 서비스에서 공통으로 쓰므로, 정상 형식의 기준을 먼저 고정한다.
    @Test
    void getDateTime_validIsoDateTime_returnsLocalDateTime() {
        LocalDateTime result = Formatter.getDateTime("2025-07-01T13:30:00");

        assertThat(result).isEqualTo(LocalDateTime.of(2025, 7, 1, 13, 30));
    }

    // null 입력은 예외가 아니라 null로 반환하도록 구현되어 있다.
    // 이 동작은 서비스 코드에서 선택 입력값을 다룰 때 깨지지 않아야 하므로 명시적으로 테스트한다.
    @Test
    void getDateTime_null_returnsNull() {
        LocalDateTime result = Formatter.getDateTime(null);

        assertThat(result).isNull();
    }

    // LocalDateTime은 날짜와 시간이 모두 있는 ISO 문자열만 허용한다.
    // 날짜만 들어오는 경우는 잘못된 입력으로 보고 DateTimeParseException이 발생해야 한다.
    @Test
    void getDateTime_invalidFormat_throwsDateTimeParseException() {
        assertThatThrownBy(() -> Formatter.getDateTime("2025-07-01"))
                .isInstanceOf(DateTimeParseException.class);
    }

    // ISO_LOCAL_DATE 형식의 문자열이 LocalDate로 정확히 변환되는지 확인한다.
    // 여행 시작일/종료일처럼 날짜만 필요한 값의 기본 파싱 규칙이다.
    @Test
    void getDate_validIsoDate_returnsLocalDate() {
        LocalDate result = Formatter.getDate("2025-07-01");

        assertThat(result).isEqualTo(LocalDate.of(2025, 7, 1));
    }

    // 날짜 파싱에서도 null 입력은 그대로 null을 반환한다.
    // getDateTime과 같은 null 처리 정책을 유지하는지 확인한다.
    @Test
    void getDate_null_returnsNull() {
        LocalDate result = Formatter.getDate(null);

        assertThat(result).isNull();
    }

    // 슬래시 형식의 날짜는 ISO_LOCAL_DATE가 아니므로 파싱 실패가 정상이다.
    // 허용 형식을 넓히지 않는 한 이 예외는 입력 검증의 일부로 볼 수 있다.
    @Test
    void getDate_invalidFormat_throwsDateTimeParseException() {
        assertThatThrownBy(() -> Formatter.getDate("2025/07/01"))
                .isInstanceOf(DateTimeParseException.class);
    }

    // ISO_LOCAL_TIME 형식의 문자열이 LocalTime으로 정확히 변환되는지 확인한다.
    // 현재 서비스에서는 직접 사용처가 많지 않지만 공통 유틸의 공개 메서드라 함께 고정한다.
    @Test
    void getTime_validIsoTime_returnsLocalTime() {
        LocalTime result = Formatter.getTime("13:30:00");

        assertThat(result).isEqualTo(LocalTime.of(13, 30));
    }

    // 시간 파싱에서도 null 입력은 null을 반환한다.
    // 세 파싱 메서드가 같은 null 처리 규칙을 갖는지 확인하는 테스트다.
    @Test
    void getTime_null_returnsNull() {
        LocalTime result = Formatter.getTime(null);

        assertThat(result).isNull();
    }

    // 하이픈으로 구분된 시간은 ISO_LOCAL_TIME 형식이 아니므로 예외가 발생해야 한다.
    // "13:30"은 Java에서 유효한 ISO time으로 처리되므로, 확실히 잘못된 값을 사용한다.
    @Test
    void getTime_invalidFormat_throwsDateTimeParseException() {
        assertThatThrownBy(() -> Formatter.getTime("13-30"))
                .isInstanceOf(DateTimeParseException.class);
    }
}
