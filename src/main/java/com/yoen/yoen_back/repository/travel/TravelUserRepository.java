package com.yoen.yoen_back.repository.travel;

import com.yoen.yoen_back.entity.travel.Travel;
import com.yoen.yoen_back.entity.travel.TravelUser;
import com.yoen.yoen_back.entity.user.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TravelUserRepository extends JpaRepository<TravelUser, Long> {
    List<TravelUser> findByTravel_TravelId(Long travelId);
    Optional<TravelUser> findByTravel_TravelIdAndUserAndIsActiveTrue(Long travelId, User user);

    List<TravelUser> findByTravelAndIsActiveTrue(Travel tv);

    // 참여자 상세 목록: user와 프로필 이미지까지 한 번에 fetch (유저별 개별 조회 N+1 제거)
    @EntityGraph(attributePaths = {"user", "user.profileImage"})
    List<TravelUser> findWithUserByTravelAndIsActiveTrue(Travel tv);

    Optional<TravelUser> findByTravelUserIdAndIsActiveTrue(Long travelUserId);

    Optional<TravelUser> findByTravelAndUserAndIsActiveTrue(Travel travel, User user);

    @Query("SELECT t FROM TravelUser tu JOIN tu.travel t LEFT JOIN FETCH t.travelImage WHERE tu.user = :user AND t.isActive = true AND tu.isActive = true")
    List<Travel> findActiveTravelsByUser(@Param("user") User user);
}
