package com.example.demo.api;

/** Consistent JSON error payload returned by both MVC handlers and security filters. */
public record ApiErrorResponse(ErrorDetail error) {
    public record ErrorDetail(String code, String message) { }

    public static ApiErrorResponse of(String code, String message) {
        return new ApiErrorResponse(new ErrorDetail(code, message));
    }
}
