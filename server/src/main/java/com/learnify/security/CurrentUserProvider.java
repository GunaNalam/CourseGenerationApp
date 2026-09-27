package com.learnify.security;

import java.util.UUID;

public interface CurrentUserProvider {

    UUID currentUserId();
}
