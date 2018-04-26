package ca.crim.nlp.voyant.tools;

import java.io.File;
import java.util.Calendar;

import ca.crim.nlp.pacte.QuickConfig;
import ca.crim.nlp.pacte.client.Corpus;

public class BackupRestore {
    static QuickConfig poCfg = null;

    public static void main(String[] args) {
        poCfg = new QuickConfig("https://patx-pacte.crim.ca", "username", "password", false, 1);
        runBackup("Pilote Voyant", "D:\\Dataset\\VoyantHisto\\backup");

        poCfg = new QuickConfig("https://patx-pacte.crim.ca", "pierre-andre.menard@crim.ca", "mRBUVtPM7El9qXNri944", false, 1);
        runRestore("D:\\Dataset\\VoyantHisto\\backup\\13_4_2018\\Pilote Voyant");
    }

    static public void runBackup(String tsNomCorpus, String tsBackupPath) {
        Corpus loCorpus = new Corpus(poCfg);
        Calendar loCal = Calendar.getInstance();
        
        // Ajouter le jour pour le backup
        tsBackupPath += "\\" + loCal.get(Calendar.DAY_OF_MONTH) + "_" + (loCal.get(Calendar.MONTH) +1) + "_"
                + loCal.get(Calendar.YEAR) + "\\" + tsNomCorpus;
        new File(tsBackupPath).mkdirs();
        System.out.println("Backup de <" + tsNomCorpus + "> vers " + tsBackupPath);
        loCorpus.exportToDisk(loCorpus.getCorpusId(tsNomCorpus), tsBackupPath, null);
        System.out.println("Exportation terminée!");
    }

    static public void runRestore(String tsPath) {
        Corpus loCorpus = new Corpus(poCfg);

        if (!new File(tsPath).exists())
            System.err.println("Path inexistant!");
        else {
            System.out.println("Upload du corpus à partir de " + tsPath);
            loCorpus.importCorpus(tsPath);
            System.out.println("Importation terminée!");
        }
    }
}
