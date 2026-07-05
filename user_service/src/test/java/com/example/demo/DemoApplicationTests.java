package com.example.demo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.example.demo.auth.dto.LoginRequest;
import com.example.demo.auth.dto.RefreshTokenRequest;
import com.example.demo.auth.dto.TokenResponse;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
		webEnvironment = SpringBootTest.WebEnvironment.MOCK,
		properties = {
				"jwt.access-token.secret=TestAccessSecretKeyForHS256ShouldBeLongEnough123!",
				"jwt.refresh-token.secret=TestRefreshSecretKeyForHS256ShouldBeLongEnough456!"
		}
)
class DemoApplicationTests {

	@Autowired
	private ApplicationContext applicationContext;

	private WebTestClient webTestClient;

	@BeforeEach
	void setUp() {
		webTestClient = WebTestClient.bindToApplicationContext(applicationContext)
				.configureClient()
				.build();
	}

	@Test
	void userCanAccessUserEndpointButNotAdmin() {
		TokenResponse userTokens = login("user", "user123");

		webTestClient.get()
				.uri("/controller/user/profile")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + userTokens.accessToken())
				.exchange()
				.expectStatus().isOk()
				.expectBody(String.class)
				.value(body -> assertThat(body).contains("user"));

		webTestClient.get()
				.uri("/controller/admin/panel")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + userTokens.accessToken())
				.exchange()
				.expectStatus().isForbidden();
	}

	@Test
	void supplierCanAccessSupplierEndpoint() {
		TokenResponse supplierTokens = login("supplier", "supplier123");

		webTestClient.get()
				.uri("/controller/supplier/portal")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + supplierTokens.accessToken())
				.exchange()
				.expectStatus().isOk()
				.expectBody(String.class)
				.value(body -> assertThat(body).contains("supplier"));
	}

	@Test
	void refreshTokenIssuesNewAdminTokens() {
		TokenResponse adminTokens = login("admin", "admin123");

		TokenResponse refreshedTokens = webTestClient.post()
				.uri("/auth/refresh")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(new RefreshTokenRequest(adminTokens.refreshToken()))
				.exchange()
				.expectStatus().isOk()
				.expectBody(TokenResponse.class)
				.returnResult()
				.getResponseBody();

		assertThat(refreshedTokens).isNotNull();
		assertThat(refreshedTokens.role()).isEqualTo("ADMIN");
		assertThat(refreshedTokens.accessToken()).isNotBlank();
		assertThat(refreshedTokens.refreshToken()).isNotBlank();

		webTestClient.get()
				.uri("/controller/admin/panel")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + refreshedTokens.accessToken())
				.exchange()
				.expectStatus().isOk();
	}

	private TokenResponse login(String username, String password) {
		return webTestClient.post()
				.uri("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(new LoginRequest(username, password))
				.exchange()
				.expectStatus().isOk()
				.expectBody(TokenResponse.class)
				.returnResult()
				.getResponseBody();
	}

}
