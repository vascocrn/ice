package org.jbei.ice.web.panels;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.Model;
import org.jbei.ice.lib.utils.RichTextRenderer;

public class ConfluenceMarkupPanel extends AbstractMarkupPanel {
    private static final long serialVersionUID = 1L;

    private Fragment markupFragment;
    private Fragment previewFragment;

    private AjaxLink confluenceMarkupLink;
    private AjaxLink previewMarkupLink;

    private TextArea markupTextArea;
    private MultiLineLabel renderedLabel;

    public ConfluenceMarkupPanel(String id) {
        super(id);

        confluenceMarkupLink = new AjaxLink("confluenceMarkupLink") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                confluenceMarkupLink.add(new SimpleAttributeModifier("class", "active"));
                previewMarkupLink.add(new SimpleAttributeModifier("class", "inactive"));

                previewFragment.setVisible(false);
                markupFragment.setVisible(true);

                target.addComponent(confluenceMarkupLink);
                target.addComponent(previewMarkupLink);
                target.addComponent(markupFragment);
                target.addComponent(previewFragment);
            }
        };

        previewMarkupLink = new AjaxLink("previewLink") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                confluenceMarkupLink.add(new SimpleAttributeModifier("class", "inactive"));
                previewMarkupLink.add(new SimpleAttributeModifier("class", "active"));

                previewFragment.setVisible(true);
                markupFragment.setVisible(false);

                target.addComponent(confluenceMarkupLink);
                target.addComponent(previewMarkupLink);
                target.addComponent(previewFragment);
                target.addComponent(markupFragment);
            }
        };

        confluenceMarkupLink.setOutputMarkupId(true);
        previewMarkupLink.setOutputMarkupId(true);
        confluenceMarkupLink.add(new SimpleAttributeModifier("class", "active"));
        previewMarkupLink.add(new SimpleAttributeModifier("class", "inactive"));

        add(confluenceMarkupLink);
        add(previewMarkupLink);

        markupFragment = createMarkupFragment();
        previewFragment = createPreviewFragment();

        previewFragment.setVisible(false);
        markupFragment.setVisible(true);

        add(markupFragment);
        add(previewFragment);
    }

    private Fragment createMarkupFragment() {
        Fragment fragment = new Fragment("markupPlaceholderFragment", "markupFragment", this);

        fragment.setOutputMarkupPlaceholderTag(true);
        fragment.setOutputMarkupId(true);

        Form<Void> form = new Form<Void>("form");
        add(form);

        markupTextArea = new TextArea<String>("notes", new Model<String>(""));
        markupTextArea.setEscapeModelStrings(false);

        OnChangeAjaxBehavior onChangeAjaxBehavior = new OnChangeAjaxBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                markupData = markupTextArea.getDefaultModelObjectAsString();
                String renderedData = "";

                if (markupData != null) {
                    renderedData = RichTextRenderer.confluenceToHtml(markupData);
                }

                renderedLabel.setDefaultModelObject(renderedData);
            }
        };

        markupTextArea.add(onChangeAjaxBehavior);

        form.add(markupTextArea);

        fragment.add(form);

        return fragment;
    }

    private Fragment createPreviewFragment() {
        Fragment fragment = new Fragment("previewPlaceholderFragment", "previewFragment", this);

        fragment.setOutputMarkupPlaceholderTag(true);
        fragment.setOutputMarkupId(true);

        renderedLabel = new MultiLineLabel("preview", new Model<String>(""));
        renderedLabel.setEscapeModelStrings(false);

        fragment.add(renderedLabel);

        return fragment;
    }

    public final TextArea<String> getMarkupTextArea() {
        return markupTextArea;
    }

    public void setData(String data) {
        markupTextArea.setDefaultModel(new Model<String>(data));
        markupData = data;

        String renderedData = "";

        if (markupData != null) {
            renderedData = RichTextRenderer.wikiToHtml(markupData);
        }

        renderedLabel.setDefaultModelObject(renderedData);
    }
}