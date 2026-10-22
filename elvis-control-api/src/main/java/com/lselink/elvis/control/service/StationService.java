package com.lselink.elvis.control.service;

import com.lselink.elvis.control.common.PageResponse;
import com.lselink.elvis.control.dto.CorpResponseDto;
import com.lselink.elvis.control.dto.StationResponseDto;
import com.lselink.elvis.control.entity.CorpEntity;
import com.lselink.elvis.control.entity.StationEntity;
import com.lselink.elvis.control.repository.CorpRepository;
import com.lselink.elvis.control.repository.StationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
            CorpEntity corp = corpMap.get(st.getCorpId());

            String corpName = "";
            String corpShortName = "";
            if (corp != null) {
                corpName = corp.getCorpName();
                corpShortName = corp.getShortName();
            }

            StationResponseDto dto = StationResponseDto.builder()
                    .id(st.getStId())
                    .name(st.getName())
                    .corpId(st.getCorpId())
                    .corpName(corpName)
                    .corpShortName(corpShortName)
                    .chargerCount(st.getChargerCount())
                    .startIdx(i)
                    .build();
            dtoList.add(dto);
        }
        return dtoList;
    }

    public StationResponseDto getStationById(String stId) {
        StationEntity st = stationRepository.findById(stId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 충전소입니다: " + stId));

        CorpEntity corp = corpRepository.findById(st.getCorpId()).orElse(null);
        String corpName = "";
        String corpShortName = "";
        if (corp != null) {
            corpName = corp.getCorpName();
            corpShortName = corp.getShortName();
        }

        return StationResponseDto.builder()
                .id(st.getStId())
                .name(st.getName())
                .corpId(st.getCorpId())
                .corpName(corpName)
                .corpShortName(corpShortName)
                .chargerCount(st.getChargerCount())
                .startIdx(0)
                .build();
    }

    public PageResponse<StationResponseDto> getPagedStations(String corpId, String keyword, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = (size <= 0) ? 10 : size;

        List<StationResponseDto> all = getAllStations(corpId);

        List<StationResponseDto> filtered = new ArrayList<>();
        String trimmedKeyword = (keyword != null) ? keyword.trim().toLowerCase() : "";

        for (StationResponseDto st : all) {
            if (trimmedKeyword.isEmpty()) {
                filtered.add(st);
            } else {
                boolean matchName = st.getName() != null && st.getName().toLowerCase().contains(trimmedKeyword);
                boolean matchId = st.getId() != null && st.getId().toLowerCase().contains(trimmedKeyword);
                boolean matchCorp = st.getCorpName() != null && st.getCorpName().toLowerCase().contains(trimmedKeyword);
                if (matchName || matchId || matchCorp) {
                    filtered.add(st);
                }
            }
        }

        long totalElements = filtered.size();
        int totalPages = (int) Math.ceil((double) totalElements / safeSize);
        if (totalPages == 0) {
            totalPages = 1;
        }

        int fromIndex = safePage * safeSize;
        List<StationResponseDto> pageContent;
        if (fromIndex >= totalElements) {
            pageContent = new ArrayList<>();
        } else {
            int toIndex = Math.min(fromIndex + safeSize, (int) totalElements);
            pageContent = new ArrayList<>(filtered.subList(fromIndex, toIndex));
        }

        boolean isLast = (safePage >= totalPages - 1);
        return PageResponse.of(pageContent, safePage, safeSize, totalElements, totalPages, isLast);
    }
}
