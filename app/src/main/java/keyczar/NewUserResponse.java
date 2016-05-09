package keyczar;

public class NewUserResponse {
    Integer Status;
    NewUserData Data;

    public Integer getStatus() {
        return Status;
    }

    public void setStatus(Integer status) {
        Status = status;
    }

    public NewUserData getData() {
        return Data;
    }

    public void setData(NewUserData data) {
        Data = data;
    }

    @Override
    public String toString() {
        return "NewUserResponse{" +
                "Status=" + Status +
                ", Data=" + Data +
                '}';
    }
}