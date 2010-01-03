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

package titan.applications;

import java.util.Observable;

import titan.TitanCommunicate;
import titan.messages.MessageDispatcher;
import titan.services.ServiceDirectory;

/**
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 */

public class TestApp implements Application {

	public String getName() {
		return "TestApp";
	}
	
	public int getUSID() {
		return 57;
	}

	public boolean startApplication(TitanCommunicate arg0, ServiceDirectory arg1, MessageDispatcher arg2) {
		System.out.println("This is the test application");
		return false;
	}

	public void update(Observable arg0, Object arg1) {
	}

	public void main(String[] args) {
		System.out.println("This is the test MAIN function");
	}
}
