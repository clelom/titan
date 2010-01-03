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
 * @author Andreas Breitenmoser
 */

#include "hardware.h"

configuration PlatformSwitchC
{
  provides interface HplMsp430GeneralIO as SwitchIn;
  uses interface Init;
}
implementation
{
  components 
      HplMsp430GeneralIOC as GeneralIOC,
	  PlatformSwitchP;

  Init = PlatformSwitchP.SwitchInit;

  //cl SwitchIn = SwitchImpl;
  SwitchIn = GeneralIOC.Port12;
  // for test with LED of TmoteSky
  //SwitchIn = GeneralIOC.Port54;
  
}
