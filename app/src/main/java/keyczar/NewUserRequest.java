package keyczar;

public class NewUserRequest {
    public String Identifier;
    public String Email;
    public String Phone;
    public String Name;
    public String XmppResource;
    public String MitroSignedRequest;

    public String getIdentifier() {
        return Identifier;
    }

    public void setIdentifier(String identifier) {
        Identifier = identifier;
    }

    public String getEmail() {
        return Email;
    }

    public void setEmail(String email) {
        Email = email;
    }

    public String getPhone() {
        return Phone;
    }

    public void setPhone(String phone) {
        Phone = phone;
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    public String getMitroSignedRequest() {
        return MitroSignedRequest;
    }

    public void setMitroSignedRequest(String mitroSignedRequest) {
        MitroSignedRequest = mitroSignedRequest;
    }
}