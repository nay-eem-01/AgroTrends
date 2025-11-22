package com.project.agriculturalblogapplication.ExceptionHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.agriculturalblogapplication.model.response.HttpResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.Serializable;

@Component
public class AuthenticationExceptionHandler implements AuthenticationEntryPoint, Serializable {
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {

        ObjectMapper mapper = new ObjectMapper();
        HttpResponse errorResponse = new HttpResponse(HttpStatus.FORBIDDEN, "(JWT) " + authException.getMessage(), null, false);
        String responseMsg = mapper.writeValueAsString(errorResponse);
        response.getWriter().write(responseMsg);
        response.setStatus(403);
    }
}
