package co.cellano.edufeed.backend.dto;

public class BiometricAuthRequest {
    private String userId;
    private String token;
    private String method; // "fingerprint", "faceid", "voice"
    private String data; // Datos biométricos en base64
    private boolean secondAttempt; // true si es segundo intento

    public BiometricAuthRequest() {
    }

    public BiometricAuthRequest(String userId, String token, String method, String data, boolean secondAttempt) {
        this.userId = userId;
        this.token = token;
        this.method = method;
        this.data = data;
        this.secondAttempt = secondAttempt;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public boolean isSecondAttempt() {
        return secondAttempt;
    }

    public void setSecondAttempt(boolean secondAttempt) {
        this.secondAttempt = secondAttempt;
    }
}
