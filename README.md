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

### 5. JwtAuthEntryPoint

Handles what happens when an unauthenticated/unauthorized request hits a protected endpoint. Sits in `security/JwtAuthEntryPoint.java`.

```java
package com.api.libraryreservation.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;

@Component
public class JwtAuthEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write(
                objectMapper.writeValueAsString(Map.of("error", "Unauthorized"))
        );
    }
}
```

**Why each piece is there, in plain terms:**

- `AuthenticationEntryPoint` — a Spring Security interface. By default, when someone hits a protected endpoint without valid auth, Spring Security sends a plain/blank 401 or redirects to a login page. This interface lets you override that with your own response.
- `commence(...)` — the one method you implement. Spring Security calls it automatically whenever authentication fails or is missing on a secured request.
- `response.setStatus(SC_UNAUTHORIZED)` — sends back a proper `401` status code.
- `response.setContentType("application/json")` — since this is an API (not a browser app with login pages), we want JSON back, not HTML.
- `objectMapper.writeValueAsString(Map.of("error", "Unauthorized"))` — writes a clean JSON body like `{"error": "Unauthorized"}` instead of a stack trace or empty response, so frontend/API clients get something predictable to handle.

In short: without this, a missing/invalid token gives a messy default error. With this, it gives a clean, consistent JSON 401 response.

### 6. JwtAuthenticationFilter

Runs on every request, checks for a JWT, and tells Spring Security who the caller is if the token is valid. Sits in `security/JwtAuthenticationFilter.java`.

```java
package com.api.libraryreservation.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if(header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            try {
                Claims claims = jwtUtil.parseToken(token);

                String username = claims.getSubject();
                String role = claims.get("role", String.class);

                List<SimpleGrantedAuthority> authorites = List.of(new SimpleGrantedAuthority("ROLE_" + role));

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(username, null, authorites);

                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException ex) {
                // invalid/expired token - SecurityContext stays empty
                // AuthorizationFilter will reject downstream, JwtAuthEntryPoint handles it
            }
        }

        filterChain.doFilter(request, response);
    }
}
```

**Why each piece is there, in plain terms:**

- `extends OncePerRequestFilter` — a Spring Security base class guaranteeing this filter runs exactly once per request, regardless of internal forwards/dispatches. This gets plugged into the security filter chain (in `SecurityConfig`, before the default auth filter).
- `request.getHeader("Authorization")` + `startsWith("Bearer ")` — JWTs are sent as `Authorization: Bearer <token>`. This checks that format and strips off `"Bearer "` (7 chars) to get the raw token.
- `jwtUtil.parseToken(token)` — this is where `JwtUtil` from step 4 gets used: verifies the signature and decodes the claims (username, role, expiry).
- `claims.getSubject()` / `claims.get("role", String.class)` — pulls the username and role back out of the token, exactly as they were put in during `generateToken`.
- `new SimpleGrantedAuthority("ROLE_" + role)` — Spring Security expects roles prefixed with `ROLE_` (its convention for `hasRole("X")` checks to work).
- `UsernamePasswordAuthenticationToken(username, null, authorites)` — builds an "already authenticated" object. `null` is the credentials field (no password needed here — the valid JWT *is* the proof).
- `SecurityContextHolder.getContext().setAuthentication(authentication)` — this is the actual "log the user in for this request" step. From here on, Spring Security treats this request as coming from an authenticated user with that role.
- `catch (JwtException ex) { }` — if the token is invalid/expired, we deliberately do nothing here. The `SecurityContext` just stays empty, so the request proceeds unauthenticated — it's `JwtAuthEntryPoint` (step 5) that handles rejecting it downstream, not this filter.
- `filterChain.doFilter(request, response)` — always called at the end, whether auth succeeded or not, so the request keeps moving through the rest of the filter chain.

In short: this filter is the bridge between "here's a JWT in the header" and "Spring Security now knows who you are for this request."

### 7. Spring Security internal flow (traced end-to-end)

Tracing one concrete request end-to-end — `alice` reserving book id `1` — to understand what actually happens internally.

**The request:**
```bash
curl -X POST http://localhost:8080/api/books/1/reserve \
  -H "Authorization: Bearer <alice's token>"
```

**Step-by-step internal flow:**

1. **SecurityContextHolderFilter** — creates a fresh, empty `SecurityContext` for this request. `SecurityContextHolder.getContext().getAuthentication()` → `null`, right now.
2. **CsrfFilter** — disabled, skipped.
3. **LogoutFilter** — not a logout request, passes through.
4. **Your `JwtAuthenticationFilter`** (`doFilterInternal`)
    - reads header: `Bearer eyJhbGc...`
    - `jwtUtil.parseToken(token)` → verifies signature using `signingKey`, checks expiry
    - extracts `username="alice"`, `role="MEMBER"` from claims
    - builds `new UsernamePasswordAuthenticationToken("alice", null, [SimpleGrantedAuthority("ROLE_MEMBER")])`
    - `SecurityContextHolder.getContext().setAuthentication(thatObject)`
    - `filterChain.doFilter(...)` — hands off control
5. **UsernamePasswordAuthenticationFilter** — Spring's built-in one, irrelevant here, passes through.
6. **ExceptionTranslationFilter** — wraps everything downstream, ready to catch auth exceptions if they occur.
7. **AuthorizationFilter**
    - checks `SecurityConfig` rules, top to bottom, for path `/api/books/1/reserve`, method `POST`
    - matches `.anyRequest().authenticated()` (since it's not `/api/auth/login` or `GET /api/books` or `POST /api/books`)
    - reads `SecurityContextHolder.getContext().getAuthentication()` → `Authenticated=true`, `Principal="alice"`, `Authorities=[ROLE_MEMBER]`
    - rule only requires "authenticated", not a specific role → **PASSES**, allows request to proceed
8. **DispatcherServlet** — matches `/api/books/{id}/reserve` POST → finds `BookController.reserveBook(id=1)` → invokes it
9. **Inside `reserveBook(1)`:**
    - `bookStore.get(1)` → finds the book
    - checks `availableCopies > 0` → true
    - `String username = SecurityContextHolder.getContext().getAuthentication().getName();` — a fresh, independent read of the same object set back in step 4; `.getName()` on `UsernamePasswordAuthenticationToken` returns the principal, `"alice"`
    - decrements `availableCopies`, creates a `Reservation` record tied to `"alice"`
    - returns `200` with the reservation

**What the `SecurityContext`'s contents actually look like at step 7/9** (if printed with `System.out.println`):
```
SecurityContextImpl [
  Authentication=UsernamePasswordAuthenticationToken [
    Principal=alice,
    Credentials=[PROTECTED]  (actually null, masked in toString)
    Authenticated=true,
    Details=null,
    Granted Authorities=[ROLE_MEMBER]
  ]
]
```
This is one single object (`SecurityContextImpl`, holding one `Authentication` object inside it) that lives for exactly this one request's lifetime, on this one thread, reachable from any code via `SecurityContextHolder.getContext()` — no need to pass it as a parameter anywhere. That's exactly why the controller method can just call `SecurityContextHolder.getContext().getAuthentication().getName()` without `username` being passed in as a method argument from anywhere.

**Which `SecurityConfig` line made step 7 pass:**
```java
.requestMatchers("/api/auth/login").permitAll()
.requestMatchers(HttpMethod.GET, "/api/books").permitAll()
.requestMatchers(HttpMethod.POST, "/api/books").hasRole("LIBRARIAN")
.anyRequest().authenticated()   // ← THIS is the rule that matched "/api/books/1/reserve"
```
Since `/api/books/1/reserve` doesn't match any of the three specific rules above it, it falls through to `.anyRequest().authenticated()` — meaning any authenticated user, regardless of role, can reserve a book. That's consistent with BR-004 ("logged-in MEMBER" only) — no `.hasRole("MEMBER")` restriction was needed specifically, though one could be added later if librarians should be barred from reserving too.

**Contrast: what happens if `alice` tries `POST /api/books` (add a book) — a 403 case**

Step 7 changes:
```
→ checks SecurityConfig rules for path "/api/books", method POST
→ matches ".requestMatchers(POST, "/api/books").hasRole("LIBRARIAN")"
→ reads SecurityContext → Authorities=[ROLE_MEMBER]
→ rule requires ROLE_LIBRARIAN specifically → alice doesn't have it → REJECTED
→ throws AccessDeniedException
→ caught by ExceptionTranslationFilter (step 6, which was just "waiting" around everything downstream)
→ since alice WAS authenticated (just not authorized), this does NOT go to JwtAuthEntryPoint
   (that's only for "not authenticated at all")
→ instead, Spring's default AccessDeniedHandler fires → produces 403
→ DispatcherServlet is NEVER reached — request stops here
```

**The precise mechanical difference:**
- **401** — no valid `SecurityContext` at all → handled by `JwtAuthEntryPoint`
- **403** — valid `SecurityContext`, but insufficient authority → handled by Spring's default `AccessDeniedHandler` (uncustomized, so it's the plain default response)
