package ca.crim.nlp.pacte;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONObject;

public class QuickConfig {
    String psBaseURLAuthen = "";
    String psBaseURLPacteBE = "";
    String psBaseURLPSCUser = "";
    String psBaseURLService = "";
    Integer pniTokenRenewDelay = -12;

    private boolean pbVerbose = true;

    public enum USERTYPE {
        PSCAdmin, PacteAdmin, CustomUser
    };

    Map<USERTYPE, Credential> poCred = new HashMap<USERTYPE, Credential>();

    // Creating an instance of HttpClient.
    CloseableHttpClient httpclient = HttpClients.createDefault();

    public QuickConfig() {
        String[] lasConfig = readConfiguration();
        setConfig(lasConfig[0], lasConfig[1], lasConfig[2], lasConfig[3], lasConfig[4], lasConfig[5], lasConfig[6],
                Boolean.parseBoolean(lasConfig[7]), Integer.parseInt(lasConfig[8]), lasConfig[9]);
    }

    private void setConfig(String tsBasePacteUrl, String tsAdminPSCUsername, String tsAdminPSCPassword,
            String tsAdminPacteUsername, String tsAdminPactePassword, String tsCustomUser, String tsCustomPassword,
            boolean tbVerbose, int tniTokenRenewDelay, String tsServiceUrl) {
        psBaseURLAuthen = tsBasePacteUrl.endsWith("/") ? tsBasePacteUrl : tsBasePacteUrl + "/";
        psBaseURLPacteBE = psBaseURLAuthen + "pacte-backend/";
        psBaseURLPSCUser = psBaseURLAuthen + "psc-users-permissions-management/";
        psBaseURLService = tsServiceUrl.endsWith("/") ? tsServiceUrl : tsServiceUrl + "/";
        pniTokenRenewDelay = tniTokenRenewDelay;
        pbVerbose = tbVerbose;

        // PSC admin
        if (tsAdminPSCUsername != null && tsAdminPSCPassword != null)
            poCred.put(USERTYPE.PSCAdmin, new Credential(tsAdminPSCUsername, tsAdminPSCPassword, pniTokenRenewDelay));

        // Pacte admin
        if (tsAdminPacteUsername != null && tsAdminPactePassword != null)
            poCred.put(USERTYPE.PacteAdmin,
                    new Credential(tsAdminPacteUsername, tsAdminPactePassword, pniTokenRenewDelay));

        // Pacte custom user
        if (tsCustomUser != null && tsCustomPassword != null)
            poCred.put(USERTYPE.CustomUser, new Credential(tsCustomUser, tsCustomPassword, pniTokenRenewDelay));
    }

    /**
     * New configuration with admin and user level credentials
     * 
     * @param tsBasePacteUrl
     * @param tsAdminPSCUsername
     * @param tsAdminPSCPassword
     * @param tsAdminPacteUsername
     * @param tsAdminPactePassword
     * @param tsCustomUser
     * @param tsCustomPassword
     * @param tbVerbose
     */
    public QuickConfig(String tsBasePacteUrl, String tsAdminPSCUsername, String tsAdminPSCPassword,
            String tsAdminPacteUsername, String tsAdminPactePassword, String tsCustomUser, String tsCustomPassword,
            boolean tbVerbose, int tniTokenRenewDelay, String tsServiceUrl) {
        if (tsBasePacteUrl == null || tsBasePacteUrl.isEmpty())
            throw new IllegalArgumentException("PACTE url should not be null");

        setConfig(tsBasePacteUrl, tsAdminPSCUsername, tsAdminPSCPassword, tsAdminPacteUsername, tsAdminPactePassword,
                tsCustomUser, tsCustomPassword, tbVerbose, tniTokenRenewDelay, tsServiceUrl);
    }

    /**
     * New configuration with user level credentials
     * 
     * @param tsBasePacteUrl
     *            Mandatory url to acessible PACTE platform.
     * @param tsCustomUser
     *            Mandatory username to access the platform.
     * @param tsCustomPassword
     *            User's password.
     * @param tbVerbose
     *            True if you want detailed processing messages.
     */
    public QuickConfig(String tsBasePacteUrl, String tsCustomUser, String tsCustomPassword, boolean tbVerbose,
            int tniTokenRenewDelay) {
        if (tsBasePacteUrl == null || tsBasePacteUrl.isEmpty())
            throw new IllegalArgumentException("PACTE url should not be null");

        if (tsCustomUser == null || tsCustomUser.isEmpty())
            throw new IllegalArgumentException("Username should not be null");

        String[] lasConfig = readConfiguration();

        setConfig(tsBasePacteUrl, null, null, null, null, tsCustomUser, tsCustomPassword, tbVerbose, tniTokenRenewDelay,
                lasConfig[9]);
    }

    public void setCustomUser(String tsUsername, String tsPassword) {
        poCred.put(USERTYPE.CustomUser, new Credential(tsUsername, tsPassword, pniTokenRenewDelay));
    }

    public Credential getUserCredential(USERTYPE toType) {
        if (poCred.keySet().contains(toType))
            return poCred.get(toType);
        else
            return null;
    }

    /**
     * Set a different route for credentials (for unusual backend configuration)
     * 
     * @param tsUrl
     *            : Complete url to the authentication backend for tokens
     */
    public void setAuthenUrl(String tsUrl) {
        psBaseURLAuthen = tsUrl.endsWith("/") ? tsUrl : tsUrl + "/";
    }

    public void setServiceUrl(String tsServiceUrl) {
        psBaseURLService = tsServiceUrl.endsWith("/") ? tsServiceUrl : tsServiceUrl + "/";
    }

    public String getServiceUrl() {
        return psBaseURLService;
    }

    public String getAuthenUrl() {
        return psBaseURLAuthen;
    }

    public String getPacteBackend() {
        return psBaseURLPacteBE;
    }

    public String getPSCUserBackend() {
        return psBaseURLPSCUser;
    }

    /**
     * Call a GET request with preconfigured user credentials.
     * 
     * @param tsTargetEndpoint
     * @param toUsertype
     * @param toParams
     * @return
     */
    public String getRequest(String tsTargetEndpoint, USERTYPE toUsertype, List<NameValuePair> toParams) {
        String lsReturn = "";
        URIBuilder loUriBuilder = null;

        try {
            loUriBuilder = new URIBuilder(tsTargetEndpoint);
        } catch (URISyntaxException e1) {
            if (pbVerbose)
                e1.printStackTrace();
            return null;
        }

        if (toParams != null)
            loUriBuilder.addParameters(toParams);

        HttpGet loGet = new HttpGet(loUriBuilder.toString());

        if (toUsertype != null) {
            loGet.addHeader("Authorization", "Bearer " + getToken(poCred.get(toUsertype)));
            loGet.addHeader("AuthorizationAudience", "Pacte");
        }

        try {
            CloseableHttpResponse response = httpclient.execute(loGet);
            lsReturn = readInput(response.getEntity().getContent());

            if (pbVerbose || ((response.getStatusLine().getStatusCode() != 200)
                    && (response.getStatusLine().getStatusCode() != 204)))
                System.out.println("Response Status line :" + response.getStatusLine());

            response.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return lsReturn;
    }

    /**
     * Call a DELETE request with preconfigured user credentials.
     * 
     * @param tsTargetEndpoint
     * @param tsToken
     * @param toParams
     * @return
     */
    public String deleteRequest(String tsTargetEndpoint, USERTYPE toUsertype, List<NameValuePair> toParams) {
        String lsReturn = "";
        URIBuilder loUriBuilder = null;

        try {
            loUriBuilder = new URIBuilder(tsTargetEndpoint);
        } catch (URISyntaxException e1) {
            e1.printStackTrace();
        }

        if (toParams != null)
            loUriBuilder.addParameters(toParams);

        HttpDelete loDel = new HttpDelete(tsTargetEndpoint);

        if (toUsertype != null) {
            loDel.addHeader("Authorization", "Bearer " + getToken(poCred.get(toUsertype)));
            loDel.addHeader("AuthorizationAudience", "Pacte");
        }

        try {
            CloseableHttpResponse response = httpclient.execute(loDel);
            if (response.getEntity() != null)
                lsReturn = readInput(response.getEntity().getContent());

            if (pbVerbose || ((response.getStatusLine().getStatusCode() != 200)
                    && (response.getStatusLine().getStatusCode() != 204)))
                System.out.println("Response Status line :" + response.getStatusLine());

            response.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return lsReturn;
    }

    /**
     * Call a POST request with preconfigured user credentials.
     * 
     * @param tsTargetEndpoint
     * @param tsJson2Post
     * @param toUsertype
     * @return
     */
    public String postRequest(String tsTargetEndpoint, String tsJson2Post, USERTYPE toUsertype) {
        HttpPost httpost = new HttpPost(tsTargetEndpoint);
        // EntityBuilder loBuilder = EntityBuilder.create();
        // HttpEntity entity = null;
        String lsResponse = "";

        // Ajouter le json
        if (tsJson2Post != null) {
            StringEntity postingString;
			postingString = new StringEntity(tsJson2Post, "UTF-8");
            httpost.setEntity(postingString);
            httpost.setHeader("Content-type", "application/json");
            httpost.setHeader("Accept", "application/json");
        }

        if (toUsertype != null) {
            httpost.setHeader("Authorization", "Bearer " + getToken(poCred.get(toUsertype)));
            httpost.setHeader("AuthorizationAudience", "Pacte");
        }

        // Executing the request.
        try {
            CloseableHttpResponse response = httpclient.execute(httpost);
            if (response.getEntity() != null)
                lsResponse = readInput(response.getEntity().getContent());

            if (pbVerbose || ((response.getStatusLine().getStatusCode() != 200)
                    && (response.getStatusLine().getStatusCode() != 204)))
                System.out.println("Response Status line :" + response.getStatusLine());

            response.close();

        } catch (ClientProtocolException e) {
            e.printStackTrace();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return lsResponse;
    }

    /**
     * Call a PUT request with preconfigured user credentials.
     * 
     * @param tsTargetEndpoint
     * @param tsJson2Post
     * @param toUsertype
     * @return
     */
    public String putRequest(String tsTargetEndpoint, String tsJson2Post, USERTYPE toUsertype) {
        HttpPut httput = new HttpPut(tsTargetEndpoint);

        String lsResponse = "";

        // Ajouter le json
        if (tsJson2Post != null) {
            StringEntity postingString;
			postingString = new StringEntity(tsJson2Post, "UTF-8");
            httput.setEntity(postingString);
            httput.setHeader("Content-type", "application/json");
            httput.setHeader("Accept", "application/json");
        }

        if (toUsertype != null) {
            httput.setHeader("Authorization", "Bearer " + getToken(poCred.get(toUsertype)));
            httput.setHeader("AuthorizationAudience", "Pacte");
        }

        // Executing the request.
        try {
            CloseableHttpResponse response = httpclient.execute(httput);
            if (response.getEntity() != null)
                lsResponse = readInput(response.getEntity().getContent());

            if (pbVerbose || ((response.getStatusLine().getStatusCode() != 200)
                    && (response.getStatusLine().getStatusCode() != 204)))
                System.out.println("Response Status line :" + response.getStatusLine());

            response.close();

        } catch (ClientProtocolException e) {
            e.printStackTrace();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return lsResponse;
    }

    public void setVerbose(boolean tbVerbose) {
        pbVerbose = tbVerbose;
    }

    public boolean getVerbose() {
        return pbVerbose;
    }

    /**
     * Get the authentication token, renewing it after the delay.
     * 
     * @return User's token
     */
    public String getToken(Credential toUserCredentials) {
        String lsReturn = null;
        Calendar ldElapsed = Calendar.getInstance();
        ldElapsed.add(Calendar.HOUR, pniTokenRenewDelay);

        if (toUserCredentials.getToken() == null || toUserCredentials.getTokenCreation().before(ldElapsed.getTime())) {
            toUserCredentials.setToken(null);
            lsReturn = postRequest(psBaseURLAuthen + "psc-authentication-service/FormLogin/login",
                    "{\"username\": \"" + toUserCredentials.getUsername() + "\",\"password\": \""
                            + toUserCredentials.getPassword() + "\",\"jwtAudience\": [\"Pacte\"]}",
                    null);

            if (lsReturn != null && !lsReturn.isEmpty() && !lsReturn.toLowerCase().contains("unauthorized"))
                toUserCredentials.setToken(new JSONObject(lsReturn).getString("token"));
        }

        return toUserCredentials.getToken();
    }

    /**
     * Read the input stream from http socket
     * 
     * @param in
     * @return
     */
    private String readInput(InputStream in) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder result = new StringBuilder();
        String line = "";

        try {
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result.toString();
    }

    /**
     * 
     */
    private String[] readConfiguration() {
        Properties prop = new Properties();
        InputStream input = null;
        String[] lasConfig = new String[10];

        try {
            input = ClassLoader.class.getResourceAsStream("/ca/crim/nlp/pacte/config.properties");

            // load a properties file
            prop.load(input);

            // get the property value and print it out
            lasConfig[0] = prop.getProperty("server");
            lasConfig[1] = prop.getProperty("PSCAdmin");
            lasConfig[2] = prop.getProperty("PSCAdminPwd");
            lasConfig[3] = prop.getProperty("PACTEAdmin");
            lasConfig[4] = prop.getProperty("PACTEAdminPwd");
            lasConfig[5] = prop.getProperty("StandardUser");
            lasConfig[6] = prop.getProperty("StandardUserPwd");
            lasConfig[7] = prop.getProperty("Verbose");
            lasConfig[8] = prop.getProperty("TokenRenewDelay");
            lasConfig[9] = prop.getProperty("ServiceUrl");

        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return lasConfig;
    }

}
