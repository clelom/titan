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
 * This is the controlling service for the Farkle application. It keeps the 
 * task graphs for the application and exchanges them. The outputs of the 
 * WSN are used to make decisions about the next steps, reconfigurations, and 
 * similar.
 * 
 * @date 16 July 2008
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 * 
 */
package titancommon.applications;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Observable;

import java.util.Vector;
import titan.ConfigReader;
import titancommon.Connection;
import titan.TitanCommunicate;
import titancommon.execution.NetworkManager;
import titancommon.execution.TaskNetwork;
import titancommon.execution.TaskNetwork.NodeConfiguration;
import titancommon.messages.DataMsg;
import titancommon.messages.MessageDispatcher;
import titancommon.services.ServiceDirectory;
import titancommon.tasks.Task;

public class Dice implements Application {

	public static final int[] ORIG_DICE = {11,12,13,14};
	public static final int DICE = ORIG_DICE.length;

        
        private static final int PICKUP_LIMIT_MIN = 3;
	private static final int PICKUP_LIMIT = 10;
	private static final int SHAKE_LIMIT = 5;
	private static final int ROLL_LIMIT = 2;
	private static final int SCORE_LIMIT = 2;
	
	// Important Titan objects
	TitanCommunicate m_com;
	ServiceDirectory m_sd;
	MessageDispatcher m_md; 
        
        // TaskNetwork templates read from configuration
							// files
	TaskNetwork m_pickup;
	TaskNetwork m_shake;
	TaskNetwork m_roll;
	TaskNetwork m_score; // GUI
	DiceFrame m_frame = new DiceFrame(this); // / task network adapted to the
												// current network
	TaskNetwork m_current; // / the network manager handling the current network
	NetworkManager m_netManager = null;
	int[] m_diceValues = new int[DICE];
	boolean[] m_participating = new boolean[DICE];
	int[] m_partDice = (int[]) ORIG_DICE.clone();
        int[] m_partPos;
	int m_iCurNetwork = 1;

	int [] m_playerScores = {0,0};
	int m_curPlayer;
        String m_curPlayerName="";
        
        boolean m_scoreOnly=false;     
        boolean m_isRestarting=false;
	
	// ////////////////////////////////////////////////////////////////////////
	// State machine data
	private final short FSM_INIT = 0;
	private final short FSM_PICKUP = 1;
	private final short FSM_SHAKE = 2;
	private final short FSM_THROW = 3;
	private final short FSM_SCORE = 4;
	private short m_state;
	/**
	 * maps ports of incoming data to their original node sources this is needed
	 * in the initial stage, when nodes are picked up.
	 */
	int[] m_portMapping = new int[DICE];
	int[] m_eventCounter;

	public boolean m_bDebugOutput = true;

	/** Unique Service ID of Farkle */
	public int getUSID() {
		return 133;
	}

	/** The name of the game ;-) */
	public String getName() {
		return "Dice";
	}

	/**
	 * Loads all task graphs needed for the execution of the farkle game
	 * 
	 * @return True if all task graphs have successfully been loaded.
	 */
	private boolean loadTaskGraphs() {
		ConfigReader cfgFile;
		String strCurFile = "";
		String strPath = "cfg/dice/";

		// read the configuration files
		try {
			strCurFile = strPath + "pickup.cfg";
			cfgFile = new ConfigReader(strCurFile);
			if (cfgFile.hasError()) {
				return false;
			}
			m_pickup = cfgFile.getTaskNetwork();

			strCurFile = strPath + "shake.cfg";
			cfgFile = new ConfigReader(strCurFile);
			if (cfgFile.hasError()) {
				return false;
			}
			m_shake = cfgFile.getTaskNetwork();

			strCurFile = strPath + "roll.cfg";
			cfgFile = new ConfigReader(strCurFile);
			if (cfgFile.hasError()) {
				return false;
			}
			m_roll = cfgFile.getTaskNetwork();

			strCurFile = strPath + "score.cfg";
			cfgFile = new ConfigReader(strCurFile);
			if (cfgFile.hasError()) {
				return false;
			}
			m_score = cfgFile.getTaskNetwork();

		} catch (FileNotFoundException e) {
			System.err.println("ERROR: titan load: error: could not open file:"
					+ strCurFile);
			return false;
		}

		return true;
	}

	/**
	 * Start an application. To proper working, it needs the communication
	 * facilities, a service directory and the message dispatcher to get the
	 * data output
	 * 
	 * @throws Exception
	 */
	public boolean startApplication(TitanCommunicate com, ServiceDirectory sd,
			MessageDispatcher md) {
         
		if (m_netManager != null && !m_isRestarting) {
			System.out.println("NetworkManager already running, opening window");
			m_frame.setVisible(true);
			return true;
		}
                m_isRestarting=false; 
                
                
 		// check input
		if (com == null || sd == null || md == null) {
			return false; // store objects
		}
		m_com = com;
		m_sd = sd;
		m_md = md;

		// load application frame
		m_frame.setDice(0, 0, FarkleFrame.DICE_NEUTRAL);
		m_frame.setDice(1, 0, FarkleFrame.DICE_NEUTRAL);
		m_frame.setDice(2, 0, FarkleFrame.DICE_NEUTRAL);
		m_frame.setDice(3, 0, FarkleFrame.DICE_NEUTRAL);
                m_frame.setTextBold(m_curPlayer);
		m_frame.show();

               
                m_frame.setMessage("Initialisiere Würfelspiel");
                m_curPlayer=0;
                //m_scoreOnly=false;
                m_curPlayerName="Spieler 1: ";
                m_frame.setTextBold(m_curPlayer);
                m_state= FSM_INIT;
                                
                
                for (int i = 0; i < m_diceValues.length; i++)
                   m_diceValues[i] = 0;
                
                m_partDice = (int[]) ORIG_DICE.clone();
                m_eventCounter = new int[DICE];
                
                for(int i=0; i<m_playerScores.length;i++){
                    m_playerScores[i]=0;
                    m_frame.setScore(i, m_playerScores[i]);
                }

                // load all task graphs
		if (!loadTaskGraphs()) {
			System.err.println("Could not load task graphs");
			return false;
		}

		// instantiate network manager
		if (m_netManager == null) {
			m_netManager = new NetworkManager(m_md, sd, com, false);
		} else {
			m_netManager.stop(md);
                        m_netManager = new NetworkManager(m_md, sd, com, false);
		}

		// register to receive data messages
		m_md.addObserver(this);

		System.out.println("Starting Dice game");

		try {
			m_current = composeNetwork(m_score, m_partDice);
		} catch (Exception e) {
			System.err.println("Could not compose network!");
			e.printStackTrace();
			return false;
		}

		// start first network tree
		try {
			m_netManager.m_bDebugOutput = false;
			m_netManager.start(m_iCurNetwork++, m_current);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return true;
	}
        
        public void restart(){
            m_isRestarting=true;
            m_state= FSM_INIT;
            startApplication(m_com,m_sd,m_md);
            m_frame.setScore(m_curPlayer, m_playerScores[m_curPlayer]);
            
        }
        
        public void close() {
        	m_md.deleteObserver(this);
        	m_netManager.stop(m_md);
        	m_netManager = null;
        	
        }
        
        public void scoreOnly(boolean on){
            if(on && !m_scoreOnly){
                m_scoreOnly=true;
                restart();
//                if (loadNextTaskNetwork(m_score)) {
//			m_state = 0;
//			System.out.println("Changed to score mode" );
//		} else {
//			System.err.println("Could not change to score mode!");
//		}
//                m_frame.setMessage("score mode");
                
            }else if (!on && m_scoreOnly){
                m_scoreOnly=false;
                restart();
            }
               
                
            
        }

	private TaskNetwork composeNetwork(TaskNetwork tnOriginal, int[] moteIDs)
			throws Exception {
		TaskNetwork tnNew = (TaskNetwork) tnOriginal.clone();

		// make sure this original has not been compiled
		if (((TaskNetwork.NodeConfiguration) (tnNew.m_Nodes.get(0))).address != -1) {
			throw new Exception(
					"Composing network: First node has not address -1: don't know what to do");
		}

		ArrayList/* <Task> */tasks = new ArrayList(Arrays
				.asList(((NodeConfiguration) tnNew.m_Nodes.get(0)).tasks));
		ArrayList/* <Connection> */connections = new ArrayList(Arrays
				.asList(((NodeConfiguration) tnNew.m_Nodes.get(0)).connections));
		Task currXTask;
		int currXTaskIndex;
		Connection currConnection;

		int conSize = connections.size();
		int orgConSize = connections.size();
		Connection connectionClone;
		Task taskClone;
		int port;
		ArrayList x2XConnectionEndTask = new ArrayList();
		ArrayList x2XConnectionEndStart = new ArrayList();

		// Search all X Tasks and THE Sink Task
		Vector xTasks = new Vector();
		Task taskName;
		int sinkTask = -1;
		for (int i = 0; i < tasks.size(); i++) {
			taskName = (Task) tasks.get(i);
			if (taskName.getAttribute("nodeID").equals("X")) {
				xTasks.add(new Integer(i));
			} else if (taskName.getName().equals("sink")) {
				// If more than one Task is a Sink--> Exception
				if (sinkTask != -1) {
					throw new Exception(
							"Error in config: more than one task is a sink");
				}
				sinkTask = i;
			} else {
				throw new Exception(
						"Error in config: static task is not a sink ("
								+ taskName.getName() + ")");
			}
		}

		// For all XTasks
		for (int i = 0; i < xTasks.size(); i++) {
			currXTask = (Task) tasks.get(((Integer) xTasks.get(i)).intValue());
			currXTaskIndex = ((Integer) xTasks.get(i)).intValue();

			// Clone the X Task
			currXTask.addAttribute("nodeID", Integer.toString(moteIDs[0]));
			for (int j = 1; j < moteIDs.length; j++) {
				taskClone = (Task) currXTask.clone();
				taskClone.addAttribute("nodeID", Integer.toString(moteIDs[j]));
				tasks.add(taskClone);
			}

			// Edit Connections
			conSize = connections.size();
			for (int j = 0; j < conSize; j++) {
				currConnection = (Connection) connections.get(j);

				if (currConnection.StartTask == currXTaskIndex) {

					if (currConnection.EndTask == sinkTask) {

						port = 0;
						for (int k = 0; k < connections.size(); k++) {
							if (((Connection) connections.get(k)).EndTask == currConnection.EndTask
									&& port < ((Connection) connections.get(k)).EndPort) {
								port = ((Connection) connections.get(k)).EndPort;
							}
						}
						port++;

						for (int k = 1; k < moteIDs.length; k++) {
							connectionClone = (Connection) currConnection
									.clone();
							connectionClone.EndPort = port;
							port++;
							connectionClone.StartTask = tasks.size()
									- moteIDs.length + k;
							connections.add(connectionClone);

						}
					} else if (x2XConnectionEndTask.contains(new Integer(j))) {
						throw new Exception(
								"x2XConnectionEndEnd: not yet implemented");
					} else {
						x2XConnectionEndStart.add(new Integer(j));
						for (int k = 1; k < moteIDs.length; k++) {
							connectionClone = (Connection) currConnection
									.clone();
							connectionClone.StartTask = tasks.size()
									- moteIDs.length + k;
							connections.add(connectionClone);
							x2XConnectionEndStart.add(new Integer(connections
									.size() - 1));
						}
					}

				} else if (currConnection.EndTask == currXTaskIndex) {

					if (x2XConnectionEndStart.contains(new Integer(j))) {
						if (j >= orgConSize) {
							for (int k = 1; k < moteIDs.length; k++) {
								connectionClone = (Connection) connections
										.get(j);
								connectionClone.EndTask = tasks.size()
										- moteIDs.length + k;
								j++;
							}
							j--;
						}

					} else {
						throw new Exception(
								"x2XConnectionEndEnd: not yet implemented");
					}
				}
			}
		}

		((NodeConfiguration) tnNew.m_Nodes.get(0)).tasks = (Task[]) tasks
				.toArray(((NodeConfiguration) tnNew.m_Nodes.get(0)).tasks);
		((NodeConfiguration) tnNew.m_Nodes.get(0)).connections = (Connection[]) connections
				.toArray(((NodeConfiguration) tnNew.m_Nodes.get(0)).connections);

		m_portMapping = new int[moteIDs.length];
		m_eventCounter = new int[moteIDs.length];
		int j = 0;
		for (int i = 0; i < connections.size(); i++) {
			if (((Connection) connections.get(i)).EndTask == sinkTask) {
				m_portMapping[j] = Integer.parseInt(((Task) tasks
						.get(((Connection) connections.get(i)).StartTask))
						.getAttribute("nodeID"));
				j++;
			}
		}

		return tnNew;
		// TaskNetwork tnNew = (TaskNetwork) tnOriginal.clone();
		//
		// // make sure this original has not been compiled
		// if (((TaskNetwork.NodeConfiguration) (tnNew.m_Nodes.get(0))).address
		// != -1) {
		// throw new Exception(
		// "Composing network: First node has not address -1: don't know what to do"
		// );
		// }
		//
		// // adapt to network: set nodeIDs
		// Task[] tasks = ((TaskNetwork.NodeConfiguration)
		// tnNew.m_Nodes.get(0)).tasks;
		// for (int i = 0; i < tasks.length; i++) {
		// String str = tasks[i].getAttribute("nodeID");
		//
		// // check the value
		// if (str != null) {
		// if (str.equals("X")) { // wildcard - replace by existing node
		// //TODO: Ask ServiceDirectory for existing nodes in composition stage
		// tasks[i].addAttribute("nodeID", "6");
		// m_portMapping[0] = 2;
		// } else if (str.equals("Y")) {
		// tasks[i].addAttribute("nodeID", "2");
		// m_portMapping[1] = 2;
		// }
		// }
		// }
		//
		// return tnNew;

	}

	/**
	 * Deletes the current configuration from the network and loads the next
	 * one.
	 * 
	 * @param tnNew
	 *            New task graph to be loaded.
	 * @return whether it has been successful
	 */
	private boolean loadNextTaskNetwork(TaskNetwork tnNew) {
		m_netManager.stop(m_md);
		m_netManager.clearAll();
		try {
			Thread.sleep(500);
		} catch (InterruptedException ex) {
		}
		try {
			m_current = composeNetwork(tnNew, m_partDice);
			m_netManager.start(m_iCurNetwork, m_current);
			m_iCurNetwork = (m_iCurNetwork + 1) % 16;
		} catch (Exception e) {
			System.err.println("Failure to switch states!");
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * Message reception comes in here. This is called by the MessageDispatcher
	 * 
	 * @param source
	 *            Source of the message
	 * @param obs
	 *            message (cast to DataMsg)
	 */
	public void update(Observable source, Object obs) {

		if (obs instanceof DataMsg) {
			DataMsg msg = (DataMsg) obs;
			// handle the message
			// System.out.println("Got DataMessage from port " + msg.destPort +
			// " which is node " + m_portMapping[msg.destPort] + " value= " +
			// msg.data[0] );

			// message content depends on FSM state
			switch (m_state) {
			case FSM_INIT:
				handleStateInit(msg);
				break;
			case FSM_PICKUP:
				handleStatePickup(msg);
				break;
			case FSM_SHAKE:
				handleStateShake(msg);
				break;
			case FSM_THROW:
				handleStateThrow(msg);
				break;
			case FSM_SCORE:
				handleStateScore(msg);
				break;
			default:
				System.err.println("\nReached unknown state!!!");
			}

		} else if (obs instanceof FarkleFrame.NextPlayerMsg) {

//			m_frame.setScore(m_curPlayer, 0, m_playerScores[m_curPlayer]);
//			m_curPlayer = (m_curPlayer+1)%PLAYERS;
//			m_farkleScore = new FarkleScore();
//			
//			m_frame.setMessage("Next Player...");
//			
//			// force into pickup state
//			m_netManager.clearAll();
//			if (loadNextTaskNetwork(m_pickup)) {
//				
//				m_state = FSM_PICKUP;
//				System.out.println("Switched to state Pickup");
//			} else {
//				System.err.println("Could not advance from scoring to pickup!");
//			}
//
//			m_eventCounter = new int[DICE];
//			m_partDice = (int[]) ORIG_DICE.clone();
//			m_frame.setMessage("Please pick up dice");				
		}
	}


	private void handleStateInit(DataMsg msg) {

		// check validity of incoming data
		if (msg.destPort < 0 || m_diceValues.length <= msg.destPort) {
			System.err.println("Received score from unknown dice (port="+msg.destPort+")");
			return;
		}

		// store and display received eye count
		m_diceValues[msg.destPort] = msg.data[0];
		m_frame.setDice(msg.destPort, msg.data[0], FarkleFrame.DICE_NEUTRAL);

		// check how many dice have responded:
		int iDone = 0;
		for (int i = 0; i < m_diceValues.length; i++) {
			if (m_diceValues[i] != 0) {
				iDone++;
			}
		}

		// report state 
		m_frame.setMessage("Würfelaugenzahl von " + iDone + "/"+ m_diceValues.length + " bekommen (" + msg.destPort + "/" + msg.data[0] + ")");

		// if all dice have responded, start game - go to PICKUP state
		if (iDone == m_diceValues.length && !m_scoreOnly) {
			m_state = FSM_PICKUP;
			m_eventCounter = new int[DICE];
			//m_farkleScore = new FarkleScore();
			if (loadNextTaskNetwork(m_pickup) == false) {
				System.err.println("Could not switch from INIT to PICKUP");
				m_frame.setMessage("ERROR switching from INIT ot PICKUP");
			} else {
				m_frame.setMessage(m_curPlayerName + "Bitte Würfel aufnehmen");
			}

		}
	}

	private void handleStatePickup(DataMsg msg) {
		// check whether we have a moving event
		if (msg.data[0] == 1) {
			// register node
			if (msg.destPort < m_eventCounter.length) {
				m_eventCounter[msg.destPort]++;
			}

                        if(m_eventCounter[msg.destPort]>PICKUP_LIMIT_MIN) 
                            m_frame.setDice(msg.destPort, m_diceValues[msg.destPort],FarkleFrame.DICE_PICKEDUP);

			if (m_bDebugOutput) {

				String strMessage = "State PICKUP: events = { ";
				for (int i = 0; i < m_eventCounter.length; i++) {
					strMessage += m_eventCounter[i] + " ";
				}
				strMessage += "}";

				System.out.println(strMessage);
				//m_frame.setMessage(strMessage);
			}
			

			// get maximum from eventcounter
			int maxevent = -1;
			for (int i = 0; i < m_eventCounter.length; i++) {
				if (maxevent < m_eventCounter[i])
					maxevent = m_eventCounter[i];
			}

			// check whether it has been moving long enough
			if (maxevent > PICKUP_LIMIT) {
				
				int iParticipating = 0;
				for (int i = 0; i < m_eventCounter.length; i++) {
					if (m_eventCounter[i] < PICKUP_LIMIT_MIN ) {
						//m_frame.setDice(i, m_diceValues[i],DiceFrame.DICE_BLOCKED);
					} else {
						m_participating[i] = true;
                                                m_frame.setDice(i, m_diceValues[i],DiceFrame.DICE_SHAKE);
						iParticipating++;
					}
				}

				m_eventCounter = new int[iParticipating];

				m_partDice = new int[iParticipating];
                                m_partPos=new int[iParticipating];
				int iCur = 0;
				for (int i = 0; i < m_participating.length; i++) {
					if (m_participating[i]) {
						m_partDice[iCur] = ORIG_DICE[i];
                                                m_partPos[iCur]=i;
                                                iCur++;
					}
				}
                            
                            
                            m_state = FSM_SHAKE;

                                
                                
                                
				if (loadNextTaskNetwork(m_shake)) {
					System.out.println("Switched to state Shake");
					m_frame.setMessage(m_curPlayerName + "Bitte Würfel schütteln");
				} else {
					System.err
							.println("Could not advance from pickup to shake!");
				}
			} 
		}
	}
	
	private void handleStateShake(DataMsg msg) {
		// check whether we have a moving event
		if (msg.data[0] == 1) {
			// register node
			if (msg.destPort < m_eventCounter.length) {
				m_eventCounter[msg.destPort]++;
			}

			if (m_bDebugOutput) {

				String strMessage = "State SHAKE: events = { ";
				for (int i = 0; i < m_eventCounter.length; i++) {
					strMessage += m_eventCounter[i] + " ";
				}
				strMessage += "}";

				System.out.println(strMessage);
				//m_frame.setMessage(strMessage);
			}

			boolean bAllShaken = true;
			for (int i = 0; i < m_eventCounter.length; i++) {
				if (m_eventCounter[i] < SHAKE_LIMIT && m_participating[i]) {
					bAllShaken = false;
					break;
				}
			}

			// check whether it has been moving long enough
			if (bAllShaken) {

                            	for (int i = 0; i < m_partDice.length; i++) {
                                            m_frame.setDice(m_partPos[i],1,DiceFrame.DICE_THROW);
				}
				if (loadNextTaskNetwork(m_roll)) {
					m_state = FSM_THROW;
					System.out.println("Switched to state Rolling");
					m_frame.setMessage(m_curPlayerName + "Bitte Würfel werfen");
				} else {
					System.err
							.println("Could not advance from shake to roll!");
				}

				// reset event counter
				m_eventCounter = new int[m_eventCounter.length];

			}
		}
	}

	private void handleStateThrow(DataMsg msg) {
		// check whether we have a moving event
		if (msg.data[0] == 0) {
			// register node
			if (msg.destPort < m_eventCounter.length) {
				m_eventCounter[msg.destPort]++;
			}

			if (m_bDebugOutput) {

				String strMessage = "State THROW: events = { ";
				for (int i = 0; i < m_eventCounter.length; i++) {
					strMessage += m_eventCounter[i] + " ";
				}
				strMessage += "}";

				System.out.println(strMessage);
				//m_frame.setMessage(strMessage);
			}
			boolean bAllThrown = true;
			for (int i = 0; i < m_eventCounter.length; i++) {
				if (m_eventCounter[i] < ROLL_LIMIT && m_participating[i]) {
					bAllThrown = false;
					break;
				}
			}

			// check whether it has been moving long enough
			if (bAllThrown) {

				//m_partDice = (int[]) ORIG_DICE.clone();
				if (loadNextTaskNetwork(m_score)) {
					m_state = FSM_SCORE;
					System.out.println("Switched to state Rolling");
					m_frame.setMessage(m_curPlayerName + "Punkte werden ermittelt...");
				} else {
					System.err
							.println("Could not advance from rolling to scoring!");
				}

				//m_eventCounter = new int[DICE];
			}
		} // if msg.data[0] == 0
	}
	
	private void handleStateScore(DataMsg msg) {

		if (msg.data[0] == 0 || msg.data.length != 2) {
			System.out.println("Reconfiguration didn't work!");
			m_netManager.clearAll();
			return;
		}
                
                

		m_frame.setDice(m_partPos[msg.destPort], msg.data[0],DiceFrame.DICE_SCORE);
		m_diceValues[m_partPos[msg.destPort]] = msg.data[0];
                

		m_eventCounter[msg.destPort]++;

		if (m_bDebugOutput) {

			String strMessage = "State SCORE: events = { ";
			for (int i = 0; i < m_eventCounter.length; i++) {
				strMessage += m_eventCounter[i] + " ";
			}
			strMessage += "}";

			System.out.println(strMessage);
			//m_frame.setMessage(strMessage);
		}

		boolean allScored = true;
		for (int i = 0; i < m_eventCounter.length; i++) {
			if (m_eventCounter[i] < SCORE_LIMIT && m_participating[i]) {
				allScored = false;
				break;
			}
		}

		if (allScored) {
			
			String strMessage = "State SCORE: dice eyes show = { ";
			for (int i = 0; i < m_diceValues.length; i++) {
				strMessage += m_diceValues[i] + " ";
			}
			strMessage += "}";
		
                        
                        for(int i=0; i<m_partDice.length;i++)
                            m_playerScores[m_curPlayer]= m_playerScores[m_curPlayer]+m_diceValues[m_partPos[i]];
                        
                        m_frame.setScore(m_curPlayer, m_playerScores[m_curPlayer]);
                           
                        
                        
                        if(m_curPlayer==1){
                            m_curPlayer=0;
                            m_curPlayerName="Spieler 1: ";
                        }
                        else{
                            m_curPlayer=1;
                            m_curPlayerName="Spieler 2: ";
                        }
                        
                        m_frame.setTextBold(m_curPlayer);
                        
                        for (int i = 0; i < m_diceValues.length; i++)
                                m_diceValues[i] = 0;
	
                
                        m_partDice = (int[]) ORIG_DICE.clone();
                        m_eventCounter = new int[DICE];
                        for(int i=0; i<m_participating.length; i++)
                            m_participating[i]=false;
                        
			if (loadNextTaskNetwork(m_score)) {
				m_state = 0;
				System.out.println("Switched to state Pickup");
			} else {
				System.err.println("Could not advance from scoring to pickup!");
			}

			m_eventCounter = new int[DICE];
			m_partDice = (int[]) ORIG_DICE.clone();
			m_frame.setMessage(m_curPlayerName + "Bitte Würfel aufnehmen");
		}
		
	}
}
