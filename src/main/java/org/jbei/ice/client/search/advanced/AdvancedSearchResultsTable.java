package org.jbei.ice.client.search.advanced;

import java.util.ArrayList;

import org.jbei.ice.client.common.table.HasEntryDataTable;
import org.jbei.ice.client.common.table.cell.EntryOwnerCell;
import org.jbei.ice.client.event.EntryViewEvent.EntryViewEventHandler;
import org.jbei.ice.shared.ColumnField;
import org.jbei.ice.shared.dto.SearchResultInfo;

import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.Window;

/**
 * Table for displaying search results. Default sort is by relevance
 *
 * @author Hector Plahar
 */
public abstract class AdvancedSearchResultsTable extends HasEntryDataTable<SearchResultInfo> {

    public AdvancedSearchResultsTable() {
    }

    @Override
    protected ArrayList<DataTableColumn<?>> createColumns() {

        ArrayList<DataTableColumn<?>> columns = new ArrayList<DataTableColumn<?>>();

        // selection column
        columns.add(super.addSelectionColumn());

        columns.add(addScoreColumn());

        // type column
        columns.add(super.addTypeColumn(true));

        // part id column. tooltip shows more info about entry
        columns.add(super.addPartIdColumn(false, 120, Unit.PX));

        // name column
        columns.add(super.addNameColumn());

        columns.add(addSummaryColumn());
        columns.add(addOwnerColumn());
        super.addHasAttachmentColumn();
        super.addHasSampleColumn();
        super.addHasSequenceColumn();
        columns.add(super.addCreatedColumn());

        return columns;
    }

    protected DataTableColumn<SafeHtml> addSummaryColumn() {
        DataTableColumn<SafeHtml> summaryColumn = new DataTableColumn<SafeHtml>(new SafeHtmlCell(),
                                                                                ColumnField.SUMMARY) {

            @Override
            public SafeHtml getValue(SearchResultInfo object) {
                String description = object.getEntryInfo().getShortDescription();
                if (description == null)
                    return SafeHtmlUtils.EMPTY_SAFE_HTML;

                int size = (Window.getClientWidth() - 850);
                if (size <= 0)
                    size = 50;

                return SafeHtmlUtils
                        .fromSafeConstant("<div style=\"width: "
                                                  + size
                                                  + "px; white-space: nowrap; overflow: hidden; text-overflow: "
                                                  + "ellipsis;\" title=\""
                                                  + description.replaceAll("\"", "'") + "\">"
                                                  + description + "</div>");
            }
        };

        this.addColumn(summaryColumn, "Summary");
        return summaryColumn;
    }

    protected DataTableColumn<SearchResultInfo> addOwnerColumn() {

        EntryOwnerCell<SearchResultInfo> cell = new EntryOwnerCell<SearchResultInfo>() {

            @Override
            public String getOwnerName(SearchResultInfo value) {
                return value.getEntryInfo().getOwner();
            }

            @Override
            public String getOwnerEmail(SearchResultInfo value) {
                return value.getEntryInfo().getOwnerEmail();
            }
        };

        DataTableColumn<SearchResultInfo> ownerColumn = new DataTableColumn<SearchResultInfo>(cell, ColumnField.OWNER) {
            @Override
            public SearchResultInfo getValue(SearchResultInfo object) {
                return object;
            }
        };

        this.addColumn(ownerColumn, "Owner");
        ownerColumn.setSortable(false);
        this.setColumnWidth(ownerColumn, 150, Unit.PX);
        return ownerColumn;
    }

    protected DataTableColumn<String> addScoreColumn() {
        DataTableColumn<String> scoreColumn = new DataTableColumn<String>(new TextCell(), ColumnField.RELEVANCE) {
            @Override
            public String getValue(SearchResultInfo object) {
                return object.getScore() + "";
            }
        };

        scoreColumn.setSortable(true);
        this.addColumn(scoreColumn, "Relevance");
        this.setColumnWidth(scoreColumn, 120, Unit.PX);
        return scoreColumn;
    }

    protected abstract EntryViewEventHandler getHandler();
}
