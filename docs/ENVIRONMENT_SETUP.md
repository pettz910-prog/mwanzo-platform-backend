# 🔧 Environment Setup & AWS Configuration

> **Configure AWS services and environment variables for production**

---

## Development Environment

### 1. Backend Configuration

**File:** `src/main/resources/application-dev.yml`

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/mwanzo_database
    username: postgres
    password: your_password

  jpa:
    hibernate:
      ddl-auto: update  # Auto-create/update tables

# LocalStack for development (free S3 mock)
aws:
  endpoint: http://localhost:4566
  region: us-east-1
  s3:
    bucket: mwanzo-videos
  cloudfront:
    enabled: false  # Disable CloudFront in dev
```

### 2. Frontend Configuration

**File:** `src/mwanzo-platform-main/.env.local`

```env
VITE_API_BASE_URL=http://localhost:8080/api/v1
```

---

## Production Environment

### AWS Credentials Setup

#### Option 1: Environment Variables (Recommended)
```bash
export AWS_ACCESS_KEY_ID=AKIA...
export AWS_SECRET_ACCESS_KEY=...
export AWS_REGION=us-east-1
```

#### Option 2: AWS Credentials File
```bash
# ~/.aws/credentials
[default]
aws_access_key_id = AKIA...
aws_secret_access_key = ...

# ~/.aws/config
[default]
region = us-east-1
```

#### Option 3: IAM Role (EC2/ECS)
No credentials needed - use IAM role attached to instance.

---

## AWS Services Configuration

### 1. S3 Setup

**Create Bucket:**
```bash
aws s3 mb s3://mwanzo-videos-prod
aws s3 mb s3://mwanzo-thumbnails-prod
```

**Bucket Policy (Private):**
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Deny",
      "Principal": "*",
      "Action": "s3:GetObject",
      "Resource": "arn:aws:s3:::mwanzo-videos-prod/*",
      "Condition": {
        "StringNotEquals": {
          "aws:SourceVpce": "your-vpce-id"
        }
      }
    }
  ]
}
```

**CORS Configuration:**
```json
[
  {
    "AllowedHeaders": ["*"],
    "AllowedMethods": ["PUT", "POST", "GET"],
    "AllowedOrigins": [
      "https://mwanzoskills.co.ke",
      "https://www.mwanzoskills.co.ke"
    ],
    "ExposeHeaders": ["ETag"]
  }
]
```

**Backend Config:**
```yaml
aws:
  access-key: ${AWS_ACCESS_KEY_ID}
  secret-key: ${AWS_SECRET_ACCESS_KEY}
  region: us-east-1
  s3:
    bucket: mwanzo-videos-prod
    thumbnail-bucket: mwanzo-thumbnails-prod
```

---

### 2. MediaConvert Setup

**Create Job Template:**
```bash
# Use AWS Console or CLI to create template
# Outputs: 360p, 720p, 1080p (HLS format)
```

**IAM Role for MediaConvert:**
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

**Backend Config:**
```yaml
aws:
  mediaconvert:
    endpoint: https://[account-id].mediaconvert.[region].amazonaws.com
    role-arn: arn:aws:iam::123456789012:role/MediaConvertRole
    job-template: MwanzoVideoTemplate
```

**Job Settings:**
- Input: S3 raw video
- Outputs:
  - 360p: 500 kbps
  - 720p: 2500 kbps
  - 1080p: 5000 kbps
- Format: HLS (HTTP Live Streaming)
- Destination: S3 processed folder

---

### 3. CloudFront Setup

**Create Distribution:**
```bash
# Origin: S3 bucket (mwanzo-videos-prod)
# Behavior: Viewer Protocol Policy = Redirect HTTP to HTTPS
# Price Class: Use All Edge Locations
```

**Signed URLs Configuration:**
```yaml
aws:
  cloudfront:
    enabled: true
    domain: d1234abcd.cloudfront.net
    key-pair-id: APKA...
    private-key: |
      -----BEGIN RSA PRIVATE KEY-----
      [Your private key from AWS Console]
      -----END RSA PRIVATE KEY-----
    url-expiration-hours: 24
```

**Generate Key Pair:**
```bash
# AWS Console → CloudFront → Key pairs (under Security)
# Download private key and store securely
```

---

### 4. RDS PostgreSQL Setup

**Create Database:**
```bash
# Use AWS Console or CLI
# Instance: db.t3.medium (dev), db.r5.large (prod)
# Multi-AZ: Enabled (prod)
# Storage: 100 GB, auto-scaling enabled
```

**Backend Config:**
```yaml
spring:
  datasource:
    url: jdbc:postgresql://mwanzo-db.abc123.us-east-1.rds.amazonaws.com:5432/mwanzo_prod
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}

  jpa:
    hibernate:
      ddl-auto: validate  # Don't auto-update in production!
```

**Security Group:**
- Allow inbound: Port 5432 from application security group only
- No public access

---

### 5. SES (Email) Setup

**Verify Domain:**
```bash
# AWS Console → SES → Verified identities
# Add domain: mwanzoskills.co.ke
# Add DNS records provided by AWS
```

**Backend Config:**
```yaml
spring:
  mail:
    host: email-smtp.us-east-1.amazonaws.com
    port: 587
    username: ${SMTP_USERNAME}
    password: ${SMTP_PASSWORD}
    properties:
      mail.smtp.auth: true
      mail.smtp.starttls.enable: true

mwanzo:
  email:
    from: noreply@mwanzoskills.co.ke
    from-name: Mwanzo Skills Campus
```

**Email Templates:**
- Welcome email
- Email verification
- Password reset
- Enrollment confirmation
- Course completion

---

## Application Configuration

### Complete Production Config

**File:** `src/main/resources/application-prod.yml`

```yaml
server:
  port: 8080
  compression:
    enabled: true

spring:
  datasource:
    url: ${DATABASE_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate.dialect: org.hibernate.dialect.PostgreSQLDialect

  mail:
    host: email-smtp.us-east-1.amazonaws.com
    port: 587
    username: ${SMTP_USERNAME}
    password: ${SMTP_PASSWORD}

# AWS Configuration
aws:
  access-key: ${AWS_ACCESS_KEY_ID}
  secret-key: ${AWS_SECRET_ACCESS_KEY}
  region: ${AWS_REGION:us-east-1}

  s3:
    bucket: ${S3_BUCKET:mwanzo-videos-prod}
    thumbnail-bucket: ${S3_THUMBNAIL_BUCKET:mwanzo-thumbnails-prod}

  mediaconvert:
    endpoint: ${MEDIACONVERT_ENDPOINT}
    role-arn: ${MEDIACONVERT_ROLE_ARN}
    job-template: MwanzoVideoTemplate

  cloudfront:
    enabled: true
    domain: ${CLOUDFRONT_DOMAIN}
    key-pair-id: ${CLOUDFRONT_KEY_PAIR_ID}
    private-key: ${CLOUDFRONT_PRIVATE_KEY}
    url-expiration-hours: 24

# JWT Configuration
jwt:
  secret: ${JWT_SECRET}  # 256-bit secret key
  expiration: 86400000   # 24 hours in milliseconds

# PayHero Configuration
payhero:
  api-key: ${PAYHERO_API_KEY}
  api-secret: ${PAYHERO_API_SECRET}
  webhook-secret: ${PAYHERO_WEBHOOK_SECRET}
  base-url: https://backend.payhero.co.ke/api/v2

# Application Settings
mwanzo:
  frontend-url: https://mwanzoskills.co.ke
  email:
    from: noreply@mwanzoskills.co.ke
    from-name: Mwanzo Skills Campus
```

---

## Environment Variables Reference

### Required for Production

| Variable | Description | Example |
|----------|-------------|---------|
| `DATABASE_URL` | PostgreSQL connection URL | `jdbc:postgresql://host:5432/db` |
| `DB_USERNAME` | Database username | `mwanzo_user` |
| `DB_PASSWORD` | Database password | `SecurePass123!` |
| `AWS_ACCESS_KEY_ID` | AWS access key | `AKIA...` |
| `AWS_SECRET_ACCESS_KEY` | AWS secret key | `...` |
| `AWS_REGION` | AWS region | `us-east-1` |
| `S3_BUCKET` | S3 video bucket | `mwanzo-videos-prod` |
| `MEDIACONVERT_ENDPOINT` | MediaConvert endpoint | `https://...` |
| `MEDIACONVERT_ROLE_ARN` | MediaConvert IAM role | `arn:aws:iam::...` |
| `CLOUDFRONT_DOMAIN` | CloudFront domain | `d1234.cloudfront.net` |
| `CLOUDFRONT_KEY_PAIR_ID` | CloudFront key pair | `APKA...` |
| `CLOUDFRONT_PRIVATE_KEY` | CloudFront private key | `-----BEGIN...` |
| `JWT_SECRET` | JWT signing key (256-bit) | `[generate random]` |
| `PAYHERO_API_KEY` | PayHero API key | `ph_...` |
| `PAYHERO_API_SECRET` | PayHero API secret | `...` |
| `SMTP_USERNAME` | AWS SES SMTP username | `AKIA...` |
| `SMTP_PASSWORD` | AWS SES SMTP password | `...` |

---

## Security Best Practices

### 1. Secrets Management

**Use AWS Secrets Manager:**
```bash
# Store sensitive values
aws secretsmanager create-secret \
  --name mwanzo/prod/database \
  --secret-string '{"username":"user","password":"pass"}'

# Retrieve in application
# Use AWS SDK to fetch secrets on startup
```

**Never commit secrets:**
```bash
# .gitignore
.env*
*.pem
*.key
application-prod.yml
```

### 2. IAM Roles (Preferred)

Instead of access keys, use IAM roles:

```yaml
# No credentials in config
aws:
  region: us-east-1
  # Credentials auto-loaded from IAM role
```

**Attach IAM role to EC2/ECS:**
- `AmazonS3FullAccess`
- `AWSElementalMediaConvertFullAccess`
- `CloudFrontFullAccess`
- `AmazonSESFullAccess`

---

## Testing Configuration

### LocalStack Setup (Development)

**Start LocalStack:**
```bash
docker-compose up -d localstack
```

**Initialize S3 bucket:**
```bash
aws --endpoint-url=http://localhost:4566 s3 mb s3://mwanzo-videos
```

**Backend uses LocalStack automatically in dev profile.**

---

## Frontend Environment

### Development
```env
# .env.local
VITE_API_BASE_URL=http://localhost:8080/api/v1
```

### Production (Vercel)
```env
# Environment variables in Vercel dashboard
VITE_API_BASE_URL=https://api.mwanzoskills.co.ke/api/v1
```

---

## Health Checks

**Backend Health Endpoint:**
```bash
curl https://api.mwanzoskills.co.ke/actuator/health
```

**Expected Response:**
```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "diskSpace": { "status": "UP" },
    "ping": { "status": "UP" }
  }
}
```

---

## Troubleshooting

### AWS Credentials Not Working
```bash
# Test credentials
aws sts get-caller-identity

# Output should show your account ID
```

### S3 Upload Fails
```bash
# Check bucket exists
aws s3 ls s3://mwanzo-videos-prod

# Test upload
echo "test" | aws s3 cp - s3://mwanzo-videos-prod/test.txt
```

### MediaConvert Not Processing
```bash
# Check IAM role has S3 permissions
# Check input/output buckets are correct
# View job in MediaConvert console
```

### CloudFront Not Serving Videos
```bash
# Check origin is S3 bucket
# Check behavior allows GET requests
# Test URL directly
curl https://d1234.cloudfront.net/videos/test.mp4
```

---

## Cost Optimization

### Development
- Use LocalStack (free)
- Use t3.micro RDS (free tier eligible)
- Minimize CloudFront usage

### Production
- Use S3 Intelligent-Tiering
- Enable CloudFront compression
- Use Reserved Instances for RDS
- Monitor costs with AWS Budgets

**Estimated Monthly Costs:**
- RDS (db.t3.medium): ~$60
- S3 (1TB storage): ~$23
- CloudFront (1TB transfer): ~$85
- MediaConvert: Pay per minute transcoded
- **Total: ~$200-300/month** (depends on usage)

---

**Last Updated:** January 18, 2026
