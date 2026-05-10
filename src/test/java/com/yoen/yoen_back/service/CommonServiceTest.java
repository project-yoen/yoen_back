package com.yoen.yoen_back.service;

import com.yoen.yoen_back.dto.etc.CategoryRequestDto;
import com.yoen.yoen_back.dto.etc.CategoryResponseDto;
import com.yoen.yoen_back.dto.etc.DestinationRequestDto;
import com.yoen.yoen_back.dto.etc.DestinationResponseDto;
import com.yoen.yoen_back.entity.Category;
import com.yoen.yoen_back.entity.travel.Destination;
import com.yoen.yoen_back.entity.travel.Travel;
import com.yoen.yoen_back.entity.travel.TravelDestination;
import com.yoen.yoen_back.enums.Nation;
import com.yoen.yoen_back.enums.PaymentType;
import com.yoen.yoen_back.repository.CategoryRepository;
import com.yoen.yoen_back.repository.travel.DestinationRepository;
import com.yoen.yoen_back.repository.travel.TravelDestinationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommonServiceTest {

    @Mock
    private TravelDestinationRepository travelDestinationRepository;

    @Mock
    private DestinationRepository destinationRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CommonService commonService;

    @Test
    @DisplayName("여행 목적지를 생성하면 기존 매핑을 비활성화하고 새 매핑을 저장한다")
    void createTravelDestination_deactivatesPreviousMappingsAndSavesNewMappings() {
        Travel travel = travel(10L);
        Destination tokyo = destination(100L, "도쿄", Nation.JAPAN);
        Destination osaka = destination(101L, "오사카", Nation.JAPAN);
        TravelDestination previousMapping = travelDestination(1L, travel, destination(99L, "교토", Nation.JAPAN));
        when(destinationRepository.findByDestinationIdAndIsActiveTrue(100L)).thenReturn(Optional.of(tokyo));
        when(destinationRepository.findByDestinationIdAndIsActiveTrue(101L)).thenReturn(Optional.of(osaka));
        when(travelDestinationRepository.findByTravel_TravelId(10L)).thenReturn(List.of(previousMapping));
        ArgumentCaptor<TravelDestination> captor = ArgumentCaptor.forClass(TravelDestination.class);

        commonService.createTravelDestination(travel, List.of(100L, 101L));

        assertThat(previousMapping.getIsActive()).isFalse();
        verify(travelDestinationRepository, times(4)).save(captor.capture());
        assertThat(captor.getAllValues()).contains(previousMapping);
        assertThat(captor.getAllValues())
                .filteredOn(mapping -> mapping != previousMapping)
                .extracting(mapping -> mapping.getDestination().getDestinationId())
                .containsExactly(100L, 101L);
        assertThat(captor.getAllValues())
                .filteredOn(mapping -> mapping != previousMapping)
                .allSatisfy(mapping -> assertThat(mapping.getTravel()).isSameAs(travel));
    }

    @Test
    @DisplayName("존재하지 않는 목적지 ID가 포함되면 예외를 던진다")
    void createTravelDestination_throwsException_whenDestinationDoesNotExist() {
        Travel travel = travel(10L);
        when(destinationRepository.findByDestinationIdAndIsActiveTrue(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commonService.createTravelDestination(travel, List.of(999L)))
                .isInstanceOf(IllegalArgumentException.class);
        verify(travelDestinationRepository, never()).save(any());
    }

    @Test
    @DisplayName("전체 목적지를 응답 DTO 목록으로 반환한다")
    void getAllDestination_returnsAllDestinationResponses() {
        when(destinationRepository.findAll()).thenReturn(List.of(
                destination(100L, "도쿄", Nation.JAPAN),
                destination(101L, "서울", Nation.KOREA)
        ));

        List<DestinationResponseDto> responses = commonService.getAllDestination();

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(DestinationResponseDto::destinationId)
                .containsExactly(100L, 101L);
        assertThat(responses).extracting(DestinationResponseDto::destinationName)
                .containsExactly("도쿄", "서울");
    }

    @Test
    @DisplayName("국가별 활성 목적지를 응답 DTO 목록으로 반환한다")
    void getNationDestinations_returnsDestinationResponsesByNation() {
        when(destinationRepository.findByNationAndIsActiveTrue(Nation.JAPAN)).thenReturn(List.of(
                destination(100L, "도쿄", Nation.JAPAN),
                destination(101L, "오사카", Nation.JAPAN)
        ));

        List<DestinationResponseDto> responses = commonService.getNationDestinations(Nation.JAPAN);

        assertThat(responses).hasSize(2);
        assertThat(responses).allSatisfy(response -> assertThat(response.nation()).isEqualTo(Nation.JAPAN));
        assertThat(responses).extracting(DestinationResponseDto::destinationName)
                .containsExactly("도쿄", "오사카");
    }

    @Test
    @DisplayName("목적지 하나를 저장한다")
    void createDestination_savesDestination() {
        DestinationRequestDto request = new DestinationRequestDto("도쿄", Nation.JAPAN);
        ArgumentCaptor<Destination> captor = ArgumentCaptor.forClass(Destination.class);

        commonService.createDestination(request);

        verify(destinationRepository).save(captor.capture());
        Destination savedDestination = captor.getValue();
        assertThat(savedDestination.getName()).isEqualTo("도쿄");
        assertThat(savedDestination.getNation()).isEqualTo(Nation.JAPAN);
    }

    @Test
    @DisplayName("목적지 여러 개를 저장하고 전체 목록을 반환한다")
    void createDestinations_savesDestinationsAndReturnsAll() {
        List<DestinationRequestDto> requests = List.of(
                new DestinationRequestDto("도쿄", Nation.JAPAN),
                new DestinationRequestDto("서울", Nation.KOREA)
        );
        List<Destination> allDestinations = List.of(
                destination(100L, "도쿄", Nation.JAPAN),
                destination(101L, "서울", Nation.KOREA)
        );
        when(destinationRepository.findAll()).thenReturn(allDestinations);
        ArgumentCaptor<Destination> captor = ArgumentCaptor.forClass(Destination.class);

        List<Destination> result = commonService.createDestinations(requests);

        assertThat(result).isSameAs(allDestinations);
        verify(destinationRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(Destination::getName)
                .containsExactly("도쿄", "서울");
        verify(destinationRepository).findAll();
    }

    @Test
    @DisplayName("카테고리 목록을 저장하고 응답 DTO 목록으로 반환한다")
    void createCategory_savesCategoriesAndReturnsResponses() {
        List<CategoryRequestDto> requests = List.of(
                new CategoryRequestDto(null, "식비", PaymentType.PAYMENT),
                new CategoryRequestDto(null, "공금", PaymentType.SHAREDFUND)
        );
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category category = invocation.getArgument(0);
            if (category.getCategoryName().equals("식비")) {
                category.setCategoryId(1L);
            } else {
                category.setCategoryId(2L);
            }
            return category;
        });
        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);

        List<CategoryResponseDto> responses = commonService.createCategory(requests);

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(CategoryResponseDto::categoryId)
                .containsExactly(1L, 2L);
        assertThat(responses).extracting(CategoryResponseDto::categoryName)
                .containsExactly("식비", "공금");
        assertThat(responses).extracting(CategoryResponseDto::type)
                .containsExactly(PaymentType.PAYMENT, PaymentType.SHAREDFUND);
        verify(categoryRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(Category::getCategoryName)
                .containsExactly("식비", "공금");
    }

    @Test
    @DisplayName("결제 타입별 카테고리 목록을 응답 DTO로 반환한다")
    void getCategoryListByType_returnsResponsesByPaymentType() {
        when(categoryRepository.findAllByType(PaymentType.PAYMENT)).thenReturn(List.of(
                category(1L, "식비", PaymentType.PAYMENT),
                category(2L, "교통", PaymentType.PAYMENT)
        ));

        List<CategoryResponseDto> responses = commonService.getCategoryListByType(PaymentType.PAYMENT);

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(CategoryResponseDto::categoryName)
                .containsExactly("식비", "교통");
        assertThat(responses).allSatisfy(response -> assertThat(response.type()).isEqualTo(PaymentType.PAYMENT));
    }

    private Travel travel(Long travelId) {
        return Travel.builder()
                .travelId(travelId)
                .travelName("도쿄 여행")
                .numOfPeople(4L)
                .numOfJoinedPeople(1L)
                .nation(Nation.JAPAN)
                .sharedFund(0L)
                .startDate(LocalDate.of(2026, 6, 1))
                .endDate(LocalDate.of(2026, 6, 5))
                .build();
    }

    private Destination destination(Long destinationId, String name, Nation nation) {
        return Destination.builder()
                .destinationId(destinationId)
                .name(name)
                .nation(nation)
                .build();
    }

    private TravelDestination travelDestination(Long travelDestinationId, Travel travel, Destination destination) {
        return TravelDestination.builder()
                .travelDestinationId(travelDestinationId)
                .travel(travel)
                .destination(destination)
                .build();
    }

    private Category category(Long categoryId, String name, PaymentType type) {
        return Category.builder()
                .categoryId(categoryId)
                .categoryName(name)
                .type(type)
                .build();
    }
}
