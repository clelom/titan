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
 * 
 * TestCompiler
 * 
 * To create a Titan jar:
 * jar cf titan.jar ./titan/*.class ./titan/* /*.class
 * jar tf titan.jar
 * 
 * Running stuff:
 * IfE-Server:
 * java -server -classpath titan.jar:jgap.jar:tinyos.jar titan.compiler.TestCompiler test_compilation.txt ../j_inputs/servicedirectories_g6_pel5_%1\$03d.txt 0 ../j_outputs/compileout_g6_pel5_%1\$03d.txt 100 > output_g6_pel5.txt &
 * java -server -classpath titan.jar:jgap.jar:tinyos.jar titan.compiler.TestCompiler test_compilation.txt ../j_inputs/customcluster_%1\$03d.txt 0 ../j_outputs/customcluster_%1\$03d.txt 90 100 > customcluster90a.txt &
 * 
 * ClemQuad: (in simulation/lib directory)
 * "C:\Program Files\Java\jdk1.5.0_14\bin\java.exe" -server -XX:SurvivorRatio=8 -Xloggc:gc.log -classpath titan.jar;jgap.jar;tinyos.jar titan.compiler.TestCompiler ../inputs/test_compilation.txt "../j_inputs/servicedirectories_g2_pel10_%1$03d_all.txt" 0 "../j_outputs/compileout_g2_pel10_%1$03d_all.txt" 1 0 > output_g2_pel10_all.txt
 * 
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 */

package titan.compiler;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import titan.ConfigReader;
import titancommon.compiler.NetworkEvaluator;
import titancommon.compiler.Compiler;
import titancommon.execution.TaskNetwork;
import titancommon.services.ServiceDirectory;

public class TestCompiler {
    
    static long ms_TOSTime;

    private static void printUsage() {
        System.err.println("Usage: TestCompiler CONFIG [CLUSTER [CLUSTERNUM [OUTPUTFILE]]]");
        System.err.println("       Tests the clustering performance of a Titan compiler with:");
        System.err.println("       CONFIG     being the application task graph file, and");
        System.err.println("       CLUSTER    being the *optional* clustering information file");
        System.err.println("       CLUSTERNUM being the *optional* cluster index CONFIG runs in");
        System.err.println("       OUTPUTFILE being the *optional* file to append output");
        System.err.println("       -> Please check TestCompiler.java for formats");
    }
    
    public TestCompiler() {
    }
    
    /**
     * This function has been created to be run by Matlab
     * @param strConfigFile
     * @param strClusterFile
     * @param iCluster
     * @param strOutputFile
     * @return
     */
    public int runBatch(String strConfigFile, String strClusterFile, int iCluster, String strOutputFile) {

    	BufferedWriter outfile;
        // truncate output file
        try {
            FileWriter fw = new FileWriter(strOutputFile);
            outfile = new BufferedWriter(fw);
        } catch (IOException e) {
            System.err.println("Could not create file: "+strOutputFile);
            return -1;
        }
        
        ServiceDirectory sd=null;
        TaskNetwork tnInitial = readCfgFile(strConfigFile);
        
        if (tnInitial==null) return -4;
        
        // read in service directory information
        try {
            BufferedReader inFile = new BufferedReader( new FileReader(strClusterFile));
            String strLine;
            
            // iterate through the whole configuration file
            while((strLine= inFile.readLine())!=null){
                sd = processClusterFileLine(iCluster, strLine);
                if (sd==null) {
                    System.err.println("Internal error: no service directory instantiated");
                    return -2;
                }
                //Compiler comp = new GreedyCompiler(sd);
                Compiler comp = new EvolutionCompiler(sd);
                compileAndEvaluate(comp, sd, tnInitial, outfile );
            }
            
            outfile.close();
            
        } catch (FileNotFoundException e) {
            System.err.println("ERROR: Could not open file: \""+strClusterFile+"\": " + e.getMessage());
            return -3;
        } catch (IOException e) {
            System.err.println("ERROR: Could read from file: \""+strClusterFile+"\": " + e.getMessage());
            return -4;
        }
        return 0;
    }
    
    /**
     * Processes a cluster definition file. The expected format is:
     * 
     * [tostime] [clusternum] {[nodesincluster] ([nodeID] [nodeType] [nodeservices] [serviceid]*)*}* [connmatrix] "\n"
     * 
     * clusternum     number of clusters {}*
     * nodesincluster number of nodes in cluster ()*
     * nodeservices   number of services on node []*
     * 
     * connmatrix are clusternum^2 [0,1] values indicating which clusters are connected
     * 
     * @param strFile  Filename of the cluster data
     * @param iCluster Local cluster number
     * @return The local ServiceDirectory
     */
    private static ServiceDirectory processClusterFileLine(int iCluster, String strLine) {
        ServiceDirectory sd;
        int[][] connmatrix;
        ServiceDirectory[] sds;
        String[] strEntries = strLine.split(";");
        
        // read header
        ms_TOSTime = Long.parseLong(strEntries[0]);
        int iClusters = Integer.parseInt(strEntries[1]);

        // fill up service directories
        sds = new ServiceDirectory[iClusters]; 
        
        // go through each cluster and extract node/service information
        int index=2;
        for (int i=0; i<iClusters;i++){
            sds[i] = new ServiceDirectory(null);

            // go through the nodes
            int iNodes = Integer.parseInt(strEntries[index++].trim());
            for ( int j=0; j<iNodes; j++ ) {
                int iNodeAddr = Integer.parseInt(strEntries[index++].trim());
                int iNodeType = Integer.parseInt(strEntries[index++].trim());
                int iServices = Integer.parseInt(strEntries[index++].trim());
                
                int [] services = new int[iServices];
                for(int k=0; k<iServices;k++) {
                    services[k] = Integer.parseInt(strEntries[index++].trim());
                }
                
                //System.out.println("Adding node " + iNodeAddr + " to cluster " + i + " with "+iServices+" services");
                
                // register node with services
                sds[i].updateNodeInfo(iNodeAddr, iNodeType, services);
            }
            
            
        }
        
        // read in connectivity between clusters
        connmatrix = new int[iClusters][iClusters];
        for( int i=0; i<iClusters; i++) {
            for(int j=0;j<iClusters;j++){
                connmatrix[i][j] = Integer.parseInt(strEntries[index++].trim());
            }
        }
        
        // alright, done reading, now create the structure
        if ( sds == null || sds.length == 0 ) {
            System.err.println("ERROR: No cluster found, can't do anything");
            return null;
        }

        // assign local service directory
        int cluster = iCluster; 
        if ( cluster > sds.length-1 ) {
            System.err.println("ERROR: Selected cluster does not exist!");
            return null;
        }
        sd = sds[cluster];
        for (int i=0; i<sds.length; i++ ) {
            if ((i!=cluster) && (connmatrix[cluster][i] != 0)) {
                sd.addForeignSD(sds[i]);
            }
        }
        return sd;
    }

    /**
     * Compiles and evaluates the given task network using the information 
     * stored in the service directory. Results are written to the output file.
     * @param sd        A ServiceDirectory indicating the state of the network
     * @param tnInitial The TaskNetwork to distribute on the network
     * @param outfile   The filename to print the output into
     */
    private static void compileAndEvaluate(Compiler comp, ServiceDirectory sd, TaskNetwork tnInitial, BufferedWriter out) {
        TaskNetwork tnCompiled = null;
        double dResult=0.0;
        boolean debugoutput = false;
        
        try{ 
            tnCompiled = comp.compileNetwork(tnInitial);
            
            if ( tnCompiled == null ) {
                System.out.println("At " + ms_TOSTime + ": Compiler did not return a task network");
            } else {
    
                NetworkEvaluator ne = new NetworkEvaluator(sd);
                dResult = ne.evaluateNet(tnCompiled);

                if (debugoutput) {
                    // print compiled network
                    System.out.println("\nCompiled onto "+tnCompiled.m_Nodes.size()+" nodes:");
                    tnCompiled.printTaskNetwork();
                    System.out.flush();
                    System.err.flush();
                    
                    // evaluation
                    System.out.println("\n\n************************************\n Evaluation:\n");
                    System.out.println("\nTotal task network cost: " + dResult );
                }
                //System.out.println("At "+ms_TOSTime + ": compiled with cost "+dResult);
            }


        } catch (Exception e) {
            if (debugoutput) {
                System.err.println("Could not compile network: " + e.getMessage());
                //e.printStackTrace();
            }
        }
        
        // write to output file
        if ( out != null ) {
            try {
                String strLine = (tnCompiled==null)?
                            (ms_TOSTime + ";0.0;0\n"):
                            (ms_TOSTime + ";" + dResult + ";" + tnCompiled.getConfigurationLine() + "\n"); 
                out.write( strLine );
                out.flush();
            } catch (IOException e) {
                System.err.println("Could not write to file: "+ e.getMessage());
            }
        }
    }

    /**
     * Reads a Titan configuration file and produces.
     * @param cfgFilename
     * @return
     */
    private static TaskNetwork readCfgFile(String cfgFilename) {
        ConfigReader cfgFile=null;
        try {
            cfgFile = new ConfigReader( cfgFilename );
            System.out.println("Configuration read:");
            System.out.println("Tasks      : " + cfgFile.numTasks() );
            System.out.println("Connections: " + cfgFile.numConnections());
        } catch (FileNotFoundException e) {
            System.err.println("ERROR: TestCompiler.readCfgFile: error: could not open file: \"" + cfgFilename+ "\" (" + e.getMessage()+")");
            return null;
        }
        TaskNetwork tnInitial = cfgFile.getTaskNetwork();
        return tnInitial;
    }

    
    /**
     * 
     * Tests a compiler and evaluates the performance of the result
     * 
     * @param args args[0] points to a configuration file that 
     *             describes a task network.
     *             if args[1] exists, it is interpreted as a 
     *             configuration file describing clustering 
     *             configurations at each time.
     */
    public static void main(String[] args) {
        
        //args[0] = "titan/test_knn.txt";
//        args[0] = "C:\\Dokumente und Einstellungen\\Clemens\\Eigene Dateien\\Programs\\Titan\\TitanJava\\titan\\test_configreader.txt";//"titan/test_configreader.txt";
//        args[1] = "C:\\Dokumente und Einstellungen\\Clemens\\Eigene Dateien\\Programs\\Titan\\TossimTraces\\servicedirectories2.txt";//"D:\\cygwin\\opt\\tinyos-1.x\\apps\\TossimTraces\\matlab\\servicedirectories2.txt";
//        args[2] = "0";
//        args[3] = "output.txt";
        
        if (args.length < 1 ) {
            printUsage();
            return;
        }
        
        // truncate outputfile
        if ( args.length>3 && args[3].indexOf("%") == -1 ) {
            try {
                FileWriter fw = new FileWriter(args[3]);
                fw.close();
            } catch (IOException e) {
                System.err.println("Could not create file: "+args[3]);
            }
        }


        
        ServiceDirectory sd=null;
        if ( args.length < 2 ) {
            // set up and fill some standard service directory
            sd = new ServiceDirectory(null);
            int [] tasks0 = {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19};
            sd.updateNodeInfo( 0, 1, tasks0 );
            int [] tasks1 = {1,3,4,5,13};
            sd.updateNodeInfo( 1, 1, tasks1 );

            // get the task network
            
//            Compiler comp = new GreedyCompiler(sd);
            Compiler comp = new EvolutionCompiler(sd);

            TaskNetwork tnInitial = readCfgFile(args[0]);
            if ( tnInitial == null ) System.exit(-7);

            try {
				compileAndEvaluate(comp, sd, tnInitial, (args.length >3)? new BufferedWriter(new FileWriter(args[3])):null );
			} catch (IOException e) {
				System.out.println("Could not open file :"+args[3]+" :" );
				e.printStackTrace();
			}
            
        } else if (args.length == 6) { // process batch of messages ////////////////////// work here!!!
        	String strConfigFile = args[0];
           	String strServDirTemplate = args[1];
        	int clusternum = Integer.parseInt(args[2]);
        	String strOutFileTemplate = args[3]; 
        	int iterations = Integer.parseInt(args[4]);
        	int iStart     = Integer.parseInt(args[5]);
        	
            TaskNetwork tnInitial = readCfgFile(strConfigFile);
            if( tnInitial == null ) {
            	System.err.println("ERROR: Could not open config file: \"" + strConfigFile + "\"");
            	System.exit(-7);
            }
        	
            // read in service directory information
            try {
                for (int iRun=iStart; iRun<iterations; iRun++ ) {
                	String strServDirectory = String.format(strServDirTemplate, iRun);
                	String strOutfile       = String.format(strOutFileTemplate, iRun);
                	
                	System.out.println("Compiling \"" + strServDirectory + "\" to \"" + strOutfile + "\"");

                	BufferedReader inFile = new BufferedReader( new FileReader(strServDirectory));

	                String strLine;
	                BufferedWriter outfile = null;
	                
	                // iterate through the whole configuration file
    				outfile = new BufferedWriter(new FileWriter(strOutfile,true));
	
	                while((strLine= inFile.readLine())!=null){
	                    sd = processClusterFileLine(clusternum, strLine);
	                    if (sd==null) {
	                        System.err.println("Internal error: no service directory instantiated");
	                        System.exit(-5);
	                    }
	                    
	                    // change here when multiple compilations are given
	                    //Compiler comp = new GreedyCompiler(sd);
	                    Compiler comp = new EvolutionCompiler(sd); 
	                    compileAndEvaluate(comp, sd, tnInitial, outfile );
	                    //outfile.write("hello");
	                }
	                
	                if (outfile != null ) {
	                	outfile.close();
	                }
                }
                
            } catch (FileNotFoundException e) {
                System.err.println("ERROR: File not found error" + e.getMessage());
                System.exit(-1);
            } catch (IOException e) {
                System.err.println("ERROR: General read error " + e.getMessage());
                System.exit(-2);
            }
        	
        	
        }else {
            int clusternum = ( args.length < 3 )? 0: Integer.parseInt(args[2]);
            TaskNetwork tnInitial = readCfgFile(args[0]);
            
            if( tnInitial == null ) System.exit(-7);

            // read in service directory information
            try {
                BufferedReader inFile = new BufferedReader( new FileReader(args[1]));
                String strLine;
                BufferedWriter outfile = null;
                
                // iterate through the whole configuration file
                if ( args.length > 3 ) {
                    try {
        				outfile = new BufferedWriter(new FileWriter(args[3],true));
        			} catch (IOException e) {
        				System.err.println("Could not open file: "+args[3]);
        				outfile = null;
        			}
                }

                while((strLine= inFile.readLine())!=null){
                    sd = processClusterFileLine(clusternum, strLine);
                    if (sd==null) {
                        System.err.println("Internal error: no service directory instantiated");
                        System.exit(-5);
                    }
                    
                    // change here when multiple compilations are given
                    //Compiler comp = new GreedyCompiler(sd);
                    Compiler comp = new EvolutionCompiler(sd); 
                    compileAndEvaluate(comp, sd, tnInitial, outfile );
                }
                
                if (outfile != null ) {
                	outfile.close();
                }
                
            } catch (FileNotFoundException e) {
                System.err.println("ERROR: Could not open file: \""+args[1]+"\": " + e.getMessage());
                System.exit(-1);
            } catch (IOException e) {
                System.err.println("ERROR: Could read from file: \""+args[1]+"\": " + e.getMessage());
                System.exit(-2);
            }
        } // if CLUSTER argument given

    } //main


}
