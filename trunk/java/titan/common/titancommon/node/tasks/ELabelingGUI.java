package titancommon.node.tasks;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import titancommon.gui.TitanFrame;
import titancommon.node.DataPacket;
import titancommon.node.TitanTask;
import titancommon.tasks.LabelingGUI;
import java.awt.Panel;
import java.awt.ScrollPane;
import java.awt.TextField;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;
import titancommon.util.StringUtil;
import java.util.StringTokenizer;


/**
 * Provides a GUI which allows for up to 8 differen labels to be assigned to
 * an activity.
 * TODO: not more than 8 labels?
 *
 * @todo: Make sure, all labels are deactivated before termination of app
 * @todo: Tidy up the code
 * @todo: Use intelligent file naming like the filewriter.
 * 
 * @author Benedikt Köppel <bkoeppel@ee.ethz.ch>
 * @author Jonas Huber <bkoeppel@ee.ethz.ch>
 */
public class ELabelingGUI extends LabelingGUI implements ExecutableTitanTask {

    private TitanTask tTask;
    private TitanFrame lblFrame;
    private LabelingPanel m_panel;
    private Vector labels = new Vector();
    private BufferedWriter fout;
    private String m_configfile;


    public void setTitanTask(TitanTask tsk) {
        tTask = tsk;
    }


    public boolean setExecParameters(short[] param) {

        // Get the config file as string
        m_configfile = StringUtil.toString(param);

        return true;
    }


    /**
     * This represents a label as it is found in the config file.
     */
    private class Label {

        public String name;
        public short id;
    }


    /**
     * Tries to read labels from the config file whose name is stored in
     * m_configfile.
     *
     * Note: This method follows the GIGO principle (garbagie in, garbage out),
     * i.e. only basic parsing is done and the config file is expected to be
     * more or less correct.
     *
     * @todo make it possible to specifiy also id
     * 
     * @return false in case of any error (e.g. no config file found), true else
     */
    private boolean parseConfig() {
        BufferedReader input;
        String line = null;
        StringTokenizer st;
        Label temp;

        try {
            input = new BufferedReader(new FileReader(m_configfile));


            // Iterating throug all lines of the config file.
            while ((line = input.readLine()) != null) {

                line = line.trim();

                // Is comment?
                if (line.charAt(0) == '#') {
                    continue;
                }

                // Parse the line
                st = new StringTokenizer(line, "\t");
                if (st.countTokens() < 2) {
                    System.err.println("[ELabelingGUI] Check the label config file's syntax!");
                    return false;
                }

                // Create new label and append it
                temp = new Label();
                temp.name = st.nextToken();
                temp.id = Short.parseShort(st.nextToken());

                labels.add(temp);
            }

            input.close();
        } catch (IOException e) {
            return false;
        }

        return true;

    }


    public void init() {

        // Get the configuration
        if (!parseConfig()) {
            System.err.println("[ELabelinGUI] Could not read config file!");
        }

        // We take the current date and time as filename
        Date now = new Date();
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd-HHmmss");
        String f = dateFormat.format(now) + "_LABELS.txt";
        try {
            if (!createFile(f)) {
                System.err.println("[ELabelingGUI] Could not create file!");
            }

            // Open an output stream
            fout = new BufferedWriter(new FileWriter(f));

        } catch (IOException e) {
            System.err.print("[ELabelingGUI] Received IO exception while creating file!");
        }

        // Start GUI
        // Create new TitanFrame
        lblFrame = new TitanFrame();

        // Registering this as a new frame with the TitanFrame class
        TitanFrame.registerFrame(lblFrame);

        // Set the conent of the main panel of the TitanFrame to our
        // acutual GUI which is implemented as a private Panel below.
        m_panel = new LabelingPanel();
        lblFrame.setTitanFramePanel(m_panel);

        // Show GUI
        lblFrame.setVisible(true);

        /*
         * Attach the thread which shall close the file properly to the
         * Runtime.addShutdownHook.
         */
        Runtime.getRuntime().addShutdownHook(new FileFlushThread());
    }


    /**
     * Creates a file using the content of the variable 'filename' as filename.
     *
     * @return  true in case of success, false else.
     * @throws IOException
     */
    private boolean createFile(String filename) throws IOException {
        File file = new File(filename);
        boolean ok = file.createNewFile();
        return ok;
    }


    public void inDataHandler(int port, DataPacket data) {
    }



    /**
     * This is the panel with the actual content.
     */
    private class LabelingPanel extends Panel {

        private int numOfLabels;    // number of labels
        private ScrollPane scrollButtonArea;
        private Panel buttonPanel;
        private Panel commPanel;
        private Button commButton;
        private TextField commText;
        // We need a button for each label we've got from the config file
        private Button[] buttons;

        public LabelingPanel() {
            numOfLabels = labels.size();
            buttons = new java.awt.Button[numOfLabels];
            initComponents();
        }

        private void initComponents() {

            scrollButtonArea = new ScrollPane();
            buttonPanel = new Panel();
            commPanel = new Panel();

            setLayout(new BorderLayout());

            // Sets the three main areas.
            add(scrollButtonArea, BorderLayout.CENTER);
            scrollButtonArea.add(buttonPanel);
            add(commPanel, BorderLayout.SOUTH);

            // Fill the comment-Panel
            commPanel.setLayout(new BorderLayout());
            commButton = new Button("Write");
            commButton.addActionListener(new CommentWriter());

            commText = new TextField();
            commPanel.add(commText, BorderLayout.CENTER);
            commPanel.add(commButton, BorderLayout.EAST);

            // Add the buttons to the buttonPanel.
            // Layout: We want tow columns and an appropriat number of rows
            // to accomodate buttons for each label
            int rows = (int) labels.size() / 2 + 1;
            buttonPanel.setLayout(new java.awt.GridLayout(rows, 2));


            // Initialize Button objects.
            for (int i = 0; i < numOfLabels; i++) {
                buttons[i] = new java.awt.Button();

                buttons[i].setLabel(((Label) labels.elementAt(i)).name);
                buttonPanel.add(buttons[i]);
                buttons[i].addActionListener(new ToggleLabel(i));
            }

        }

        /** Exit the Application */
        void exitForm(java.awt.event.WindowEvent evt) {
            //System.exit(0); // TODO: BAD!!!
            }
    }

    /**
     * This represents a toggleable label. If it is activated, the timestamp
     * of this event is stored and if it s deactivated again, a line is
     * added to the output file.
     */
    private class ToggleLabel implements java.awt.event.ActionListener {

        private int m_id = 0;
        private String m_label;
        private boolean m_active;
        private long m_startTime;

        public ToggleLabel(int id) {
            m_id = id;
        }

        /**
         * Handles any event on the button belonging to this ToggleLabel.
         * @param evt
         */
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            short[] out = new short[2];

            synchronized (this) {
                if (m_active) {
                    // Deactivate label
                    m_panel.buttons[m_id].setForeground(Color.black);
                    m_active = false;
                    long endTime = System.currentTimeMillis();

                    // Write label to file
                    try {
                        fout.write(m_startTime + "\t" + endTime + "\t" + ((Label) labels.elementAt(m_id)).id);
                        fout.newLine();
                        fout.flush();
                    } catch (IOException e) {
                        System.err.println("[ELabelingGUI] Received IOException while writing label!");
                    }

                    // Send label
                    out[0] = ((Label) labels.elementAt(m_id)).id;
                    out[1] = 0;
                    DataPacket pkg = new DataPacket(out);
                    pkg.setTimestamp(endTime);
                    tTask.send(0, pkg);

                    System.out.println("[ELabelingGUI] Deactivated label " + ((Label) labels.elementAt(m_id)).name);
                } else {
                    // Activate label
                    m_startTime = System.currentTimeMillis();
                    m_panel.buttons[m_id].setForeground(Color.red);
                    m_active = true;

                    // Write starting time to file as a comment
                    try {
                        fout.write("-1\t" + m_startTime + "\t" + ((Label) labels.elementAt(m_id)).id);
                        fout.newLine();
                        fout.flush();
                    } catch (IOException e) {
                        System.err.println("[ELabelingGUI] Received IOException while writing label!");
                    }

                    // Send label
                    out[0] = ((Label) labels.elementAt(m_id)).id;
                    out[1] = 1;
                    DataPacket pkg = new DataPacket(out);
                    pkg.setTimestamp(m_startTime);
                    tTask.send(0, pkg);

                    System.out.println("[ELabelingGUI] Activated label " + ((Label) labels.elementAt(m_id)).name);
                }
            }
        }
    }

    /**
     * Implementes an ActionListener which writes the content of commText
     * to the output file upon his actionPerformed() method being called.
     */
    private class CommentWriter implements java.awt.event.ActionListener {

        private boolean blue = false;

        public void CommenWriter() {
        }

        /**
         * If the button for adding a comment to the file is pressed, this
         * is executed and we do as requested.
         *
         * @param evt
         */
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            try {
                fout.write("-2\t" + System.currentTimeMillis() + "\t" + m_panel.commText.getText());
                fout.newLine();
            } catch (IOException e) {
                System.err.println("[ELabelingGUI] Received IOException while writing comment!");
            }

            // Change the color of the button's text to give feedback to the user.
            synchronized (this) {
                if (blue) {
                    m_panel.commButton.setForeground(Color.black);
                    blue = false;
                } else {
                    m_panel.commButton.setForeground(Color.blue);
                    blue = true;
                }
            }

        }
    }

    /**
     * This implements a thread whose only purpose is to properly close the open
     * file and ensure minimal to no data losses at unexpected shutdowns.
     *
     * It is kind of a workaround which is necessary because the finalize() method
     * was never called.
     */
    private class FileFlushThread extends Thread {

        public void run() {
            try {
                System.out.println("[ELabelingGUI] Closing file...");
                fout.flush();
                fout.close();
                System.out.println("[ELabelingGUI] Done!");
            } catch (IOException e) {
                System.err.println("[ELabelingGUI] Could *not* close file: " + e.getMessage());
            }
        }
    }
}
