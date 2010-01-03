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
* Actuator driver
*
* @author Andreas Breitenmoser
*
* Implementation of the analog switch control.
* Different functions are available for controlling the switch. 
*
* Created: 05.12.2007
* Last modified: 05.12.2007
*
************************************************************************************/


module SwitchP
{
  provides
  {
	interface Init;
	interface Switch;
  }
  
  uses interface HplMsp430GeneralIO as SwitchIn;
}
implementation
{
  bool posLogic;
  
  command error_t Init.init()
  {
      dbg("Init", "Switch: initialised.\n");
	atomic posLogic = TRUE; // positive logic is enabled as default
    
    return SUCCESS;
  }
  
  async command void Switch.switchInitPort()
  {
	atomic
	{
		P1DIR |= 1 << 2; // make pin output-pin
		P1SEL &= ~(1 << 2); // select IO functionality
		P1OUT &= ~(1 << 2); // switch must be turned off as default
	}
  }

  async command void Switch.switchOn()
  {
	if (posLogic)
		call SwitchIn.set();
	else
		call SwitchIn.clr();
    dbg("SwitchC", "Switch turned on.\n");
  }

  async command void Switch.switchOff()
  {
	if (posLogic)
		call SwitchIn.clr();
	else
		call SwitchIn.set();
    dbg("SwitchC", "Switch turned off.\n");
  }

  async command void Switch.switchToggle()
  {
    call SwitchIn.toggle();
    dbg("SwitchC", "Switch toggled.\n");
  }
  
  async command void Switch.switchInverted()
  {
	if (posLogic)
		atomic posLogic = FALSE;
	else
		atomic posLogic = TRUE;
    dbg("SwitchC", "Negative logic enabled.\n");
  }
}
