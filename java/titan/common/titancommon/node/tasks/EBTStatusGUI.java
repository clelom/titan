/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package titancommon.node.tasks;

import java.awt.Color;
import java.awt.Panel;
import titancommon.gui.TitanFrame;
import titancommon.node.DataPacket;
import titancommon.node.TitanTask;
import titancommon.tasks.BTStatusGUI;

/**
 * This is a simple GUI which can be used to display the status of the connections
 * to bluetooth sensors.
 *
 * @author Benedikt Köppel <bkoeppel@ee.ethz.ch>
 * @author Jonas Huber <bkoeppel@ee.ethz.ch>
 */
public class EBTStatusGUI extends BTStatusGUI implements ExecutableTitanTask {

    private TitanTask tTask;
    private TitanFrame stFrame;
    private BTStatusPanel m_panel;
    
    public void setTitanTask(TitanTask tsk) {
        tTask = tsk;
    }

    public boolean setExecParameters(short[] param) {
        return true;
    }

    public void init() {
        System.out.println("Created new ETBStatusGUI->BTStatusFrame()");

        // Create new TitanFrame
        stFrame = new TitanFrame();

        // Registering this as a new frame with the TitanFrame class
        TitanFrame.registerFrame(stFrame);

        // Set the conent of the main panel of this TitanFrame to our
        // acutual GUI which is implemented as a private Panel below.
        m_panel = new BTStatusPanel();
        stFrame.setTitanFramePanel( m_panel );

        // Show GUI
        stFrame.setVisible(true);
       


    }

    public void inDataHandler(int port, DataPacket data) {
        // Note: The node on port 0 is refered to as "Node 1" in the GUI!
        m_panel.setStatus(port, data.sdata[0]);
    }


    /**
     * This class holds the actual GUI to be displayed within the TitanFrame.
     */
    private class BTStatusPanel extends Panel {

        /** Creates new panel. */
        public BTStatusPanel() {
            initComponents();
        }

        /**
         * Sets the text and the color of a status label belonging to the node
         * with nodeNumber according to status.
         *
         * For "protocol" @see titancommon.node.tasks.EBTSensor
         *
         * @todo input validation?
         *
         * @param nodeNumber
         * @param status
         */
        public void setStatus(int nodeNumber, short status) {

            if (nodeNumber > 7 || nodeNumber < 0) {
                System.err.println("[EBTStatusGUI] Received packet from invalid node " + nodeNumber);
                return;
            }

            if (status == -2) {
                states[nodeNumber].setForeground(Color.green);
                states[nodeNumber].setText("Connected");
            } else if (status == -3) {
                states[nodeNumber].setForeground(Color.red);
                states[nodeNumber].setText("Not connected");
            } else if (status == -1) {
                states[nodeNumber].setForeground(Color.orange);
                states[nodeNumber].setText("Connecting...");
            } else {
                // If status is not negative, it is not a status but rather
                // the number of bytes waiting in the input buffer. So let's
                // inform the user about that.
                states[nodeNumber].setText("Connected (" + status + " Bytes)");

                // FIXME
                // REMOVE THIS AGAIN
                // Write buffer size into output.log
                System.out.println("-1\t" + nodeNumber + "\t" + status);
                // REMOVE THIS AGAIN
                // FIXME
            }
        }

        /** This method is called from within the constructor to
         * initialize the form.
         * WARNING: Do NOT modify this code. The content of this method is
         * always regenerated by the Form Editor.
         */
        // <editor-fold defaultstate="collapsed" desc="Generated Code">
        private void initComponents() {

            int i = 0;
            for (i = 0; i < 7; i++) {
                labels[i] = new java.awt.Label();
                states[i] = new java.awt.Label();
            }

            setLayout(new java.awt.GridLayout(7, 2));

            labels[0].setText("Node 1:");
            add(labels[0]);

            add(states[0]);


            labels[1].setText("Node 2:");
            add(labels[1]);

            add(states[1]);


            labels[2].setText("Node 3:");
            add(labels[2]);

            add(states[2]);

            labels[3].setText("Node 4:");
            add(labels[3]);

            add(states[3]);


            labels[4].setText("Node 5:");
            add(labels[4]);

            add(states[4]);


            labels[5].setText("Node 6:");
            add(labels[5]);

            add(states[5]);


            labels[6].setText("Node 7:");
            add(labels[6]);

            add(states[6]);


            /* By default "deactivate" all entries. */
            for (i = 0; i < 7; i++) {
                states[i].setText("Not used.");
                states[i].setForeground(Color.gray);
            }

        }// </editor-fold>

        /** Exit the Application */
        private void exitForm(java.awt.event.WindowEvent evt) {
            //System.exit(0); // TODO: BAD!!!
        }
        // Variables declaration - do not modify
        private java.awt.Label[] labels = new java.awt.Label[7];
        private java.awt.Label[] states = new java.awt.Label[7];
        // End of variables declaration
    };

}
