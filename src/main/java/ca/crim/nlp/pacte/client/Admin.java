package ca.crim.nlp.pacte.client;

import org.json.JSONObject;

import ca.crim.nlp.pacte.QuickConfig;
import ca.crim.nlp.pacte.Credential;
import ca.crim.nlp.pacte.QuickConfig.USERTYPE;

public class Admin {
    private QuickConfig poCfg = null;

    public Admin(QuickConfig toConfig) {
        poCfg = toConfig;
    }

    public String listAllUsers() {
        String lsReturn = null;

        lsReturn = poCfg.getRequest(poCfg.getAuthenUrl() + "psc-users-permissions-management/Users/users",
                USERTYPE.PSCAdmin, null);

        return lsReturn;
    }

    /**
     * Reset the password of a user
     * 
     * @param tsUsername
     * @param tsOldPassword
     * @param tsNewPassword
     * @return
     */
    public boolean resetPassword(String tsUsername, String tsOldPassword, String tsNewPassword) {
        String lsReturn = null;

        poCfg.setCustomUser(tsUsername, tsOldPassword);

        lsReturn = poCfg.putRequest(poCfg.getAuthenUrl() + "/psc-users-permissions-management/Users/myPassword",
                "{\"password\":\"" + tsNewPassword + "\"}", USERTYPE.CustomUser);

        if (lsReturn != null && lsReturn != "") {
            if (lsReturn.contains("\"id\":")) {
                System.out.println(lsReturn);
            }
        }

        return false;
    }

    /**
     * 
     * @param tsUsername
     * @param tsPassword
     * @param tsPrenom
     * @param tsNom
     * @return
     */
    public Credential createUser(String tsUsername, String tsPassword, String tsPrenom, String tsNom) {
        String lsReturn = "";
        Credential loCred = null;

        // Ajouter un nouvel utilisateur
        lsReturn = poCfg
                .postRequest(poCfg.getPacteBackend() + "PlatformUsers/platformUser",
                        "{\"password\": \"" + tsPassword + "\",\"firstName\":\"" + tsPrenom + "\",\"lastName\":\""
                                + tsNom + "\",\"email\":\"" + tsUsername + "\", \"jwtAudience\": [\"Pacte\"]}",
                        USERTYPE.PacteAdmin);

        if (lsReturn != null && !lsReturn.isEmpty() && lsReturn.toLowerCase().contains("userprofileid")) {
            if (poCfg.getVerbose()) {
                System.out.println("Utilisateur " + tsUsername + " a été créé!");
                System.out.println(lsReturn);
            }
            JSONObject loObj = new JSONObject(lsReturn);
            loCred = new Credential( loObj.getString("userId"), loObj.getString("userProfileId"), tsUsername, tsPassword);
            
        } else if (poCfg.getVerbose()) {
            if (lsReturn.toLowerCase().contains("conflict"))
                System.err.println("Utilisateur " + tsUsername + " existant! (possiblement avec d'autres accès)");

            else if (lsReturn.toLowerCase().contains("Unauthorized"))
                System.out.println("Accès administrateur invalides!");
        }

        return loCred;
    }

    /**
     * Delete the configured custom account
     * 
     * @param tsUserID
     * @return
     */
    public boolean deleteUser(String tsUserId) {
        String lsUsername = null;
        
        // Delete the user
        poCfg.deleteRequest(poCfg.getPSCUserBackend() + "Users/user/" + tsUserId, USERTYPE.PSCAdmin, null);

        lsUsername = poCfg.getRequest(poCfg.getPSCUserBackend() + "Users/user/" + tsUserId, USERTYPE.PSCAdmin, null);
        if (new JSONObject(lsUsername).has("username"))
            lsUsername = new JSONObject(lsUsername).getString("username");
        else if (lsUsername.toLowerCase().indexOf("not found") >= 0)
            return true;

        return (lsUsername == null);
    }

    /**
     * Verify if a user exists
     * 
     * @param tsUsername
     * @param tsPassword
     * @return Unique ID of user, Null is non-existant
     */
    public String checkUser(String tsUsername, String tsPassword) {
        String lsReturn = "";

        // Se logger et obtenir un token
        poCfg.setCustomUser(tsUsername, tsPassword);

        lsReturn = poCfg.getRequest(poCfg.getPacteBackend() + "PlatformUsers/myPlatformUserContacts",
                USERTYPE.CustomUser, null);

        if (lsReturn != null && !lsReturn.isEmpty() && !lsReturn.contains("Forbidden")
                && !lsReturn.contains("Unauthorized")) {
            JSONObject loJson = new JSONObject(lsReturn);
            if (poCfg.getVerbose())
                System.out.println("Utilisateur " + loJson.getJSONObject("user").getString("userProfileId") + " existant.");
            return loJson.getJSONObject("user").getString("userProfileId");
        } else
            return null;

    }

    /**
     * 
     * @param tsUserID1
     * @param tsUserID2
     * @return
     */
    public boolean addContact(String tsUserID) {
        String lsReturn = null;

        lsReturn = poCfg.postRequest(poCfg.getPacteBackend() + "PlatformUsers/myPlatformUserContact",
                "{\"contactUserProfileId\": \"" + tsUserID + "\"}", USERTYPE.CustomUser);

        if (lsReturn.contains("{\"contactStatus\":\""))
            return true;

        return false;
    }

    public boolean removeContact(String tsUserID) {
        String lsReturn = null;

        if ((tsUserID == null) || tsUserID.isEmpty())
            return false;

        lsReturn = poCfg.deleteRequest(poCfg.getPacteBackend() + "PlatformUsers/myPlatformUserContact/" + tsUserID,
                USERTYPE.CustomUser, null);

        if (!lsReturn.contains("\"Unauthorized\""))
            return true;

        return false;
    }
}
