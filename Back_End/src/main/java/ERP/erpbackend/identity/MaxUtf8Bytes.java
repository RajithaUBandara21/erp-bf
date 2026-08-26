package ERP.erpbackend.identity;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Bounds a string's UTF-8 encoded byte length, distinct from {@code @Size}
 * which bounds the UTF-16 character count. Needed for fields such as a
 * BCrypt password, whose 72-byte limit is enforced on the encoded bytes.
 */
@Documented
@Constraint(validatedBy = MaxUtf8BytesValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface MaxUtf8Bytes {

	int value();

	String message() default "must not exceed {value} bytes when UTF-8 encoded";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
