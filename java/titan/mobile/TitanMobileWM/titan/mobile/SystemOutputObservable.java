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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package titan.mobile;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Observable;

/**
 *
 * @author Mirco Rossi <mrossi@ife.ee.ethz.ch>
 */
public class SystemOutputObservable extends Observable {
   
   private PrintStream m_output;
   
   public SystemOutputObservable () {
      try {
         m_output = new PrintStream(new FileOutputStream("output.log"));
         System.out.println("Writing output to output.log");
      } catch (FileNotFoundException ex) {
         m_output = null;
         System.out.println("Could not open output file");
      }
   }
   
        /**
     * Submit message to be delivered to all registered recipicents.
     */
    public void sendMessage(Object obs) {
        if(m_output != null) m_output.print(obs);
        setChanged();
        this.notifyObservers( obs );
        clearChanged();
    }
}
