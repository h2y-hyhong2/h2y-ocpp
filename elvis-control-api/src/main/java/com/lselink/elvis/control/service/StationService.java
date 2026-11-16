package com.lselink.elvis.control.service;

import com.lselink.elvis.control.common.PageResponse;
import com.lselink.elvis.control.dto.CorpResponseDto;
import com.lselink.elvis.control.dto.StationResponseDto;
import com.lselink.elvis.control.entity.CorpEntity;
import com.lselink.elvis.control.entity.StationEntity;
import com.lselink.elvis.control.repository.CorpRepository;
import com.lselink.elvis.control.repository.StationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class StationService {

    private final StationRepository stationRepository;
    private final CorpRepository corpRepository;

    public StationService(StationRepository stationRepository, CorpRepository corpRepository) {
        this.stationRepository = stationRepository;
        this.corpRepository = corpRepository;
    }

    public List<CorpResponseDto> getAllCorps() {
        List<CorpEntity> corps = corpRepository.findByUseYnOrderByCorpIdAsc("Y");
        List<CorpResponseDto> resultList = new ArrayList<>();
        for (CorpEntity c : corps) {
            CorpResponseDto dto = new CorpResponseDto(
                    c.getCorpId(),
                    c.getCorpName(),
                    c.getShortName(),
                    c.getTag(),
                    c.getColorClass(),
                    c.getBadge()
            );
            resultList.add(dto);
        }
        return resultList;
    }

    public List<StationResponseDto> getAllStations(String corpId) {
        List<StationEntity> stations;
        if (corpId != null && !corpId.equalsIgnoreCase("ALL")) {
            stations = stationRepository.findByCorpIdOrderByStIdAsc(corpId);
        } else {
            stations = stationRepository.findAllByOrderByStIdAsc();
        }

        List<CorpEntity> allCorps = corpRepository.findAll();
        Map<String, CorpEntity> corpMap = allCorps.stream()
                .collect(Collectors.toMap(CorpEntity::getCorpId, c -> c, (c1, c2) -> c1));

        List<StationResponseDto> dtoList = new ArrayList<>();
        for (int i = 0; i < stations.size(); i++) {
            StationEntity st = stations.get(i);
            dtoList.add(toStationResponseDto(st, corpMap.get(st.getCorpId()), i));
        }
        return dtoList;
    }

    public StationResponseDto getStationById(String stId) {
        StationEntity st = stationRepository.findById(stId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 충전소입니다: " + stId));

        CorpEntity corp = corpRepository.findById(st.getCorpId()).orElse(null);
        return toStationResponseDto(st, corp, 0);
    }

    public PageResponse<StationResponseDto> getPagedStations(String corpId, String keyword, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = (size <= 0) ? 10 : Math.min(size, 100);
        Pageable pageable = PageRequest.of(safePage, safeSize);

        Page<StationEntity> stationPage = stationRepository.searchStations(corpId, keyword, pageable);
        List<StationEntity> content = stationPage.getContent();

        if (content.isEmpty()) {
            return PageResponse.of(Collections.emptyList(), safePage, safeSize,
                    stationPage.getTotalElements(), stationPage.getTotalPages(), stationPage.isLast());
        }

        Set<String> corpIds = content.stream()
                .map(StationEntity::getCorpId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<String, CorpEntity> corpMap = corpRepository.findAllById(corpIds).stream()
                .collect(Collectors.toMap(CorpEntity::getCorpId, c -> c, (c1, c2) -> c1));

        int baseIndex = (int) pageable.getOffset();
        List<StationResponseDto> dtos = new ArrayList<>(content.size());
        for (int i = 0; i < content.size(); i++) {
            StationEntity st = content.get(i);
            dtos.add(toStationResponseDto(st, corpMap.get(st.getCorpId()), baseIndex + i));
        }

        return PageResponse.of(dtos, safePage, safeSize,
                stationPage.getTotalElements(), stationPage.getTotalPages(), stationPage.isLast());
    }

    private StationResponseDto toStationResponseDto(StationEntity st, CorpEntity corp, int startIdx) {
        String corpName = (corp != null && corp.getCorpName() != null) ? corp.getCorpName() : "";
        String corpShortName = (corp != null && corp.getShortName() != null) ? corp.getShortName() : "";

        return StationResponseDto.builder()
                .id(st.getStId())
                .name(st.getName())
                .corpId(st.getCorpId())
                .corpName(corpName)
                .corpShortName(corpShortName)
                .chargerCount(st.getChargerCount())
                .startIdx(startIdx)
                .build();
    }
}
