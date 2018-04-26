package ca.crim.nlp.pacte;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

import ca.crim.nlp.pacte.client.Corpus;

public class Demo {

    private QuickConfig poCfg = null;

    public Demo(QuickConfig toConfig) {
        poCfg = toConfig;
    }

    /**
     * Add basic resources to a custom user
     * 
     * @param tbTagset
     * @param tbSchemas
     * @param tbCorpus
     * @return
     */
    public boolean giveRessources(boolean tbTagset, boolean tbSchemas, boolean tbCorpus) {
        Corpus loCorpus = new Corpus(poCfg);

        if (tbTagset) {
            for (File loF : getResources("ca/crim/nlp/pacte/client"))
                if (loF.getName().endsWith(".tagset"))
                    System.out.println(loCorpus.createTagset(readFile(loF)));
        }

        return true;
    }

    private File[] getResources(String folder) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        URL url = loader.getResource(folder);
        String path = url.getPath();
        return new File(path).listFiles();
    }

    private String readFile(File loResource) {
        try {
            return new String(
                    Files.readAllBytes(Paths.get(ClassLoader.class
                            .getResource("/ca/crim/nlp/pacte/client/" + loResource.getName()).toURI())),
                    Charset.forName("UTF-8"));
        } catch (IOException | URISyntaxException e) {
            return null;
        }
    }

}
