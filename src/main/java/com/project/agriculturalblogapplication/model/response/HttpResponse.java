package com.project.agriculturalblogapplication.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class HttpResponse {

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


}
