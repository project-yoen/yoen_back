package com.yoen.yoen_back.service;

import com.yoen.yoen_back.dto.record.TravelRecordImageDto;
import com.yoen.yoen_back.dto.record.TravelRecordRequestDto;
import com.yoen.yoen_back.dto.record.TravelRecordResponseDto;
import com.yoen.yoen_back.dto.record.TravelRecordUpdateDto;
import com.yoen.yoen_back.entity.image.Image;
import com.yoen.yoen_back.entity.image.TravelRecordImage;
import com.yoen.yoen_back.entity.travel.Travel;
import com.yoen.yoen_back.entity.travel.TravelRecord;
import com.yoen.yoen_back.entity.travel.TravelUser;
import com.yoen.yoen_back.entity.user.User;
import com.yoen.yoen_back.enums.Gender;
import com.yoen.yoen_back.enums.Nation;
import com.yoen.yoen_back.enums.Role;
import com.yoen.yoen_back.repository.image.TravelRecordImageRepository;
import com.yoen.yoen_back.repository.travel.TravelRecordRepository;
import com.yoen.yoen_back.repository.travel.TravelRepository;
import com.yoen.yoen_back.repository.travel.TravelUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecordServiceTest {

    // RecordService는 여행 기록과 기록 이미지를 함께 다루지만, 실제 파일 업로드는 ImageService의 책임이다.
    // 유닛 테스트에서는 Repository와 ImageService를 mock으로 두고 기록 생성/수정/삭제 흐름만 검증한다.

    @Mock
    private TravelRecordRepository travelRecordRepository;

    @Mock
    private TravelRecordImageRepository travelRecordImageRepository;

    @Mock
    private ImageService imageService;

    @Mock
    private TravelRepository travelRepository;

    @Mock
    private TravelUserRepository travelUserRepository;

    @Mock
    private MultipartFile multipartFile;

    @InjectMocks
    private RecordService recordService;

    // travelId 기준 전체 여행 기록 조회는 Repository 호출 결과를 그대로 반환하는 단순 위임 메서드다.
    // 이 테스트는 불필요한 가공 없이 repository 결과가 보존되는지 확인한다.
    @Test
    void getAllTravelRecordsByTravelId_existingRecords_returnsRepositoryResult() {
        TravelRecord record = travelRecord(100L, travel(1L), travelUser(10L, travel(1L), user(1L), Role.WRITER));
        when(travelRecordRepository.findByTravel_TravelIdAndIsActiveTrue(1L)).thenReturn(List.of(record));

        List<TravelRecord> result = recordService.getAllTravelRecordsByTravelId(1L);

        assertThat(result).containsExactly(record);
    }

    // Travel 엔티티 기준 전체 여행 기록 조회도 단순 위임 메서드다.
    // Controller에서는 권한 체크 후 Travel 객체를 얻어 이 메서드로 넘긴다.
    @Test
    void getAllTravelRecordsByTravel_existingRecords_returnsRepositoryResult() {
        Travel travel = travel(1L);
        TravelRecord record = travelRecord(100L, travel, travelUser(10L, travel, user(1L), Role.WRITER));
        when(travelRecordRepository.findByTravelAndIsActiveTrue(travel)).thenReturn(List.of(record));

        List<TravelRecord> result = recordService.getAllTravelRecordsByTravel(travel);

        assertThat(result).containsExactly(record);
    }

    // 날짜별 조회는 입력 시간을 해당 날짜의 00:00:00으로 정규화한 뒤 다음 날 00:00:00 전까지 조회한다.
    // 조회된 기록마다 연결된 활성 이미지를 읽어 응답 DTO의 이미지 목록으로 변환하는지도 확인한다.
    @Test
    void getTravelRecordsByDate_validDate_returnsRecordDtosWithImages() {
        User user = user(1L);
        Travel travel = travel(1L);
        TravelUser travelUser = travelUser(10L, travel, user, Role.WRITER);
        TravelRecord record = travelRecord(100L, travel, travelUser);
        Image image = image(200L, user, "record.jpg", "https://image.example/record.jpg");
        TravelRecordImage recordImage = travelRecordImage(300L, record, image);
        LocalDateTime start = LocalDateTime.of(2025, 7, 1, 0, 0);
        when(travelRecordRepository.findAllByTravelAndRecordTimeBetweenAndIsActiveTrue(travel, start, start.plusDays(1))).thenReturn(List.of(record));
        when(travelRecordImageRepository.findByTravelRecordAndIsActiveTrue(record)).thenReturn(List.of(recordImage));

        List<TravelRecordResponseDto> result = recordService.getTravelRecordsByDate(travel, "2025-07-01T10:30:00");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).travelRecordId()).isEqualTo(100L);
        assertThat(result.get(0).travelNickName()).isEqualTo("Traveler 1");
        assertThat(result.get(0).images()).containsExactly(new TravelRecordImageDto(300L, "https://image.example/record.jpg"));
    }

    // 이미지 없는 기록 생성 케이스다.
    // 여행과 참여자를 찾은 뒤 TravelRecord만 저장하고, 응답의 이미지 목록은 빈 리스트여야 한다.
    @Test
    void createTravelRecord_withoutImages_savesRecordAndReturnsEmptyImageList() {
        User user = user(1L);
        Travel travel = travel(1L);
        TravelUser travelUser = travelUser(10L, travel, user, Role.WRITER);
        TravelRecordRequestDto dto = new TravelRecordRequestDto(null, 1L, "title", "content", "2025-07-01T10:30:00");
        TravelRecord savedRecord = travelRecord(100L, travel, travelUser);
        when(travelRepository.getReferenceById(1L)).thenReturn(travel);
        when(travelUserRepository.findByTravelAndUserAndIsActiveTrue(travel, user)).thenReturn(Optional.of(travelUser));
        when(travelRecordRepository.save(org.mockito.ArgumentMatchers.any(TravelRecord.class))).thenReturn(savedRecord);

        TravelRecordResponseDto result = recordService.createTravelRecord(user, dto, null);

        assertThat(result.travelRecordId()).isEqualTo(100L);
        assertThat(result.travelNickName()).isEqualTo("Traveler 1");
        assertThat(result.images()).isEmpty();
        verify(imageService, never()).saveImages(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    // 기록을 작성하려는 사용자가 해당 여행의 참여자가 아니면 생성할 수 없다.
    // 이 경우 기록 저장이나 이미지 저장으로 진행하지 않고 AccessDeniedException이 발생해야 한다.
    @Test
    void createTravelRecord_missingTravelUser_throwsAccessDeniedException() {
        User user = user(1L);
        Travel travel = travel(1L);
        TravelRecordRequestDto dto = new TravelRecordRequestDto(null, 1L, "title", "content", "2025-07-01T10:30:00");
        when(travelRepository.getReferenceById(1L)).thenReturn(travel);
        when(travelUserRepository.findByTravelAndUserAndIsActiveTrue(travel, user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recordService.createTravelRecord(user, dto, null))
                .isInstanceOf(AccessDeniedException.class);
    }

    // 이미지가 포함된 기록 생성 케이스다.
    // ImageService는 mock으로 업로드 결과를 반환하고, RecordService가 TravelRecordImage 매핑을 저장하는지 확인한다.
    // 여행 대표 이미지가 비어 있으면 첫 번째 기록 이미지를 기반으로 대표 이미지도 설정한다.
    @Test
    void createTravelRecord_withImages_savesImagesAndRecordImageMappings() {
        User user = user(1L);
        Travel travel = travel(1L);
        TravelUser travelUser = travelUser(10L, travel, user, Role.WRITER);
        TravelRecordRequestDto dto = new TravelRecordRequestDto(null, 1L, "title", "content", "2025-07-01T10:30:00");
        TravelRecord savedRecord = travelRecord(100L, travel, travelUser);
        Image uploadedImage = image(200L, user, "record.jpg", "https://image.example/record.jpg");
        Image travelProfileImage = image(201L, user, "profile.jpg", "https://image.example/profile.jpg");
        TravelRecordImage savedRecordImage = travelRecordImage(300L, savedRecord, uploadedImage);
        when(travelRepository.getReferenceById(1L)).thenReturn(travel);
        when(travelUserRepository.findByTravelAndUserAndIsActiveTrue(travel, user)).thenReturn(Optional.of(travelUser));
        when(travelRecordRepository.save(org.mockito.ArgumentMatchers.any(TravelRecord.class))).thenReturn(savedRecord);
        when(imageService.saveImages(user, List.of(multipartFile))).thenReturn(List.of(uploadedImage));
        when(imageService.saveImageByUrl(user, "https://image.example/record.jpg")).thenReturn(travelProfileImage);
        when(travelRecordImageRepository.save(org.mockito.ArgumentMatchers.any(TravelRecordImage.class))).thenReturn(savedRecordImage);

        TravelRecordResponseDto result = recordService.createTravelRecord(user, dto, List.of(multipartFile));

        assertThat(result.travelRecordId()).isEqualTo(100L);
        assertThat(result.images()).containsExactly(new TravelRecordImageDto(300L, "https://image.example/record.jpg"));
        assertThat(travel.getTravelImage()).isEqualTo(travelProfileImage);
        verify(imageService).saveImages(user, List.of(multipartFile));
        verify(imageService).saveImageByUrl(user, "https://image.example/record.jpg");
    }

    // 이미지 없는 기록 수정 케이스다.
    // 기존 TravelRecord 엔티티의 제목, 내용, 기록 시간이 요청 DTO 값으로 바뀌고 저장되는지 확인한다.
    @Test
    void updateTravelRecord_withoutImages_updatesFieldsAndReturnsEmptyImageList() {
        User user = user(1L);
        Travel travel = travel(1L);
        TravelUser travelUser = travelUser(10L, travel, user, Role.WRITER);
        TravelRecord record = travelRecord(100L, travel, travelUser);
        TravelRecordUpdateDto dto = new TravelRecordUpdateDto(100L, 1L, "updated title", "updated content", "2025-07-02T11:30:00", List.of());
        when(travelRecordRepository.getReferenceById(100L)).thenReturn(record);

        TravelRecordResponseDto result = recordService.updateTravelRecord(user, dto, null);

        assertThat(record.getTitle()).isEqualTo("updated title");
        assertThat(record.getContent()).isEqualTo("updated content");
        assertThat(record.getRecordTime()).isEqualTo(LocalDateTime.of(2025, 7, 2, 11, 30));
        assertThat(result.images()).isEmpty();
        verify(travelRecordRepository).save(record);
    }

    // 수정 요청에 삭제할 이미지 ID가 포함된 경우다.
    // deleteTravelRecordImage 내부 흐름을 통해 실제 이미지 삭제 서비스 호출과 매핑 soft delete가 수행되어야 한다.
    @Test
    void updateTravelRecord_withRemovedImages_deletesRequestedImages() {
        User user = user(1L);
        Travel travel = travel(1L);
        TravelUser travelUser = travelUser(10L, travel, user, Role.WRITER);
        TravelRecord record = travelRecord(100L, travel, travelUser);
        Image image = image(200L, user, "record.jpg", "https://image.example/record.jpg");
        TravelRecordImage recordImage = travelRecordImage(300L, record, image);
        TravelRecordUpdateDto dto = new TravelRecordUpdateDto(100L, 1L, "updated title", "updated content", "2025-07-02T11:30:00", List.of(300L));
        when(travelRecordRepository.getReferenceById(100L)).thenReturn(record);
        when(travelRecordImageRepository.findWithTravelAndImageById(300L)).thenReturn(Optional.of(recordImage));

        recordService.updateTravelRecord(user, dto, null);

        verify(imageService).deleteImage(200L);
        assertThat(recordImage.getIsActive()).isFalse();
        verify(travelRecordImageRepository).save(recordImage);
    }

    // 수정 시 새 이미지를 추가하는 케이스다.
    // 이미 여행 대표 이미지가 있는 경우에는 saveImageByUrl을 다시 호출하지 않고 새 기록 이미지 매핑만 저장한다.
    @Test
    void updateTravelRecord_withImages_savesNewImageMappings() {
        User user = user(1L);
        Travel travel = travel(1L);
        travel.setTravelImage(image(999L, user, "existing.jpg", "https://image.example/existing.jpg"));
        TravelUser travelUser = travelUser(10L, travel, user, Role.WRITER);
        TravelRecord record = travelRecord(100L, travel, travelUser);
        Image uploadedImage = image(200L, user, "record.jpg", "https://image.example/record.jpg");
        TravelRecordImage savedRecordImage = travelRecordImage(300L, record, uploadedImage);
        TravelRecordUpdateDto dto = new TravelRecordUpdateDto(100L, 1L, "updated title", "updated content", "2025-07-02T11:30:00", List.of());
        when(travelRecordRepository.getReferenceById(100L)).thenReturn(record);
        when(imageService.saveImages(user, List.of(multipartFile))).thenReturn(List.of(uploadedImage));
        when(travelRecordImageRepository.save(org.mockito.ArgumentMatchers.any(TravelRecordImage.class))).thenReturn(savedRecordImage);

        TravelRecordResponseDto result = recordService.updateTravelRecord(user, dto, List.of(multipartFile));

        assertThat(result.images()).containsExactly(new TravelRecordImageDto(300L, "https://image.example/record.jpg"));
        verify(imageService).saveImages(user, List.of(multipartFile));
        verify(imageService, never()).saveImageByUrl(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString());
    }

    // 기록 이미지 하나를 삭제하는 정상 케이스다.
    // 원본 Image는 ImageService를 통해 삭제하고, TravelRecordImage 매핑은 isActive=false로 soft delete한다.
    @Test
    void deleteTravelRecordImage_existingImage_deletesImageAndSoftDeletesMapping() {
        User user = user(1L);
        Travel travel = travel(1L);
        TravelRecord record = travelRecord(100L, travel, travelUser(10L, travel, user, Role.WRITER));
        Image image = image(200L, user, "record.jpg", "https://image.example/record.jpg");
        TravelRecordImage recordImage = travelRecordImage(300L, record, image);
        when(travelRecordImageRepository.findWithTravelAndImageById(300L)).thenReturn(Optional.of(recordImage));

        recordService.deleteTravelRecordImage(300L);

        verify(imageService).deleteImage(200L);
        assertThat(recordImage.getIsActive()).isFalse();
        verify(travelRecordImageRepository).save(recordImage);
    }

    // 삭제하려는 기록 이미지 매핑을 찾지 못한 경우다.
    // 없는 이미지를 삭제했다고 성공 처리하지 않고 IllegalArgumentException을 던진다.
    @Test
    void deleteTravelRecordImage_missingImage_throwsIllegalArgumentException() {
        when(travelRecordImageRepository.findWithTravelAndImageById(300L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recordService.deleteTravelRecordImage(300L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // 여행 기록 삭제는 연결된 모든 활성 기록 이미지를 먼저 삭제한 뒤 기록 자체를 soft delete한다.
    // 이미지 삭제 흐름과 최종 TravelRecord 저장이 모두 수행되는지 확인한다.
    @Test
    void deleteTravelRecord_existingRecord_deletesImagesAndSoftDeletesRecord() {
        User user = user(1L);
        Travel travel = travel(1L);
        TravelRecord record = travelRecord(100L, travel, travelUser(10L, travel, user, Role.WRITER));
        Image firstImage = image(200L, user, "first.jpg", "https://image.example/first.jpg");
        Image secondImage = image(201L, user, "second.jpg", "https://image.example/second.jpg");
        TravelRecordImage firstRecordImage = travelRecordImage(300L, record, firstImage);
        TravelRecordImage secondRecordImage = travelRecordImage(301L, record, secondImage);
        when(travelRecordRepository.getReferenceById(100L)).thenReturn(record);
        when(travelRecordImageRepository.findAllByTravelRecordAndIsActiveTrue(record)).thenReturn(List.of(firstRecordImage, secondRecordImage));
        when(travelRecordImageRepository.findWithTravelAndImageById(300L)).thenReturn(Optional.of(firstRecordImage));
        when(travelRecordImageRepository.findWithTravelAndImageById(301L)).thenReturn(Optional.of(secondRecordImage));

        recordService.deleteTravelRecord(100L);

        verify(imageService).deleteImage(200L);
        verify(imageService).deleteImage(201L);
        assertThat(firstRecordImage.getIsActive()).isFalse();
        assertThat(secondRecordImage.getIsActive()).isFalse();
        assertThat(record.getIsActive()).isFalse();
        verify(travelRecordRepository).save(record);
    }

    private User user(Long userId) {
        return User.builder()
                .userId(userId)
                .email("user" + userId + "@example.com")
                .password("password")
                .name("User " + userId)
                .nickname("user" + userId)
                .gender(Gender.MALE)
                .birthday(LocalDate.of(2000, 1, 1))
                .build();
    }

    private Travel travel(Long travelId) {
        return Travel.builder()
                .travelId(travelId)
                .travelName("Tokyo")
                .numOfPeople(3L)
                .numOfJoinedPeople(1L)
                .nation(Nation.JAPAN)
                .sharedFund(0L)
                .build();
    }

    private TravelUser travelUser(Long travelUserId, Travel travel, User user, Role role) {
        return TravelUser.builder()
                .travelUserId(travelUserId)
                .travel(travel)
                .user(user)
                .role(role)
                .travelNickname("Traveler " + user.getUserId())
                .build();
    }

    private TravelRecord travelRecord(Long travelRecordId, Travel travel, TravelUser travelUser) {
        return TravelRecord.builder()
                .travelRecordId(travelRecordId)
                .travel(travel)
                .travelUser(travelUser)
                .title("title")
                .content("content")
                .recordTime(LocalDateTime.of(2025, 7, 1, 10, 30))
                .build();
    }

    private Image image(Long imageId, User user, String objectKey, String imageUrl) {
        return Image.builder()
                .imageId(imageId)
                .user(user)
                .objectKey(objectKey)
                .imageUrl(imageUrl)
                .build();
    }

    private TravelRecordImage travelRecordImage(Long imageId, TravelRecord record, Image image) {
        return TravelRecordImage.builder()
                .travelRecordImageId(imageId)
                .travelRecord(record)
                .image(image)
                .build();
    }
}
