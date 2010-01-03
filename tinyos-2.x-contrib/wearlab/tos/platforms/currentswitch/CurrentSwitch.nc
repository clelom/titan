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
 * @author Clemens Lombriser
 */
 
interface CurrentSwitch
{
  /**
   * Opens the switch -- no current flows
   */
  async void command setOpen();
  
  /**
   * Closes the switch -- current flows
   */
  async void command setClose();
  
  /**
   * Closes the switch if bClose is true -- current flows
   */
  async void command set( bool bClose );
  
  /**
   * Toggles the state of the switch
   */
  async void command toggle();
  
  /**
   * returns the state of the switch
   * @return true if switch is closed -- current flows
   */
  async bool command getState();

}
