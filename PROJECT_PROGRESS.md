# Photo Album Service — Project Progress

## Project Goal

Build a Java 21 + Spring Boot REST backend for a photo album service while learning:

- Core Java and OOP
- Spring Boot and REST API design
- Dependency injection
- JSON serialization/deserialization
- JPA / Hibernate
- PostgreSQL and relational database design
- Repository pattern
- Validation and HTTP error handling
- Concurrency, race conditions, and thread pools
- Async processing
- Later: authentication, S3, queues/SQS, Redis, Docker, and distributed-system concepts

---

## Current Tech Stack

- Java 21
- Spring Boot 4.1.1
- Maven
- IntelliJ IDEA
- Git + GitHub
- PostgreSQL 18
- Spring Data JPA
- Hibernate
- PostgreSQL JDBC driver
- HikariCP connection pool

GitHub repository:

`git@github.com:zhengyicoding/photo-album-service.git`

---

## Current Architecture

```text
HTTP Request
     ↓
Controller
     ↓
Service
     ↓
Repository
     ↓
Spring Data JPA
     ↓
Hibernate
     ↓
JDBC / HikariCP
     ↓
PostgreSQL
```

Current domain relationship:

```text
User
  |
  | one-to-many
  v
Album
```

Each album belongs to exactly one user.

---

## Current Project Structure

Approximate structure:

```text
src/main/java/com/example/photoalbum
├── PhotoAlbumServiceApplication.java
├── controller
│   ├── AlbumController.java
│   └── UserController.java
├── dto
│   ├── CreateAlbumRequest.java
│   └── CreateUserRequest.java
├── model
│   ├── Album.java
│   └── User.java
├── repository
│   ├── AlbumRepository.java
│   └── UserRepository.java
└── service
    ├── AlbumService.java
    └── UserService.java
```

The original in-memory `ConcurrentHashMap` storage has been replaced by PostgreSQL persistence.

---

## PostgreSQL Setup

Local database:

```text
photo_album
```

Local PostgreSQL user:

```text
erinxu
```

Spring configuration:

```properties
spring.application.name=photo-album-service

spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/photo_album}
spring.datasource.username=${DB_USERNAME:erinxu}
spring.datasource.password=${DB_PASSWORD:}

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

Notes:

- `ddl-auto=update` is convenient for local development and learning.
- Production should eventually use database migrations such as Flyway or Liquibase.
- Database passwords/secrets should not be committed to Git.

---

## Persistence Milestone

The application now persists albums in PostgreSQL instead of RAM.

Old design:

```text
AlbumService
    ↓
ConcurrentHashMap
```

Current design:

```text
AlbumService
    ↓
AlbumRepository
    ↓
Spring Data JPA / Hibernate
    ↓
PostgreSQL
```

This means albums survive Spring Boot restarts.

---

## JPA / Hibernate Concepts Learned

### JPA

JPA is the Java Persistence API/specification used to describe how Java objects map to relational database data.

### Hibernate

Hibernate is the JPA implementation currently used by Spring Boot.

### Spring Data JPA

Spring Data JPA provides repository abstractions such as:

```java
save(...)
findById(...)
findAll()
deleteById(...)
```

and can generate queries from repository method names.

Current stack:

```text
Spring Data JPA
      ↓
JPA API/specification
      ↓
Hibernate
      ↓
JDBC
      ↓
PostgreSQL
```

---

## Current `User` Entity

A `User` entity was added so albums can be owned by a specific user.

Conceptually:

```java
@Entity
@Table(name = "users")
public class User {

    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private String email;

    protected User() {
    }

    public User(String id, String email) {
        this.id = id;
        this.email = email;
    }

    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }
}
```

Important database rule:

```text
UNIQUE(email)
```

so two users cannot share the same email.

---

## Current `Album` Entity

`Album` is now a JPA entity and belongs to a user.

Conceptually:

```java
@Entity
@Table(
    name = "albums",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_album_user_name",
            columnNames = {"user_id", "name"}
        )
    }
)
public class Album {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    protected Album() {
    }

    public Album(String id, String name, User user) {
        this.id = id;
        this.name = name;
        this.user = user;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public User getUser() {
        return user;
    }
}
```

Important JPA concepts:

- `@Entity` makes the class persistent.
- `@Id` identifies the primary key.
- `@ManyToOne` means many albums may belong to one user.
- `@JoinColumn(name = "user_id")` creates the foreign-key column used to connect an album to its owner.
- `nullable = false` means an album must have an owner.

---

## Database Schema Verified

Hibernate successfully created the schema in PostgreSQL.

Conceptually:

```text
users
--------------------------------
id          PRIMARY KEY
email       NOT NULL, UNIQUE
```

and:

```text
albums
--------------------------------
id          PRIMARY KEY
name        NOT NULL
user_id     NOT NULL, FOREIGN KEY → users.id
```

Hibernate also created:

```text
UNIQUE(user_id, name)
```

for per-user album-name uniqueness.

---

## Album Name Uniqueness Rule

We decided that album names should be unique **per user**, not globally.

Desired behavior:

```text
Alice + Vacation  → allowed
Bob   + Vacation  → allowed
Alice + Vacation  → duplicate, reject
```

The correct database constraint is:

```text
UNIQUE(user_id, name)
```

Important distinction:

- `user_id` alone is not unique.
- `name` alone is not unique.
- The combination `(user_id, name)` must be unique.

This is a composite unique constraint.

---

## Why the Database Constraint Matters

The service can first check:

```java
albumRepository.existsByUser_IdAndName(userId, name)
```

for a friendly early error.

However, two concurrent requests could both execute the check before either insert finishes:

```text
Request A                   Request B
---------                   ---------
check → not found           check → not found
insert                      insert
```

Therefore the application-level check is not enough by itself.

PostgreSQL must enforce:

```text
UNIQUE(user_id, name)
```

The database constraint is the final correctness guarantee under concurrency.

---

## `UserRepository`

Created:

```java
public interface UserRepository
        extends JpaRepository<User, String> {
}
```

Spring Data JPA provides methods including:

```java
save(...)
findById(...)
findAll()
deleteById(...)
```

---

## `AlbumRepository`

Current intended repository:

```java
public interface AlbumRepository
        extends JpaRepository<Album, String> {

    boolean existsByUser_IdAndName(
            String userId,
            String name
    );

    List<Album> findAllByUser_Id(
            String userId
    );

    Optional<Album> findByIdAndUser_Id(
            String albumId,
            String userId
    );
}
```

### Spring Data derived-query naming lesson

A startup error occurred because this method was initially named:

```java
existByUser_IdAndName(...)
```

Spring Data did not recognize `existBy` as a valid query prefix.

Correct name:

```java
existsByUser_IdAndName(...)
```

Useful derived-query prefixes include:

```text
findBy...
existsBy...
countBy...
deleteBy...
```

---

## `CreateUserRequest`

Created in the `dto` package:

```java
public record CreateUserRequest(String email) {
}
```

Incoming JSON:

```json
{
  "email": "alice@example.com"
}
```

is deserialized by Spring/Jackson into a `CreateUserRequest` object.

---

## `UserService`

Created in the `service` package.

Conceptually:

```java
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User createUser(String email) {

        User user = new User(
                UUID.randomUUID().toString(),
                email
        );

        return userRepository.save(user);
    }
}
```

Responsibilities:

- generate a UUID
- create the `User` entity
- persist the user through `UserRepository`

---

## `UserController`

Created in the `controller` package.

Conceptually:

```java
@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<User> createUser(
            @RequestBody CreateUserRequest request) {

        User user = userService.createUser(request.email());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(user);
    }
}
```

Request flow:

```text
POST /users
    ↓
UserController
    ↓
CreateUserRequest
    ↓
UserService
    ↓
UserRepository
    ↓
PostgreSQL
```

---

## `AlbumService` Direction

Album creation now requires both a user ID and an album name.

Conceptually:

```java
public Album createAlbum(
        String userId,
        String name) {

    User user = userRepository
            .findById(userId)
            .orElseThrow(() ->
                    new RuntimeException("User not found"));

    if (albumRepository
            .existsByUser_IdAndName(userId, name)) {

        throw new RuntimeException(
                "Album name already exists"
        );
    }

    String albumId = UUID.randomUUID().toString();

    Album album = new Album(albumId, name, user);

    return albumRepository.save(album);
}
```

This should later be improved with custom exceptions instead of raw `RuntimeException`.

---

## User-Scoped Album Queries

The service/repository should scope album access by user.

Examples:

```java
public List<Album> getAllAlbums(String userId) {
    return albumRepository.findAllByUser_Id(userId);
}
```

and:

```java
public Album getAlbum(String userId, String albumId) {
    return albumRepository
            .findByIdAndUser_Id(albumId, userId)
            .orElse(null);
}
```

This prevents one user's album query from automatically returning another user's album just because the album UUID is known.

---

## Planned User-Scoped Album API

Until authentication is added, the API should explicitly include `userId` in the path:

```http
POST /users/{userId}/albums
GET  /users/{userId}/albums
GET  /users/{userId}/albums/{albumId}
```

Later, authentication should provide the current user identity instead of trusting arbitrary user IDs supplied by the client.

---

## Earlier Album REST Endpoints

Original API:

```http
POST /albums
GET  /albums
GET  /albums/{id}
```

These endpoints taught:

- `@RestController`
- `@RequestMapping`
- `@PostMapping`
- `@GetMapping`
- `@RequestBody`
- `@PathVariable`
- `ResponseEntity`
- Jackson serialization/deserialization

The API is now being migrated to user-scoped album routes.

---

## HTTP Testing with IntelliJ

Requests are tested using an IntelliJ `.http` file.

Correct example:

```http
### Create Alice
POST http://localhost:8080/users
Content-Type: application/json

{
  "email": "alice@example.com"
}
```

Important formatting rules:

1. Use `Content-Type: application/json` with a colon.
2. Put a blank line between HTTP headers and the JSON request body.
3. Separate different requests with `###`.

---

## HTTP Debugging Lessons Learned

### 400 Bad Request

A malformed request produced:

```text
400 Bad Request
```

because the blank line between HTTP headers and JSON body was missing.

Correct HTTP structure:

```text
request line
headers
blank line
body
```

### 415 Unsupported Media Type

The next request reached Spring but returned:

```text
415 Unsupported Media Type
```

because Spring saw the incoming request as:

```text
Content-Type: */*;charset=UTF-8
```

The request must explicitly send:

```http
Content-Type: application/json
```

This is necessary for Spring/Jackson to deserialize the body into:

```java
@RequestBody CreateUserRequest request
```

At this checkpoint, the next HTTP-testing task is to verify that the corrected `POST /users` request returns `201 Created`.

---

## Spring Concepts Learned So Far

### `@RestController`

Marks a class as a REST controller. Java return values are serialized into the HTTP response body.

### `@RequestBody`

Deserializes an HTTP request body into a Java object.

```text
JSON
 ↓
Jackson
 ↓
Java object
```

### `@PathVariable`

Extracts values from a URL path.

### `ResponseEntity`

Allows explicit control of HTTP status and response body.

Examples:

```java
ResponseEntity.ok(album)
```

→ `200 OK` + body

```java
ResponseEntity.status(HttpStatus.CREATED).body(album)
```

→ `201 Created` + body

```java
ResponseEntity.notFound().build()
```

→ `404 Not Found`

### Dependency Injection

Dependencies are injected through constructors and commonly stored as:

```java
private final AlbumService albumService;
```

### Controller vs Service vs Repository

```text
Controller = HTTP concerns
Service    = business logic
Repository = database access
```

---

## Java Concepts Learned So Far

- classes and objects
- constructors
- `private final`
- constructor injection
- `List<Album>`
- records
- UUID generation
- `Optional`
- interface-based repositories
- generics such as `JpaRepository<Album, String>`

---

## Current Immediate Next Steps

1. Fix/verify the IntelliJ request for `POST /users` so it sends `Content-Type: application/json` correctly.
2. Confirm `POST /users` returns `201 Created`.
3. Create Alice and Bob.
4. Verify both rows exist in the `users` table.
5. Create album `"Vacation"` for Alice.
6. Create album `"Vacation"` for Bob and confirm it is allowed.
7. Try to create a second `"Vacation"` for Alice and confirm it is rejected.
8. Return `409 Conflict` for duplicate album names instead of a generic `500`.
9. Add custom exceptions such as:
   - `UserNotFoundException`
   - `AlbumAlreadyExistsException`
10. Add global exception handling with `@RestControllerAdvice`.
11. Add input validation for:
   - blank/invalid email
   - blank album name
   - album name length

---

## Later Roadmap

### API quality

- Request validation
- Custom exceptions
- Consistent JSON error responses
- `409 Conflict`
- Pagination
- Authentication / authorization

### Photo functionality

- Create `Photo` entity
- Associate photos with albums
- Add photo metadata endpoints
- Add per-album sequence numbers

### Concurrency

- Reproduce a sequence-number race condition
- Learn `synchronized`, locks, and atomics
- Compare application locks vs database constraints/locks
- Implement database-safe sequence assignment

### Async processing

- Configure a Spring thread pool
- Use `@Async`
- Background thumbnail/image processing
- Compare synchronous and asynchronous request flows

### Production-style extensions

- S3 object storage
- SQS or another durable queue
- Retry/idempotency handling
- Redis caching
- Docker
- Flyway/Liquibase migrations

---

## Resume Prompt

If continuing in another ChatGPT conversation:

> I am building a Java 21 + Spring Boot photo album REST API with PostgreSQL, Spring Data JPA, and Hibernate. Users own albums, the database enforces `UNIQUE(user_id, name)`, and `User` repository/service/controller layers have been added. Please use the attached `PROJECT_PROGRESS.md` as the source of truth and continue from the Current Immediate Next Steps section. Explain the Java, Spring, database, REST, and concurrency concepts while implementing them.
