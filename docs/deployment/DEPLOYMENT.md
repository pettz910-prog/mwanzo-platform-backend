# 🚀 Production Deployment Guide

> **Complete guide for deploying to AWS (backend) and Vercel (frontend)**

---

## Deployment Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    PRODUCTION SETUP                          │
└─────────────────────────────────────────────────────────────┘

Frontend (Vercel)
  ├─ Domain: mwanzoskills.co.ke
  ├─ CDN: Vercel Edge Network
  └─ Build: Automatic on git push

        ↓ HTTPS Requests

Backend (AWS)
  ├─ Application Load Balancer (ALB)
  │   ├─ Domain: api.mwanzoskills.co.ke
  │   ├─ SSL: AWS Certificate Manager
  │   └─ Health checks
  │
  ├─ Auto Scaling Group (ASG)
  │   ├─ Min: 2 instances
  │   ├─ Max: 10 instances
  │   └─ Instance: t3.large (2 vCPU, 8GB RAM)
  │
  ├─ RDS PostgreSQL (Multi-AZ)
  │   ├─ Instance: db.r5.large
  │   ├─ Storage: 200 GB (auto-scaling)
  │   └─ Backups: Daily, 30-day retention
  │
  ├─ S3 Buckets
  │   ├─ mwanzo-videos-prod (videos)
  │   └─ mwanzo-thumbnails-prod (images)
  │
  ├─ AWS MediaConvert (video transcoding)
  │
  ├─ CloudFront CDN (video delivery)
  │
  └─ Monitoring
      ├─ CloudWatch (logs, metrics, alarms)
      └─ AWS X-Ray (tracing)
```

**Estimated Cost:** $200-400/month (depending on traffic)

---

## Prerequisites

### 1. AWS Account Setup
- AWS Account with billing enabled
- IAM user with admin access
- AWS CLI configured
- Domain name registered

### 2. Required Tools
```bash
# Install AWS CLI
brew install awscli  # macOS
# or download from https://aws.amazon.com/cli/

# Install Docker
# Download from https://www.docker.com/products/docker-desktop

# Install Terraform (optional, for IaC)
brew install terraform
```

### 3. Vercel Account
- Create account at https://vercel.com
- Install Vercel CLI: `npm install -g vercel`

---

## Part 1: Backend Deployment (AWS)

### Option A: AWS Elastic Beanstalk (Easiest)

**1. Create Application Package:**
```bash
# Build JAR
./mvnw clean package -DskipTests

# JAR location: target/mwanzo-course-platform-backend-0.0.1-SNAPSHOT.jar
```

**2. Create `Dockerrun.aws.json`:**
```json
{
  "AWSEBDockerrunVersion": "1",
  "Image": {
    "Name": "your-dockerhub-username/mwanzo-backend:latest",
    "Update": "true"
  },
  "Ports": [
    {
      "ContainerPort": 8080
    }
  ]
}
```

**3. Deploy to Elastic Beanstalk:**
```bash
# Initialize EB
eb init -p docker mwanzo-backend --region us-east-1

# Create environment
eb create mwanzo-prod \
  --instance-type t3.large \
  --database \
  --database.engine postgres \
  --database.version 15

# Deploy
eb deploy
```

**4. Configure Environment Variables:**
```bash
eb setenv \
  SPRING_PROFILES_ACTIVE=prod \
  DATABASE_URL=jdbc:postgresql://... \
  DB_USERNAME=mwanzo_user \
  DB_PASSWORD=SecurePassword123! \
  AWS_ACCESS_KEY_ID=AKIA... \
  AWS_SECRET_ACCESS_KEY=... \
  AWS_REGION=us-east-1 \
  S3_BUCKET=mwanzo-videos-prod \
  JWT_SECRET=$(openssl rand -base64 32) \
  PAYHERO_API_KEY=ph_... \
  PAYHERO_API_SECRET=...
```

---

### Option B: Docker + ECS (More Control)

**1. Create Dockerfile:**
```dockerfile
# Dockerfile
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "app.jar"]
```

**2. Build and Push to ECR:**
```bash
# Authenticate Docker to ECR
aws ecr get-login-password --region us-east-1 | \
  docker login --username AWS --password-stdin \
  123456789012.dkr.ecr.us-east-1.amazonaws.com

# Build image
docker build -t mwanzo-backend .

# Tag for ECR
docker tag mwanzo-backend:latest \
  123456789012.dkr.ecr.us-east-1.amazonaws.com/mwanzo-backend:latest

# Push to ECR
docker push 123456789012.dkr.ecr.us-east-1.amazonaws.com/mwanzo-backend:latest
```

**3. Create ECS Task Definition:**
```json
{
  "family": "mwanzo-backend",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "2048",
  "memory": "4096",
  "containerDefinitions": [
    {
      "name": "mwanzo-backend",
      "image": "123456789012.dkr.ecr.us-east-1.amazonaws.com/mwanzo-backend:latest",
      "portMappings": [
        {
          "containerPort": 8080,
          "protocol": "tcp"
        }
      ],
      "environment": [
        {"name": "SPRING_PROFILES_ACTIVE", "value": "prod"},
        {"name": "AWS_REGION", "value": "us-east-1"}
      ],
      "secrets": [
        {
          "name": "DATABASE_URL",
          "valueFrom": "arn:aws:secretsmanager:us-east-1:123456789012:secret:mwanzo/prod/database"
        }
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/mwanzo-backend",
          "awslogs-region": "us-east-1",
          "awslogs-stream-prefix": "ecs"
        }
      }
    }
  ]
}
```

**4. Create ECS Service:**
```bash
aws ecs create-service \
  --cluster mwanzo-cluster \
  --service-name mwanzo-backend \
  --task-definition mwanzo-backend \
  --desired-count 2 \
  --launch-type FARGATE \
  --network-configuration "awsvpcConfiguration={subnets=[subnet-xxx],securityGroups=[sg-xxx]}" \
  --load-balancers "targetGroupArn=arn:aws:elasticloadbalancing:...,containerName=mwanzo-backend,containerPort=8080"
```

---

### Database Setup (RDS PostgreSQL)

**1. Create RDS Instance:**
```bash
aws rds create-db-instance \
  --db-instance-identifier mwanzo-prod-db \
  --db-instance-class db.r5.large \
  --engine postgres \
  --engine-version 15.4 \
  --master-username mwanzo_admin \
  --master-user-password 'SecurePassword123!' \
  --allocated-storage 200 \
  --storage-type gp3 \
  --storage-encrypted \
  --backup-retention-period 30 \
  --multi-az \
  --publicly-accessible false \
  --vpc-security-group-ids sg-xxx
```

**2. Run Database Migrations:**
```bash
# Connect to RDS
psql -h mwanzo-prod-db.abc123.us-east-1.rds.amazonaws.com \
     -U mwanzo_admin \
     -d postgres

# Application will auto-create tables on first run
# Or use Flyway/Liquibase for controlled migrations
```

---

### S3 Buckets Setup

**1. Create Buckets:**
```bash
# Videos bucket
aws s3 mb s3://mwanzo-videos-prod --region us-east-1

# Thumbnails bucket
aws s3 mb s3://mwanzo-thumbnails-prod --region us-east-1
```

**2. Configure Bucket Policies:**
```bash
# Make private (only CloudFront can access)
aws s3api put-bucket-policy \
  --bucket mwanzo-videos-prod \
  --policy file://s3-bucket-policy.json
```

**3. Enable Versioning:**
```bash
aws s3api put-bucket-versioning \
  --bucket mwanzo-videos-prod \
  --versioning-configuration Status=Enabled
```

---

### CloudFront Setup

**1. Create CloudFront Distribution:**
```bash
# Use AWS Console or CLI
# Origin: S3 bucket (mwanzo-videos-prod)
# Behavior: Viewer Protocol Policy = Redirect HTTP to HTTPS
# Cache: CachingOptimized policy
```

**2. Generate CloudFront Key Pair:**
```bash
# AWS Console → CloudFront → Key pairs
# Download private key and store in AWS Secrets Manager
```

**3. Configure Custom Domain:**
```bash
# Add CNAME record in Route 53
# cdn.mwanzoskills.co.ke → d1234abcd.cloudfront.net
```

---

### MediaConvert Setup

**1. Create IAM Role:**
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

**2. Create Job Template:**
```bash
# Use AWS Console to create template
# Name: MwanzoVideoTemplate
# Outputs:
#   - 360p HLS
#   - 720p HLS
#   - 1080p HLS
```

---

### Load Balancer Setup

**1. Create Application Load Balancer:**
```bash
aws elbv2 create-load-balancer \
  --name mwanzo-alb \
  --subnets subnet-xxx subnet-yyy \
  --security-groups sg-xxx \
  --scheme internet-facing
```

**2. Create Target Group:**
```bash
aws elbv2 create-target-group \
  --name mwanzo-backend-tg \
  --protocol HTTP \
  --port 8080 \
  --vpc-id vpc-xxx \
  --health-check-path /actuator/health
```

**3. Add SSL Certificate:**
```bash
# Request certificate in AWS Certificate Manager
aws acm request-certificate \
  --domain-name api.mwanzoskills.co.ke \
  --validation-method DNS

# Add HTTPS listener
aws elbv2 create-listener \
  --load-balancer-arn arn:aws:elasticloadbalancing:... \
  --protocol HTTPS \
  --port 443 \
  --certificates CertificateArn=arn:aws:acm:... \
  --default-actions Type=forward,TargetGroupArn=arn:aws:elasticloadbalancing:...
```

---

### Auto Scaling Configuration

**1. Create Launch Template:**
```bash
aws ec2 create-launch-template \
  --launch-template-name mwanzo-backend-template \
  --version-description "v1" \
  --launch-template-data '{
    "ImageId": "ami-xxx",
    "InstanceType": "t3.large",
    "SecurityGroupIds": ["sg-xxx"],
    "IamInstanceProfile": {"Arn": "arn:aws:iam::..."},
    "UserData": "IyEvYmluL2Jhc2gKZG9ja2VyIHJ1biAtZCAtcCA4MDgwOjgwODAg..."
  }'
```

**2. Create Auto Scaling Group:**
```bash
aws autoscaling create-auto-scaling-group \
  --auto-scaling-group-name mwanzo-backend-asg \
  --launch-template LaunchTemplateName=mwanzo-backend-template \
  --min-size 2 \
  --max-size 10 \
  --desired-capacity 2 \
  --health-check-type ELB \
  --health-check-grace-period 300 \
  --target-group-arns arn:aws:elasticloadbalancing:... \
  --vpc-zone-identifier "subnet-xxx,subnet-yyy"
```

**3. Configure Scaling Policies:**
```bash
# Scale up on high CPU
aws autoscaling put-scaling-policy \
  --auto-scaling-group-name mwanzo-backend-asg \
  --policy-name scale-up \
  --scaling-adjustment 2 \
  --adjustment-type ChangeInCapacity \
  --cooldown 300

# Scale down on low CPU
aws autoscaling put-scaling-policy \
  --auto-scaling-group-name mwanzo-backend-asg \
  --policy-name scale-down \
  --scaling-adjustment -1 \
  --adjustment-type ChangeInCapacity \
  --cooldown 300
```

---

## Part 2: Frontend Deployment (Vercel)

### 1. Prepare Frontend

**Update Environment Variables:**
```env
# .env.production
VITE_API_BASE_URL=https://api.mwanzoskills.co.ke/api/v1
```

**Verify Build:**
```bash
cd src/mwanzo-platform-main
npm run build
npm run preview  # Test production build locally
```

---

### 2. Deploy to Vercel

**Method 1: Vercel CLI (Recommended)**
```bash
# Login to Vercel
vercel login

# Deploy
vercel --prod

# Follow prompts:
# - Link to project
# - Set environment variables
# - Deploy
```

**Method 2: Git Integration (Auto-Deploy)**
```bash
# Push to GitHub
git add .
git commit -m "Deploy to production"
git push origin main

# Vercel auto-deploys on push (if connected to repo)
```

---

### 3. Custom Domain Setup

**Add Domain in Vercel:**
```bash
# Vercel Dashboard → Project → Settings → Domains
# Add: mwanzoskills.co.ke
# Add: www.mwanzoskills.co.ke
```

**Configure DNS:**
```bash
# Add these records in your DNS provider:
# A     @       76.76.21.21
# CNAME www     cname.vercel-dns.com
```

---

### 4. Environment Variables in Vercel

**Set via Dashboard:**
```
Vercel Dashboard → Project → Settings → Environment Variables

Production:
  VITE_API_BASE_URL = https://api.mwanzoskills.co.ke/api/v1

Preview:
  VITE_API_BASE_URL = https://api-staging.mwanzoskills.co.ke/api/v1

Development:
  VITE_API_BASE_URL = http://localhost:8080/api/v1
```

---

## Part 3: Domain & DNS Configuration

### 1. Route 53 Setup

**Create Hosted Zone:**
```bash
aws route53 create-hosted-zone \
  --name mwanzoskills.co.ke \
  --caller-reference $(date +%s)
```

**Add Records:**
```bash
# Backend API (ALB)
api.mwanzoskills.co.ke → ALB DNS (mwanzo-alb-xxx.us-east-1.elb.amazonaws.com)

# CDN (CloudFront)
cdn.mwanzoskills.co.ke → CloudFront (d1234abcd.cloudfront.net)

# Frontend (Vercel) - handled by Vercel
mwanzoskills.co.ke → Vercel
www.mwanzoskills.co.ke → Vercel
```

---

## Part 4: Monitoring & Logging

### 1. CloudWatch Setup

**Create Log Groups:**
```bash
aws logs create-log-group --log-group-name /aws/elasticbeanstalk/mwanzo-backend
aws logs create-log-group --log-group-name /aws/mediaconvert/mwanzo
```

**Create Alarms:**
```bash
# High CPU alarm
aws cloudwatch put-metric-alarm \
  --alarm-name mwanzo-high-cpu \
  --alarm-description "Alert when CPU exceeds 80%" \
  --metric-name CPUUtilization \
  --namespace AWS/EC2 \
  --statistic Average \
  --period 300 \
  --threshold 80 \
  --comparison-operator GreaterThanThreshold \
  --evaluation-periods 2 \
  --alarm-actions arn:aws:sns:us-east-1:123456789012:mwanzo-alerts

# Database connection alarm
aws cloudwatch put-metric-alarm \
  --alarm-name mwanzo-db-connections \
  --metric-name DatabaseConnections \
  --namespace AWS/RDS \
  --statistic Average \
  --period 300 \
  --threshold 90 \
  --comparison-operator GreaterThanThreshold
```

---

### 2. Application Monitoring

**Spring Boot Actuator Endpoints:**
```yaml
# application-prod.yml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,info
  endpoint:
    health:
      show-details: when-authorized
```

**Access Metrics:**
```bash
curl https://api.mwanzoskills.co.ke/actuator/health
curl https://api.mwanzoskills.co.ke/actuator/metrics
```

---

## Part 5: CI/CD Pipeline

### GitHub Actions Workflow

```yaml
# .github/workflows/deploy-prod.yml
name: Deploy to Production

on:
  push:
    branches: [main]

jobs:
  deploy-backend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Setup Java
        uses: actions/setup-java@v3
        with:
          java-version: '17'

      - name: Build JAR
        run: ./mvnw clean package -DskipTests

      - name: Build Docker image
        run: docker build -t mwanzo-backend .

      - name: Push to ECR
        run: |
          aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin $ECR_REGISTRY
          docker tag mwanzo-backend:latest $ECR_REGISTRY/mwanzo-backend:latest
          docker push $ECR_REGISTRY/mwanzo-backend:latest

      - name: Deploy to ECS
        run: |
          aws ecs update-service --cluster mwanzo-cluster --service mwanzo-backend --force-new-deployment

  deploy-frontend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Setup Node
        uses: actions/setup-node@v3
        with:
          node-version: '18'

      - name: Deploy to Vercel
        uses: amondnet/vercel-action@v25
        with:
          vercel-token: ${{ secrets.VERCEL_TOKEN }}
          vercel-org-id: ${{ secrets.VERCEL_ORG_ID }}
          vercel-project-id: ${{ secrets.VERCEL_PROJECT_ID }}
          vercel-args: '--prod'
```

---

## Part 6: Security Hardening

### 1. Network Security

**Security Groups:**
```bash
# ALB Security Group (allow 80, 443)
# Backend Security Group (allow 8080 from ALB only)
# RDS Security Group (allow 5432 from Backend only)
```

**VPC Configuration:**
```bash
# Public subnets: ALB
# Private subnets: Backend instances, RDS
# NAT Gateway: For backend to access internet (S3, MediaConvert)
```

---

### 2. Secrets Management

**Use AWS Secrets Manager:**
```bash
# Store sensitive data
aws secretsmanager create-secret \
  --name mwanzo/prod/database \
  --secret-string '{
    "username":"mwanzo_admin",
    "password":"SecurePassword123!",
    "host":"mwanzo-prod-db.abc123.us-east-1.rds.amazonaws.com"
  }'

# Store JWT secret
aws secretsmanager create-secret \
  --name mwanzo/prod/jwt \
  --secret-string '{"secret":"'$(openssl rand -base64 32)'"}'
```

**Retrieve in Application:**
```java
// Application reads from Secrets Manager on startup
@Value("${aws.secretsmanager.secret-name}")
private String secretName;
```

---

## Part 7: Cost Optimization

### 1. Reserved Instances
```bash
# Save up to 72% by committing to 1-3 years
# RDS Reserved Instance: ~60% savings
# EC2 Reserved Instances: ~72% savings
```

### 2. S3 Lifecycle Policies
```json
{
  "Rules": [
    {
      "Id": "MoveToGlacier",
      "Status": "Enabled",
      "Transitions": [
        {
          "Days": 90,
          "StorageClass": "GLACIER"
        }
      ]
    }
  ]
}
```

### 3. CloudFront Optimization
- Enable compression
- Use price class: "Use US, Europe, Asia"
- Invalidation only when necessary

---

## Part 8: Disaster Recovery

### 1. Database Backups
```bash
# Automated daily backups (enabled)
# Manual snapshots before major changes
aws rds create-db-snapshot \
  --db-instance-identifier mwanzo-prod-db \
  --db-snapshot-identifier mwanzo-prod-backup-$(date +%Y%m%d)
```

### 2. Application Backups
```bash
# Docker images in ECR (retained)
# Code in GitHub (version controlled)
# Configuration in Secrets Manager (backed up)
```

---

## Deployment Checklist

### Pre-Deployment
- [ ] All tests passing
- [ ] Environment variables configured
- [ ] Database migrations tested
- [ ] SSL certificates ready
- [ ] DNS records prepared
- [ ] Monitoring configured

### Deployment
- [ ] Deploy database changes first
- [ ] Deploy backend
- [ ] Verify backend health
- [ ] Deploy frontend
- [ ] Verify frontend

### Post-Deployment
- [ ] Test critical flows (login, enrollment, payment)
- [ ] Check logs for errors
- [ ] Monitor performance metrics
- [ ] Verify email notifications
- [ ] Test video upload/streaming

---

## Rollback Procedure

### Backend Rollback
```bash
# ECS: Deploy previous task definition
aws ecs update-service \
  --cluster mwanzo-cluster \
  --service mwanzo-backend \
  --task-definition mwanzo-backend:previous-version

# Elastic Beanstalk: Restore previous version
eb deploy --version previous-version-label
```

### Frontend Rollback
```bash
# Vercel: Rollback to previous deployment
vercel rollback [deployment-url]
```

---

## Troubleshooting

### Backend Not Responding
```bash
# Check ECS service
aws ecs describe-services --cluster mwanzo-cluster --services mwanzo-backend

# Check CloudWatch logs
aws logs tail /aws/elasticbeanstalk/mwanzo-backend --follow

# Check RDS connection
psql -h mwanzo-prod-db.abc123.us-east-1.rds.amazonaws.com -U mwanzo_admin
```

### Frontend Not Loading
```bash
# Check Vercel deployment logs
vercel logs [deployment-url]

# Verify DNS
dig mwanzoskills.co.ke
```

### Videos Not Streaming
```bash
# Check S3 bucket
aws s3 ls s3://mwanzo-videos-prod

# Check CloudFront distribution
aws cloudfront get-distribution --id E1234ABCD
```

---

**Estimated Deployment Time:**
- Initial setup: 2-3 hours
- Subsequent deploys: 5-10 minutes

**Monthly Cost Estimate:**
- **Minimal Traffic (< 1000 users):** ~$150/month
- **Moderate Traffic (1000-10000 users):** ~$300/month
- **High Traffic (10000+ users):** ~$500-1000/month (with scaling)

---

**Last Updated:** January 18, 2026
