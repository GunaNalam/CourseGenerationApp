package com.learnify.security;

import com.learnify.entity.User;
import com.learnify.repository.UserRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class AuthenticatedUserResolver {

    private final UserRepository userRepository;

    public AuthenticatedUserResolver(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User resolve(Jwt jwt) {
        return userRepository.findByAuth0Sub(jwt.getSubject())
            .orElseGet(() -> createUser(jwt));
    }

    private User createUser(Jwt jwt) {
        User user = new User();
        user.setAuth0Sub(jwt.getSubject());
        user.setEmail(jwt.getClaimAsString("email"));
        return userRepository.save(user);
    }
}
