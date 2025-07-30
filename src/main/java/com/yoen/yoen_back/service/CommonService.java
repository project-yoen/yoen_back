package com.yoen.yoen_back.service;

import com.yoen.yoen_back.dto.etc.CategoryRequestDto;
import com.yoen.yoen_back.dto.etc.DestinationRequestDto;
import com.yoen.yoen_back.dto.etc.DestinationResponseDto;
import com.yoen.yoen_back.entity.Category;
import com.yoen.yoen_back.entity.travel.Destination;
import com.yoen.yoen_back.entity.travel.Travel;
import com.yoen.yoen_back.entity.travel.TravelDestination;
import com.yoen.yoen_back.enums.Nation;
import com.yoen.yoen_back.repository.CategoryRepository;
import com.yoen.yoen_back.repository.travel.DestinationRepository;
import com.yoen.yoen_back.repository.travel.TravelDestinationRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommonService {

    private final TravelDestinationRepository travelDestinationRepository;
    private final DestinationRepository destinationRepository;
    private final CategoryRepository categoryRepository;

    /** Destination 관련 **/
    //여행_목적지 객체 매핑
    @Transactional
    public void createTravelDestination(Travel tv, List<Long> destinationIds) {
        destinationIds.forEach(destinationId -> {
            Destination dt = destinationRepository.findByDestinationIdAndIsActiveTrue(destinationId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 목적지 ID: " + destinationId));
            List<TravelDestination> tdl = travelDestinationRepository.findByTravel_TravelId(tv.getTravelId());

            // TravelDestination 이었던 것들 전부 비활성화
            tdl.forEach(travelDestination -> {
                travelDestination.setIsActive(false);
                travelDestinationRepository.save(travelDestination);
            });

            TravelDestination td = TravelDestination.builder()
                    .travel(tv)
                    .destination(dt)
                    .build();
            travelDestinationRepository.save(td);
        });
    }

    public List<DestinationResponseDto> getAllDestination() {
        List<Destination> dt = destinationRepository.findAll();
        return dt.stream().map(destination ->
                new DestinationResponseDto(destination.getDestinationId(),
                        destination.getNation(), destination.getName())).toList();
    }
    public List<DestinationResponseDto> getNationDestinations(Nation nation ) {
        List<Destination> dt = destinationRepository.findByNationAndIsActiveTrue(nation);
        return dt.stream().map(destination ->
                new DestinationResponseDto(destination.getDestinationId(),
                        destination.getNation(), destination.getName())).toList();
    }

    public void createDestination(DestinationRequestDto dto) {
        Destination dt = Destination.builder()
                .name(dto.name())
                .nation(dto.nation())
                .build();
        destinationRepository.save(dt);
    }



    public List<Destination> createDestinations(List<DestinationRequestDto> dtos) {
        dtos.forEach(this::createDestination);
        return destinationRepository.findAll();
    }


    /** 카테고리 관련 **/
    public CategoryRequestDto createCategory(CategoryRequestDto dto) {
        Category category = Category.builder()
                .categoryName(dto.categoryName())
                .type(dto.categoryType())
                .build();
        categoryRepository.save(category);
        return new CategoryRequestDto(category.getCategoryId(), category.getCategoryName(), category.getType());
    }

}
