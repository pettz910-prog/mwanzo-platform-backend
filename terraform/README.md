# Terraform Infrastructure - Quick Reference

Complete AWS infrastructure for Mwanzo Course Platform.

## Quick Start

```bash
# 1. Configure variables
cp terraform.tfvars.example terraform.tfvars
nano terraform.tfvars  # Edit with your values

# 2. Initialize
terraform init

# 3. Plan
terraform plan -out=tfplan

# 4. Deploy
terraform apply tfplan

# 5. Get outputs
terraform output backend_url
```

## Files

| File | Purpose |
|------|---------|
| `main.tf` | Provider and backend configuration |
| `variables.tf` | Input variables |
| `vpc.tf` | Network (VPC, subnets, security groups) |
| `rds.tf` | PostgreSQL database |
| `s3.tf` | Video storage bucket |
| `cloudfront.tf` | CDN for video delivery |
| `iam.tf` | IAM roles and policies |
| `ecs.tf` | Container service (backend) |
| `outputs.tf` | Output values |

## Cost Estimate

**~$130-170/month** for production with:
- 2 ECS containers (Fargate)
- RDS PostgreSQL (db.t3.small)
- 500GB S3 + CloudFront
- Application Load Balancer
- NAT Gateway

## Key Outputs

```bash
# Backend API URL
terraform output backend_url

# Database endpoint
terraform output database_endpoint

# CDN URL
terraform output cdn_url

# All environment variables
terraform output application_env_vars
```

## Common Commands

```bash
# View plan
terraform plan

# Apply changes
terraform apply

# Destroy everything
terraform destroy

# Show current state
terraform show

# List resources
terraform state list

# Get specific output
terraform output -raw backend_url
```

## Cost Optimization

Save **$40/month** with budget configuration:

```hcl
# terraform.tfvars
backend_desired_count = 1           # Save $15/mo
db_instance_class     = "db.t3.micro"  # Save $10/mo (Free Tier)
enable_multi_az_rds   = false       # Already disabled
enable_nat_gateway    = true        # Keep this (MediaConvert needs it)
enable_cloudfront     = true        # Keep this (saves S3 costs)
```

## Troubleshooting

**Issue**: Bucket name already exists
```bash
# Edit terraform.tfvars
s3_video_bucket_name = "mwanzo-videos-prod-unique-name-123"
```

**Issue**: ECS tasks not starting
```bash
# Check logs
aws logs tail /ecs/mwanzo-backend-prod --follow
```

**Issue**: Database connection failed
```bash
# Verify security groups allow traffic
terraform output | grep security
```

## Documentation

- **Full Guide**: [../docs/TERRAFORM_DEPLOYMENT_GUIDE.md](../docs/TERRAFORM_DEPLOYMENT_GUIDE.md)
- **AWS Integration**: [../docs/AWS_INTEGRATION_GUIDE.md](../docs/AWS_INTEGRATION_GUIDE.md)

## Architecture

```
Internet
    ↓
Application Load Balancer (Public Subnets)
    ↓
ECS Fargate Tasks (Private Subnets)
    ↓
RDS PostgreSQL (Private Subnets)

Videos: S3 → MediaConvert → CloudFront
```

## Security

- All secrets in AWS Secrets Manager
- Private subnets for ECS and RDS
- Security groups restrict access
- Encryption at rest (S3, RDS)
- HTTPS only via CloudFront

## Next Steps

1. Deploy: `terraform apply`
2. Get MediaConvert endpoint: `aws mediaconvert describe-endpoints`
3. Test health: `curl $(terraform output -raw backend_url)/actuator/health`
4. Deploy frontend to Vercel
5. Configure custom domain (optional)
