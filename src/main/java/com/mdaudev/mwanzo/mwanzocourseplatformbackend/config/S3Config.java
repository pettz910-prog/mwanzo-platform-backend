package com.mdaudev.mwanzo.mwanzocourseplatformbackend.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * S3 Configuration
 *
 * Configures AWS S3 client for LocalStack (development) or AWS S3 (production).
 *
 * @author Mwanzo Development Team
 * @version 1.0
 * @since 2026-01-12
 */
@Configuration
@ConfigurationProperties(prefix = "aws.s3")
@Data
public class S3Config {

    private String endpoint;
    private String region;
    private String bucket;
    private String accessKey;
    private String secretKey;
    private boolean useLocalstack;

    /**
     * Create AmazonS3 client bean.
     * Uses LocalStack in development, AWS S3 in production.
     */
    @Bean
    public AmazonS3 amazonS3() {
        BasicAWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);

        AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials));

        if (useLocalstack) {
            // LocalStack configuration
            builder.withEndpointConfiguration(
                    new AwsClientBuilder.EndpointConfiguration(endpoint, region)
            ).withPathStyleAccessEnabled(true);
        } else {
            // AWS S3 configuration
            builder.withRegion(region);
        }

        return builder.build();
    }
}