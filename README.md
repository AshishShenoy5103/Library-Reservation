# Library Reservation

Learning JWT via library reservation project.

## Setup

### 1. Add JJWT dependency to `pom.xml`

Add the following to your `pom.xml` under `<dependencies>`:

```xml
<dependencies>
    <!-- JJWT API -->
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
        <version>0.13.0</version>
    </dependency>

    <!-- JJWT Implementation (runtime only) -->
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-impl</artifactId>
        <version>0.13.0</version>
        <scope>runtime</scope>
    </dependency>

    <!-- JJWT Jackson (for JSON parsing, runtime only) -->
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-jackson</artifactId>
        <version>0.13.0</version>
        <scope>runtime</scope>
    </dependency>
</dependencies>
```

> `jjwt-api` is a compile dependency (code against it directly).
> `jjwt-impl` and `jjwt-jackson` are runtime-only.

### 2. Generate and configure the JWT secret

Generate a real 256-bit secret. Run in a terminal:

```bash
openssl rand -base64 32
```

Save that value to `.idea > workspace.xml` as an environment variable named `JWT_SECRET`.

Add to `application.properties`:

```properties
jwt.secret=${JWT_SECRET}
jwt.expiry-ms=${JWT_EXPIRY_MS:3600000}
```

> **Note:** `jwt.secret` has no default value — it fetches from the `JWT_SECRET` env variable, and if it's not set, the app fails to start.
>
> `jwt.expiry-ms` has a default value of `3600000` ms = 1 hour, used if `JWT_EXPIRY_MS` isn't set.

### 3. UserDetailsService

`UserDetailsService` is a core Spring Security interface. Given a username, it fetches the user (from DB or elsewhere) and returns a `UserDetails` object (username, password, authorities/roles, account status flags). Spring Security calls this internally during authentication — you don't call it directly.

It's an interface with one abstract method, so you implement it and override `loadUserByUsername`:

```java
public interface UserDetailsService {
    UserDetails loadUserByUsername(String username) throws UsernameNotFoundException;
}
```

> Used during login (`AuthenticationManager` calls it to verify credentials) and again on later requests when the JWT filter reloads the user to set `SecurityContext` — the token proves who they claim to be, this service confirms they still exist/are valid.

### 4. JwtUtil

Handles creating and reading JWTs. Sits in `security/JwtUtil.java`.

```java
package com.api.libraryreservation.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtUtil {
    @Value("${jwt.secret}")
    private String secretBase64;

    @Value("${jwt.expiry-ms:3600000}")
    private long expiryMs;

    private Key signingKey;

    @PostConstruct
    public void init() {
        byte[] keyBytes = Base64.getDecoder().decode(secretBase64);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(String username, String role) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(username)
                .claim("role", role)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expiryMs))
                .signWith(signingKey)
                .compact();
    }
    
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith((javax.crypto.SecretKey)signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
```

**Why each piece is there, in plain terms:**

- `@Component` — makes this a Spring bean so it can be `@Autowired` anywhere (controllers, filters).
- `@Value("${jwt.secret}")` / `@Value("${jwt.expiry-ms:3600000}")` — pulls in the secret and expiry from `application.properties`, which ultimately come from env vars.
- `@PostConstruct init()` — runs once, right after Spring builds this bean. Decodes the Base64 secret into raw bytes and turns it into a `Key` object via `Keys.hmacShaKeyFor(...)`. We do this once at startup instead of on every request — no point re-decoding the same secret repeatedly.
- `generateToken(username, role)` — builds the actual token: who it's for (`subject`), a custom claim (`role`, so you know their permissions without a DB lookup), when it was issued, when it expires, and signs it with the key so nobody can forge or edit it. `.compact()` turns it into the final JWT string you send to the client.
- `parseToken(token)` — the reverse: takes an incoming token, verifies the signature matches (proving it wasn't tampered with) using the same key, and returns the `Claims` (payload) inside — username, role, expiry, etc. If the signature doesn't match or it's expired, this throws an exception.

In short: `generateToken` runs at login to hand out a token. `parseToken` runs on every protected request to check the token is legit and pull out who's making the request.

