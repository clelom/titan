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
* Component for controlling an analog switch. 
*
* Created: 05.12.2007
* Last modified: 05.12.2007
*
************************************************************************************/

configuration SwitchC
{
  provides interface Switch;
}
implementation
{
  components SwitchP, PlatformSwitchC;

  Switch = SwitchP;

  SwitchP.Init <- PlatformSwitchC.Init;
  SwitchP.SwitchIn -> PlatformSwitchC.SwitchIn;
}
