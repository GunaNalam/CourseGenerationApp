package com.learnify.admin;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Admin gate for a single-admin-scale project (design/components/09-admin-observability.md)
 * — a configured allowlist of Auth0 `sub` values, not a full role system.
 */
@Component
public class AdminAuthorizer {

    private final Set<String> allowedSubs;

    public AdminAuthorizer(@Value("${admin.allowed-subs:}") String allowedSubsCsv) {
        this.allowedSubs = Arrays.stream(allowedSubsCsv.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toSet());
    }

    public boolean isAdmin(String auth0Sub) {
        return auth0Sub != null && allowedSubs.contains(auth0Sub);
    }
}
