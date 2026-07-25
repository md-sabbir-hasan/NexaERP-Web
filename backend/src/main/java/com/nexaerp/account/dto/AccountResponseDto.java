package com.nexaerp.account.dto;

import com.nexaerp.account.AccountType;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountResponseDto {
    private Long id;
    private String code;
    private String name;
    private String description;
    private AccountType type;
    private Boolean isActive;
    private Boolean isDefault;
    private Long parentId;
    private String parentName;
    private BigDecimal currentBalance;
    private Boolean hasChildren;
    private List<AccountResponseDto> children; //for Tree structure
}
