package ca.crim.nlp.voyant;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONObject;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.file.FileStorage;
import org.voyanttools.trombone.tool.progress.Progress;
import org.voyanttools.trombone.tool.progress.Progress.Status;
import org.voyanttools.trombone.util.TestHelper;

import ca.crim.nlp.pacte.QuickConfig;
import ca.crim.nlp.pacte.client.services.NERService;
import ca.crim.nlp.pacte.client.services.NERService.LINKING_METHOD;
import ca.crim.nlp.voyant.tools.ArchiveFile;
import ca.crim.nlp.voyant.tools.FileUtils;

/**
 * Client that manages the link between an online file storage and the NER
 * tagging service to provide NE annotations from a raw UTF-8 encoded text file.
 * 
 * The main function takes the input and output (respectivly) file paths as
 * parameters. If you want to use it within Java, use the
 *
 */
public class VoyantPacteClient {
    String psStorage = "";
    QuickConfig poConfig = null;
    NERService poNER = null;

    /**
     * Command-line example which receive 1) an input file path (to a raw UTF-8
     * text file or a zip file containing multiple raw text files) and a 2)
     * output directory in which to store the json files returned by the NER
     * tagging service from CRIM.
     * 
     * @param args
     * @throws IOException 
     */
    public static void main(String[] args) throws IOException {
        VoyantPacteClient loClient = null;
        String lsInput = null;
        String lsOutput = null;

        if (args.length != 3) {
            System.err.println("Command-line usage : java VoyantPacteClient inputFilePath outputPath configFilePath");
            return;
        }
        lsInput = args[0];
        lsOutput = args[1];
        String configFilePath = args[2];

        if (!(new File(lsInput).exists())) {
            System.err.println("File " + lsInput + " does not exists! Stopping process.");
            return;
        }
        
        File configFile = new File(configFilePath);
        assert configFile.exists() : "Config file does not exist: "+configFile;
        loClient = new VoyantPacteClient(configFile);
        
        Storage storage = new FileStorage();
        Progress progress = Progress.retrieve(storage, UUID.randomUUID().toString());
        Map<String, String> lasAnnots = loClient.getNERAnnotations(lsInput, progress);
        storage.destroy();
        if (lasAnnots == null) {
            System.err.println("The NER process could not process the file.");
            return;
        }
        // Save each json to a separate file, if multiple file were sent
        try {
            File loOut = new File(lsOutput);
            loOut.mkdirs();
            for (String lsKey : lasAnnots.keySet())
                FileUtils.writeToFile(lasAnnots.get(lsKey), new File(loOut, lsKey), "UTF-8");
            System.out.println("File stored in " + lsOutput);
            
        } catch (IOException e) {
            System.err.println("Cannot write to " + lsOutput);
            e.printStackTrace();
            return;
        }
    }

    /**
     * Standard constructor reading from the specified config file.
     * @throws IOException 
     */
    public VoyantPacteClient(File propertiesFile) throws IOException {
    		Properties properties = new Properties();
    		Reader reader = new FileReader(propertiesFile);
    		properties.load(reader);
    		reader.close();
    		initProperties(properties);
    }
    
    private void initProperties(Properties properties) {
		psStorage = properties.getProperty("storageserver");
		if (psStorage == null || psStorage.isEmpty()) {
			throw new IllegalArgumentException("storageserver must be configured");
		}
		poConfig = new QuickConfig(properties.getProperty("server"), 
				properties.getProperty("PSCAdmin"), 
				properties.getProperty("PSCAdminPwd"), 
				properties.getProperty("PACTEAdmin"), 
				properties.getProperty("PACTEAdminPwd"), 
				properties.getProperty("StandardUser"), 
				properties.getProperty("StandardUserPwd"), 
				Boolean.parseBoolean(properties.getProperty("Verbose")), 
				Integer.parseInt(properties.getProperty("TokenRenewDelay")), 
				properties.getProperty("ServiceUrl"));
		poNER = new NERService(poConfig);
    }

    /**
     * Manage the full
     * 
     * @param tsFilePath
     * @return
     * @throws IOException 
     */
    public Map<String, String> getNERAnnotations(String tsFilePath, Progress progress) throws IOException {
        Map<String, String> lasAnnotations = new HashMap<String, String>();
        String lsUserSpace = null;
        String lsUserId = null;
        String lsFileId = null;
        String lsResult = null;
        List<String> lasLabels = new ArrayList<String>();

        // Get userspace
        lsUserSpace = poConfig.getRequest(psStorage + "user_space", null, null);
        lsUserId = new JSONObject(lsUserSpace).getString("user_space_id");
        lsUserSpace = "?user_space_id=" + lsUserId;

        // Upload file
        lsResult = storeFile(tsFilePath, lsUserSpace);
        lsFileId = new JSONObject(lsResult).getString("filename");

        // Execute service
        lasLabels.add("GPE");
        poNER.setOptions(lsUserId, psStorage + "corpus/" + lsFileId + lsUserSpace, "NER_model_OntoNotes_GPE", true,
                LINKING_METHOD.Cluster, psStorage + "report" + lsUserSpace, psStorage + "annotations" + lsUserSpace,
                psStorage + "schema" + lsUserSpace, lasLabels, null);
        poNER.execute();

        // Monitor the execution by polling
        lsResult = poNER.checkStatus().toLowerCase();
        int current = 0;
        String currentString = "\"current\": ";
		progress.update(.1f, Status.RUNNING, "pactePending", "Pending Geolocation with PACTE");
        while (lsResult.contains("\"status\": \"pending\"") || lsResult.contains("\"status\": \"progress\"")) {
            try {
                Thread.sleep(200);
                lsResult = poNER.checkStatus().toLowerCase();
                int pos = lsResult.indexOf(currentString);
                if (pos>-1) {
                	pos+=currentString.length();
                	int newCurrent = Integer.parseInt(lsResult.substring(pos, lsResult.indexOf(',', pos)));
                	if (newCurrent>current) {
                		progress.update((float) newCurrent/100, Status.RUNNING, "pacteAnalysis", "Geolocation with PACTE");
                		current = newCurrent;
                	}
                }
            } catch (Exception e) {
            	try {
					progress.update(1f, Status.ABORTED, "pacteError", "Error during PACTE Geolocation: "+e.getMessage());
				} catch (IOException e1) {
					e1.printStackTrace();
				}
            	throw new RuntimeException(e);
            }
        }

        // Get the results by unzipping the file and reading all the JSONs
        if (lsResult.contains("success")) {
            File loTemp;
            String lsId = UUID.randomUUID().toString();
            loTemp = new File(new File(tsFilePath).getParentFile(), lsId + ".zip");
            saveStream(psStorage + "get_annotations" + lsUserSpace, loTemp);
            try {
                new File(loTemp.getParentFile(), lsId).mkdirs();
                new ArchiveFile(loTemp).unpack(new File(loTemp.getParentFile(), lsId));

                // Read all annotations
                for (File loFile : FileUtils.getFilesInFolder(new File(loTemp.getParentFile(), lsId))) {
                    String lsContent = FileUtils.readFile(loFile);
                    lasAnnotations.put(loFile.getName(), lsContent);
                }

                // Cleanup temp files
                FileUtils.deleteDirectory(new File(loTemp.getParentFile(), lsId));
                loTemp.delete();

            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        // Erase last status
        poConfig.deleteRequest(psStorage + "user_space/" + lsUserId, null, null);

        return lasAnnotations;
    }

    /**
     * Upload a file to the custom file server
     * 
     * @param tsFilePath
     *            Local path to the file to upload
     * @param tsUserSpace
     *            The userspace to upload to on the server. Must have been
     *            previously requested.
     */
    String storeFile(String tsFilePath, String tsUserSpace) {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpPost postRequest = null;
        String lsResponse = null;

        if ((tsUserSpace != null) && !tsUserSpace.isEmpty())
            postRequest = new HttpPost(psStorage + "/corpus" + tsUserSpace);
        else
            // Anonymous mode
            postRequest = new HttpPost(psStorage + "/corpus");

        try {
            MultipartEntity multiPartEntity = new MultipartEntity();
            multiPartEntity.addPart("fileName", new StringBody(tsFilePath));
            FileBody fileBody = new FileBody(new File(tsFilePath), "application/octect-stream");
            multiPartEntity.addPart("file", fileBody);
            postRequest.setEntity(multiPartEntity);

        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
        }

        // Executing the request.
        try {
            CloseableHttpResponse response = httpclient.execute(postRequest);
            if (response.getEntity() != null)
                lsResponse = readInput(response.getEntity().getContent());

            if (response.getStatusLine().getStatusCode() != 200)
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

    public void saveStream(String tsTargetEndpoint, File toOutput) {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        // String lsReturn = "";
        // URIBuilder loUriBuilder = null;
        // byte[] buffer = new byte[8 * 1024];
        HttpGet loGet = null;
        CloseableHttpResponse response = null;
        OutputStream output = null;

        try {
            loGet = new HttpGet(new URIBuilder(tsTargetEndpoint).toString());
            response = httpclient.execute(loGet);
            InputStream in = new BufferedInputStream(response.getEntity().getContent());

            if (response.getStatusLine().getStatusCode() != 200)
                System.out.println("Response Status line :" + response.getStatusLine());

            output = new FileOutputStream(toOutput);
            IOUtils.copy(in, output);

        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        } finally {
            try {
                response.getEntity().getContent().close();
                output.close();
            } catch (UnsupportedOperationException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return;
    }
}
