package ca.crim.nlp.voyant.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * Commonly used file and directory methods
 * 
 */
public class FileUtils {

    /**
     * OS end of file string
     */
    public final static String EOL = System.getProperty("line.separator");

    // File name operation

    /**
     * Get the extension of a file
     * 
     * @param fullStringName
     *            The name of the file
     * @return The extension (without the dot)
     */
    public static String getExtension(final String fullStringName) {
        int pos = fullStringName.lastIndexOf('.');
        String extension = "";

        if (pos != -1 && pos != fullStringName.length() - 1) {
            extension = fullStringName.substring(pos + 1).toLowerCase();
        }
        return extension;
    }

    /**
     * Check if a file is a .msg file
     * 
     * @param file
     *            The potential msg file
     * @return True if it is a .msg file
     */
    public static boolean isMsgFile(final File file) {
        String extension = getExtension(file.getName());
        return extension.compareTo("msg") == 0;
    }

    /**
     * Check if file is a package
     * 
     * @param filename
     * @return
     */
    public static boolean isPackage(String name) {
        return getExtension(name).toLowerCase().compareTo("zip") == 0;
    }

    /**
     * Remove the extension from a file name
     * 
     * @param fullStringName
     *            The file name without the path
     * @return The file name without extension
     */
    public static String getFileName(String fullStringName) {
        int pos = fullStringName.lastIndexOf('.');
        String name = "";

        if (pos != -1 && pos != fullStringName.length() - 1) {
            name = fullStringName.substring(0, pos);
        }
        return name;
    }

    /**
     * Get the folder of a file
     * 
     * @param aFile
     * @return The path of the folder containing the file
     */
    public static String getFolder(File aFile) {
        String folderPath = "";

        if (aFile.isDirectory()) {
            folderPath = aFile.getAbsolutePath();
        } else {
            int pos = aFile.getAbsolutePath().lastIndexOf(File.separator);
            if (pos != -1) {
                folderPath = aFile.getAbsolutePath().substring(0, pos);
            }
        }
        return folderPath;
    }

    /**
     * Get the last element in a path
     * 
     * @param path
     * @return
     */
    public static String getLastPathElement(final String path) {
        String lastElement = "";
        int position = path.lastIndexOf(File.separator, (int) Math.max(path.length() - 2, 0));

        if (position == -1) {
            lastElement = path;
        } else {
            lastElement = path.substring(position + 1);
        }

        return lastElement;
    }

    // Folder method

    /**
     * Delete a directory and all files it contains
     * 
     * @param file
     */
    public static void deleteDirectory(File file) {
        if (file.exists()) {
            if (file.isFile()) {
                file.delete();
            } else {
                for (File f : file.listFiles()) {
                    deleteDirectory(f);
                }
                file.delete();
            }
        }
    }

    /**
     * Delete a directory or all files it contains
     * 
     * @param file
     */
    public static void deleteDirectory(File file, boolean tbDeleteFileOnly) {
        if (file.exists()) {

            if (file.isFile()) {
                file.delete();

            } else {
                for (File f : file.listFiles()) {
                    deleteDirectory(f);
                }
                if (!tbDeleteFileOnly)
                    file.delete();
            }
        }
    }

    /**
     * Delete a directory or all files it contains
     * 
     * @param tsFullPath
     * @param tbDeleteFileOnly
     */
    public static void deleteDirectory(String tsFullPath, boolean tbDeleteFileOnly) {
        deleteDirectory(new File(tsFullPath), tbDeleteFileOnly);
    }

    /**
     * Get all files from a folder, filtered by extension
     * 
     * @param folder
     *            to check
     * @param tsExtension
     *            to filter with
     * @param allFiles
     *            : list of all files
     */
    public static void getFilesInFolder(File folder, String tsExtension, List<File> allFiles) {
        if (folder.isFile()) {
            if (isPackage(folder.getName())) {
                File extractedTo = new File(getFolder(folder) + folder.getName() + System.currentTimeMillis() + "/");
                extractedTo.mkdirs();

                try {
                    new ArchiveFile(folder).unpack(extractedTo);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                getFilesInFolder(extractedTo, tsExtension, allFiles);
            } else {
                if (tsExtension == null)
                    allFiles.add(folder);
                else if (folder.getName().substring(folder.getName().lastIndexOf(".") + 1)
                        .equalsIgnoreCase(tsExtension))
                    allFiles.add(folder);
            }
        } else if (folder.isDirectory()) {
            for (File f : folder.listFiles()) {
                getFilesInFolder(f, tsExtension, allFiles);
            }
        }
    }

    public static List<File> getFilesInFolder(File folder) {
        List<File> loFiles = new ArrayList<File>();

        getFilesInFolder(folder, loFiles);

        return loFiles;
    }

    /**
     * Get all files from a folder, filtered by extension
     * 
     * @param folder
     *            to check
     * @param tsExtension
     *            to filter with
     * @return list of all filtered files
     */
    public static List<String> getFilesInFolder(File folder, String tsExtension) {
        List<String> lasAllFiles = new Vector<String>();

        if (folder.isFile()) {
            if (isPackage(folder.getName())) {
                File extractedTo = new File(getFolder(folder) + folder.getName() + System.currentTimeMillis() + "/");
                extractedTo.mkdirs();

                try {
                    new ArchiveFile(folder).unpack(extractedTo);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                lasAllFiles.addAll(getFilesInFolder(extractedTo, tsExtension));

            } else {
                if (tsExtension == null)
                    lasAllFiles.add(folder.getAbsolutePath());
                else if (folder.getName().substring(folder.getName().lastIndexOf(".") + 1)
                        .equalsIgnoreCase(tsExtension))
                    lasAllFiles.add(folder.getAbsolutePath());
            }

        } else if (folder.isDirectory()) {
            for (File f : folder.listFiles()) {
                lasAllFiles.addAll(getFilesInFolder(f, tsExtension));
            }
        }

        return lasAllFiles;
    }

    /**
     * Get all files from a folder, filtered by extension
     * 
     * @param folder
     *            to check
     * @param tsExtension
     *            to filter with
     * @param allFiles
     *            : list of all files
     */
    public static void getFilesInFolder(String folder, String tsExtension, List<File> allFiles) {
        File loFile = new File(folder);

        if (loFile.exists())
            getFilesInFolder(loFile, tsExtension, allFiles);
    }

    /**
     * Get all files from a folder, filtered by extension
     * 
     * @param folder
     *            to check
     * @param tsExtension
     *            to filter with
     * @return list of all files
     */
    public static List<String> getFilesInFolder(String folder, String tsExtension) {
        List<String> lasAllFiles = new Vector<String>();
        File loFile = new File(folder);

        if (loFile.exists())
            lasAllFiles.addAll(getFilesInFolder(loFile, tsExtension));

        return lasAllFiles;
    }

    /**
     * Get all file in a folder
     * 
     * @param folder
     * @param allFiles
     */
    private static void getFilesInFolder(File folder, List<File> allFiles) {
        if (folder.isFile()) {
            if (isPackage(folder.getName())) {
                File extractedTo = new File(getFolder(folder) + folder.getName() + System.currentTimeMillis() + "/");
                extractedTo.mkdirs();

                try {
                    new ArchiveFile(folder).unpack(extractedTo);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                getFilesInFolder(extractedTo, allFiles);
            } else {
                allFiles.add(folder);
            }

        } else if (folder.isDirectory()) {
            for (File f : folder.listFiles()) {
                getFilesInFolder(f, allFiles);
            }
        }
    }

    /**
     * Copy a file without overwriting
     * 
     * @param source
     * @param destination
     * @throws IOException
     */
    public static void copyNoOverwrite(File source, File destination) throws IOException {

        int index = 0;
        while (destination.exists()) {
            destination = new File(getFolder(destination) + "/" + getFileName(destination.getName()) + index + "."
                    + getExtension(destination.getName()));
        }
        destination.createNewFile();
        copy(source, destination);
    }

    /**
     * Copy file and overwrite
     * 
     * @param source
     * @param destination
     * @throws IOException
     */
    public static void copy(File source, File destination) throws IOException {
        OutputStream out = new FileOutputStream(destination);
        InputStream in = new FileInputStream(source);

        byte[] buffer = new byte[2024];
        int numRead;

        while ((numRead = in.read(buffer)) >= 0) {
            out.write(buffer, 0, numRead);
        }

        out.close();
        in.close();
    }

    // File reader and writer

    /**
     * Read an UTF8 file
     */
    public static String readFile(final File file) throws IOException {
        return readFile(file, "UTF8");
    }

    /**
     * Read an UTF8 stream
     */
    public static String readFile(final InputStream file) throws IOException {
        return readFile(file, "UTF8");
    }

    /**
     * Read a file
     * 
     * @param file
     * @param encoding
     * @return
     * @throws IOException
     */
    public static String readFile(final File file, final String encoding) throws IOException {
        return readFile(new FileInputStream(file), encoding);
    }

    /**
     * Read an inputstream
     * 
     * @param file
     * @param encoding
     * @return
     * @throws IOException
     */
    public static String readFile(final InputStream file, final String encoding) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(file, encoding));

        String line = reader.readLine();
        while (line != null) {
            sb.append(new String(line.getBytes()));
            sb.append(EOL);
            line = reader.readLine();
        }
        reader.close();
        return sb.toString();
    }

    /**
     * Read a file from a stream and returns a list of lines.
     * 
     * @param file
     * @param encoding
     *            File encoding for reading input file
     * @return
     * @throws IOException
     */
    public static List<String> readAllLines(final InputStream toFileStream, final String tsEncoding)
            throws IOException {
        List<String> loLines = new ArrayList<String>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(toFileStream));

        String line = reader.readLine();
        while (line != null) {
            loLines.add(new String(line.getBytes(), tsEncoding));
            line = reader.readLine();
        }
        reader.close();
        return loLines;
    }

    /**
     * Read a file and returns a list of lines.
     * 
     * @param toFile
     * @param tsEncoding
     *            File encoding for reading input file
     * @return
     * @throws IOException
     */
    public static List<String> readAllLines(final File toFile, final String tsEncoding) throws IOException {
        List<String> loLines = new ArrayList<String>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(toFile)));

        String line = reader.readLine();
        while (line != null) {
            loLines.add(new String(line.getBytes(), tsEncoding));
            line = reader.readLine();
        }
        reader.close();
        return loLines;
    }

    /**
     * Read a file and returns a list of lines.
     * 
     * @param toFile
     * @param tsEncoding
     *            File encoding for reading input file
     * @return
     * @throws IOException
     */
    public static List<String> readAllLines(final File toFile) throws IOException {
        List<String> loLines = new ArrayList<String>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(toFile)));

        String line = reader.readLine();
        while (line != null) {
            loLines.add(new String(line.getBytes()));
            line = reader.readLine();
        }
        reader.close();
        return loLines;
    }

    /**
     * Write a file
     * 
     * @param content
     * @param outFile
     * @param encoding
     * @throws IOException
     */
    public static void writeToFile(final String content, final File outFile, final String encoding) throws IOException {
        if (!outFile.exists()) {
            outFile.createNewFile();
        }

        byte[] bytes = content.getBytes(encoding);
        FileOutputStream writer = new FileOutputStream(outFile);
        writer.write(bytes);
        writer.close();
    }

    /**
     * Write a file
     * 
     * @param content
     * @param outFile
     * @param encoding
     * @throws IOException
     */
    public static void writeToFile(final InputStream content, final File outFile) throws IOException {
        OutputStream outputStream = null;

//        if (!outFile.exists())
//            outFile.createNewFile();

        try {
            // write the inputStream to a FileOutputStream
            outputStream = new FileOutputStream(outFile);

            int read = 0;
            byte[] bytes = new byte[1024];

            while ((read = content.read(bytes)) != -1) {
                outputStream.write(bytes, 0, read);
            }

        } catch (IOException e) {
            e.printStackTrace();

        } finally {
            if (content != null) {
                try {
                    content.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();

                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }

    }

    /**
     * Append text at the end of a file
     * 
     * @param content
     *            String to write
     * @param outFile
     *            File to open or create
     * @param encoding
     *            Encoding to use
     * @throws IOException
     */
    public static void appendToFile(final String content, final String outFile, final String encoding)
            throws IOException {
        OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(outFile, true), encoding);
        out.write(content);
        out.close();
    }

    /**
     * 
     * @param content
     * @param outFile
     * @param encoding
     * @throws IOException
     */
    public static void appendToFile(final String content, final File toOutFile, final String encoding)
            throws IOException {
        OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(toOutFile, true), encoding);
        out.write(content);
        out.close();
    }

    /**
     * Write to an UTF8 file
     * 
     * @param content
     * @param outFile
     * @throws IOException
     */
    public static void writeToFile(final String content, final File outFile) throws IOException {
        writeToFile(content, outFile, "UTF8");
    }

    /**
     * Verify if a file exists on disk
     * 
     * @param tsFullPath
     *            : fully qualified path to the file
     * @return
     */
    public static boolean isFileExists(String tsFullPath) {
        return (new File("tsFullPath")).exists();
    }

    /**
     * 
     * @param tsPath
     * @return
     */
    public static boolean isCleanFilename(String tsPath) {
        String lsOS = System.getProperty("os.name").toLowerCase();

        if (lsOS.indexOf("win") >= 0) {
            return isCleanFilenameWindows(tsPath);
        } else if (lsOS.indexOf("mac") >= 0) {
            return isCleanFilenameMac(tsPath);
        } else { // assume Unix/Linux (nix or sunos)
            return isCleanFilenameOther(tsPath);
        }
    }

    public static boolean isCleanFilenameWindows(String tsPath) {
        return !tsPath.matches("[^\\\\/:*?\"<>|]*([\\\\/:*?\"<>|][^\\\\/:*?\"<>|]*)+");
    }

    public static boolean isCleanFilenameMac(String tsPath) {
        return !tsPath.matches("[^/:]*([/:][^/:]*)+");
    }

    public static boolean isCleanFilenameOther(String tsPath) {
        return !tsPath.matches("[^/]*([/][^/]*)+");
    }

    /**
     * Enlève les caractères invalides d'une chaine de caractère et les remplace
     * par un autre caractère.
     * 
     * @param tsPath
     *            Chemin ou nom de fichier à modifier.
     * @param tsReplacementChar
     *            Caractère de remplacement. Si null, un espace est utilisé.
     * @return
     */
    public static String cleanFilename(String tsPath, String tsReplacementChar) {
        String lsOS = System.getProperty("os.name").toLowerCase();

        if (tsReplacementChar == null)
            tsReplacementChar = "";

        if (lsOS.indexOf("win") >= 0) {
            return cleanFilenameWindows(tsPath, tsReplacementChar);
        } else if (lsOS.indexOf("mac") >= 0) {
            return cleanFilenameMac(tsPath, tsReplacementChar);
        } else { // assume Unix/Linux (nix or sunos)
            return cleanFilenameOther(tsPath, tsReplacementChar);
        }
    }

    public static String cleanFilenameWindows(String tsPath, String tsReplacementChar) {
        return tsPath.replaceAll("[\\\\/:*?\"<>|]", tsReplacementChar);
    }

    public static String cleanFilenameMac(String tsPath, String tsReplacementChar) {
        return tsPath.replaceAll("[/:]", tsReplacementChar);
    }

    public static String cleanFilenameOther(String tsPath, String tsReplacementChar) {
        return tsPath.replaceAll("[/]", tsReplacementChar);
    }
}
