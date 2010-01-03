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

import java.io.FilterOutputStream;
import java.io.OutputStream;
import java.util.Observable;

/**
 *
 * @author Mirco Rossi <mrossi@ife.ee.ethz.ch>
 */
public class SystemOutputDispacher extends FilterOutputStream {
    
    private SystemOutputObservable m_outputObserverable;
    
    public SystemOutputDispacher(OutputStream aStream, SystemOutputObservable  outputObserverable) {
            super(aStream);
            m_outputObserverable=outputObserverable;
    }
        
        public void write(byte b[])  {
            String aString = new String(b);
            m_outputObserverable.sendMessage((Object)aString);
        }
        
        public void write(byte b[], int off, int len ) {
            String aString = new String(b,off,len);
            m_outputObserverable.sendMessage((Object)aString);
        }
    }

