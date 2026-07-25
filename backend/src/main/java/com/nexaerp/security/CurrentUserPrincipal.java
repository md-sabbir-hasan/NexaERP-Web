package com.nexaerp.security;

import java.security.Principal;

public record CurrentUserPrincipal(
        Long userId,
        String email
) implements Principal {

    @Override
    public String getName() {
        return email;
    }
}