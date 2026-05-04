package com.aziz.demosec.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = TunisianDeliveryAddressValidator.class)
public @interface ValidTunisianDeliveryAddress {
    String message() default "Delivery address must contain a recognized Tunisian city (e.g. Tunis, Sousse, Sfax, Nabeul…)";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
