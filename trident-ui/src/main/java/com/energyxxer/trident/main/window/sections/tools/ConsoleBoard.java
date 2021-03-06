package com.energyxxer.trident.main.window.sections.tools;

import com.energyxxer.trident.main.window.TridentWindow;
import com.energyxxer.trident.ui.modules.FileModuleToken;
import com.energyxxer.trident.ui.scrollbar.OverlayScrollBarUI;
import com.energyxxer.trident.ui.scrollbar.OverlayScrollPaneLayout;
import com.energyxxer.trident.ui.styledcomponents.StyledLabel;
import com.energyxxer.trident.ui.styledcomponents.StyledTextField;
import com.energyxxer.trident.ui.theme.change.ThemeListenerManager;
import com.energyxxer.util.logger.Debug;
import com.energyxxer.util.out.ConsoleOutputStream;

import javax.swing.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by User on 12/15/2016.
 */
public class ConsoleBoard extends ToolBoard {

    private static final int CONSOLE_HEIGHT = 300;
    private final StyledTextField inputField;

    private ThemeListenerManager tlm = new ThemeListenerManager();

    private ArrayList<String> commandHistory = new ArrayList<>();
    private int selectedCommand = -1;
    private String writingCommand = null;

    private static HashMap<String, CommandHandler> commandHandlers = new HashMap<>();

    public ConsoleBoard(ToolBoardMaster parent) {
        super(parent);
        this.setLayout(new BorderLayout());
        this.setPreferredSize(new Dimension(0, CONSOLE_HEIGHT));

        JTextPane console = new JTextPane();
        console.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
        tlm.addThemeChangeListener(t -> {
            console.setBackground(t.getColor(Color.WHITE, "Console.background"));
            console.setSelectionColor(t.getColor(new Color(50, 100, 175), "Console.selection.background","General.textfield.selection.background"));
            console.setSelectedTextColor(t.getColor(Color.BLACK, "Console.selection.foreground","General.textfield.selection.foreground","Console.foreground","General.foreground"));
            console.setFont(new Font(t.getString("Console.font","Editor.font","default:monospaced"), 0, 12));
            console.setForeground(t.getColor(Color.BLACK, "Console.foreground"));

            if(console.getStyle("warning") != null) console.removeStyle("warning");
            if(console.getStyle("error") != null) console.removeStyle("error");

            Style warningStyle = console.addStyle("warning", null);
            StyleConstants.setForeground(warningStyle, t.getColor(new Color(255, 140, 0), "Console.warning"));

            Style errorStyle = console.addStyle("error", null);
            StyleConstants.setForeground(errorStyle, t.getColor(new Color(200,50,50), "Console.error"));

            Style debugStyle = console.addStyle("debug", null);
            StyleConstants.setForeground(debugStyle, new Color(104,151,187));
        });
        /*clear.addActionListener(e -> {
            try {
                console.getDocument().remove(0,console.getDocument().getLength());
            } catch(BadLocationException x) {}
        });*/
        console.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                AttributeSet hyperlink = console.getStyledDocument().getCharacterElement(console.viewToModel(e.getPoint())).getAttributes();
                if(hyperlink.containsAttribute("IS_HYPERLINK",true)) {
                    String path = (String) hyperlink.getAttribute("PATH");
                    int location = Integer.parseInt((String) hyperlink.getAttribute("LOCATION"));
                    int length = Integer.parseInt((String) hyperlink.getAttribute("LENGTH"));

                    TridentWindow.tabManager.openTab(new FileModuleToken(new File(path)), location, length);
                }
            }
        });
        console.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                AttributeSet hyperlink = console.getStyledDocument().getCharacterElement(console.viewToModel(e.getPoint())).getAttributes();

                console.setCursor(Cursor.getPredefinedCursor((hyperlink.containsAttribute("IS_HYPERLINK",true)) ? Cursor.HAND_CURSOR : Cursor.TEXT_CURSOR));
            }
        });
        console.setEditable(false);
        console.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        //console.setEditorKit(JEditorPane.createEditorKitForContentType("text/html"));

        //tlm.addThemeChangeListener(t -> textConsoleOut.update());

        Debug.addStream(new ConsoleOutputStream(console));
        /*Console.addInfoStream(new ConsoleOutputStream(console));
        Console.addWarnStream(new ConsoleOutputStream(console,"warning"));
        Console.addErrStream(new ConsoleOutputStream(console,"error"));
        Console.addDebugStream(new ConsoleOutputStream(console,"debug"));*/

        //consoleOut = new PrintStream(textConsoleOut);
        //System.setOut(new PrintStream(new MultiOutputStream(consoleOut, System.out)));
        //System.setErr(new PrintStream(new MultiOutputStream(consoleOut, System.err)));

        JPanel consoleWrapper = new JPanel(new BorderLayout());
        consoleWrapper.add(console);
        JScrollPane consoleScrollPane = new JScrollPane(consoleWrapper);
        consoleScrollPane.getVerticalScrollBar().setUnitIncrement(17);
        consoleScrollPane.getHorizontalScrollBar().setUnitIncrement(17);

        consoleScrollPane.setLayout(new OverlayScrollPaneLayout(consoleScrollPane, tlm));

        consoleScrollPane.getVerticalScrollBar().setUI(new OverlayScrollBarUI(consoleScrollPane, tlm));
        consoleScrollPane.getHorizontalScrollBar().setUI(new OverlayScrollBarUI(consoleScrollPane, tlm));
        consoleScrollPane.getVerticalScrollBar().setOpaque(false);
        consoleScrollPane.getHorizontalScrollBar().setOpaque(false);


        this.add(consoleScrollPane, BorderLayout.CENTER);
        JPanel fieldPane = new JPanel(new BorderLayout());
        this.add(fieldPane, BorderLayout.SOUTH);
        StyledLabel fieldLabel;
        fieldPane.add(fieldLabel = new StyledLabel("",tlm), BorderLayout.WEST);
        fieldLabel.setIconName("console_input");
        fieldPane.add(inputField = new StyledTextField("", "Console", tlm));
        inputField.setPreferredSize(new Dimension(1, 24));

        tlm.addThemeChangeListener(t -> {
            fieldPane.setBackground(inputField.getBackground());
            consoleScrollPane.setBackground(console.getBackground());
            consoleScrollPane.setBorder(BorderFactory.createMatteBorder(Math.max(t.getInteger("Console.header.border.thickness"),0), 0, 0, 0, t.getColor(new Color(200, 200, 200), "Console.header.border.color")));
        });

        inputField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if(e.getModifiers() != 0) return;
                if(e.getKeyCode() == KeyEvent.VK_ENTER) {
                    e.consume();
                    String command = inputField.getText();
                    if(!command.isEmpty()) {
                        addToHistory(command);
                        runCommand(command);
                    }
                    inputField.setText("");
                } else if(e.getKeyCode() == KeyEvent.VK_UP) {
                    getPreviousCommand();
                } else if(e.getKeyCode() == KeyEvent.VK_DOWN) {
                    getNextCommand();
                }
            }
        });

        registerCommandHandler("clear", new CommandHandler() {
            @Override
            public String getDescription() {
                return "Clears the console";
            }

            @Override
            public void printHelp() {
                Debug.log();
                Debug.log("CLEAR: Clears the console");
            }

            @Override
            public void handle(String[] args) {
                console.setText("");
            }
        });
        registerCommandHandler("help", new CommandHandler() {
            @Override
            public String getDescription() {
                return "Shows this message";
            }

            @Override
            public void printHelp() {
                Debug.log();
                Debug.log("HELP: Shows a list of all commands");
            }

            @Override
            public void handle(String[] args) {
                if(args.length <= 1) {
                    Debug.log();
                    Debug.log("Available Commands:");

                    for(Map.Entry<String, CommandHandler> entry : commandHandlers.entrySet()) {
                        Debug.log("  > " + entry.getKey() + ": " + entry.getValue().getDescription());
                    }
                } else {
                    if(commandHandlers.containsKey(args[1])) {
                        commandHandlers.get(args[1]).printHelp();
                    } else {
                        Debug.log("Unknown command '" + args[1] + "'");
                    }
                }
            }
        });
    }

    private void addToHistory(String command) {
        if(commandHistory.isEmpty() || !command.equals(commandHistory.get(0))) {
            commandHistory.add(0, command);
        }
        selectedCommand = -1;
        writingCommand = "";
    }

    private void getPreviousCommand() {
        if(selectedCommand == -1) {
            writingCommand = inputField.getText();
        }
        if(selectedCommand+1 < commandHistory.size()) {
            selectedCommand++;
            inputField.setText(commandHistory.get(selectedCommand));
        }
    }

    private void getNextCommand() {
        if(selectedCommand-1 >= -1 && selectedCommand-1 < commandHistory.size()) {
            selectedCommand--;
            if(selectedCommand > -1) {
                inputField.setText(commandHistory.get(selectedCommand));
            } else {
                inputField.setText(writingCommand);
            }
        }
    }

    @Override
    public String getName() {
        return "Console";
    }

    @Override
    public String getIconName() {
        return "console";
    }

    private void runCommand(String command) {
        String[] args = command.split(" ", -1);
        if(commandHandlers.containsKey(args[0])) {
            commandHandlers.get(args[0]).handle(args);
        } else {
            Debug.log("Unknown command '" + args[0] + "'");
        }
    }

    public static void registerCommandHandler(String commandName, CommandHandler handler) {
        commandHandlers.put(commandName, handler);
    }

    public interface CommandHandler {
        String getDescription();
        void printHelp();
        void handle(String[] args);
    }
}
