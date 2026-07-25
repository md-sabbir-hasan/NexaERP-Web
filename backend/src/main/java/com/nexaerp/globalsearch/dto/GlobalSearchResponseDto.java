package com.nexaerp.globalsearch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlobalSearchResponseDto {
    private String query;
    private List<GlobalSearchGroupDto> groups;
}
