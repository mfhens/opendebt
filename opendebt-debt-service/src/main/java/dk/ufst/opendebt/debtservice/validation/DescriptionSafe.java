package dk.ufst.opendebt.debtservice.validation;

import java.lang.annotation.*;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Validates that a description field complies with PSRM GDPR requirements:
 *
 * <ul>
 *   <li>Maximum 100 characters
 *   <li>No CPR numbers (10 consecutive digits)
 *   <li>No PII of third parties
 * </ul>
 */
@Documented
@Constraint(validatedBy = DescriptionSafeValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface DescriptionSafe {
  String message() default "Description contains invalid content (max 100 chars, no CPR numbers)";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
