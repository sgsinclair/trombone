package ca.crim.nlp.pacte.client;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import ca.crim.nlp.pacte.QuickConfig;
import ca.crim.nlp.pacte.QuickConfig.USERTYPE;

public class Project {
    private QuickConfig poCfg = null;

    public Project(QuickConfig toConfig) {
        poCfg = toConfig;
    }

    /**
     * Get the unique identifier for all your documents of the specified step.
     * 
     * @param tsStepId
     * @param tniFrom
     *            : Starting index
     * @param tniTo
     *            : End index
     * @return
     */
    public List<String> getStepDocumentIds(String tsStepId, int tniFrom, int tniTo) {
        List<String> loDocs = new ArrayList<String>();
        String lsReturn = "";

        for (int lniCpt = tniFrom; lniCpt <= tniTo; lniCpt++) {

            lsReturn = poCfg.getRequest(poCfg.getPacteBackend()
                    + "ProjectStepDocumentDistribution/myProjectStepDocumentByIndex/" + tsStepId + "/" + lniCpt,
                    USERTYPE.CustomUser, null);

            if (lsReturn != null && !lsReturn.isEmpty())
                loDocs.add(new JSONObject(lsReturn).getString("id"));

        }

        return loDocs;
    }

    /**
     * Retrieve a participant id from a step
     * 
     * @param tsStepId
     * @param tsFirstName
     * @param tsLastName
     * @return
     */
    public String getStepParticipantId(String tsStepId, String tsFirstName, String tsLastName) {
        String lsReturn = "";

        lsReturn = poCfg.getRequest(poCfg.getPacteBackend() + "/ProjectSteps/projectStep/" + tsStepId,
                USERTYPE.CustomUser, null);

        if (lsReturn != null && !lsReturn.isEmpty()) {
            JSONArray loParts = new JSONObject(lsReturn).getJSONArray("participants");

            for (int lniCpt = 0; lniCpt < loParts.length(); lniCpt++) {
                JSONObject loP = loParts.getJSONObject(lniCpt);
                if (loP.getString("firstname").equalsIgnoreCase(tsFirstName)
                        && loP.getString("lastname").equalsIgnoreCase(tsLastName))
                    return loP.getString("id");
            }
        }

        return null;
    }

    /**
     * Retrieve a project unique identifier by name.
     * 
     * @param tsProjectName
     * @return
     */
    public String getProjectId(String tsProjectName) {
        String lsId = null;
        String lsReturn = null;

        lsReturn = poCfg.getRequest(poCfg.getPacteBackend() + "/Projects/projects", USERTYPE.CustomUser, null);

        if (lsReturn != null && !lsReturn.isEmpty()) {
            int lniPos = lsReturn.toLowerCase().indexOf("\"title\":\"" + tsProjectName.toLowerCase() + "\"");
            if (lniPos >= 0) {
                lniPos = lsReturn.substring(0, lniPos).lastIndexOf("\"id\":\"") + 6;
                lsId = lsReturn.substring(lniPos, lniPos + 36);
                // System.out.println("Project " + tsProjectName + " (" + lsId +
                // ") a été trouvé!");
                return lsId;
            }
        }

        return null;
    }

    /**
     * Retrieve a step unique identifier by name from a project.
     * 
     * @param tsProjectName
     * @return
     */
    public String getStepId(String tsProjectId, String lsStepName) {
        String lsId = null;
        String lsReturn = null;

        lsReturn = poCfg.getRequest(poCfg.getPacteBackend() + "ProjectSteps/projectSteps/" + tsProjectId,
                USERTYPE.CustomUser, null);

        if (lsReturn != null && !lsReturn.isEmpty()) {
            int lniPos = lsReturn.toLowerCase().indexOf("\"title\":\"" + lsStepName.toLowerCase() + "\"");
            if (lniPos >= 0) {
                lniPos = lsReturn.substring(0, lniPos).lastIndexOf("\"id\":\"") + 6;
                lsId = lsReturn.substring(lniPos, lniPos + 36);
                // System.out.println("Project " + tsProjectName + " (" + lsId +
                // ") a été trouvé!");
                return lsId;
            }
        }

        return null;
    }
}
