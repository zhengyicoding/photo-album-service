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
│   ├── ApiError.java
│   ├── CreateAlbumRequest.java
│   └── CreateUserRequest.java
├── exception
│   ├── AlbumAlreadyExistsException.java
│   ├── GlobalExceptionHandler.java
│   ├── UserAlreadyExistsException.java
│   └── UserNotFoundException.java
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

The project now also has centralized REST exception handling and Bean Validation for incoming request DTOs.

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

Current repository:

```java
public interface UserRepository
        extends JpaRepository<User, String> {

    boolean existsByEmail(String email);
}
```

Spring Data JPA provides methods including:

```java
save(...)
findById(...)
existsById(...)
findAll()
deleteById(...)
```

and derives the email-existence query from:

```java
existsByEmail(...)
```

This supports an application-level duplicate-email check before inserting a new user.

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

## Request DTO Validation

### `CreateUserRequest`

Current request DTO:

```java
public record CreateUserRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email

) {
}
```

Incoming JSON is deserialized by Spring/Jackson into a `CreateUserRequest`.

### `CreateAlbumRequest`

Current request DTO:

```java
public record CreateAlbumRequest(

        @NotBlank(message = "Album name is required")
        @Size(
                max = 100,
                message = "Album name must be at most 100 characters"
        )
        String name

) {
}
```

Controller parameters use:

```java
@Valid @RequestBody ...
```

so Bean Validation runs after JSON deserialization and before the service method executes.

Validated behavior now tested successfully:

```text
invalid/blank email
→ 400 Bad Request

blank album name
→ 400 Bad Request
```

---

## `UserService`

Created in the `service` package.

Current behavior:

```java
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User createUser(String email) {

        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException(
                    "User already exists: " + email
            );
        }

        User user = new User(
                UUID.randomUUID().toString(),
                email
        );

        return userRepository.save(user);
    }
}
```

Responsibilities:

- check whether the email already exists
- throw a domain-specific exception for duplicate email
- generate a UUID
- create the `User` entity
- persist the user through `UserRepository`

The database also has a unique constraint on `users.email`, so PostgreSQL remains the final correctness guarantee.

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
            @Valid @RequestBody CreateUserRequest request) {

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
JSON deserialization
    ↓
Bean Validation
    ↓
UserController
    ↓
UserService
    ↓
UserRepository
    ↓
PostgreSQL
```

Tested behavior:

```text
new valid user
→ 201 Created

duplicate email
→ 409 Conflict

invalid email
→ 400 Bad Request
```

---

## `AlbumService`

Album creation now requires both a user ID and an album name.

Current behavior is conceptually:

```java
public Album createAlbum(
        String userId,
        String name) {

    User user = userRepository
            .findById(userId)
            .orElseThrow(() ->
                    new UserNotFoundException(
                            "User not found: " + userId
                    ));

    if (albumRepository
            .existsByUser_IdAndName(userId, name)) {

        throw new AlbumAlreadyExistsException(
                "Album name already exists: " + name
        );
    }

    String albumId = UUID.randomUUID().toString();

    Album album = new Album(
            albumId,
            name,
            user
    );

    return albumRepository.save(album);
}
```

Important behavior:

```text
unknown user
→ UserNotFoundException
→ 404 Not Found

same user + duplicate album name
→ AlbumAlreadyExistsException
→ 409 Conflict
```

The application-level `existsByUser_IdAndName(...)` check provides a friendly business error, while PostgreSQL's `UNIQUE(user_id, name)` constraint remains the final correctness guarantee.

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

## Current User-Scoped Album API

The album API is now scoped by user:

```http
POST /users/{userId}/albums
GET  /users/{userId}/albums
GET  /users/{userId}/albums/{albumId}
```

Until authentication is added, `userId` is explicitly supplied in the URL.

Later, authentication should determine the current user identity instead of trusting an arbitrary user ID supplied by the client.

Tested behavior includes:

```text
valid new album for existing user
→ 201 Created

unknown user
→ 404 Not Found

same user + duplicate album name
→ 409 Conflict

different user + same album name
→ 201 Created

blank album name
→ 400 Bad Request
```

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

### 400 Bad Request from malformed HTTP

A malformed request initially produced `400 Bad Request` because the blank line between HTTP headers and the JSON body was missing.

Correct HTTP structure:

```text
request line
headers
blank line
body
```

### 415 Unsupported Media Type

A later request reached Spring but returned `415 Unsupported Media Type` because Spring saw the incoming content type as:

```text
Content-Type: */*;charset=UTF-8
```

The corrected request explicitly sends:

```http
Content-Type: application/json
```

This allows Spring/Jackson to deserialize the JSON body into the request DTO.

### HTTP request testing milestone completed

The corrected user and album requests have now been tested successfully.

Current expected API contract:

```text
POST /users with valid new email
→ 201 Created

POST /users with duplicate email
→ 409 Conflict

POST /users with invalid email
→ 400 Bad Request

POST /users/{userId}/albums with valid new album
→ 201 Created

POST /users/{unknownUserId}/albums
→ 404 Not Found

POST duplicate album for same user
→ 409 Conflict

POST same album name for a different user
→ 201 Created

POST blank album name
→ 400 Bad Request
```

---

## Centralized Exception Handling

Custom exception classes have been added:

```text
UserNotFoundException
UserAlreadyExistsException
AlbumAlreadyExistsException
```

A consistent response DTO has also been introduced:

```java
public record ApiError(
        String error,
        String message
) {
}
```

`GlobalExceptionHandler` uses:

```java
@RestControllerAdvice
```

and methods annotated with:

```java
@ExceptionHandler(...)
```

to translate Java/domain exceptions into meaningful HTTP responses.

Examples:

```text
UserNotFoundException
→ 404 Not Found

UserAlreadyExistsException
→ 409 Conflict

AlbumAlreadyExistsException
→ 409 Conflict
```

This replaces the earlier behavior where a raw `RuntimeException` produced `500 Internal Server Error` for expected business conflicts.

---

## Validation Milestone

Bean Validation is now used on request DTOs.

Important annotations learned:

```java
@NotBlank
@Email
@Size
@Valid
```

Flow:

```text
HTTP JSON
   ↓
Jackson deserialization
   ↓
request DTO
   ↓
@Valid
   ↓
Bean Validation constraints
   ↓
valid? ── yes → controller/service
   |
   no
   ↓
400 Bad Request
```

This separates:

```text
invalid input
→ 400 Bad Request
```

from:

```text
valid input but conflicting business state
→ 409 Conflict
```

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

The user/repository/error-handling/validation milestone is complete and the HTTP tests passed.

Next:

1. Handle the database-level duplicate-album race cleanly:
   - keep `UNIQUE(user_id, name)` as the final correctness guarantee
   - translate Spring's `DataIntegrityViolationException`
   - return `409 Conflict` if concurrent requests race past the service-level existence check
2. Optionally add an `AlbumNotFoundException` so missing albums use the same centralized error-handling style.
3. Introduce a `Photo` JPA entity.
4. Associate each photo with an album.
5. Add a photo metadata/upload endpoint.
6. Add a per-album sequence number.
7. Deliberately reproduce a concurrent sequence-number race.
8. Compare Java in-process synchronization with database-safe concurrency control.

The next major learning phase is:

```text
Photo persistence
    ↓
per-album sequence numbers
    ↓
concurrent requests
    ↓
race conditions
    ↓
database-safe atomic updates
```

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

> I am building a Java 21 + Spring Boot 4.1.1 photo album REST API with PostgreSQL, Spring Data JPA, and Hibernate. Users own albums, PostgreSQL enforces `UNIQUE(user_id, name)`, and the User/Album repository-service-controller layers are implemented. I have also added Bean Validation, custom exceptions, `ApiError`, and global exception handling with `@RestControllerAdvice`. My tests now pass for 201 user/album creation, 400 invalid input, 404 unknown user, 409 duplicate email, 409 duplicate album for the same user, and allowing the same album name for different users. Please use the attached `PROJECT_PROGRESS.md` as the source of truth and continue from the Current Immediate Next Steps section. The next technical step is to handle database-level duplicate races with `DataIntegrityViolationException`, then begin the Photo entity and concurrency/sequence-number work. Explain the Java, Spring, database, REST, and concurrency concepts as we implement them.
