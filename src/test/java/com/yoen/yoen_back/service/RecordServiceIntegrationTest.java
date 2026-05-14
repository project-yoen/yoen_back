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
import com.yoen.yoen_back.repository.image.ImageRepository;
import com.yoen.yoen_back.repository.image.TravelRecordImageRepository;
import com.yoen.yoen_back.repository.travel.TravelRecordRepository;
import com.yoen.yoen_back.repository.travel.TravelRepository;
import com.yoen.yoen_back.repository.travel.TravelUserRepository;
import com.yoen.yoen_back.repository.user.UserRepository;
import com.yoen.yoen_back.support.PostgresIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(RecordService.class)
class RecordServiceIntegrationTest extends PostgresIntegrationTestSupport {

    @Autowired
    private RecordService recordService;

    @Autowired
    private TravelRecordRepository travelRecordRepository;

    @Autowired
    private TravelRecordImageRepository travelRecordImageRepository;

    @Autowired
    private TravelRepository travelRepository;

    @Autowired
    private TravelUserRepository travelUserRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ImageRepository imageRepository;

    @MockitoBean
    private ImageService imageService;

    @Test
    void createTravelRecord_withoutImages_savesRecordAndReturnsEmptyImages() {
        User user = saveUser("writer");
        Travel travel = saveTravel("Tokyo Trip");
        TravelUser travelUser = saveTravelUser(travel, user, Role.WRITER, "writer-nickname");
        TravelRecordRequestDto request = new TravelRecordRequestDto(null, travel.getTravelId(), "First day", "Arrived", "2025-07-01T10:30:00");

        TravelRecordResponseDto response = recordService.createTravelRecord(user, request, null);

        TravelRecord savedRecord = travelRecordRepository.findById(response.travelRecordId()).orElseThrow();
        assertThat(savedRecord.getTravel().getTravelId()).isEqualTo(travel.getTravelId());
        assertThat(savedRecord.getTravelUser().getTravelUserId()).isEqualTo(travelUser.getTravelUserId());
        assertThat(savedRecord.getTitle()).isEqualTo("First day");
        assertThat(savedRecord.getContent()).isEqualTo("Arrived");
        assertThat(savedRecord.getRecordTime()).isEqualTo(LocalDateTime.of(2025, 7, 1, 10, 30));
        assertThat(response.travelNickName()).isEqualTo("writer-nickname");
        assertThat(response.images()).isEmpty();
    }

    @Test
    void createTravelRecord_withImages_savesRecordImageMappingsAndSetsTravelImage() {
        User user = saveUser("writer");
        Travel travel = saveTravel("Tokyo Trip");
        saveTravelUser(travel, user, Role.WRITER, "writer-nickname");
        Image uploadedImage = saveImage(user, "record.jpg", "https://image.example/record.jpg");
        Image travelImage = saveImage(user, "travel.jpg", "https://image.example/travel.jpg");
        MultipartFile file = new MockMultipartFile("images", "record.jpg", "image/jpeg", "image".getBytes());
        TravelRecordRequestDto request = new TravelRecordRequestDto(null, travel.getTravelId(), "First day", "Arrived", "2025-07-01T10:30:00");
        when(imageService.saveImages(user, List.of(file))).thenReturn(List.of(uploadedImage));
        when(imageService.saveImageByUrl(user, "https://image.example/record.jpg")).thenReturn(travelImage);

        TravelRecordResponseDto response = recordService.createTravelRecord(user, request, List.of(file));

        TravelRecord savedRecord = travelRecordRepository.findById(response.travelRecordId()).orElseThrow();
        Travel savedTravel = travelRepository.findById(travel.getTravelId()).orElseThrow();
        List<TravelRecordImage> savedImages = travelRecordImageRepository.findAllByTravelRecordAndIsActiveTrue(savedRecord);
        assertThat(response.images()).containsExactly(new TravelRecordImageDto(savedImages.get(0).getTravelRecordImageId(), "https://image.example/record.jpg"));
        assertThat(savedImages).hasSize(1);
        assertThat(savedImages.get(0).getImage().getImageId()).isEqualTo(uploadedImage.getImageId());
        assertThat(savedTravel.getTravelImage().getImageId()).isEqualTo(travelImage.getImageId());
    }

    @Test
    void getTravelRecordsByDate_existingRecords_returnsRecordsWithImagesForDate() {
        User user = saveUser("writer");
        Travel travel = saveTravel("Tokyo Trip");
        TravelUser travelUser = saveTravelUser(travel, user, Role.WRITER, "writer-nickname");
        TravelRecord targetRecord = saveTravelRecord(travel, travelUser, "Target", "content", LocalDateTime.of(2025, 7, 1, 10, 30), true);
        saveTravelRecord(travel, travelUser, "Other day", "content", LocalDateTime.of(2025, 7, 2, 10, 30), true);
        Image image = saveImage(user, "record.jpg", "https://image.example/record.jpg");
        TravelRecordImage recordImage = saveTravelRecordImage(targetRecord, image, true);

        List<TravelRecordResponseDto> response = recordService.getTravelRecordsByDate(travel, "2025-07-01T12:00:00");

        assertThat(response).hasSize(1);
        assertThat(response.get(0).travelRecordId()).isEqualTo(targetRecord.getTravelRecordId());
        assertThat(response.get(0).travelNickName()).isEqualTo("writer-nickname");
        assertThat(response.get(0).images()).containsExactly(new TravelRecordImageDto(recordImage.getTravelRecordImageId(), "https://image.example/record.jpg"));
    }

    @Test
    void updateTravelRecord_withoutImages_updatesRecordFields() {
        User user = saveUser("writer");
        Travel travel = saveTravel("Tokyo Trip");
        TravelUser travelUser = saveTravelUser(travel, user, Role.WRITER, "writer-nickname");
        TravelRecord record = saveTravelRecord(travel, travelUser, "Before", "before content", LocalDateTime.of(2025, 7, 1, 10, 30), true);
        TravelRecordUpdateDto request = new TravelRecordUpdateDto(record.getTravelRecordId(), travel.getTravelId(), "After", "after content", "2025-07-02T11:30:00", List.of());

        TravelRecordResponseDto response = recordService.updateTravelRecord(user, request, null);

        TravelRecord savedRecord = travelRecordRepository.findById(record.getTravelRecordId()).orElseThrow();
        assertThat(savedRecord.getTitle()).isEqualTo("After");
        assertThat(savedRecord.getContent()).isEqualTo("after content");
        assertThat(savedRecord.getRecordTime()).isEqualTo(LocalDateTime.of(2025, 7, 2, 11, 30));
        assertThat(response.images()).isEmpty();
    }

    @Test
    void updateTravelRecord_withRemovedImage_softDeletesRequestedImageMapping() {
        User user = saveUser("writer");
        Travel travel = saveTravel("Tokyo Trip");
        setTravelImage(travel, saveImage(user, "travel.jpg", "https://image.example/travel.jpg"));
        TravelUser travelUser = saveTravelUser(travel, user, Role.WRITER, "writer-nickname");
        TravelRecord record = saveTravelRecord(travel, travelUser, "Before", "before content", LocalDateTime.of(2025, 7, 1, 10, 30), true);
        Image image = saveImage(user, "record.jpg", "https://image.example/record.jpg");
        TravelRecordImage recordImage = saveTravelRecordImage(record, image, true);
        TravelRecordUpdateDto request = new TravelRecordUpdateDto(record.getTravelRecordId(), travel.getTravelId(), "After", "after content", "2025-07-02T11:30:00", List.of(recordImage.getTravelRecordImageId()));

        recordService.updateTravelRecord(user, request, null);

        TravelRecordImage savedRecordImage = travelRecordImageRepository.findById(recordImage.getTravelRecordImageId()).orElseThrow();
        assertThat(savedRecordImage.getIsActive()).isFalse();
        verify(imageService).deleteImage(image.getImageId());
    }

    @Test
    void updateTravelRecord_withImages_savesAdditionalImageMappings() {
        User user = saveUser("writer");
        Travel travel = saveTravel("Tokyo Trip");
        setTravelImage(travel, saveImage(user, "travel.jpg", "https://image.example/travel.jpg"));
        TravelUser travelUser = saveTravelUser(travel, user, Role.WRITER, "writer-nickname");
        TravelRecord record = saveTravelRecord(travel, travelUser, "Before", "before content", LocalDateTime.of(2025, 7, 1, 10, 30), true);
        Image uploadedImage = saveImage(user, "new-record.jpg", "https://image.example/new-record.jpg");
        MultipartFile file = new MockMultipartFile("images", "new-record.jpg", "image/jpeg", "image".getBytes());
        TravelRecordUpdateDto request = new TravelRecordUpdateDto(record.getTravelRecordId(), travel.getTravelId(), "After", "after content", "2025-07-02T11:30:00", List.of());
        when(imageService.saveImages(user, List.of(file))).thenReturn(List.of(uploadedImage));

        TravelRecordResponseDto response = recordService.updateTravelRecord(user, request, List.of(file));

        List<TravelRecordImage> savedImages = travelRecordImageRepository.findAllByTravelRecordAndIsActiveTrue(record);
        assertThat(savedImages).hasSize(1);
        assertThat(savedImages.get(0).getImage().getImageId()).isEqualTo(uploadedImage.getImageId());
        assertThat(response.images()).containsExactly(new TravelRecordImageDto(savedImages.get(0).getTravelRecordImageId(), "https://image.example/new-record.jpg"));
    }

    @Test
    void deleteTravelRecord_existingRecord_softDeletesRecordAndImages() {
        User user = saveUser("writer");
        Travel travel = saveTravel("Tokyo Trip");
        setTravelImage(travel, saveImage(user, "travel.jpg", "https://image.example/travel.jpg"));
        TravelUser travelUser = saveTravelUser(travel, user, Role.WRITER, "writer-nickname");
        TravelRecord record = saveTravelRecord(travel, travelUser, "Before", "before content", LocalDateTime.of(2025, 7, 1, 10, 30), true);
        Image firstImage = saveImage(user, "first.jpg", "https://image.example/first.jpg");
        Image secondImage = saveImage(user, "second.jpg", "https://image.example/second.jpg");
        TravelRecordImage firstRecordImage = saveTravelRecordImage(record, firstImage, true);
        TravelRecordImage secondRecordImage = saveTravelRecordImage(record, secondImage, true);

        recordService.deleteTravelRecord(record.getTravelRecordId());

        TravelRecord savedRecord = travelRecordRepository.findById(record.getTravelRecordId()).orElseThrow();
        TravelRecordImage savedFirstImage = travelRecordImageRepository.findById(firstRecordImage.getTravelRecordImageId()).orElseThrow();
        TravelRecordImage savedSecondImage = travelRecordImageRepository.findById(secondRecordImage.getTravelRecordImageId()).orElseThrow();
        assertThat(savedRecord.getIsActive()).isFalse();
        assertThat(savedFirstImage.getIsActive()).isFalse();
        assertThat(savedSecondImage.getIsActive()).isFalse();
        verify(imageService).deleteImage(firstImage.getImageId());
        verify(imageService).deleteImage(secondImage.getImageId());
    }

    private User saveUser(String suffix) {
        User user = User.builder()
                .email(suffix + "@example.com")
                .password("password")
                .name("User " + suffix)
                .nickname("user-" + suffix)
                .gender(Gender.MALE)
                .birthday(LocalDate.of(2000, 1, 1))
                .build();
        return userRepository.saveAndFlush(user);
    }

    private Travel saveTravel(String travelName) {
        Travel travel = Travel.builder()
                .travelName(travelName)
                .numOfPeople(3L)
                .numOfJoinedPeople(1L)
                .nation(Nation.JAPAN)
                .sharedFund(0L)
                .startDate(LocalDate.of(2025, 7, 1))
                .endDate(LocalDate.of(2025, 7, 3))
                .build();
        return travelRepository.saveAndFlush(travel);
    }

    private TravelUser saveTravelUser(Travel travel, User user, Role role, String travelNickname) {
        TravelUser travelUser = TravelUser.builder()
                .travel(travel)
                .user(user)
                .role(role)
                .travelNickname(travelNickname)
                .build();
        return travelUserRepository.saveAndFlush(travelUser);
    }

    private TravelRecord saveTravelRecord(Travel travel, TravelUser travelUser, String title, String content, LocalDateTime recordTime, boolean active) {
        TravelRecord travelRecord = TravelRecord.builder()
                .travel(travel)
                .travelUser(travelUser)
                .title(title)
                .content(content)
                .recordTime(recordTime)
                .build();
        travelRecord.setIsActive(active);
        return travelRecordRepository.saveAndFlush(travelRecord);
    }

    private Image saveImage(User user, String objectKey, String imageUrl) {
        Image image = Image.builder()
                .user(user)
                .objectKey(objectKey)
                .imageUrl(imageUrl)
                .build();
        return imageRepository.saveAndFlush(image);
    }

    private TravelRecordImage saveTravelRecordImage(TravelRecord record, Image image, boolean active) {
        TravelRecordImage travelRecordImage = TravelRecordImage.builder()
                .travelRecord(record)
                .image(image)
                .build();
        travelRecordImage.setIsActive(active);
        return travelRecordImageRepository.saveAndFlush(travelRecordImage);
    }

    private void setTravelImage(Travel travel, Image image) {
        travel.setTravelImage(image);
        travelRepository.saveAndFlush(travel);
    }
}
