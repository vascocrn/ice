package org.jbei.ice.client.bulkupload.sheet.header;

import org.jbei.ice.client.AppController;
import org.jbei.ice.client.bulkupload.model.SheetCellData;
import org.jbei.ice.client.bulkupload.sheet.CellColumnHeader;
import org.jbei.ice.client.bulkupload.sheet.Header;
import org.jbei.ice.client.bulkupload.sheet.cell.BioSafetySheetCell;
import org.jbei.ice.client.bulkupload.sheet.cell.FileInputCell;
import org.jbei.ice.client.bulkupload.sheet.cell.MultiSuggestSheetCell;
import org.jbei.ice.client.bulkupload.sheet.cell.StatusSheetCell;
import org.jbei.ice.shared.AutoCompleteField;
import org.jbei.ice.shared.dto.EntryInfo;
import org.jbei.ice.shared.dto.PlasmidInfo;
import org.jbei.ice.shared.dto.StrainInfo;

/**
 * Headers for strain with plasmid sheet
 *
 * @author Hector Plahar
 */
public class StrainWithPlasmidHeaders extends BulkUploadHeaders {

    public StrainWithPlasmidHeaders() {
        headers.add(new CellColumnHeader(Header.PI, true));
        headers.add(new CellColumnHeader(Header.FUNDING_SOURCE));
        headers.add(new CellColumnHeader(Header.IP));
        headers.add(new CellColumnHeader(Header.BIOSAFETY, true, new BioSafetySheetCell(), null));
        headers.add(new CellColumnHeader(Header.STATUS, true, new StatusSheetCell(), null));

        //strain information
        headers.add(new CellColumnHeader(Header.STRAIN_NAME, true, "e.g. JBEI-0001"));
        headers.add(new CellColumnHeader(Header.STRAIN_ALIAS));
        headers.add(new CellColumnHeader(Header.STRAIN_LINKS));
        headers.add(new CellColumnHeader(Header.STRAIN_SELECTION_MARKERS, false, new MultiSuggestSheetCell(
                AppController.autoCompleteData.get(AutoCompleteField.SELECTION_MARKERS)), null));
        headers.add(new CellColumnHeader(Header.STRAIN_PARENTAL_STRAIN));
        headers.add(new CellColumnHeader(Header.STRAIN_GEN_PHEN));
        headers.add(new CellColumnHeader(
                Header.STRAIN_PLASMIDS,
                false,
                new MultiSuggestSheetCell(AppController.autoCompleteData.get(AutoCompleteField.PLASMID_NAME)), null));
        headers.add(new CellColumnHeader(Header.STRAIN_KEYWORDS));
        headers.add(new CellColumnHeader(Header.STRAIN_SUMMARY));
        headers.add(new CellColumnHeader(Header.STRAIN_NOTES));
        headers.add(new CellColumnHeader(Header.STRAIN_REFERENCES));
        headers.add(new CellColumnHeader(Header.STRAIN_SEQ_FILENAME, false, new FileInputCell(), null));
        headers.add(new CellColumnHeader(Header.STRAIN_ATT_FILENAME, false, new FileInputCell(), null));

        // plasmid information
        headers.add(new CellColumnHeader(Header.PLASMID_NAME, true, "e.g. pTSH117"));
        headers.add(new CellColumnHeader(Header.PLASMID_ALIAS));
        headers.add(new CellColumnHeader(Header.PLASMID_LINKS));
        headers.add(new CellColumnHeader(Header.PLASMID_SELECTION_MARKERS, false, new MultiSuggestSheetCell(
                AppController.autoCompleteData.get(AutoCompleteField.SELECTION_MARKERS)), null));
        headers.add(new CellColumnHeader(Header.CIRCULAR));
        headers.add(new CellColumnHeader(Header.PLASMID_BACKBONE));
        headers.add(new CellColumnHeader(Header.PLASMID_PROMOTERS, false, new MultiSuggestSheetCell(
                AppController.autoCompleteData.get(
                        AutoCompleteField.PROMOTERS)), null));
        headers.add(new CellColumnHeader(Header.PLASMID_ORIGIN_OF_REPLICATION, false, new MultiSuggestSheetCell(
                AppController.autoCompleteData.get(AutoCompleteField.ORIGIN_OF_REPLICATION)), null));
        headers.add(new CellColumnHeader(Header.PLASMID_KEYWORDS));
        headers.add(new CellColumnHeader(Header.PLASMID_SUMMARY));
        headers.add(new CellColumnHeader(Header.PLASMID_NOTES));
        headers.add(new CellColumnHeader(Header.PLASMID_REFERENCES));
        headers.add(new CellColumnHeader(Header.PLASMID_SEQ_FILENAME, false, new FileInputCell(), null));
        headers.add(new CellColumnHeader(Header.PLASMID_ATT_FILENAME, false, new FileInputCell(), null));
    }

    @Override
    public SheetCellData extractValue(Header header, EntryInfo info) {
        SheetCellData data = extractCommon(header, info);
        if (data != null)
            return data;


        data = extractCommon(header, info.getInfo());
        if (data != null)
            return data;

        StrainInfo strain = (StrainInfo) info;
        PlasmidInfo plasmid = (PlasmidInfo) info.getInfo();

        String value = null;
        switch (header) {
            case STRAIN_PARENTAL_STRAIN:
                value = strain.getHost();
                break;

            case STRAIN_GEN_PHEN:
                value = strain.getGenotypePhenotype();
                break;

            case STRAIN_PLASMIDS:
                value = strain.getPlasmids();
                break;

            // plasmid specific
            case PLASMID_BACKBONE:
                value = plasmid.getBackbone();
                break;

            case PLASMID_ORIGIN_OF_REPLICATION:
                value = plasmid.getOriginOfReplication();
                break;

            case PLASMID_PROMOTERS:
                value = plasmid.getPromoters();
                break;

            case CIRCULAR:
                if (plasmid.getCircular() == null)
                    value = "";
                else
                    value = Boolean.toString(plasmid.getCircular());
                break;
        }

        if (value == null)
            return null;

        data = new SheetCellData();
        data.setId(value);
        data.setValue(value);
        return null;
    }
}
