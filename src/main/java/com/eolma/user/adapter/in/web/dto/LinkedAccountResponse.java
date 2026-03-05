package com.eolma.user.adapter.in.web.dto;

import com.eolma.user.domain.model.SocialAccount;

import java.time.Instant;

public record LinkedAccountResponse(
        String provider,
        Instant linkedAt
) {
    public static LinkedAccountResponse from(SocialAccount socialAccount) {
        return new LinkedAccountResponse(
                socialAccount.getProvider().name(),
                socialAccount.getCreatedAt()
        );
    }
}
