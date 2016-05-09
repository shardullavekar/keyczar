package keyczar;

public class Request {
    public String analyticsId;
    String deviceId;
    public String encryptedPrivateKey;
    public String groupKeyEncryptedForMe;
    public String groupPublicKey;
    public String publicKey;
    public String userId;

    public String getAnalyticsId() {
        return analyticsId;
    }

    public void setAnalyticsId(String analyticsId) {
        this.analyticsId = analyticsId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getEncryptedPrivateKey() {
        return encryptedPrivateKey;
    }

    public void setEncryptedPrivateKey(String encryptedPrivateKey) {
        this.encryptedPrivateKey = encryptedPrivateKey;
    }

    public String getGroupKeyEncryptedForMe() {
        return groupKeyEncryptedForMe;
    }

    public void setGroupKeyEncryptedForMe(String groupKeyEncryptedForMe) {
        this.groupKeyEncryptedForMe = groupKeyEncryptedForMe;
    }

    public String getGroupPublicKey() {
        return groupPublicKey;
    }

    public void setGroupPublicKey(String groupPublicKey) {
        this.groupPublicKey = groupPublicKey;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}