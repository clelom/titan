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
 * TOSCommandLine.java
 * 
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 * 
 * This file contains the main module of the TOSCommandLine 
 * package. It starts a console which allows to enter 
 * commands for a TinyOS based sensor network.
 * 
 */

package TOSCommandLine;

import java.io.*;
import java.util.*;

/**
 * TOSCommandLine starts a command line and offers a possibility to 
 * include custom commands. It is basically used to start up different 
 * programs and help handling the inputs more easily as multiple 
 * command line executables. 
 */
public class TOSCommandLine {
    
    /**
     * Program entry point. In this function, all commands 
     * need to be registered with the TOSCommandLine object.
     * 
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        TOSCommandLine tsc = new TOSCommandLine();
        
        // Register here all the command instances
        tsc.registerCommand( new TOSHelp(tsc.m_Commands) );
        tsc.registerCommand( new TOSExit(tsc) );
        tsc.registerCommand( new TOSTitanWrapper() );
        
        tsc.start(args);
    }
    
    /* ******************************************************* */
    
    /** A Vector with all registered commands on the service */
    private Map<String,ITOSCommand> m_Commands;
    
    /** Exits the command line if turning TRUE. */
    private boolean m_bQuit;
    
    /* ******************************************************* */
    
    /** Initializes TOSCommandLine. */
    TOSCommandLine() {
        m_Commands = new TreeMap<String,ITOSCommand>();
    }
    
    /** Starts the execution of the command line. Reads user 
     * input and calls registered commands.
     * @param args Not yet used
     */
    public void start( String[] args ) {
        
        // welcome message
        System.out.println("Welcome to the TinyOS command line");
        System.out.println("Please enter your command:");

        // startup commands
        int start_str_idx = 0;
        String[] start_str = {"titan"}; // these are "autostart" commands

        // bind standard input to stream
        BufferedReader din = new BufferedReader(new InputStreamReader(System.in));	
        
        // command iteration
        m_bQuit = false;
        while(m_bQuit == false) {
            // read next command
            System.out.print(">");
            
            String strLine="";

            if (start_str_idx < start_str.length) {
              strLine = start_str[start_str_idx++];
              System.out.println(strLine);
            }
            else {
              try {
                  strLine = din.readLine();
              } catch (IOException e) {
                  e.printStackTrace();
              }
            }
            
            // remove whitespaces and separate entries
            strLine = strLine.trim();
            String[] strArgs = strLine.split(" "); 
            if (strArgs.length == 0 ) continue;
            if (strArgs[0].length() == 0) continue;
            
            // extract command
            String strCommand = strArgs[0];
            
            if ( m_Commands.containsKey(strCommand) ) {
                ITOSCommand cmd = (ITOSCommand)m_Commands.get(strCommand);
                cmd.execute( strLine.split(" "));
            } else {
                System.out.println(strCommand + ": Unknown command");
            }
            
        } // while read commands
        
        System.out.println("Bye bye");
        System.exit(0);
    }
    
    /** Registers a command with the TOSCommand line.
     * @param cmd Reference to the command object to be registered
     * @return TRUE if successfully added. FALSE may indicate that the command name has already been registered 
     */
    public boolean registerCommand( ITOSCommand cmd ) {
        
        // make sure there is something
        if ( cmd == null ) return false;
        
        // make sure everything is initialized
        if ( m_Commands == null ) {
            System.out.println("No commands object available!");
            System.exit(-1);
        }
        
        // check whether the command already exists
        if ( !m_Commands.containsKey( cmd.toString() )) {
            m_Commands.put( cmd.toString(), cmd );
        } else return false;
        
        // add command into list
        return true;
    }
    
    /** Can be called by a command to trigger the end of the command 
     *  line entry loop. This quits the application.
     */
    public void finish() {
        m_bQuit = true;
    }
}
