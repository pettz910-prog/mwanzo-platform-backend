/**
 * CloudFront CDN Configuration
 *
 * Creates:
 * - CloudFront distribution for video delivery
 * - SSL certificate (optional)
 * - Cache behaviors
 * - Custom domain (optional)
 */

# CloudFront Distribution
resource "aws_cloudfront_distribution" "videos" {
  count = var.enable_cloudfront ? 1 : 0

  enabled             = true
  is_ipv6_enabled     = true
  comment             = "${var.project_name} Videos CDN"
  default_root_object = ""
  price_class         = "PriceClass_100"  # Use only US, Canada, Europe (cheapest)

  # Origin - S3 Bucket
  origin {
    domain_name = aws_s3_bucket.videos.bucket_regional_domain_name
    origin_id   = "S3-${aws_s3_bucket.videos.id}"

    s3_origin_config {
      origin_access_identity = aws_cloudfront_origin_access_identity.videos[0].cloudfront_access_identity_path
    }
  }

  # Default Cache Behavior
  default_cache_behavior {
    target_origin_id       = "S3-${aws_s3_bucket.videos.id}"
    viewer_protocol_policy = "redirect-to-https"
    allowed_methods        = ["GET", "HEAD", "OPTIONS"]
    cached_methods         = ["GET", "HEAD", "OPTIONS"]
    compress               = true

    forwarded_values {
      query_string = false
      headers      = ["Origin", "Access-Control-Request-Headers", "Access-Control-Request-Method"]

      cookies {
        forward = "none"
      }
    }

    min_ttl     = 0
    default_ttl = 86400   # 1 day
    max_ttl     = 31536000 # 1 year
  }

  # Cache Behavior for Videos (longer cache)
  ordered_cache_behavior {
    path_pattern           = "videos/*"
    target_origin_id       = "S3-${aws_s3_bucket.videos.id}"
    viewer_protocol_policy = "redirect-to-https"
    allowed_methods        = ["GET", "HEAD", "OPTIONS"]
    cached_methods         = ["GET", "HEAD", "OPTIONS"]
    compress               = true

    forwarded_values {
      query_string = false

      cookies {
        forward = "none"
      }
    }

    min_ttl     = 0
    default_ttl = 2592000  # 30 days
    max_ttl     = 31536000 # 1 year
  }

  # Cache Behavior for Processed Videos (very long cache)
  ordered_cache_behavior {
    path_pattern           = "processed/*"
    target_origin_id       = "S3-${aws_s3_bucket.videos.id}"
    viewer_protocol_policy = "redirect-to-https"
    allowed_methods        = ["GET", "HEAD", "OPTIONS"]
    cached_methods         = ["GET", "HEAD", "OPTIONS"]
    compress               = true

    forwarded_values {
      query_string = false

      cookies {
        forward = "none"
      }
    }

    min_ttl     = 0
    default_ttl = 2592000  # 30 days
    max_ttl     = 31536000 # 1 year
  }

  # Geo Restrictions (Optional)
  restrictions {
    geo_restriction {
      restriction_type = "none"
      # To restrict to specific countries:
      # restriction_type = "whitelist"
      # locations        = ["KE", "US", "GB"]
    }
  }

  # SSL Certificate
  viewer_certificate {
    cloudfront_default_certificate = var.cdn_domain_name == "" ? true : false
    acm_certificate_arn            = var.cdn_domain_name != "" ? aws_acm_certificate.cdn[0].arn : null
    ssl_support_method             = var.cdn_domain_name != "" ? "sni-only" : null
    minimum_protocol_version       = "TLSv1.2_2021"
  }

  # Custom Domain (Optional)
  aliases = var.cdn_domain_name != "" ? [var.cdn_domain_name] : []

  tags = {
    Name = "${var.project_name}-cdn-${var.environment}"
  }
}

# ACM Certificate for Custom Domain (Must be in us-east-1 for CloudFront)
resource "aws_acm_certificate" "cdn" {
  count = var.enable_cloudfront && var.cdn_domain_name != "" ? 1 : 0

  provider          = aws
  domain_name       = var.cdn_domain_name
  validation_method = "DNS"

  lifecycle {
    create_before_destroy = true
  }

  tags = {
    Name = "${var.project_name}-cdn-cert-${var.environment}"
  }
}

# DNS Validation (if using Route 53)
# resource "aws_route53_record" "cdn_cert_validation" {
#   count = var.enable_cloudfront && var.cdn_domain_name != "" ? 1 : 0
#
#   zone_id = var.route53_zone_id
#   name    = tolist(aws_acm_certificate.cdn[0].domain_validation_options)[0].resource_record_name
#   type    = tolist(aws_acm_certificate.cdn[0].domain_validation_options)[0].resource_record_type
#   records = [tolist(aws_acm_certificate.cdn[0].domain_validation_options)[0].resource_record_value]
#   ttl     = 60
# }

# Outputs
output "cloudfront_domain_name" {
  description = "CloudFront distribution domain name"
  value       = var.enable_cloudfront ? aws_cloudfront_distribution.videos[0].domain_name : ""
}

output "cloudfront_id" {
  description = "CloudFront distribution ID"
  value       = var.enable_cloudfront ? aws_cloudfront_distribution.videos[0].id : ""
}
