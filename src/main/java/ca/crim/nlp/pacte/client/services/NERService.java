package ca.crim.nlp.pacte.client.services;

import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import ca.crim.nlp.pacte.QuickConfig;

public class NERService implements iServices {
    QuickConfig poConfig = null;
    String psServiceUrl = null;
    String psCorpusId = null;
    String psModel = null;
    String psDocUrl = null;
    Boolean pbDoLinking = false;
    String psLinkingMethod = null;
    String psReportUrl = null;
    String psSchemaUpload = null;
    String psAnnotationUploadUrl = null;
    List<String> psLabels = null;
    Map<String, String> poParams = new HashMap<String, String>();
    String psLastUUID = null;
    final String SERVICENAME = "pacte_semantic";
    final String TOOLNAME = "ner";

    public enum LINKING_METHOD {
        Cluster, AltNameLength, Population, Graph
    };

    public NERService(QuickConfig toCfg) throws InvalidParameterException {
        if (toCfg == null)
            throw new InvalidParameterException("QuickConfig parameter is null.");
        else {
            poConfig = toCfg;
            psServiceUrl = poConfig.getServiceUrl();
        }
    }

    /**
     * 
     * @return
     */
    public boolean setOptions(String tsCorpusId, String tsDocUrl, String tsModelName, boolean tbDoLinking,
            LINKING_METHOD tsLinkingMethod, String tsReportUrl, String tsAnnotationUploadUrl, String tsSchemaUploadUrl,
            List<String> tsLabels, Map<String, String> tasCustomParams) {
        psCorpusId = tsCorpusId;
        psDocUrl = tsDocUrl;
        psModel = tsModelName;
        pbDoLinking = tbDoLinking;

        switch (tsLinkingMethod) {
        case Cluster:
            psLinkingMethod = "cluster";
            break;
        case Population:
            psLinkingMethod = "population";
            break;
        case Graph:
            psLinkingMethod = "graph";
            break;
        case AltNameLength:
            psLinkingMethod = "altnamelength";
            break;
        }

        psReportUrl = tsReportUrl;
        psSchemaUpload = tsSchemaUploadUrl;
        psAnnotationUploadUrl = tsAnnotationUploadUrl;
        psLabels = tsLabels;
        if ((tasCustomParams != null) && (tasCustomParams.size() > 0))
            poParams.putAll(tasCustomParams);

        return true;
    }

    String getJSONConfig() {
        JSONObject loJ = new JSONObject();

        loJ.put("annot_out_url", psAnnotationUploadUrl);
        loJ.put("corpus_id", psCorpusId);
        JSONArray loLabel = new JSONArray();
        for (String lsVal : psLabels)
            loLabel.put(lsVal);
        loJ.put("labels", loLabel);
        loJ.put("linking", pbDoLinking);
        loJ.put("linking_method", psLinkingMethod);
        loJ.put("model_name", psModel);
        loJ.put("report_out_url", psReportUrl);
        loJ.put("schema_upload_url", psSchemaUpload);
        loJ.put("tool", TOOLNAME);
        if (poParams.size() > 0)
            for (String lsKey : poParams.keySet())
                loJ.put(lsKey, poParams.get(lsKey));

        return loJ.toString();
    }

    @Override
    public String execute() {
        String lsResults = null;

        lsResults = poConfig.postRequest(poConfig.getServiceUrl() + "pacte_semantic/process?doc_url=" + psDocUrl,
                getJSONConfig(), null);
        JSONObject loR = new JSONObject(lsResults);

        if (loR.has("uuid"))
            psLastUUID = loR.getString("uuid");

        return psLastUUID;
    }

    @Override
    public String getInfo() {
        return null;
    }

    @Override
    public String checkStatus(String tsUUID) {
        String lsResponse = null;

        lsResponse = poConfig.getRequest(poConfig.getServiceUrl() + SERVICENAME + "/status?uuid=" + tsUUID, null, null);

        return lsResponse;
    }

    @Override
    public String checkStatus() {
        return checkStatus(psLastUUID);
    }
}
