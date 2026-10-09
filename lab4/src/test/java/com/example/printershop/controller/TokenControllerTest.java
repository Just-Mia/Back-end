package com.example.printershop.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class TokenControllerTest {

    @Mock
    private OAuth2AuthorizedClient authorizedClient;

    @Mock
    private OAuth2AccessToken accessToken;

    private final TokenController tokenController = new TokenController();

    @Test
    void token_authorizedClientWithAccessToken_returnsTokenValueInMap() {

        when(authorizedClient.getAccessToken()).thenReturn(accessToken);
        when(accessToken.getTokenValue()).thenReturn("sample-jwt-value");


        Map<String, String> result = tokenController.token(authorizedClient);


        assertThat(result).containsExactly(Map.entry("access_token", "sample-jwt-value"));
    }
}
