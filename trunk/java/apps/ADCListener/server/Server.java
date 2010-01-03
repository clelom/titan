package server;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Formatter;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Vector;
import utils.FIFO;
import filters.*;


/**
 * The server which will listen for incoming UDP-packets from ADCUDPEcho, sort it, process it and print it into a logfile
 * @author Benedikt Kšppel
 */
public class Server {
	
	/**
	 * port number, on which this server is listening
	 */
	private static int portNumber = 0;
	
	/**
	 * number of samples per packet
	 */
	private static int samplesPerPacket = 0;
	
	/**
	 * period between two samples in milliseconds
	 */
	private static int samplingPeriod;
	
	/**
	 * address of this server
	 */
	private static InetAddress myAddress;
	
	/**
	 * vector containing all nodes
	 */
	private static Vector<Mote> nodes;

	/**
	 * Starts the server
	 * @param args command line arguments: <my local ip> <port number> <sampling period> [ <nodes > ]*
	 * @throws FileNotFoundException if the server couldn't create the logfile
	 * @throws UnknownHostException if something with the InetAddress went wrong
	 * TODO: pack all used classes with jar:
	 * 			cd Eclipse/workspace/ADCServer/bin
	 * 			echo 'Main-Class: ch.ethz.ee.adcserver.Server' > Manifest.txt
	 * 			jar cmf Mainfest.txt ADCServer.jar ch/
	 * Start server with: java -jar ADCServer.jar <my local ip> <port number> <sampling period> [ <nodes > ]*
	 */
	public static void main( String args[] ) throws FileNotFoundException, UnknownHostException {
		
		nodes = new Vector<Mote>();
		
		samplesPerPacket = 10;
		
		if ( args.length >= 3 ) {
			myAddress = InetAddress.getByName(args[0]);
			portNumber = Integer.parseInt(args[1]);
			samplingPeriod = Integer.parseInt(args[2]);
			
			for ( int i=3; i<args.length; i++) {
				Mote node = new Mote(myAddress, InetAddress.getByName(args[i]), samplingPeriod, 61616);
				nodes.add( node );
			}
			
		} else {
			System.out.println("Usage: java -jar ADCServer.jar <my local ip> <port number> <sampling period> [ <nodes > ]*");
			System.exit(1);
		}

		System.out.println("Server starting...");
		
		/*
		 * print stream for file output
		 * generate a file 'datastream_yyyyMMdd_HHmmss.txt'
		 */
		Date startTime;
		startTime = new Date();
		
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
		dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		String logFilename = "datalog_" + dateFormat.format(startTime) + ".txt";
		
		PrintStream logFile = new PrintStream(new FileOutputStream(logFilename));
		Formatter logFormatter = new Formatter(logFile, Locale.US);
		    
		/*
		 * FIFO for storing all incoming packets
		 */
		FIFO<XYZPacket> packetStorage = new FIFO<XYZPacket>();
		
		/*
		 * create all Filters
		 * create a FilterBank
		 * add all Filters into the FilterBank
		 */
		
		/*
		 * timeFilter will fail, if a packet comes too late
		 */
		TimeFilter timeFilter = new TimeFilter();
		timeFilter.init(startTime.getTime(), 1000);
		
		/*
		 * duplicateFilter will fail, if a packet was already processed
		 */
		DuplicateFilter duplicateFilter = new DuplicateFilter();
		

		FilterBank<XYZPacket> filterBank = new FilterBank<XYZPacket>();		
		filterBank.addFilter( timeFilter );
		filterBank.addFilter( duplicateFilter );
		
		/*
		 * create the thread which processes the data from the fifo 
		 */
		FileWriter writer = new FileWriter( packetStorage, logFormatter, filterBank );
		writer.start();
		writer.setPriority(Thread.MIN_PRIORITY);
		System.out.println("Started thread " + writer.getName() + ": ID " + writer.getId() + ", priority " + writer.getPriority() + ", state " + writer.getState() );

		
		
		/*
		 * create a server thread, and start it
		 */
		Listener listener = new Listener(portNumber, samplesPerPacket, samplingPeriod, packetStorage);
		listener.start();
		listener.setPriority(Thread.MAX_PRIORITY);
		System.out.println("Started thread " + listener.getName() + ": ID " + listener.getId() + ", priority " + listener.getPriority() + ", state " + listener.getState() );
		
		/*
		 * start all nodes
		 */
		for ( int i=0; i<nodes.size(); i++) {
			System.out.println("Starting node " + nodes.get(i).getNodeAddress().getHostAddress() );
			nodes.get(i).startADC();
			System.out.println("...started");
		}
		
		/*
		 * create thread which will be executed on ctrl-c / SIGKILL
		 */
		ShutdownMotes shutdown = new ShutdownMotes(nodes);
		Runtime.getRuntime().addShutdownHook( shutdown ); 
		System.out.println("Created shutdown hook.");
	}
}
