# 🎨 Frontend Integration Guide

> **Complete guide for integrating React frontend with Spring Boot backend**

---

## Overview

The frontend communicates with the backend via REST API using:
- **HTTP Client:** Axios-like fetch wrapper
- **State Management:** TanStack Query (React Query)
- **Authentication:** JWT tokens in localStorage
- **Type Safety:** TypeScript interfaces matching backend DTOs

---

## Quick Setup

### 1. Install Dependencies

```bash
cd src/mwanzo-platform-main
npm install
```

### 2. Configure API URL

**File:** `.env.local`
```env
VITE_API_BASE_URL=http://localhost:8080/api/v1
```

### 3. Start Development Server

```bash
npm run dev
```

---

## API Client Architecture

### Core API Client

**File:** `src/services/api/client.ts`

```typescript
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

// Centralized API client with automatic auth
async function apiClient<T>(
  endpoint: string,
  options: RequestInit = {}
): Promise<T> {
  const token = localStorage.getItem('mwanzo_auth_token');

  const headers: HeadersInit = {
    'Content-Type': 'application/json',
    ...(token && { Authorization: `Bearer ${token}` }),
    ...options.headers,
  };

  const response = await fetch(`${API_BASE_URL}${endpoint}`, {
    ...options,
    headers,
  });

  if (response.status === 401) {
    // Auto-logout on unauthorized
    localStorage.removeItem('mwanzo_auth_token');
    window.location.href = '/login';
    throw new Error('Unauthorized');
  }

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || 'Request failed');
  }

  return response.json();
}
```

---

## Key Integration Flows

### 1. Authentication Flow

**Login:**
```typescript
// src/services/api/authService.ts
export async function login(email: string, password: string) {
  const response = await apiClient<AuthResponse>('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
  });

  // Store token
  localStorage.setItem('mwanzo_auth_token', response.token);

  return response;
}
```

**Usage in Component:**
```typescript
// src/pages/Login.tsx
const handleLogin = async (e: React.FormEvent) => {
  e.preventDefault();
  try {
    const response = await login(email, password);
    navigate('/dashboard');
  } catch (error) {
    toast.error('Invalid credentials');
  }
};
```

---

### 2. Video Upload Flow (Instructor)

**Complete flow with presigned URLs:**

```typescript
// src/pages/UploadVideo.tsx
async function uploadVideo(
  file: File,
  thumbnail: File,
  metadata: VideoMetadata
) {
  // Step 1: Get presigned URLs
  const [videoUrl, thumbnailUrl] = await Promise.all([
    apiClient<UploadUrlResponse>('/videos/upload-url/video', {
      method: 'POST',
      body: JSON.stringify({
        fileName: file.name,
        contentType: file.type,
      }),
    }),
    apiClient<UploadUrlResponse>('/videos/upload-url/thumbnail', {
      method: 'POST',
      body: JSON.stringify({
        fileName: thumbnail.name,
        contentType: thumbnail.type,
      }),
    }),
  ]);

  // Step 2: Upload files directly to S3
  await Promise.all([
    fetch(videoUrl.presignedUrl, {
      method: 'PUT',
      body: file,
      headers: { 'Content-Type': file.type },
    }),
    fetch(thumbnailUrl.presignedUrl, {
      method: 'PUT',
      body: thumbnail,
      headers: { 'Content-Type': thumbnail.type },
    }),
  ]);

  // Step 3: Create video record in database
  const video = await apiClient<VideoDTO>(`/videos/sections/${metadata.sectionId}/videos`, {
    method: 'POST',
    body: JSON.stringify({
      courseId: metadata.courseId,
      title: metadata.title,
      description: metadata.description,
      displayOrder: metadata.displayOrder,
      s3Key: videoUrl.s3Key,
      thumbnailS3Key: thumbnailUrl.s3Key,
      isPreview: metadata.isPreview,
    }),
  });

  return video;
}
```

**UI Component:**
```typescript
export function VideoUploadForm() {
  const [uploading, setUploading] = useState(false);
  const [progress, setProgress] = useState(0);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setUploading(true);

    try {
      const video = await uploadVideo(videoFile, thumbnailFile, {
        courseId,
        sectionId,
        title,
        description,
        displayOrder,
        isPreview,
      });

      toast.success('Video uploaded successfully!');
      navigate(`/instructor/courses/${courseId}`);
    } catch (error) {
      toast.error('Upload failed');
    } finally {
      setUploading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit}>
      <input
        type="file"
        accept="video/*"
        onChange={(e) => setVideoFile(e.target.files?.[0])}
      />
      <input
        type="file"
        accept="image/*"
        onChange={(e) => setThumbnailFile(e.target.files?.[0])}
      />
      <button disabled={uploading}>
        {uploading ? `Uploading... ${progress}%` : 'Upload Video'}
      </button>
    </form>
  );
}
```

---

### 3. Course Enrollment Flow

**Student enrolls in course:**

```typescript
// src/services/api/enrollmentService.ts
export async function enrollInCourse(
  courseId: string,
  paymentMethod: 'FREE' | 'MPESA',
  phoneNumber?: string
) {
  return apiClient<EnrollmentDTO>(`/enrollments/${courseId}`, {
    method: 'POST',
    body: JSON.stringify({
      courseId,
      paymentMethod,
      phoneNumber,
    }),
  });
}
```

**For Paid Courses (M-Pesa Flow):**

```typescript
// src/components/MpesaPaymentModal.tsx
export function MpesaPaymentModal({ course, onSuccess }: Props) {
  const [phoneNumber, setPhoneNumber] = useState('');
  const [polling, setPolling] = useState(false);

  const handlePayment = async () => {
    // 1. Enroll (creates pending enrollment)
    const enrollment = await enrollInCourse(
      course.id,
      'MPESA',
      phoneNumber
    );

    // 2. Initiate payment
    const payment = await apiClient<PaymentResponse>('/payments', {
      method: 'POST',
      body: JSON.stringify({
        enrollmentId: enrollment.id,
        phoneNumber,
      }),
    });

    toast.info('Check your phone for M-Pesa prompt');

    // 3. Poll payment status
    setPolling(true);
    const intervalId = setInterval(async () => {
      const status = await apiClient<PaymentStatusResponse>(
        `/payments/status/${payment.transactionReference}`
      );

      if (status.paymentStatus === 'SUCCESS') {
        clearInterval(intervalId);
        setPolling(false);
        toast.success('Payment successful!');
        onSuccess();
      } else if (status.paymentStatus === 'FAILED') {
        clearInterval(intervalId);
        setPolling(false);
        toast.error('Payment failed');
      }

      if (!status.shouldContinuePolling) {
        clearInterval(intervalId);
        setPolling(false);
      }
    }, 3000); // Poll every 3 seconds

    // Timeout after 5 minutes
    setTimeout(() => {
      clearInterval(intervalId);
      setPolling(false);
      toast.error('Payment timeout');
    }, 300000);
  };

  return (
    <Dialog>
      <DialogContent>
        <h2>Complete Payment</h2>
        <p>Course: {course.title}</p>
        <p>Price: KES {course.price}</p>

        <Input
          type="tel"
          placeholder="254712345678"
          value={phoneNumber}
          onChange={(e) => setPhoneNumber(e.target.value)}
        />

        <Button onClick={handlePayment} disabled={polling}>
          {polling ? 'Processing...' : 'Pay with M-Pesa'}
        </Button>
      </DialogContent>
    </Dialog>
  );
}
```

---

### 4. Video Player with Progress Tracking

**Optimized progress tracking (saves every 15s or on key events):**

```typescript
// src/components/VideoPlayer.tsx
export function VideoPlayer({ video, studentId }: Props) {
  const videoRef = useRef<HTMLVideoElement>(null);
  const [currentTime, setCurrentTime] = useState(video.lastPositionSeconds || 0);
  const lastSaveTime = useRef(0);

  // Load last position on mount
  useEffect(() => {
    if (videoRef.current && video.lastPositionSeconds) {
      videoRef.current.currentTime = video.lastPositionSeconds;
    }
  }, []);

  // Save progress on key events
  const saveProgress = useCallback(async (position: number) => {
    await apiClient(`/videos/${video.id}/progress`, {
      method: 'POST',
      body: JSON.stringify({
        studentId,
        positionSeconds: Math.floor(position),
      }),
    });
    lastSaveTime.current = Date.now();
  }, [video.id, studentId]);

  // Track time updates
  const handleTimeUpdate = () => {
    const time = videoRef.current?.currentTime || 0;
    setCurrentTime(time);

    // Save every 15 seconds
    if (Date.now() - lastSaveTime.current > 15000) {
      saveProgress(time);
    }
  };

  // Save on pause
  const handlePause = () => {
    saveProgress(videoRef.current?.currentTime || 0);
  };

  // Save on page unload (before closing tab)
  useEffect(() => {
    const handleUnload = () => {
      // Use sendBeacon for reliability
      navigator.sendBeacon(
        `${API_BASE_URL}/videos/progress/batch`,
        JSON.stringify({
          studentId,
          updates: [{
            videoId: video.id,
            positionSeconds: Math.floor(videoRef.current?.currentTime || 0),
          }],
        })
      );
    };

    window.addEventListener('beforeunload', handleUnload);
    return () => window.removeEventListener('beforeunload', handleUnload);
  }, []);

  return (
    <video
      ref={videoRef}
      src={video.streamingUrl}
      poster={video.thumbnailUrl}
      controls
      onTimeUpdate={handleTimeUpdate}
      onPause={handlePause}
      onEnded={() => saveProgress(video.durationSeconds)}
    />
  );
}
```

---

### 5. Job Posting Flow (Employer)

```typescript
// src/pages/PostJob.tsx
export function PostJob() {
  const [formData, setFormData] = useState<JobFormData>({
    title: '',
    description: '',
    company: '',
    location: '',
    type: 'FULL_TIME',
    salary: '',
    requiredCourses: [],
  });

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    try {
      const job = await apiClient<JobDTO>('/jobs', {
        method: 'POST',
        body: JSON.stringify(formData),
      });

      toast.success('Job posted successfully!');
      navigate(`/jobs/${job.id}`);
    } catch (error) {
      toast.error('Failed to post job');
    }
  };

  return (
    <form onSubmit={handleSubmit}>
      <Input
        label="Job Title"
        value={formData.title}
        onChange={(e) => setFormData({ ...formData, title: e.target.value })}
      />

      <Textarea
        label="Description"
        value={formData.description}
        onChange={(e) => setFormData({ ...formData, description: e.target.value })}
      />

      <Select
        label="Required Courses"
        multiple
        value={formData.requiredCourses}
        onChange={(courses) => setFormData({ ...formData, requiredCourses: courses })}
      >
        {/* Load courses from API */}
      </Select>

      <Button type="submit">Post Job</Button>
    </form>
  );
}
```

---

### 6. Admin Course Approval Flow

```typescript
// src/pages/admin/CourseApproval.tsx
export function CourseApproval() {
  const [pendingCourses, setPendingCourses] = useState<CourseDTO[]>([]);

  useEffect(() => {
    loadPendingCourses();
  }, []);

  const loadPendingCourses = async () => {
    const courses = await apiClient<CourseDTO[]>('/admin/courses?status=PENDING');
    setPendingCourses(courses);
  };

  const handleApprove = async (courseId: string) => {
    await apiClient(`/admin/courses/${courseId}/approve`, {
      method: 'POST',
    });
    toast.success('Course approved');
    loadPendingCourses();
  };

  const handleReject = async (courseId: string, reason: string) => {
    await apiClient(`/admin/courses/${courseId}/reject`, {
      method: 'POST',
      body: JSON.stringify({ reason }),
    });
    toast.success('Course rejected');
    loadPendingCourses();
  };

  return (
    <div>
      <h1>Pending Course Approvals</h1>
      {pendingCourses.map((course) => (
        <div key={course.id}>
          <h3>{course.title}</h3>
          <p>Instructor: {course.instructorName}</p>
          <Button onClick={() => handleApprove(course.id)}>Approve</Button>
          <Button onClick={() => handleReject(course.id, 'Reason...')}>Reject</Button>
        </div>
      ))}
    </div>
  );
}
```

---

## State Management with React Query

### Setup

```typescript
// src/main.tsx
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 5 * 60 * 1000, // 5 minutes
      cacheTime: 10 * 60 * 1000, // 10 minutes
      refetchOnWindowFocus: false,
    },
  },
});

root.render(
  <QueryClientProvider client={queryClient}>
    <App />
  </QueryClientProvider>
);
```

### Custom Hooks

```typescript
// src/hooks/useCourses.ts
export function useCourses(params?: CourseQueryParams) {
  return useQuery({
    queryKey: ['courses', params],
    queryFn: () => apiClient<PageResponse<CourseDTO>>('/courses', {
      method: 'GET',
      // Add query params
    }),
  });
}

// Usage
const { data, isLoading, error } = useCourses({ page: 0, size: 12 });
```

---

## Error Handling

### Global Error Handler

```typescript
// src/utils/errorHandling.ts
export function handleApiError(error: unknown) {
  if (error instanceof Error) {
    toast.error(error.message);
  } else {
    toast.error('An unexpected error occurred');
  }
}
```

### Usage in Components

```typescript
try {
  await someApiCall();
} catch (error) {
  handleApiError(error);
}
```

---

## Type Safety

### TypeScript Interfaces

**File:** `src/types/api.types.ts`

```typescript
// Matches backend DTOs exactly
export interface CourseDTO {
  id: string;
  title: string;
  slug: string;
  shortDescription: string;
  thumbnailUrl: string;
  price: number;
  originalPrice?: number;
  level: 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED';
  // ... all other fields
}

export interface VideoDTO {
  id: string;
  sectionId: string;
  title: string;
  videoUrl: string;
  streamingUrl: string;
  durationSeconds: number;
  isPreview: boolean;
  isCompleted?: boolean;
  progressPercentage?: number;
  // ... all other fields
}
```

---

## Performance Optimization

### 1. Code Splitting

```typescript
// Lazy load routes
const Dashboard = lazy(() => import('./pages/Dashboard'));
const CourseDetail = lazy(() => import('./pages/CourseDetail'));

<Route
  path="/dashboard"
  element={
    <Suspense fallback={<Loading />}>
      <Dashboard />
    </Suspense>
  }
/>
```

### 2. Image Optimization

```typescript
<img
  src={course.thumbnailUrl}
  alt={course.title}
  loading="lazy"
  className="w-full h-48 object-cover"
/>
```

### 3. Debounced Search

```typescript
const debouncedSearch = useMemo(
  () =>
    debounce((query: string) => {
      searchCourses(query);
    }, 300),
  []
);
```

---

## Testing Integration

### API Mocking

```typescript
// src/tests/mocks/handlers.ts
import { rest } from 'msw';

export const handlers = [
  rest.post('/api/v1/auth/login', (req, res, ctx) => {
    return res(
      ctx.json({
        token: 'mock-token',
        userId: '123',
        name: 'Test User',
      })
    );
  }),
];
```

### Component Tests

```typescript
// src/pages/Login.test.tsx
test('login form submits correctly', async () => {
  render(<Login />);

  fireEvent.change(screen.getByLabelText('Email'), {
    target: { value: 'test@example.com' },
  });
  fireEvent.change(screen.getByLabelText('Password'), {
    target: { value: 'password' },
  });

  fireEvent.click(screen.getByRole('button', { name: /login/i }));

  await waitFor(() => {
    expect(window.location.pathname).toBe('/dashboard');
  });
});
```

---

## Maintaining Brand Theme

### Theme Configuration

**File:** `tailwind.config.ts`

```typescript
export default {
  theme: {
    extend: {
      colors: {
        primary: '#2563eb',      // Blue
        secondary: '#8b5cf6',    // Purple
        accent: '#14b8a6',       // Teal/Turquoise
        gold: '#f59e0b',         // Gold for ratings
        success: '#10b981',      // Green
        error: '#ef4444',        // Red
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
      },
    },
  },
};
```

### Consistent Components

Use shadcn/ui components for consistency:
- Buttons → `<Button variant="primary" />`
- Forms → `<Input />`, `<Textarea />`, `<Select />`
- Dialogs → `<Dialog />`, `<AlertDialog />`
- Cards → `<Card />`
- Toast → `toast.success()`, `toast.error()`

---

## Deployment

### Environment Variables (Vercel)

```bash
# Vercel Dashboard → Environment Variables
VITE_API_BASE_URL=https://api.mwanzoskills.co.ke/api/v1
```

### Build Command

```bash
npm run build
```

### Deploy

```bash
vercel --prod
```

---

## Troubleshooting

### CORS Errors
- Check backend `CorsConfig.java` allows frontend domain
- Ensure credentials are included: `credentials: 'include'`

### 401 Errors
- Token expired → Implement refresh token flow
- Token invalid → User must re-login

### Video Not Playing
- Check CloudFront signed URL is valid
- Ensure user has access (enrolled in course)

---

**Last Updated:** January 18, 2026
