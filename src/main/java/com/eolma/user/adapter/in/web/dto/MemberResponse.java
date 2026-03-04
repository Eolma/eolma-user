package com.eolma.user.adapter.in.web.dto;

import com.eolma.user.domain.model.Member;

public record MemberResponse(
        Long id,
        String email,
        String nickname,
        String profileImage,
        String role,
        String status
) {

    public static MemberResponse from(Member member) {
        return new MemberResponse(
                member.getId(),
                member.getEmail(),
                member.getNickname(),
                member.getProfileImage(),
                member.getRole().name(),
                member.getStatus().name()
        );
    }
}
