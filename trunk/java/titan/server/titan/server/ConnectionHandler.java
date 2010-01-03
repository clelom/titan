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
 * ConnectionHandler handles a connection from a client. This class implements a simple HTTP server supporting 
 * GET and POST commands. It is instantiated from ServiceServer upon connection from a client.
 * 
 * GET allows to download resources, such as application classes or TaskGraph configuration files.
 * 
 * Some test code has been added to allow to access the server functionality from a webbrowser. Accessing the 
 * server with the "/" resource will return a website, where the available services can be entered.
 * 
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 * @date 12 July 2008
 * 
 */

package titan.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;

public class ConnectionHandler extends Thread {

	/** Server name returned in the HTTP header */
	private static String SERVER_NAME = "Titan Service Server";
	/** Path to the resource directory. CLASS files will be downloaded from here. */
	private String m_ServerPath;
	/** Composer to use to answer with services */
	private ServiceComposer m_ServiceComposer;
	
	// connection handling objects
	private Socket m_socket;
	private InputStream m_input;
	private OutputStream m_output;
	private PrintStream  m_print;
	private BufferedReader m_read;
	
	/** Instantiates a Connection handler handling client requests on that socket.
	 *  At this point, the communication streams are opened. Call start() to start 
	 *  the Connection handler thread.
	 * @param serviceComposer 
	 */
	public ConnectionHandler(Socket socket, String strServerPath, ServiceComposer serviceComposer) {

        System.out.println("\nNew connection from " + socket.getInetAddress());

		m_socket = socket;
		m_ServerPath = strServerPath;
		m_ServiceComposer = serviceComposer;

		// open output
		try {
			m_output = m_socket.getOutputStream();
	        m_print = new PrintStream(m_output);
			m_input = m_socket.getInputStream();
			m_read = new BufferedReader(new InputStreamReader(m_input));
		} catch (IOException e) {
			e.printStackTrace();
		}
        
	}
	
	//////////////////////////////////////////////////////////////////////////
	// GET requests
	
	/** Handle a GET request. This function reads the header and breaks into subfunctions to handle the request. */
	protected void handleGET( String [] strCommandLine ) throws IOException {
		
		System.out.println("This is a GET command to " + strCommandLine[1]);
		
		// wait for the end of the header
		String strLine;
		do {
			strLine = m_read.readLine();
			if (strLine == null) throw new IOException("GET Header ended prematurely");
		} while( strLine.trim().length() != 0);
		
		// check whether a specific resource is wanted
		if ( ! strCommandLine[1].equals("/") ) {
			handleGETResource(strCommandLine);
			return;
		} else {
			// no specific resource wanted - return website for user entry
			responseBrowserEntry();
		}

	}

	/** Reads in a file with the given resource name from the SERVER_PATH directory */
	public byte [] getResourceData( String resource ) {

		try {
			String strPath = m_ServerPath + resource;
		
			File file = new File(strPath);
			FileInputStream rcStream = new FileInputStream(file);

			byte [] data = new byte[(int)file.length()];

			rcStream.read(data);
			rcStream.close();

			return data;
		
		} catch (Exception e) {
			return null;
		}

	}
	

	/** Handle a GET request on a specific resource - return the file content if found. */
	protected void handleGETResource( String [] strCommandLine ) throws IOException {
		
		byte [] data = getResourceData(strCommandLine[1]);

		// check whether file could be read
		if (data != null ) {
			m_print.println("HTTP/1.0 200 OK");
			m_print.println("Content-Type: application/octed-stream");
			m_print.println("Server: " + SERVER_NAME);
			m_print.println("Accept-Ranges: bytes");
			m_print.println("Connection: close");
			m_print.println("Content-Length: " + data.length);
			m_print.println("");
			m_print.write(data);
			//m_print.println("");
		} else respondErrorResourceNotFound();
	}
	

	//////////////////////////////////////////////////////////////////////////
	// POST requests

	/** Handle a POST request - here, data about the services available in the 
	 * WSAN are submitted to the server in order to compose services for the  
	 * WSAN.
	 */
	protected void handlePOST( String [] strCommandLine ) throws IOException {
		
		System.out.println("This is a POST command");
		
		// parse header entries - the only interesting is the Content-Length
		String strLine;
		int contentLength = -1;
		do {
			strLine = m_read.readLine();
			if (strLine == null) throw new IOException("POST Header ended prematurely");
			System.out.println("Read: " + strLine);
			
			if (strLine.indexOf("Content-Length: ") == 0 ){
				contentLength = Integer.parseInt(strLine.substring("Content-Length: ".length(),strLine.length()));
			}
		} while( strLine.trim().length() != 0);
		
		// check whether Content-Length has been given
		if ( contentLength == -1 ) {
			respondErrorLengthRequired(); // respond with an error
			return;
		}
		
		// read in POST data
		char [] data = new char[contentLength];

		String strAnswer = "";
		
		// Parse service data
		// expected format: "services=1+2+3"
		try {
			System.out.println("Received " + m_read.read(data) + " bytes of service data");
			String strData = new String(data);
			if( strData.indexOf("services=") != 0 ) throw new Exception("Missing services= tag");
			String [] strServices = strData.substring("services=".length(),strData.length()).split("\\+");
			int [] services = new int[strServices.length];
			for (int i=0;i<services.length;i++) services[i] = Integer.parseInt(strServices[i]);
			System.out.println("Found "+services.length+" services");
			
			strAnswer = m_ServiceComposer.composeServices(services);
			
		} catch (Exception e) {
			e.printStackTrace();
			respondErrorInvalidPostContent();
			return;
		}

		m_print.println("HTTP/1.0 200 OK");
		m_print.println("Content-Type: text/plain");
		m_print.println("Server: " + SERVER_NAME);
		m_print.println("Connection: close");
		m_print.println("Content-Length: " + strAnswer.length());
		m_print.println("");
		m_print.print(strAnswer);
		m_print.println("");
	}

	
	/** Main function for thread execution. Waits on the input stream for HTTP commands */
	public void run() {
		
		try {
			while(true) {
				// read in line
				String strLine = m_read.readLine();
				if (strLine == null) throw new IOException("Read returned null");
				//System.out.println("Read: " + strLine );

				// analyze entry. A HTTP request consists of three entries:
				// COMMAND RESOURCE HTTP/1.x
				String [] words = strLine.split(" ");
				if ( words.length != 3 ) continue;
				
				// check whether it is a HTTP header
				if ( words[2].indexOf("HTTP/1.") == 0 ) {
					String strCommand = words[0];
					
					// check for the command
					if ( strCommand.equals("GET")) handleGET(words);
					else if ( strCommand.equals("POST")) handlePOST(words);
					else {
						// any other commands: wait for the end of the header, then reply with an error
						do {
							strLine = m_read.readLine();
							if (strLine == null) throw new IOException(strCommand + " header ended early");
						} while( strLine.trim().length() != 0);
						
						respondErrorInvalidCommand();
					}
					
				}
				
			}
		} catch (IOException e) {
			// connection closed, read error, or similar - close connection
		}

		System.out.println("\nClosing connection from " + m_socket.getInetAddress());

		// close everything down
		try {
			m_print.close();
			m_output.close();
			m_read.close();
			m_input.close();
			m_socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	//////////////////////////////////////////////////////////////////////////
	// HTTP responses
	
	/** Responds with a HTML page that allows entering service data for the server.*/
	private void responseBrowserEntry() {
		// return answer
		String strAnswer = "<html><body>\n" +
						   "  Welcome to the Titan ServiceServer.<br/>Indicate the " + 
						   "  services available for composition, separated by space " +
						   "  <form action=\"/\" method=\"POST\">\n" +
						   "    Services: <input type=\"text\" name=\"services\" size=\"50\"/> \n" +
						   "    <input type=\"submit\" />\n" +
						   "  </form>\n" +
						   "</body></html>";
		
		m_print.println("HTTP/1.0 200 OK");
		m_print.println("Content-Type: text/html");
		m_print.println("Server: " + SERVER_NAME);
		m_print.println("Connection: close");
		m_print.println("Content-Length: " + strAnswer.length());
		m_print.println("");
		m_print.print(strAnswer);
		m_print.println("");
	}
	
	/** this is returned if the POST command contains invalid data */
	private void respondErrorInvalidPostContent() {
		String strAnswer = "<html><body><h1>501 Invalid POST content</h1>The format of the submitted services is incorrect. Expected format is: \"services=1+2+3\".</html>";

		m_print.println("HTTP/1.0 501 Invalid POST content");
		m_print.println("Content-Type: text/html");
		m_print.println("Server: " + SERVER_NAME);
		m_print.println("Connection: close");
		m_print.println("Content-Length: " + strAnswer.length());
		m_print.println("");
		m_print.print(strAnswer);
		m_print.println("");
	}



	/** Responds to the client with an error indicating that the POST 
	 * Content-Length header entry has been missing.
	 */
	protected void respondErrorLengthRequired() {
		String strAnswer = "<html><body><h1>411 Length Required</h1></html>";

		m_print.println("HTTP/1.0 411 Length Required");
		m_print.println("Content-Type: text/html");
		m_print.println("Server: " + SERVER_NAME);
		m_print.println("Connection: close");
		m_print.println("Content-Length: " + strAnswer.length());
		m_print.println("");
		m_print.print(strAnswer);
		m_print.println("");
	}
	
	/** Responds to the client with an error indicating that the HTTP 
	 *  command used is not supported by the server.
	 */
	protected void respondErrorInvalidCommand() {
		String strAnswer = "<html><body><h1>405 Method not allowed</h1></html>";

		m_print.println("HTTP/1.0 405 Method Not Allowed");
		m_print.println("Allow: GET, POST");
		m_print.println("Content-Type: text/html");
		m_print.println("Server: " + SERVER_NAME);
		m_print.println("Connection: close");
		m_print.println("Content-Length: " + strAnswer.length());
		m_print.println("");
		m_print.print(strAnswer);
		m_print.println("");
		
	}

	/** Responds to the client with an error indicating that the resource 
	 *  requested is not available on the server.
	 */
	protected void respondErrorResourceNotFound() {
		String strAnswer = "<html><body><h1>404 Resource not found</h1></html>";

		m_print.println("HTTP/1.0 404 Resource not found");
		m_print.println("Content-Type: text/html");
		m_print.println("Server: " + SERVER_NAME);
		m_print.println("Connection: close");
		m_print.println("Content-Length: " + strAnswer.length());
		m_print.println("");
		m_print.print(strAnswer);
		m_print.println("");
		
	}
}
