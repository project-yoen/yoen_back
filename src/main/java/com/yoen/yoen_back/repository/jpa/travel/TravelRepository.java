package com.yoen.yoen_back.repository.jpa.travel;

import com.yoen.yoen_back.entity.travel.Travel;
import org.springframework.data.jpa.repository.JpaRepository;


public interface TravelRepository extends JpaRepository<Travel, Long> {

}
