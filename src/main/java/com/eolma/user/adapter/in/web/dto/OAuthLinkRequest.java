package com.eolma.user.adapter.in.web.dto;

import com.eolma.user.domain.model.AuthProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OAuthLinkRequest(
        @NotBlank
        String linkToken,

        @NotNull
        AuthProvider provider,

        @NotBlank
        String code,

        @NotBlank
        String redirectUri,

        String password
) {
}
