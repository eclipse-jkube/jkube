/*
 * Mock AWS SDK v1 class for testing purposes only.
 * Simulates com.amazonaws.auth.AWSSessionCredentials interface.
 */
package com.amazonaws.auth;

public interface AWSSessionCredentials extends AWSCredentials {
  String getSessionToken();
}