package com.yoen.yoen_back.configuration;

import com.yoen.yoen_back.entity.Category;
import com.yoen.yoen_back.entity.travel.Destination;
import com.yoen.yoen_back.enums.Nation;
import com.yoen.yoen_back.enums.PaymentType;
import com.yoen.yoen_back.repository.CategoryRepository;
import com.yoen.yoen_back.repository.travel.DestinationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeedDataInitializerTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private DestinationRepository destinationRepository;

    @Test
    @DisplayName("기본 카테고리와 목적지가 없으면 저장한다")
    void run_savesMissingSeedData() throws Exception {
        SeedDataInitializer initializer = new SeedDataInitializer(categoryRepository, destinationRepository);
        ArgumentCaptor<Category> categoryCaptor = ArgumentCaptor.forClass(Category.class);
        ArgumentCaptor<Destination> destinationCaptor = ArgumentCaptor.forClass(Destination.class);

        initializer.run(null);

        verify(categoryRepository, times(12)).save(categoryCaptor.capture());
        assertThat(categoryCaptor.getAllValues())
                .extracting(Category::getCategoryName)
                .containsExactly("식비", "카페/간식", "교통", "숙소", "관광/입장권", "쇼핑", "기타", "항공권", "숙소예약", "교통예약", "티켓예약", "기타");
        assertThat(categoryCaptor.getAllValues())
                .extracting(Category::getType)
                .containsExactly(PaymentType.PAYMENT, PaymentType.PAYMENT, PaymentType.PAYMENT, PaymentType.PAYMENT, PaymentType.PAYMENT, PaymentType.PAYMENT, PaymentType.PAYMENT,
                        PaymentType.PREPAYMENT, PaymentType.PREPAYMENT, PaymentType.PREPAYMENT, PaymentType.PREPAYMENT, PaymentType.PREPAYMENT);

        verify(destinationRepository, times(14)).save(destinationCaptor.capture());
        assertThat(destinationCaptor.getAllValues())
                .extracting(Destination::getName)
                .containsExactly("서울", "부산", "제주", "강릉", "경주", "여수", "전주", "도쿄", "오사카", "교토", "후쿠오카", "삿포로", "오키나와", "나고야");
        assertThat(destinationCaptor.getAllValues())
                .extracting(Destination::getNation)
                .containsExactly(Nation.KOREA, Nation.KOREA, Nation.KOREA, Nation.KOREA, Nation.KOREA, Nation.KOREA, Nation.KOREA,
                        Nation.JAPAN, Nation.JAPAN, Nation.JAPAN, Nation.JAPAN, Nation.JAPAN, Nation.JAPAN, Nation.JAPAN);
    }

    @Test
    @DisplayName("이미 존재하는 시드 데이터는 다시 저장하지 않는다")
    void run_skipsExistingSeedData() throws Exception {
        when(categoryRepository.existsByCategoryNameAndType("식비", PaymentType.PAYMENT)).thenReturn(true);
        when(destinationRepository.existsByNationAndName(Nation.KOREA, "서울")).thenReturn(true);
        SeedDataInitializer initializer = new SeedDataInitializer(categoryRepository, destinationRepository);
        ArgumentCaptor<Category> categoryCaptor = ArgumentCaptor.forClass(Category.class);
        ArgumentCaptor<Destination> destinationCaptor = ArgumentCaptor.forClass(Destination.class);

        initializer.run(null);

        verify(categoryRepository, times(11)).save(categoryCaptor.capture());
        assertThat(categoryCaptor.getAllValues())
                .extracting(Category::getCategoryName)
                .doesNotContain("식비");

        verify(destinationRepository, times(13)).save(destinationCaptor.capture());
        assertThat(destinationCaptor.getAllValues())
                .extracting(Destination::getName)
                .doesNotContain("서울");
    }
}
