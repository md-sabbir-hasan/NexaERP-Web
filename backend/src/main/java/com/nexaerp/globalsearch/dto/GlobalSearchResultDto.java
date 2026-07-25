package com.nexaerp.globalsearch.dto;

import com.nexaerp.globalsearch.GlobalSearchResultType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlobalSearchResultDto {
    private Long id;
    private GlobalSearchResultType type;
    private String title;
    private String subtitle;
    private String status;
}
