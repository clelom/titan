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

/**
 * Represents the direct interface to TOSSIM. It maps TOSSIM commands to a 
 * Java interface and reports DEBUG messages 
 * 
 * @author Urs Hunkeler <urs.hunkeler@epfl.ch>
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 */

package titan.simulation;

import java.io.*;
import java.util.*;

import titan.LocalSettings;


public class Tossim {
    private static int ID = 0;
    
    private InputStreamReader isr = null;
    private BufferedReader br = null;
    private InputStreamReader esr = null;
    private BufferedReader ber = null;
    private OutputStreamWriter osr = null;
    private PrintWriter pw = null;
    private String id = "t" + (ID++);
    private long tPerS = -1; // ticksPerSecond
    private DebugListener dbgListener = null;
    private Radio radio = null;
    private List<SimChannel> m_Channels = new ArrayList<SimChannel>();
    
    private File m_dir = null; ///< execution directory
    
    public File getDir() {
        return m_dir;
    }
    public Tossim(String dir) {
        this(new File(dir));
    }
    
    public Tossim(File dir) {
        initTossim(dir);
        runFunc("from TOSSIM import *");
        runFunc("import sys");
        runFunc(id + " = Tossim([])");
    }
    
    public void setDebugListener(DebugListener dbgListener) {
        this.dbgListener = dbgListener;
    }
    
    public boolean runNextEvent() {
        String ret = null;
        boolean b = false;
        
        ret = runFunc(id + ".runNextEvent()");
        if(ret == null) throw new RuntimeException("TOSSIM: Expected return value (did python crash?)");
        
        ret = ret.trim();
        if(ret.equalsIgnoreCase("true") || ret.equals("1")) {
            b = true;
        } else if(ret.equalsIgnoreCase("false") || ret.equals(0) || Integer.parseInt(ret) == 0) {
            b = false;
        } else {
            throw new RuntimeException("Unexpected return value: " + ret);
        }
        
        return b;
    }
    
    public Mote getNode(int moteID) {
        Mote m = new Mote(this, moteID);
        String ret = runFunc(m.getVarID() + " = " + id + ".getNode(" + moteID + ")");
        if(ret != null) {
            throw new RuntimeException("Unexpected return value: " + ret);
        }
        
        return m;
    }
    
    public Radio radio() {
        if(radio == null) {
            radio = new Radio(this);
            String ret = runFunc(radio.getVarID() + " = " + id + ".radio()");
            if(ret != null) {
                throw new RuntimeException("Unexpected return value: " + ret);
            }
        }
        
        return radio;
    }
    
    public long ticksPerSecond() {
        if(tPerS < 0) {
            String ret = runFunc(id + ".ticksPerSecond()");
            try {
                ret = ret.trim();
                ret = ret.toLowerCase();
                if(ret.endsWith("l")) {
                    ret = ret.substring(0, ret.length() - 1);
                }
                tPerS = Long.parseLong(ret.trim());
            } catch(Exception ex) {
                throw new RuntimeException("Unexpected return value: " + ret);
            }
        }
        return tPerS;
    }
    
    public long time() {
        long time = 0l;
        String ret = runFunc(id + ".time()");
        try {
            String ret2 = ret.trim();
            ret2 = ret2.toLowerCase();
            if(ret2.endsWith("l")) {
                ret2 = ret2.substring(0, ret2.length() - 1);
            }
            time = Long.parseLong(ret2.trim());
        } catch(Exception ex) {
            throw new RuntimeException("Unexpected return value: " + ret);
        }
        
        return time;
    }
    
    public void runFor(float s) { // in seconds
        //TODO: what happens if t.runNextEvent() fails (no event to run)?
        long stopTime = time();
        stopTime += (long)(s * ticksPerSecond());
        String ret = runFunc("while(" + stopTime + " > " + id + ".time()): dummy = " + id + ".runNextEvent()\n");
        if(ret != null) {
            throw new RuntimeException("Unexpected return value: " + ret);
        }
    }
    
    /**
     * Opens a TOSSIM debug channel
     * @param channel Name of the channel as used in the dbg() command
     * @param print whether all messages to the channel should also be printed to stdout
     * @return the channel opened
     */
    public SimChannel addChannel(String channel, boolean print) {
        //TODO: TCP Streaming
        SimChannel sc = new SimChannel(channel, this, print );
        sc.start();
        m_Channels.add(sc);
        return sc;
        
        /*
         // register channel for standard output
          String ret = runFunc( id + ".addChannel(\"" + channel + "\", sys.stderr)");
          if (ret != null ) {
          throw new RuntimeException("Could not add channel: "+ channel);
          }
          
          return null;
          */
    }
    
    public String getID() {
        return id;
    }
    
    private void initTossim(File dir) {
        if(dir == null || !dir.isDirectory()) {
            throw new RuntimeException("Parameter dir needs to be a valid directory");
        }
        
        m_dir = dir;
        
        try {
            System.out.println("Starting TOSSIM...");
            String strExec = LocalSettings.getPythonStartString(m_dir.getAbsolutePath());

            Process p = Runtime.getRuntime().exec(strExec, null, m_dir);
            
            osr = new OutputStreamWriter(p.getOutputStream());
            pw = new PrintWriter(osr);
            isr = new InputStreamReader(p.getInputStream());
            br = new BufferedReader(isr);
            esr = new InputStreamReader(p.getErrorStream());
            ber = new BufferedReader(esr);

        } catch(IOException ioe) {
            ioe.printStackTrace();
        }
        waitForPrompt();
    }
    
    protected String runFunc(String cmd) {
        //System.out.println("COMMAND: " + cmd);
        pw.print(cmd+"\n");
        pw.flush();
        waitForPrompt();
        
        String line = null;
        try {
            if(br.ready()) {
                line = br.readLine();
            }
        } catch(IOException ioe) {
            ioe.printStackTrace();
        }
        return line;
    }
    
    protected void runFuncAsync(String cmd) {
        pw.print(cmd+"\n");
        pw.flush();
    }
    
    /*
     private void readData() {
     String line = null;
     try {
     while(br.ready()) {
     line = br.readLine();
     if(line == null) break;
     System.out.println("**> " + line);
     }
     } catch(IOException ioe) {
     ioe.printStackTrace();
     }
     }
     */
    
    private void waitForPrompt() {
        String line = null;
        try {
            while(true) {
                int c = ber.read();
                if(c < 0) {
                    // end of stream or error
                    break;
                }
                if(((char)c) == '>') {
                    // we found a prompt
                    line = ">";
                    while(ber.ready()) {
                        line += ((char)ber.read());
                    }
                    break;
                } else if(((char)c) == '.') {
                    // we found an intermediate prompt, ignore
                    ber.read();
                    ber.read();
                    ber.read();
                } else {
                    line = "" + ((char)c);
                    line += ber.readLine();
                    System.out.println("--> " + line);
                    if(dbgListener != null) {
                        int i1 = line.indexOf('(');
                        int i2 = line.indexOf(')');
                        int i3 = line.indexOf(':');
                        
                        if ( i1==-1 && i2==-1 && i3==-1 ) continue;
                        
                        try {
                            String type = line.substring(0, i1).trim();
                            int node = Integer.parseInt(line.substring(i1 + 1, i2).trim());
                            String msg = line.substring(i3 + 1).trim();
                            if(type.equalsIgnoreCase("debug")) {
                                dbgListener.debugMsg(node, msg);
                            } else {
                                dbgListener.errorMsg(node, msg);
                            }
                        } catch( java.lang.StringIndexOutOfBoundsException e ) {
                            System.out.println("line=\""+line+"\",i1="+i1+",i2="+i2+",i3="+i3);
                        } catch( NumberFormatException e ) {
                            System.err.println("Unexpected response: " + line);
                        } catch(Exception ex) {
                            System.out.println("line=\""+line+"\",i1="+i1+",i2="+i2+",i3="+i3);
                            ex.printStackTrace();
                        } 
                    }
                }
            }
        } catch(IOException ioe) {
            ioe.printStackTrace();
        }
    }
    
    public void close() {
        for (int i=0; i<m_Channels.size(); i++ ){
            m_Channels.get(i).stopChannel();
        }
        
        if(br  != null) try {  br.close(); } catch(IOException ioe) {}
        if(isr != null) try { isr.close(); } catch(IOException ioe) {}
        if(ber != null) try { ber.close(); } catch(IOException ioe) {}
        if(esr != null) try { esr.close(); } catch(IOException ioe) {}
        if(pw  != null)        pw.close();
        if(osr != null) try { osr.close(); } catch(IOException ioe) {}
        
    }
    
    public static void main(String[] args) {
        Tossim t = new Tossim(args[0]);
        System.out.println("> Ticks: " + t.ticksPerSecond());
        t.addChannel("Init",true);
        t.addChannel("LedsC",true);
        t.setDebugListener(new DebugListener() {
            public void debugMsg(int node, String msg) {
                System.out.println("=> [" + node + "]: " + msg);
            }
            public void errorMsg(int node, String msg) {
                System.out.println("*> [" + node + "]: " + msg);
            }
        });
        System.out.println("> t.runNextEvent(): " + t.runNextEvent());
        Mote m = t.getNode(1);
        System.out.println("> m.isOn(): " + m.isOn());
        m.bootAtTime(1234l);
        System.out.println("> m.isOn(): " + m.isOn());
        System.out.println("> t.runNextEvent(): " + t.runNextEvent());
        System.out.println("> m.isOn(): " + m.isOn());
        System.out.println("> Time: " + t.time());
        t.runFor(5);
        System.out.println("> Time: " + t.time());
        t.close();
    }
}
