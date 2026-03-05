package com.eolma.user.application.port.out.dto;

public record OAuthUserInfo(
        String providerId,
        String email,
        String name,
        String profileImage
) {
}
