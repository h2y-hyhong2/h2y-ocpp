package com.lselink.elvis.control.service;

import com.lselink.elvis.control.dto.StationResponseDto;
import com.lselink.elvis.control.entity.CorpEntity;
import com.lselink.elvis.control.entity.StationEntity;
import com.lselink.elvis.control.repository.CorpRepository;
import com.lselink.elvis.control.repository.StationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StationService {

    private final StationRepository stationRepository;
    private final CorpRepository corpRepository;

    public List<CorpEntity> getAllCorps() {
        return corpRepository.findByUseYnOrderByCorpIdAsc("Y");
    }

    public List<StationResponseDto> getAllStations(String corpId) {
        List<StationEntity> stations = (corpId != null && !corpId.equalsIgnoreCase("ALL"))
                ? stationRepository.findByCorpIdOrderByStIdAsc(corpId)
                : stationRepository.findAllByOrderByStIdAsc();

        Map<String, CorpEntity> corpMap = corpRepository.findAll().stream()
                .collect(Collectors.toMap(CorpEntity::getCorpId, c -> c, (c1, c2) -> c1));

        return IntStream.range(0, stations.size()).mapToObj(i -> {
            StationEntity st = stations.get(i);
            CorpEntity corp = corpMap.get(st.getCorpId());
            return StationResponseDto.builder()
                    .id(st.getStId())
                    .name(st.getName())
                    .corpId(st.getCorpId())
                    .corpName(corp != null ? corp.getCorpName() : "")
                    .corpShortName(corp != null ? corp.getShortName() : "")
                    .chargerCount(st.getChargerCount())
                    .startIdx(i)
                    .build();
        }).collect(Collectors.toList());
    }
}
