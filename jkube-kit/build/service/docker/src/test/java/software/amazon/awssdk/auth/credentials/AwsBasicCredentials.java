/*
 * Mock AWS SDK v2 class for testing purposes only.
 * Simulates AWS SDK v2 basic credentials.
 */
package software.amazon.awssdk.auth.credentials;

public class AwsBasicCredentials implements AwsCredentials {
  private final String accessKeyId;
  private final String secretAccessKey;

  public AwsBasicCredentials(String accessKeyId, String secretAccessKey) {
    this.accessKeyId = accessKeyId;
    this.secretAccessKey = secretAccessKey;
  }

  @Override
  public String accessKeyId() {
    return accessKeyId;
  }

  @Override
  public String secretAccessKey() {
    return secretAccessKey;
  }

  public static AwsBasicCredentials create(String accessKeyId, String secretAccessKey) {
    return new AwsBasicCredentials(accessKeyId, secretAccessKey);
  }
}