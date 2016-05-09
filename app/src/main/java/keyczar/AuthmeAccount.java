package keyczar;

import java.util.Date;

public class AuthmeAccount {
        Date CreatedAt;
        String Identifier;
        String Email;
        Integer Status;

        public Date getCreatedAt() {
            return CreatedAt;
        }

        public void setCreatedAt(Date createdAt) {
            CreatedAt = createdAt;
        }

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

        public Integer getStatus() {
            return Status;
        }

        public void setStatus(Integer status) {
            Status = status;
        }

        @Override
        public String toString() {
            return "AuthmeAccount{" +
                    "CreatedAt=" + CreatedAt +
                    ", Identifier='" + Identifier + '\'' +
                    ", Email='" + Email + '\'' +
                    ", Status=" + Status +
                    '}';
        }
    }