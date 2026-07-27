package com.nexaerp.costcenter;

import com.nexaerp.common.response.ApiResponse;
import com.nexaerp.costcenter.dto.CostCenterLookupDto;
import com.nexaerp.costcenter.dto.CostCenterRequestDto;
import com.nexaerp.costcenter.dto.CostCenterResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cost-centers")
@RequiredArgsConstructor
public class CostCenterController {

    private final CostCenterService costCenterService;

    @GetMapping
    @PreAuthorize("hasAuthority('VIEW_COST_CENTER')")
    public ResponseEntity<ApiResponse<List<CostCenterResponseDto>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(costCenterService.getAll()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('VIEW_COST_CENTER')")
    public ResponseEntity<ApiResponse<CostCenterResponseDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(costCenterService.getById(id)));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('VIEW_COST_CENTER')")
    public ResponseEntity<ApiResponse<List<CostCenterResponseDto>>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(ApiResponse.success(costCenterService.search(keyword, active)));
    }

    @GetMapping("/lookup")
    @PreAuthorize("hasAuthority('LOOKUP_COST_CENTER')")
    public ResponseEntity<ApiResponse<List<CostCenterLookupDto>>> lookup() {
        return ResponseEntity.ok(ApiResponse.success(costCenterService.lookup()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CREATE_COST_CENTER')")
    public ResponseEntity<ApiResponse<CostCenterResponseDto>> create(
            @Valid @RequestBody CostCenterRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success("Cost center created", costCenterService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('EDIT_COST_CENTER')")
    public ResponseEntity<ApiResponse<CostCenterResponseDto>> update(
            @PathVariable Long id, @Valid @RequestBody CostCenterRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success("Cost center updated", costCenterService.update(id, request)));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('EDIT_COST_CENTER')")
    public ResponseEntity<ApiResponse<Void>> activate(@PathVariable Long id) {
        costCenterService.activate(id);
        return ResponseEntity.ok(ApiResponse.success("Cost center activated", null));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('DEACTIVATE_COST_CENTER')")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable Long id) {
        costCenterService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.success("Cost center deactivated", null));
    }
}
