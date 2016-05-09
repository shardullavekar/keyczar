package keyczar;

public class KeyPair {
    public String encryption;
    public String signing;

    public String getSigning() {
        return signing;
    }

    public void setSigning(String signing) {
        this.signing = signing;
    }

    public String getEncryption() {
        return encryption;
    }

    public void setEncryption(String encryption) {
        this.encryption = encryption;
    }
}
