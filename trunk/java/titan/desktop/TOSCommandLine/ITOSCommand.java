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
 * ITOSCommand.java
 * 
 * @author  Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 * 
 */

package TOSCommandLine;


/** ITOSCommand provides an interface to be used by all commands 
 *  of the TOSCommandLine command line.
 * 
 */
public interface ITOSCommand {
    
    /** Explains how the command is to be used by a user. 
     *  This should give the format:
     *  commandname REQUIRED_ARGUMENT [OPTIONAL_ARGUMENT]
     *  
     *  Description of what the command does.
     *  
     * @return a string containing the description in the format given above (including newlines)
     */
    public String getHelp();
    
    /** Called by the command line on the invocation of the command 
     *  by the user.
     * @param args list of arguments passed by the user. The first one is the command string
     * @return zero if successful
     */
    public int execute( String[] args );
    
    /** Returns the name of the command as the user must enter it. 
     *  This should not include any whitespace characters.
     * @return command name
     */
    public String toString();
}
