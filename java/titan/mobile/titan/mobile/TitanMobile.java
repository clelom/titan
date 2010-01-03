/*
    This file is part of Titan.

    Titan is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as 
    published by the Free Software Foundation, either version 3 of 
    the License, or (at your option) any later version.

    Titan is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Titan. If not, see <http://www.gnu.org/licenses/>.
*/

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Mirco Rossi <mrossi@ife.ee.ethz.ch>
 */
package titan.mobile;

import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Hashtable;

import javax.bluetooth.RemoteDevice;


import titancommon.TitanCommand;

public class TitanMobile {
    /**
     * the unique instance of the Midlet as a Singleton pattern
     */
     public static TitanMobile singleton;

    /**
     * Path Definitions: 
     *          APP_PATH: Path of the application 
     *          CFG_PATH: Path of the config files
     *          PIC_PATH: Path with pics for the GUI   
     */
    public  static String APP_PATH;
    public  static String  CFG_PATH = "data/cfg/";
    public  static String  PIC_PATH = "data/pics/";
    //THE GUI
    private MobileGUI m_MobileGUI;
    // Worker Threads in order to make the background tasks
    private Thread btDeviceDiscoveryThread;
    private Thread btServiceDiscoveryThread;
    private Thread TitanInpThread;
    // bluetooth discovery stuff
    private boolean m_btStop = false;
    // Titan stuff
    //private int m_iBTChoice = -1;
    public boolean m_bTitanRun = false;
    /** found bluetoothdevices */
    public Hashtable m_btDevices = new Hashtable(4);
    private Hashtable m_btCOMServices = new Hashtable(3);
    private TitanBluetoothCom m_BlueCom;
    public TitanCommand m_TitanCommand;
     /*
     * PrintStream is created to reroute System.out and System.err to 
     * the SystemOutputDispacher.
     */
    PrintStream m_sysOut;
    //System.out Observable
    SystemOutputObservable m_systemOutputObserverable;
    
    public TitanMobile() {
        // instanciation of the singleton
        singleton = this;

        //Reroute System.out
        m_systemOutputObserverable=new SystemOutputObservable();
        m_sysOut = new PrintStream(new SystemOutputDispacher(new ByteArrayOutputStream(),m_systemOutputObserverable));
        System.setOut(m_sysOut);
        System.setErr(m_sysOut);

        // instanciation of the TitanBluetoothCom
        m_BlueCom = new TitanBluetoothCom();
        m_MobileGUI = new MobileGUI();
        m_systemOutputObserverable.addObserver(m_MobileGUI);
        m_MobileGUI.setVisible(true);
        m_TitanCommand = new TitanCommand();
  
        APP_PATH = System.getProperty("user.dir");
        if (!APP_PATH.endsWith("\\")) {
            APP_PATH = APP_PATH + "\\";
        }
        CFG_PATH=APP_PATH+CFG_PATH;
        PIC_PATH=APP_PATH+PIC_PATH;

    }

    public static void execTitanCommand(String command) {
        command = command.trim();
        
        if (command.startsWith("titan")) {
            int counter = 0;
            boolean escaped = false;
            for (int i = 0; i < command.length(); i++) {
                
                // toggle escape
                escaped = (command.charAt(i) == '"') ? !escaped : escaped;
                
                counter += ((escaped == false) && (command.charAt(i) == ' ')) ? 1 : 0;
            }
            String[] com = new String[counter + 1];

            int index = 0;
            for (int i = 0; i < com.length; i++) {
                
                escaped = false;
                com[i] = new String();
                for ( ; index < command.length(); index++ ) {
                    escaped = (command.charAt(index) == '"') ? !escaped : escaped;
                    
                    if ( !escaped && command.charAt(index) == ' ' ) {
                        index++;
                        break;
                    }
                    if ( command.charAt(index) != '"' ) com[i] += command.charAt(index);
                }

                if (index >= command.length()) break;

            }
            
            if ( TitanMobile.singleton.m_TitanCommand == null) {
               System.err.println("ERROR: execTitanCommand: TitanMobile has no TitanCommand!");
               TitanMobile.singleton.m_TitanCommand = new TitanCommand();
            } else TitanMobile.singleton.m_TitanCommand.execute(com);
        } // zu testzwecken
        else {
            try {
                getBlueCom().send(command);
            } catch (IOException e) {
                getGUI().setDebugText(TitanMobile.singleton.getClass().getName(), "execTitanCommand:send", e.toString());
            }
        }
    }

    public static MobileGUI getGUI() {
        return TitanMobile.singleton.m_MobileGUI;
    }

    public static TitanCommand getTitanCommand() {
        return TitanMobile.singleton.m_TitanCommand;
    }

    public static boolean getBTStop() {
        return TitanMobile.singleton.m_btStop;
    }

   /* public static void setBTChoice(int i) {
        TitanMobile.singleton.m_iBTChoice = i;
    }*/

    public static String getBTChoiceAddress(int num) {
                
        
        String name="";
        RemoteDevice rd = null;
        boolean nameAvailable=true;
        try{
        rd = (RemoteDevice) TitanMobile.singleton.m_btDevices.get(new Integer(num));
        }catch(Exception e){  
      
        }

        try {
            name = rd.getFriendlyName(false);

        }catch(NullPointerException e) {
                   //TitanMobile.singleton.getGUI().setDebugText(TitanMobile.singleton.getClass().getName(), "getBTChoiceAddress:m_iBTChoice="+ Integer.toBinaryString(TitanMobile.singleton.m_iBTChoice) +";HashSize: "+Integer.toString(TitanMobile.singleton.m_btDevices.size()), e.toString());
        }catch (Exception e) {
            nameAvailable = false;
       
        }

        if (!nameAvailable || name.trim().equals("")) {
            name = rd.getBluetoothAddress();
        }
        
        return name;
        
    }

    public static void putBTCOMService(int key, String value) {
        TitanMobile.singleton.m_btCOMServices.put(new Integer(key), value);
    }

    public static void btConnectionLost() {
        TitanMobile.singleton.m_bTitanRun = false;
        getGUI().Alert("Bluetooth connection lost!");
        try {
            getBlueCom().close();
        } catch (IOException e) {
            getGUI().setDebugText(TitanMobile.singleton.getClass().getName(), "btConnectionLost:close", e.toString());
        }
    }

    public static void stopThread() throws IOException {
        if (TitanMobile.singleton.m_bTitanRun) {
            TitanMobile.singleton.m_bTitanRun = false;

            TitanMobile.singleton.m_BlueCom.close();
        } else if (!TitanMobile.singleton.m_btStop) {
            TitanMobile.singleton.m_btStop = true;
        }
    }

    public static void startTitan(String url)throws IOException {
               
        boolean isConnected=TitanMobile.singleton.m_BlueCom.connect(url);

        
        if(isConnected)
            System.out.println("Connection to "+ url +" established");
        else
            System.out.println("Connection to "+ url +" failed");
        
        TitanMobile.singleton.m_bTitanRun = true;

        TitanMobile.singleton.TitanInpThread = new Thread(new TitanComThread());
        TitanMobile.singleton.TitanInpThread.start();
        
        
    }
    
    
    public static void startTitan(int num) throws IOException {
        boolean isConnected=TitanMobile.singleton.m_BlueCom.connect((String) TitanMobile.singleton.m_btCOMServices.get(new Integer(num)));
        
        FileWriter WriteResult = new FileWriter("BT.txt");
        PrintWriter ResultOutput = new PrintWriter(WriteResult,true);
        //print text to file
        ResultOutput.println((String) TitanMobile.singleton.m_btCOMServices.get(new Integer(num)));
        
        if(isConnected)
            System.out.println("Connection to "+ getBTChoiceAddress(num) +" established");
        else
            System.out.println("Connection to "+ getBTChoiceAddress(num) +" failed");
        
        TitanMobile.singleton.m_bTitanRun = true;

        TitanMobile.singleton.TitanInpThread = new Thread(new TitanComThread());
        TitanMobile.singleton.TitanInpThread.start();
    }

    public static void startServiceDiscovery(int num) {

        TitanMobile.singleton.m_btStop = false;

        TitanMobile.singleton.m_btCOMServices.clear();

        //RemoteDevice rd = (RemoteDevice) TitanMobile.singleton.m_btDevices.get(new Integer(TitanMobile.singleton.m_iBTChoice));

        RemoteDevice rd = (RemoteDevice) TitanMobile.singleton.m_btDevices.get(new Integer(num));
        
        
        TitanMobile.singleton.btServiceDiscoveryThread = new Thread(new BluetoothServiceDiscoveryThread(rd));
        TitanMobile.singleton.btServiceDiscoveryThread.start();
    }

    public static void startDeviceDiscovery() {
        TitanMobile.singleton.m_btStop = false;

        TitanMobile.singleton.m_btDevices.clear();

        // Start a worker thread to make a device discovery
        TitanMobile.singleton.btDeviceDiscoveryThread = new Thread(new BluetoothDeviceDiscoveryThread());
        TitanMobile.singleton.btDeviceDiscoveryThread.start();
    }

    public static TitanBluetoothCom getBlueCom() {
        return TitanMobile.singleton.m_BlueCom;
    }

    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new  

              Runnable() {

                 
            
        
    

public void run() {
                new TitanMobile();
            }
        });
    }
    
}
