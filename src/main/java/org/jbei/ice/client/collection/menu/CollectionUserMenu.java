package org.jbei.ice.client.collection.menu;

import java.util.ArrayList;
import java.util.Set;

import org.jbei.ice.shared.FolderDetails;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.HTMLTable.Cell;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

/**
 * Left bar menu for showing user collections. Also adds widgets such as
 * an icon for adding a new user collection and edit/delete
 * 
 * TODO : this could probably extend CollectionEntryMenu or a common
 * TODO : abstract parent class extracted from both
 * 
 * @author Hector Plahar
 */
public class CollectionUserMenu extends Composite implements HasClickHandlers {

    interface Resources extends ClientBundle {

        static Resources INSTANCE = GWT.create(Resources.class);

        @Source("org/jbei/ice/client/resource/image/plus.png")
        ImageResource plusImage();

        @Source("org/jbei/ice/client/resource/image/minus.png")
        ImageResource minusImage();

        @Source("org/jbei/ice/client/resource/image/edit.png")
        ImageResource editImage();

        @Source("org/jbei/ice/client/resource/image/delete.png")
        ImageResource deleteImage();

        @Source("org/jbei/ice/client/resource/image/busy.gif")
        ImageResource busyIndicatorImage();
    }

    private final FlexTable table;
    private MenuItem currentSelected;
    private MenuItem currentEditSelection;
    private final TextBox quickAddBox;
    private final Image quickAddButton;
    private int row;
    private final TextBox editCollectionNameBox;
    private int editRow = -1;
    private int editIndex = -1;
    private MenuCell previousSelected;

    public CollectionUserMenu() {

        table = new FlexTable();
        initWidget(table);

        // quick edit
        editCollectionNameBox = new TextBox();
        editCollectionNameBox.setStyleName("input_box");
        editCollectionNameBox.setWidth("99%");
        editCollectionNameBox.setVisible(false);

        // quick add widgets
        quickAddBox = new TextBox();
        quickAddBox.setStyleName("input_box");
        quickAddBox.setText("Enter new collection name...");
        quickAddBox.setWidth("99%");
        quickAddButton = new Image(Resources.INSTANCE.plusImage());
        quickAddButton.setStyleName("collection_quick_add_image");
        quickAddBox.addFocusHandler(new FocusHandler() {

            @Override
            public void onFocus(FocusEvent event) {
                quickAddBox.setText("");
            }
        });

        table.setCellPadding(0);
        table.setCellSpacing(0);
        table.setStyleName("collection_menu_table");

        String html = "<span>MY COLLECTIONS</span><span style=\"float: right\" id=\"quick_add\"></span>";
        HTMLPanel panel = new HTMLPanel(html);
        panel.add(quickAddButton, "quick_add");
        table.setWidget(row, 0, panel);
        table.getFlexCellFormatter().setStyleName(row, 0, "collections_menu_header");

        // add quick add box
        row += 1;
        table.setWidget(row, 0, quickAddBox);
        initComponents();
    }

    // todo : move to model/presenter/handler
    protected boolean validate() {
        if (quickAddBox.getText().isEmpty()) {
            quickAddBox.setStyleName("entry_input_error");
            return false;
        }
        return true;
    }

    private void initComponents() {
        quickAddButton.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                switchButton();
            }
        });
    }

    public TextBox getQuickEditBox() {
        return this.editCollectionNameBox;
    }

    public void addQuickEditBlurHandler(BlurHandler handler) {
        this.editCollectionNameBox.addBlurHandler(handler);
    }

    public void addQuickEditKeyDownHandler(final KeyDownHandler handler) {
        this.editCollectionNameBox.addKeyDownHandler(new KeyDownHandler() {

            @Override
            public void onKeyDown(KeyDownEvent event) {
                if (event.getNativeKeyCode() != KeyCodes.KEY_ENTER)
                    return;
                handler.onKeyDown(event);
            }
        });
    }

    public void showFolderCount(FolderDetails details) {
        for (int i = 0; i < table.getRowCount(); i += 1) {
            Widget w = table.getWidget(i, 0);
            if (!(w instanceof MenuCell))
                continue;

            MenuCell cell = (MenuCell) w;

            long id = Long.decode(cell.getMenuItem().getId());
            if (details.getId() == id) {
                table.setWidget(i, 0, cell);
                cell.showFolderCount();
            }
        }
    }

    public void setMenuItems(ArrayList<MenuItem> items) {
        if (items == null || items.isEmpty())
            return;

        for (MenuItem item : items) {
            addMenuItem(item);
        }
    }

    /**
     * checks if the user clicked within the menu contents
     * and not, for eg. the header
     * 
     * @param event
     *            user click event
     * @return true if a response is required for user selection
     */
    public boolean isValidClick(ClickEvent event) {
        if (event == null)
            return false;

        Cell cell = this.table.getCellForEvent(event);
        if (cell == null)
            return false;

        boolean isValid = (cell.getCellIndex() != 0 || cell.getRowIndex() != 0);
        if (!isValid)
            return isValid;

        if (quickAddBox.isVisible())
            isValid = (cell.getRowIndex() != 1);

        return isValid;
    }

    /**
     * replaces current edit cell (in menu)
     * with new cell with folder
     * 
     * @param folder
     *            new folder for cell
     */
    public void setMenuItem(MenuItem item) {
        if (this.editIndex == -1 && this.editRow == -1)
            return;

        if (item == null)
            return;

        final MenuCell cell = new MenuCell(item);
        cell.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                currentSelected = cell.getMenuItem();
            }
        });
        table.setWidget(editRow, editIndex, cell);
        this.editCollectionNameBox.setVisible(false);
    }

    public void addMenuItem(MenuItem item) {
        if (item == null)
            return;

        final MenuCell cell = new MenuCell(item);
        cell.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                currentSelected = cell.getMenuItem();
            }
        });

        row += 1;
        table.setWidget(row, 0, cell);
    }

    public boolean removeMenuItem(MenuItem item) {
        if (item == null)
            return false;

        for (int i = 0; i < table.getRowCount(); i += 1) {
            Widget w = table.getWidget(i, 0);
            if (!(w instanceof MenuCell))
                continue;

            MenuCell cell = (MenuCell) w;
            if (!cell.getMenuItem().getId().equals(item.getId()))
                continue;

            table.remove(cell);
            row -= 1;
            return true;
        }

        return false;
    }

    /**
     * sets the busy indicator where the folder counts are displayed
     * to indicate that some form of update is taking place
     * 
     * @param folders
     */
    public void setBusyIndicator(Set<String> ids) {
        for (int i = 0; i < table.getRowCount(); i += 1) {
            Widget w = table.getWidget(i, 0);
            if (!(w instanceof MenuCell))
                continue;

            MenuCell cell = (MenuCell) w;
            if (ids.contains(cell.getMenuItem().getId()))
                cell.showBusyIndicator();
        }
    }

    public void updateCounts(ArrayList<MenuItem> items) {
        for (int i = 0; i < table.getRowCount(); i += 1) {
            Widget w = table.getWidget(i, 0);
            if (!(w instanceof MenuCell))
                continue;

            MenuCell cell = (MenuCell) w;

            for (MenuItem item : items) {
                if (item.getId().equals(cell.getMenuItem().getId())) {
                    cell.updateCount(item.getCount());
                    cell.showFolderCount();
                    break;
                }
            }
        }
    }

    @Override
    public HandlerRegistration addClickHandler(final ClickHandler handler) {
        return addDomHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                if (!isValidClick(event))
                    return;

                handler.onClick(event);
            }
        }, ClickEvent.getType());
    }

    public Widget getQuickAddButton() {
        return this.quickAddButton;
    }

    public TextBox getQuickAddBox() {
        return this.quickAddBox;
    }

    public MenuItem getCurrentSelection() {
        return currentSelected;
    }

    public MenuItem getCurrentEditSelection() {
        return currentEditSelection;
    }

    public void switchButton() {
        if (quickAddBox.isVisible()) {
            quickAddButton.setUrl(Resources.INSTANCE.plusImage().getSafeUri());
            quickAddButton.setStyleName("collection_quick_add_image");
            quickAddBox.setVisible(false);
            quickAddBox.setStyleName("input_box");
        } else {
            quickAddButton.setUrl(Resources.INSTANCE.minusImage().getSafeUri());
            quickAddButton.setStyleName("collection_quick_add_image");
            quickAddBox.setText("");
            quickAddBox.setVisible(true);
            quickAddBox.setFocus(true);
        }
    }

    public void hideQuickText() {
        quickAddButton.setUrl(Resources.INSTANCE.plusImage().getSafeUri());
        quickAddButton.setStyleName("collection_quick_add_image");
        quickAddBox.setVisible(false);
        quickAddBox.setStyleName("input_box");
    }

    // inner class
    private class MenuCell extends Composite implements HasClickHandlers {

        private final HTMLPanel panel;
        private final MenuItem item;
        private final String html;
        private final Image busy;

        private Label count;
        private final HoverCell action;
        private final String folderId;

        public MenuCell(final MenuItem item) {

            super.sinkEvents(Event.ONMOUSEOVER | Event.ONMOUSEOUT);
            // text box used when user wishes to edit a collection name

            this.item = item;
            folderId = "right" + item.getId();
            action = new HoverCell();
            action.getEdit().addClickHandler(new ClickHandler() {

                @Override
                public void onClick(ClickEvent event) {
                    event.stopPropagation();
                    Cell cell = table.getCellForEvent(event);
                    editCollectionNameBox.setText(item.getName());
                    editRow = cell.getRowIndex();
                    editIndex = cell.getCellIndex();
                    currentEditSelection = item;
                    table.setWidget(cell.getRowIndex(), cell.getCellIndex(), editCollectionNameBox);
                    editCollectionNameBox.setVisible(true);
                    editCollectionNameBox.setFocus(true);
                }
            });

            html = "<span style=\"padding: 5px\" class=\"collection_user_menu\">" + item.getName()
                    + "</span><span class=\"menu_count\" id=\"" + folderId + "\"></span>";

            panel = new HTMLPanel(html);

            count = new Label(formatNumber(item.getCount()));

            panel.add(count, folderId);
            panel.setStyleName("collection_user_menu_row");
            initWidget(panel);

            // menu cell clickHandler
            addClickHandler();

            // init busy indicator
            busy = new Image(Resources.INSTANCE.busyIndicatorImage());
        }

        protected void addClickHandler() {
            this.addClickHandler(new ClickHandler() {

                @Override
                public void onClick(ClickEvent event) {
                    if (previousSelected != null)
                        previousSelected.removeStyleName("collection_user_menu_row_selected");

                    currentSelected = item;
                    MenuCell.this.addStyleName("collection_user_menu_row_selected");
                    previousSelected = MenuCell.this;
                }
            });
        }

        public void showBusyIndicator() {
            setRightPanel(busy);
        }

        public void showFolderCount() {
            setRightPanel(count);
        }

        public void updateCount(long newCount) {
            item.setCount(newCount);
            count = new Label(formatNumber(item.getCount()));
        }

        public MenuItem getMenuItem() {
            return this.item;
        }

        @Override
        public void onBrowserEvent(Event event) {
            super.onBrowserEvent(event);

            if (item.isSystem())
                return;

            switch (DOM.eventGetType(event)) {
            case Event.ONMOUSEOVER:
                setRightPanel(action);
                break;

            case Event.ONMOUSEOUT:
                EventTarget target = event.getRelatedEventTarget(); // image

                if (Element.is(target)) {
                    Element element = Element.as(target);
                    Element eDelete = action.getDelete().getElement();
                    Element eEdit = action.getEdit().getElement();

                    if (element.equals(eEdit) || element.equals(eDelete))
                        break;
                }
                setRightPanel(count);
                break;
            }
        }

        private void setRightPanel(Widget widget) {
            Widget toReplace = panel.getWidget(0);
            if (toReplace == null)
                Window.alert("Cannot replace widget");
            else {
                panel.remove(0);
                panel.add(widget, folderId);
            }
        }

        @Override
        public HandlerRegistration addClickHandler(ClickHandler handler) {
            return addDomHandler(handler, ClickEvent.getType());
        }

        private String formatNumber(long l) {
            NumberFormat format = NumberFormat.getFormat("##,###");
            return format.format(l);
        }
    }

    private class HoverCell extends Composite {
        private final HorizontalPanel panel;
        private final Image edit;
        private final Image delete;

        public HoverCell() {

            panel = new HorizontalPanel();
            panel.setStyleName("user_collection_action");
            initWidget(panel);

            edit = new Image(Resources.INSTANCE.editImage());
            delete = new Image(Resources.INSTANCE.deleteImage());

            panel.add(edit);
            panel.setHeight("16px");
            HTML pipe = new HTML("&nbsp;|&nbsp;");
            pipe.addStyleName("color_eee");
            panel.add(pipe);
            panel.add(delete);
            panel.setStyleName("menu_count");
        }

        public Image getEdit() {
            return this.edit;
        }

        public Image getDelete() {
            return this.delete;
        }
    }

    public void addQuickAddKeyPressHandler(final KeyPressHandler handler) {
        quickAddBox.addKeyPressHandler(new KeyPressHandler() {

            @Override
            public void onKeyPress(KeyPressEvent event) {
                if (event.getCharCode() != KeyCodes.KEY_ENTER)
                    return;

                if (!validate())
                    return;

                quickAddBox.setVisible(false);
                handler.onKeyPress(event);
            }
        });
    }
}
