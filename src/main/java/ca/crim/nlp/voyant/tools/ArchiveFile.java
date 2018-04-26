/**
 * 
 */
package ca.crim.nlp.voyant.tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Functions related to archived or zipped files
 *
 */
public class ArchiveFile {
    private File poPath = null;

    public ArchiveFile(File toFile) {
        poPath = toFile;
    }

    public ArchiveFile(String tsFile) {
        poPath = new File(tsFile);
    }

    /**
     * Unpack a zip file in a directory
     * 
     * @param zipFile
     *            The zip file
     * @param outputdir
     *            Destination of the output of the zip file
     * @throws Exception
     */
    public void unpack(File outputdir) throws Exception {
        if (FileUtils.getExtension(poPath.getName()).toLowerCase().compareTo("zip") != 0) {
            throw new Exception("Only zip package supported");
        }

        ZipFile zip = new ZipFile(poPath);
        Enumeration<? extends ZipEntry> elements = zip.entries();
        while (elements.hasMoreElements()) {
            ZipEntry entry = elements.nextElement();
            File fileOut = new File(outputdir.getAbsolutePath() + "/" + entry.getName());
            fileOut.createNewFile();
            InputStream content = zip.getInputStream(entry);

            if (content != null) {
                OutputStream out = new FileOutputStream(fileOut);

                byte[] buffer = new byte[2024];
                int numRead;

                while ((numRead = content.read(buffer)) >= 0) {
                    out.write(buffer, 0, numRead);
                }
                content.close();
                out.close();
            }
        }
        zip.close();
    }
}
