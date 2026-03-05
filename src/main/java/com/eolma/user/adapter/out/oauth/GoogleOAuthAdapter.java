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
public class GoogleOAuthAdapter implements OAuthProviderPort {

    private static final Logger log = LoggerFactory.getLogger(GoogleOAuthAdapter.class);
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String USER_INFO_URL = "https://www.googleapis.com/oauth2/v2/userinfo";

    private final OAuthConfig.ProviderConfig config;
    private final RestClient restClient;

    public GoogleOAuthAdapter(OAuthConfig oAuthConfig, RestClient.Builder restClientBuilder) {
        this.config = oAuthConfig.getGoogle();
        this.restClient = restClientBuilder.build();
    }

    @Override
    public OAuthTokenResponse exchangeToken(String code, String redirectUri) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
        params.add("client_id", config.getClientId());
        params.add("client_secret", config.getClientSecret());
        params.add("redirect_uri", redirectUri);
        params.add("grant_type", "authorization_code");

        try {
            JsonNode response = restClient.post()
                    .uri(TOKEN_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(params)
                    .retrieve()
                    .body(JsonNode.class);

            return new OAuthTokenResponse(response.get("access_token").asText());
        } catch (Exception e) {
            log.error("Google token exchange failed: {}", e.getMessage());
            throw new EolmaException(ErrorType.OAUTH_PROVIDER_ERROR,
                    "Failed to exchange token with Google");
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

            return new OAuthUserInfo(
                    response.get("id").asText(),
                    response.get("email").asText(),
                    response.has("name") ? response.get("name").asText() : null,
                    response.has("picture") ? response.get("picture").asText() : null
            );
        } catch (Exception e) {
            log.error("Google user info fetch failed: {}", e.getMessage());
            throw new EolmaException(ErrorType.OAUTH_PROVIDER_ERROR,
                    "Failed to fetch user info from Google");
        }
    }
}
