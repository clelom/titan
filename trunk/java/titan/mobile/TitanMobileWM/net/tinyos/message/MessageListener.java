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
 * This class is a stub to immitate the TinyOS message package for 
 * implementation on the mobile phone without actually having to 
 * include the real packages.
 *
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 */

package net.tinyos.message;

public interface MessageListener {
	public void messageReceived(int address,Message msg);
}
