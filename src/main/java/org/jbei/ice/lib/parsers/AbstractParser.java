package org.jbei.ice.lib.parsers;

import org.jbei.ice.lib.dto.FeaturedDNASequence;
import org.jbei.ice.lib.dto.entry.PartData;
import org.jbei.ice.lib.entry.HasEntry;
import org.jbei.ice.storage.DAOFactory;
import org.jbei.ice.storage.ModelToInfoFactory;
import org.jbei.ice.storage.hibernate.dao.SequenceDAO;
import org.jbei.ice.storage.model.Entry;

public abstract class AbstractParser extends HasEntry {

    protected final Entry entry;
    protected final String userId;
    protected final PartData partData;
    protected String fileName;
    private final SequenceDAO sequenceDAO;

    public AbstractParser(String userId, String entryId) {
        this.entry = getEntry(entryId);
        if (this.entry == null)
            throw new IllegalArgumentException("Could not retrieve entry with id " + entryId);
        this.partData = ModelToInfoFactory.getInfo(entry);
        this.userId = userId;
        this.sequenceDAO = DAOFactory.getSequenceDAO();
    }

    public AbstractParser() {
        this.entry = null;
        this.userId = null;
        this.partData = null;
        sequenceDAO = DAOFactory.getSequenceDAO();
    }

    public FeaturedDNASequence parse(String textSequence, String... entryType) throws InvalidFormatParserException {
        throw new UnsupportedOperationException("Not implemented for this parser");
    }

    /**
     * Replace different line termination characters with the newline character (\n).
     *
     * @param sequence Text to clean.
     * @return String with only newline character (\n).
     */
    protected String cleanSequence(String sequence) {
        sequence = sequence.trim();
        sequence = sequence.replace("\n\n", "\n"); // *nix
        sequence = sequence.replace("\n\r\n\r", "\n"); // win
        sequence = sequence.replace("\r\r", "\n"); // mac
        sequence = sequence.replace("\n\r", "\n"); // *win
        return sequence;
    }
}
