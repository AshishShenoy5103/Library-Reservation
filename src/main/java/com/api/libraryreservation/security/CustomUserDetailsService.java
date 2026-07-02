package com.api.libraryreservation.security;

import jakarta.annotation.PostConstruct;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class CustomUserDetailsService implements UserDetailsService {
    private static class StoredUser {
        String username;
        String passwordHash;
        String role;

        StoredUser(String username, String passwordHash, String role) {
            this.username = username;
            this.passwordHash = passwordHash;
            this.role = role;
        }
    }

    public String getRole(String username) {
        StoredUser storedUser = users.get(username);
        return storedUser != null ? storedUser.role.replace("ROLE_", ""):null;
    }

    private final Map<String, StoredUser> users = new HashMap<>();
    private final PasswordEncoder passwordEncoder;

    public CustomUserDetailsService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    public void seed() {
        users.put("alice", new StoredUser("alice", passwordEncoder.encode("alicepass"), "ROLE_MEMBER"));
        users.put("bob", new StoredUser("alice", passwordEncoder.encode("bobpass"), "ROLE_MEMBER"));
        users.put("charlie", new StoredUser("charlie", passwordEncoder.encode("charliepass"), "ROLE_LIBRARIAN"));
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        StoredUser storedUser = users.get(username);
        if(storedUser == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }

        return org.springframework.security.core.userdetails.User
                .withUsername(storedUser.username)
                .password(storedUser.passwordHash)
                .roles(storedUser.role.replace("ROLE_", ""))
                .build();
    }
}
