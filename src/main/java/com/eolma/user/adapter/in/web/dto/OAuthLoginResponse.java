package com.eolma.user.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OAuthLoginResponse(
        String accessToken,
        String refreshToken,
        Boolean nicknameRequired,
        String linkToken,
        List<String> existingProviders,
        String email
) {
    public static OAuthLoginResponse existingMember(String accessToken, String refreshToken) {
        return new OAuthLoginResponse(accessToken, refreshToken, null, null, null, null);
    }

    public static OAuthLoginResponse newMember(String accessToken, String refreshToken) {
        return new OAuthLoginResponse(accessToken, refreshToken, true, null, null, null);
    }

    public static OAuthLoginResponse linkRequired(String linkToken, List<String> existingProviders, String email) {
        return new OAuthLoginResponse(null, null, null, linkToken, existingProviders, email);
    }
}
