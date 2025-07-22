package com.yoen.yoen_back.service;

import com.yoen.yoen_back.common.utils.Formatter;
import com.yoen.yoen_back.dto.ExchangeRateDto;
import com.yoen.yoen_back.entity.ExchangeRate;
import com.yoen.yoen_back.repository.ExchangeRateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ExchangeRateUpdateService {

    private final ExchangeRateRepository exchangeRateRepository;
    private final WebClient webClient;

    // 15분 주기
    @Scheduled(fixedDelay = 1000 * 60 * 15)
    public void setExchangeRate() {
        webClient.get()
                .retrieve()
                .bodyToMono(ExchangeRateDto.class)
                .subscribe(dto -> {
                    // 응답이 도착했을 때 비동기 처리
                    ExchangeRate rate = ExchangeRate.builder()
                            .exchangeRate(Double.parseDouble(dto.country().get(1).value()))
                            .build();
                    exchangeRateRepository.save(rate);
                });

    }

    public ExchangeRate getExchangeRate(String time) {
        LocalDateTime localTime = Formatter.getDateTime(time);
        return exchangeRateRepository.findTopByCreatedAtLessThanOrderByCreatedAtDesc(localTime).orElse(null);
    }

    public ExchangeRate getExchangeRate(LocalDateTime time) {
        return exchangeRateRepository.findTopByCreatedAtLessThanOrderByCreatedAtDesc(time).orElse(null);
    }

    public ExchangeRate getExchangeRateReverse(String time) {
        LocalDateTime localTime = Formatter.getDateTime(time);
        ExchangeRate er = exchangeRateRepository.findTopByCreatedAtLessThanOrderByCreatedAtDesc(localTime).orElse(null);
        if (er != null && er.getExchangeRate() != 0) {
            return ExchangeRate.builder()
                    .exchangeRate(1 / er.getExchangeRate())
                    .build();
        }
        return null;
    }

    public ExchangeRate getExchangeRateReverse(LocalDateTime time) {
        ExchangeRate er = exchangeRateRepository.findTopByCreatedAtLessThanOrderByCreatedAtDesc(time).orElse(null);
        if (er != null && er.getExchangeRate() != 0) {
            return ExchangeRate.builder()
                    .exchangeRate(1 / er.getExchangeRate())
                    .build();
        }
        return null;
    }



}
