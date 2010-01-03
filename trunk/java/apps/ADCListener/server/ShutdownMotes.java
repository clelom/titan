package server;
import java.util.Vector;

/**
 * Shutdown-Thread to stop all nodes
 * @author Benedikt Kšppel
 */
public class ShutdownMotes extends Thread{
	
	/**
	 * vector of all nodes
	 */
	private Vector<Mote> nodes;

	/**
	 * Constructor of Shutdown-Thread
	 * @param nodes all nodes which were started, and must be stopped at the end of the program
	 */
	public ShutdownMotes(Vector<Mote> nodes) {
		this.nodes = nodes;
		System.out.println("Created shutdown thread.");
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Thread#run()
	 */
	public void run() {
		System.out.println("Shutting down...");
		
		/*
		 * shutdown all nodes
		 * FIXME: this might not work, if Listener.java has still an open socket!
		 */
		for ( int i=0; i<nodes.size(); i++) {
			System.out.println("Stopping node " + nodes.get(i).getNodeAddress().getHostAddress() );
			nodes.get(i).stopADC();
			System.out.println("...stopped");
		}
	}
}
