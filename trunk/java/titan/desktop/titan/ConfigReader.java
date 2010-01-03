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
 * ConfigReader.java
 * 
 * Implements a configuraton file reader for Titan.
 * 
 * ConfigReader reads a Titan configuration file the format is as follows:
 * Between every format block, any number of white space characters are allowed.
 * 
 * File:
 *  +--------+    +-------+
 *  | BLOCK  +--->+ BLOCK +
 *  +--------+    +-------+
 * 
 * BLOCK:
 *  
 *      +------+    +---+    +---+        +-------+        +---+
 *  --->+ NAME +--->+ = +--->+ [ +---+--->+ ENTRY +---+--->+ ] +--->
 *      +------+    +---+    +---+   �    +-------+   |    +---+
 *                                   |                |
 *                                   |      +---+     |
 *                                   +------| , +<----+
 *                                          +---+
 * 
 * ENTRY:
 * 
 *     +------+    +---+    +--------+    +---+
 *  -->+ NAME +--->+ ( +--->+ PARAMS +--->+ ) +---+--------------------------+--->
 *     +------+    +---+    +--------+    +---+   |                          �
 *                                                |    +---+    +---------+  |
 *                                                +--->+ : +--->+ ATTRIBS +--+
 *                                                     +---+    +---------+
 * 
 * PARAMS:
 *             +-------+        
 *  -->---+--->+ PARAM +---+--->
 *        �    +-------+   |    
 *        |                |
 *        |      +---+     |
 *        +------| , +<----+
 *               +---+
 * 
 * ATTRIBS
 *     +---+        +----------+    +---+    +-------+         +---+
 *  -->+ ( +---+--->+ ATTRIBID +--->+ = +--->+ VALUE +--->+--->+ ) +--->
 *     +---+   �    +----------+    +---+    +-------+    |    +---+
 *             |                                          |
 *             |                    +---+                 |
 *             +--------------------+ , +<----------------+
 *                                  +---+
 * 
 * Remarks:
 * The two blocks in the configuration file must be called "tasks" and "connections". In the 
 * "tasks" block, the entries represent tasks, which are searched in the task database in the 
 * "Task" subdirectory. In the "connections" block only connections are allowed. Which 
 * instantiate "Connection" objects.
 * 
 * Valid attributes for tasks are are:
 * nodeID
 * 
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 */

package titan;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.io.*;

import titancommon.Connection;
import titancommon.execution.TaskNetwork;
import titancommon.tasks.*;
import titancommon.tasks.*;
import titancommon.execution.*;

public class ConfigReader {
    
    // enum workaround for old compiler
//  private class CFGState {
//  final static int INIT =1;
//  final static int TASKS=2;
//  final static int CONNECTIONS=3;
//  final static int ERROR=4;
//  final static int SUCCESS=5;
//  }
//  int m_State;
    // this would be code for Java 5
    private enum CFGState {INIT,TASKS,CONNECTIONS,ERROR,SUCCESS};
    CFGState m_State;
    
    private Map<String,Task> m_TaskMap = new HashMap<String,Task>();
    private ArrayList<Task> m_Tasks = new ArrayList<Task>();
    private ArrayList<Connection> m_Connections = new ArrayList<Connection>();
    
    private int m_iLine;
    private BufferedReader m_cfgFile;
    
    private class CfgError extends Throwable {
        private static final long serialVersionUID = 1L;
        String strError;
        int    iLine;
        CfgError( String error, int line) {strError = error;iLine=line;}
    }
    
    int m_iError;
    
    /**
     * Parses a configuration file, and is able to formulate 
     * configuration messages
     * @param configuration file 
     */
    public ConfigReader( String strFilename ) throws FileNotFoundException
    {
        try {
            getTasks(Task.class.getPackage().toString().substring( 8 ));
        } catch (ClassNotFoundException e1) {
            // seems that the package does not exist or cannot be found
            System.err.println("WARNING: Could not open tasks package at: \""+ Task.class.getPackage().toString().substring( 8 ) + "\" ("+e1.getMessage()+") -> Loading AliasTask only");
            m_TaskMap.put( "alias", new AliasTask());
        }
        
        m_cfgFile = new BufferedReader( new FileReader(strFilename));
        m_iLine=0;
        
        m_iError=0;
        
        try {
            String strLine = getLine();
            
            while (strLine != null) {
                strLine = processBlock( strLine );
                if ( strLine.length() == 0 ) {
                    strLine = getLine();
                }
            }
            
        } catch (CfgError e) {
            System.err.println( "ERROR: on line "+e.iLine+": " + e.strError);
            m_iError = 1;
        }
        
    }
    
    private String processBlock( String strLine ) throws CfgError {
        
        String strBlockName;
        
        // check whether the block is on a single line
        if( strLine.indexOf("=") == -1 ) {
            
            // check for whitespace
            if ( (strLine.indexOf(" ") != -1) || (strLine.indexOf("\t") != -1)) {
                throw new CfgError("no whitespace allowed in block name", m_iLine);
            }
            
            // store block name
            strBlockName = strLine;
            
            // get next line
            strLine = getLine();
            
            // check on next line
            if( strLine.charAt(0) != '=' ) {
                throw new CfgError("expected '=' as first non-whitespace character", m_iLine);
            }
            
            strLine = strLine.substring(1,strLine.length());
        } else {
            strBlockName = strLine.substring( 0, strLine.indexOf("="));
            // check for whitespace
            if ( (strBlockName.indexOf(" ") != -1) || (strBlockName.indexOf("\t") != -1)) {
                throw new CfgError("no whitespace allowed in block name", m_iLine);
            }
            strLine = strLine.substring( strLine.indexOf("=")+1, strLine.length());
        }
        
        // get the beginning of the block
        if( strLine.indexOf("[") == -1 ) {
            strLine.trim();
            if (strLine.compareTo("")!=0) {
                throw new CfgError("no non-whitespace allowed between '=' and '['", m_iLine );
            }
            strLine = getLine();
            
            // check on next line
            if( strLine.charAt(0) != '[' ) {
                throw new CfgError("expected '[' as first non-whitespace character", m_iLine);
            }
            
            strLine = strLine.substring(1,strLine.length());
        } else {
            strLine.trim();
            if( strLine.charAt(0) != '[' ) {
                throw new CfgError("expected '[' as first non-whitespace character", m_iLine);
            }
            
            strLine = strLine.substring( strLine.indexOf("[")+1, strLine.length());
        }
        
        // alright, strLine is now at the beginning of the block. Now examine, 
        // which block will be analyzed
        
        if ( strBlockName.toLowerCase().compareTo("tasks") == 0 ) {
            strLine = processTasks(strLine.trim());
        } else if ( strBlockName.toLowerCase().compareTo("connections") == 0 ){
            strLine = processConnections(strLine.trim());
        } else {
            throw new CfgError("Unknow block '"+strBlockName+"'", m_iLine);
        }
        
        // return the rest of the line
        return strLine;
        
    }
    
    private String processConnections(String strLine) throws CfgError {
        
        // get together the first entry
        int iEndIndex;
        while ( (iEndIndex = strLine.indexOf(")")) == -1 ) {
            
            if ( strLine.length() > 0 && strLine.toLowerCase().charAt(0) != 'c') {
                throw new CfgError("'Connection' expected", m_iLine);
            }
            
            if ( strLine.indexOf("]") != -1 ) {
                throw new CfgError("unexpected ']'", m_iLine);
            }
            
            strLine += getLine();
        }
        
        // extract connection settings
        String strConnection = strLine.substring(0,iEndIndex);
        strLine = strLine.substring(iEndIndex+1, strLine.length());
        
        // check whether the syntax is correct
        strConnection.replaceAll(" ", "");
        strConnection.replaceAll("\t", "");
        if ( strConnection.indexOf("Connection(") != 0 ) {
            throw new CfgError("bad connection format", m_iLine);
        }
        strConnection = strConnection.substring( 11, strConnection.length());
        String [] strInfo = strConnection.split(",");
        if (strInfo.length !=4 ){
            throw new CfgError("Connection must contain 4 parameters", m_iLine);
        }
        
        int st=0,sp=0,dt=0,dp=0;
        
        try {
        	sp = Integer.parseInt(strInfo[1].trim());
        } catch (NumberFormatException e) {throw new CfgError("Source port is not numeric", m_iLine);};
        try {
        	dp = Integer.parseInt(strInfo[3].trim());
        } catch (NumberFormatException e) {throw new CfgError("Source port is not numeric", m_iLine);};

        try {
        	st = Integer.parseInt(strInfo[0].trim());
        } catch (NumberFormatException e) {
        	if (strInfo[0].trim().compareTo("X") == 0) {
        		st = 255;
        	} else throw new CfgError("Invalid source task", m_iLine);
        }

        try {
        	dt = Integer.parseInt(strInfo[2].trim());
        } catch (NumberFormatException e) {
        	if (strInfo[2].trim().compareTo("X") == 0) {
        		dt = 255;
        	} else throw new CfgError("Invalid destination task", m_iLine);
        }
        
    	processConnection( st, sp, dt, dp);
        
        // check whether we need to go on
        if ( strLine.length() == 0 ) {
            strLine = getLine();
        }
        strLine.trim();
        
        if ( strLine.charAt(0) == ',' ) {
            return processConnections( strLine.substring(1,strLine.length()));
        } else if ( strLine.charAt(0) == ']' ) {
            return strLine.substring(1,strLine.length());
        } else {
            throw new CfgError("Syntax error, expected: ',' or ']'", m_iLine );
        }
        
    }
    
    /**
     * Process tasks expectes a series of tasks
     * @return
     * @throws CfgError 
     */
    private String processTasks( String strLine ) throws CfgError  {
        
        int iIndexComma       = strLine.indexOf(",");
        int iIndexParantheses = strLine.indexOf("(");
        int iIndexEnd         = strLine.indexOf("]");
        
        // if line is empty, advance to the next
        strLine.trim();
        /*		if ( iIndexComma == -1 && iIndexParantheses == -1 && iIndexEnd == -1 ) {
         strLine.trim();
         if ( strLine.length() != 0 ) {
         getTask( strLine, null, null );
         return "";
         } else return processTasks(getLine());
         }
         */
        if ( strLine.length() == 0) return processTasks(getLine() );
        
        // remove negative values
        iIndexComma       = iIndexComma      <0 ? strLine.length() : iIndexComma;
        iIndexParantheses = iIndexParantheses<0 ? strLine.length() : iIndexParantheses;
        iIndexEnd         = iIndexEnd        <0 ? strLine.length() : iIndexEnd;
        
        if ( iIndexEnd < iIndexComma && iIndexEnd < iIndexParantheses ) {
            
            // check whether we are at the end
            if ( iIndexEnd == 0 ) {
                return strLine.length() > 1 ? strLine.substring(1,strLine.length()) : "";
            }
            
            String strTask = strLine.substring( 0, iIndexEnd);
            strTask.trim();
            getTask( strTask, null, null );
            strLine = strLine.substring( iIndexEnd+1, strLine.length() );
            return strLine;
        } else if ( iIndexComma <  iIndexParantheses ) {
            String strTask = strLine.substring( 0, iIndexComma );
            strTask.trim();
            getTask( strTask, null, null );
            strLine = strLine.substring( iIndexComma+1, iIndexComma );
            return strLine;
        } else if ( iIndexParantheses < iIndexComma ) {
            
            String strTask = strLine.substring( 0, iIndexParantheses );
            strTask.trim();
            strLine = strLine.substring( iIndexParantheses+1, strLine.length() );
            
            ////////////////////////////////////
            // get arguments
            
            // get closing parantheses
            int iIndexClose;
            while ( (iIndexClose = strLine.indexOf(")")) == -1 ) {
                strLine += getLine();
            }
            
            String strArguments = strLine.substring(0,iIndexClose);
            
            if ( strArguments.indexOf("(") != -1 ) {
                throw new CfgError( "Found nested parentheses - this is not implemented yet, sorry", m_iLine);
            }
            
            strLine = strLine.substring( iIndexClose+1,strLine.length());
            
            ////////////////////////////////////
            // check whether there are any attributes
            strLine.trim();
            String strAttributes = null;
            if (strLine.length() == 0) {
                strLine = getLine();
            }
            if (strLine.charAt(0) == ':') {
                // get the end of the paramter list
                int iAttribEnd;
                while( (iAttribEnd = strLine.indexOf(")")) == -1 ) {
                    strLine += getLine();
                }
                strAttributes = strLine.substring(2,iAttribEnd);
                strLine = strLine.substring( iAttribEnd+1,strLine.length());
            }
            
            // ok, now we have all the data we need
            getTask( strTask, strArguments, strAttributes );
            
            ////////////////////////////////////
            // cleanup
            strLine.trim();
            if (strLine.length() == 0 ){
                strLine = getLine();
            }
            if ( strLine.charAt(0) == ']') {
                return strLine.substring(1,strLine.length());
            }
            else if (strLine.charAt(0) == ',')
            {
                return processTasks(strLine.substring(1, strLine.length()));
            }
            else
            {
                throw new CfgError("Expected ','", m_iLine);
            }
            
        } else {
            strLine += getLine();
            return processTasks( strLine );
        }
        
    }
    
    private void getTask( String strTask, String strArguments, String strAttributes ) throws CfgError {
        
        String[] args = null;
        Map<String, String> attributes = null;
        
        if ( strArguments != null ) {
            StringTokenizer st = new StringTokenizer(strArguments, ",", false);
            
            if (st.hasMoreTokens())
            {
                ArrayList<String> arguments = new ArrayList<String>();
                while (st.hasMoreTokens())
                {
                    String strToken = st.nextToken();
                    arguments.add(new String(strToken.trim()));
                }
                
                args = (String[])arguments.toArray(new String[arguments.size()]);
            }
            
        }
        
        if (strAttributes != null) {
            StringTokenizer st = new StringTokenizer(strAttributes, ",", false);
            attributes = new HashMap<String,String>();
            
            if (st.hasMoreTokens())
            {
                while (st.hasMoreTokens())
                {
                    String strToken = st.nextToken();
                    
                    if (strToken.indexOf("=") != -1) {
                        int iEq = strToken.indexOf("=");
                        String strName = strToken.substring(0, iEq);
                        String strValue = strToken.substring(iEq + 1, strToken.length());
                        
                        attributes.put(strName, strValue);
                    }
                } // while
            } // if attributes != null
            
            
        }
        
        processTask( strTask.toLowerCase(), args, attributes );
    }
    
    /**
     * Gets the next line from the configuration file
     * @return string containing the next line
     * @throws CfgError 
     * @throws IOException
     */
    private String getLine(  ) throws CfgError
    {
        if ( m_cfgFile == null ) throw new CfgError( "file not open", m_iLine);
        
        String strLine;
        
        try {
            while ( (strLine=m_cfgFile.readLine()) != null ) {
                m_iLine++;
                
                if ( strLine == null ) {
                    throw new CfgError( "End of File", m_iLine);
                }
                
                // remove comments, trim whitespace
                if ( strLine.indexOf("#") != -1 ) {
                    strLine = strLine.substring( 0, strLine.indexOf("#"));
                }
                strLine = strLine.trim();
                if ( strLine.compareTo("") != 0 ) break;
                
            }
        } catch (IOException e) {
            throw new CfgError( "ReadLine failed", m_iLine );
        }
        
        return strLine;
    }
    
    /**
     * Extracts connection parameters from connection definition line
     * @param strLine  Line containing connection data
     * @param iLine    line number
     * @throws CfgError 
     */
    private void processConnection(int iStartTask,int iStartPort,int iEndTask,int iEndPort) throws CfgError {
        
        Task tskStart, tskEnd;
        
        if (iStartTask != 255) {
	        try {
	            tskStart = (Task)m_Tasks.get(iStartTask);
	            
		        if ( (iStartPort < 0) || (iStartPort + 1 > tskStart.getOutPorts()) ) {
		            throw new CfgError("Source task does not have enough output ports", m_iLine);
		        }
	        } catch ( IndexOutOfBoundsException e ) {
	            throw new CfgError("Source task does not exist", m_iLine);
	        }
	        
        }
        
        if (iEndTask != 255) {
        	try {
	            tskEnd   = (Task)m_Tasks.get(iEndTask);
	        } catch ( IndexOutOfBoundsException e ) {
	            throw new CfgError("Target task does not exist", m_iLine);
	        }
	        
	        if ( (iEndPort < 0) || (iEndPort + 1 > tskEnd.getInPorts()) ) {
	            throw new CfgError("Target task does not have enough input ports", m_iLine);
	        }
        }
        
        if (iStartTask == 255 && iEndTask == 255 ) {
        	throw new CfgError("Relaying from broadcast to broadcast", m_iLine);
        }
        
        m_Connections.add( new Connection(iStartTask,iStartPort,iEndTask,iEndPort));
    }
    
    private void processTask(String strTask, String[] strArguments, Map<String, String> attributes) throws CfgError {
        
        Task listTask = null;
        
        // check whether the task name or the TaskID is given
        try {
            strTask.trim();
            int iTaskID = Integer.parseInt(strTask);
            listTask = (Task)m_TaskMap.get( ""+iTaskID );
        
        } catch (NumberFormatException e) {
            // extract by task name
            listTask = (Task)m_TaskMap.get( strTask );
        }
        
        if ( listTask == null ) {
            throw new CfgError( "Unknown task '"+strTask+"'", m_iLine );
        }

        Task newTask = (Task)listTask.clone();
        
        if ( !newTask.setConfiguration(strArguments) ) {
            throw new CfgError("Task '"+strTask+"' could not be configured", m_iLine);
        }
        
        // pass attributes
        if (attributes != null)
        {
            for (Iterator<String> i = attributes.keySet().iterator(); i.hasNext(); ) {
                String strKey = (String)i.next();
                newTask.addAttribute(strKey, attributes.get(strKey));
            }
        }
        
        m_Tasks.add( newTask );
        
//      System.out.println( "Added task: " + strTask );
        
    }
    
    /**
     * Returns the number of tasks read from the configuration file
     * @return Number of tasks
     */
    public int numTasks( ) {
        
        if ( m_State == CFGState.ERROR ) return 0;
        
        return m_Tasks.size();
    }
    
    /**
     * Returns the number of connections read from the configuration file 
     * @return Number of connections
     */
    public int numConnections() {
        
        if ( m_State == CFGState.ERROR ) return 0;
        
        return m_Connections.size();
    }
    
    /**
     * Returns the task indicated by the index
     * @param iIndex
     * @return the task at this position, or null
     */
    public TaskNetwork getTaskNetwork( ) {
        
        // check whether the configuration file has been correctly read
        if (m_iError!=0) return null;
        
        Task[] tasks = new Task[m_Tasks.size()];
        for ( int i=0; i < m_Tasks.size(); i++ ) {
            tasks[i] = (Task)m_Tasks.get(i);
        }
        
        Connection [] cons = new Connection[m_Connections.size()];
        for ( int i=0; i < m_Connections.size(); i++ ) {
            cons[i] = (Connection)m_Connections.get(i);
        }
        
        return new TaskNetwork( tasks, cons );
        
    }
    
    public boolean hasError() {
        return (m_iError!=0);
    }
    
    
    /**
     * I got this from: http://forum.java.sun.com/thread.jspa?threadID=341935&tstart=0
     * Many thanks to RKeene.
     * @param pckgname
     * @return
     * @throws ClassNotFoundException 
     * @throws ClassNotFoundException
     */
    public boolean getTasks(String pckgname) throws ClassNotFoundException {
        
        // Get a File object for the package
        File directory = null;
        try {
            ClassLoader cld = Thread.currentThread().getContextClassLoader();
            if (cld == null) {
                throw new ClassNotFoundException("Can't get class loader.");
            }
            String path = pckgname.replace('.', '/');
            URL resource = cld.getResource(path);
            if (resource == null) {
                throw new ClassNotFoundException("No resource for " + path);
            }
            String strFile = resource.getFile();
            
            // Java seems not to be able to handle spaces in the path
            strFile = strFile.replace("%20", " ");
            
            directory = new File(strFile);
        } catch (NullPointerException x) {
            throw new ClassNotFoundException(pckgname + " (" + directory
                    + ") does not appear to be a valid package");
        }
        
        if (directory.exists()) {
            // Get the list of the files contained in the package
            String[] files = directory.list();
            for (int i = 0; i < files.length; i++) {
                
                // we are only interested in .class files
                if (files[i].endsWith(".class")) {
                    
                    String strTask = files[i].substring(0, files[i].length() - 6 );
                    
                    // do not include the Task interface
                    if ( strTask.compareTo("Task") == 0 ) continue;
                    
                    // removes the .class extension
                    try {
                        Task newTask = (Task)Class.forName(pckgname + '.'+ strTask).newInstance();
                        
                        //System.out.println("ConfigReader: Adding task \""+newTask.getName()+"\"");
                        
                        Task mapTask = (Task)newTask.clone();
                        m_TaskMap.put( newTask.getName().toLowerCase(), mapTask );
                        m_TaskMap.put( ""+newTask.getID(), mapTask );
                        
                    } catch (InstantiationException e) {
                    } catch (IllegalAccessException e) {
                    } catch (ClassNotFoundException e) {
                    }
                    
                    
                }
            }
        } else {
            return false;
        }
        return true;
    }	
    
    ////////////////////////////////////////////////////////////////////////////
    // 
    /**
     * 
     * Tests the ConfigReader functionality. This code is only intended for 
     * testing purposes.
     * 
     * @param args Command line arguments. There can be optionally 1 argument 
     *             speciying the configuration file to be read.
     */
    public static void main(String[] args) {
        System.out.println("Testing ConfigReader...\n");
        try {
            ConfigReader cfgFile;
            if ( args.length > 0 ) {
                cfgFile  = new ConfigReader( args[0] );
            } else {
                cfgFile  = new ConfigReader( "Titan/test_configreader.txt" );
            }
            
            System.out.println("Tasks created      : "+cfgFile.numTasks());
            System.out.println("Connections created: "+cfgFile.numConnections());
            
            
        } catch (FileNotFoundException e) {
            System.err.println("Error: Could not open test.txt\n");
        }
    }
    
}
