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
 * TOSHelp.java
 * 
 * @author Clemens <lombriser@ife.ee.ethz.ch>
 * 
 * Help command for the TOSCommandLine. This prints either all available 
 * commands or usage information about individual ones.
 * 
 */

package TOSCommandLine;

import java.util.Map;

public class TOSHelp implements ITOSCommand {
    
    /** Reference to the commands registered in TOSCommandLine */
    private Map<String,ITOSCommand> m_Commands;
    
    /** Initializes a TOSHelp command
     * @param cmds Reference to the list of commands registered with TOSCommandLine
     */
    TOSHelp ( Map<String,ITOSCommand> cmds ) {
        m_Commands = cmds;
        
    }
    
    /** Called by the help command. This should print usage information 
     * @return String with usage information
     */
    public String getHelp() {
        String strHelp = "help [command]\n\n";
        strHelp += "Prints usage information about the given command,";
        strHelp += "or a list of all known commands.";
        return strHelp;
    }
    
    /** Executes the command
     * @param args The command line arguments of the command. The first entry is the program name
     * @return Returns zero if all goes well
     */
    public int execute(String[] args) {
        
        // is a command given?
        if ( args.length == 1 ) {
            
            // give general information about commands
            
            System.out.println("Currently available commands:");
            
            // iterate through all commands
            Object[] strKeys = m_Commands.keySet().toArray();
            for( int i = 0; i< strKeys.length; i++ ) {
                ITOSCommand cmd = (ITOSCommand) m_Commands.get( strKeys[i] );
                
                // print command name
                if ( cmd != null ) {
                    System.out.print( cmd.toString() + "\t" );
                }
                
                // 4 per line
                if ( i%4 == 3 ) {
                    System.out.print("\n");
                }
            }
            
            // no double newline at the end
            if ( strKeys.length %4 != 0 ) {
                System.out.print("\n");
            }
            
        } else {
            
            // give command specific usage information
            
            ITOSCommand cmd = (ITOSCommand)m_Commands.get( args[1] );
            
            // check whether the command really exists
            if ( cmd == null ) {
                System.out.println("help: Error: command \""+args[1]+"\" not known.");
            } else {
                System.out.println(cmd.getHelp());
            }
        }
        
        return 0;
    }
    
    /** @return The name of the command as typed in the command line */
    public String toString() {
        return "help";
    }
}
