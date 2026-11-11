package edu.bits.itsm.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// A label for controller methods: "only these roles may call this".
// Example: @AllowedRoles({Role.ADMIN, Role.TECHNICIAN})
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AllowedRoles {
    Role[] value();
}