package com.eolma.user.application.usecase;

import com.eolma.common.exception.EolmaException;
import com.eolma.common.exception.ErrorType;
import com.eolma.user.domain.model.Member;
import com.eolma.user.domain.repository.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateMemberProfileUseCase {

    private final MemberRepository memberRepository;

    public UpdateMemberProfileUseCase(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Transactional
    public Member execute(String memberId, String nickname, String profileImage) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new EolmaException(ErrorType.USER_NOT_FOUND,
                        "Member not found: " + memberId));

        if (nickname != null && !nickname.equals(member.getNickname())
                && memberRepository.existsByNickname(nickname)) {
            throw new EolmaException(ErrorType.INVALID_REQUEST,
                    "Nickname already in use");
        }

        member.updateProfile(nickname, profileImage);
        return member;
    }
}
