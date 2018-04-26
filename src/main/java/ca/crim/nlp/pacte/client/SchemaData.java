package ca.crim.nlp.pacte.client;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

public class SchemaData {
    List<String> FeatureList = new ArrayList<String>();
    
    public SchemaData(String tsJson) {
        JSONObject loSchema = new JSONObject(tsJson);
        
        if (loSchema.has("schema"))
            loSchema = new JSONObject(loSchema.getJSONObject("schema").getString("schemaJsonContent"));
   
        loSchema = loSchema.getJSONObject("properties");
        
        for (String lsKey : loSchema.keySet()) {
            if (",schematype,_corpusid,_documentid,offsets,".indexOf("," + lsKey.toLowerCase() + ",") < 0 )
            FeatureList.add(lsKey);
        }
        
        System.out.println(loSchema.toString());
    }
}
