package titancommon.gui;

import java.awt.Button;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Panel;
import java.util.Vector;

/**
 * Parent class for *all* GUIs to be used within Titan, i.e. all GUIs should
 * be childs of TitanFrame.
 *
 * It enables cycling through all open TitanFrames by placing a "Next" button
 * on top of instances of child classes.
 *
 * A child class instance must implement its actual GUI as a (private) class 
 * which inherits from java.awt.Panel. In the constructor of the child class,
 * the main panel of the TitanFrame must be set to an instance of the actual GUI
 * panel and the instance has to be registered as a TitanFrame using the static
 * function registerTitanFrame().
 *
 * @see titancommon.node.tasks.EBTStatusGUI as an example.
 *
 * @todo Maybe implement a "Prev" button as well.
 * @todo Error reporting, other issues denoted in the code.
 * @todo Issue with Farkle/Dice-GUI being started by titancommon.TitanCommon
 * 
 * @author Benedikt Köppel <bkoeppel@ee.ethz.ch>
 * @author Jonas Huber <huberjo@ee.ethz.ch>
 */
public class TitanFrame extends Frame {

    /* Class variables (!) */
    private static int currentFrame = -1;   // Holds instance which has focus
    private static Vector frames = new Vector();    // Contains the references to the open frames

    /* Instance variables */
    private Button m_nextFocusButton;
    private Panel m_mainPanel;

    /**
     * Registers a new child with the list of open TitanFrames. *Every* subclass
     * has to do this if it wants to be accessible through cycling.
     *
     * @todo Find a way to do this automagically...
     * 
     * @param f
     */
    public static void registerFrame(TitanFrame f) {
        System.out.println("[TitanFrame] Registering frame " + f.getName());
        synchronized (TitanFrame.class) {
            frames.add(f);
            System.out.println(frames.size());
        }

    }

    /*
     * The following are instance methods!
     */
    /**
     * Construcor. Updates the class' informations about open frames.
     */
    public TitanFrame() {
        // Set up our "next"-button
        m_nextFocusButton = new Button("Next");
        add(m_nextFocusButton, java.awt.BorderLayout.NORTH);
        m_nextFocusButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                focusOnNextFrame();
            }
        });

        // Set the size of the frame. JavaME does silently ignore this, so it's ok.
        setSize(320, 480);
    }

    /**
     * Sets the main content of this TitanFrame to the specified panel. This has
     * to be called by *every* child instance if it wants to actually show its GUI.
     *
     * @param Panel gui
     */
    public void setTitanFramePanel(Panel gui) {
        m_mainPanel = gui;
        add(m_mainPanel, java.awt.BorderLayout.CENTER);
        m_mainPanel.setVisible(true);
    }

    /**
     * Brings the next frame to the front.
     */
    private void focusOnNextFrame() {
        synchronized (TitanFrame.class) {
            if (currentFrame < frames.size() - 1) {
                currentFrame = currentFrame + 1;
            } else {
                currentFrame = 0;
            }
            ((TitanFrame) frames.get(currentFrame)).toFront();

            // Todo: remove this message!
            System.out.println("[TitanFrame] Set focus to frame " + currentFrame);
        }
    }
}
