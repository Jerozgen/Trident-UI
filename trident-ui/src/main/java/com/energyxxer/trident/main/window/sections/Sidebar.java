package com.energyxxer.trident.main.window.sections;

import com.energyxxer.trident.global.Preferences;
import com.energyxxer.trident.main.window.TridentWindow;
import com.energyxxer.trident.main.window.actions.ActionManager;
import com.energyxxer.trident.ui.ToolbarButton;
import com.energyxxer.trident.ui.explorer.ProjectExplorerMaster;
import com.energyxxer.trident.ui.scrollbar.OverlayScrollPaneLayout;
import com.energyxxer.trident.ui.styledcomponents.Padding;
import com.energyxxer.trident.ui.styledcomponents.StyledLabel;
import com.energyxxer.trident.ui.styledcomponents.StyledMenuItem;
import com.energyxxer.trident.ui.styledcomponents.StyledPopupMenu;
import com.energyxxer.trident.ui.theme.change.ThemeListenerManager;
import com.energyxxer.xswing.ComponentResizer;
import com.energyxxer.xswing.OverlayBorderPanel;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Created by User on 12/15/2016.
 */
public class Sidebar extends OverlayBorderPanel {

    private ThemeListenerManager tlm = new ThemeListenerManager();

    private JPanel expanded = new JPanel(new BorderLayout());
    private JPanel collapsed = new JPanel(new BorderLayout());

    public Sidebar() {
        super(new BorderLayout(), new Insets(0, 0, 0, ComponentResizer.DIST));
        this.setOpaque(false);
    }

    {
        expanded.setPreferredSize(new Dimension(350, 5));
        expanded.setMinimumSize(new Dimension(50, 0));
        expanded.setMaximumSize(new Dimension(700, 0));
        tlm.addThemeChangeListener(t -> {
            expanded.setBackground(t.getColor(Color.WHITE, "Explorer.background"));
            expanded.setBorder(new CompoundBorder(BorderFactory.createEmptyBorder(0, 0, 0, ComponentResizer.DIST), BorderFactory.createMatteBorder(0, 0, 0, Math.max(t.getInteger(1, "Explorer.border.thickness"), 0), t.getColor(new Color(200, 200, 200), "Explorer.border.color"))));
            expanded.setOpaque(false);
        });

        JPanel header = new JPanel(new BorderLayout());

        StyledLabel label = new StyledLabel("Project Explorer", "Explorer.header", tlm);
        label.setFontSize(14);
        label.setPreferredSize(new Dimension(500, 25));
        header.add(new Padding(15, tlm, "Explorer.header.indent"), BorderLayout.WEST);
        header.add(label, BorderLayout.CENTER);

        tlm.addThemeChangeListener(t -> {
            header.setBackground(t.getColor(this.getBackground(), "Explorer.header.background"));
            header.setPreferredSize(new Dimension(500, t.getInteger(25, "Explorer.header.height")));
            label.setPreferredSize(new Dimension(500, t.getInteger(25, "Explorer.header.height")));
        });

        JPanel buttonPanel = new JPanel(new GridBagLayout());
        buttonPanel.setOpaque(false);

        //((FlowLayout) buttonPanel.getLayout()).setHgap(2);

        header.add(buttonPanel, BorderLayout.EAST);

        {
            ToolbarButton refresh = new ToolbarButton("reload", tlm);
            refresh.setHintText("Refresh Explorer");

            refresh.addActionListener(e -> ActionManager.getAction("RELOAD_WORKSPACE").perform());

            buttonPanel.add(refresh);
        }

        {
            ToolbarButton configure = new ToolbarButton("cog_dropdown", tlm);
            configure.setHintText("Configure");

            configure.addActionListener(e -> {
                StyledPopupMenu menu = new StyledPopupMenu("What is supposed to go here?");/*

                {
                    StyledMenuItem item = new StyledMenuItem("Flatten Empty Packages", "checkmark");
                    item.setIconName(TridentWindow.projectExplorer.getFlag(ProjectExplorerMaster.FLATTEN_EMPTY_PACKAGES) ? "checkmark" : "blank");

                    item.addActionListener(aa -> {
                        TridentWindow.projectExplorer.toggleFlag(ProjectExplorerMaster.FLATTEN_EMPTY_PACKAGES);
                        TridentWindow.projectExplorer.refresh();

                        Preferences.put("explorer.flatten_empty_packages",Boolean.toString(TridentWindow.projectExplorer.getFlag(ProjectExplorerMaster.FLATTEN_EMPTY_PACKAGES)));
                    });

                    menu.add(item);
                }*/

                {
                    StyledMenuItem item = new StyledMenuItem("Show Project Files", "checkmark");
                    item.setIconName(TridentWindow.projectExplorer.getFlag(ProjectExplorerMaster.SHOW_PROJECT_FILES) ? "checkmark" : "blank");

                    item.addActionListener(aa -> {
                        TridentWindow.projectExplorer.toggleFlag(ProjectExplorerMaster.SHOW_PROJECT_FILES);
                        TridentWindow.projectExplorer.refresh();

                        Preferences.put("explorer.show_project_files",Boolean.toString(TridentWindow.projectExplorer.getFlag(ProjectExplorerMaster.SHOW_PROJECT_FILES)));
                    });
                    menu.add(item);
                }

                /*{
                    StyledMenuItem item = new StyledMenuItem("Debug Width", "checkmark");
                    item.setIconName(TridentWindow.projectExplorer.getFlag(ExplorerFlag.DEBUG_WIDTH) ? "checkmark" : "blank");

                    item.addActionListener(aa -> {
                        TridentWindow.projectExplorer.toggleFlag(ExplorerFlag.DEBUG_WIDTH);

                        Preferences.put("explorer.debug_width",Boolean.toString(TridentWindow.projectExplorer.getFlag(ExplorerFlag.DEBUG_WIDTH)));
                    });
                    menu.add(item);
                }*/

                menu.show(configure, configure.getWidth()/2, configure.getHeight());

                TridentWindow.projectExplorer.refresh();
            });

            buttonPanel.add(configure);
        }

        {
            ToolbarButton collapse = new ToolbarButton("arrow_left", tlm);
            collapse.setHintText("Collapse Explorer");

            collapse.addActionListener(e -> collapse());

            buttonPanel.add(collapse);
        }

        expanded.add(header, BorderLayout.NORTH);

        JScrollPane sp = new JScrollPane(TridentWindow.projectExplorer = new ProjectExplorerMaster());
        sp.setBorder(new EmptyBorder(0, 0, 0, 0));
        sp.setLayout(new OverlayScrollPaneLayout(sp, tlm));

        expanded.add(sp, BorderLayout.CENTER);
    }
    {

        collapsed.setPreferredSize(new Dimension(29, 50));
        tlm.addThemeChangeListener(t -> {
            collapsed.setBackground(t.getColor(Color.WHITE, "Explorer.background"));
            collapsed.setBorder(BorderFactory.createMatteBorder(0, 0, 0, Math.max(t.getInteger(1, "Explorer.border.thickness"), 0), t.getColor(new Color(200, 200, 200), "Explorer.border.color")));
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setOpaque(false);

        collapsed.add(buttonPanel, BorderLayout.NORTH);

        {
            ToolbarButton expand = new ToolbarButton("arrow_right", tlm);
            expand.setHintText("Expand Explorer");

            expand.addActionListener(e -> expand());

            buttonPanel.add(expand);
        }

        if(Preferences.get("explorer.expanded", "true").equals("true")) {
            expand();
        } else {
            collapse();
        }

        ComponentResizer sidebarResizer = new ComponentResizer(expanded);
        sidebarResizer.setResizable(false, false, false, true);
    }

    public void expand() {
        this.removeAll();
        this.add(expanded, BorderLayout.CENTER);
        update();

        Preferences.put("explorer.expanded", "true");
    }

    public void collapse() {
        this.removeAll();
        this.add(collapsed, BorderLayout.CENTER);
        update();

        Preferences.put("explorer.expanded", "false");
    }

    private void update() {
        this.revalidate();
        this.repaint();

        if(TridentWindow.welcomePane != null) {
            TridentWindow.welcomePane.revalidate();
            TridentWindow.welcomePane.repaint();
        }
    }
}
