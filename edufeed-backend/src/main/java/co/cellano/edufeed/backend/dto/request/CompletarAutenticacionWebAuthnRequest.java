package co.cellano.edufeed.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Request para completar autenticación WebAuthn.
 */
public class CompletarAutenticacionWebAuthnRequest {
    @NotBlank(message = "El credentialId es obligatorio")
    private String credentialId;

    @NotBlank(message = "La signature es obligatoria")
    private String signature;

    private String authenticatorData; // Base64
    private String clientDataJSON; // Base64

    public String getCredentialId() {
        return credentialId;
    }

    public void setCredentialId(String credentialId) {
        this.credentialId = credentialId;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getAuthenticatorData() {
        return authenticatorData;
    }

    public void setAuthenticatorData(String authenticatorData) {
        this.authenticatorData = authenticatorData;
    }

    public String getClientDataJSON() {
        return clientDataJSON;
    }

    public void setClientDataJSON(String clientDataJSON) {
        this.clientDataJSON = clientDataJSON;
    }
}
