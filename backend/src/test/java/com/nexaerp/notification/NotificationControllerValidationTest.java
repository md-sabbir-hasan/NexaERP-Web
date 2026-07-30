package com.nexaerp.notification;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class NotificationControllerValidationTest {

    @Test
    void paginationParametersHaveBackwardCompatibleBounds() throws NoSuchMethodException {
        Method method = NotificationController.class.getMethod(
                "getNotifications", int.class, int.class, boolean.class);
        Parameter page = method.getParameters()[0];
        Parameter size = method.getParameters()[1];

        Min pageMin = page.getAnnotation(Min.class);
        Min sizeMin = size.getAnnotation(Min.class);
        Max sizeMax = size.getAnnotation(Max.class);

        assertNotNull(pageMin);
        assertNotNull(sizeMin);
        assertNotNull(sizeMax);
        assertEquals(0, pageMin.value());
        assertEquals(1, sizeMin.value());
        assertEquals(100, sizeMax.value());
    }
}
