package com.eolma.user.adapter.in.web.dto;

public record LoginResponse(
        String accessToken,
        String refreshToken
) {
}
