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

/************************************************************************************
* Interface
*
* @author Andreas Breitenmoser
*
* Commands for controlling an analog switch. 
*
* Created: 05.12.2007
* Last modified: 05.12.2007
*
************************************************************************************/

interface Switch
{
 /*
  * Initialise the port settings.  
  */
  async command void switchInitPort();
  
 /*
  * Turn the switch on.  
  */
  async command void switchOn();

 /*
  * Turn the switch on.  
  */
  async command void switchOff();

 /*
  * Toggle the switch: if it was off, turn it on, if it was on, turn it off.
  */
  async command void switchToggle();
   
 /*
  * Negative logic. The commands are changed to negative logic, e.g. switchOn() turns the switch off.
  */
  async command void switchInverted();
}
