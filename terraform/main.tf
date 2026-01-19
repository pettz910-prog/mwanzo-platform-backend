/**
 * Mwanzo Course Platform - Terraform Infrastructure
 *
 * Complete AWS infrastructure for production deployment.
 * Cost-optimized for small to medium scale (~$130-150/month).
 *
 * Author: Mwanzo Development Team
 * Version: 1.0
 */

terraform {
  required_version = ">= 1.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  # Store Terraform state in S3 (recommended for production)
  # Uncomment after creating the bucket manually
  # backend "s3" {
  #   bucket         = "mwanzo-terraform-state"
  #   key            = "production/terraform.tfstate"
  #   region         = "us-east-1"
  #   encrypt        = true
  #   dynamodb_table = "mwanzo-terraform-locks"
  # }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = "Mwanzo-Course-Platform"
      Environment = var.environment
      ManagedBy   = "Terraform"
    }
  }
}

# Data sources
data "aws_caller_identity" "current" {}
data "aws_availability_zones" "available" {
  state = "available"
}
