package com.nexaerp.settings.dto;


import com.nexaerp.settings.SettingKey;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SettingResponseDto {
    private Long id;
    private SettingKey key;
    private String value;
    private String description;
    private LocalDateTime updatedAt;
}
