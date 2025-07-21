package com.yoen.yoen_back.controller;

import com.yoen.yoen_back.dto.DestinationDto;
import com.yoen.yoen_back.entity.travel.Destination;
import com.yoen.yoen_back.service.TravelService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/destination")
public class DestinationController {
    private final TravelService travelService;

    @PostMapping("/set-destination")
    public List<Destination> createDestination(@RequestBody List<DestinationDto> destinations) {
        return travelService.createDestinations(destinations);
    }
}
