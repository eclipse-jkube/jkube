/*
 * Mock AWS SDK v2 class for testing purposes only.
 * Simulates AWS SDK v2 session credentials.
 */
package software.amazon.awssdk.auth.credentials;

public class AwsSessionCredentialsImpl implements AwsSessionCredentials {
  private final String accessKeyId;
  private final String secretAccessKey;
  private final String sessionToken;

  public AwsSessionCredentialsImpl(String accessKeyId, String secretAccessKey, String sessionToken) {
    this.accessKeyId = accessKeyId;
    this.secretAccessKey = secretAccessKey;
    this.sessionToken = sessionToken;
  }

  @Override
  public String accessKeyId() {
    return accessKeyId;
  }

  @Override
  public String secretAccessKey() {
    return secretAccessKey;
  }

  @Override
  public String sessionToken() {
    return sessionToken;
  }

  public static AwsSessionCredentialsImpl create(String accessKeyId, String secretAccessKey, String sessionToken) {
    return new AwsSessionCredentialsImpl(accessKeyId, secretAccessKey, sessionToken);
  }
}