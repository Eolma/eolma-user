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
import org.springframework.stereotype.Service;

import static com.eolma.common.logging.StructuredLogger.kv;

@Service
public class RefreshTokenUseCase {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenUseCase.class);

    private final TokenProvider tokenProvider;
    private final RefreshTokenStore refreshTokenStore;
    private final MemberRepository memberRepository;

    public RefreshTokenUseCase(TokenProvider tokenProvider,
                                RefreshTokenStore refreshTokenStore,
                                MemberRepository memberRepository) {
        this.tokenProvider = tokenProvider;
        this.refreshTokenStore = refreshTokenStore;
        this.memberRepository = memberRepository;
    }

    public LoginResponse execute(String refreshToken) {
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new EolmaException(ErrorType.UNAUTHORIZED, "Invalid refresh token");
        }

        Long memberId = tokenProvider.getMemberIdFromToken(refreshToken);

        String storedToken = refreshTokenStore.find(memberId)
                .orElseThrow(() -> new EolmaException(ErrorType.UNAUTHORIZED,
                        "Refresh token not found or expired"));

        if (!storedToken.equals(refreshToken)) {
            throw new EolmaException(ErrorType.UNAUTHORIZED, "Refresh token mismatch");
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new EolmaException(ErrorType.USER_NOT_FOUND,
                        "Member not found: " + memberId));

        String newAccessToken = tokenProvider.createAccessToken(member);
        String newRefreshToken = tokenProvider.createRefreshToken(member);

        refreshTokenStore.save(memberId, newRefreshToken);
        log.info("Token refreshed: {}", kv("memberId", memberId));

        return new LoginResponse(newAccessToken, newRefreshToken);
    }
}
