/**
 * Terraform Outputs
 *
 * Important values to use after deployment
 */

# Network Outputs
output "vpc_id" {
  description = "VPC ID"
  value       = aws_vpc.main.id
}

output "public_subnet_ids" {
  description = "Public subnet IDs"
  value       = aws_subnet.public[*].id
}

output "private_subnet_ids" {
  description = "Private subnet IDs"
  value       = aws_subnet.private[*].id
}

# Backend Application
output "backend_url" {
  description = "Backend API URL (use this for frontend)"
  value       = "http://${aws_lb.main.dns_name}"
}

output "backend_health_check" {
  description = "Backend health check URL"
  value       = "http://${aws_lb.main.dns_name}/actuator/health"
}

# Database
output "database_endpoint" {
  description = "RDS PostgreSQL endpoint"
  value       = aws_db_instance.main.endpoint
}

output "database_connection_string" {
  description = "JDBC connection string"
  value       = "jdbc:postgresql://${aws_db_instance.main.address}:${aws_db_instance.main.port}/${var.db_name}"
}

# S3 and CloudFront
output "s3_videos_bucket" {
  description = "S3 bucket name for videos"
  value       = aws_s3_bucket.videos.id
}

output "cdn_url" {
  description = "CloudFront CDN URL (use this for video delivery)"
  value       = var.enable_cloudfront ? "https://${aws_cloudfront_distribution.videos[0].domain_name}" : "N/A - CloudFront disabled"
}

# IAM Roles
output "mediaconvert_role_arn" {
  description = "MediaConvert service role ARN (use in application.yml)"
  value       = aws_iam_role.mediaconvert.arn
}

# Environment Variables for Application
output "application_env_vars" {
  description = "Environment variables for application configuration"
  value = {
    AWS_REGION                      = var.aws_region
    AWS_S3_BUCKET                   = aws_s3_bucket.videos.id
    AWS_S3_REGION                   = var.aws_region
    AWS_MEDIACONVERT_ROLE_ARN       = aws_iam_role.mediaconvert.arn
    AWS_MEDIACONVERT_OUTPUT_BUCKET  = aws_s3_bucket.videos.id
    AWS_CLOUDFRONT_ENABLED          = var.enable_cloudfront
    AWS_CLOUDFRONT_DOMAIN           = var.enable_cloudfront ? "https://${aws_cloudfront_distribution.videos[0].domain_name}" : ""
    DATABASE_HOST                   = aws_db_instance.main.address
    DATABASE_PORT                   = aws_db_instance.main.port
    DATABASE_NAME                   = var.db_name
  }
}

# Secrets ARNs
output "secrets" {
  description = "AWS Secrets Manager secret ARNs"
  value = {
    database_credentials = aws_secretsmanager_secret.db_credentials.arn
    jwt_secret           = aws_secretsmanager_secret.jwt_secret.arn
  }
  sensitive = true
}

# Next Steps
output "next_steps" {
  description = "What to do after deployment"
  value = <<-EOT

    ✅ Infrastructure Deployed Successfully!

    📝 NEXT STEPS:

    1. Get MediaConvert Endpoint:
       aws mediaconvert describe-endpoints --region ${var.aws_region}
       Add to application-prod.yml: aws.mediaconvert.endpoint

    2. Test Backend Health:
       curl http://${aws_lb.main.dns_name}/actuator/health

    3. Configure Frontend:
       Update frontend .env with:
       VITE_API_URL=http://${aws_lb.main.dns_name}

    4. Database Migration:
       Connect to RDS and run Flyway migrations:
       Host: ${aws_db_instance.main.address}
       Port: ${aws_db_instance.main.port}
       Database: ${var.db_name}

    5. Upload Test Video:
       Use Postman to test video upload flow

    6. Monitor Logs:
       aws logs tail /ecs/${var.project_name}-backend-${var.environment} --follow

    7. Set Up Custom Domain (Optional):
       - Create Route 53 hosted zone
       - Point domain to ALB: ${aws_lb.main.dns_name}
       - Request ACM certificate
       - Update ALB listener with HTTPS

    📊 ESTIMATED MONTHLY COST: $130-150

    🔗 USEFUL LINKS:
    - Backend: http://${aws_lb.main.dns_name}
    - Health Check: http://${aws_lb.main.dns_name}/actuator/health
    - CloudFront: ${var.enable_cloudfront ? "https://${aws_cloudfront_distribution.videos[0].domain_name}" : "Disabled"}

  EOT
}
