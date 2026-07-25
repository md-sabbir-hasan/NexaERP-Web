package com.nexaerp.globalsearch;

import com.nexaerp.common.response.ApiResponse;
import com.nexaerp.globalsearch.dto.GlobalSearchResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/global-search")
@RequiredArgsConstructor
public class GlobalSearchController {

    private final GlobalSearchService globalSearchService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<GlobalSearchResponseDto>> search(
            @RequestParam(name = "q") String query,
            @RequestParam(required = false) Integer limit,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                globalSearchService.search(query, limit, authentication)));
    }
}
