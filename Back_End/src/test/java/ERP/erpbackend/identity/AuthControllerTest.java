package ERP.erpbackend.identity;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ERP.erpbackend.common.JpaAuditingConfig;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
		controllers = AuthController.class,
		excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class))
class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private RegistrationService registrationService;

	@Test
	void registersAccountAndReturns201() throws Exception {
		RegisteredAccount account = new RegisteredAccount(
				UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "ada@acme.test");
		when(registrationService.register(any(RegisterRequest.class))).thenReturn(account);

		String requestBody = """
				{
				  "organizationName": "Acme Corp",
				  "fullName": "Ada Owner",
				  "email": "ada@acme.test",
				  "password": "Sunrise8"
				}
				""";

		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.userId").value(account.userId().toString()))
				.andExpect(jsonPath("$.tenantId").value(account.tenantId().toString()))
				.andExpect(jsonPath("$.organizationId").value(account.organizationId().toString()))
				.andExpect(jsonPath("$.email").value("ada@acme.test"));
	}

	@Test
	void rejectsInvalidRequestWithValidationDetails() throws Exception {
		String requestBody = """
				{
				  "organizationName": "Acme Corp",
				  "fullName": "Ada Owner",
				  "email": "",
				  "password": "weak"
				}
				""";

		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.email").exists())
				.andExpect(jsonPath("$.errors.password").exists());
	}

}
