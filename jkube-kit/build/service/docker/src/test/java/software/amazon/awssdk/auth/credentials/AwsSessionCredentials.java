/*
 * Mock AWS SDK v2 class for testing purposes only.
 * Simulates software.amazon.awssdk.auth.credentials.AwsSessionCredentials interface.
 */
package software.amazon.awssdk.auth.credentials;

public interface AwsSessionCredentials extends AwsCredentials {
  String sessionToken();
}