package ERP.erpbackend.identity;

public record PermissionResponse(String code, String resource, PermissionAction action) {
}
