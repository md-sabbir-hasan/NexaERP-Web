package com.nexaerp.globalsearch;

import com.nexaerp.globalsearch.dto.GlobalSearchResponseDto;
import org.springframework.security.core.Authentication;

public interface GlobalSearchService {
    GlobalSearchResponseDto search(String query, Integer limit, Authentication authentication);
}
