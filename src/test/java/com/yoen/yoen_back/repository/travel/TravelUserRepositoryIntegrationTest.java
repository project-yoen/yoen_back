package com.yoen.yoen_back.repository.travel;

import com.yoen.yoen_back.entity.travel.Travel;
import com.yoen.yoen_back.entity.travel.TravelUser;
import com.yoen.yoen_back.entity.user.User;
import com.yoen.yoen_back.enums.Gender;
import com.yoen.yoen_back.enums.Nation;
import com.yoen.yoen_back.enums.Role;
import com.yoen.yoen_back.repository.user.UserRepository;
import com.yoen.yoen_back.support.PostgresIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TravelUserRepositoryIntegrationTest extends PostgresIntegrationTestSupport {

    @Autowired
    private TravelUserRepository travelUserRepository;

    @Autowired
    private TravelRepository travelRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void findByTravel_TravelId_existingTravelUsers_returnsAllTravelUsersForTravel() {
        Travel targetTravel = saveTravel("Tokyo", true);
        Travel otherTravel = saveTravel("Osaka", true);
        TravelUser activeTravelUser = saveTravelUser(targetTravel, saveUser("active"), Role.WRITER, true);
        TravelUser inactiveTravelUser = saveTravelUser(targetTravel, saveUser("inactive"), Role.READER, false);
        saveTravelUser(otherTravel, saveUser("other"), Role.READER, true);

        List<TravelUser> result = travelUserRepository.findByTravel_TravelId(targetTravel.getTravelId());

        assertThat(result)
                .extracting(TravelUser::getTravelUserId)
                .containsExactlyInAnyOrder(activeTravelUser.getTravelUserId(), inactiveTravelUser.getTravelUserId());
    }

    @Test
    void findByTravel_TravelIdAndUserAndIsActiveTrue_activeTravelUser_returnsTravelUser() {
        Travel travel = saveTravel("Tokyo", true);
        User user = saveUser("member");
        TravelUser travelUser = saveTravelUser(travel, user, Role.WRITER, true);

        Optional<TravelUser> result = travelUserRepository.findByTravel_TravelIdAndUserAndIsActiveTrue(travel.getTravelId(), user);

        assertThat(result).isPresent();
        assertThat(result.get().getTravelUserId()).isEqualTo(travelUser.getTravelUserId());
    }

    @Test
    void findByTravel_TravelIdAndUserAndIsActiveTrue_inactiveTravelUser_returnsEmpty() {
        Travel travel = saveTravel("Tokyo", true);
        User user = saveUser("member");
        saveTravelUser(travel, user, Role.READER, false);

        Optional<TravelUser> result = travelUserRepository.findByTravel_TravelIdAndUserAndIsActiveTrue(travel.getTravelId(), user);

        assertThat(result).isEmpty();
    }

    @Test
    void findByTravelAndIsActiveTrue_existingTravelUsers_returnsOnlyActiveTravelUsers() {
        Travel travel = saveTravel("Tokyo", true);
        TravelUser activeTravelUser = saveTravelUser(travel, saveUser("active"), Role.WRITER, true);
        saveTravelUser(travel, saveUser("inactive"), Role.READER, false);

        List<TravelUser> result = travelUserRepository.findByTravelAndIsActiveTrue(travel);

        assertThat(result)
                .extracting(TravelUser::getTravelUserId)
                .containsExactly(activeTravelUser.getTravelUserId());
    }

    @Test
    void findByTravelUserIdAndIsActiveTrue_activeTravelUser_returnsTravelUser() {
        Travel travel = saveTravel("Tokyo", true);
        TravelUser travelUser = saveTravelUser(travel, saveUser("member"), Role.WRITER, true);

        Optional<TravelUser> result = travelUserRepository.findByTravelUserIdAndIsActiveTrue(travelUser.getTravelUserId());

        assertThat(result).isPresent();
        assertThat(result.get().getTravelUserId()).isEqualTo(travelUser.getTravelUserId());
    }

    @Test
    void findByTravelUserIdAndIsActiveTrue_inactiveTravelUser_returnsEmpty() {
        Travel travel = saveTravel("Tokyo", true);
        TravelUser travelUser = saveTravelUser(travel, saveUser("member"), Role.READER, false);

        Optional<TravelUser> result = travelUserRepository.findByTravelUserIdAndIsActiveTrue(travelUser.getTravelUserId());

        assertThat(result).isEmpty();
    }

    @Test
    void findByTravelAndUserAndIsActiveTrue_activeTravelUser_returnsTravelUser() {
        Travel travel = saveTravel("Tokyo", true);
        User user = saveUser("member");
        TravelUser travelUser = saveTravelUser(travel, user, Role.WRITER, true);

        Optional<TravelUser> result = travelUserRepository.findByTravelAndUserAndIsActiveTrue(travel, user);

        assertThat(result).isPresent();
        assertThat(result.get().getTravelUserId()).isEqualTo(travelUser.getTravelUserId());
    }

    @Test
    void findByTravelAndUserAndIsActiveTrue_differentUser_returnsEmpty() {
        Travel travel = saveTravel("Tokyo", true);
        saveTravelUser(travel, saveUser("member"), Role.READER, true);
        User otherUser = saveUser("other");

        Optional<TravelUser> result = travelUserRepository.findByTravelAndUserAndIsActiveTrue(travel, otherUser);

        assertThat(result).isEmpty();
    }

    @Test
    void findActiveTravelsByUser_mixedTravelAndMembershipStates_returnsOnlyActiveTravelsForActiveMemberships() {
        User targetUser = saveUser("target");
        User otherUser = saveUser("other");
        Travel activeTravel = saveTravel("Tokyo", true);
        Travel inactiveTravel = saveTravel("Osaka", false);
        Travel inactiveMembershipTravel = saveTravel("Kyoto", true);
        Travel otherUserTravel = saveTravel("Sapporo", true);
        saveTravelUser(activeTravel, targetUser, Role.WRITER, true);
        saveTravelUser(inactiveTravel, targetUser, Role.READER, true);
        saveTravelUser(inactiveMembershipTravel, targetUser, Role.READER, false);
        saveTravelUser(otherUserTravel, otherUser, Role.READER, true);

        List<Travel> result = travelUserRepository.findActiveTravelsByUser(targetUser);

        assertThat(result)
                .extracting(Travel::getTravelId)
                .containsExactly(activeTravel.getTravelId());
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

    private Travel saveTravel(String travelName, boolean active) {
        Travel travel = Travel.builder()
                .travelName(travelName)
                .numOfPeople(3L)
                .numOfJoinedPeople(1L)
                .nation(Nation.JAPAN)
                .sharedFund(0L)
                .startDate(LocalDate.of(2025, 7, 1))
                .endDate(LocalDate.of(2025, 7, 3))
                .build();
        travel.setIsActive(active);
        return travelRepository.saveAndFlush(travel);
    }

    private TravelUser saveTravelUser(Travel travel, User user, Role role, boolean active) {
        TravelUser travelUser = TravelUser.builder()
                .travel(travel)
                .user(user)
                .role(role)
                .travelNickname(user.getNickname())
                .build();
        travelUser.setIsActive(active);
        return travelUserRepository.saveAndFlush(travelUser);
    }
}
