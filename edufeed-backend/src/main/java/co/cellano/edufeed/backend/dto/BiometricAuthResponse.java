package co.cellano.edufeed.backend.dto;

import java.util.Map;

public class BiometricAuthResponse {
    private boolean success;
    private String message;
    private Map<String, Object> userData;
    private boolean requiresSecondFactor;

    public BiometricAuthResponse() {
    }

    public BiometricAuthResponse(boolean success, String message, Map<String, Object> userData) {
        this.success = success;
        this.message = message;
        this.userData = userData;
        this.requiresSecondFactor = false;
    }

    public BiometricAuthResponse(boolean success, String message, Map<String, Object> userData,
            boolean requiresSecondFactor) {
        this.success = success;
        this.message = message;
        this.userData = userData;
        this.requiresSecondFactor = requiresSecondFactor;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, Object> getUserData() {
        return userData;
    }

    public void setUserData(Map<String, Object> userData) {
        this.userData = userData;
    }

    public boolean isRequiresSecondFactor() {
        return requiresSecondFactor;
    }

    public void setRequiresSecondFactor(boolean requiresSecondFactor) {
        this.requiresSecondFactor = requiresSecondFactor;
    }
}
