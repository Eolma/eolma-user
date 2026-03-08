package com.eolma.user.application.usecase;

import com.eolma.common.exception.EolmaException;
import com.eolma.common.exception.ErrorType;
import com.eolma.user.domain.model.Member;
import com.eolma.user.domain.repository.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetMemberProfileUseCase {

    private final MemberRepository memberRepository;

    public GetMemberProfileUseCase(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Transactional(readOnly = true)
    public Member execute(String memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new EolmaException(ErrorType.USER_NOT_FOUND,
                        "Member not found: " + memberId));
    }
}
