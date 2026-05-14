package com.yoen.yoen_back.service;

import com.yoen.yoen_back.dto.travel.TravelNicknameUpdateDto;
import com.yoen.yoen_back.dto.travel.TravelRequestDto;
import com.yoen.yoen_back.dto.travel.TravelResponseDto;
import com.yoen.yoen_back.dto.travel.TravelUserDto;
import com.yoen.yoen_back.entity.travel.Travel;
import com.yoen.yoen_back.entity.travel.TravelUser;
import com.yoen.yoen_back.entity.user.User;
import com.yoen.yoen_back.enums.Gender;
import com.yoen.yoen_back.enums.Nation;
import com.yoen.yoen_back.enums.Role;
import com.yoen.yoen_back.repository.travel.TravelRepository;
import com.yoen.yoen_back.repository.travel.TravelUserRepository;
import com.yoen.yoen_back.repository.user.UserRepository;
import com.yoen.yoen_back.support.PostgresIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TravelService.class)
class TravelServiceIntegrationTest extends PostgresIntegrationTestSupport {

    @Autowired
    private TravelService travelService;

    @Autowired
    private TravelRepository travelRepository;

    @Autowired
    private TravelUserRepository travelUserRepository;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private CommonService commonService;

    @MockitoBean
    private ImageService imageService;

    @Test
    void createTravel_validRequest_savesTravelAndWriterTravelUser() {
        User writer = saveUser("writer");
        TravelRequestDto request = new TravelRequestDto(
                null,
                "Tokyo Trip",
                3L,
                Nation.JAPAN,
                "2025-07-01",
                "2025-07-03",
                List.of(10L, 11L)
        );

        TravelResponseDto response = travelService.createTravel(writer, request);

        Travel savedTravel = travelRepository.findById(response.travelId()).orElseThrow();
        Optional<TravelUser> savedTravelUser = travelUserRepository.findByTravelAndUserAndIsActiveTrue(savedTravel, writer);
        assertThat(savedTravel.getTravelName()).isEqualTo("Tokyo Trip");
        assertThat(savedTravel.getNumOfPeople()).isEqualTo(3L);
        assertThat(savedTravel.getNumOfJoinedPeople()).isEqualTo(1L);
        assertThat(savedTravel.getSharedFund()).isZero();
        assertThat(savedTravel.getNation()).isEqualTo(Nation.JAPAN);
        assertThat(savedTravel.getStartDate()).isEqualTo(LocalDate.of(2025, 7, 1));
        assertThat(savedTravel.getEndDate()).isEqualTo(LocalDate.of(2025, 7, 3));
        assertThat(savedTravelUser).isPresent();
        assertThat(savedTravelUser.get().getRole()).isEqualTo(Role.WRITER);
        assertThat(savedTravelUser.get().getTravelNickname()).isEqualTo(writer.getNickname());
        verify(commonService).createTravelDestination(savedTravel, List.of(10L, 11L));
    }

    @Test
    void getAllTravelUser_activeAndInactiveMembers_returnsOnlyActiveMembers() {
        Travel travel = saveTravel("Tokyo Trip", 3L, 2L);
        TravelUser activeTravelUser = saveTravelUser(travel, saveUser("active"), Role.WRITER, "active-nickname", true);
        saveTravelUser(travel, saveUser("inactive"), Role.READER, "inactive-nickname", false);

        List<TravelUserDto> result = travelService.getAllTravelUser(travel);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).travelUserId()).isEqualTo(activeTravelUser.getTravelUserId());
        assertThat(result.get(0).travelNickname()).isEqualTo("active-nickname");
    }

    @Test
    void increaseNumOfJoinedPeople_availableCapacity_increasesPersistedCount() {
        Travel travel = saveTravel("Tokyo Trip", 3L, 1L);

        Boolean result = travelService.increaseNumOfJoinedPeople(travel);

        Travel savedTravel = travelRepository.findById(travel.getTravelId()).orElseThrow();
        assertThat(result).isTrue();
        assertThat(savedTravel.getNumOfJoinedPeople()).isEqualTo(2L);
    }

    @Test
    void increaseNumOfJoinedPeople_fullCapacity_doesNotChangeCount() {
        Travel travel = saveTravel("Tokyo Trip", 2L, 2L);

        Boolean result = travelService.increaseNumOfJoinedPeople(travel);

        Travel savedTravel = travelRepository.findById(travel.getTravelId()).orElseThrow();
        assertThat(result).isFalse();
        assertThat(savedTravel.getNumOfJoinedPeople()).isEqualTo(2L);
    }

    @Test
    void updateTravelUserNickname_existingTravelUser_updatesNickname() {
        Travel travel = saveTravel("Tokyo Trip", 3L, 1L);
        TravelUser travelUser = saveTravelUser(travel, saveUser("member"), Role.READER, "old-nickname", true);

        travelService.updateTravelUserNickname(new TravelNicknameUpdateDto(travelUser.getTravelUserId(), travel.getTravelId(), "new-nickname"));

        TravelUser savedTravelUser = travelUserRepository.findById(travelUser.getTravelUserId()).orElseThrow();
        assertThat(savedTravelUser.getTravelNickname()).isEqualTo("new-nickname");
    }

    @Test
    void leaveTravel_activeTravelUser_decreasesCountAndSoftDeletesTravelUser() {
        Travel travel = saveTravel("Tokyo Trip", 3L, 2L);
        TravelUser travelUser = saveTravelUser(travel, saveUser("member"), Role.READER, "member-nickname", true);

        travelService.leaveTravel(travelUser);

        Travel savedTravel = travelRepository.findById(travel.getTravelId()).orElseThrow();
        TravelUser savedTravelUser = travelUserRepository.findById(travelUser.getTravelUserId()).orElseThrow();
        assertThat(savedTravel.getNumOfJoinedPeople()).isEqualTo(1L);
        assertThat(savedTravelUser.getIsActive()).isFalse();
        assertThat(travelUserRepository.findByTravelAndUserAndIsActiveTrue(travel, travelUser.getUser())).isEmpty();
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

    private Travel saveTravel(String travelName, Long numOfPeople, Long numOfJoinedPeople) {
        Travel travel = Travel.builder()
                .travelName(travelName)
                .numOfPeople(numOfPeople)
                .numOfJoinedPeople(numOfJoinedPeople)
                .nation(Nation.JAPAN)
                .sharedFund(0L)
                .startDate(LocalDate.of(2025, 7, 1))
                .endDate(LocalDate.of(2025, 7, 3))
                .build();
        return travelRepository.saveAndFlush(travel);
    }

    private TravelUser saveTravelUser(Travel travel, User user, Role role, String travelNickname, boolean active) {
        TravelUser travelUser = TravelUser.builder()
                .travel(travel)
                .user(user)
                .role(role)
                .travelNickname(travelNickname)
                .build();
        travelUser.setIsActive(active);
        return travelUserRepository.saveAndFlush(travelUser);
    }
}
