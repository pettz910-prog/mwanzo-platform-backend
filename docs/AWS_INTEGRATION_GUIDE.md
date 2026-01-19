# AWS Integration Guide

Complete guide for configuring AWS services for the Mwanzo Course Platform.

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Required AWS Services](#required-aws-services)
3. [IAM Configuration](#iam-configuration)
4. [S3 Configuration](#s3-configuration)
5. [MediaConvert Configuration](#mediaconvert-configuration)
6. [CloudFront Configuration](#cloudfront-configuration)
7. [Environment Variables](#environment-variables)
8. [Development vs Production](#development-vs-production)
9. [Cost Estimates](#cost-estimates)
10. [Troubleshooting](#troubleshooting)

---

## Architecture Overview

```
┌─────────────┐
│   Frontend  │ (React/Vercel)
└──────┬──────┘
       │
       ▼
┌─────────────────────────────────────────────────┐
│         Backend (Spring Boot on AWS)            │
│  ┌──────────┐  ┌──────────┐  ┌───────────┐    │
│  │   S3     │  │MediaConv.│  │CloudFront │    │
│  │  Bucket  │→ │   Job    │→ │    CDN    │    │
│  └──────────┘  └──────────┘  └───────────┘    │
└─────────────────────────────────────────────────┘
```

### Video Upload Flow

1. **Frontend** requests presigned URL from backend
2. **Backend** generates presigned S3 URL (valid 15 min)
3. **Frontend** uploads video directly to S3
4. **Frontend** notifies backend of successful upload
5. **Backend** creates video record in DB (status: PROCESSING)
6. **Backend** submits MediaConvert job
7. **MediaConvert** transcodes video to 360p, 720p, 1080p
8. **MediaConvert** webhook notifies backend on completion
9. **Backend** updates video record (status: READY)
10. **CloudFront** delivers video to students

---

## Required AWS Services

### 1. Amazon S3 (Simple Storage Service)
- **Purpose**: Store original videos, thumbnails, and transcoded outputs
- **Region**: Choose closest to your users (e.g., `us-east-1`)
- **Bucket Structure**:
  ```
  mwanzo-videos-prod/
  ├── videos/           # Original uploads
  │   └── {uuid}/
  │       └── video.mp4
  ├── thumbnails/       # Course thumbnails
  │   └── {uuid}/
  │       └── thumb.jpg
  └── processed/        # Transcoded outputs
      ├── 360p/
      ├── 720p/
      └── 1080p/
  ```

### 2. AWS MediaConvert
- **Purpose**: Transcode videos to multiple qualities
- **Output Formats**: HLS (adaptive bitrate streaming)
- **Qualities**: 360p, 720p, 1080p

### 3. Amazon CloudFront (CDN)
- **Purpose**: Fast global video delivery
- **Cache**: Reduces S3 costs and latency
- **SSL**: Free HTTPS with AWS Certificate Manager

### 4. Amazon RDS PostgreSQL
- **Purpose**: Application database
- **Version**: PostgreSQL 15+
- **Instance**: `db.t3.micro` (Free Tier) → `db.t3.small` (Production)

---

## IAM Configuration

### Production (Recommended): IAM Role for EC2/ECS

**Best Practice**: Use IAM instance roles instead of access keys.

1. **Create IAM Role**:
   - Go to IAM → Roles → Create Role
   - Select: AWS Service → EC2 (or ECS)
   - Name: `MwanzoPlatformRole`

2. **Attach Policies**:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:GetObject",
        "s3:PutObject",
        "s3:DeleteObject",
        "s3:ListBucket"
      ],
      "Resource": [
        "arn:aws:s3:::mwanzo-videos-prod",
        "arn:aws:s3:::mwanzo-videos-prod/*"
      ]
    },
    {
      "Effect": "Allow",
      "Action": [
        "mediaconvert:CreateJob",
        "mediaconvert:GetJob",
        "mediaconvert:DescribeEndpoints"
      ],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "iam:PassRole"
      ],
      "Resource": "arn:aws:iam::YOUR_ACCOUNT_ID:role/MediaConvertRole"
    }
  ]
}
```

3. **MediaConvert Service Role**:

Create separate role for MediaConvert to access S3:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:GetObject",
        "s3:PutObject"
      ],
      "Resource": [
        "arn:aws:s3:::mwanzo-videos-prod/*"
      ]
    }
  ]
}
```

**Trust Relationship**:
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Service": "mediaconvert.amazonaws.com"
      },
      "Action": "sts:AssumeRole"
    }
  ]
}
```

### Development: Access Keys

For local development only:

1. Create IAM user: `mwanzo-dev-user`
2. Attach same policies as above
3. Generate access key
4. Add to `.env` file (NEVER commit to git)

---

## S3 Configuration

### Create S3 Bucket

```bash
aws s3api create-bucket \
  --bucket mwanzo-videos-prod \
  --region us-east-1
```

### Enable Versioning (Optional)

```bash
aws s3api put-bucket-versioning \
  --bucket mwanzo-videos-prod \
  --versioning-configuration Status=Enabled
```

### Configure CORS

```json
[
  {
    "AllowedHeaders": ["*"],
    "AllowedMethods": ["GET", "PUT", "POST"],
    "AllowedOrigins": [
      "https://mwanzo.com",
      "http://localhost:5173"
    ],
    "ExposeHeaders": ["ETag"],
    "MaxAgeSeconds": 3000
  }
]
```

Apply CORS:
```bash
aws s3api put-bucket-cors \
  --bucket mwanzo-videos-prod \
  --cors-configuration file://cors.json
```

### Lifecycle Policy (Cost Optimization)

Delete incomplete multipart uploads after 7 days:

```json
{
  "Rules": [
    {
      "Id": "DeleteIncompleteUploads",
      "Status": "Enabled",
      "AbortIncompleteMultipartUpload": {
        "DaysAfterInitiation": 7
      }
    }
  ]
}
```

---

## MediaConvert Configuration

### Get MediaConvert Endpoint

Each AWS account has a unique MediaConvert endpoint:

```bash
aws mediaconvert describe-endpoints --region us-east-1
```

Output:
```json
{
  "Endpoints": [
    {
      "Url": "https://abcdefg123.mediaconvert.us-east-1.amazonaws.com"
    }
  ]
}
```

**Save this URL** - you'll need it for `aws.mediaconvert.endpoint`.

### Create MediaConvert Queue (Optional)

Default queue is usually sufficient:

```bash
aws mediaconvert list-queues --region us-east-1
```

---

## CloudFront Configuration

### Create CloudFront Distribution

1. **Origin**:
   - Origin Domain: `mwanzo-videos-prod.s3.us-east-1.amazonaws.com`
   - Origin Path: Leave empty
   - Enable Origin Shield: No (saves cost)

2. **Default Cache Behavior**:
   - Viewer Protocol Policy: Redirect HTTP to HTTPS
   - Allowed HTTP Methods: GET, HEAD, OPTIONS
   - Cache Policy: CachingOptimized
   - Compress Objects Automatically: Yes

3. **Distribution Settings**:
   - Price Class: Use All Edge Locations (or choose based on audience)
   - Alternate Domain Names (CNAMEs): `cdn.mwanzo.com`
   - SSL Certificate: Request from ACM or use default

### SSL Certificate (ACM)

1. Request certificate in **us-east-1** (required for CloudFront)
2. Domain: `cdn.mwanzo.com` and `*.mwanzo.com`
3. Validation: DNS (add CNAME records)
4. Wait for validation (~5-30 minutes)

### DNS Configuration

Add CNAME in your DNS provider:

```
cdn.mwanzo.com  CNAME  d1234abcd.cloudfront.net
```

---

## Environment Variables

### Development (application-dev.yml)

```yaml
aws:
  credentials:
    use-iam-role: false
    access-key: ${AWS_ACCESS_KEY_ID}      # From .env
    secret-key: ${AWS_SECRET_ACCESS_KEY}  # From .env

  s3:
    bucket: mwanzo-videos-dev
    region: us-east-1
    endpoint: http://localhost:4566       # LocalStack
    use-localstack: true

  mediaconvert:
    enabled: true
    use-mock: true                        # Simulates transcoding
    mock-processing-seconds: 30
    endpoint: ""
    role-arn: ""
    queue-arn: ""
    output-bucket: mwanzo-videos-dev

  cloudfront:
    enabled: false                        # Use S3 URLs in dev
    domain: ""
```

### Production (application-prod.yml)

```yaml
aws:
  credentials:
    use-iam-role: true                    # Uses EC2/ECS instance role
    access-key: ""                        # Empty - uses IAM role
    secret-key: ""                        # Empty - uses IAM role

  s3:
    bucket: mwanzo-videos-prod
    region: us-east-1
    endpoint: ""                          # Empty - uses AWS S3
    use-localstack: false

  mediaconvert:
    enabled: true
    use-mock: false                       # Real AWS MediaConvert
    endpoint: https://abcdefg123.mediaconvert.us-east-1.amazonaws.com
    role-arn: arn:aws:iam::123456789012:role/MediaConvertRole
    queue-arn: arn:aws:mediaconvert:us-east-1:123456789012:queues/Default
    output-bucket: mwanzo-videos-prod

  cloudfront:
    enabled: true
    domain: https://cdn.mwanzo.com
```

### .env File (Local Development)

```bash
# AWS Credentials (NEVER commit to git!)
AWS_ACCESS_KEY_ID=AKIAXXXXXXXXXXXXXXXX
AWS_SECRET_ACCESS_KEY=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx

# Database
DATABASE_URL=jdbc:postgresql://localhost:5432/mwanzo_dev
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=postgres

# JWT
JWT_SECRET=your-super-secret-jwt-key-min-256-bits
JWT_EXPIRATION_MS=86400000
```

---

## Development vs Production

### Development Setup

**Option 1: LocalStack (Recommended)**

Simulates AWS services locally for free:

```bash
# Install LocalStack
pip install localstack

# Start LocalStack
localstack start -d

# Create local S3 bucket
aws --endpoint-url=http://localhost:4566 s3 mb s3://mwanzo-videos-dev
```

**Option 2: Real AWS (Costs Money)**

Use real AWS services with separate dev bucket:
- Bucket: `mwanzo-videos-dev`
- CloudFront: Disabled (use S3 URLs)
- MediaConvert: Mock mode enabled

### Production Setup

1. **Deploy to AWS** (EC2, ECS, or Elastic Beanstalk)
2. **Attach IAM role** to instance
3. **Set environment**: `SPRING_PROFILES_ACTIVE=prod`
4. **Configure CloudFront** for CDN delivery
5. **Enable real MediaConvert** transcoding

---

## Cost Estimates

### Monthly Costs (Assuming 1000 students, 50 courses)

| Service | Usage | Cost |
|---------|-------|------|
| **S3 Storage** | 500GB (videos + transcoded) | $11.50 |
| **S3 Requests** | 100K PUT, 1M GET | $0.50 |
| **MediaConvert** | 100 videos/month (10 min avg) | $20.00 |
| **CloudFront** | 500GB transfer | $42.50 |
| **RDS PostgreSQL** | db.t3.small | $25.00 |
| **EC2/ECS** | t3.medium (backend) | $30.00 |
| **Data Transfer** | 50GB out | $4.50 |
| **Total** | | **~$134/month** |

### Cost Optimization Tips

1. **Use CloudFront**: Reduces S3 data transfer costs by 50%
2. **Delete old transcodes**: Keep only 360p, 720p for old videos
3. **S3 Intelligent-Tiering**: Auto-move to cheaper storage
4. **Reserved Instances**: Save 30-70% on EC2/RDS
5. **Right-size instances**: Start small, scale up
6. **Enable S3 Transfer Acceleration**: Only if users are global

---

## Troubleshooting

### Issue: "S3 bucket not found"

**Cause**: Bucket doesn't exist or wrong region

**Solution**:
```bash
# Check if bucket exists
aws s3 ls s3://mwanzo-videos-prod

# Create if missing
aws s3 mb s3://mwanzo-videos-prod --region us-east-1
```

### Issue: "Access Denied" on S3

**Cause**: IAM permissions insufficient

**Solution**:
1. Check IAM role/user has `s3:GetObject`, `s3:PutObject` permissions
2. Verify bucket policy doesn't block access
3. Check backend logs for actual error message

### Issue: "MediaConvert endpoint not configured"

**Cause**: Missing MediaConvert endpoint URL

**Solution**:
```bash
# Get your account's MediaConvert endpoint
aws mediaconvert describe-endpoints --region us-east-1

# Add to application.yml
aws.mediaconvert.endpoint: https://YOUR_ENDPOINT.mediaconvert.us-east-1.amazonaws.com
```

### Issue: Video upload succeeds but transcoding fails

**Cause**: MediaConvert IAM role misconfigured

**Solution**:
1. Verify MediaConvert role has S3 read/write permissions
2. Check `aws.mediaconvert.role-arn` is correct
3. Check MediaConvert CloudWatch logs for errors

### Issue: CloudFront returns 403 Forbidden

**Cause**: S3 bucket policy blocks CloudFront

**Solution**:
1. Add CloudFront OAI (Origin Access Identity) to S3 bucket policy
2. Or make S3 bucket public read (less secure)

### Viewing Logs

**Application Logs**:
```bash
# Check Spring Boot logs
tail -f /var/log/mwanzo/application.log
```

**AWS CloudWatch Logs**:
```bash
# View MediaConvert job logs
aws logs tail /aws/mediaconvert/jobs --follow

# View Lambda logs (if using webhooks)
aws logs tail /aws/lambda/mediaconvert-webhook --follow
```

### Testing AWS Integration

**Test S3 Upload**:
```bash
curl -X POST http://localhost:8080/api/v1/videos/upload-url \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "courseId": "uuid-here",
    "sectionId": "uuid-here",
    "fileName": "test-video.mp4",
    "contentType": "video/mp4"
  }'
```

**Test MediaConvert**:
1. Upload video via presigned URL
2. Check CloudWatch logs for MediaConvert job
3. Verify transcoded outputs in S3 bucket

---

## Security Best Practices

1. **Never commit AWS credentials** to git
2. **Use IAM roles** in production (not access keys)
3. **Enable MFA** on AWS root account
4. **Restrict S3 bucket** to CloudFront OAI only
5. **Enable CloudFront signed URLs** for premium content
6. **Rotate access keys** every 90 days (if using)
7. **Use AWS Secrets Manager** for production credentials
8. **Enable AWS CloudTrail** for audit logging
9. **Set up AWS Budgets** to alert on unexpected costs
10. **Use S3 Block Public Access** on buckets

---

## Next Steps

1. ✅ Complete this AWS setup
2. 📦 Create Terraform IaC for automated deployment
3. 🎨 Enhance frontend video upload component
4. 🚀 Deploy to AWS with CI/CD pipeline

For Terraform deployment guide, see: [TERRAFORM_GUIDE.md](TERRAFORM_GUIDE.md) (coming next)
