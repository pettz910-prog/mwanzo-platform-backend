/**
 * S3 Storage Configuration
 *
 * Creates:
 * - S3 bucket for videos and thumbnails
 * - Bucket policies
 * - CORS configuration
 * - Lifecycle rules for cost optimization
 */

# S3 Bucket for Videos
resource "aws_s3_bucket" "videos" {
  bucket = var.s3_video_bucket_name

  tags = {
    Name = "${var.project_name}-videos-${var.environment}"
  }
}

# Block Public Access (CloudFront will access via OAI)
resource "aws_s3_bucket_public_access_block" "videos" {
  bucket = aws_s3_bucket.videos.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# Enable Versioning (Optional - costs more but allows recovery)
resource "aws_s3_bucket_versioning" "videos" {
  bucket = aws_s3_bucket.videos.id

  versioning_configuration {
    status = "Suspended"  # Change to "Enabled" for version history
  }
}

# Server-Side Encryption
resource "aws_s3_bucket_server_side_encryption_configuration" "videos" {
  bucket = aws_s3_bucket.videos.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
    bucket_key_enabled = true
  }
}

# CORS Configuration
resource "aws_s3_bucket_cors_configuration" "videos" {
  bucket = aws_s3_bucket.videos.id

  cors_rule {
    allowed_headers = ["*"]
    allowed_methods = ["GET", "PUT", "POST", "HEAD"]
    allowed_origins = [
      "https://${var.domain_name}",
      "https://www.${var.domain_name}",
      "http://localhost:5173"  # Development
    ]
    expose_headers  = ["ETag"]
    max_age_seconds = 3000
  }
}

# Lifecycle Rules (Cost Optimization)
resource "aws_s3_bucket_lifecycle_configuration" "videos" {
  bucket = aws_s3_bucket.videos.id

  rule {
    id     = "delete-incomplete-uploads"
    status = "Enabled"

    abort_incomplete_multipart_upload {
      days_after_initiation = 7
    }
  }

  rule {
    id     = "transition-old-versions"
    status = "Enabled"

    noncurrent_version_transition {
      noncurrent_days = 30
      storage_class   = "STANDARD_IA"
    }

    noncurrent_version_transition {
      noncurrent_days = 90
      storage_class   = "GLACIER"
    }

    noncurrent_version_expiration {
      noncurrent_days = 365
    }
  }
}

# CloudFront Origin Access Identity
resource "aws_cloudfront_origin_access_identity" "videos" {
  count = var.enable_cloudfront ? 1 : 0

  comment = "OAI for ${var.project_name} videos bucket"
}

# Bucket Policy - Allow CloudFront OAI
resource "aws_s3_bucket_policy" "videos" {
  bucket = aws_s3_bucket.videos.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "AllowCloudFrontOAI"
        Effect = "Allow"
        Principal = var.enable_cloudfront ? {
          AWS = aws_cloudfront_origin_access_identity.videos[0].iam_arn
        } : {
          Service = "mediaconvert.amazonaws.com"
        }
        Action = [
          "s3:GetObject",
          "s3:PutObject"
        ]
        Resource = "${aws_s3_bucket.videos.arn}/*"
      },
      {
        Sid    = "AllowMediaConvertAccess"
        Effect = "Allow"
        Principal = {
          Service = "mediaconvert.amazonaws.com"
        }
        Action = [
          "s3:GetObject",
          "s3:PutObject",
          "s3:PutObjectAcl"
        ]
        Resource = "${aws_s3_bucket.videos.arn}/*"
      }
    ]
  })
}

# Outputs
output "s3_bucket_name" {
  description = "S3 bucket name for videos"
  value       = aws_s3_bucket.videos.id
}

output "s3_bucket_arn" {
  description = "S3 bucket ARN"
  value       = aws_s3_bucket.videos.arn
}

output "s3_bucket_domain_name" {
  description = "S3 bucket domain name"
  value       = aws_s3_bucket.videos.bucket_regional_domain_name
}
