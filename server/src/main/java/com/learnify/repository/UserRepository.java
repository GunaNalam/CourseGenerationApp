package com.learnify.repository;

import com.learnify.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByAuth0Sub(String auth0Sub);
}
