package org.jbei.ice.lib.entry.sequence;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;

import org.jbei.ice.ControllerException;
import org.jbei.ice.lib.access.PermissionException;
import org.jbei.ice.lib.access.PermissionsController;
import org.jbei.ice.lib.account.model.Account;
import org.jbei.ice.lib.dao.DAOException;
import org.jbei.ice.lib.dao.hibernate.TraceSequenceDAO;
import org.jbei.ice.lib.dto.ConfigurationKey;
import org.jbei.ice.lib.entry.EntryAuthorization;
import org.jbei.ice.lib.entry.model.Entry;
import org.jbei.ice.lib.entry.model.Plasmid;
import org.jbei.ice.lib.models.Sequence;
import org.jbei.ice.lib.models.TraceSequence;
import org.jbei.ice.lib.models.TraceSequenceAlignment;
import org.jbei.ice.lib.parsers.ABIParser;
import org.jbei.ice.lib.parsers.GeneralParser;
import org.jbei.ice.lib.parsers.InvalidFormatParserException;
import org.jbei.ice.lib.parsers.bl2seq.Bl2SeqException;
import org.jbei.ice.lib.parsers.bl2seq.Bl2SeqParser;
import org.jbei.ice.lib.parsers.bl2seq.Bl2SeqResult;
import org.jbei.ice.lib.search.blast.BlastException;
import org.jbei.ice.lib.search.blast.BlastPlus;
import org.jbei.ice.lib.search.blast.ProgramTookTooLongException;
import org.jbei.ice.lib.utils.SerializationUtils;
import org.jbei.ice.lib.utils.Utils;
import org.jbei.ice.lib.vo.DNASequence;
import org.jbei.ice.lib.vo.SequenceTraceFile;

/**
 * ABI to manipulate DNA sequence trace analysis
 *
 * @author Zinovii Dmytriv
 */
public class SequenceAnalysisController {

    private final TraceSequenceDAO traceDao;
    private final PermissionsController permissionsController;
    private final EntryAuthorization entryAuthorization;

    public static final String tracesDirName = "traces";

    public SequenceAnalysisController() {
        traceDao = new TraceSequenceDAO();
        permissionsController = new PermissionsController();
        entryAuthorization = new EntryAuthorization();
    }

    /**
     * Create a new {@link TraceSequence} record and associated with the {@link Entry} entry.
     * <p/>
     * Creates a database record and write the inputStream to disk.
     *
     * @param entry
     * @param filename
     * @param depositor
     * @param sequence
     * @param uuid
     * @param date
     * @param inputStream
     * @return Saved traceSequence
     * @throws ControllerException
     */
    public TraceSequence importTraceSequence(Entry entry, String filename, String depositor, String sequence,
            String uuid, Date date, InputStream inputStream) throws ControllerException {
        if (entry == null) {
            throw new ControllerException("Failed to save trace sequence with null entry!");
        }

        if (filename == null || filename.isEmpty()) {
            throw new ControllerException("Failed to save trace sequence without filename!");
        }

        if (sequence == null || sequence.isEmpty()) {
            throw new ControllerException("Failed to save trace sequence without sequence!");
        }

        TraceSequence traceSequence = new TraceSequence(entry, uuid, filename, depositor, sequence, date);
        File tracesDir = Paths.get(Utils.getConfigValue(ConfigurationKey.DATA_DIRECTORY), tracesDirName).toFile();

        try {
            return traceDao.create(tracesDir, traceSequence, inputStream);
        } catch (DAOException e) {
            throw new ControllerException(e);
        }
    }

    /**
     * Create a new {@link TraceSequence} record and associated with the {@link Entry} entry.
     * <p/>
     * Unlike importTraceSequence this method auto generates uuid and timestamp.
     *
     * @param entry
     * @param filename
     * @param depositor
     * @param sequence
     * @param inputStream
     * @return Saved traceSequence
     * @throws ControllerException
     */
    public TraceSequence uploadTraceSequence(Entry entry, String filename, String depositor,
            String sequence, InputStream inputStream) throws ControllerException {
        return importTraceSequence(entry, filename, depositor, sequence, Utils.generateUUID(), new Date(), inputStream);
    }

    /**
     * Remove a {@link TraceSequence} from the database and disk.
     *
     * @param traceSequence
     * @throws ControllerException
     * @throws PermissionException
     */
    public void removeTraceSequence(Account account, TraceSequence traceSequence) throws ControllerException,
            PermissionException {
        if (traceSequence == null) {
            throw new ControllerException("Failed to delete null Trace Sequence!");
        }

        entryAuthorization.expectWrite(account.getEmail(), traceSequence.getEntry());

        try {
            File tracesDir = Paths.get(Utils.getConfigValue(ConfigurationKey.DATA_DIRECTORY), tracesDirName).toFile();
            traceDao.delete(tracesDir, traceSequence);
        } catch (DAOException e) {
            throw new ControllerException(e);
        }
    }

    /**
     * Retrieve the {@link TraceSequence} associated with the given {@link Entry} entry.
     *
     * @param entry
     * @return Retrieved TraceSequence
     * @throws ControllerException
     */
    public List<TraceSequence> getTraceSequences(Entry entry) throws ControllerException {
        if (entry == null) {
            throw new ControllerException("Failed to get trace sequences for null entry!");
        }

        List<TraceSequence> traces = null;

        SequenceController sequenceController = new SequenceController();

        try {
            Sequence sequence = sequenceController.getByEntry(entry);

            if (sequence == null) { // it will remove invalid alignments
                rebuildAllAlignments(entry);

                traces = TraceSequenceDAO.getByEntry(entry);
            } else {
                traces = TraceSequenceDAO.getByEntry(entry);

                boolean wasUpdated = false;
                for (TraceSequence traceSequence : traces) {
                    if (traceSequence.getTraceSequenceAlignment() == null
                            || traceSequence.getTraceSequenceAlignment().getSequenceHash() == null
                            || traceSequence.getTraceSequenceAlignment().getSequenceHash()
                                            .isEmpty()
                            || !traceSequence.getTraceSequenceAlignment().getSequenceHash()
                                             .equals(sequence.getFwdHash())) {
                        buildOrRebuildAlignment(traceSequence, sequence);

                        wasUpdated = true;
                    }
                }

                if (wasUpdated) { // fetch again because alignment has been updated
                    traces = TraceSequenceDAO.getByEntry(entry);
                }
            }
        } catch (DAOException e) {
            throw new ControllerException(e);
        }

        return traces;
    }

    public TraceSequence getTraceSequenceByFileId(String fileId) throws ControllerException {
        try {
            return traceDao.getByFileId(fileId);
        } catch (DAOException e) {
            throw new ControllerException(e);
        }
    }

    /**
     * Parses a given sequence file (Genbank, Fasta, ABI) and return an {@link DNASequence}.
     *
     * @param bytes
     * @return Parsed Sequence as {@link DNASequence}.
     * @throws ControllerException
     */
    public DNASequence parse(byte[] bytes) throws ControllerException {
        if (bytes.length == 0) {
            return null;
        }

        // Trying to parse as Fasta, Genbank, etc
        DNASequence dnaSequence = GeneralParser.getInstance().parse(bytes);

        if (dnaSequence == null) {
            // Trying to parse as ABI

            ABIParser abiParser = new ABIParser();

            try {
                dnaSequence = abiParser.parse(bytes);
            } catch (InvalidFormatParserException e) {
                return null;
            }
        }

        return dnaSequence;
    }

    /**
     * Retrieve the {@link File} associated with the given {@link TraceSequence}.
     *
     * @param traceSequence
     * @return {@link File} object.
     * @throws ControllerException
     */
    public File getFile(TraceSequence traceSequence) throws ControllerException {
        return Paths.get(Utils.getConfigValue(ConfigurationKey.DATA_DIRECTORY), tracesDirName,
                         traceSequence.getFileId()).toFile();
    }

    /**
     * Retrieve the {@link SequenceTraceFile} value object of the given TraceSequence.
     *
     * @param traceSequence
     * @return SequenceTraceFile object
     * @throws ControllerException
     */
    public SequenceTraceFile getSequenceTraceFile(TraceSequence traceSequence) throws ControllerException {
        if (traceSequence == null) {
            return null;
        }

        String base64Data;
        File file = getFile(traceSequence);
        byte[] bytes;

        try (FileInputStream fileStream = new FileInputStream(file)) {
            bytes = new byte[(int) (file.length())];
            fileStream.read(bytes);
        } catch (IOException e) {
            throw new ControllerException(e);
        }

        base64Data = SerializationUtils.serializeBytesToBase64String(bytes);
        SequenceTraceFile result = new SequenceTraceFile();

        result.setDepositorEmail(traceSequence.getDepositor());
        result.setFileId(traceSequence.getFileId());
        result.setFileName(traceSequence.getFilename());
        result.setTimeStamp(new Date());
        result.setBase64Data(base64Data);

        return result;
    }

    /**
     * Calculate sequence alignment between the given {@link TraceSequence} and {@link Sequence}
     * using bl2seq, and save the result into the database.
     *
     * @param traceSequence traceSequence
     * @param sequence      sequence
     * @throws ControllerException
     */
    public void buildOrRebuildAlignment(TraceSequence traceSequence, Sequence sequence) throws ControllerException {
        if (traceSequence == null) {
            throw new ControllerException("Failed to rebuild alignment for null trace sequence!");
        }

        // if sequence is null => delete alignment
        if (sequence == null || sequence.getEntry() == null) {
            return;
        }

        // actually build alignment
        String traceSequenceString = traceSequence.getSequence();
        String entrySequenceString = sequence.getSequence();

        int entrySequenceLength = entrySequenceString.length();
        boolean isCircular = (sequence.getEntry() instanceof Plasmid) && ((Plasmid) sequence.getEntry()).getCircular();

        if (isCircular) {
            entrySequenceString += entrySequenceString;
        }

        String bl2seqOutput;
        try {
            bl2seqOutput = BlastPlus.runBlast2Seq(entrySequenceString, traceSequenceString);
        } catch (BlastException e) {
            throw new ControllerException(e);
        } catch (ProgramTookTooLongException e) {
            throw new ControllerException(e);
        }

        if (bl2seqOutput == null || bl2seqOutput.isEmpty()) {
            return;
        }

        try {
            List<Bl2SeqResult> bl2seqAlignmentResults = Bl2SeqParser.parse(bl2seqOutput);

            if (bl2seqAlignmentResults.size() > 0) {
                int maxAlignedSequenceLength = -1;
                Bl2SeqResult maxBl2SeqResult = null;

                for (Bl2SeqResult bl2seqResult : bl2seqAlignmentResults) {
                    int querySequenceLength = bl2seqResult.getQuerySequence().length();

                    if (maxAlignedSequenceLength < querySequenceLength) {
                        maxAlignedSequenceLength = querySequenceLength;
                        maxBl2SeqResult = bl2seqResult;
                    }
                }

                if (maxBl2SeqResult != null) {
                    int strand = maxBl2SeqResult.getOrientation() == 0 ? 1 : -1;
                    TraceSequenceAlignment traceSequenceAlignment = traceSequence.getTraceSequenceAlignment();
                    int queryStart = maxBl2SeqResult.getQueryStart();
                    int queryEnd = maxBl2SeqResult.getQueryEnd();
                    int subjectStart = maxBl2SeqResult.getSubjectStart();
                    int subjectEnd = maxBl2SeqResult.getSubjectEnd();

                    if (isCircular) {
                        if (queryStart > entrySequenceLength - 1) {
                            queryStart = queryStart - entrySequenceLength;
                        }

                        if (queryEnd > entrySequenceLength - 1) {
                            queryEnd = queryEnd - entrySequenceLength;
                        }

                        if (subjectEnd > entrySequenceLength - 1) {
                            subjectEnd = subjectEnd - entrySequenceLength;
                        }

                        if (subjectStart > entrySequenceLength - 1) {
                            subjectStart = subjectStart - entrySequenceLength;
                        }
                    }

                    if (traceSequenceAlignment == null) {
                        traceSequenceAlignment = new TraceSequenceAlignment(traceSequence,
                                                                            maxBl2SeqResult.getScore(), strand,
                                                                            queryStart, queryEnd,
                                                                            subjectStart, subjectEnd,
                                                                            maxBl2SeqResult.getQuerySequence(),
                                                                            maxBl2SeqResult.getSubjectSequence(),
                                                                            sequence.getFwdHash(),
                                                                            new Date());

                        traceSequence.setTraceSequenceAlignment(traceSequenceAlignment);
                    } else {
                        traceSequenceAlignment.setModificationTime(new Date());
                        traceSequenceAlignment.setScore(maxBl2SeqResult.getScore());
                        traceSequenceAlignment.setStrand(strand);
                        traceSequenceAlignment.setQueryStart(queryStart);
                        traceSequenceAlignment.setQueryEnd(queryEnd);
                        traceSequenceAlignment.setSubjectStart(subjectStart);
                        traceSequenceAlignment.setSubjectEnd(subjectEnd);
                        traceSequenceAlignment.setQueryAlignment(maxBl2SeqResult.getQuerySequence());
                        traceSequenceAlignment.setSubjectAlignment(maxBl2SeqResult.getSubjectSequence());
                        traceSequenceAlignment.setSequenceHash(sequence.getFwdHash());
                    }

                    traceDao.save(traceSequence);
                }
            }
        } catch (Bl2SeqException e) {
            throw new ControllerException(e);
        } catch (DAOException e) {
            throw new ControllerException(e);
        }
    }

    /**
     * Calculate sequence alignments between the sequence associated with an {@link Entry} entry
     * with all the {@link TraceSequence}s associated with that entry.
     * <p/>
     * Calls buildOrReplaceAlignment on each TraceSequence.
     *
     * @param entry
     * @throws ControllerException
     */
    public void rebuildAllAlignments(Entry entry) throws ControllerException {
        if (entry == null) {
            throw new ControllerException("Failed to rebuild alignment for null entry!");
        }

        SequenceController sequenceController = new SequenceController();
        Sequence sequence = sequenceController.getByEntry(entry);

        if (sequence == null) {
            return;
        }

        List<TraceSequence> traceSequences = getTraceSequences(entry);
        for (TraceSequence traceSequence : traceSequences) {
            buildOrRebuildAlignment(traceSequence, sequence);
        }
    }
}
