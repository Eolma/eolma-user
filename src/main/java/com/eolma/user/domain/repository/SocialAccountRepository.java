package com.eolma.user.domain.repository;

import com.eolma.user.domain.model.AuthProvider;
import com.eolma.user.domain.model.SocialAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, String> {

    Optional<SocialAccount> findByProviderAndProviderId(AuthProvider provider, String providerId);

    List<SocialAccount> findByMemberId(String memberId);

    List<SocialAccount> findByMemberEmail(String email);
}
