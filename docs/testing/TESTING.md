# 🧪 Testing Guide

> **Comprehensive testing strategy for backend and frontend**

---

## Testing Strategy

### Test Pyramid
```
    /\
   /E2E\      ← Few (Slow, Expensive)
  /──────\
 /  API   \   ← Some (Medium Speed)
/──────────\
/ Unit Tests \ ← Many (Fast, Cheap)
```

**Target Coverage:** >80%

---

## Backend Testing (Spring Boot)

### 1. Unit Tests

**Test Services:**
```java
// src/test/java/com/mdaudev/mwanzo/.../service/CourseServiceTest.java
@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CourseService courseService;

    @Test
    void createCourse_Success() {
        // Arrange
        UUID instructorId = UUID.randomUUID();
        CreateCourseRequest request = CreateCourseRequest.builder()
            .title("Test Course")
            .categoryId(UUID.randomUUID())
            .price(BigDecimal.valueOf(5000))
            .build();

        when(categoryRepository.findById(any())).thenReturn(Optional.of(new Category()));
        when(courseRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        // Act
        CourseDetailDTO result = courseService.createCourse(instructorId, request);

        // Assert
        assertNotNull(result);
        assertEquals("Test Course", result.getTitle());
        verify(courseRepository).save(any(Course.class));
    }

    @Test
    void createCourse_CategoryNotFound_ThrowsException() {
        when(categoryRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
            courseService.createCourse(UUID.randomUUID(), new CreateCourseRequest())
        );
    }
}
```

**Run Unit Tests:**
```bash
./mvnw test
```

---

### 2. Integration Tests

**Test Controllers with Real Database:**
```java
// src/test/java/com/mdaudev/mwanzo/.../controller/AuthControllerIntegrationTest.java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase
@Transactional
class AuthControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Test
    void register_Success() {
        RegisterRequest request = new RegisterRequest(
            "John Doe",
            "john@example.com",
            "Password123!",
            "254712345678",
            "STUDENT"
        );

        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
            "/api/v1/auth/register",
            request,
            AuthResponse.class
        );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getToken());
        assertTrue(userRepository.existsByEmail("john@example.com"));
    }

    @Test
    void login_Success() {
        // Create user first
        User user = createTestUser();
        userRepository.save(user);

        LoginRequest request = new LoginRequest("test@example.com", "Password123!");

        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
            "/api/v1/auth/login",
            request,
            AuthResponse.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody().getToken());
    }
}
```

**Run Integration Tests:**
```bash
./mvnw verify
```

---

### 3. Test Data Setup

**Test Configuration:**
```java
// src/test/java/com/mdaudev/mwanzo/.../config/TestConfig.java
@TestConfiguration
public class TestConfig {

    @Bean
    public AmazonS3 s3Client() {
        return mock(AmazonS3.class); // Mock S3 in tests
    }

    @Bean
    public PayHeroService payHeroService() {
        return mock(PayHeroService.class); // Mock payments
    }
}
```

**Test Data Builder:**
```java
// src/test/java/com/mdaudev/mwanzo/.../TestDataBuilder.java
public class TestDataBuilder {

    public static User createTestUser() {
        return User.builder()
            .name("Test User")
            .email("test@example.com")
            .password("$2a$10$...") // BCrypt hash of "Password123!"
            .role(UserRole.STUDENT)
            .isActive(true)
            .build();
    }

    public static Course createTestCourse() {
        return Course.builder()
            .title("Test Course")
            .slug("test-course")
            .description("Test description")
            .price(BigDecimal.valueOf(5000))
            .level(CourseLevel.BEGINNER)
            .status(CourseStatus.PUBLISHED)
            .build();
    }
}
```

---

### 4. Test Database

**Use H2 for Tests:**
```yaml
# src/test/resources/application-test.yml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true

aws:
  endpoint: http://localhost:4566  # LocalStack
```

---

## Frontend Testing (React)

### 1. Unit Tests (Vitest)

**Test Components:**
```typescript
// src/components/CourseCard.test.tsx
import { render, screen } from '@testing-library/react';
import { CourseCard } from './CourseCard';

describe('CourseCard', () => {
  const mockCourse = {
    id: '123',
    title: 'Test Course',
    price: 5000,
    thumbnailUrl: 'https://example.com/image.jpg',
    // ... other required fields
  };

  it('renders course information', () => {
    render(<CourseCard course={mockCourse} />);

    expect(screen.getByText('Test Course')).toBeInTheDocument();
    expect(screen.getByText('KES 5,000')).toBeInTheDocument();
  });

  it('shows discount badge when originalPrice exists', () => {
    const discountedCourse = {
      ...mockCourse,
      originalPrice: 8000,
      discountPercentage: 37,
    };

    render(<CourseCard course={discountedCourse} />);

    expect(screen.getByText('37% OFF')).toBeInTheDocument();
  });
});
```

**Run Unit Tests:**
```bash
npm test
```

---

### 2. Integration Tests

**Test API Integration:**
```typescript
// src/services/api/courseService.test.ts
import { rest } from 'msw';
import { setupServer } from 'msw/node';
import { getCourses } from './courseService';

const server = setupServer(
  rest.get('/api/v1/courses', (req, res, ctx) => {
    return res(
      ctx.json({
        content: [
          { id: '1', title: 'Course 1' },
          { id: '2', title: 'Course 2' },
        ],
        totalElements: 2,
      })
    );
  })
);

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

test('getCourses returns courses', async () => {
  const result = await getCourses({ page: 0, size: 12 });

  expect(result.content).toHaveLength(2);
  expect(result.content[0].title).toBe('Course 1');
});
```

---

### 3. E2E Tests (Cypress)

**Setup:**
```bash
npm install -D cypress
npx cypress open
```

**Test User Flow:**
```typescript
// cypress/e2e/enrollment.cy.ts
describe('Course Enrollment Flow', () => {
  beforeEach(() => {
    // Login first
    cy.login('student@test.com', 'Password123!');
  });

  it('allows student to enroll in free course', () => {
    // Visit course page
    cy.visit('/courses/web-development-basics');

    // Click enroll button
    cy.contains('Enroll Now').click();

    // Should redirect to learning page
    cy.url().should('include', '/learn/');

    // Should see first video
    cy.contains('Welcome Video').should('be.visible');
  });

  it('shows payment modal for paid course', () => {
    cy.visit('/courses/advanced-react');

    cy.contains('Enroll Now').click();

    // Payment modal should appear
    cy.get('[role="dialog"]').should('be.visible');
    cy.contains('Pay with M-Pesa').should('be.visible');
  });
});
```

**Run E2E Tests:**
```bash
npm run test:e2e
```

---

## API Testing (Postman/Newman)

### Postman Collection

**Import Collection:**
```bash
# Located in: ./postman/Mwanzo-API.postman_collection.json
```

**Run with Newman (CLI):**
```bash
npm install -g newman
newman run postman/Mwanzo-API.postman_collection.json \
  --environment postman/dev-environment.json
```

**Test Scripts Example:**
```javascript
// Postman test script
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Response has token", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.token).to.be.a('string');
});

// Save token for subsequent requests
pm.environment.set("auth_token", pm.response.json().token);
```

---

## Test Coverage

### Backend Coverage

**Generate Report:**
```bash
./mvnw clean test jacoco:report
```

**View Report:**
```bash
open target/site/jacoco/index.html
```

### Frontend Coverage

**Generate Report:**
```bash
npm test -- --coverage
```

**View Report:**
```bash
open coverage/index.html
```

---

## CI/CD Integration

### GitHub Actions

```yaml
# .github/workflows/test.yml
name: Test Suite

on: [push, pull_request]

jobs:
  backend-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '17'
      - name: Run tests
        run: ./mvnw verify
      - name: Upload coverage
        uses: codecov/codecov-action@v3

  frontend-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-node@v3
        with:
          node-version: '18'
      - name: Install dependencies
        run: npm ci
      - name: Run tests
        run: npm test
      - name: E2E tests
        run: npm run test:e2e
```

---

## Performance Testing

### Load Testing (K6)

```javascript
// load-test.js
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '2m', target: 100 }, // Ramp up to 100 users
    { duration: '5m', target: 100 }, // Stay at 100 users
    { duration: '2m', target: 0 },   // Ramp down
  ],
};

export default function () {
  const res = http.get('https://api.mwanzoskills.co.ke/api/v1/courses');

  check(res, {
    'status is 200': (r) => r.status === 200,
    'response time < 500ms': (r) => r.timings.duration < 500,
  });

  sleep(1);
}
```

**Run Load Test:**
```bash
k6 run load-test.js
```

---

## Test Accounts

### Development Test Users

| Role | Email | Password | Purpose |
|------|-------|----------|---------|
| Student | student@test.com | Student123! | Test student features |
| Instructor | instructor@test.com | Instructor123! | Test course creation |
| Employer | employer@test.com | Employer123! | Test job posting |
| Admin | admin@test.com | Admin123! | Test admin features |

**⚠️ Change in production!**

---

## Best Practices

### 1. Test Naming
```java
// Good: Describes what is tested and expected outcome
void createCourse_WithValidData_ReturnsCreatedCourse()

// Bad: Vague naming
void test1()
```

### 2. Arrange-Act-Assert (AAA)
```java
@Test
void testExample() {
    // Arrange - Set up test data
    var user = createTestUser();

    // Act - Execute the code under test
    var result = service.doSomething(user);

    // Assert - Verify the outcome
    assertEquals(expected, result);
}
```

### 3. Test Independence
- Each test should run independently
- Don't rely on test execution order
- Clean up after tests

### 4. Mock External Dependencies
- S3 uploads
- Payment gateways
- Email services
- External APIs

---

## Troubleshooting Tests

### Backend Tests Failing
```bash
# Clean build and re-run
./mvnw clean test

# Run single test class
./mvnw test -Dtest=CourseServiceTest

# Run with debug logging
./mvnw test -Dspring.profiles.active=test -Dlogging.level.root=DEBUG
```

### Frontend Tests Failing
```bash
# Clear cache and re-run
npm test -- --clearCache

# Run in watch mode
npm test -- --watch

# Run single test file
npm test -- CourseCard.test.tsx
```

---

**Last Updated:** January 18, 2026
