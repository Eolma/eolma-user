package com.eolma.user.application.usecase;

import com.eolma.user.adapter.out.redis.RefreshTokenStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import static com.eolma.common.logging.StructuredLogger.kv;

@Service
public class LogoutUseCase {

    private static final Logger log = LoggerFactory.getLogger(LogoutUseCase.class);

    private final RefreshTokenStore refreshTokenStore;

    public LogoutUseCase(RefreshTokenStore refreshTokenStore) {
        this.refreshTokenStore = refreshTokenStore;
    }

    public void execute(String memberId) {
        refreshTokenStore.delete(memberId);
        log.info("Member logged out: {}", kv("memberId", memberId));
    }
}
