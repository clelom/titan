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
 
configuration CurrentSwitchC {
  provides interface CurrentSwitch;
}

implementation {

  components HplMsp430GeneralIOC as GeneralIOC;
  components MainC;
  components CurrentSwitchP;

  CurrentSwitchP -> MainC.Boot;
  CurrentSwitchP.SwitchPin0 -> GeneralIOC.Port60;
  CurrentSwitchP.SwitchPin1 -> GeneralIOC.Port61;
  
  CurrentSwitch = CurrentSwitchP;
  
}
