package ca.crim.nlp.pacte;

import java.util.Date;

public class Credential {
    private String psUsername = null;
    private String psPassword = null;
    private String psPrenom = null;
    private String psNom = null;
    private String psToken = null;
    private String psUserId = null;
    private String psUserProfileId = null;
    private Date pdTokenCreation = null;

    public Credential(String tsUsername, String tsPassword, int tniRenewHour) {
        psUsername = tsUsername;
        psPassword = tsPassword;
    }

    public Credential(String tsUserId, String tsUsername, String tsPassword, String tsPrenom, String tsNom) {
        psUsername = tsUsername;
        psPassword = tsPassword;
        psUserId = tsUserId;
        psNom = tsNom;
        psPrenom = tsPrenom;
    }

    public Credential(String tsUserId, String tsUserProfileId, String tsUsername, String tsPassword) {
        psUsername = tsUsername;
        psPassword = tsPassword;
        psUserId = tsUserId;
        psUserProfileId = tsUserProfileId;
    }

    public String getUsername() {
        return psUsername;
    }

    public String getName() {
        return psPrenom;
    }

    public String getSurname() {
        return psNom;
    }

    public String getPassword() {
        return psPassword;
    }

    public String getUserId() {
        return psUserId;
    }

    public String getUserProfileId() {
        return psUserProfileId;
    }

    public String getToken() {
        return psToken;
    }

    public Date getTokenCreation() {
        return pdTokenCreation;
    }

    public void setToken(String tsNewToken) {
        psToken = ((tsNewToken == null) || (tsNewToken.isEmpty())) ? null : tsNewToken;
        pdTokenCreation = new Date();
    }

}
