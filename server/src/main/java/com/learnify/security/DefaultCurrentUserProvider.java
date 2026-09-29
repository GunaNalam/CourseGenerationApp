package com.learnify.security;

import com.learnify.entity.User;
import com.learnify.repository.UserRepository;
import java.util.UUID;

/**
 * Single-user-mode implementation of {@link CurrentUserProvider} — always resolves to one
 * system default user. Not a Spring bean; {@link JwtCurrentUserProvider} is the active
 * implementation. Swap back to this one (re-add {@code @Component} there instead) to run
 * without Auth0.
 */
public class DefaultCurrentUserProvider implements CurrentUserProvider {

    private static final String DEFAULT_AUTH0_SUB = "default|single-user-mode";
    private static final String DEFAULT_EMAIL = "default@learnify.local";

    private final UserRepository userRepository;

    public DefaultCurrentUserProvider(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UUID currentUserId() {
        return userRepository.findByAuth0Sub(DEFAULT_AUTH0_SUB)
            .orElseGet(this::createDefaultUser)
            .getId();
    }

    private User createDefaultUser() {
        User user = new User();
        user.setAuth0Sub(DEFAULT_AUTH0_SUB);
        user.setEmail(DEFAULT_EMAIL);
        return userRepository.save(user);
    }
}
