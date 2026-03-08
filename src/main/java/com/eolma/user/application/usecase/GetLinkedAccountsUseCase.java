package com.eolma.user.application.usecase;

import com.eolma.user.adapter.in.web.dto.LinkedAccountResponse;
import com.eolma.user.domain.model.SocialAccount;
import com.eolma.user.domain.repository.SocialAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GetLinkedAccountsUseCase {

    private final SocialAccountRepository socialAccountRepository;

    public GetLinkedAccountsUseCase(SocialAccountRepository socialAccountRepository) {
        this.socialAccountRepository = socialAccountRepository;
    }

    @Transactional(readOnly = true)
    public List<LinkedAccountResponse> execute(String memberId) {
        List<SocialAccount> accounts = socialAccountRepository.findByMemberId(memberId);
        return accounts.stream()
                .map(LinkedAccountResponse::from)
                .toList();
    }
}
