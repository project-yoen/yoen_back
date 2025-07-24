package com.yoen.yoen_back.service;

import com.yoen.yoen_back.common.utils.Formatter;
import com.yoen.yoen_back.dto.travel.TravelRecordImageDto;
import com.yoen.yoen_back.dto.travel.TravelRecordRequestDto;
import com.yoen.yoen_back.dto.travel.TravelRecordResponseDto;
import com.yoen.yoen_back.entity.image.Image;
import com.yoen.yoen_back.entity.image.TravelRecordImage;
import com.yoen.yoen_back.entity.travel.Travel;
import com.yoen.yoen_back.entity.travel.TravelRecord;
import com.yoen.yoen_back.entity.travel.TravelUser;
import com.yoen.yoen_back.entity.user.User;
import com.yoen.yoen_back.repository.image.TravelRecordImageRepository;
import com.yoen.yoen_back.repository.travel.TravelRecordRepository;
import com.yoen.yoen_back.repository.travel.TravelRepository;
import com.yoen.yoen_back.repository.travel.TravelUserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RecordService {

    private final TravelRecordRepository travelRecordRepository;
    private final TravelRecordImageRepository travelRecordImageRepository;

    private final ImageService imageService;

    /**
     * 조회용
     **/
    private final TravelRepository travelRepository;
    private final TravelUserRepository travelUserRepository;
    private final SpringDataWebProperties springDataWebProperties;


    public List<TravelRecord> getAllTravelRecordsByTravelId(Long travelId) {
        return travelRecordRepository.findByTravel_TravelIdAndIsActiveTrue(travelId);
    }

    // TODO: 날짜별 여행기록 리스트 받기
    public List<TravelRecordResponseDto> getTravelRecordsByDate(Long travelUserId, String date) {
        TravelUser travelUser = travelUserRepository.getReferenceById(travelUserId);
        Travel tv = travelUser.getTravel();
        LocalDateTime startDateTime = Formatter.getDateTime(date);
        List<TravelRecord> tvrList = travelRecordRepository.findAllByTravelAndRecordTimeBetween(tv, startDateTime, startDateTime.plusDays(1));

        return tvrList.stream().map(tvr -> {
            List<TravelRecordImageDto> trilist = travelRecordImageRepository.findByTravelRecordAndIsActiveTrue(tvr).stream().map(tvri -> new TravelRecordImageDto(tvri.getTravelRecordImageId(), tvri.getImage().getImageUrl())).toList();
            return new TravelRecordResponseDto(tvr.getTravelRecordId(), tvr.getTitle(), tvr.getContent(), tvr.getRecordTime(), trilist);
        }).toList();
    }

    // TODO: 여행기록 아이디로 여행기록 정보 받기 (보류)


    // 여행 기록 추가 할시 (한번에 이미지까지 저장) (생성)
    @Transactional
    public TravelRecordResponseDto createTravelRecord(User user, TravelRecordRequestDto dto, List<MultipartFile> files) {
        Travel tv = travelRepository.getReferenceById(dto.travelId());
        TravelUser tu = travelUserRepository.getReferenceById(dto.travelUserId());
        TravelRecord travelRecord = TravelRecord.builder()
                .travel(tv)
                .travelUser(tu)
                .title(dto.title())
                .content(dto.content())
                .recordTime(Formatter.getDateTime(dto.recordTime()))
                .build();
        // 먼저 저장
        TravelRecord tr = travelRecordRepository.save(travelRecord);

        // 이미지 파일이 존재할 시
        if (files != null && !files.isEmpty()) {
            List<Image> images = imageService.saveImages(user, files); // 클라우드에 업로드 및 image 레포지토리에 저장
            // TODO: 여기서부턴 좀 수정이 있어야할거 같음 지금 이미지를 불러다가 응답하는게 좀 복잡함 (왜 세개로 분리했는지 고민)
            List<TravelRecordImageDto> imagesDto = images.stream().map(image -> {
                TravelRecordImage tri = TravelRecordImage.builder()
                        .image(image)
                        .travelRecord(travelRecord)
                        .build();
                TravelRecordImage tmp = travelRecordImageRepository.save(tri); // travelRecordImage 레포에 image들 저장
                return new TravelRecordImageDto(tmp.getTravelRecordImageId(), image.getImageUrl());
            }).toList();

            return new TravelRecordResponseDto(tr.getTravelRecordId(), tr.getTitle(), tr.getContent(), tr.getRecordTime(), imagesDto);
        }
        // 이미지 파일이 존재하지 않을 시

        return new TravelRecordResponseDto(tr.getTravelRecordId(), tr.getTitle(), tr.getContent(), tr.getRecordTime(), new ArrayList<>());

    }

    // 사진을 제외한 여행기록을 수정할시 (수정)
    @Transactional
    public TravelRecordResponseDto updateTravelRecord(TravelRecordRequestDto dto) {
        TravelRecord tr = travelRecordRepository.getReferenceById(dto.travelRecordId());

        // 수정
        tr.setTitle(dto.title());
        tr.setContent(dto.content());
        tr.setRecordTime(Formatter.getDateTime(dto.recordTime()));
        travelRecordRepository.save(tr);

        return new TravelRecordResponseDto(tr.getTravelRecordId(), tr.getTitle(), tr.getContent(), tr.getRecordTime(), new ArrayList<>());
    }

    // 기존 여행기록에 사진들을 추가할시 (수정)
    public void updateTravelRecordImages(User user, Long paymentId, List<MultipartFile> files) {
        //받은 이미지들을 저장한다
        List<Image> images = imageService.saveImages(user, files);
        TravelRecord tr = travelRecordRepository.getReferenceById(paymentId);
        //이미지 리스트를 하나하나 변환하여 DTO List로 저장한다
        List<TravelRecordImageDto> imagesDto = images.stream().map(
                image -> {
                    TravelRecordImage tri = TravelRecordImage.builder()
                            .image(image)
                            .travelRecord(tr)
                            .build();
                    TravelRecordImage tmp = travelRecordImageRepository.save(tri);

                    return new TravelRecordImageDto(tmp.getTravelRecordImageId(), image.getImageUrl());
                }
        ).toList();
    }

    // 기존 여행기록에서 사진을 삭제할시 (수정)
    public void deleteTravelRecordImage(Long travelRecordImageId) {
        Optional<TravelRecordImage> paymentImage = travelRecordImageRepository.findByTravelRecordImageIdAndIsActiveTrue(travelRecordImageId);
        paymentImage.ifPresent(image -> {
            // 사진 모집단 삭제 (클라우드 삭제)
            Image img = image.getImage();
            imageService.deleteImage(img.getImageId());

            // paymentImage 삭제
            image.setIsActive(false);
            travelRecordImageRepository.save(image);
        });
    }

    // 여행기록 삭제 (삭제)
    public void deleteTravelRecord(Long travelRecordId) {
        TravelRecord travelRecord = travelRecordRepository.getReferenceById(travelRecordId);
        // 관련 이미지 가져오기
        List<TravelRecordImage> images = travelRecordImageRepository.findAllByTravelRecord_TravelRecordId(travelRecordId);
        // 관련 이미지 삭제
        images.forEach(image -> {
            // 이미지 모집단 중 삭제
            deleteTravelRecordImage(image.getTravelRecordImageId());
        });

        // 여행 기록 삭제
        travelRecord.setIsActive(false);
        travelRecordRepository.save(travelRecord);
    }
}
