package org.jbei.ice.lib.vo;

import java.util.Set;

import org.jbei.ice.lib.models.Entry;
import org.jbei.ice.lib.models.SequenceFeature;

public interface ISequenceValueObject {
    int getId();

    void setId(int id);

    String getSequence();

    void setSequence(String sequence);

    String getSequenceUser();

    void setSequenceUser(String sequenceUser);

    String getFwdHash();

    void setFwdHash(String fwdHash);

    String getRevHash();

    void setRevHash(String revHash);

    void setEntry(Entry entry);

    Entry getEntry();

    void setSequenceFeatures(Set<SequenceFeature> sequenceFeatures);

    Set<SequenceFeature> getSequenceFeatures();
}