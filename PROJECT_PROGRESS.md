# Photo Album Service — Project Progress

## Project Goal

Build a Java + Spring Boot REST backend for a photo album service while learning:

- Core Java
- Object-oriented programming
- Spring Boot
- REST API design
- Dependency injection
- JSON serialization/deserialization
- Persistence with PostgreSQL and JPA/Hibernate
- Concurrency and thread pools
- Race-condition handling
- Async processing
- Later: S3, queues, Redis, and distributed-system concepts

---

## Current Tech Stack

- Java 21
- Spring Boot
- Maven
- IntelliJ IDEA
- Git + GitHub

GitHub repository:

`git@github.com:zhengyicoding/photo-album-service.git`

---

## Current Project Structure

```text
src/main/java/com/example/photoalbum
├── PhotoAlbumServiceApplication.java
├── controller
│   └── AlbumController.java
├── dto
│   └── CreateAlbumRequest.java
├── model
│   └── Album.java
└── service
    └── AlbumService.java
```

The application currently uses in-memory storage with `ConcurrentHashMap`.

---

## Current Architecture

```text
HTTP Request
     ↓
AlbumController
     ↓
AlbumService
     ↓
ConcurrentHashMap
```

Later, the in-memory map will be replaced by:

```text
HTTP Request
     ↓
Controller
     ↓
Service
     ↓
Repository
     ↓
PostgreSQL
```

---

## Implemented REST Endpoints

### Create an album

```http
POST /albums
Content-Type: application/json

{
  "name": "Vacation"
}
```

Expected response:

```http
201 Created
```

Example JSON:

```json
{
  "id": "generated-uuid",
  "name": "Vacation"
}
```

---

### Get all albums

```http
GET /albums
```

Example response:

```json
[
  {
    "id": "generated-uuid",
    "name": "Vacation"
  }
]
```

If there are no albums:

```json
[]
```

---

### Get one album by ID

```http
GET /albums/{id}
```

If found:

```http
200 OK
```

with an album JSON body.

If not found:

```http
404 Not Found
```

---

## Album Model

Current idea:

```java
public class Album {

    private String id;
    private String name;

    public Album(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
```

Important idea:

- `Album` is a normal Java object.
- Spring/Jackson serializes it into JSON when returned from a REST controller.

---

## Request DTO

```java
public record CreateAlbumRequest(String name) {
}
```

The client sends:

```json
{
  "name": "Vacation"
}
```

Spring deserializes that JSON into:

```java
CreateAlbumRequest
```

Then:

```java
request.name()
```

returns the album name.

---

## AlbumService

Current responsibility:

- Generate a UUID
- Create an `Album`
- Store it in memory
- Retrieve one album
- Retrieve all albums

Current storage:

```java
private final Map<String, Album> albums =
        new ConcurrentHashMap<>();
```

Why `ConcurrentHashMap` instead of `HashMap`:

Spring Boot can process multiple HTTP requests on different threads, so concurrent access must be considered.

---

## AlbumController

Current controller design:

```java
@RestController
@RequestMapping("/albums")
public class AlbumController {

    private final AlbumService albumService;

    public AlbumController(AlbumService albumService) {
        this.albumService = albumService;
    }

    @PostMapping
    public ResponseEntity<Album> createAlbum(
            @RequestBody CreateAlbumRequest request) {

        Album album =
                albumService.createAlbum(request.name());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(album);
    }

    @GetMapping
    public List<Album> getAllAlbums() {
        return albumService.getAllAlbums();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Album> getAlbum(
            @PathVariable String id) {

        Album album = albumService.getAlbum(id);

        if (album == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(album);
    }
}
```

---

## Spring Concepts Learned

### `@SpringBootApplication`

Marks the main Spring Boot application and triggers Spring Boot configuration/component scanning.

---

### `@RestController`

Marks a class as a REST controller.

A Java return value such as:

```java
List<Album>
```

is still a Java object inside the program.

Spring then serializes it into JSON for the HTTP response.

---

### `@RequestMapping("/albums")`

Sets the base URL for the controller.

---

### `@GetMapping`

Maps an HTTP GET request to a Java method.

Examples:

```java
@GetMapping
```

with the class-level `/albums` mapping means:

```http
GET /albums
```

And:

```java
@GetMapping("/{id}")
```

means:

```http
GET /albums/{id}
```

---

### `@PostMapping`

Maps an HTTP POST request to a Java method.

---

### `@RequestBody`

Tells Spring to deserialize the HTTP request body into a Java object.

```text
JSON
 ↓
Jackson
 ↓
CreateAlbumRequest
```

---

### `@PathVariable`

Reads a value from the URL path.

```text
/albums/abc123
        ↓
id = "abc123"
```

---

### `ResponseEntity`

Lets the controller explicitly control:

- HTTP status
- HTTP response body

Examples:

```java
ResponseEntity.ok(album);
```

means:

```text
200 OK
+ album body
```

```java
ResponseEntity
        .status(HttpStatus.CREATED)
        .body(album);
```

means:

```text
201 Created
+ album body
```

```java
ResponseEntity.notFound().build();
```

means:

```text
404 Not Found
+ no response body
```

---

## Java Concepts Learned

### `private final`

Example:

```java
private final AlbumService albumService;
```

`private`:

- Encapsulates the dependency inside the controller.

`final`:

- The reference is assigned once and cannot later point to another `AlbumService`.

This works well with constructor injection.

---

### Constructor Injection

```java
public AlbumController(AlbumService albumService) {
    this.albumService = albumService;
}
```

Spring creates/manages the `AlbumService` and injects it into the controller.

---

### Java Records

```java
public record CreateAlbumRequest(String name) {
}
```

A concise way to represent immutable request data.

---

### `List<Album>`

This:

```java
public List<Album> getAllAlbums()
```

means the Java method returns a list containing `Album` objects.

It does not itself mean JSON.

JSON conversion happens afterward because Spring is handling the method as part of a `@RestController`.

---

### UUID

Used to generate unique album IDs:

```java
UUID.randomUUID().toString();
```

---

## Serialization vs Deserialization

### Serialization

```text
Java object
    ↓
Jackson
    ↓
JSON
```

Example:

```java
Album
```

becomes:

```json
{
  "id": "...",
  "name": "Vacation"
}
```

### Deserialization

```text
JSON
    ↓
Jackson
    ↓
Java object
```

Example:

```json
{
  "name": "Vacation"
}
```

becomes:

```java
CreateAlbumRequest
```

---

## REST Backend vs Server-Rendered Website

This project is a REST backend.

A REST backend generally returns data such as JSON:

```json
{
  "id": "123",
  "name": "Vacation"
}
```

A server-rendered website returns complete HTML generated on the server.

For this project, Spring Boot is responsible for:

- HTTP APIs
- Business logic
- Database access
- Validation
- Concurrency
- Background processing

It is not currently responsible for building the visual frontend.

---

## IntelliJ HTTP Testing

A `requests.http` file can be used to test the API:

```http
### Create Vacation album
POST http://localhost:8080/albums
Content-Type: application/json

{
  "name": "Vacation"
}


### Create Family album
POST http://localhost:8080/albums
Content-Type: application/json

{
  "name": "Family"
}


### Get all albums
GET http://localhost:8080/albums
```

Important:

- Blank line separates HTTP headers from request body.
- `###` separates different HTTP requests.

---

## Git Progress

Initial Spring Boot project and first REST endpoint have been committed and pushed.

Typical future workflow:

```bash
git status
git add .
git commit -m "Describe the milestone"
git push
```

Suggested commit milestones:

- Add album REST endpoints
- Add validation and exception handling
- Add repository layer
- Add PostgreSQL persistence
- Add photo endpoints
- Add async image processing
- Add concurrency-safe sequence handling

---

## Important Design Ideas Learned

### Controller vs Service

Controller:

- Handles HTTP
- Reads request data
- Chooses HTTP status codes
- Returns responses

Service:

- Contains business logic
- Creates albums
- Retrieves albums
- Later coordinates database/storage operations

Good mental model:

```text
Controller = HTTP concerns
Service    = business logic
Repository = data access
```

---

### Java Concurrency vs Distributed Concurrency

Java concurrency asks:

> What if two threads execute this code at the same time?

Distributed concurrency asks:

> What if two different application servers execute this operation at the same time?

This distinction will matter later when implementing photo sequence numbers and concurrent uploads.

---

## Current Limitation

Albums are stored only in memory.

If the application stops or restarts:

```text
ConcurrentHashMap contents
        ↓
      lost
```

This is intentional for the current learning stage.

---

## Next Steps

### Immediate

1. Verify all three album endpoints.
2. Add request validation.
3. Improve `404` handling with a custom exception.
4. Add a repository layer.

### Persistence

5. Add Spring Data JPA.
6. Add PostgreSQL.
7. Convert `Album` into a persistent entity.
8. Replace `ConcurrentHashMap` with a repository.

### Photo functionality

9. Create `Photo` model/entity.
10. Add photo upload metadata endpoint.
11. Associate photos with albums.
12. Add per-album sequence numbers.

### Concurrency

13. Reproduce a sequence-number race condition.
14. Learn `synchronized`, locks, atomics, and thread safety.
15. Understand why in-process locking is insufficient across multiple servers.
16. Implement a database-safe atomic sequence solution.

### Async processing

17. Configure a Spring thread pool.
18. Use `@Async`.
19. Process photos in background workers.
20. Compare synchronous vs asynchronous requests.

### Production-style extensions

21. Add S3 for object storage.
22. Add SQS or another durable queue.
23. Add retry/idempotency handling.
24. Explore Redis caching.
25. Add Docker and deployment configuration.

---

## Resume Prompt

If continuing this project in another ChatGPT conversation, use:

> I am building a Java 21 + Spring Boot photo album REST API. Please use the attached `PROJECT_PROGRESS.md` as the source of truth for what I have already built and learned. Continue from the Next Steps section and explain the Java/Spring concepts as we implement them.
