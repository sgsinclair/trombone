package ca.crim.nlp.pacte.client;

import ca.crim.nlp.pacte.QuickConfig;
import ca.crim.nlp.pacte.QuickConfig.USERTYPE;

import java.util.Map;

import org.json.JSONObject;

public class Lexicon {

    private QuickConfig poCfg = null;

    public Lexicon(QuickConfig toConfig) {
        poCfg = toConfig;
    }

    /**
     * Vérifie si le lexique existe et le crée au besoin.
     * 
     * @param tsNomCorpus
     * @param tsToken
     * @param tsLangage
     * @return
     */
    public String createLexicon(String tsLexiconName) {
        String lsReturn = "";
        String lsIdLexicon = null;
        JSONObject loNewLex = null;

        lsReturn = poCfg.postRequest(poCfg.getPacteBackend() + "Lexicons/lexicon",
                "{\"title\": \"" + tsLexiconName.replace("\"", "\\\"") + "\","
                        + "\"description\": \"\",\"licence\": \"\",\"version\":\"\",\"source\":\"\",\"tagsetId\": {}}",
                USERTYPE.CustomUser);

        loNewLex = new JSONObject(lsReturn);
        if (lsReturn != null && !lsReturn.isEmpty() && loNewLex.has("id")) {
            lsIdLexicon = loNewLex.getString("id");
        } else
            System.err.println("Create lexicon response : "
                    + (loNewLex.has("message") ? loNewLex.getString("message") : "unknown"));

        return lsIdLexicon;
    }

    /**
     * V�rifie si un lexique existe
     * 
     * @param tsLexiconName
     * @return GUID du lexique s'il existe, null sinon.
     */
    public String checkLexicon(String tsLexiconName) {
        String lsReturn = "";
        String lsIdLexicon = "";

        lsReturn = poCfg.getRequest(poCfg.getPacteBackend() + "Lexicons/lexicons", USERTYPE.CustomUser, null).trim();

        if (lsReturn != null && !lsReturn.isEmpty()) {
            if (lsReturn.toLowerCase().contains("\"title\":")) {

                String[] lasLex = lsReturn.substring(1, lsReturn.length() - 2).split("},{");

                for (String lsL : lasLex)
                    if (lsL.contains("\"" + tsLexiconName + "\""))

                        return lsIdLexicon;
            }
        }

        return null;
    }

    /**
     * Create a new domain with a new id
     * 
     * @param tsLexiconId
     * @param tsPreferredName
     * @param tsParentId
     * @param tasDomainNames
     *            : Language/Name
     * @return the unique ID of the new domain
     */
    public String createDomain(String tsLexiconId, String tsPreferredName, String tsParentId,
            Map<String, String> tasDomainNames) {
        String lsDom = null;
        String lsReturn = null;

        lsDom = "{\"userDefinedId\": \"\", \"lexiconId\": \"" + tsLexiconId + "\"," + "  \"parentDomainId\": null ,"
                + " \"parentDomain\": " + (tsParentId == null ? null : "\"" + tsParentId + "\"") + ","
                + "  \"name\": \"" + tsPreferredName.replace("\"", "\\\"") + "\", \"domainsTitleLocalized\": ["
                + getLocalizedString("title", tasDomainNames) + "]}";

        // Lancer l'appel
        lsReturn = poCfg.postRequest(poCfg.getPacteBackend() + "Domains/domain", lsDom, USERTYPE.CustomUser);

        // Récupérer l'id
        JSONObject loRet = new JSONObject(lsReturn);
        if (loRet.has("domainId"))
            return loRet.getString("domainId");
        else
            return null;
    }

    public String createConcept(String tsLexiconId, String tsPreferredName, Map<String, String> tasConceptNames,
            Map<String, String> tasExampleNames, Map<String, String> tasDescriptionNames) {
        String lsCon = null;
        String lsReturn = null;

        lsCon = "{\"userDefinedId\": \"\", \"lexiconId\": \"" + tsLexiconId + "\"," + "  \"name\": \""
                + tsPreferredName.replace("\"", "\\\"") + "\", \"conceptsTitleLocalized\": ["
                + getLocalizedString("title", tasConceptNames) + "], \"conceptsExampleLocalized\": ["
                + getLocalizedString("example", tasExampleNames) + "], \"conceptsDescriptionLocalized\": ["
                + getLocalizedString("description", tasDescriptionNames) + "]" + "}";

        // Lancer l'appel
        lsReturn = poCfg.postRequest(poCfg.getPacteBackend() + "Concepts/concept", lsCon, USERTYPE.CustomUser);

        // Récupérer l'id
        JSONObject loRet = new JSONObject(lsReturn);
        if (loRet.has("conceptId"))
            return loRet.getString("conceptId");
        else
            return null;
    }

    public void linkDomainConcept(String tsDomainId, String tsConceptId) {
        String lsCon = null;

        lsCon = "{\"conceptId\": \"" + tsConceptId + "\", \"domainId\": \"" + tsDomainId + "\"}";

        // Lancer l'appel
        poCfg.postRequest(poCfg.getPacteBackend() + "DomainsToConcepts/domainToConcept", lsCon, USERTYPE.CustomUser);

    }

    public void linkConceptTerm(String tsConceptId, String tsTermId) {
        String lsCon = null;

        lsCon = "{\"conceptId\": \"" + tsConceptId + "\", \"termId\": \"" + tsTermId + "\"}";

        // Lancer l'appel
        poCfg.postRequest(poCfg.getPacteBackend() + "TermsToConcepts/termToConcept", lsCon, USERTYPE.CustomUser);
    }

    public String createTerm(String tsLexiconId, String tsName, String tsUserId, String tsLangue, String tsPostag,
            String tsGenre, String tsNombre) {
        String lsTerm = null;
        String lsReturn = null;

        lsTerm = "{\"userDefinedId\": \"" + tsUserId + "\", " + "  \"lexiconId\": \"" + tsLexiconId + "\" ,"
                + "  \"language\": " + (tsLangue == null ? "\"\"" : "\"" + tsLangue + "\"") + "," + "  \"posTag\": "
                + (tsPostag == null ? "\"\"" : "\"" + tsPostag + "\"") + "," + "  \"genre\": "
                + (tsGenre == null ? "\"\"" : "\"" + tsGenre + "\"") + "," + "  \"number\": "
                + (tsNombre == null ? "\"\"" : "\"" + tsNombre + "\"") + "," + "  \"name\": \""
                + tsName.replace("\"", "\\\"") + "\"}";

        // Lancer l'appel
        lsReturn = poCfg.postRequest(poCfg.getPacteBackend() + "Terms/term", lsTerm, USERTYPE.CustomUser);

        // Récupérer l'id
        JSONObject loRet = new JSONObject(lsReturn);
        if (loRet.has("id"))
            return loRet.getString("id");
        else
            return null;
    }

    /**
     * 
     * @param tsLangs
     * @return
     */
    private String getLocalizedString(String tsHeader, Map<String, String> tsLangs) {
        String lsLang = "";

        // Loop les langues
        if (tsLangs != null)
            for (String lsKey : tsLangs.keySet()) {
                lsLang += "{\"" + tsHeader + "\":\"" + tsLangs.get(lsKey).replace("\"", "\\\"") + "\",\"language\":\""
                        + lsKey + "\"},";
            }

        if (tsLangs != null && !tsLangs.isEmpty() && lsLang.length() > 0)
            return lsLang.substring(0, lsLang.length() - 1);
        else
            return lsLang;
    }
}
