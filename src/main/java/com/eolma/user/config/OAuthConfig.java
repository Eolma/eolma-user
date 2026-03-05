package com.eolma.user.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "oauth")
public class OAuthConfig {

    private ProviderConfig google = new ProviderConfig();
    private ProviderConfig kakao = new ProviderConfig();

    @Getter
    @Setter
    public static class ProviderConfig {
        private String clientId;
        private String clientSecret;
    }
}
