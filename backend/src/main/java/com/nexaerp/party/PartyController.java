package com.nexaerp.party;

import com.nexaerp.audit.AuditAction;
import com.nexaerp.audit.AuditLogService;
import com.nexaerp.common.response.ApiResponse;
import com.nexaerp.fileupload.FileUploadService;
import com.nexaerp.fileupload.dto.FileUploadResponseDto;
import com.nexaerp.party.dto.PartyRequestDto;
import com.nexaerp.party.dto.PartyResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/parties")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class PartyController {
    private final PartyService partyService;
    private final FileUploadService fileUploadService;
    private final PartyRepository partyRepository;
    private final AuditLogService auditLogService;

    @PostMapping
    @PreAuthorize("hasAuthority('CREATE_PARTY')")
    public ResponseEntity<ApiResponse<PartyResponseDto>> create(
            @Valid @RequestBody PartyRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success("Party created",
                partyService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('EDIT_PARTY')")
    public ResponseEntity<ApiResponse<PartyResponseDto>> update(
            @PathVariable Long id,
            @Valid @RequestBody PartyRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success("Party updated",
                partyService.update(id, request)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('VIEW_PARTY')")
    public ResponseEntity<ApiResponse<PartyResponseDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(partyService.getById(id)));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('VIEW_PARTY')")
    public ResponseEntity<ApiResponse<List<PartyResponseDto>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(partyService.getAll()));
    }

    @GetMapping("/type/{type}")
    @PreAuthorize("hasAnyAuthority('VIEW_PARTY', 'LOOKUP_PARTIES')")
    public ResponseEntity<ApiResponse<List<PartyResponseDto>>> getByType(
            @PathVariable PartyType type) {
        return ResponseEntity.ok(ApiResponse.success(partyService.getByType(type)));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('DEACTIVATE_PARTY')")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable Long id) {
        partyService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.success("Party deactivated", null));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('ACTIVATE_PARTY')")
    public ResponseEntity<ApiResponse<Void>> activate(@PathVariable Long id) {

        partyService.activate(id);

        return ResponseEntity.ok(
                ApiResponse.success("Party activated", null)
        );
    }

//    for file upload
// Document upload endpoint
@PostMapping("/{id}/documents/trade-license")
public ResponseEntity<ApiResponse<FileUploadResponseDto>> uploadTradeLicense(
        @PathVariable Long id,
        @RequestParam("file") MultipartFile file) {

    FileUploadResponseDto response =
            fileUploadService.upload(file, "PARTY", id);

    // Update party with file URL
    partyRepository.findById(id).ifPresent(party -> {
        party.setTradeLicenseUrl(response.getFileUrl());
        partyRepository.save(party);
        auditLogService.log(
                AuditAction.UPLOADED,
                "PARTY_DOCUMENT",
                party.getId(),
                null,
                "Trade License uploaded: " + response.getOriginalName()
        );
    });

    return ResponseEntity.ok(ApiResponse.success(
            "Trade license uploaded", response));
}

    @PostMapping("/{id}/documents/bin-certificate")
    public ResponseEntity<ApiResponse<FileUploadResponseDto>> uploadBinCertificate(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {

        FileUploadResponseDto response =
                fileUploadService.upload(file, "PARTY", id);

        partyRepository.findById(id).ifPresent(party -> {
            party.setBinCertificateUrl(response.getFileUrl());
            partyRepository.save(party);
            auditLogService.log(
                    AuditAction.UPLOADED,
                    "PARTY_DOCUMENT",
                    party.getId(),
                    null,
                    "Bin Certificate: " + response.getOriginalName()
            );
        });

        return ResponseEntity.ok(ApiResponse.success(
                "BIN certificate uploaded", response));
    }

    @PostMapping("/{id}/documents/tin-certificate")
    public ResponseEntity<ApiResponse<FileUploadResponseDto>> uploadTinCertificate(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {

        FileUploadResponseDto response =
                fileUploadService.upload(file, "PARTY", id);

        partyRepository.findById(id).ifPresent(party -> {
            party.setTinCertificateUrl(response.getFileUrl());
            partyRepository.save(party);
            auditLogService.log(
                    AuditAction.UPLOADED,
                    "PARTY_DOCUMENT",
                    party.getId(),
                    null,
                    "Tin Certificate: " + response.getOriginalName()
            );
        });

        return ResponseEntity.ok(ApiResponse.success(
                "TIN certificate uploaded", response));
    }

    @PostMapping("/{id}/documents/nid")
    public ResponseEntity<ApiResponse<FileUploadResponseDto>> uploadNid(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {

        FileUploadResponseDto response =
                fileUploadService.upload(file, "PARTY", id);

        partyRepository.findById(id).ifPresent(party -> {
            party.setNidUrl(response.getFileUrl());
            partyRepository.save(party);
            auditLogService.log(
                    AuditAction.UPLOADED,
                    "PARTY_DOCUMENT",
                    party.getId(),
                    null,
                    "NID: " + response.getOriginalName()
            );
        });

        return ResponseEntity.ok(ApiResponse.success("NID uploaded", response));
    }

}
