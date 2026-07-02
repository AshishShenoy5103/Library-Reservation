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

