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
 * Power Switch for Sensor Button
 * @author Andreas Bubenhofer
 */
#include "hardware.h"

configuration PowerSwitchC {
  provides interface GeneralIO as LS;
  provides interface GeneralIO as ACC;
  provides interface GeneralIO as MIC;
  provides interface GeneralIO as DBG; //for debuging purpose
  provides interface GeneralIO as nRFSTE; //for debuging purpose clr this port for power savings about 20mW!!!!!
  
}
implementation
{
  components 
      HplMsp430GeneralIOC as GeneralIOC
    , new Msp430GpioC() as LSImpl
    , new Msp430GpioC() as ACCImpl
    , new Msp430GpioC() as MICImpl
	, new Msp430GpioC() as DbgPin
	, new Msp430GpioC() as STEPIN
    ;

  LS = LSImpl;
  LSImpl -> GeneralIOC.Port10;

  ACC = ACCImpl;
  ACCImpl -> GeneralIOC.Port11;

  MIC = MICImpl;
  MICImpl -> GeneralIOC.Port12;
  
  DBG = DbgPin;
  DbgPin -> GeneralIOC.Port24;
  
  nRFSTE = STEPIN;
  STEPIN -> GeneralIOC.Port50;
  

}

