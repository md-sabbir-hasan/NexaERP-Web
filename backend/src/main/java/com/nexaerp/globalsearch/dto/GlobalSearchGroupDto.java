package com.nexaerp.globalsearch.dto;

import com.nexaerp.globalsearch.GlobalSearchResultType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlobalSearchGroupDto {
    private GlobalSearchResultType type;
    private List<GlobalSearchResultDto> results;
}
