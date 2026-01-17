# Mwanzo Skills Campus - Video System Flow Documentation

## 🎯 System Goals & Design Principles

### Core Goals:
1. **Accessibility**: Easy video access for every Kenyan (low bandwidth support)
2. **Performance**: Minimal database calls, optimized for scale
3. **User Experience**: Smooth playback, resume capability, clear progress
4. **Instructor-Friendly**: Simple upload process, clear management
5. **Cost-Effective**: LocalStack for development, S3 for production

### Design Principles:
- **Batch Progress Updates**: Only save progress on meaningful events (pause, exit, completion)
- **Frontend State Management**: Track progress client-side during playback
- **Preview Support**: Free preview videos for marketing
- **Crash Courses**: Support 1-3 video mini-courses
- **Thumbnail Requirements**: Every video needs thumbnail for UI
- **Mobile-First**: Optimize for mobile data constraints

---

## 👨‍🏫 TUTOR (INSTRUCTOR) FLOW

### Phase 1: Course Creation
```
1. Tutor logs in (role: INSTRUCTOR)
   ↓
2. Click "Create New Course"
   ↓
3. Fill course details:
   - Title, Description, Category
   - Price (or mark as FREE)
   - Course Thumbnail (REQUIRED - 1280x720px recommended)
   - Short Description (for cards)
   - Learning Objectives
   - Requirements
   ↓
4. Click "Save Draft" → Course created with status: DRAFT
```

### Phase 2: Course Structure Setup
```
5. After course saved, redirect to "Course Builder"
   ↓
6. Create Sections (Modules):
   - Click "Add Section"
   - Enter: Section Title, Description, Display Order
   - Example: 
     * Section 1: "Introduction to Python"
     * Section 2: "Variables and Data Types"
     * Section 3: "Control Flow"
   ↓
7. Sections appear in Course Builder sidebar
```

### Phase 3: Video Upload & Management
```
8. Click on a Section to add videos
   ↓
9. Click "Upload Video" button
   ↓
10. Upload Flow (LocalStack/S3):
    
    FRONTEND:
    ├─ Select video file (.mp4, .webm, max 500MB)
    ├─ Generate presigned upload URL from backend
    ├─ Upload directly to S3/LocalStack (client-side)
    ├─ Show upload progress bar
    └─ On upload complete, call backend to create video record
    
    BACKEND:
    ├─ Generate presigned S3 URL (15min expiry)
    ├─ Return URL to frontend
    ├─ Frontend uploads to S3
    ├─ Frontend notifies backend when done
    ├─ Backend creates Video record with status: UPLOADED
    └─ (Optional) Trigger AWS MediaConvert for transcoding
    
11. Fill Video Details:
    - Title (REQUIRED)
    - Description (optional)
    - Display Order (auto-incremented)
    - Duration (auto-detected from file)
    - Upload Thumbnail (REQUIRED - 854x480px)
    - Mark as Preview Video? (YES/NO)
      * If YES: Free for everyone to watch
      * If NO: Only enrolled students can watch
    ↓
12. Click "Save Video"
    ↓
13. Video appears in section with status:
    - UPLOADED: Video uploaded, not yet processed
    - PROCESSING: Being transcoded (if enabled)
    - READY: Available for streaming
    - FAILED: Processing failed, retry needed
```

### Phase 4: Preview & Crash Course Setup

**Preview Videos (Marketing):**
```
14. For marketing purposes, mark 1-2 videos as "Preview"
    - These videos are FREE for all users (even non-enrolled)
    - Display with "FREE PREVIEW" badge
    - Great for:
      * Course introduction
      * Sample lesson
      * Instructor introduction
```

**Crash Courses (1-3 Videos):**
```
15. For short courses (crash courses):
    - Create single section: "Course Content"
    - Upload 1-3 videos covering key topics
    - Mark first video as Preview (optional)
    - Price accordingly (lower price for short courses)
    
    Example:
    Course: "Git Crash Course"
    Section 1: "Essential Git Commands"
      ├─ Video 1: "Git Basics" (10 min) [PREVIEW]
      ├─ Video 2: "Branching & Merging" (15 min)
      └─ Video 3: "GitHub Workflow" (12 min)
    Total: 37 minutes, Price: KSh 499
```

### Phase 5: Course Publishing
```
16. Review course in preview mode
    ↓
17. When ready, click "Publish Course"
    ↓
18. Backend validation:
    - At least 1 section exists
    - At least 1 video exists
    - Course thumbnail exists
    - All videos have thumbnails
    - Price is set (or marked FREE)
    ↓
19. If valid:
    - Course status: DRAFT → PUBLISHED
    - Visible to students in catalog
    ↓
20. If invalid:
    - Show error messages
    - Tutor fixes issues and re-submits
```

### Tutor Dashboard Features:
```
- View total students enrolled
- Track course revenue
- See video view counts
- Monitor course ratings
- Edit published courses (students notified of updates)
- Unpublish course (students keep access if already paid)
```

---

## 👨‍🎓 STUDENT FLOW

### Phase 1: Course Discovery
```
1. Student browses course catalog
   ↓
2. Course cards display:
   - Course Thumbnail
   - Title
   - Instructor Name
   - Rating (stars)
   - Price (or "FREE")
   - Duration (e.g., "5 hours")
   - Number of lectures
   - "PREVIEW AVAILABLE" badge (if preview videos exist)
   ↓
3. Click course card → Course Detail Page
```

### Phase 2: Course Preview (Before Purchase)
```
4. Course Detail Page shows:
   - Course description
   - What you'll learn
   - Requirements
   - Instructor bio
   - Course curriculum (sections & video titles)
   - Reviews/ratings
   - **Preview Videos Section** (if available)
   ↓
5. Preview Videos Section:
   - Shows all videos marked as "Preview"
   - Display with "FREE PREVIEW" badge
   - Click video → Opens video player
   - Can watch full preview video without enrollment
   - Great for deciding whether to buy course
   ↓
6. If interested, click "Enroll Now" or "Buy Course"
```

### Phase 3: Enrollment & Payment
```
7. Click "Enroll Now"
   ↓
8. If FREE course:
   - Instant enrollment
   - Enrollment status: ACTIVE
   - Redirect to "My Courses"
   ↓
9. If PAID course:
   - Show payment page
   - Enter M-Pesa phone number
   - Click "Pay KSh X"
   - Enrollment created with status: PENDING_PAYMENT
   ↓
10. M-Pesa STK Push sent to phone
    ↓
11. Student enters M-Pesa PIN on phone
    ↓
12. Payment completed:
    - Enrollment status: PENDING_PAYMENT → ACTIVE
    - Student can now access all videos
    ↓
13. Redirect to "My Courses" or "Start Learning"
```

### Phase 4: Watching Videos

**Initial Course Access:**
```
14. Student opens course from "My Courses"
    ↓
15. Course Player Page loads:
    
    LEFT SIDEBAR:
    ├─ Course curriculum (all sections & videos)
    ├─ Each video shows:
    │  ├─ Title
    │  ├─ Duration
    │  ├─ Completed checkbox (✓ if watched 80%+)
    │  └─ Last watched position (if partially watched)
    └─ Overall progress: "5 of 20 videos completed (25%)"
    
    MAIN AREA:
    ├─ Video player
    ├─ Video title & description
    ├─ Instructor info
    └─ Navigation: "Previous Video" | "Next Video"
    
    ↓
16. Click first unwatched video (or resume from last position)
```

**Video Playback:**
```
17. Video player loads with:
    - Video streaming URL (CloudFront/LocalStack)
    - Controls: Play/Pause, Volume, Fullscreen, Speed
    - Quality selector: 1080p, 720p, 480p, 360p (adaptive)
    - Resume from last position (if previously watched)
    ↓
18. During playback:
    
    FRONTEND (Client-Side):
    ├─ Track current position every second
    ├─ Store progress in browser state (not DB yet)
    ├─ Update UI progress bar
    ├─ Mark video as "in progress"
    └─ NO database calls during continuous playback
    
    ↓
19. Progress saved to backend ONLY on these events:
    
    ✅ User pauses video
    ✅ User switches to another video
    ✅ User leaves course page
    ✅ Browser/tab closed (beforeunload event)
    ✅ Video reaches 80%+ (mark as complete)
    ✅ Every 2 minutes as backup (debounced)
    
    API Call:
    POST /api/v1/videos/{videoId}/progress
    {
      "studentId": "uuid",
      "positionSeconds": 145
    }
    
    Backend:
    ├─ Update VideoProgress record
    ├─ If progress >= 80%, mark video complete
    ├─ Update enrollment overall progress
    └─ Return updated progress
```

**Progress Tracking Logic:**
```
20. Video marked COMPLETE when:
    - Watched >= 80% of video duration
    - OR user manually marks as complete
    ↓
21. Frontend shows:
    - Green checkmark ✓ next to video
    - Updated overall progress percentage
    - "Next video" button enabled
    ↓
22. Backend updates:
    - VideoProgress.isCompleted = true
    - VideoProgress.completedAt = now
    - Enrollment.progressPercentage recalculated
    - Video.viewCount += 1 (only on first completion)
```

### Phase 5: Course Completion
```
23. When all videos watched (100% progress):
    ↓
24. Backend marks:
    - Enrollment.videosCompleted = true
    - Enrollment.isCompleted = true (if quizzes also done)
    - Enrollment.completedAt = now
    - Enrollment.status = COMPLETED
    ↓
25. Student sees:
    - "Congratulations! Course Completed" message
    - Certificate download button (if implemented)
    - Option to rate/review course
    - "Continue to Next Course" suggestions
```

---

## 🎬 VIDEO UPLOAD IMPLEMENTATION (LocalStack)

### LocalStack Setup (Development)

**docker-compose.yml:**
```yaml
version: '3.8'
services:
  localstack:
    image: localstack/localstack:latest
    ports:
      - "4566:4566"  # LocalStack gateway
    environment:
      - SERVICES=s3
      - DEBUG=1
      - DATA_DIR=/tmp/localstack/data
      - DOCKER_HOST=unix:///var/run/docker.sock
    volumes:
      - "./localstack-data:/tmp/localstack"
      - "/var/run/docker.sock:/var/run/docker.sock"
```

**application.yml (Development):**
```yaml
aws:
  s3:
    endpoint: http://localhost:4566
    region: us-east-1
    bucket: mwanzo-videos
    access-key: test  # LocalStack doesn't validate
    secret-key: test
    use-localstack: true
```

### Backend Video Upload Flow

**1. Generate Presigned Upload URL:**
```java
@PostMapping("/videos/upload-url")
public ResponseEntity<PresignedUrlResponse> getUploadUrl(
        @RequestParam String fileName,
        @RequestParam String contentType) {
    
    // Generate unique S3 key
    String s3Key = "videos/" + UUID.randomUUID() + "/" + fileName;
    
    // Generate presigned URL (15 min expiry)
    String presignedUrl = s3Service.generatePresignedUploadUrl(
        s3Key, 
        contentType, 
        Duration.ofMinutes(15)
    );
    
    return ResponseEntity.ok(new PresignedUrlResponse(
        presignedUrl,
        s3Key
    ));
}
```

**2. Frontend Uploads to S3:**
```javascript
// Frontend code
async function uploadVideo(file) {
  // Step 1: Get presigned URL
  const { presignedUrl, s3Key } = await fetch('/api/v1/videos/upload-url', {
    method: 'POST',
    body: JSON.stringify({
      fileName: file.name,
      contentType: file.type
    })
  }).then(r => r.json());
  
  // Step 2: Upload directly to S3
  await fetch(presignedUrl, {
    method: 'PUT',
    body: file,
    headers: {
      'Content-Type': file.type
    },
    onUploadProgress: (e) => {
      const percent = (e.loaded / e.total) * 100;
      updateProgressBar(percent);
    }
  });
  
  // Step 3: Notify backend upload complete
  return { s3Key };
}
```

**3. Create Video Record:**
```java
@PostMapping("/videos/sections/{sectionId}/videos")
public ResponseEntity<Video> createVideo(
        @PathVariable UUID sectionId,
        @RequestBody CreateVideoRequest request) {
    
    // Validate S3 key exists
    boolean exists = s3Service.objectExists(request.getS3Key());
    if (!exists) {
        throw new IllegalArgumentException("Video file not found in S3");
    }
    
    // Get video metadata (duration, size)
    VideoMetadata metadata = s3Service.getVideoMetadata(request.getS3Key());
    
    // Generate streaming URL
    String videoUrl = s3Service.generateStreamingUrl(request.getS3Key());
    
    // Create video record
    Video video = videoService.createVideo(
        request.getCourseId(),
        sectionId,
        request.getTitle(),
        request.getDisplayOrder(),
        metadata.getDurationSeconds(),
        videoUrl,
        request.getThumbnailUrl(),
        request.getIsPreview()
    );
    
    return ResponseEntity.status(201).body(video);
}
```

### S3 Service Implementation

```java
@Service
public class S3Service {
    
    @Value("${aws.s3.bucket}")
    private String bucketName;
    
    @Value("${aws.s3.endpoint}")
    private String endpoint;
    
    @Value("${aws.s3.use-localstack}")
    private boolean useLocalStack;
    
    private final AmazonS3 s3Client;
    
    // Generate presigned upload URL
    public String generatePresignedUploadUrl(String s3Key, String contentType, Duration expiry) {
        Date expiration = Date.from(Instant.now().plus(expiry));
        
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, s3Key)
                .withMethod(HttpMethod.PUT)
                .withExpiration(expiration)
                .withContentType(contentType);
        
        URL url = s3Client.generatePresignedUrl(request);
        return url.toString();
    }
    
    // Generate streaming URL (CloudFront or LocalStack)
    public String generateStreamingUrl(String s3Key) {
        if (useLocalStack) {
            return String.format("%s/%s/%s", endpoint, bucketName, s3Key);
        } else {
            // Production: CloudFront URL
            return String.format("https://cdn.mwanzoskills.com/%s", s3Key);
        }
    }
    
    // Check if object exists
    public boolean objectExists(String s3Key) {
        return s3Client.doesObjectExist(bucketName, s3Key);
    }
    
    // Get video metadata
    public VideoMetadata getVideoMetadata(String s3Key) {
        ObjectMetadata metadata = s3Client.getObjectMetadata(bucketName, s3Key);
        
        return VideoMetadata.builder()
                .fileSizeBytes(metadata.getContentLength())
                .contentType(metadata.getContentType())
                .durationSeconds(extractDuration(s3Key))  // Use ffmpeg or MediaInfo
                .build();
    }
}
```

---

## 📊 DATABASE OPTIMIZATION STRATEGY

### Minimize Database Calls

**BEFORE (Inefficient):**
```
❌ Save progress every 5 seconds during playback
   = 720 DB writes for 1-hour video
   = Database overload!
```

**AFTER (Optimized):**
```
✅ Save progress only on meaningful events:
   1. Video paused
   2. Video switched
   3. Browser closed
   4. Every 2 minutes (backup)
   5. Video 80%+ watched (completion)
   
   = ~30 DB writes for 1-hour video
   = 24x fewer database calls!
```

### Frontend State Management

```javascript
// Video player state (client-side only)
const [videoState, setVideoState] = useState({
  currentPosition: 0,
  duration: 0,
  isPlaying: false,
  lastSavedPosition: 0,  // Last position saved to backend
  needsSync: false        // Dirty flag
});

// Update position every second (NO backend call)
useEffect(() => {
  const interval = setInterval(() => {
    if (videoState.isPlaying) {
      setVideoState(prev => ({
        ...prev,
        currentPosition: videoRef.current.currentTime,
        needsSync: true
      }));
    }
  }, 1000);
  
  return () => clearInterval(interval);
}, [videoState.isPlaying]);

// Save to backend only on events
const saveProgress = async () => {
  if (!videoState.needsSync) return;
  
  await fetch(`/api/v1/videos/${videoId}/progress`, {
    method: 'POST',
    body: JSON.stringify({
      studentId,
      positionSeconds: Math.floor(videoState.currentPosition)
    })
  });
  
  setVideoState(prev => ({
    ...prev,
    lastSavedPosition: prev.currentPosition,
    needsSync: false
  }));
};

// Trigger save on events
useEffect(() => {
  // Save on pause
  videoRef.current.addEventListener('pause', saveProgress);
  
  // Save on page unload
  window.addEventListener('beforeunload', saveProgress);
  
  // Save every 2 minutes as backup
  const backupInterval = setInterval(saveProgress, 120000);
  
  return () => {
    videoRef.current.removeEventListener('pause', saveProgress);
    window.removeEventListener('beforeunload', saveProgress);
    clearInterval(backupInterval);
  };
}, []);
```

---

## 🎨 THUMBNAIL REQUIREMENTS

### Course Thumbnail:
- **Resolution**: 1280x720px (16:9 aspect ratio)
- **Format**: JPG or PNG
- **Max Size**: 500KB
- **Purpose**: Course card in catalog, course header
- **Design Tips**:
    - Clear, high-contrast text
    - Professional design
    - Include course topic/technology logo
    - Instructor photo (optional)

### Video Thumbnail:
- **Resolution**: 854x480px (16:9 aspect ratio)
- **Format**: JPG or PNG
- **Max Size**: 200KB
- **Purpose**: Video list, hover preview
- **Design Tips**:
    - Screenshot from video + text overlay
    - Consistent style within course
    - Include video number/title
    - Use course branding colors

### Thumbnail Upload Flow:
```
1. Tutor uploads video
   ↓
2. System generates auto-thumbnail from video frame
   ↓
3. Tutor can:
   - Use auto-generated thumbnail
   - Upload custom thumbnail (recommended)
   ↓
4. Thumbnail uploaded to S3 (same as video)
   ↓
5. Thumbnail URL stored in Video.thumbnailUrl
```

---

## 📱 MOBILE OPTIMIZATION

### Adaptive Bitrate Streaming:
```
- 1080p (1920x1080) - WiFi, high-speed data
- 720p (1280x720)   - Good 4G connection
- 480p (854x480)    - Standard 3G/4G
- 360p (640x360)    - Slow connection, data saving
```

### Data Saving Mode:
```
- User enables "Data Saver" in settings
- Default to 360p quality
- Preload only 10 seconds ahead
- Download video for offline (future feature)
```

---

## 🔄 SUMMARY: KEY OPTIMIZATIONS

### 1. **Progress Tracking**
- ✅ Client-side tracking during playback
- ✅ Batch updates to backend (2-min intervals)
- ✅ Event-based saves (pause, exit, complete)
- ❌ NO continuous DB writes

### 2. **Video Upload**
- ✅ Direct S3 upload (presigned URLs)
- ✅ LocalStack for development
- ✅ Frontend handles upload progress
- ✅ Backend validates after upload

### 3. **Preview System**
- ✅ Mark videos as "Preview"
- ✅ Free access for non-enrolled users
- ✅ Great for marketing

### 4. **Crash Courses**
- ✅ Support 1-3 video mini-courses
- ✅ Single section structure
- ✅ Lower pricing

### 5. **Thumbnails**
- ✅ Required for courses & videos
- ✅ Auto-generation + custom upload
- ✅ Consistent 16:9 aspect ratio

---

## ✅ NEXT STEPS

1. **Review this flow** - Confirm it matches your vision
2. **Implement S3Service** - LocalStack integration
3. **Optimize VideoController** - Event-based progress
4. **Update Frontend Docs** - Developer guide
5. **Test Complete Flow** - End-to-end verification

---

**Does this flow align with your vision? Any changes needed before implementation?**