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

