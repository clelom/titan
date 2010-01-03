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
 * TOSExit.java
 * 
 * @author Clemens <lombriser@ife.ee.ethz.ch>
 * 
 * The TOSExit command triggers the end of the TOSCommandLine application.
 * 
 */

package TOSCommandLine;

public class TOSExit implements ITOSCommand {
    
    /** reference to TOSCommandLine */
    private TOSCommandLine m_tsd;
    
    /** Initializes TOSExit
     * @param tsd reference to the TOSCommandLine to finish when called
     */
    public TOSExit (TOSCommandLine tsd ) {
        m_tsd = tsd;
    }
    
    /** Gives usage information
     * @return A string containing usage information for the command
     */
    public String getHelp() {
        return "exit\n\nThis closes the TinyOS command line";
    }
    
    /** Executes TOSExit. This will trigger the command line 
     * to exit the application after the commands return.
     * @return always zero
     */
    public int execute(String[] args) {
        m_tsd.finish();
        return 0;
    }
    
    /** @return The command name: "exit" */
    public String toString() {
        return "exit";
    }
    
}
