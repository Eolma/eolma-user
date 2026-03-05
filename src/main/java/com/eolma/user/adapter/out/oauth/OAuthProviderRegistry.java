package com.eolma.user.adapter.out.oauth;

import com.eolma.user.application.port.out.OAuthProviderPort;
import com.eolma.user.domain.model.AuthProvider;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

@Component
public class OAuthProviderRegistry {

    private final Map<AuthProvider, OAuthProviderPort> providers;

    public OAuthProviderRegistry(GoogleOAuthAdapter googleOAuthAdapter,
                                  KakaoOAuthAdapter kakaoOAuthAdapter) {
        this.providers = new EnumMap<>(AuthProvider.class);
        this.providers.put(AuthProvider.GOOGLE, googleOAuthAdapter);
        this.providers.put(AuthProvider.KAKAO, kakaoOAuthAdapter);
    }

    public OAuthProviderPort getProvider(AuthProvider provider) {
        OAuthProviderPort port = providers.get(provider);
        if (port == null) {
            throw new IllegalArgumentException("Unsupported OAuth provider: " + provider);
        }
        return port;
    }
}
