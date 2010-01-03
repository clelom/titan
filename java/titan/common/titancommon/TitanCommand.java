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
 * TitanCommand.java
 * 
 * The command for TOSCommandLine connecting to the Tiny Task 
 * Network (Titan).
 * 
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 * @author Tonio Gsell <tgsell@ee.ethz.ch>
 * @author Jeremy Constantin <jeremyc@student.ethz.ch>
 */
package titancommon;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import net.tinyos.message.Message;
import net.tinyos.message.MessageListener;
import titan.ConfigReader;
import titan.TitanCommunicate;
import titan.messages.SerialMsg;
import titancommon.applications.Application;
import titancommon.applications.Farkle;
import titancommon.applications.Dice;
import titancommon.execution.*;
import titancommon.messages.*;
import titancommon.node.TitanLocalNode;
import titancommon.route.TitanCommunicateRouter;
import titancommon.route.TitanLocalRouter;
import titancommon.route.TitanMasterRouter;
import titancommon.server.ClientGetService;
import titancommon.services.ServiceDirectory;

public class TitanCommand implements MessageListener {

   protected TitanCommunicateRouter m_Comm;
   protected TitanMasterRouter m_MasterRouter;
   protected TitanLocalRouter m_LocalRouter;
   protected ServiceDirectory m_ServiceDirectory;
   protected TitanDataCom m_DataCom = new TitanDataCom(false);
   public MessageDispatcher m_msgDispatcher = new MessageDispatcher();   
   
   // configuration protocol version
   static public final short TC_VERSION = 1;  // changed to 0 in last tmote C code update?!
   
   public static TitanCommand singleton;
   
   static public final short BROADCAST_ADDR = (short)65535;

   // configuration protocol message type
   // this is equivalent to the definitions in TitanComm.h
   static public final short TITANCOMM_CONFIG = 0;
   static public final short TITANCOMM_CFGTASK = 1;
   static public final short TITANCOMM_CFGCONN = 2;
   static public final short TITANCOMM_CFGSUCC = 3;
   static public final short TITANCOMM_DISCOVER = 4;
   static public final short TITANCOMM_DICS_REP = 5;
   static public final short TITANCOMM_FORWARD = 6;
   static public final short TITANCOMM_DATAMSG = 7;
   static public final short TITANCOMM_ERROR = 8;   
   static public final short TITANCOMM_CFGSTART = 9;
   static public final short TITANCOMM_CACHE_START =10;
   static public final short TITANCOMM_CACHE_STORE =11;
   
   // task identifiers, copied from TitanTaskUIDs.h
   static public final int TITAN_COMM_MODULE = 0;
   static public final int TITAN_TASK_SIMPLEWRITER = 1;
   static public final int TITAN_TASK_AVERAGE = 2;
   static public final int TITAN_TASK_DUPLICATOR = 3;
   static public final int TITAN_TASK_LEDS = 4;
   
   // error codes
   static public final String[] TITAN_ERROR_CODES = {
      "UNDEFINED ERROR",
      "ERROR_NO_MEMORY",
      "ERROR_INPUT",
      "ERROR_MULT_INST",
      "ERROR_CONFIG",
      "ERROR_SEGMENT_SIZE",
      "ERROR_PACKET",
      "ERROR_RECEIVED_SOMETHING",
      "ERROR_TYPE",
      "ERROR_NOT_IMPLEMENTED",
      "ERROR_OUT_FIFO_FULL",
      "ERROR_IN_FIFO_EMPTY",
      "ERROR_NO_CONTEXT"
   };
   
   public static final int TOSH_DATA_LENGTH = 50;//28;
   public static final int TITAN_PACKET_HEADER_SIZE = 5; // 28-sizeof(uint16_t) - 3*sizeof(uint8_t)
   public static final int TITAN_PACKET_SIZE = TOSH_DATA_LENGTH - TITAN_PACKET_HEADER_SIZE; 

   private HashMap m_NetworkManagers = new HashMap();
   private HashMap m_Applications = new HashMap();

   private List/*<TitanLocalNode>*/ m_TitanLocalNodes = new LinkedList/*<TitanLocalNode>*/();

   ////////////////////////////////////////////////////////////////////////////
   // Constructor
   public TitanCommand() {
      singleton = this;
      /*
       * Commented this out because it creates instances of Farkle and Dice GUIs
       * which are not used under normal circumstances but interfere with our
       * window switch stuff.
       * Jonas Huber <huberjo@ee.ethz.ch>
       */
      //Farkle farkle = new Farkle();
      //m_Applications.put(farkle.getName().toLowerCase(), farkle);
      //Dice dice = new Dice();
      //m_Applications.put(dice.getName().toLowerCase(), dice);
      m_msgDispatcher.addObserver(m_DataCom);
   }
   ////////////////////////////////////////////////////////////////////////////
   // ITOSCommand interface
   /**
    * Returns a list of available subcommands for Titan
    * @return A multiline string containing available commands
    */
   public String getHelp() {
        return  "titan\n\nConnects to the Tiny Task Network (Titan)\n\n" + 
        "usage: titan COMMAND NUMBER\n" +
        "Where COMMAND has the following effect:\n" +
        "   start        starts up the Titan framework. With parameter SIM, it starts a simulation.\n" + 
        "   load         loads the file in the parameter PATH and instantiates a network manager\n" +
        "   clear        removes all configurations from mote NUMBER\n" +
        "   get          downloads the application with name NAME\n" +
        "   services     Lists the contents of the service directory\n" +
        "   sim          Controls the simulation\n" +
        "   stop         stops the execution of network manager with configuration number ID\n" +
        "   help         Prints this message screen\n" +
        "   node         start / stop local execution nodes\n" +
        " Test commands:\n" + 
        "   adctest      Loads a ADC test config onto node NUMBER\n" +
        "   ledtest      Loads a LED test config onto node NUMBER\n" +
        "   datatest     Loads Data test config onto node NUMBER\n" +
        "   cachetest    Loads the ledtest from the cache to NUMBER\n" +
        "   cfgstart     Starts the configuration NUMBER\n" +
        "   data         Sends a message with to node NUMBER to PORT with [DATA1 DATA2 DATA3]\n";
   }

   /**
    * Called by the TOSCommandLine shell to start the execution of the command
    * @param args A list of arguments provided to this command. First entry is "titan".
    * @return Positive if
    */
   public int execute(String[] args) {

      // check communications
      if ((args.length < 2) || (args[1].compareTo("help") == 0)) {
         System.out.print(getHelp());
         return -1;
      } 
      
      if (args[1].compareTo("start") == 0) {
         if ((args.length > 2) && (args[2].compareToIgnoreCase("sim")==0)) {
           System.out.println("Starting Titan in SIMULATION MODE");
           m_Comm = new TitanCommunicateRouter(TitanCommunicate.SIM, null);
           m_Comm.SimAddChannel("TitanTask"); //cl debug
           m_Comm.SimAddChannel("TitanConfig"); //cl debug 
           m_Comm.SimAddChannel("Titan"); //cl debug 
           //m_Comm.SimAddChannel("TitanPacket"); //cl debug 
           //m_Comm.SimAddChannel("TitanComm"); //cl debug 
         } else if ((args.length > 2) && (args[2].compareToIgnoreCase("bt")==0 || args[2].compareToIgnoreCase("bluetooth")==0)) {
           System.out.println("Starting Titan in BLUETOOTH MODE");
           m_MasterRouter = new TitanMasterRouter(TitanCommunicate.BLUETOOTH);
         } else if ((args.length > 2) && (args[2].compareToIgnoreCase("tmote")==0)){
           System.out.println("Starting Titan in TMOTE MODE");
           m_MasterRouter = new TitanMasterRouter(TitanCommunicate.TMOTE);
         } else if ((args.length == 2) || (args[2].compareToIgnoreCase("tcp")==0)){
           System.out.println("Starting Titan in TCP_ONLY MODE");
           m_MasterRouter = new TitanMasterRouter(TitanCommunicate.TCP_ONLY);
         } else {
           System.err.println("Unknown communication interface");
           return 0;
         }

         if (m_MasterRouter != null) {
           m_Comm = m_MasterRouter.getTCom();
         }

         m_Comm.registerListener(new SerialMsg(), this);
         m_ServiceDirectory = new ServiceDirectory(m_Comm);

         System.out.println("Connection established: " + (m_Comm.isConnected()?"SUCCESS":"FAILURE"));

         Performance.begin("titan start");

         return 0;

      ////////////////////////////////////////////////////////////////////////
      // titan router command - alternative to start
      } else if (args[1].compareTo("router") == 0 ) {
        String[] marg = new String[args.length - 2];
        for (int i = 2; i < args.length; i++) {
          marg[i - 2] = args[i];
        }

        final TitanLocalRouter tlr = TitanLocalRouter.create(marg);
        if (tlr == null) {
          System.err.println("could not create TitanLocalRouter");
          return -1;
        }

        m_LocalRouter = tlr;
        m_Comm = tlr.getTCom();
        // if needed, call m_LocalRouter.stop() later to stop the router
        (new Thread( new Runnable() {
           public void run() {
             System.out.println("starting TitanLocalRouter...");
             tlr.run();
           }
         } )).start();

        return 0;

      // all other commands need a tcom, so abort if not available
      } else if (m_Comm == null) {
         System.out.println("Titan Master or Router have not been started yet.");
         System.out.println("Use \"titan start [SIM]\" to start Titan");
         System.out.println(getHelp());

         return -1;
      }

      ////////////////////////////////////////////////////////////////////////
      // LEDTEST
      if (args[1].compareTo("ledtest") == 0) {

         if (args.length != 3) {
            System.out.println("Usage: titan ledtest NODEID");
            return -1;
         }

         Performance.begin("ledtest");

         // set up the configuration messages
         short[] fwdconfig = {(short) ((TC_VERSION << 4) | TITANCOMM_CONFIG), // msg type
            /*128 |*/2, 1, 0, 0,     // task num, conn num, master ID /*with delayed start*/
            (5 << 4) | 2, // config ID, num tasks
            0, 1, 0, 0, // task 0 - SimpleWriter
            0, 4, 1, 0, // task 2 - LEDs
            (5 << 4) | 1, // config ID, num tasks connections
            0, 0, 1, 0, // connection
            0 }; // ending zero

         SerialMsg msg = new SerialMsg(fwdconfig.length + SerialMsg.DEFAULT_MESSAGE_SIZE);

         msg.set_length((short) fwdconfig.length);
         msg.set_address((args[2].compareTo("all") == 0) ? BROADCAST_ADDR : Integer.parseInt(args[2]));
         msg.set_data(fwdconfig);

         System.out.println("Sending configuration message 1/1 (" + fwdconfig.length + " bytes) ...");
         if (m_Comm.send(0, msg)) {
            System.out.println("ok");
         } else {
            System.out.println("failed");
         }

      ////////////////////////////////////////////////////////////////////////
      // Test delayed start
        } else if ( args[1].compareTo("test") == 0 ) {
                
                if ( args.length != 3 ) { System.out.println( "Usage: titan ledtest NODEID" );	return -1;}
                
                // set up the configuration messages
                short [] fwdconfig = 
                { (short)((TC_VERSION << 4) | TITANCOMM_CONFIG), // msg type
                        5, 6, 0, 0,     // task num, conn num, master ID /*with delayed start*/
                        (2<<4)|1,       // config ID, num tasks
                        0, 0, 
                };
                
                SerialMsg msg = new SerialMsg(fwdconfig.length+SerialMsg.DEFAULT_MESSAGE_SIZE);
                
                msg.set_length((short)fwdconfig.length);
                msg.set_address((args[2].compareTo("all")==0)? BROADCAST_ADDR : Integer.parseInt(args[2]));
                msg.set_data(fwdconfig);
                
                System.out.print("Sending configuration message 1/1 ("+fwdconfig.length+" bytes) ...");
                m_Comm.send( 0, msg );
            System.out.println("ok");

      ////////////////////////////////////////////////////////////////////////
      // CFGStart
      } else if (args[1].compareTo("cfgstart") == 0) {

            if ( args.length != 3 ) { System.out.println( "Usage: titan cfgstart CONFIGID" );	return -1;}
            
            // set up the configuration messages
            short [] fwdconfig = 
            { (short)((TC_VERSION << 4) | TITANCOMM_CFGSTART), // msg type
            		0,0,(short)Integer.parseInt(args[2]),
            };
            
            SerialMsg msg = new SerialMsg(fwdconfig.length+SerialMsg.DEFAULT_MESSAGE_SIZE);
            
            msg.set_length((short)fwdconfig.length);
            msg.set_address(BROADCAST_ADDR); // always broadcast
            msg.set_data(fwdconfig);
            
            System.out.print("Sending configuration start message ("+fwdconfig.length+" bytes) ...");
            m_Comm.send( 0, msg );
            System.out.println("ok");

      ////////////////////////////////////////////////////////////////////////
      // DATATEST
      } else if (args[1].compareTo("datatest") == 0) {

         if (args.length != 3) {
            System.out.println("Usage: titan datatest NODEID");
            return -1;
         }

         // set up the configuration messages
         short[] fwdconfig = {(short) ((TC_VERSION << 4) | TITANCOMM_CONFIG), // msg type
            2, 1, 0, 0, // task num, conn num, master ID
            (5 << 4) | 2, // config ID, num tasks
            0, 1, 0, 0, // task 0 - SimpleWriter
            0, 0, 1, 3,0,0,(short)((args[2].compareTo("all")==0)? 0 : Integer.parseInt(args[2])-1), // task 1 - Communicator
            (5 << 4) | 1, // config ID, num tasks connections
            0, 0, 1, 0, // connection
            0};

         SerialMsg msg = new SerialMsg(fwdconfig.length + SerialMsg.DEFAULT_MESSAGE_SIZE);

         msg.set_length((short) fwdconfig.length);
         msg.set_address(Integer.parseInt(args[2]));
         msg.set_data(fwdconfig);

         System.out.println("Sending configuration message 1/1 (" + fwdconfig.length + " bytes) ...");
         m_Comm.send(0, msg);
         System.out.println("ok");

      ////////////////////////////////////////////////////////////////////////
      // ADCTEST
      } else if (args[1].compareTo("adctest") == 0) {

         if (args.length < 3 || args.length > 4) {
            System.out.println("Usage: titan adctest NODEID [SAMPLEPERIOD]");
            return -1;
         }

         short port = (short) Integer.parseInt(args[2]);
         short sampleh = 1; // sampleperiod = 256 = 1/4 second
         short samplel = 0;
         if (args.length == 4) {
            sampleh = (short) (Integer.parseInt(args[3]) / 256);
            samplel = (short) (Integer.parseInt(args[3]) % 256);
         }

         // set up the configuration messages
         short[] fwdconfig = {(short) ((TC_VERSION << 4) | TITANCOMM_CONFIG), // msg type
            2, 1, 0, 0, // task num, conn num, master ID
            (5 << 4) | 2, // config ID, num tasks
            0, 18, 0, 3, sampleh, samplel, 3, // task 0 - Accelerometer Samplerate sampleh*256+samplel, 3 samples per packet
            0, 0, 1, 3, 0, 0, port, // task 1 - Communicator
            (5 << 4) | 1, // config ID, num tasks connections
            0, 0, 1, 0, // connection
            0};
            
         SerialMsg msg = new SerialMsg(fwdconfig.length+SerialMsg.DEFAULT_MESSAGE_SIZE);

            
            msg.set_length((short)fwdconfig.length);
            msg.set_address(Integer.parseInt(args[2]));
            msg.set_data(fwdconfig);
            
            System.out.print("Sending configuration message 1/1 (" + fwdconfig.length + " bytes) ...");
            m_Comm.send( 0, msg );
            System.out.println("ok");
            
            ////////////////////////////////////////////////////////////////////////
            // CACHETEST
        } else if ( args[1].compareTo("cachetest") == 0 ) {
        	
            if ( args.length != 3 ) { System.out.println( "Usage: titan cachetest NODEID" );	return -1;}

            short [] fwdconfig = 
            { (short)((TC_VERSION << 4) | TITANCOMM_CACHE_START), // msg type
                    2, 1, 0, 0,     // task num, conn num, master ID
                    (5<<4)|0, // configID
                    0
            };

         SerialMsg msg = new SerialMsg(fwdconfig.length + 3);

         msg.set_length((short) fwdconfig.length);
         msg.set_address(Integer.parseInt(args[2]));
         msg.set_data(fwdconfig);

         System.out.println("Sending configuration message 1/1 (" + fwdconfig.length + " bytes) ...");
         m_Comm.send(0, msg);
         System.out.println("ok");

      ////////////////////////////////////////////////////////////////////////
      // CLEARCONFIG
      } else if (args[1].compareTo("clear") == 0) {

         if (args.length != 3) {
            System.out.println("Usage: titan clear NODEID");
            return -1;
         }

         Performance.begin("Clear configuration");

         // set up the configuration messages
         short[] fwdconfig = {
            (short) ((TC_VERSION << 4) | TITANCOMM_CONFIG), // msg type
            0, 0, 0, 0, // task num, conn num, master ID
            0, 0  // indicate nothing more
         };


         SerialMsg msg = new SerialMsg(fwdconfig.length + SerialMsg.DEFAULT_MESSAGE_SIZE);

         msg.set_length((short) fwdconfig.length);
         msg.set_address((args[2].compareTo("all") == 0) ? BROADCAST_ADDR : Integer.parseInt(args[2]));
         msg.set_data(fwdconfig);

         System.out.println("Sending configuration message 1/1 (" + fwdconfig.length + " bytes) ...");
         if (m_Comm.send(0, msg)) {
            System.out.println("ok");
         } else {
            System.out.println("failed");
         }

      ////////////////////////////////////////////////////////////////////////
      // SERVICES - list 
      } else if (args[1].compareTo("services") == 0) {

         m_ServiceDirectory.printInfo(System.out);

      ////////////////////////////////////////////////////////////////////////
      // LOAD - loads a configuration from a file
      } else if (args[1].compareTo("load") == 0) {

         //if ( args.length != 3 ) { System.out.println( "Usage: titan load FILENAME" );	return -1;}

         if (args.length != 3) {
            System.out.println("Usage: titan load FILENAME");
            return -1;
         }

         Performance.begin("titan load \"" + args[2] + "\"");
         // read the configuration
         ConfigReader cfgFile;
         try {
            cfgFile = new ConfigReader(args[2]);
            System.out.println("Configuration read:");
            System.out.println("Tasks      : " + cfgFile.numTasks());
            System.out.println("Connections: " + cfgFile.numConnections());
         } catch (FileNotFoundException e) {
            System.out.println("ERROR: titan load: error: could not open file:" + args[2]);
            return -1;
         }

         if (cfgFile.hasError()) {
            return -1;
         }
         
         Performance.printEvent("titan load - parsed configuration file");

         // instantiate a network manager to run the task network
         NetworkManager nm = new NetworkManager(m_msgDispatcher, m_ServiceDirectory, m_Comm);
         //nm.m_bDebugOutput = false;
         int iConfigNumber = m_NetworkManagers.size()+1;
         m_NetworkManagers.put(new Integer(iConfigNumber), nm);
         try {
            nm.start(iConfigNumber, cfgFile.getTaskNetwork());
            m_msgDispatcher.addObserver(nm);
         } catch (Exception e) {
            // do a handling of different exception types
            System.err.println("General ERROR in NetworkManager.start: " + e.getMessage());
            e.printStackTrace();
         }
         Performance.end("titan load \"" + args[2] + "\"");
         System.out.println("Started network manager with ID " + iConfigNumber);


      ////////////////////////////////////////////////////////////////////////
      // STOP - stops a network manager
      } else if (args[1].compareTo("stop") == 0) {

         if (args.length != 3) {
            System.out.println("Usage: titan stop ID");
            return -1;
         }

         // remove the item
         NetworkManager nm = (NetworkManager) m_NetworkManagers.remove(new Integer(Integer.parseInt(args[2])));
         if (nm == null) {
            System.out.println("Could not find configuration " + args[2]);
            return -2;
         }

         // stop the execution
         nm.stop(m_msgDispatcher);

      ////////////////////////////////////////////////////////////////////////
      // RUN - runs an application
      } else if (args[1].compareTo("app") == 0) {

         if (args.length != 3) {
            System.out.println("Usage: titan app NAME");
            return -1;
         }

         Application app = (Application) m_Applications.get(args[2].toLowerCase());

         if (app == null) {
            System.out.println("Application \"" + args[2] + "\" not found");
            return -1;
         }

         if (!app.startApplication(m_Comm, m_ServiceDirectory, m_msgDispatcher)) {
            System.err.println("Failed to start application");
            return -1;
         }

      ////////////////////////////////////////////////////////////////////////
      // GET - download new application
      } else if (args[1].compareTo("get") == 0) {

         if (args.length != 3) {
            System.out.println("Usage: titan get NAME");
            return -1;
         }

         // download and instantiate
         Performance.begin("titan get \"" + args[2] + "\"");
         
         System.out.println("Contacting application server...");
         Application app = ClientGetService.getService(args[2]);

         // check output
         if (app == null) {
            System.out.println("ERROR: Could not get application \"" + args[2] + "\"");
            return -1;
         }

         Performance.end("titan get \"" + args[2] + "\"");
         System.out.println("Registering application...");
         m_Applications.put(app.getName().toLowerCase(), app);

         System.out.println("Application " + app.getName() + " successfully added. You can start it with \"titan app " + app.getName().toLowerCase() + "\"");

      ////////////////////////////////////////////////////////////////////////
      // DATA
      } else if (args[1].compareTo("data") == 0) {

         if (args.length < 4) {
            System.out.println("Usage: titan data DESTINATION PORT [DATA0 [DATA1 ...]]");
         }

         try {

               short[] fwdconfig = new short[args.length ];

               int i=0;

               fwdconfig[i++] = (TC_VERSION << 4) | TITANCOMM_DATAMSG; // msg type
               fwdconfig[i++] = (short) Integer.parseInt(args[3]); // destination port on other device
               fwdconfig[i++] = (short) (args.length - 4); // data length

               for (int j = 4; j < args.length; j++) {
                  fwdconfig[i++] = (short) Integer.parseInt(args[j]);
               }

               SerialMsg msg = new SerialMsg(fwdconfig.length + SerialMsg.DEFAULT_MESSAGE_SIZE);

               msg.set_length((short) fwdconfig.length);
               msg.set_address(Integer.parseInt(args[2]));
               msg.set_data(fwdconfig);
//               msg.set_type(SerialMsg.AM_TITANCOMMDATAMSG);

               System.out.println("Sending data message to (" + args[2] + "," + args[3] + ") with " + fwdconfig.length + " bytes...");
               m_Comm.send(0, msg);
               System.out.println("ok");

//               short[] fwdconfig = new short[3 + args.length - 4];
//               int i = 0;
//               fwdconfig[i++] = (TC_VERSION << 4) | TITANCOMM_DATAMSG; // msg type
//               fwdconfig[i++] = (short) Integer.parseInt(args[3]); // destination port on other device
//               fwdconfig[i++] = (short) (args.length - 4); // data length
//
//               for (int j = 4; j < args.length; j++) {
//                  fwdconfig[i++] = (short) Integer.parseInt(args[j]);
//               }
//
//               SerialMsg msg = new SerialMsg(fwdconfig.length + 3);
//
//               msg.set_length((short) fwdconfig.length);
//               msg.set_address(Integer.parseInt(args[2]));
//               msg.set_data(fwdconfig);
//               msg.set_type(SerialMsg.AM_TITANCOMMDATAMSG);
//
//               System.out.println("Sending data message to (" + args[2] + "," + args[3] + ") with " + fwdconfig.length + " bytes...");
//               m_Comm.send(0, msg);
//               System.out.println("ok");

         } catch (Exception e) {
            System.out.println("ERROR when parsing input: " + e.getLocalizedMessage());
            return -1;
         }



      ////////////////////////////////////////////////////////////////////////
      // SIM - all kinds of simulation stuff
      } else if (args[1].compareTo("sim") == 0 ) {
      
         if ( args.length < 3 ) { System.out.println( "Usage: titan sim COMMAND\n\n       Where command can be:\n         RUN X run X seconds" );   return -1;}
         
         if ( args[2].compareTo("run") == 0 ) {
            if (args.length != 4) {System.out.println("Usage: titan sim run SECONDS"); return -1;}
            m_Comm.SimContinue(Float.parseFloat(args[3]));
         } else if (args[2].toLowerCase().compareTo("addchannel") == 0) {
            if (args.length != 4) {System.out.println("Usage: titan sim addchannel NAME"); return -1;}
            m_Comm.SimAddChannel(args[3]);
         } else {
            System.out.println("Unknown SIM command");
         }

      ////////////////////////////////////////////////////////////////////////
      // NODE - titan local node commands
      } else if (args[1].compareTo("node") == 0 ) {
        if (args.length < 3) {
          System.out.println("Usage: titan node COMMAND\n\nWhere COMMAND can be:\n   start [<node_id> <tcp_port>]\n   stop [<node_id>]");
          return -1;
        }

        // start local node execution engine
        else if (args[2].compareTo("start") == 0) {
          int node_id = -1;
          int tcp_port = -1;

          if (args.length < 4) {
            node_id = TitanLocalNode.LOCAL_NODE_ID_BASE + m_TitanLocalNodes.size();
            if (m_LocalRouter != null)
              node_id += m_LocalRouter.getNetID() << TitanLocalRouter.CLIENT_BITS;
          }
          else {
            node_id = Integer.parseInt(args[3], 10);
          }

          if (args.length < 5) {
            int addp = node_id;
            // uncomment for normal operation, when each router has its own ip,
            // i.e. same ports possible for same local node client id
            if (m_LocalRouter != null)
              addp -= m_LocalRouter.getNetID() << TitanLocalRouter.CLIENT_BITS;
            tcp_port = TitanLocalNode.LOCAL_NODE_TCP_PORT_BASE + addp;
          }
          else {
            tcp_port = Integer.parseInt(args[4], 10);
          }

          if (TitanLocalNode.getNodeById(m_TitanLocalNodes, node_id) == null) {
            TitanLocalNode tln = new TitanLocalNode(node_id, tcp_port);
            if (tln.start()) {
              m_TitanLocalNodes.add(tln);
              Thread.yield();  // let server thread go into accept(), if it is not already
              System.out.println("started local node (" + node_id + ") on tcp port " + tcp_port);
              m_Comm.TCPConnect(node_id, "localhost", tcp_port);
            }
            else {
              System.err.println("start of local node failed!");
            }
          }
          else {
            System.err.println("local node id = " + node_id + " already in use");
          }
        }

        // stop local node execution engine
        else if (args[2].compareTo("stop") == 0) {
          if (!m_TitanLocalNodes.isEmpty()) {
            int node_id = -1;
            TitanLocalNode tln = null;
            if (args.length < 4) {
              tln = (TitanLocalNode) m_TitanLocalNodes.get(m_TitanLocalNodes.size() - 1);
              node_id = tln.getNodeId();
            }
            else {
              node_id = Integer.parseInt(args[3], 10);
              tln = TitanLocalNode.getNodeById(m_TitanLocalNodes, node_id);
            }

            if (tln != null) {
              tln.stop();
              m_TitanLocalNodes.remove(tln);
              m_Comm.TCPDisconnect(node_id);
            }
            else {
              System.err.println("local node id = " + node_id + " not started");
            }
          }
          else {
            System.err.println("stop failed: no local node is running");
          }
        }

        else {
          System.out.println("Unknown NODE command");
        }

      ////////////////////////////////////////////////////////////////////////
      // STRESS - executes a stress test
      } else if (args[1].compareTo("stress") == 0) {
         int factor = 1;
         int inc = 0;
         int start = 1;
         int wait = 10;

         if (args.length == 6) {
           start = Integer.parseInt(args[2], 10);
           inc = Integer.parseInt(args[3], 10);
           factor = Integer.parseInt(args[4], 10);
           wait = Integer.parseInt(args[5], 10);
         }
         else {
           System.out.println("Usage: titan stress <start> <inc> <factor> <wait in s>");
         }

         final String stress_txt = "stress.txt";
         String loadArgs[] = { "titan", "load", stress_txt };
         String stopArgs[] = { "titan", "stop", "1" };
         String nodeStartArgs[] = { "titan", "node", "start" };
         //String nodeStopArgs[] = { "titan", "node", "stop" };

         int nodes_actv = 0;
         for (int n = start; true; n = factor * n + inc) {
           String cfgText = titancommon.node.tasks.test.StressTest.getConfig(n);

           try {
             FileWriter fw = new FileWriter(stress_txt, false);
             fw.write(cfgText);
             fw.close();
           }
           catch (IOException ioe) {
             System.err.println("could not open file for writing: " + stress_txt);
             System.err.println(ioe.getLocalizedMessage());
           }

           int nodes = ((n - 1) / 3) + 2;
           for (int i = 0; i < (nodes - nodes_actv); i++) {
             execute(nodeStartArgs);
           }
           nodes_actv = nodes;

           System.out.println("loading config for n = " + n);
           execute(loadArgs);

           try { Thread.sleep(wait * 1000); }
           catch (InterruptedException ie) { }

           execute(stopArgs);

           try { Thread.sleep(500); }
           catch (InterruptedException ie) { }
         }

      } else {
         System.out.println("Don't know what to do");
      }

      return 0;
   }

   public String toString() {
      return "titan";
   }
   ////////////////////////////////////////////////////////////////////////////
   // MessageListener argument
   /**
    * Called by TitanDataComm to indicate that a new message has been received. 
    * This function determines which Titan message type the message is and calls 
    * the corresponding handler.
    * 
    * @param addr Message destination
    * @param msg  The TinyOS message structure received
    */
   public void messageReceived(int addr, Message msg) {

      SerialMsg SMsg = (SerialMsg) msg;
      int saddr = SMsg.get_address();

      //System.out.println("Master msgRcv - dest: " + saddr);
      
      // saddr 0: net_id = 0, MasterRouter
      if (saddr != 0) {
        m_MasterRouter.messageReceived(addr, msg);
        return;
      }

      switch ((SMsg.getElement_data(0) & 0xF)) {
         case TITANCOMM_CONFIG: // this message should not be received
            System.out.println("Configuration message received!");
            break;
         case TITANCOMM_CFGTASK: // this message should not be received
            System.out.println("Task message received!");
            break;
         case TITANCOMM_CFGCONN: // this message should not be received
            System.out.println("Connection message received!");
            break;
         case TITANCOMM_CFGSUCC:

            Performance.end("Configuration done");
            short [] sdata = new short[4];
            sdata[0] = SMsg.getElement_data(0);
            sdata[1] = SMsg.getElement_data(1);
            sdata[2] = SMsg.getElement_data(2);
            sdata[3] = SMsg.getElement_data(3);

            short iNodeAddr = (short)(sdata[2]*256 + sdata[3]);
            short iConfig   = sdata[1];
            

            // print on console
            System.out.println("Configuration " + iConfig + " successfully loaded onto node " + iNodeAddr );

            // notify the service directory about what node is alive
            m_ServiceDirectory.nodeIsAlive(iNodeAddr);

            // notify anybody interested in messages
            ConfigurationSuccessMsg csm = new ConfigurationSuccessMsg(iConfig, iNodeAddr);
            m_msgDispatcher.sendMessage(csm);

            break;
         case TITANCOMM_DISCOVER: // this shoul not be received
            System.out.println("Discovery message received!");
            break;
         case TITANCOMM_DICS_REP: // forward to service directory
            m_ServiceDirectory.messageReceived(addr, SMsg);
            break;
         case TITANCOMM_DATAMSG:

            // extract message content and send to DataComm
            short iPort = SMsg.getElement_data(1);
            short iLength = SMsg.getElement_data(2);
            byte[] data = new byte[iLength];
            for (int i = 0; i < iLength; i++) {
               data[i] = (byte) SMsg.getElement_data(i + 3);
            }
//                m_DataCom.processMessage( iPort, data );

            // try to figure out the node address of the sending node by asking 
            // the running network managers whether one of their nodes is sending 
            // data to the master node.
            // This info can be used by the service directory to keep it alive
/*                for ( Iterator<Integer> nmiter = m_NetworkManagers.keySet().iterator(); nmiter.hasNext(); ) {
            int iAddr = m_NetworkManagers.get(nmiter.next()).getNodeAddrByPort(iPort);
            if ( iAddr != -1 ) m_ServiceDirectory.nodeIsAlive(iAddr);
            }
             */

            DataMsg dmsg = new DataMsg(iPort, data);
            dmsg.rssi = SMsg.get_rssi();
            m_msgDispatcher.sendMessage(dmsg);

            break;

         case TITANCOMM_ERROR:
            short iNodeID = (short) ((((int) SMsg.getElement_data(1)) << 8) | (((int) SMsg.getElement_data(2)) & 0xFF));
            short iConfigID = SMsg.getElement_data(3);
            short iErrSource = SMsg.getElement_data(4);
            short iErrType = SMsg.getElement_data(5);
            if (0 <= iErrType && iErrType < TITAN_ERROR_CODES.length) {
               System.out.println("\nReceived error from node " + iNodeID + "(" + iConfigID + "): Source: " + iErrSource + " Error: " + TITAN_ERROR_CODES[iErrType]);
            } else {
               System.out.println("\nReceived error from node " + iNodeID + "(" + iConfigID + "): Source: " + iErrSource + " Error: " + iErrType);
            }

            m_ServiceDirectory.nodeIsAlive(iNodeID);

            ErrorMsg emsg = new ErrorMsg(iNodeID, iConfigID, iErrSource, iErrType);
            m_msgDispatcher.sendMessage(emsg);

            break;

         default:
            System.out.println("Unknown message received!");
      }

   }

}
