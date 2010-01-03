/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package titancommon.bluetooth;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;

/**
 * The Main class reads in command line arguments and binds together the 
 * different components to run the BluetoothGateway
 *
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 */
public class Main {

   private static HashMap m_BTConnections = new HashMap();
   //private static MainWindow m_mainWindow;
   private static ConfigFileReader m_config;
   
   /**
    * Initializes a Bluetooth connection to the given address and starts a 
    * server.
    * @param address the Bluetooth MAC address of the device
    */
   public static void addBTConnection(String address, String sensorname, String format) {
      
      FrameParser fp = new FrameParser(sensorname,format,m_config.getDescription(format));
      TCPServer tcpserver = new TCPServer();
      fp.addObserver(tcpserver);
      fp.addObserver(new FileDumper());
      BTConnection btc = new BTConnection(address,fp );
      
      new Thread(tcpserver).start();
      new Thread(btc).start();
      
      m_BTConnections.put(btc.getAddress(), btc);
   }

   /**
    * Notifies the control that a bluetooth connection has been terminated
    * @param btc the bluetooth connection that has terminated itself
    */
   public static void removeBTConnection(BTConnection btc) {
      m_BTConnections.remove(btc.getAddress());
      //m_mainWindow.closedBTConnection(btc.getAddress());
   }

   /**
    * Starts up the main window.
    * @param args
    */
   public static void main(String [] args) {

      try {
         System.out.println("Opening config file");
         m_config = new ConfigFileReader("btg_config.txt");
      } catch (FileNotFoundException ex) {
         System.out.println("Main: could not find config file, using default values");
         if (m_config == null) m_config = new ConfigFileReader();
         m_config.addBTDevice("1000E85CDC3F (NT motion sensor 119)");
         m_config.addBTDevice("1000E89D082E (NT motion sensor 120)");
         m_config.addFilter("DX4;cs-s-s-s-s-s-s-s-s-s-s-s");
      } catch (IOException ex) {
         System.out.println("Main: read error in config file: " + ex.getLocalizedMessage());
      }
      
      //m_mainWindow = new MainWindow();
      //m_config.configureMainWindow(m_mainWindow);
      //java.awt.EventQueue.invokeLater(new Runnable() {
      //   public void run() { m_mainWindow.setVisible(true); }
      //});
   }

}
