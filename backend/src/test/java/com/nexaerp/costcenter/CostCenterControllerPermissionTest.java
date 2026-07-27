package com.nexaerp.costcenter;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CostCenterControllerPermissionTest {

    @Test
    void endpointsUseExpectedPermissions() throws NoSuchMethodException {
        assertPermission("getAll", "hasAuthority('VIEW_COST_CENTER')");
        assertPermission("lookup", "hasAuthority('LOOKUP_COST_CENTER')");
        assertPermission("create", "hasAuthority('CREATE_COST_CENTER')",
                com.nexaerp.costcenter.dto.CostCenterRequestDto.class);
        assertPermission("deactivate", "hasAuthority('DEACTIVATE_COST_CENTER')", Long.class);
    }

    private void assertPermission(String method, String expected, Class<?>... parameterTypes)
            throws NoSuchMethodException {
        PreAuthorize annotation = CostCenterController.class
                .getMethod(method, parameterTypes)
                .getAnnotation(PreAuthorize.class);
        assertEquals(expected, annotation.value());
    }
}
