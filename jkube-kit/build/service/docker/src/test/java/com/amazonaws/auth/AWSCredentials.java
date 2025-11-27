/*
 * Mock AWS SDK v1 class for testing purposes only.
 * Simulates com.amazonaws.auth.AWSCredentials interface.
 */
package com.amazonaws.auth;

public interface AWSCredentials {
  String getAWSAccessKeyId();
  String getAWSSecretKey();
}