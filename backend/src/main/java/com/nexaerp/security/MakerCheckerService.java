package com.nexaerp.security;

import com.nexaerp.common.exception.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MakerCheckerService {

    private final CurrentUserService currentUserService;

    public void validateChecker(
            Long createdBy,
            String documentName
    ) {
        if (createdBy == null) {
            throw new BusinessRuleException(
                    documentName +
                            " creator information is missing"
            );
        }

        Long currentUserId =
                currentUserService.getCurrentUserId();

        if (createdBy.equals(currentUserId)) {
            throw new BusinessRuleException(
                    "Creator cannot post their own " +
                            documentName
            );
        }
    }
}