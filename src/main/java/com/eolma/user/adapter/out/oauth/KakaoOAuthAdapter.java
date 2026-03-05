package com.eolma.user.adapter.out.oauth;

import com.eolma.common.exception.EolmaException;
import com.eolma.common.exception.ErrorType;
import com.eolma.user.application.port.out.OAuthProviderPort;
import com.eolma.user.application.port.out.dto.OAuthTokenResponse;
import com.eolma.user.application.port.out.dto.OAuthUserInfo;
import com.eolma.user.config.OAuthConfig;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Component
public class KakaoOAuthAdapter implements OAuthProviderPort {

    private static final Logger log = LoggerFactory.getLogger(KakaoOAuthAdapter.class);
    private static final String TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

    private final OAuthConfig.ProviderConfig config;
    private final RestClient restClient;

    public KakaoOAuthAdapter(OAuthConfig oAuthConfig, RestClient.Builder restClientBuilder) {
        this.config = oAuthConfig.getKakao();
        this.restClient = restClientBuilder.build();
    }

    @Override
    public OAuthTokenResponse exchangeToken(String code, String redirectUri) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", config.getClientId());
        params.add("client_secret", config.getClientSecret());
        params.add("redirect_uri", redirectUri);
        params.add("code", code);

        try {
            JsonNode response = restClient.post()
                    .uri(TOKEN_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(params)
                    .retrieve()
                    .body(JsonNode.class);

            return new OAuthTokenResponse(response.get("access_token").asText());
        } catch (Exception e) {
            log.error("Kakao token exchange failed: {}", e.getMessage());
            throw new EolmaException(ErrorType.OAUTH_PROVIDER_ERROR,
                    "Failed to exchange token with Kakao");
        }
    }

    @Override
    public OAuthUserInfo getUserInfo(String accessToken) {
        try {
            JsonNode response = restClient.get()
                    .uri(USER_INFO_URL)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(JsonNode.class);

            String providerId = response.get("id").asText();
            JsonNode kakaoAccount = response.get("kakao_account");

            String email = kakaoAccount != null && kakaoAccount.has("email")
                    ? kakaoAccount.get("email").asText() : null;

            String nickname = null;
            String profileImage = null;
            if (kakaoAccount != null && kakaoAccount.has("profile")) {
                JsonNode profile = kakaoAccount.get("profile");
                nickname = profile.has("nickname") ? profile.get("nickname").asText() : null;
                profileImage = profile.has("profile_image_url") ? profile.get("profile_image_url").asText() : null;
            }

            return new OAuthUserInfo(providerId, email, nickname, profileImage);
        } catch (Exception e) {
            log.error("Kakao user info fetch failed: {}", e.getMessage());
            throw new EolmaException(ErrorType.OAUTH_PROVIDER_ERROR,
                    "Failed to fetch user info from Kakao");
        }
    }
}
