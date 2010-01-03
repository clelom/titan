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
 * TitanComThread.java
 * 
 * This Thread listens on a InputStream for incoming packages and reassembles them.
 * 
 * @author Tonio Gsell <tgsell@ee.ethz.ch>
 */

package titan.mobile;

import java.io.IOException;
import java.io.InputStream;

public class TitanComThread implements Runnable {

	public void run() {
		InputStream m_Input = null;
		char c;
		int errCounter = 0;
		
		m_Input = TitanMobile.getBlueCom().getInputStream();
		
		if(m_Input != null) {
			while(TitanMobile.singleton.m_bTitanRun) {
				try {
					c = (char) m_Input.read();
					TitanMobile.getBlueCom().receive(c);
					
				} catch (IOException e) {
					if(errCounter == 2)
						TitanMobile.btConnectionLost();
					else
						errCounter++;
				}
			}
		}
		else
			System.out.println("ERROR: No connection available!");
	}
}
