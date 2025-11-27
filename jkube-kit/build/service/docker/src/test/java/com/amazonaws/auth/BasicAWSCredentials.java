/*
 * Mock AWS SDK v1 class for testing purposes only.
 * Simulates com.amazonaws.auth.BasicAWSCredentials class.
 */
package com.amazonaws.auth;

public class BasicAWSCredentials implements AWSCredentials {
  private final String accessKey;
  private final String secretKey;

  public BasicAWSCredentials(String accessKey, String secretKey) {
    this.accessKey = accessKey;
    this.secretKey = secretKey;
  }

  @Override
  public String getAWSAccessKeyId() {
    return accessKey;
  }

  @Override
  public String getAWSSecretKey() {
    return secretKey;
  }
}