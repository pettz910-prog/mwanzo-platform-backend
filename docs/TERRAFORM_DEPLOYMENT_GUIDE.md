# Terraform Deployment Guide

Complete step-by-step guide to deploy the Mwanzo Course Platform to AWS using Terraform.

**Cost-Effective Production Deployment: ~$130-150/month**

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [What Gets Deployed](#what-gets-deployed)
3. [Installation](#installation)
4. [Configuration](#configuration)
5. [Deployment Steps](#deployment-steps)
6. [Post-Deployment](#post-deployment)
7. [Cost Breakdown](#cost-breakdown)
8. [Scaling](#scaling)
9. [Troubleshooting](#troubleshooting)
10. [Cleanup](#cleanup)

---

## Prerequisites

### Required Tools

1. **Terraform** (v1.0+)
   ```bash
   # macOS
   brew install terraform

   # Windows (Chocolatey)
   choco install terraform

   # Linux
   wget https://releases.hashicorp.com/terraform/1.6.0/terraform_1.6.0_linux_amd64.zip
   unzip terraform_1.6.0_linux_amd64.zip
   sudo mv terraform /usr/local/bin/
   ```

2. **AWS CLI** (v2)
   ```bash
   # macOS
   brew install awscli

   # Windows
   # Download from: https://aws.amazon.com/cli/

   # Linux
   curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
   unzip awscliv2.zip
   sudo ./aws/install
   ```

3. **Docker** (for building backend image)
   ```bash
   # macOS
   brew install docker

   # Windows/Linux
   # Download from: https://www.docker.com/products/docker-desktop
   ```

### AWS Requirements

1. **AWS Account** with administrator access
2. **AWS CLI Configured**:
   ```bash
   aws configure
   # Enter:
   # - AWS Access Key ID
   # - AWS Secret Access Key
   # - Default region: us-east-1
   # - Default output format: json
   ```

3. **Verify AWS Access**:
   ```bash
   aws sts get-caller-identity
   ```

---

## What Gets Deployed

### Infrastructure Components

```
┌─────────────────────────────────────────────────────────┐
│                     AWS Cloud                           │
│                                                         │
│  ┌──────────────────────────────────────────────────┐  │
│  │  VPC (10.0.0.0/16)                               │  │
│  │                                                  │  │
│  │  ┌─────────────┐         ┌──────────────┐      │  │
│  │  │  Public     │         │  Private     │      │  │
│  │  │  Subnets    │         │  Subnets     │      │  │
│  │  │             │         │              │      │  │
│  │  │ ┌─────────┐ │         │ ┌──────────┐│      │  │
│  │  │ │   ALB   │ │         │ │   ECS    ││      │  │
│  │  │ │  (HTTP) │─┼────────▶│ │  Tasks   ││      │  │
│  │  │ └─────────┘ │         │ └──────────┘│      │  │
│  │  │             │         │              │      │  │
│  │  │             │         │ ┌──────────┐│      │  │
│  │  │             │         │ │   RDS    ││      │  │
│  │  │             │         │ │PostgreSQL││      │  │
│  │  │             │         │ └──────────┘│      │  │
│  │  └─────────────┘         └──────────────┘      │  │
│  │                                                  │  │
│  └──────────────────────────────────────────────────┘  │
│                                                         │
│  ┌──────────────┐   ┌────────────┐   ┌─────────────┐  │
│  │  S3 Bucket   │   │MediaConvert│   │ CloudFront  │  │
│  │   (Videos)   │──▶│  (Transcode│──▶│    (CDN)    │  │
│  └──────────────┘   └────────────┘   └─────────────┘  │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

### Resources Created

| Resource | Purpose | Estimated Cost/Month |
|----------|---------|---------------------|
| **VPC** | Network isolation | Free |
| **2 Public Subnets** | ALB placement | Free |
| **2 Private Subnets** | ECS + RDS | Free |
| **Internet Gateway** | Internet access | Free |
| **NAT Gateway** | Private subnet internet | $32.00 |
| **Application Load Balancer** | Traffic distribution | $16.00 |
| **ECS Fargate (2 tasks)** | Backend containers | $30.00 |
| **RDS PostgreSQL (db.t3.small)** | Database | $25.00 |
| **S3 Bucket** | Video storage (500GB) | $11.50 |
| **CloudFront** | CDN (500GB transfer) | $42.50 |
| **Data Transfer** | ~50GB out | $4.50 |
| **Secrets Manager** | Credentials storage | $0.80 |
| **CloudWatch Logs** | Application logs | $2.00 |
| **Total** | | **~$164/month** |

---

## Installation

### Step 1: Clone Repository

```bash
cd mwanzo-course-platform-backend
ls terraform/  # Should see *.tf files
```

### Step 2: Build Backend Docker Image

```bash
# Build Spring Boot application
./mvnw clean package -DskipTests

# Build Docker image
docker build -t mwanzo-backend:latest .

# Create ECR repository
aws ecr create-repository --repository-name mwanzo-backend --region us-east-1

# Login to ECR
aws ecr get-login-password --region us-east-1 | \
  docker login --username AWS --password-stdin <YOUR_ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com

# Tag and push image
docker tag mwanzo-backend:latest <YOUR_ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com/mwanzo-backend:latest
docker push <YOUR_ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com/mwanzo-backend:latest
```

---

## Configuration

### Step 3: Configure Terraform Variables

```bash
cd terraform/

# Copy example variables
cp terraform.tfvars.example terraform.tfvars

# Edit terraform.tfvars
nano terraform.tfvars
```

**terraform.tfvars**:
```hcl
# AWS Configuration
aws_region   = "us-east-1"
environment  = "prod"
project_name = "mwanzo"

# Database (CHANGE THESE!)
db_username = "mwanzo_admin"
db_password = "YourStrongPassword123!@#"  # Min 16 chars

# Backend Docker Image (UPDATE THIS!)
backend_docker_image = "123456789012.dkr.ecr.us-east-1.amazonaws.com/mwanzo-backend:latest"

# S3 Bucket (must be globally unique)
s3_video_bucket_name = "mwanzo-videos-prod-yourname"

# JWT Secret (CHANGE THIS!)
jwt_secret = "generate-with-openssl-rand-base64-32"

# Cost Optimization
enable_nat_gateway  = true   # Required for private subnets to access internet
enable_multi_az_rds = false  # Enable for HA (doubles RDS cost)
enable_cloudfront   = true   # Recommended
```

**Generate Secure Secrets**:
```bash
# Generate JWT secret
openssl rand -base64 32

# Generate strong DB password
openssl rand -base64 24
```

### Step 4: Review Configuration

```bash
# Check Terraform version
terraform version

# Review what will be created
cat main.tf vpc.tf ecs.tf
```

---

## Deployment Steps

### Step 5: Initialize Terraform

```bash
cd terraform/

# Initialize Terraform (downloads providers)
terraform init
```

**Expected Output**:
```
Initializing the backend...
Initializing provider plugins...
- Finding hashicorp/aws versions matching "~> 5.0"...
- Installing hashicorp/aws v5.31.0...

Terraform has been successfully initialized!
```

### Step 6: Plan Deployment

```bash
# Generate execution plan
terraform plan -out=tfplan

# Review changes
# Should show ~50-60 resources to create
```

**Review Key Outputs**:
- VPC and subnets
- Security groups
- RDS instance
- ECS cluster and service
- Load balancer
- S3 bucket
- CloudFront distribution

### Step 7: Deploy Infrastructure

```bash
# Apply the plan
terraform apply tfplan

# This will take ~15-20 minutes
# Terraform will create all resources
```

**Progress Indicators**:
```
aws_vpc.main: Creating...
aws_vpc.main: Creation complete after 3s
aws_subnet.public[0]: Creating...
aws_subnet.private[0]: Creating...
...
aws_ecs_service.backend: Still creating... [5m0s elapsed]
aws_rds_instance.main: Still creating... [10m0s elapsed]
...

Apply complete! Resources: 58 added, 0 changed, 0 destroyed.

Outputs:

backend_url = "http://mwanzo-alb-prod-1234567890.us-east-1.elb.amazonaws.com"
```

### Step 8: Save Outputs

```bash
# Save all outputs to file
terraform output -json > terraform-outputs.json

# Get backend URL
terraform output backend_url

# Get database endpoint
terraform output database_endpoint
```

---

## Post-Deployment

### Step 9: Get MediaConvert Endpoint

```bash
# Get your account's MediaConvert endpoint
aws mediaconvert describe-endpoints --region us-east-1
```

**Output**:
```json
{
  "Endpoints": [
    {
      "Url": "https://abcdefg123.mediaconvert.us-east-1.amazonaws.com"
    }
  ]
}
```

**Save this URL** - needed for application configuration.

### Step 10: Update Application Configuration

The application configuration is set via ECS environment variables (already done in `ecs.tf`).

To update MediaConvert endpoint manually:

```bash
# Update ECS task definition with MediaConvert endpoint
# Edit ecs.tf and add:
{
  name  = "AWS_MEDIACONVERT_ENDPOINT"
  value = "https://YOUR_ENDPOINT.mediaconvert.us-east-1.amazonaws.com"
}

# Re-deploy
terraform apply
```

### Step 11: Run Database Migrations

```bash
# Get database endpoint
DB_HOST=$(terraform output -raw rds_endpoint)
DB_USER=$(terraform output -raw db_username)

# Connect via psql (if installed)
psql -h $DB_HOST -U $DB_USER -d mwanzo_prod

# Or use Spring Boot Flyway (automatic on first run)
# Backend will run migrations on startup
```

### Step 12: Test Backend Health

```bash
# Get backend URL
BACKEND_URL=$(terraform output -raw backend_url)

# Test health endpoint
curl $BACKEND_URL/actuator/health

# Expected response:
# {"status":"UP"}
```

### Step 13: Test API Endpoints

```bash
# Get all courses
curl $BACKEND_URL/api/v1/courses

# Get categories
curl $BACKEND_URL/api/v1/categories
```

### Step 14: Configure Frontend

Update frontend `.env.production`:

```bash
VITE_API_URL=http://mwanzo-alb-prod-xxxx.us-east-1.elb.amazonaws.com
```

Deploy frontend to Vercel:

```bash
cd ../frontend
vercel --prod
```

---

## Cost Breakdown

### Monthly Cost Estimate (Light Traffic)

| Service | Configuration | Monthly Cost |
|---------|--------------|--------------|
| ECS Fargate | 2 tasks × 1vCPU × 2GB | $30.00 |
| RDS PostgreSQL | db.t3.small, 20GB | $25.00 |
| ALB | 1 instance | $16.00 |
| NAT Gateway | 1 instance + 50GB | $36.00 |
| S3 | 500GB storage + requests | $12.00 |
| CloudFront | 500GB transfer | $42.50 |
| Data Transfer | 50GB out | $4.50 |
| Secrets Manager | 2 secrets | $0.80 |
| CloudWatch Logs | 5GB | $2.00 |
| **Total** | | **~$169/month** |

### Cost Optimization Tips

**Save $32/month - Remove NAT Gateway**:
```hcl
enable_nat_gateway = false
```
⚠️ Private subnets lose internet access (MediaConvert won't work)

**Save $16/month - Use Network Load Balancer**:
- NLB is cheaper but less features
- Not included in this config

**Save $10/month - Use db.t3.micro**:
```hcl
db_instance_class = "db.t3.micro"
```
⚠️ Free Tier eligible but lower performance

**Save $20/month - Reduce ECS tasks**:
```hcl
backend_desired_count = 1
```
⚠️ No redundancy, higher latency

**Optimized Budget Config (~$110/month)**:
```hcl
backend_desired_count = 1
db_instance_class     = "db.t3.micro"
enable_multi_az_rds   = false
enable_cloudfront     = true  # Keep this, saves S3 costs
```

---

## Scaling

### Horizontal Scaling (More Containers)

```hcl
# terraform.tfvars
backend_desired_count = 4  # Scale from 2 to 4 containers
```

```bash
terraform apply
```

### Vertical Scaling (Bigger Containers)

```hcl
# terraform.tfvars
backend_cpu    = 2048  # 2 vCPU (was 1024)
backend_memory = 4096  # 4 GB (was 2048)
```

```bash
terraform apply
```

### Auto Scaling (Automatic)

Already configured in `ecs.tf`:
- Scales up when CPU > 70%
- Scales up when Memory > 80%
- Max 10 containers

### Database Scaling

```hcl
# terraform.tfvars
db_instance_class = "db.t3.medium"  # Upgrade from t3.small
```

```bash
terraform apply
# RDS will have brief downtime during upgrade
```

---

## Troubleshooting

### Issue: Terraform init fails

**Error**: "Failed to download provider"

**Solution**:
```bash
# Clear Terraform cache
rm -rf .terraform .terraform.lock.hcl

# Re-initialize
terraform init
```

### Issue: "Bucket name already exists"

**Error**: `aws_s3_bucket.videos: creating... Error: BucketAlreadyExists`

**Solution**: S3 bucket names must be globally unique:
```hcl
# terraform.tfvars
s3_video_bucket_name = "mwanzo-videos-prod-yourname-12345"
```

### Issue: ECS tasks not starting

**Check logs**:
```bash
# Get cluster and service name
terraform output ecs_cluster_name
terraform output ecs_service_name

# View ECS events
aws ecs describe-services \
  --cluster mwanzo-cluster-prod \
  --services mwanzo-backend-service-prod

# View container logs
aws logs tail /ecs/mwanzo-backend-prod --follow
```

**Common causes**:
1. Docker image not pushed to ECR
2. Database credentials wrong
3. IAM permissions insufficient

### Issue: Health check failing

**Test health endpoint directly**:
```bash
# Get task IP
aws ecs list-tasks --cluster mwanzo-cluster-prod

# Test from within VPC (requires bastion host)
curl http://<TASK_IP>:8080/actuator/health
```

**Check**:
- Database connection
- Environment variables
- Application logs

### Issue: High costs

**Identify cost drivers**:
```bash
# View AWS cost explorer
aws ce get-cost-and-usage \
  --time-period Start=2025-01-01,End=2025-01-31 \
  --granularity DAILY \
  --metrics BlendedCost \
  --group-by Type=DIMENSION,Key=SERVICE
```

**Common culprits**:
- NAT Gateway ($32/month)
- CloudFront data transfer (pay-per-GB)
- RDS Multi-AZ (doubles cost)

---

## Cleanup

### Full Teardown

```bash
cd terraform/

# Destroy all resources
terraform destroy

# Confirm: yes

# This will take ~10 minutes
```

**Resources Destroyed**:
- All ECS tasks stopped
- Load balancer deleted
- RDS final snapshot created
- S3 bucket **NOT** deleted (manual step)

### Manual Cleanup

```bash
# Delete S3 bucket (careful - deletes all videos!)
aws s3 rm s3://mwanzo-videos-prod --recursive
aws s3 rb s3://mwanzo-videos-prod

# Delete ECR images
aws ecr delete-repository \
  --repository-name mwanzo-backend \
  --force

# Delete RDS snapshots (if keeping them costs money)
aws rds describe-db-snapshots --query 'DBSnapshots[*].DBSnapshotIdentifier'
aws rds delete-db-snapshot --db-snapshot-identifier mwanzo-db-final-snapshot-xxx
```

---

## Next Steps

1. ✅ Infrastructure deployed
2. 🔒 Set up custom domain with HTTPS (Route 53 + ACM)
3. 📊 Configure monitoring alerts (CloudWatch)
4. 🔄 Set up CI/CD pipeline (GitHub Actions)
5. 🧪 Run load tests (Apache JMeter)
6. 📝 Document API endpoints
7. 🎨 Deploy frontend to Vercel

---

## Additional Resources

- [AWS Well-Architected Framework](https://aws.amazon.com/architecture/well-architected/)
- [Terraform AWS Provider Docs](https://registry.terraform.io/providers/hashicorp/aws/latest/docs)
- [ECS Best Practices](https://docs.aws.amazon.com/AmazonECS/latest/bestpracticesguide/intro.html)
- [RDS Best Practices](https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/CHAP_BestPractices.html)

---

**Questions?** Check the main [AWS_INTEGRATION_GUIDE.md](./AWS_INTEGRATION_GUIDE.md) for AWS-specific configuration details.
