package com.smartflow.smestocksensebackend.entity;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiPurchaseRequestTest {

    @Test
    void nonAdminManagerSenderBlocked() {
        AiPurchaseRequest request = validRequest();
        request.setSender(employee(RoleCode.EMPLOYEE));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> validate(request));

        assertEquals("sender must be ADMIN or MANAGER.", ex.getMessage());
    }

    @Test
    void recipientMustBeEmployee() {
        AiPurchaseRequest request = validRequest();
        request.setReceiver(employee(RoleCode.MANAGER));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> validate(request));

        assertEquals("receiver must be EMPLOYEE.", ex.getMessage());
    }

    @Test
    void requestedQuantityMustBePositive() {
        AiPurchaseRequest request = validRequest();
        request.setRequestedQuantity(0);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> validate(request));

        assertEquals("requestedQuantity must be positive.", ex.getMessage());
    }

    @Test
    void aiSuggestedQuantityMustBeNonNegative() {
        AiPurchaseRequest request = validRequest();
        request.setAiSuggestedQuantity(-1);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> validate(request));

        assertEquals("aiSuggestedQuantity must be non-negative.", ex.getMessage());
    }

    @Test
    void quantitiesRemainSeparate() throws Exception {
        AiPurchaseRequest request = validRequest();
        request.setAiSuggestedQuantity(70);
        request.setRequestedQuantity(50);

        validate(request);

        assertEquals(70, request.getAiSuggestedQuantity());
        assertEquals(50, request.getRequestedQuantity());
    }

    @Test
    void invalidHorizonBlocked() {
        AiPurchaseRequest request = validRequest();
        request.setHorizonDays((short) 21);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> validate(request));

        assertEquals("horizonDays must be 7, 14, or 30.", ex.getMessage());
    }

    @Test
    void productWarehouseReferencesValidated() {
        AiPurchaseRequest request = validRequest();
        request.setProduct(null);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> validate(request));

        assertEquals("product is required.", ex.getMessage());
    }

    private AiPurchaseRequest validRequest() {
        AiPurchaseRequest request = new AiPurchaseRequest();
        request.setCode("YC-AI-1");
        request.setModelMetadata(new ForecastModelMetadata());
        request.setProduct(new Product());
        request.setWarehouse(new Warehouse());
        request.setHorizonDays((short) 7);
        request.setAiSuggestedQuantity(70);
        request.setRequestedQuantity(50);
        request.setSender(employee(RoleCode.MANAGER));
        request.setReceiver(employee(RoleCode.EMPLOYEE));
        request.setContent("Nhap hang theo de xuat AI");
        return request;
    }

    private Employee employee(RoleCode roleCode) {
        Role role = new Role();
        role.setCode(roleCode);
        Employee employee = new Employee();
        employee.setRole(role);
        return employee;
    }

    private void validate(AiPurchaseRequest request) throws Exception {
        Method method = AiPurchaseRequest.class.getDeclaredMethod("validate");
        method.setAccessible(true);
        try {
            method.invoke(request);
        } catch (java.lang.reflect.InvocationTargetException ex) {
            if (ex.getCause() instanceof Exception cause) {
                throw cause;
            }
            throw ex;
        }
    }
}
