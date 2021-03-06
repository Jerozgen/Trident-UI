package com.energyxxer.trident.ui.editor.completion;

import com.energyxxer.enxlex.lexical_analysis.summary.SummaryModule;
import com.energyxxer.enxlex.suggestions.Suggestion;
import com.energyxxer.enxlex.suggestions.SuggestionModule;
import com.energyxxer.trident.global.keystrokes.KeyMap;
import com.energyxxer.trident.main.window.TridentWindow;
import com.energyxxer.trident.main.window.sections.quick_find.StyledExplorerMaster;
import com.energyxxer.trident.ui.editor.TridentEditorComponent;
import com.energyxxer.trident.ui.editor.behavior.editmanager.edits.CompoundEdit;
import com.energyxxer.trident.ui.editor.behavior.editmanager.edits.DeletionEdit;
import com.energyxxer.trident.ui.editor.behavior.editmanager.edits.InsertionEdit;
import com.energyxxer.trident.ui.editor.completion.snippets.Snippet;
import com.energyxxer.trident.ui.editor.completion.snippets.SnippetManager;
import com.energyxxer.trident.ui.modules.ModuleToken;
import com.energyxxer.trident.ui.scrollbar.OverlayScrollPane;
import com.energyxxer.trident.ui.styledcomponents.StyledLabel;
import com.energyxxer.trident.ui.theme.change.ThemeListenerManager;
import com.energyxxer.util.Lazy;
import com.energyxxer.util.StringUtil;
import com.energyxxer.util.logger.Debug;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;

public class SuggestionDialog extends JDialog implements KeyListener, FocusListener, SuggestionInterface {
    private TridentEditorComponent editor;

    private OverlayScrollPane scrollPane;
    private StyledLabel parameterLabel;
    private StyledExplorerMaster explorer;

    private ThemeListenerManager tlm = new ThemeListenerManager();

    private SummaryModule summary = null;
    private SummaryModule lastSuccessfulSummary = null;

    private boolean locked = false;
    private boolean forceLocked = false;

    private SuggestionModule activeResults = null;

    private ArrayList<ExpandableSuggestionToken> activeTokens = new ArrayList<>();
    private boolean safe = false;

    public SuggestionDialog(TridentEditorComponent editor) {
        super(TridentWindow.jframe, false);
        this.setUndecorated(true);
        this.editor = editor;

        this.explorer = new StyledExplorerMaster("EditorSuggestions");

        JPanel contentPane = new JPanel(new BorderLayout());
        scrollPane = new OverlayScrollPane(tlm, explorer);
        contentPane.add(scrollPane);
        contentPane.add(parameterLabel = new StyledLabel(" <ENTITY>", "EditorSuggestions.header", tlm), BorderLayout.NORTH);

        this.setContentPane(contentPane);

        tlm.addThemeChangeListener(t -> {
            //titleBar.setBackground(t.getColor(new Color(230, 230, 230), "FindInPath.header.background"));
            int thickness = Math.max(t.getInteger(1, "EditorSuggestions.border.thickness"), 0);
            contentPane.setBackground(t.getColor(new Color(200, 200, 200), "EditorSuggestions.header.background"));
            contentPane.setBorder(BorderFactory.createMatteBorder(thickness, thickness, thickness, thickness, t.getColor(new Color(200, 200, 200), "EditorSuggestions.border.color")));
            parameterLabel.setBorder(BorderFactory.createMatteBorder(0, 0, thickness, 0, t.getColor(new Color(200, 200, 200), "EditorSuggestions.border.color")));
        });

        editor.addKeyListener(this);
        editor.addFocusListener(this);
        this.addKeyListener(this);

        editor.addCharacterDriftListener(h -> {
            if(summary != null) {
                summary.updateIndices(h);
            }
        });
    }

    public void showSuggestions(SuggestionModule results) {
        if(this.isVisible()) return;
        if(!safe) return;
        activeResults = results;
        explorer.clear();
        activeTokens.clear();

        boolean any = false;
        boolean anyExpandable = false;

        if(results != null) {
            for(Snippet snippet : SnippetManager.getAll()) {
                snippet.expanderApplied = false;
            }
            StringBuilder sb = new StringBuilder();
            boolean createdEverywhereSnippets = false;
            for (int i = 0; i < results.getSuggestions().size(); i++) {
                Suggestion suggestion = results.getSuggestions().get(i);
                for (SuggestionToken token : SuggestionExpander.expand(suggestion, this, results)) {
                    if(token instanceof ExpandableSuggestionToken) {
                        SuggestionExplorerItem item = new SuggestionExplorerItem(((ExpandableSuggestionToken) token), explorer);
                        item.setDetailed(true);
                        explorer.addElement(item);
                        activeTokens.add(((ExpandableSuggestionToken) token));
                        if (!anyExpandable) {
                            item.setSelected(true);
                            explorer.setSelected(item, null);
                        }
                        anyExpandable = true;
                    } else if(token instanceof ParameterNameSuggestionToken) {
                        sb.append(((ParameterNameSuggestionToken) token).getParameterName());
                        sb.append(", ");
                    }
                    any = true;
                }
                if(!createdEverywhereSnippets && i == results.getSuggestions().size()-1) {
                    results.getSuggestions().addAll(SnippetManager.createSuggestionsForTag(null));
                    createdEverywhereSnippets = true;
                }
            }
            if(sb.length() > 0) {
                sb.setLength(sb.length()-2);
                parameterLabel.setText(" <" + sb.toString() + ">");
            } else {
                parameterLabel.setText("");
            }
        }

        if(any) {
            Debug.log("Received " + explorer.getTotalCount() + " suggestions");
            //Debug.log(explorer.getChildren().stream().map(ExplorerElement::getToken).collect(Collectors.toList()));
            this.setVisible(true);
            filter();
            int shownTokens = 0;
            for(ExpandableSuggestionToken token : activeTokens) {
                if(token.isEnabled()) shownTokens += 1;
            }
            Debug.log("After filtering: " + shownTokens);
            relocate(Math.min(results.getSuggestionIndex(), editor.getDocument().getLength()));
            editor.requestFocus();
        } else {
            Debug.log("No suggestions received");
            this.setVisible(false);
        }
    }

    public void submit(String text, Suggestion suggestion, boolean dismiss) {
        if(dismiss) {
            this.setVisible(false);
        } else {
            this.forceLocked = true;
        }

        int endIndex = -1;
        if(suggestion instanceof SnippetSuggestion) {
            text = text.replace("\n", "\n" + StringUtil.repeat("    ", editor.getDocumentIndentationAt(editor.getCaretPosition())));
            endIndex = text.indexOf("$END$");
            text = text.replaceFirst("\\$END\\$", "");
        }
        int deletions = StringUtil.getSequenceCount(text, "\b");
        String finalText = text.substring(deletions);

        CompoundEdit edit = new CompoundEdit();
        edit.appendEdit(new Lazy<>(() -> new DeletionEdit(editor, editor.getCaretPosition() - activeResults.getSuggestionIndex() + deletions)));
        edit.appendEdit(new Lazy<>(() -> new InsertionEdit(finalText, editor)));
        editor.getEditManager().insertEdit(edit);
        if(endIndex > -1) {
            editor.getCaret().moveBy(endIndex - finalText.length());
        }

        if(!dismiss) {
            this.forceLocked = false;
            lock();
        }

        if(dismiss) {
            setSafeToSuggest(false);
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        //Debug.log("Pressed");
        if(!this.isVisible() || !anyEnabled) return;
        if(TridentWindow.jframe.getFocusOwner() == null) editor.requestFocus();
        if(e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            this.setVisible(false);
            e.consume();
            return;
        }
        int selectedIndex = explorer.getFirstSelectedIndex();
        if(selectedIndex < 0) selectedIndex = 0;
        if(e.getKeyCode() == KeyEvent.VK_DOWN) {
            selectedIndex++;
            if(selectedIndex >= explorer.getTotalCount()) {
                selectedIndex = 0;
            }
            explorer.setSelectedIndex(selectedIndex);
            e.consume();
        } else if(e.getKeyCode() == KeyEvent.VK_UP) {
            selectedIndex--;
            if(selectedIndex < 0) {
                selectedIndex = explorer.getTotalCount()-1;
            }
            explorer.setSelectedIndex(selectedIndex);
            e.consume();
        } else if(KeyMap.SUGGESTION_SELECT.wasPerformedExact(e)) {
            java.util.List<ModuleToken> tokens = explorer.getSelectedTokens();
            if(!tokens.isEmpty()) {
                tokens.get(0).onInteract();
            }
            e.consume();
        }

        Rectangle rect = explorer.getVisibleRect(selectedIndex);
        rect.y -= scrollPane.getViewport().getViewRect().y;

        scrollPane.getViewport().scrollRectToVisible(rect);
    }

    @Override
    public void keyReleased(KeyEvent e) {

    }

    @Override
    public void dismiss(boolean force) {
        if(isVisible() && !forceLocked) {
            if (force || !locked || (activeResults != null && editor.getCaretWordPosition() != activeResults.getSuggestionIndex() && editor.getSoftCaretWordPosition() != activeResults.getSuggestionIndex())) {
                this.setVisible(false);
            }
        }
        locked = false;
    }

    @Override
    public void setVisible(boolean b) {
        super.setVisible(b);
    }

    private boolean anyEnabled = true;

    public void filter() {
        if(isVisible() && activeResults != null) {
            try {
                int cwpos = activeResults.getSuggestionIndex();
                if(editor.getCaretPosition() < cwpos || editor.getSoftCaretWordPosition() > cwpos || (!activeResults.changedSuggestionIndex() && editor.getCaretWordPosition() > cwpos)) {
                    dismiss(true);
                    return;
                }
                String typed = editor.getDocument().getText(cwpos, editor.getCaretPosition() - cwpos);

                anyEnabled = false;
                for(ExpandableSuggestionToken token : activeTokens) {
                    token.setEnabledFilter(typed);
                    if(token.isEnabled()) anyEnabled = true;
                }

                if(anyEnabled) this.explorer.setForceSelectNext(true);

                explorer.repaint();
                relocate();
            } catch (BadLocationException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void resize() {
        int shownTokens = 0;
        for(ExpandableSuggestionToken token : activeTokens) {
            if(token.isEnabled()) shownTokens += 1;
        }
        this.setSize(new Dimension(400, Math.min(300, explorer.getRowHeight() * shownTokens + 2 + parameterLabel.getPreferredSize().height)));
    }

    private void relocate(int index) {
        resize();
        try {
            Rectangle rect = editor.modelToView(index);
            if(rect == null) return;
            Point loc = rect.getLocation();
            loc.y += rect.height;
            loc.translate(editor.getLocationOnScreen().x, editor.getLocationOnScreen().y);
            if(loc.y + this.getHeight() >= TridentWindow.jframe.getLocationOnScreen().y + TridentWindow.jframe.getHeight()) {
                loc.y -= editor.getLineHeight();
                loc.y -= this.getHeight();
            }
            this.setLocation(loc);
        } catch (BadLocationException x) {
            Debug.log("BadLocationException: " + x.getMessage() + "; index " + x.offsetRequested(), Debug.MessageType.ERROR);
            x.printStackTrace();
        } catch(IllegalComponentStateException ignored) {

        }
    }

    @Override
    public void relocate() {
        if(editor != null && editor.isVisible() && editor.isShowing()) {
            relocate(activeResults != null ? Math.min(activeResults.getSuggestionIndex(), editor.getDocument().getLength()) : editor.getCaretWordPosition());
        }
    }

    @Override
    public void focusGained(FocusEvent e) {

    }

    @Override
    public void focusLost(FocusEvent e) {
        if(e.getOppositeComponent() != null && e.getOppositeComponent() != this && e.getOppositeComponent() != explorer) {
            dismiss(true);
        }
    }

    public void setSummary(SummaryModule summary, boolean matched) {
        this.summary = summary;
        if(lastSuccessfulSummary == null || matched) lastSuccessfulSummary = summary;
    }

    public SummaryModule getSummary() {
        return summary;
    }

    public SummaryModule getLastSuccessfulSummary() {
        return lastSuccessfulSummary;
    }

    @Override
    public void lock() {
        locked = true;
        filter();
    }

    @Override
    public void setSafeToSuggest(boolean safe) {
        //Debug.log("Set safe to suggest: " + safe);
        this.safe = safe;
    }

    private boolean disposed = false;

    @Override
    public void dispose() {
        if(!disposed) {
            super.dispose();
            tlm.dispose();
            explorer.dispose();
        }
        disposed = true;
    }
}
