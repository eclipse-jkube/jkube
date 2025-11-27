/*
 * Mock AWS SDK v1 class for testing purposes only.
 * Simulates com.amazonaws.auth.DefaultAWSCredentialsProviderChain class.
 */
package com.amazonaws.auth;

/**
 * Mock implementation of AWS SDK v1 DefaultAWSCredentialsProviderChain.
 * This is used for testing the reflection-based credential loading.
 */
public class DefaultAWSCredentialsProviderChain {
  private final AWSCredentials credentials;

  public DefaultAWSCredentialsProviderChain() {
    // Return test credentials based on environment variables
    String accessKey = System.getenv("AWS_ACCESS_KEY_ID");
    String secretKey = System.getenv("AWS_SECRET_ACCESS_KEY");
    String sessionToken = System.getenv("AWS_SESSION_TOKEN");

    if (accessKey != null && secretKey != null) {
      if (sessionToken != null) {
        this.credentials = new BasicSessionCredentials(accessKey, secretKey, sessionToken);
      } else {
        this.credentials = new BasicAWSCredentials(accessKey, secretKey);
      }
    } else {
      this.credentials = null;
    }
  }

  public AWSCredentials getCredentials() {
    if (credentials == null) {
      throw new RuntimeException("Unable to load AWS credentials");
    }
    return credentials;
  }
}