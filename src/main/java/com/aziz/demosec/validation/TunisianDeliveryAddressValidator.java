package com.aziz.demosec.validation;

import com.aziz.demosec.Entities.DeliveryType;
import com.aziz.demosec.dto.pharmacy.PharmacyOrderRequestDTO;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Set;

public class TunisianDeliveryAddressValidator
        implements ConstraintValidator<ValidTunisianDeliveryAddress, PharmacyOrderRequestDTO> {

    private static final Set<String> SUPPORTED_CITIES = Set.of(
        "tunis", "sfax", "sousse", "kairouan", "bizerte", "gabes", "gabès",
        "ariana", "gafsa", "monastir", "ben arous", "kasserine",
        "medenine", "médenine", "nabeul", "beja", "béja", "jendouba",
        "mahdia", "siliana", "zaghouan", "tozeur", "kebili", "kébili",
        "tataouine", "hammamet", "djerba"
    );

    @Override
    public boolean isValid(PharmacyOrderRequestDTO dto, ConstraintValidatorContext ctx) {
        // Only validate for HOME_DELIVERY
        if (dto == null || dto.getDeliveryType() != DeliveryType.HOME_DELIVERY) {
            return true;
        }

        String address = dto.getDeliveryAddress();
        if (address == null || address.isBlank()) {
            return true; // @NotNull already covers this
        }

        String trimmed = address.trim();

        // Require "Street, City" format with comma
        if (!trimmed.contains(",")) {
            return fail(ctx,
                "Incomplete address. Required format: \"Street/Avenue, City\" " +
                "(e.g. \"Avenue Bourguiba, Tunis\")");
        }

        String[] parts = trimmed.split(",");
        String streetPart = trimmed.substring(0, trimmed.lastIndexOf(",")).trim();
        String cityPart = parts[parts.length - 1].trim();

        if (streetPart.length() < 3) {
            return fail(ctx, "Street name is too short. Please enter a complete street name.");
        }

        if (cityPart.length() < 2) {
            return fail(ctx, "City name is missing after the comma.");
        }

        // City must be recognized in the last segment
        boolean cityFound = SUPPORTED_CITIES.stream()
                .anyMatch(city -> cityPart.toLowerCase().contains(city));

        if (!cityFound) {
            return fail(ctx,
                "\"" + cityPart + "\" is not a recognized Tunisian city. " +
                "Supported: Tunis, Sousse, Sfax, Ariana, Nabeul, Monastir, Gabès, Bizerte, Kairouan…");
        }

        return true;
    }

    private boolean fail(ConstraintValidatorContext ctx, String message) {
        ctx.disableDefaultConstraintViolation();
        ctx.buildConstraintViolationWithTemplate(message)
           .addPropertyNode("deliveryAddress")
           .addConstraintViolation();
        return false;
    }
}
