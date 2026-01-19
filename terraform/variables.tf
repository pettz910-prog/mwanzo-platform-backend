/**
 * Terraform Variables
 *
 * Configure these values in terraform.tfvars
 */

variable "aws_region" {
  description = "AWS region for resources"
  type        = string
  default     = "us-east-1"
}

variable "environment" {
  description = "Environment name (dev, staging, prod)"
  type        = string
  default     = "prod"
}

variable "project_name" {
  description = "Project name for resource naming"
  type        = string
  default     = "mwanzo"
}

# VPC Configuration
variable "vpc_cidr" {
  description = "CIDR block for VPC"
  type        = string
  default     = "10.0.0.0/16"
}

variable "availability_zones" {
  description = "Number of availability zones (2-3 recommended)"
  type        = number
  default     = 2
}

# RDS Configuration
variable "db_instance_class" {
  description = "RDS instance type (db.t3.micro for free tier, db.t3.small for prod)"
  type        = string
  default     = "db.t3.small"
}

variable "db_allocated_storage" {
  description = "RDS storage in GB"
  type        = number
  default     = 20
}

variable "db_name" {
  description = "Database name"
  type        = string
  default     = "mwanzo_prod"
}

variable "db_username" {
  description = "Database master username"
  type        = string
  default     = "mwanzo_admin"
  sensitive   = true
}

variable "db_password" {
  description = "Database master password (set in terraform.tfvars)"
  type        = string
  sensitive   = true
}

# ECS Configuration
variable "backend_cpu" {
  description = "CPU units for backend container (1024 = 1 vCPU)"
  type        = number
  default     = 1024  # 1 vCPU
}

variable "backend_memory" {
  description = "Memory for backend container in MB"
  type        = number
  default     = 2048  # 2 GB
}

variable "backend_desired_count" {
  description = "Number of backend containers to run"
  type        = number
  default     = 2
}

variable "backend_docker_image" {
  description = "Docker image for backend (ECR or Docker Hub)"
  type        = string
  default     = "mwanzo/backend:latest"
}

# S3 Configuration
variable "s3_video_bucket_name" {
  description = "S3 bucket name for videos"
  type        = string
  default     = "mwanzo-videos-prod"
}

# Domain Configuration
variable "domain_name" {
  description = "Domain name for the platform (e.g., mwanzo.com)"
  type        = string
  default     = ""
}

variable "cdn_domain_name" {
  description = "CDN subdomain (e.g., cdn.mwanzo.com)"
  type        = string
  default     = ""
}

# Cost Optimization
variable "enable_nat_gateway" {
  description = "Enable NAT Gateway (costs $32/month but required for private subnets to access internet)"
  type        = bool
  default     = true
}

variable "enable_multi_az_rds" {
  description = "Enable RDS Multi-AZ (doubles RDS cost but provides HA)"
  type        = bool
  default     = false
}

variable "enable_cloudfront" {
  description = "Enable CloudFront CDN (recommended for production)"
  type        = bool
  default     = true
}

# JWT Configuration
variable "jwt_secret" {
  description = "JWT secret key (minimum 256 bits)"
  type        = string
  sensitive   = true
}

variable "jwt_expiration_ms" {
  description = "JWT token expiration in milliseconds"
  type        = number
  default     = 86400000  # 24 hours
}

# Tags
variable "tags" {
  description = "Additional tags for resources"
  type        = map(string)
  default     = {}
}
