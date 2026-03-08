package com.eolma.user.adapter.in.web.dto;

import com.eolma.user.domain.model.Member;

public record PublicMemberResponse(
        String id,
        String nickname,
        String profileImage
) {

    public static PublicMemberResponse from(Member member) {
        return new PublicMemberResponse(
                member.getId(),
                member.getNickname(),
                member.getProfileImage()
        );
    }
}
