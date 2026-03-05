package com.eolma.user.application.usecase;

import com.eolma.common.exception.EolmaException;
import com.eolma.common.exception.ErrorType;
import com.eolma.user.adapter.in.web.dto.LoginResponse;
import com.eolma.user.adapter.out.redis.RefreshTokenStore;
import com.eolma.user.application.port.out.TokenProvider;
import com.eolma.user.domain.model.Member;
import com.eolma.user.domain.repository.MemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.eolma.common.logging.StructuredLogger.kv;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SetNicknameUseCase {

    private static final Logger log = LoggerFactory.getLogger(SetNicknameUseCase.class);

    private final MemberRepository memberRepository;
    private final TokenProvider tokenProvider;
    private final RefreshTokenStore refreshTokenStore;

    public SetNicknameUseCase(MemberRepository memberRepository,
                               TokenProvider tokenProvider,
                               RefreshTokenStore refreshTokenStore) {
        this.memberRepository = memberRepository;
        this.tokenProvider = tokenProvider;
        this.refreshTokenStore = refreshTokenStore;
    }

    @Transactional
    public LoginResponse execute(Long memberId, String nickname) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new EolmaException(ErrorType.NOT_FOUND,
                        "Member not found"));

        if (memberRepository.existsByNickname(nickname)) {
            throw new EolmaException(ErrorType.INVALID_REQUEST,
                    "Nickname already in use");
        }

        member.setNickname(nickname);
        memberRepository.save(member);

        log.info("Nickname set: {} {}", kv("memberId", memberId), kv("nickname", nickname));

        // 닉네임이 포함된 JWT 재발급
        String accessToken = tokenProvider.createAccessToken(member);
        String refreshToken = tokenProvider.createRefreshToken(member);
        refreshTokenStore.save(member.getId(), refreshToken);

        return new LoginResponse(accessToken, refreshToken);
    }
}
