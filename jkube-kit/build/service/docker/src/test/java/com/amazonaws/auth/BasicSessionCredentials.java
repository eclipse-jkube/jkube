/*
 * Mock AWS SDK v1 class for testing purposes only.
 * Simulates com.amazonaws.auth.BasicSessionCredentials class.
 */
package com.amazonaws.auth;

public class BasicSessionCredentials implements AWSSessionCredentials {
  private final String accessKey;
  private final String secretKey;
  private final String sessionToken;

  public BasicSessionCredentials(String accessKey, String secretKey, String sessionToken) {
    this.accessKey = accessKey;
    this.secretKey = secretKey;
    this.sessionToken = sessionToken;
  }

  @Override
  public String getAWSAccessKeyId() {
    return accessKey;
  }

  @Override
  public String getAWSSecretKey() {
    return secretKey;
  }

  @Override
  public String getSessionToken() {
    return sessionToken;
  }
}