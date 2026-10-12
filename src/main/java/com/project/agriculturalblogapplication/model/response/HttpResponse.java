package com.project.agriculturalblogapplication.model.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class HttpResponse {

    private static final ObjectMapper JSON = new ObjectMapper();

    private HttpStatus status;
    private String message;
    private Object payload;
    private boolean success;

    public HttpResponse(HttpStatus status, boolean success, String message, Object payload) {
        super();
        this.status = status;
        this.success = success;
        this.message = message;
        this.payload = payload;
    }

    public HttpResponse(HttpStatus status, boolean success, String message) {
        super();
        this.status = status;
        this.success = success;
        this.message = message;
    }

    public static ResponseEntity<HttpResponse> getResponseEntity(boolean success, String message, Object payload) {
        return new ResponseEntity<>(new HttpResponse(success ? HttpStatus.OK : HttpStatus.BAD_REQUEST, success, message, payload), success ? HttpStatus.OK : HttpStatus.BAD_REQUEST);
    }

    public static ResponseEntity<HttpResponse> getResponseEntity(boolean success, String message) {
        return new ResponseEntity<>(new HttpResponse(success ? HttpStatus.OK : HttpStatus.BAD_REQUEST, success, message, null), success ? HttpStatus.OK : HttpStatus.BAD_REQUEST);
    }

    public static ResponseEntity<HttpResponse> getResponseEntity(HttpStatus httpStatus, String message, Object payload) {
        return new ResponseEntity<>(new HttpResponse(httpStatus, httpStatus.equals(HttpStatus.OK), message, payload), httpStatus);
    }

    public static ResponseEntity<HttpResponse> getResponseEntity(HttpStatus httpStatus, String message) {
        return new ResponseEntity<>(new HttpResponse(httpStatus, httpStatus.equals(HttpStatus.OK), message, null), httpStatus);
    }

    /** Writes an error envelope from a servlet filter, where no controller advice runs. */
    public static void writeError(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(JSON.writeValueAsString(new HttpResponse(status, false, message)));
    }
}
