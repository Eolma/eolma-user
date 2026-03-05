package com.eolma.user.application.port.out;

import com.eolma.user.application.port.out.dto.OAuthTokenResponse;
import com.eolma.user.application.port.out.dto.OAuthUserInfo;

public interface OAuthProviderPort {

    OAuthTokenResponse exchangeToken(String code, String redirectUri);

    OAuthUserInfo getUserInfo(String accessToken);
}
