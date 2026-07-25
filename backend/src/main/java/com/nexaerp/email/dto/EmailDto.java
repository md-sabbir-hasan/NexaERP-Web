package com.nexaerp.email.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailDto {
    private String to;
    private String subject;
    private String body;
}
