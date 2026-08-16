package com.issuetracker.security;

import java.lang.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

/** Injects the authenticated {@link AuthUser} into controller methods. */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@AuthenticationPrincipal
public @interface CurrentUser {}
