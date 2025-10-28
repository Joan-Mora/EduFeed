package co.cellano.edufeed.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Request para completar registro WebAuthn.
 */
public class CompletarRegistroWebAuthnRequest {
    @NotBlank(message = "El credentialId es obligatorio")
    private String credentialId;

    @NotBlank(message = "La publicKey es obligatoria")
    private String publicKey;

    private String attestationObject; // Base64
    private String clientDataJSON; // Base64

    public String getCredentialId() {
        return credentialId;
    }

    public void setCredentialId(String credentialId) {
        this.credentialId = credentialId;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getAttestationObject() {
        return attestationObject;
    }

    public void setAttestationObject(String attestationObject) {
        this.attestationObject = attestationObject;
    }

    public String getClientDataJSON() {
        return clientDataJSON;
    }

    public void setClientDataJSON(String clientDataJSON) {
        this.clientDataJSON = clientDataJSON;
    }
}
