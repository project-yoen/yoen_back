package com.yoen.yoen_back.service;

import com.yoen.yoen_back.dto.travel.TravelNicknameUpdateDto;
import com.yoen.yoen_back.dto.travel.TravelProfileImageDto;
import com.yoen.yoen_back.dto.travel.TravelRequestDto;
import com.yoen.yoen_back.dto.travel.TravelResponseDto;
import com.yoen.yoen_back.dto.travel.TravelUserDto;
import com.yoen.yoen_back.dto.travel.TravelUserResponseDto;
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
import com.yoen.yoen_back.repository.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TravelServiceTest {

    // TravelService가 사용하는 Repository와 보조 서비스는 모두 Mock으로 둔다.
    // 덕분에 DB, 파일 업로드, 목적지 저장 로직 없이 TravelService 로직만 검증한다.
    @Mock
    private TravelRepository travelRepository;

    @Mock
    private TravelUserRepository travelUserRepository;

    @Mock
    private CommonService commonService;

    @Mock
    private TravelRecordRepository travelRecordRepository;

    @Mock
    private TravelRecordImageRepository travelRecordImageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ImageService imageService;

    // 위 Mock들을 TravelService 생성자에 주입한다.
    @InjectMocks
    private TravelService travelService;

    @Test
    @DisplayName("사용자가 참여 중인 여행 목록을 응답 DTO로 반환한다")
    void getAllTravelByUser_returnsTravelResponses() {
        User user = user(1L, "alice", "Alice");
        Travel travelWithImage = travel(10L, "도쿄 여행", 3L, 2L, 50000L);
        travelWithImage.setTravelImage(image(1L, "https://cdn.example.com/travel.png", "travel.png"));
        Travel travelWithoutImage = travel(11L, "서울 여행", 2L, 1L, 0L);
        // 사용자가 참여 중인 여행 목록을 Repository가 반환한다고 가정한다.
        when(travelUserRepository.findActiveTravelsByUser(user))
                .thenReturn(List.of(travelWithImage, travelWithoutImage));

        List<TravelResponseDto> responses = travelService.getAllTravelByUser(user);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).travelId()).isEqualTo(10L);
        assertThat(responses.get(0).travelImageUrl()).isEqualTo("https://cdn.example.com/travel.png");
        assertThat(responses.get(1).travelId()).isEqualTo(11L);
        assertThat(responses.get(1).travelImageUrl()).isEmpty();
    }

    @Test
    @DisplayName("여행을 생성하면 여행과 작성자 TravelUser를 저장하고 목적지를 매핑한다")
    void createTravel_savesTravelWriterAndDestinations() {
        User user = user(1L, "alice", "Alice");
        TravelRequestDto request = new TravelRequestDto(
                null,
                "도쿄 여행",
                4L,
                Nation.JAPAN,
                "2026-06-01",
                "2026-06-05",
                List.of(100L, 101L)
        );
        // 실제 DB가 없으므로 save()가 호출되면 ID가 채워진 엔티티처럼 돌려준다.
        when(travelRepository.save(any(Travel.class))).thenAnswer(invocation -> {
            Travel savedTravel = invocation.getArgument(0);
            savedTravel.setTravelId(10L);
            return savedTravel;
        });
        // 저장된 TravelUser가 작성자 권한으로 만들어졌는지 검증하기 위해 캡처한다.
        ArgumentCaptor<TravelUser> travelUserCaptor = ArgumentCaptor.forClass(TravelUser.class);

        TravelResponseDto response = travelService.createTravel(user, request);

        assertThat(response.travelId()).isEqualTo(10L);
        assertThat(response.travelName()).isEqualTo("도쿄 여행");
        assertThat(response.numOfPeople()).isEqualTo(4L);
        assertThat(response.numOfJoinedPeople()).isEqualTo(1L);
        assertThat(response.sharedFund()).isZero();
        assertThat(response.startDate()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(response.endDate()).isEqualTo(LocalDate.of(2026, 6, 5));

        verify(travelUserRepository).save(travelUserCaptor.capture());
        TravelUser savedTravelUser = travelUserCaptor.getValue();
        assertThat(savedTravelUser.getTravel().getTravelId()).isEqualTo(10L);
        assertThat(savedTravelUser.getUser()).isSameAs(user);
        assertThat(savedTravelUser.getRole()).isEqualTo(Role.WRITER);
        assertThat(savedTravelUser.getTravelNickname()).isEqualTo("Alice");
        verify(commonService).createTravelDestination(any(Travel.class), eq(List.of(100L, 101L)));
    }

    @Test
    @DisplayName("여행 정보를 수정하고 목적지를 다시 매핑한다")
    void updateTravel_updatesTravelAndDestinations() {
        Travel travel = travel(10L, "기존 여행", 2L, 1L, 0L);
        TravelRequestDto request = new TravelRequestDto(
                10L,
                "수정된 여행",
                5L,
                Nation.KOREA,
                "2026-07-01",
                "2026-07-03",
                List.of(200L)
        );
        // 수정 로직은 기존 Travel 엔티티를 가져와 필드를 바꾼 뒤 다시 저장한다.
        when(travelRepository.getReferenceById(10L)).thenReturn(travel);
        when(travelRepository.save(travel)).thenReturn(travel);

        Travel updatedTravel = travelService.updateTravel(request);

        assertThat(updatedTravel.getTravelName()).isEqualTo("수정된 여행");
        assertThat(updatedTravel.getNumOfPeople()).isEqualTo(5L);
        assertThat(updatedTravel.getNation()).isEqualTo(Nation.KOREA);
        assertThat(updatedTravel.getStartDate()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(updatedTravel.getEndDate()).isEqualTo(LocalDate.of(2026, 7, 3));
        verify(commonService).createTravelDestination(travel, List.of(200L));
        verify(travelRepository).save(travel);
    }

    @Test
    @DisplayName("여행 상세 정보를 조회한다")
    void getTravelDetail_returnsTravelResponse() {
        Travel travel = travel(10L, "도쿄 여행", 3L, 2L, 10000L);
        travel.setTravelImage(image(1L, "https://cdn.example.com/travel.png", "travel.png"));
        when(travelRepository.getReferenceById(10L)).thenReturn(travel);

        TravelResponseDto response = travelService.getTravelDetail(10L);

        assertThat(response.travelId()).isEqualTo(10L);
        assertThat(response.travelName()).isEqualTo("도쿄 여행");
        assertThat(response.travelImageUrl()).isEqualTo("https://cdn.example.com/travel.png");
    }

    @Test
    @DisplayName("현재 사용자의 TravelUser 정보를 조회한다")
    void getTravelUser_returnsTravelUserDto() {
        User user = user(1L, "alice", "Alice");
        Travel travel = travel(10L, "도쿄 여행", 3L, 1L, 0L);
        TravelUser travelUser = travelUser(100L, travel, user, Role.WRITER, "앨리스");
        // 현재 사용자와 여행으로 TravelUser를 찾을 수 있어야 정상 응답을 만든다.
        when(travelUserRepository.findByTravelAndUserAndIsActiveTrue(travel, user))
                .thenReturn(Optional.of(travelUser));

        TravelUserDto response = travelService.getTravelUser(user, travel);

        assertThat(response.travelUserId()).isEqualTo(100L);
        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.travelId()).isEqualTo(10L);
        assertThat(response.role()).isEqualTo(Role.WRITER);
        assertThat(response.travelNickname()).isEqualTo("앨리스");
    }

    @Test
    @DisplayName("현재 사용자의 TravelUser가 없으면 예외를 던진다")
    void getTravelUser_throwsException_whenTravelUserDoesNotExist() {
        User user = user(1L, "alice", "Alice");
        Travel travel = travel(10L, "도쿄 여행", 3L, 1L, 0L);
        when(travelUserRepository.findByTravelAndUserAndIsActiveTrue(travel, user))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> travelService.getTravelUser(user, travel))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("여행 닉네임을 수정한다")
    void updateTravelUserNickname_updatesNickname() {
        TravelUser travelUser = travelUser(100L, travel(10L, "도쿄 여행", 3L, 1L, 0L), user(1L, "alice", "Alice"), Role.READER, "기존닉");
        when(travelUserRepository.findByTravelUserIdAndIsActiveTrue(100L)).thenReturn(Optional.of(travelUser));

        travelService.updateTravelUserNickname(new TravelNicknameUpdateDto(100L, 10L, "새닉"));

        assertThat(travelUser.getTravelNickname()).isEqualTo("새닉");
        verify(travelUserRepository).save(travelUser);
    }

    @Test
    @DisplayName("여행 참여자 상세 목록을 사용자 닉네임 기준으로 정렬해 반환한다")
    void getDetailTravelUser_returnsSortedUserDetails() {
        Travel travel = travel(10L, "도쿄 여행", 3L, 2L, 0L);
        User charlie = user(2L, "charlie@example.com", "Charlie");
        User alice = user(1L, "alice@example.com", "Alice");
        alice.setProfileImage(image(9L, "https://cdn.example.com/alice.png", "alice.png"));
        TravelUser charlieTravelUser = travelUser(102L, travel, charlie, Role.READER, "찰리");
        TravelUser aliceTravelUser = travelUser(101L, travel, alice, Role.WRITER, "앨리스");
        // 일부러 Charlie, Alice 순서로 반환해서 서비스가 이름 기준 정렬을 하는지 검증한다.
        when(travelUserRepository.findByTravelAndIsActiveTrue(travel))
                .thenReturn(List.of(charlieTravelUser, aliceTravelUser));
        when(userRepository.getReferenceById(1L)).thenReturn(alice);
        when(userRepository.getReferenceById(2L)).thenReturn(charlie);

        List<TravelUserResponseDto> responses = travelService.getDetailTravelUser(travel);

        assertThat(responses).extracting(TravelUserResponseDto::nickName)
                .containsExactly("Alice", "Charlie");
        assertThat(responses.get(0).imageUrl()).isEqualTo("https://cdn.example.com/alice.png");
        assertThat(responses.get(1).imageUrl()).isEmpty();
    }

    @Test
    @DisplayName("정원 미만이면 참여 인원을 증가시킨다")
    void increaseNumOfJoinedPeople_increasesCount_whenUnderCapacity() {
        Travel travel = travel(10L, "도쿄 여행", 3L, 2L, 0L);

        Boolean result = travelService.increaseNumOfJoinedPeople(travel);

        assertThat(result).isTrue();
        assertThat(travel.getNumOfJoinedPeople()).isEqualTo(3L);
        verify(travelRepository).save(travel);
    }

    @Test
    @DisplayName("정원이 가득 차면 참여 인원을 증가시키지 않는다")
    void increaseNumOfJoinedPeople_returnsFalse_whenTravelIsFull() {
        Travel travel = travel(10L, "도쿄 여행", 3L, 3L, 0L);

        Boolean result = travelService.increaseNumOfJoinedPeople(travel);

        assertThat(result).isFalse();
        assertThat(travel.getNumOfJoinedPeople()).isEqualTo(3L);
        verify(travelRepository, never()).save(any());
    }

    @Test
    @DisplayName("참여 인원이 0보다 크면 감소시킨다")
    void decreaseNumOfJoinedPeople_decreasesCount_whenPositive() {
        Travel travel = travel(10L, "도쿄 여행", 3L, 1L, 0L);

        Boolean result = travelService.decreaseNumOfJoinedPeople(travel);

        assertThat(result).isTrue();
        assertThat(travel.getNumOfJoinedPeople()).isZero();
        verify(travelRepository).save(travel);
    }

    @Test
    @DisplayName("참여 인원이 0이면 감소시키지 않는다")
    void decreaseNumOfJoinedPeople_returnsFalse_whenCountIsZero() {
        Travel travel = travel(10L, "도쿄 여행", 3L, 0L, 0L);

        Boolean result = travelService.decreaseNumOfJoinedPeople(travel);

        assertThat(result).isFalse();
        assertThat(travel.getNumOfJoinedPeople()).isZero();
        verify(travelRepository, never()).save(any());
    }

    @Test
    @DisplayName("여행을 나가면 참여자 수를 줄이고 TravelUser를 비활성화한다")
    void leaveTravel_decreasesCountAndSoftDeletesTravelUser() {
        Travel travel = travel(10L, "도쿄 여행", 3L, 1L, 0L);
        TravelUser travelUser = travelUser(100L, travel, user(1L, "alice", "Alice"), Role.READER, "앨리스");

        travelService.leaveTravel(travelUser);

        assertThat(travel.getNumOfJoinedPeople()).isZero();
        assertThat(travelUser.getIsActive()).isFalse();
        verify(travelRepository).save(travel);
        verify(travelUserRepository).save(travelUser);
    }

    @Test
    @DisplayName("참여자 수를 줄일 수 없으면 여행 나가기에 실패한다")
    void leaveTravel_throwsException_whenCountCannotDecrease() {
        Travel travel = travel(10L, "도쿄 여행", 3L, 0L, 0L);
        TravelUser travelUser = travelUser(100L, travel, user(1L, "alice", "Alice"), Role.READER, "앨리스");

        assertThatThrownBy(() -> travelService.leaveTravel(travelUser))
                .isInstanceOf(IllegalStateException.class);
        verify(travelUserRepository, never()).save(any());
    }

    @Test
    @DisplayName("기록 이미지를 여행 대표 이미지로 설정한다")
    void updateTravelProfileImage_setsImageFromTravelRecordImage() {
        User user = user(1L, "alice", "Alice");
        Travel travel = travel(10L, "도쿄 여행", 3L, 1L, 0L);
        Image oldImage = image(1L, "https://cdn.example.com/old.png", "old.png");
        Image recordImage = image(2L, "https://cdn.example.com/record.png", "record.png");
        Image copiedImage = image(3L, "https://cdn.example.com/copied.png", "copied.png");
        // 기존 대표 이미지가 있으면 삭제하고, 기록 이미지 URL을 복사해 새 대표 이미지로 사용한다.
        travel.setTravelImage(oldImage);
        TravelRecordImage travelRecordImage = TravelRecordImage.builder()
                .travelRecord(TravelRecord.builder().travel(travel).build())
                .image(recordImage)
                .build();
        when(travelRecordImageRepository.findByTravelRecordImageIdAndIsActiveTrue(20L))
                .thenReturn(Optional.of(travelRecordImage));
        when(imageService.saveImageByUrl(user, "https://cdn.example.com/record.png")).thenReturn(copiedImage);

        travelService.updateTravelProfileImage(user, travel, new TravelProfileImageDto(10L, 20L), null);

        assertThat(travel.getTravelImage()).isSameAs(copiedImage);
        verify(imageService).deleteImage(oldImage);
        verify(imageService).saveImageByUrl(user, "https://cdn.example.com/record.png");
    }

    @Test
    @DisplayName("업로드 파일을 여행 대표 이미지로 설정한다")
    void updateTravelProfileImage_setsUploadedImage() {
        User user = user(1L, "alice", "Alice");
        Travel travel = travel(10L, "도쿄 여행", 3L, 1L, 0L);
        Image uploadedImage = image(2L, "https://cdn.example.com/uploaded.png", "uploaded.png");
        // 실제 업로드 파일을 만들지 않고 MultipartFile mock으로 이미지 저장 흐름만 검증한다.
        MultipartFile multipartFile = mock(MultipartFile.class);
        when(imageService.saveImage(user, multipartFile)).thenReturn(uploadedImage);

        travelService.updateTravelProfileImage(user, travel, new TravelProfileImageDto(10L, -1L), multipartFile);

        assertThat(travel.getTravelImage()).isSameAs(uploadedImage);
        verify(imageService).saveImage(user, multipartFile);
        verify(imageService, never()).deleteImage(any(Image.class));
    }

    private Travel travel(Long travelId, String travelName, Long numOfPeople, Long numOfJoinedPeople, Long sharedFund) {
        // 여러 테스트에서 재사용하는 기본 여행 fixture.
        return Travel.builder()
                .travelId(travelId)
                .travelName(travelName)
                .numOfPeople(numOfPeople)
                .numOfJoinedPeople(numOfJoinedPeople)
                .nation(Nation.JAPAN)
                .sharedFund(sharedFund)
                .startDate(LocalDate.of(2026, 6, 1))
                .endDate(LocalDate.of(2026, 6, 5))
                .build();
    }

    private TravelUser travelUser(Long travelUserId, Travel travel, User user, Role role, String travelNickname) {
        // 여행 참여자와 권한을 표현하는 fixture.
        return TravelUser.builder()
                .travelUserId(travelUserId)
                .travel(travel)
                .user(user)
                .role(role)
                .travelNickname(travelNickname)
                .build();
    }

    private User user(Long userId, String email, String nickname) {
        // TravelService 테스트에서는 인증/비밀번호가 중요하지 않아 최소 필드만 채운다.
        return User.builder()
                .userId(userId)
                .email(email)
                .password("encoded-password")
                .name(nickname)
                .nickname(nickname)
                .gender(Gender.MALE)
                .birthday(LocalDate.of(1998, 3, 12))
                .build();
    }

    private Image image(Long imageId, String imageUrl, String objectKey) {
        // 이미지가 연결된 상태를 표현하기 위한 fixture.
        return Image.builder()
                .imageId(imageId)
                .imageUrl(imageUrl)
                .objectKey(objectKey)
                .build();
    }
}
