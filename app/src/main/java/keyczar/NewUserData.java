package keyczar;

public class NewUserData {
    AuthmeAccount account;
    String Key;
    String XmppPassword;
    String UnsignedLoginToken;
    Integer PrivateGroupId;
    Boolean Verified;

    public AuthmeAccount getAccount() {
        return account;
    }

    public void setAccount(AuthmeAccount account) {
        this.account = account;
    }

    public String getKey() {
        return Key;
    }

    public void setKey(String key) {
        Key = key;
    }

    public String getXmppPassword() {
        return XmppPassword;
    }

    public void setXmppPassword(String xmppPassword) {
        XmppPassword = xmppPassword;
    }

    public String getUnsignedLoginToken() {
        return UnsignedLoginToken;
    }

    public void setUnsignedLoginToken(String unsignedLoginToken) {
        UnsignedLoginToken = unsignedLoginToken;
    }

    public Integer getPrivateGroupId() {
        return PrivateGroupId;
    }

    public void setPrivateGroupId(Integer privateGroupId) {
        PrivateGroupId = privateGroupId;
    }

    public Boolean getVerified() {
        return Verified;
    }

    public void setVerified(Boolean verified) {
        Verified = verified;
    }

    @Override
    public String toString() {
        return "NewUserData{" +
                "account=" + account +
                ", Key='" + Key + '\'' +
                ", XmppPassword='" + XmppPassword + '\'' +
                ", UnsignedLoginToken='" + UnsignedLoginToken + '\'' +
                ", PrivateGroupId=" + PrivateGroupId +
                ", Verified=" + Verified +
                '}';
    }
}