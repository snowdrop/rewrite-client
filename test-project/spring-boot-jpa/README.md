## Spring Boot JPA

This project is a Spring Boot JPA demonstrating how we could use the following annotations with the `AuditingEntityListener` to create for each new record

```java
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
```

the `auditing` information:
```bash
  {
    "createdAt": "2026-03-17T11:25:46.296569",
    "createdBy": "system",
    "updatedAt": "2026-03-17T11:25:46.296569",
    "updatedBy": "system",
    ...
```

## Curl requests

```bash
# Create a user:                                                                                                                                                                                         
curl -s -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"John Doe","email":"john@example.com","phone":"123-456-7890"}' | jq

# Create another user:                                                                                                                                                                                   
curl -s -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Jane Smith","email":"jane@example.com","phone":"098-765-4321"}' | jq

# List all users:
curl -s http://localhost:8080/api/users | jq

# Get a user by ID:
curl -s http://localhost:8080/api/users/1 | jq

# Update a user:
curl -s -X PUT http://localhost:8080/api/users/1 \
  -H "Content-Type: application/json" \
  -d '{"name":"John Updated","email":"john.updated@example.com","phone":"111-222-3333"}' | jq

# Delete a user:
curl -s -X DELETE http://localhost:8080/api/users/1
```

# Commands to be executed to migrate it to Quarkus

```bash

```