package com.reviewtap.common;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.URI;
import java.net.URISyntaxException;

/** URL absoluta, exclusivamente HTTPS, con host y sin credenciales. Evita open redirects hacia destinos arbitrarios. */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = HttpsUrl.Validator.class)
public @interface HttpsUrl {

    String message() default "Debe ser una URL válida que empiece por https://";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<HttpsUrl, String> {

        @Override
        public boolean isValid(String value, ConstraintValidatorContext ctx) {
            if (value == null || value.isBlank()) {
                return true; // @NotBlank decide si es obligatorio
            }
            return isValidHttpsUrl(value);
        }

        public static boolean isValidHttpsUrl(String value) {
            if (value.length() > 2048 || value.chars().anyMatch(Character::isWhitespace)) {
                return false;
            }
            try {
                URI uri = new URI(value);
                return "https".equalsIgnoreCase(uri.getScheme())
                        && uri.getHost() != null
                        && !uri.getHost().isBlank()
                        && uri.getUserInfo() == null;
            } catch (URISyntaxException e) {
                return false;
            }
        }
    }
}
