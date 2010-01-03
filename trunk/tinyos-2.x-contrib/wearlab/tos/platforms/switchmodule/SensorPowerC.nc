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

/************************************************************************
* Sensor Power Control
*
* @author Andreas Breitenmoser
*
 * Component controls the power supplies of the acceleration, light and pressure sensors.
 * So each sensor can be enabled separately.
*
* Created: 30.11.2007
* Last modified: 30.11.2007
*
************************************************************************/

#include "hardware.h"

configuration SensorPowerC
{
  provides interface GeneralIO as PS_LIGHT;
  provides interface GeneralIO as PS_ACC;
  provides interface GeneralIO as PS_PRESS;
}
implementation
{
  components HplMsp430GeneralIOC as GeneralIOC;
  components
	new Msp430GpioC() as PS_LIGHTImpl,
    new Msp430GpioC() as PS_ACCImpl,
    new Msp430GpioC() as PS_PRESS1Impl,
    new Msp430GpioC() as PS_PRESS2Impl,
    new Msp430GpioC() as PS_PRESS3Impl,
    new Msp430GpioC() as PS_PRESS4Impl,
    new Msp430GpioC() as PS_PRESS5Impl;
  components SensorPowerP;

  PS_LIGHT = PS_LIGHTImpl;
  PS_LIGHTImpl -> GeneralIOC.Port15;

  PS_ACC = PS_ACCImpl;
  PS_ACCImpl -> GeneralIOC.Port16;

  PS_PRESS = SensorPowerP.PS_PRESS;
  SensorPowerP.Port17 -> PS_PRESS1Impl;
  SensorPowerP.Port20 -> PS_PRESS2Impl;
  SensorPowerP.Port51 -> PS_PRESS3Impl;
  SensorPowerP.Port52 -> PS_PRESS4Impl;
  SensorPowerP.Port53 -> PS_PRESS5Impl;
  PS_PRESS1Impl -> GeneralIOC.Port17;
  PS_PRESS2Impl -> GeneralIOC.Port20;
  PS_PRESS3Impl -> GeneralIOC.Port51;
  PS_PRESS4Impl -> GeneralIOC.Port52;
  PS_PRESS5Impl -> GeneralIOC.Port53;
  
}
