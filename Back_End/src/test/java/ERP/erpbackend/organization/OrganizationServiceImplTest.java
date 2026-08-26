package ERP.erpbackend.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceImplTest {

	@Mock
	private TenantRepository tenantRepository;

	@Mock
	private OrganizationRepository organizationRepository;

	@InjectMocks
	private OrganizationServiceImpl organizationService;

	@Test
	void usesFreeSlugAsIs() {
		when(tenantRepository.existsByCode("acme-corp")).thenReturn(false);
		when(tenantRepository.save(any(Tenant.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(organizationRepository.save(any(Organization.class))).thenAnswer(invocation -> invocation.getArgument(0));

		organizationService.createTenantAndOrganization("Acme Corp");

		ArgumentCaptor<Tenant> tenantCaptor = ArgumentCaptor.forClass(Tenant.class);
		verify(tenantRepository).save(tenantCaptor.capture());
		assertThat(tenantCaptor.getValue().getCode()).isEqualTo("acme-corp");

		ArgumentCaptor<Organization> organizationCaptor = ArgumentCaptor.forClass(Organization.class);
		verify(organizationRepository).save(organizationCaptor.capture());
		assertThat(organizationCaptor.getValue().getCode()).isEqualTo("acme-corp");
	}

	@Test
	void fallsBackToOrgPrefixWhenNameHasNoAsciiAlphanumericCharacters() {
		when(tenantRepository.existsByCode("org")).thenReturn(false);
		when(tenantRepository.save(any(Tenant.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(organizationRepository.save(any(Organization.class))).thenAnswer(invocation -> invocation.getArgument(0));

		organizationService.createTenantAndOrganization("雅虎");

		ArgumentCaptor<Tenant> tenantCaptor = ArgumentCaptor.forClass(Tenant.class);
		verify(tenantRepository).save(tenantCaptor.capture());
		assertThat(tenantCaptor.getValue().getCode()).isEqualTo("org");

		ArgumentCaptor<Organization> organizationCaptor = ArgumentCaptor.forClass(Organization.class);
		verify(organizationRepository).save(organizationCaptor.capture());
		assertThat(organizationCaptor.getValue().getCode()).isEqualTo("org");
	}

	@Test
	void retriesOrgPrefixWithNumericSuffixWhenTaken() {
		when(tenantRepository.existsByCode("org")).thenReturn(true);
		when(tenantRepository.existsByCode("org-2")).thenReturn(false);
		when(tenantRepository.save(any(Tenant.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(organizationRepository.save(any(Organization.class))).thenAnswer(invocation -> invocation.getArgument(0));

		organizationService.createTenantAndOrganization("@@@");

		ArgumentCaptor<Tenant> tenantCaptor = ArgumentCaptor.forClass(Tenant.class);
		verify(tenantRepository).save(tenantCaptor.capture());
		assertThat(tenantCaptor.getValue().getCode()).isEqualTo("org-2");

		ArgumentCaptor<Organization> organizationCaptor = ArgumentCaptor.forClass(Organization.class);
		verify(organizationRepository).save(organizationCaptor.capture());
		assertThat(organizationCaptor.getValue().getCode()).isEqualTo("org-2");
	}

	@Test
	void retriesWithNumericSuffixUntilCodeIsFree() {
		when(tenantRepository.existsByCode("acme-corp")).thenReturn(true);
		when(tenantRepository.existsByCode("acme-corp-2")).thenReturn(true);
		when(tenantRepository.existsByCode("acme-corp-3")).thenReturn(false);
		when(tenantRepository.save(any(Tenant.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(organizationRepository.save(any(Organization.class))).thenAnswer(invocation -> invocation.getArgument(0));

		organizationService.createTenantAndOrganization("Acme Corp");

		ArgumentCaptor<Tenant> tenantCaptor = ArgumentCaptor.forClass(Tenant.class);
		verify(tenantRepository).save(tenantCaptor.capture());
		assertThat(tenantCaptor.getValue().getCode()).isEqualTo("acme-corp-3");

		ArgumentCaptor<Organization> organizationCaptor = ArgumentCaptor.forClass(Organization.class);
		verify(organizationRepository).save(organizationCaptor.capture());
		assertThat(organizationCaptor.getValue().getCode()).isEqualTo("acme-corp-3");
	}

	@Test
	void fallsBackToRandomSuffixOnceNumericSuffixesAreExhausted() {
		when(tenantRepository.existsByCode(anyString())).thenReturn(true);
		when(tenantRepository.save(any(Tenant.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(organizationRepository.save(any(Organization.class))).thenAnswer(invocation -> invocation.getArgument(0));

		organizationService.createTenantAndOrganization("Acme Corp");

		verify(tenantRepository, times(6)).existsByCode(anyString());

		ArgumentCaptor<Tenant> tenantCaptor = ArgumentCaptor.forClass(Tenant.class);
		verify(tenantRepository).save(tenantCaptor.capture());
		assertThat(tenantCaptor.getValue().getCode()).matches("acme-corp-[0-9a-f]{8}");
	}

}
