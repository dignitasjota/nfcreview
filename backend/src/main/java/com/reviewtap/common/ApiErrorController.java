package com.reviewtap.common;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sustituye la respuesta de error por defecto de Spring Boot (con timestamp/path) por el mismo
 * formato {@link ApiError} del resto de la API, para los errores que ocurren antes de llegar a un
 * controlador (rutas mal formadas, fallos del contenedor, etc.).
 */
@RestController
public class ApiErrorController implements ErrorController {

    @RequestMapping("/error")
    public ResponseEntity<ApiError> error(HttpServletRequest request) {
        Object statusAttr = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int status = statusAttr instanceof Integer i ? i : 500;
        HttpStatus resolved = HttpStatus.resolve(status);
        if (resolved == null) {
            resolved = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        String code = switch (resolved) {
            case NOT_FOUND -> "NOT_FOUND";
            case BAD_REQUEST -> "BAD_REQUEST";
            case UNAUTHORIZED -> "UNAUTHORIZED";
            case FORBIDDEN -> "FORBIDDEN";
            case METHOD_NOT_ALLOWED -> "METHOD_NOT_ALLOWED";
            default -> resolved.is5xxServerError() ? "INTERNAL_ERROR" : "ERROR";
        };
        String message = resolved.is5xxServerError() ? "Se ha producido un error inesperado" : resolved.getReasonPhrase();
        return ResponseEntity.status(resolved).body(ApiError.of(code, message));
    }
}
