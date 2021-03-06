package org.jbei.ice.lib.parsers;

import org.biojava.bio.BioException;
import org.biojava.bio.seq.DNATools;
import org.biojava.bio.symbol.SimpleSymbolList;
import org.biojava.bio.symbol.SymbolList;
import org.jbei.ice.lib.dto.FeaturedDNASequence;

import java.util.ArrayList;

/**
 * Parser to handle file with simply nucleotide sequences. Technically these files are not FASTA
 * files, even though some people think they are.
 *
 * @author Zinovii Dmytriv, Timothy Ham
 */
public class PlainParser extends AbstractParser {

    @Override
    public FeaturedDNASequence parse(String textSequence, String... entryType) throws InvalidFormatParserException {
        SymbolList sl;
        try {
            textSequence = cleanSequence(textSequence);
            sl = new SimpleSymbolList(DNATools.getDNA().getTokenization("token"), textSequence
                    .replaceAll("\\s+", "").replaceAll("[\\.|~]", "-").replaceAll("[0-9]", ""));
        } catch (BioException e) {
            throw new InvalidFormatParserException("Couldn't parse Plain sequence!", e);
        }
        return new FeaturedDNASequence(sl.seqString(), new ArrayList<>());
    }
}
