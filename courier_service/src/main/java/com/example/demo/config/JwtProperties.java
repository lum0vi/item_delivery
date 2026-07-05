package com.example.demo.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private TokenProperties accessToken = new TokenProperties();
    private TokenProperties refreshToken = new TokenProperties();

    public TokenProperties getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(TokenProperties accessToken) {
        this.accessToken = accessToken;
    }

    public TokenProperties getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(TokenProperties refreshToken) {
        this.refreshToken = refreshToken;
    }

    public static class TokenProperties {
        private String secret;
        private long expirationMs;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public long getExpirationMs() {
            return expirationMs;
        }

        public void setExpirationMs(long expirationMs) {
            this.expirationMs = expirationMs;
        }
    }
}
