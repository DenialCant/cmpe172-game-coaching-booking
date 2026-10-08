package com.cmpe172.game_coaching_booking.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class AuthInterceptorTest {

    private final AuthInterceptor interceptor = new AuthInterceptor();

    @Test
    void wrongRoleIsRejectedWith403() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/provider/dashboard");
        request.getSession().setAttribute(SessionKeys.USER_ID, 1);
        request.getSession().setAttribute(SessionKeys.ROLE, "CUSTOMER");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, response, new Object()));
        assertEquals(403, response.getStatus());
    }

    @Test
    void matchingRoleIsAllowed() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/customer/appointments");
        request.getSession().setAttribute(SessionKeys.USER_ID, 1);
        request.getSession().setAttribute(SessionKeys.ROLE, "CUSTOMER");

        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
    }
}
