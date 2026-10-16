package com.example.printershop.configuration;

import com.example.printershop.domain.annotation.CurrentUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.ServletWebRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CurrentUserArgumentResolverTest {

    private final CurrentUserArgumentResolver resolver = new CurrentUserArgumentResolver();

    @SuppressWarnings("unused")
    static class Dummy {
        void annotated(@CurrentUser String username) {
        }

        void plain(String username) {
        }

        void annotatedWrongType(@CurrentUser Integer username) {
        }
    }

    private MethodParameter param(String method, Class<?> type) throws NoSuchMethodException {
        return new MethodParameter(Dummy.class.getDeclaredMethod(method, type), 0);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("AC6: supportsParameter - true з анотацією, false без неї")
    void supportsParameter_trueOnlyWhenAnnotated() throws Exception {
        assertThat(resolver.supportsParameter(param("annotated", String.class))).isTrue();
        assertThat(resolver.supportsParameter(param("plain", String.class))).isFalse();
        assertThat(resolver.supportsParameter(param("annotatedWrongType", Integer.class))).isFalse();
    }

    @Test
    @DisplayName("AC7: resolveArgument повертає ім'я автентифікованого користувача (String)")
    void resolveArgument_returnsAuthenticatedUsername() throws Exception {
        TestingAuthenticationToken auth = new TestingAuthenticationToken("customer", "n/a", "ROLE_USER");
        auth.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(auth);

        Object result = resolver.resolveArgument(param("annotated", String.class), null,
                new ServletWebRequest(new MockHttpServletRequest()), null);

        assertThat(result).isInstanceOf(String.class).isEqualTo("customer");
    }

    @Test
    @DisplayName("resolveArgument без автентифікації -> AccessDeniedException")
    void resolveArgument_noAuthentication_throws() throws Exception {
        MethodParameter parameter = param("annotated", String.class);
        ServletWebRequest request = new ServletWebRequest(new MockHttpServletRequest());

        assertThatThrownBy(() -> resolver.resolveArgument(parameter, null, request, null))
                .isInstanceOf(AccessDeniedException.class);
    }
}
