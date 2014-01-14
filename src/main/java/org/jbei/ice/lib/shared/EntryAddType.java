package org.jbei.ice.lib.shared;

import org.jbei.ice.lib.dao.IDataTransferModel;
import org.jbei.ice.lib.dto.entry.EntryType;

public enum EntryAddType implements IDataTransferModel {

    PLASMID("Plasmid"),
    STRAIN("Strain"),
    PART("Part"),
    STRAIN_WITH_PLASMID("Strain with One Plasmid"),
    ARABIDOPSIS("Arabidopsis Seed");

    private String display;

    EntryAddType(String display) {
        this.display = display;
    }

    private EntryAddType() {
    }

    // TODO :
    public static EntryAddType stringToType(String str) {
        if (str == null)
            return null;

        if (str.contains(PLASMID.toString().toLowerCase()) && str.contains(STRAIN.toString().toLowerCase()))
            return STRAIN_WITH_PLASMID;

        for (EntryAddType type : EntryAddType.values()) {
            if (str.equalsIgnoreCase(type.toString()))
                return type;
        }
        return null;
    }

    public static EntryType addTypeToType(EntryAddType type) {
        if (type == STRAIN_WITH_PLASMID)
            return EntryType.STRAIN;

        for (EntryType entryType : EntryType.values()) {
            if (type.name().equals(entryType.name()))
                return entryType;
        }

        return null;
    }

    public String getDisplay() {
        return this.display;
    }

    @Override
    public String toString() {
        return this.display;
    }
}
