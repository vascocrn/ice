package org.jbei.ice.lib.search.blast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.biojava.bio.seq.DNATools;
import org.biojava.bio.seq.RNATools;
import org.biojava.bio.symbol.IllegalSymbolException;
import org.biojava.bio.symbol.SymbolList;
import org.jbei.ice.controllers.ApplicationContoller;
import org.jbei.ice.lib.logging.Logger;
import org.jbei.ice.lib.managers.ManagerException;
import org.jbei.ice.lib.managers.SequenceManager;
import org.jbei.ice.lib.models.Sequence;
import org.jbei.ice.lib.utils.JbeirSettings;
import org.jbei.ice.lib.utils.Utils;

public class Blast {
    public static final String BLASTN_PROGRAM = "blastn";
    public static final String TBLASTX_PROGRAM = "tblastx";

    private static boolean rebuilding;

    private static final String BL2SEQ_COMMAND_PATTERN = "%s -p blastn -i %s -j %s -r 2 -F F";
    private static final String BLASTALL_COMMAND_PATTERN = "%s -p %s -d %s -m 8";

    private String blastBlastall;
    private String bl2seq;
    private String blastDatabaseName;
    private String bigFastaFile = "bigfastafile";
    private String blastFormatLogFile;
    private String blastDirectory;

    public Blast() {
        blastDirectory = JbeirSettings.getSetting("BLAST_DIRECTORY");
        blastBlastall = JbeirSettings.getSetting("BLAST_BLASTALL");
        bl2seq = JbeirSettings.getSetting("BLAST_BL2SEQ");
        blastDatabaseName = blastDirectory + File.separator
                + JbeirSettings.getSetting("BLAST_DATABASE_NAME");
        blastFormatLogFile = JbeirSettings.getSetting("BLAST_DATABASE_NAME") + ".log";

        if (!isBlastDatabaseExists()) {
            Logger.info("Creating blast db for the first time");

            ApplicationContoller.scheduleBlastIndexRebuildJob(1000);
        }
    }

    public void rebuildDatabase() throws BlastException {
        try { // The big try
            synchronized (this) {
                File newbigFastaFileDir = new File(blastDirectory + ".new");

                if (newbigFastaFileDir.exists()) {
                    try {
                        FileUtils.deleteDirectory(newbigFastaFileDir);
                    } catch (Exception e) {
                        throw new BlastException(e);
                    }

                }
                if (!newbigFastaFileDir.mkdir()) {
                    throw new BlastException("Could not create " + blastDirectory + ".new");
                }
                File bigFastaFile = new File(newbigFastaFileDir.getPath() + File.separator
                        + this.bigFastaFile);
                FileWriter bigFastaWriter = new FileWriter(bigFastaFile);
                writeBigFastaFile(bigFastaWriter);
                formatBlastDb(newbigFastaFileDir);

                setRebuilding(true);
                renameBlastDb(newbigFastaFileDir);
                setRebuilding(false);
            }
        } catch (IOException e) {
            throw new BlastException("Failed to rebuild Blast database!", e);
        } catch (SecurityException e) {
            throw new BlastException("Failed to rebuild Blast database!", e);
        } catch (Exception e) {
            throw new BlastException("Failed to rebuild Blast database!", e);
        }
    }

    /**
     * get only the longest distinct matches. No partial matches for the
     * same record.
     * 
     * @param queryString
     *            Sequence to be queried
     * @return
     * @throws ProgramTookTooLongException
     * @throws BlastException
     */
    public ArrayList<BlastResult> query(String queryString, String blastProgram)
            throws ProgramTookTooLongException, BlastException {
        ArrayList<BlastResult> tempResults = queryAll(queryString, blastProgram);

        HashMap<String, BlastResult> tempHashMap = new HashMap<String, BlastResult>();

        for (BlastResult result : tempResults) {
            if (tempHashMap.containsKey(result.getSubjectId())) {
                BlastResult currentResult = tempHashMap.get(result.getSubjectId());

                if (result.getRelativeScore() > currentResult.getRelativeScore()) {
                    tempHashMap.put(result.getSubjectId(), result);
                }
            } else {
                tempHashMap.put(result.getSubjectId(), result);
            }
        }

        ArrayList<BlastResult> outputResult = new ArrayList<BlastResult>(tempHashMap.values());
        Collections.sort(outputResult);

        return outputResult;
    }

    public List<String> runBl2Seq(String query, List<String> subjects) throws BlastException,
            ProgramTookTooLongException {
        ArrayList<String> result = new ArrayList<String>();

        try {
            File dataDirectory = new File(JbeirSettings.getSetting("DATA_DIRECTORY"));

            File queryFile = File.createTempFile("query-", ".seq", dataDirectory);
            FileWriter referenceFileWriter = new FileWriter(queryFile);
            referenceFileWriter.write(query);
            referenceFileWriter.close();

            for (String subject : subjects) {
                File subjectFile = File.createTempFile("subject-", ".seq", dataDirectory);

                FileWriter subjectFileWriter = new FileWriter(subjectFile);
                subjectFileWriter.write(subject);
                subjectFileWriter.close();

                String commandString = String.format(BL2SEQ_COMMAND_PATTERN, bl2seq, queryFile
                        .getPath(), subjectFile.getPath());
                Logger.info("Bl2seq query: " + commandString);

                String resultItem = runExternalProgram(commandString);

                result.add(resultItem);

                if (!subjectFile.delete()) {
                    throw new BlastException("Could not delete subjectFile "
                            + subjectFile.getName());
                }
            }
            if (!queryFile.delete()) {
                throw new BlastException("Could not delete queryFile " + queryFile.getName());
            }
        } catch (IOException e) {
            throw new BlastException(e);
        }

        return result;
    }

    /**
     * Run bl2seq. Because bl2seq needs two inputs, it has to use external files.
     * 
     * @param query
     * @param subject
     * @return
     * @throws BlastException
     * @throws ProgramTookTooLongException
     */
    public String runBl2Seq(String query, String subject) throws BlastException,
            ProgramTookTooLongException {
        String result = "";

        ArrayList<String> subjects = new ArrayList<String>();
        subjects.add(subject);

        List<String> bl2seqResults = runBl2Seq(query, subjects);

        if (bl2seqResults.size() > 0) {
            result = bl2seqResults.get(0);
        }

        return result;
    }

    /**
     * Standard blast query
     * 
     * @param queryString
     * @return
     * @throws ProgramTookTooLongException
     * @throws BlastException
     */
    private ArrayList<BlastResult> queryAll(String queryString, String blastProgram)
            throws ProgramTookTooLongException, BlastException {
        ArrayList<BlastResult> result = new ArrayList<BlastResult>();

        if (!isBlastDatabaseExists()) {
            Logger.info("Creating blast database for the first time");

            ApplicationContoller.scheduleBlastIndexRebuildJob(5000);
        } else {
            while (isRebuilding()) {
                try {
                    wait(50);
                } catch (InterruptedException e) {
                    throw new BlastException(e);
                }
            }

            String commandString = String.format(BLASTALL_COMMAND_PATTERN, blastBlastall,
                getProgram(blastProgram), blastDatabaseName);

            Logger.info("Blast query: " + commandString);

            result = processBlastOutput(runExternalProgram(queryString, commandString));
        }

        return result;
    }

    private String getProgram(String program) throws BlastException {
        if (program != null && (program.equals(BLASTN_PROGRAM) || program.equals(TBLASTX_PROGRAM))) {
            return program;
        } else {
            throw new BlastException(new BlastException("Invalid program"));
        }
    }

    private boolean isBlastDatabaseExists() {
        File blastDatabaseFile = new File(blastDatabaseName + ".nsq");

        return blastDatabaseFile.exists();
    }

    private String breakUpLines(String input) {
        StringBuilder result = new StringBuilder();

        int counter = 0;
        int index = 0;
        int end = input.length();
        while (index < end) {
            result = result.append(input.substring(index, index + 1));
            counter = counter + 1;
            index = index + 1;

            if (counter == 59) {
                result = result.append("\n");
                counter = 0;
            }
        }
        return result.toString();
    }

    private void renameBlastDb(File newBigFastaFileDir) throws IOException, BlastException {
        File oldBlastDir = new File(blastDirectory + ".old");
        if (oldBlastDir.exists()) {
            FileUtils.deleteDirectory(oldBlastDir);
        }

        File currentBlastDir = new File(blastDirectory);
        if (currentBlastDir.exists()) {
            if (!currentBlastDir.renameTo(oldBlastDir)) {
                throw new BlastException("Could not rename directory " + blastDirectory + ".old");
            }
        } else {
            // no current blast directory
        }

        if (!newBigFastaFileDir.renameTo(currentBlastDir)) {
            throw new BlastException("Could not rename blast db");
        }
    }

    private void formatBlastDb(File bigFastaFileDir) throws IOException {
        ArrayList<String> commands = new ArrayList<String>();
        commands.add(JbeirSettings.getSetting("BLAST_FORMATDB"));
        commands.add("-i");
        commands.add(this.bigFastaFile);
        commands.add("-l");
        commands.add(this.blastFormatLogFile);
        commands.add("-n");
        commands.add(JbeirSettings.getSetting("BLAST_DATABASE_NAME"));
        commands.add("-o");
        commands.add("-pF");
        commands.add("-t");
        commands.add(JbeirSettings.getSetting("BLAST_DATABASE_NAME"));
        String commandString = Utils.join(" ", commands);
        Logger.info("formatdb: " + commandString);

        Runtime runTime = Runtime.getRuntime();

        Process process = runTime.exec(commandString, new String[0], bigFastaFileDir);
        InputStream blastOutputStream = process.getInputStream();
        InputStream blastErrorStream = process.getErrorStream();
        try {
            process.waitFor();
        } catch (InterruptedException e) {
            throw new IOException("Could not run formatdb", e);
        }
        StringWriter writer = new StringWriter();
        IOUtils.copy(blastOutputStream, writer);
        blastOutputStream.close();
        String outputString = writer.toString();
        Logger.debug("format output was: " + outputString);
        writer = new StringWriter();
        IOUtils.copy(blastErrorStream, writer);
        String errorString = writer.toString();
        Logger.debug("format error was: " + errorString);
        process.destroy();

        if (errorString.length() > 0) {
            throw new IOException("Could not format blast db");
        }
    }

    private void writeBigFastaFile(FileWriter bigFastaWriter) throws IOException, BlastException {
        List<Sequence> sequencesList = null;

        try {
            sequencesList = SequenceManager.getAllSequences();
        } catch (ManagerException e) {
            throw new BlastException(e);
        }

        for (Sequence sequence : sequencesList) {
            String recordId = sequence.getEntry().getRecordId();
            String sequenceString = "";

            String temp = sequence.getSequence();
            if (temp != null) {
                SymbolList symL = null;

                try {
                    symL = DNATools.createDNA(sequence.getSequence().trim());
                } catch (IllegalSymbolException e1) {
                    // maybe it's rna?
                    try {
                        symL = RNATools.createRNA(sequence.getSequence().trim());
                    } catch (IllegalSymbolException e2) {
                        // skip this sequence
                        Logger.debug("invalid characters in sequence for "
                                + sequence.getEntry().getRecordId());
                        Logger.debug(e2.toString());
                    }
                }

                if (symL != null) {
                    sequenceString = breakUpLines(symL.seqString());
                }
            }
            if (sequenceString.length() > 0) {
                bigFastaWriter.write(">" + recordId + "\n");
                bigFastaWriter.write(sequenceString + "\n");
            }
        }

        bigFastaWriter.flush();
        bigFastaWriter.close();
    }

    private String runExternalProgram(String commandString) throws ProgramTookTooLongException,
            BlastException {
        return runExternalProgram("", commandString);
    }

    private String runExternalProgram(String inputString, String commandString)
            throws ProgramTookTooLongException, BlastException {

        Runtime runTime = Runtime.getRuntime();
        StringBuilder outputString = new StringBuilder();
        StringBuilder errorString = new StringBuilder();

        try {
            Process process = runTime.exec(commandString);

            if (inputString.length() > 0) {
                BufferedWriter programInputWriter = new BufferedWriter(new OutputStreamWriter(
                        process.getOutputStream()));

                programInputWriter.write(inputString);
                programInputWriter.flush();
                programInputWriter.close();
                process.getOutputStream().close();
            }

            long maxWait = 5000L;
            long startTime = System.currentTimeMillis();

            BufferedReader programOutputReader = new BufferedReader(new InputStreamReader(process
                    .getInputStream()));
            BufferedReader programErrorReader = new BufferedReader(new InputStreamReader(process
                    .getErrorStream()));

            String tempError = null;
            String tempOutput = null;

            /*
             * How to Deal with External Blast
             * 
             * Blast seems to be blocked by the stdin not being flushed, and stdout
             * not being emptied. This means that this thread should read the stdout
             * of blast periodically. But since it may take a while for blast to to start
             * filling stdout, it will be empty for a while before there is any data in it.
             * 
             * So, to prevent blocking as well as doing proper time out, this loop
             * tries to read from the stdout and sterr. If nothing is read for a long 
             * time, it times out. If something is read, it's collected. If nothing is 
             * read after getting some output, it quits. 
             * 
             * @ tham
             * 
             */
            boolean errorStreamFinished = false;
            boolean outputStreamFinished = false;
            boolean somethingWasRead = false;
            while (true) {
                if (programOutputReader.ready()) {
                    somethingWasRead = true;
                    tempOutput = programOutputReader.readLine();

                    outputString.append(tempOutput);
                    outputString.append("\n");
                } else if (somethingWasRead) {
                    outputStreamFinished = true;
                }

                if (programErrorReader.ready()) {
                    somethingWasRead = true;
                    tempError = programErrorReader.readLine();

                    errorString.append(tempError);
                    errorString.append("\n");

                } else if (somethingWasRead) {
                    errorStreamFinished = true;
                }
                if (errorStreamFinished && outputStreamFinished) {
                    break;
                }
                if (System.currentTimeMillis() - startTime > maxWait) {
                    throw new ProgramTookTooLongException("Blast took too long");
                }
            }

            programOutputReader.close();
            programErrorReader.close();
            process.destroy();

        } catch (IOException e) {
            Logger.warn("IO exception in running external program: " + e.toString());
        }

        if (errorString.length() > 0) {
            throw new BlastException(errorString.toString());
        }
        return outputString.toString();
    }

    private ArrayList<BlastResult> processBlastOutput(String blastOutput) {
        ArrayList<String> lines = new ArrayList<String>(Arrays.asList(blastOutput.split("\n")));

        ArrayList<BlastResult> blastResults = new ArrayList<BlastResult>();

        for (String line : lines) {
            String[] columns = line.split("\t");
            if (columns.length == 12) {
                BlastResult blastResult = new BlastResult();

                blastResult.setQueryId(columns[0]);
                blastResult.setSubjectId(columns[1]);
                blastResult.setPercentId(Float.parseFloat(columns[2]));
                blastResult.setAlignmentLength(Integer.parseInt(columns[3]));
                blastResult.setMismatches(Integer.parseInt(columns[4]));
                blastResult.setGapOpenings(Integer.parseInt(columns[5]));
                blastResult.setqStart(Integer.parseInt(columns[6]));
                blastResult.setqEnd(Integer.parseInt(columns[7]));
                blastResult.setsStart(Integer.parseInt(columns[8]));
                blastResult.setsEnd(Integer.parseInt(columns[9]));
                blastResult.seteValue(Float.parseFloat(columns[10]));
                blastResult.setBitScore(Float.parseFloat(columns[11]));
                blastResult.setRelativeScore(blastResult.getPercentId()
                        * blastResult.getAlignmentLength() * blastResult.getBitScore());
                blastResults.add(blastResult);
            }
        }
        return blastResults;
    }

    private static synchronized void setRebuilding(boolean rebuilding) {
        Blast.rebuilding = rebuilding;
    }

    private static synchronized boolean isRebuilding() {
        return rebuilding;
    }

    public static void main(String[] args) {
        Blast b = new Blast();

        String reference = "ATGAGCAAAGGCGAAGAACTGTTTACCGGCGTGGTGCCGATTCTGGTGGAACTGGATGGCGATGTGAACGGCCATAAATTTAGCGTGAGCGGCGAAGGCGAAGGCGATGCGACCTATGGCAAACTGACCCTGAAATTTATTTGCACCACCGGCAAACTGCCGGTGCCGTGGCCGACCCTGGTGACCACCTTTACCTATGGCGTGCAGTGCTTTGCGCGCTATCCGGATCATATGAAACAGCATGATTTTTTTAAAAGCGCGATGCCGGAAGGCTATGTGCAGGAACGCACCATTTTTTTTAAAGATGATGGCAACTATAAAACCCGCGCGGAAGTGAAATTTGAAGGCGATACCCTGGTGAACCGCATTGAACTGAAAGGCATTGATTTTAAAGAAGATGGCAACATTCTGGGCCATAAACTGGAATATAACTATAACAGCCATAAAGTGTATATTACCGCGGATAAACAGAAAAACGGCATTAAAGTGAACTTTAAAACCCGCCATAACATTGAAGATGGCAGCGTGCAGCTGGCGGATCATTATCAGCAGAACACCCCGATTGGCGATGGCCCGGTGCTGCTGCCGGATAACCATTATCTGAGCACCCAGAGCGCGCTGAGCAAAGATCCGAACGAAAAACGCGATCATATGGTGCTGCTGGAATTTGTGACCGCGGCGGGCATTACCCATGGCATGGATGAACTGTATAAAa";
        String subject = "ATGAGCAAAGGCGAAGAACTGTTTACCGGCGTGGTGCCGATTCTGGTGGAACTGGATGGCGATGTGAACGGCCATAAATTTAGCGTGCGCGGCGAAGGCGAAGGCGATGCGACCAACGGCAAACTGACCCTGAAATTTATTTGCACCACCGGCAAACTGCCGGTGCCGTGGCCGACCCTGGTGACCACCCTGACCTATGGCGTGCAGTGCTTTAGCCGCTATCCGGATCATATGAAACAGCATGATTTTTTTAAAAGCGCGATGCCGGAAGGCTATGTGCAGGAACGCACCATTAGCTTTAAAGATGATGGCACCTATAAAACCCGCGCGGAAGTGAAATTTGAAGGCGATACCCTGGTGAACCGCATTGAACTGAAAGGCATTGATTTTAAAGAAGATGGCAACATTCTGGGCCATAAACTGGAATATAACTTTAACAGCCATAACGTGTATATTACCGCGGATAAACAGAAAAACGGCATTAAAGCGAACTTTAAAATTCGCCATAACGTGGAAGATGGCAGCGTGCAGCTGGCGGATCATTATCAGCAGAACACCCCGATTGGCGATGGCCCGGTGCTGCTGCCGGATAACCATTATCTGAGCACCCAGAGCGTGCTGAGCAAAGATCCGAACGAAAAACGCGATCATATGGTGCTGCTGGAATTTGTGACCGCGGCGGGCATTACCCATGGCATGGATGAACTGTATAAA";
        String subject2 = "ATGAGCAAAGGCGAAGAACTGTTTACCGGCGTGGTGCCGATTCTGGTGGAACTGGATGGCGATGTGAACGGCCATAAATTTAGCGTGAGCGGCGAAGGCGAAGGCGATGCGACCTATGGCAAACTGACCCTGAAATTTATTTGCACCACCGGCAAACTGCCGGTGCCGTGGCCGACCCTGGTGACCACCCTGACCTATGGCGTGCAGTGCTTTAGCCGCTATCCGGATCATATGAAACAGCATGATTTTTTTAAAAGCGCGATGCCGGAAGGCTATGTGCAGGAACGCACCATTTTTTTTAAAGATGATGGCAACTATAAAACCCGCGCGGAAGTGAAATTTGAAGGCGATACCCTGGTGAACCGCATTGAACTGAAAGGCATTGATTTTAAAGAAGATGGCAACATTCTGGGCCATAAACTGGAATATAACTATAACAGCCATAACGTGTATATTATGGCGGATAAACAGAAAAACGGCATTAAAGTGAACTTTAAAATTCGCCATAACATTGAAGATGGCAGCGTGCAGCTGGCGGATCATTATCAGCAGAACACCCCGATTGGCGATGGCCCGGTGCTGCTGCCGGATAACCATTATCTGAGCACCCAGAGCGCGCTGAGCAAAGATCCGAACGAAAAACGCGATCATATGGTGCTGCTGGAATTTGTGACCGCGGCGGGCATTACCCATGGCATGGATGAACTGTATAAA";

        try {
            String result = b.runBl2Seq(reference, subject);
            System.out.println(result);
        } catch (BlastException e) {
            e.printStackTrace();
        } catch (ProgramTookTooLongException e) {
            e.printStackTrace();
        }

        ArrayList<String> list = new ArrayList<String>();
        list.add(subject);
        list.add(subject2);

        try {
            List<String> results = b.runBl2Seq(reference, list);
            for (String result : results) {
                System.out.println("###############");
                System.out.println(result);
            }
        } catch (BlastException e) {
            e.printStackTrace();
        } catch (ProgramTookTooLongException e) {
            e.printStackTrace();
        }
    }
}
