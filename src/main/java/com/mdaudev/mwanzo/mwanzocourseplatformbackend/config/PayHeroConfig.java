package com.mdaudev.mwanzo.mwanzocourseplatformbackend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * PayHero Configuration
 *
 * Loads PayHero M-Pesa integration credentials from application.yml.
 *
 * Configuration Properties:
 * - payhero.base-url: PayHero API endpoint
 * - payhero.api-username: PayHero API username
 * - payhero.api-password: PayHero API password
 * - payhero.channel-id: PayHero channel ID
 * - payhero.callback-url: Webhook URL for payment callbacks
 *
 * @author Mwanzo Development Team
 * @version 1.0
 * @since 2026-01-09
 */
@Configuration
@ConfigurationProperties(prefix = "payhero")
@Data
public class PayHeroConfig {

    /**
     * PayHero API base URL.
     * Production: https://backend.payhero.co.ke/api/v2/payments
     */
    private String baseUrl;

    /**
     * PayHero API username.
     * Set via environment variable: PAYHERO_USERNAME
     */
    private String apiUsername;

    /**
     * PayHero API password.
     * Set via environment variable: PAYHERO_PASSWORD
     */
    private String apiPassword;

    /**
     * PayHero channel ID.
     * Set via environment variable: PAYHERO_CHANNEL_ID
     */
    private String channelId;

    /**
     * Callback URL for PayHero webhooks.
     * Example: https://yourdomain.com/api/v1/payments/callback
     */
    private String callbackUrl;
}