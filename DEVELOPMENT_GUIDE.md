# Medi-D Development - Quick Start Guide

## Prerequisites

- Java 17+ (Spring Boot 3.5.10)
- Maven 3.8+
- PostgreSQL 14+ (for production)
- Git

---

## Setup Instructions

### 1. Clone Repository
```bash
git clone <repository-url>
cd medi-d
```

### 2. Build Project
```bash
mvn clean install
```

### 3. Run Application
```bash
mvn spring-boot:run
```

Application starts at: `http://localhost:8080`

---

## Project Structure

```
medi-d/
├── src/
│   ├── main/
│   │   ├── java/com/medid/
│   │   │   ├── config/              # Configuration classes
│   │   │   ├── controller/          # REST controllers (5 files)
│   │   │   ├── service/             # Service interfaces & implementations (10 files)
│   │   │   ├── dto/                 # Data Transfer Objects (10 files)
│   │   │   ├── entity/              # JPA entities (5 files)
│   │   │   ├── repository/          # Spring Data repositories (5 files)
│   │   │   ├── exception/           # Custom exceptions
│   │   │   └── MediDApplication.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       └── java/com/medid/
│           └── MediDApplicationTests.java
├── pom.xml
├── mvnw
└── README.md
```

---

## Key Architecture

### Layered Architecture

```
┌─────────────────┐
│   REST API      │ (Controllers)
├─────────────────┤
│   Service Layer │ (Interfaces + Implementations)
├─────────────────┤
│   Repository    │ (Data Access)
├─────────────────┤
│   Entity        │ (Database Mapping)
└─────────────────┘
```

### Design Patterns Used

1. **Dependency Injection** - Spring @Autowired
2. **Repository Pattern** - Data access abstraction
3. **Service Pattern** - Business logic encapsulation
4. **Interface Segregation** - Service interfaces
5. **DTO Pattern** - Request/Response separation
6. **Singleton Pattern** - Spring beans

---

## Service Layer Overview

### Service Interfaces (5)

| Service | Operations | Interface |
|---------|-----------|-----------|
| Patient | Get, Create | `IPatientService` |
| Medicine | CRUD, Search | `IMedicineService` |
| Appointment | CRUD, Schedule | `IAppointmentService` |
| Prescription | CRUD, Workflow | `IPrescriptionService` |
| PrescriptionItem | CRUD, Item Mgmt | `IPrescriptionItemService` |

### Implementation Pattern

```java
@Slf4j
@Service
@Transactional
public class MedicineService implements IMedicineService {
    
    @Autowired
    private MedicineRepository medicineRepository;
    
    @Override
    public MedicineResponseDTO getMedicineById(Long id) {
        // 1. Validate input
        // 2. Log operation
        // 3. Query repository
        // 4. Handle not found
        // 5. Convert to DTO
        // 6. Return result
    }
}
```

---

## API Endpoints Summary

### Patient Management
- `GET /api/patients/{id}` - Get patient
- `POST /api/patients` - Create patient

### Medicine Management
- `GET /api/medicines/{id}` - Get medicine
- `GET /api/medicines/search/by-name?name=xxx` - Search
- `GET /api/medicines/low-stock?minimumStock=50` - Stock query
- `POST /api/medicines` - Create
- `PUT /api/medicines/{id}` - Update
- `DELETE /api/medicines/{id}` - Delete

### Appointment Management
- `GET /api/appointments/{id}` - Get appointment
- `GET /api/appointments/patient/{patientId}` - Patient appointments
- `GET /api/appointments/doctor/{doctorId}` - Doctor appointments
- `GET /api/appointments/check-slot` - Slot availability
- `POST /api/appointments` - Create
- `PUT /api/appointments/{id}` - Update
- `DELETE /api/appointments/{id}` - Delete

### Prescription Management
- `GET /api/prescriptions/{id}` - Get prescription
- `GET /api/prescriptions/pending` - Pending prescriptions
- `GET /api/prescriptions/dispensed` - Dispensed prescriptions
- `GET /api/prescriptions/patient/{patientId}` - Patient prescriptions
- `POST /api/prescriptions` - Create
- `PUT /api/prescriptions/{id}` - Update
- `DELETE /api/prescriptions/{id}` - Delete

### Prescription Item Management
- `GET /api/prescription-items/{id}` - Get item
- `GET /api/prescription-items/prescription/{prescriptionId}` - Items by prescription
- `GET /api/prescription-items/medicine/{medicineId}` - Items by medicine
- `POST /api/prescription-items` - Create
- `PUT /api/prescription-items/{id}` - Update
- `DELETE /api/prescription-items/{id}` - Delete

---

## Database Schema

### Tables

```sql
patients (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(255),
  phone_number VARCHAR(20),
  email VARCHAR(255),
  address VARCHAR(500)
)

medicines (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(255),
  stock INTEGER,
  expiry_date DATE,
  version INTEGER
)

appointments (
  id BIGSERIAL PRIMARY KEY,
  patient_id BIGINT FK,
  doctor_id BIGINT,
  slot_timestamp TIMESTAMP,
  status VARCHAR(50)
)

prescriptions (
  id BIGSERIAL PRIMARY KEY,
  appointment_id BIGINT FK (UNIQUE),
  diagnosis VARCHAR(500),
  status VARCHAR(50),
  created_at TIMESTAMP
)

prescription_items (
  id BIGSERIAL PRIMARY KEY,
  prescription_id BIGINT FK,
  medicine_id BIGINT FK,
  quantity_required INTEGER
)
```

---

## Common Development Tasks

### Adding a New Endpoint

1. **Create DTO** (if needed)
```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewEntityRequestDTO {
    private String field1;
}
```

2. **Create Service Interface**
```java
public interface INewService {
    NewEntityResponseDTO create(NewEntityRequestDTO dto);
}
```

3. **Implement Service**
```java
@Service
@Transactional
public class NewService implements INewService {
    @Override
    public NewEntityResponseDTO create(NewEntityRequestDTO dto) {
        // Validation, business logic, database call
    }
}
```

4. **Create Controller**
```java
@RestController
@RequestMapping("/api/new-entity")
public class NewController {
    @Autowired
    private INewService newService;
    
    @PostMapping
    public ResponseEntity<NewEntityResponseDTO> create(
        @RequestBody NewEntityRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(newService.create(dto));
    }
}
```

### Testing a Service

```java
@DataJpaTest
public class NewServiceTests {
    
    @Autowired
    private NewRepository repository;
    
    private INewService service;
    
    @BeforeEach
    void setUp() {
        service = new NewService();
        // Mock repository
    }
    
    @Test
    void testCreate() {
        NewEntityRequestDTO request = new NewEntityRequestDTO("value");
        NewEntityResponseDTO response = service.create(request);
        assertNotNull(response.getId());
    }
}
```

### Adding Validation

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntityRequestDTO {
    
    @NotBlank(message = "Field is required")
    @Size(min = 3, max = 100)
    private String name;
    
    @NotNull(message = "ID is required")
    @Positive(message = "ID must be positive")
    private Long id;
    
    @Email(message = "Valid email required")
    private String email;
}
```

---

## Logging Guide

### Log Levels

```java
@Slf4j
@Service
public class ExampleService {
    
    public void exampleMethod() {
        // DEBUG: Low-level details for developers
        log.debug("Starting operation with params: {}", params);
        
        // INFO: Important business events
        log.info("User created with ID: {}", userId);
        
        // WARN: Potentially harmful situations
        log.warn("Resource not found, using default: {}", default);
        
        // ERROR: Error events with potential continuance
        log.error("Failed to process request", exception);
    }
}
```

### Configuration

```properties
# application.properties
logging.level.root=INFO
logging.level.com.medid=DEBUG
logging.pattern.console=%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n
```

---

## Exception Handling

### Custom Exceptions

```java
// ResourceNotFoundException.java
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}

// InvalidRequestException.java
public class InvalidRequestException extends RuntimeException {
    public InvalidRequestException(String message) {
        super(message);
    }
}
```

### Usage in Services

```java
@Service
public class ExampleService {
    
    public void getEntity(Long id) {
        if (id == null || id <= 0) {
            throw new InvalidRequestException("ID must be positive");
        }
        
        return repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Entity not found with ID: " + id));
    }
}
```

### Global Exception Handler

```java
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
        ResourceNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse("Not Found", e.getMessage()));
    }
    
    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRequest(
        InvalidRequestException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse("Bad Request", e.getMessage()));
    }
}
```

---

## Dependency Injection Best Practices

### Correct (Interface-based)
```java
@Service
public class ConsumerService {
    @Autowired
    private IMedicineService medicineService;  // Depends on interface
}
```

### Avoid (Class-based)
```java
@Service
public class ConsumerService {
    @Autowired
    private MedicineService medicineService;  // Tight coupling
}
```

### Benefits of Interface Dependency

- ✅ Easy to test (mock implementations)
- ✅ Easy to extend (alternative implementations)
- ✅ Loose coupling
- ✅ Single responsibility

---

## Transaction Management

### Service-Level Transactions

```java
@Service
@Transactional  // All public methods are transactional
public class EntityService {
    
    @Override
    public void complexOperation() {
        // Multiple database operations
        repository1.save(entity1);
        repository2.save(entity2);
        // Automatic rollback if any method throws exception
    }
    
    @Transactional(readOnly = true)  // Optimization for queries
    public List<Entity> getAll() {
        return repository.findAll();
    }
}
```

### Key Points

- ✅ Automatic rollback on exception
- ✅ Automatic commit on success
- ✅ ACID compliance
- ✅ Cascade operation support

---

## Testing Strategy

### Unit Tests
```bash
mvn test -Dtest=EntityServiceTests
```

### Integration Tests
```bash
mvn test -Dtest=*IntegrationTests
```

### Test Coverage
```bash
mvn test jacoco:report
# Report: target/site/jacoco/index.html
```

---

## Environment Configurations

### Development
```properties
# application.properties
spring.datasource.url=jdbc:postgresql://localhost:5432/medid_dev
spring.jpa.hibernate.ddl-auto=update
logging.level.com.medid=DEBUG
```

### Production
```properties
# application-prod.properties
spring.datasource.url=jdbc:postgresql://db-host:5432/medid_prod
spring.jpa.hibernate.ddl-auto=validate
logging.level.com.medid=INFO
```

### Testing
```properties
# application-test.properties
spring.datasource.url=jdbc:h2:mem:testdb
spring.jpa.hibernate.ddl-auto=create-drop
logging.level.com.medid=DEBUG
```

---

## Performance Tips

### 1. Use Derived Query Methods
```java
// ✅ Optimized
List<Medicine> findByStockLessThanEqual(Integer stock);

// ❌ Not optimized
@Query("SELECT m FROM Medicine m WHERE m.stock <= :stock")
List<Medicine> findLowStock(@Param("stock") Integer stock);
```

### 2. Use Read-Only Transactions
```java
@Transactional(readOnly = true)
public List<Medicine> getAllMedicines() {
    return medicineRepository.findAll();
}
```

### 3. Implement Pagination
```java
// Future enhancement
Page<Medicine> page = medicineRepository.findAll(PageRequest.of(0, 20));
```

### 4. Use Caching
```java
// Future enhancement
@Cacheable("medicines")
public Medicine getMedicineById(Long id) {
    return medicineRepository.findById(id).orElse(null);
}
```

---

## Compilation Verification

### Clean Build
```bash
mvn clean compile
```

### Quick Verify
```bash
mvn compile -q
```

### With Warnings
```bash
mvn compile -Wall
```

---

## Troubleshooting

### Build Issues

**Problem:** `[ERROR] COMPILATION ERROR`
```bash
# Solution: Clean and rebuild
mvn clean install -DskipTests
```

**Problem:** `[ERROR] Dependency conflicts`
```bash
# Solution: Update dependencies
mvn dependency:tree
mvn dependency:resolve
```

### Runtime Issues

**Problem:** `EntityNotFoundException`
```java
// Solution: Check if entity exists before accessing
Medicine medicine = medicineRepository.findById(id)
    .orElseThrow(() -> new ResourceNotFoundException("Medicine not found"));
```

**Problem:** `DataIntegrityViolationException`
```java
// Solution: Validate foreign keys exist
if (!patientRepository.existsById(patientId)) {
    throw new InvalidRequestException("Patient not found");
}
```

---

## IDE Setup

### IntelliJ IDEA

1. Open project
2. Configure JDK 17+
3. Mark `src/main/java` as Sources
4. Mark `src/test/java` as Test Sources
5. Install Lombok plugin
6. Enable annotation processing

### VS Code

1. Install Extension Pack for Java
2. Install Spring Boot Extension Pack
3. Create `.vscode/settings.json`:
```json
{
    "java.compile.nullAnalysis.mode": "automatic",
    "[java]": {
        "editor.defaultFormatter": "redhat.java",
        "editor.formatOnSave": true
    }
}
```

---

## Version Control

### Commit Message Format
```
feat: Add new feature
fix: Fix specific bug
docs: Update documentation
style: Code style changes
refactor: Refactor code
test: Add tests
chore: Build/dependency updates
```

### Branch Naming
```
feature/new-feature-name
fix/bug-name
docs/documentation-update
```

---

## Deployment Checklist

- ✓ Code compiled without warnings
- ✓ All tests passing
- ✓ Code reviewed and approved
- ✓ Database migrations ready
- ✓ Environment variables configured
- ✓ Logging properly configured
- ✓ API documentation updated
- ✓ Security validations in place
- ✓ Performance tested
- ✓ Backup strategy planned

---

## Useful Maven Commands

```bash
# Download dependencies
mvn dependency:download-sources

# Generate dependency tree
mvn dependency:tree

# Check for updates
mvn versions:display-dependency-updates

# Format code
mvn spotless:apply

# Run specific test
mvn test -Dtest=TestClassName

# Skip tests during build
mvn install -DskipTests

# Run with specific profile
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"

# Package application
mvn clean package

# Create fat JAR
mvn assembly:assembly
```

---

## Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Data JPA](https://spring.io/projects/spring-data-jpa)
- [Maven Documentation](https://maven.apache.org/)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)

---

## Support

For issues or questions:
1. Check documentation
2. Search existing issues
3. Create new issue with:
   - Error message
   - Steps to reproduce
   - Expected vs actual behavior
   - Environment details

---

**Last Updated:** February 18, 2024
**Version:** 1.0.0
**Status:** Production Ready ✅

