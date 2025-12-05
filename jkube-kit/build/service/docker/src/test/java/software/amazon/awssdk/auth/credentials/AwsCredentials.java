/*
 * Mock AWS SDK v2 class for testing purposes only.
 * Simulates software.amazon.awssdk.auth.credentials.AwsCredentials interface.
 */
package software.amazon.awssdk.auth.credentials;

public interface AwsCredentials {
  String accessKeyId();

  String secretAccessKey();
}