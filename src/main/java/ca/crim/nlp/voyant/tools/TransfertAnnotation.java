package ca.crim.nlp.voyant.tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import ca.crim.nlp.pacte.QuickConfig;
import ca.crim.nlp.pacte.client.Corpus;
import ca.crim.nlp.pacte.client.PacteDocument;

/**
 * Transfert annotations from an annotation group (containing NER annotations)
 * to each separate annotation group of annotators in a project.
 * 
 */
public class TransfertAnnotation {
    QuickConfig poConfig = null;

    public static void main(String[] args) {
        TransfertAnnotation loTransfert = new TransfertAnnotation();
        Map<String, List<String>> loDocAnnots = null;
        String lsCorpusId = "ab7b0bbc-453b-44b2-b893-11bf2aa88bbf";
        String lsSourceGroup = "Locations";

        // Parse arguments for command line execution

        loTransfert.poConfig = new QuickConfig("https://patx-pacte.crim.ca", "username",
                "password", false, 1);
        loDocAnnots = loTransfert.readSourceAnnotations(lsCorpusId, lsSourceGroup);

        // Push annotations
        loTransfert.writeDestinationAnnotation(loDocAnnots, "s1", lsCorpusId);
    }

    /**
     * Read all the annotations from the source group.
     * 
     * @param tsCorpusId
     * @param tsBucketId
     * @return
     */
    Map<String, List<String>> readSourceAnnotations(String tsCorpusId, String tsBucketName) {
        Map<String, List<String>> loSources = new HashMap<String, List<String>>();
        Corpus loCorpus = new Corpus(poConfig);
        List<PacteDocument> lasDocs = null;
        String lsGroupId = null;

        // Get all documents
        lasDocs = loCorpus.getDocuments(tsCorpusId);
        lsGroupId = loCorpus.getGroupId(tsBucketName, tsCorpusId);

        for (PacteDocument loDoc : lasDocs) {
            loSources.put(loDoc.getID(), new ArrayList<String>());

            // Fetch all annotations for the document
            String lsAnnotations = loCorpus.getAnnotations(tsCorpusId, loDoc.getID(), lsGroupId + ":NER");
            if (!lsAnnotations.equals("{}")) {
                JSONObject loJson = new JSONObject(lsAnnotations);
                JSONArray laoAnnot = loJson.getJSONObject(tsCorpusId).getJSONObject(lsGroupId).getJSONArray("NER");
                for (int lniCpt = 0; lniCpt < laoAnnot.length(); lniCpt++) {

                    loSources.get(loDoc.getID()).add(convertAnnotation(laoAnnot.getJSONObject(lniCpt)));
                }
            }
        }

        return loSources;
    }

    /**
     * Convert a NER annotation to a NELinking format
     * 
     * @return
     */
    private String convertAnnotation(JSONObject toAnnot) {
        if (toAnnot == null)
            return "";

        // new annotation
        toAnnot.remove("annotationId");

        // change type
        toAnnot.put("schemaType", "NELinking");

        // rename adm_string -> admgeonamelink
        toAnnot.put("AdmGeoname", toAnnot.get("adm_string"));
        toAnnot.remove("adm_string");

        // rename uri -> geonamelink
        toAnnot.put("GeonameLink", toAnnot.get("uri"));
        toAnnot.remove("uri");

        // rename official_name -> officialName
        toAnnot.put("OfficialName", toAnnot.get("official_name"));
        toAnnot.remove("official_name");

        // rename confidence -> ServiceLinkConfidence
        toAnnot.put("ServiceLinkConfidence", toAnnot.get("confidence"));
        toAnnot.remove("confidence");

        // rename entity_type -> EntityType
        toAnnot.put("EntityType", toAnnot.get("entity_type"));
        toAnnot.remove("entity_type");

        // rename text -> SurfaceName
        toAnnot.put("SurfaceName", toAnnot.get("text"));
        toAnnot.remove("text");

        // rename country -> Country
        toAnnot.put("Country", toAnnot.get("country"));
        toAnnot.remove("country");

        return toAnnot.toString();
    }

    /**
     * 
     * @param toDocAnnots
     * @param tasBucketId
     */
    void writeDestinationAnnotation(Map<String, List<String>> toDocAnnots, String tsStepPrefix, String tsCorpusId) {
        Corpus loCorpus = new Corpus(poConfig);
        Map<String, String> loGroups = null;

        // Fetch all annotator's groups
        loGroups = loCorpus.getGroups(tsCorpusId);

        // Push annotations into each buckets
        for (String lsGroup : loGroups.keySet())
            if (loGroups.get(lsGroup).startsWith("#" + tsStepPrefix)) {
                for (String lsDocId : toDocAnnots.keySet()) {
                    // Add all the annotations
                    for (String lsAnnot : toDocAnnots.get(lsDocId))
                        loCorpus.addAnnotation(tsCorpusId, lsGroup, lsAnnot);
                }
            }
    }
}
