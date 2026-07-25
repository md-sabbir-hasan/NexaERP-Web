package com.nexaerp.security;

import com.nexaerp.common.exception.BusinessRuleException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    public CurrentUserPrincipal getCurrentPrincipal() {
        Authentication authentication =
                SecurityContextHolder.getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()) {
            throw new BusinessRuleException(
                    "Authenticated user could not be determined"
            );
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof CurrentUserPrincipal currentUser)) {
            throw new BusinessRuleException(
                    "Authenticated user information is invalid"
            );
        }

        return currentUser;
    }

    public Long getCurrentUserId() {
        return getCurrentPrincipal().userId();
    }

    public String getCurrentUserEmail() {
        return getCurrentPrincipal().email();
    }
}