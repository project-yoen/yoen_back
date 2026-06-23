package com.yoen.yoen_back.configuration;

import com.yoen.yoen_back.entity.Category;
import com.yoen.yoen_back.entity.travel.Destination;
import com.yoen.yoen_back.enums.Nation;
import com.yoen.yoen_back.enums.PaymentType;
import com.yoen.yoen_back.repository.CategoryRepository;
import com.yoen.yoen_back.repository.travel.DestinationRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SeedDataInitializer implements ApplicationRunner {

    private final CategoryRepository categoryRepository;
    private final DestinationRepository destinationRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedCategories();
        seedDestinations();
    }

    private void seedCategories() {
        List<CategorySeed> seeds = List.of(
                new CategorySeed("식비", PaymentType.PAYMENT),
                new CategorySeed("카페/간식", PaymentType.PAYMENT),
                new CategorySeed("교통", PaymentType.PAYMENT),
                new CategorySeed("숙소", PaymentType.PAYMENT),
                new CategorySeed("관광/입장권", PaymentType.PAYMENT),
                new CategorySeed("쇼핑", PaymentType.PAYMENT),
                new CategorySeed("기타", PaymentType.PAYMENT),
                new CategorySeed("항공권", PaymentType.PREPAYMENT),
                new CategorySeed("숙소예약", PaymentType.PREPAYMENT),
                new CategorySeed("교통예약", PaymentType.PREPAYMENT),
                new CategorySeed("티켓예약", PaymentType.PREPAYMENT),
                new CategorySeed("기타", PaymentType.PREPAYMENT)
        );

        seeds.forEach(seed -> {
            if (!categoryRepository.existsByCategoryNameAndType(seed.name(), seed.type())) {
                categoryRepository.save(Category.builder()
                        .categoryName(seed.name())
                        .type(seed.type())
                        .build());
            }
        });
    }

    private void seedDestinations() {
        List<DestinationSeed> seeds = List.of(
                new DestinationSeed(Nation.KOREA, "서울"),
                new DestinationSeed(Nation.KOREA, "부산"),
                new DestinationSeed(Nation.KOREA, "제주"),
                new DestinationSeed(Nation.KOREA, "강릉"),
                new DestinationSeed(Nation.KOREA, "경주"),
                new DestinationSeed(Nation.KOREA, "여수"),
                new DestinationSeed(Nation.KOREA, "전주"),
                new DestinationSeed(Nation.JAPAN, "도쿄"),
                new DestinationSeed(Nation.JAPAN, "오사카"),
                new DestinationSeed(Nation.JAPAN, "교토"),
                new DestinationSeed(Nation.JAPAN, "후쿠오카"),
                new DestinationSeed(Nation.JAPAN, "삿포로"),
                new DestinationSeed(Nation.JAPAN, "오키나와"),
                new DestinationSeed(Nation.JAPAN, "나고야")
        );

        seeds.forEach(seed -> {
            if (!destinationRepository.existsByNationAndName(seed.nation(), seed.name())) {
                destinationRepository.save(Destination.builder()
                        .nation(seed.nation())
                        .name(seed.name())
                        .build());
            }
        });
    }

    private record CategorySeed(String name, PaymentType type) {
    }

    private record DestinationSeed(Nation nation, String name) {
    }
}
