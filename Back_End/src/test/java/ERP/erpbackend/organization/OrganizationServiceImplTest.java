package ERP.erpbackend.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

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

	private Organization organizationWith(UUID id, String name) {
		Organization organization = new Organization();
		organization.setName(name);
		organization.setCode(name);
		ReflectionTestUtils.setField(organization, "id", id);
		return organization;
	}

	@Test
	void findNamesByIdsReturnsNamesKeyedByIdForExistingOrganizations() {
		UUID id1 = UUID.randomUUID();
		UUID id2 = UUID.randomUUID();
		when(organizationRepository.findAllById(Set.of(id1, id2)))
				.thenReturn(List.of(organizationWith(id1, "Head Office"), organizationWith(id2, "Colombo Branch")));

		Map<UUID, String> names = organizationService.findNamesByIds(Set.of(id1, id2));

		assertThat(names).containsExactlyInAnyOrderEntriesOf(Map.of(id1, "Head Office", id2, "Colombo Branch"));
	}

	@Test
	void findNamesByIdsOmitsIdsThatDoNotResolveToAnOrganization() {
		UUID existingId = UUID.randomUUID();
		UUID deletedId = UUID.randomUUID();
		when(organizationRepository.findAllById(Set.of(existingId, deletedId)))
				.thenReturn(List.of(organizationWith(existingId, "Head Office")));

		Map<UUID, String> names = organizationService.findNamesByIds(Set.of(existingId, deletedId));

		assertThat(names).containsOnlyKeys(existingId);
	}

	@Test
	void findNamesByIdsShortCircuitsOnEmptyInputWithoutQuerying() {
		Map<UUID, String> names = organizationService.findNamesByIds(Set.of());

		assertThat(names).isEmpty();
		verifyNoInteractions(organizationRepository);
	}

	@Test
	void findTenantIdReturnsTheOwningTenantForAnExistingOrganization() {
		UUID organizationId = UUID.randomUUID();
		UUID tenantId = UUID.randomUUID();
		Organization organization = organizationWith(organizationId, "Head Office");
		organization.setTenantId(tenantId);
		when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));

		assertThat(organizationService.findTenantId(organizationId)).contains(tenantId);
	}

	@Test
	void findTenantIdIsEmptyForAnUnknownOrganizationId() {
		UUID organizationId = UUID.randomUUID();
		when(organizationRepository.findById(organizationId)).thenReturn(Optional.empty());

		assertThat(organizationService.findTenantId(organizationId)).isEmpty();
	}

	@Test
	void findActiveByTenantIdsMapsWhateverTheActiveOnlyQueryReturns() {
		UUID tenantId = UUID.randomUUID();
		Organization organization = organizationWith(UUID.randomUUID(), "Head Office");
		organization.setTenantId(tenantId);
		when(organizationRepository.findByTenantIdInAndActiveTrue(Set.of(tenantId)))
				.thenReturn(List.of(organization));

		List<OrganizationSummary> result = organizationService.findActiveByTenantIds(Set.of(tenantId));

		assertThat(result).singleElement().satisfies(summary -> {
			assertThat(summary.id()).isEqualTo(organization.getId());
			assertThat(summary.tenantId()).isEqualTo(tenantId);
			assertThat(summary.name()).isEqualTo("Head Office");
		});
	}

	@Test
	void findActiveByTenantIdsShortCircuitsOnEmptyInputWithoutQuerying() {
		assertThat(organizationService.findActiveByTenantIds(Set.of())).isEmpty();
		verifyNoInteractions(organizationRepository);
	}

	@Test
	void findTenantNamesByIdsKeysNamesByIdAndOmitsUnknownIds() {
		UUID existingId = UUID.randomUUID();
		UUID deletedId = UUID.randomUUID();
		Tenant tenant = new Tenant();
		tenant.setName("Acme Corp");
		ReflectionTestUtils.setField(tenant, "id", existingId);
		when(tenantRepository.findAllById(Set.of(existingId, deletedId))).thenReturn(List.of(tenant));

		Map<UUID, String> names = organizationService.findTenantNamesByIds(Set.of(existingId, deletedId));

		assertThat(names).containsOnlyKeys(existingId).containsEntry(existingId, "Acme Corp");
	}

	@Test
	void findTenantNamesByIdsShortCircuitsOnEmptyInputWithoutQuerying() {
		assertThat(organizationService.findTenantNamesByIds(Set.of())).isEmpty();
		verifyNoInteractions(tenantRepository);
	}

	@Test
	void findAllByTenantIdReturnsPlanLimitAndEveryOrganizationIncludingInactiveOnesInQueryOrder() {
		UUID tenantId = UUID.randomUUID();
		Tenant tenant = new Tenant();
		tenant.setPlan("Pro E-commerce");
		tenant.setMaxOrganizations(5);
		ReflectionTestUtils.setField(tenant, "id", tenantId);
		when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

		Organization head = organizationWith(UUID.randomUUID(), "Head Office");
		head.setTenantId(tenantId);
		ReflectionTestUtils.setField(head, "createdAt", Instant.parse("2026-01-01T00:00:00Z"));
		Organization retired = organizationWith(UUID.randomUUID(), "Old Branch");
		retired.setTenantId(tenantId);
		retired.setActive(false);
		ReflectionTestUtils.setField(retired, "createdAt", Instant.parse("2026-02-01T00:00:00Z"));
		when(organizationRepository.findByTenantIdOrderByCreatedAtAsc(tenantId)).thenReturn(List.of(head, retired));

		OrganizationListView view = organizationService.findAllByTenantId(tenantId);

		assertThat(view.plan()).isEqualTo("Pro E-commerce");
		assertThat(view.maxOrganizations()).isEqualTo(5);
		assertThat(view.organizations()).hasSize(2);
		assertThat(view.organizations().get(0).name()).isEqualTo("Head Office");
		assertThat(view.organizations().get(0).active()).isTrue();
		assertThat(view.organizations().get(0).createdAt()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
		assertThat(view.organizations().get(1).name()).isEqualTo("Old Branch");
		assertThat(view.organizations().get(1).active()).isFalse();
	}

	@Test
	void findAllByTenantIdThrowsWhenTheTenantIdDoesNotResolve() {
		UUID tenantId = UUID.randomUUID();
		when(tenantRepository.findById(tenantId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> organizationService.findAllByTenantId(tenantId))
				.isInstanceOf(IllegalStateException.class);
	}

	private Tenant tenantWithLimit(UUID tenantId, int maxOrganizations) {
		Tenant tenant = new Tenant();
		tenant.setMaxOrganizations(maxOrganizations);
		ReflectionTestUtils.setField(tenant, "id", tenantId);
		return tenant;
	}

	@Test
	void createOrganizationSavesAnActiveOrgUnderTheTenantWithAPerTenantUniqueCode() {
		UUID tenantId = UUID.randomUUID();
		when(tenantRepository.findByIdForUpdate(tenantId)).thenReturn(Optional.of(tenantWithLimit(tenantId, 5)));
		when(organizationRepository.countByTenantId(tenantId)).thenReturn(1L);
		when(organizationRepository.existsByTenantIdAndCode(tenantId, "warehouse-b")).thenReturn(false);
		when(organizationRepository.save(any(Organization.class))).thenAnswer(invocation -> invocation.getArgument(0));

		OrganizationDetail detail = organizationService.createOrganization(tenantId, "Warehouse B");

		ArgumentCaptor<Organization> captor = ArgumentCaptor.forClass(Organization.class);
		verify(organizationRepository).save(captor.capture());
		assertThat(captor.getValue().getCode()).isEqualTo("warehouse-b");
		assertThat(captor.getValue().getTenantId()).isEqualTo(tenantId);
		assertThat(captor.getValue().isActive()).isTrue();
		assertThat(detail.name()).isEqualTo("Warehouse B");
	}

	@Test
	void createOrganizationAppendsANumericSuffixWhenTheTenantAlreadyHasThatCode() {
		UUID tenantId = UUID.randomUUID();
		when(tenantRepository.findByIdForUpdate(tenantId)).thenReturn(Optional.of(tenantWithLimit(tenantId, 5)));
		when(organizationRepository.countByTenantId(tenantId)).thenReturn(1L);
		when(organizationRepository.existsByTenantIdAndCode(tenantId, "warehouse-b")).thenReturn(true);
		when(organizationRepository.existsByTenantIdAndCode(tenantId, "warehouse-b-2")).thenReturn(false);
		when(organizationRepository.save(any(Organization.class))).thenAnswer(invocation -> invocation.getArgument(0));

		organizationService.createOrganization(tenantId, "Warehouse B");

		ArgumentCaptor<Organization> captor = ArgumentCaptor.forClass(Organization.class);
		verify(organizationRepository).save(captor.capture());
		assertThat(captor.getValue().getCode()).isEqualTo("warehouse-b-2");
	}

	@Test
	void createOrganizationRejectsCreationAtOrOverTheTenantLimitWithoutSaving() {
		UUID tenantId = UUID.randomUUID();
		when(tenantRepository.findByIdForUpdate(tenantId)).thenReturn(Optional.of(tenantWithLimit(tenantId, 2)));
		when(organizationRepository.countByTenantId(tenantId)).thenReturn(2L);

		assertThatThrownBy(() -> organizationService.createOrganization(tenantId, "Third"))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
		verify(organizationRepository, never()).save(any());
	}

}
