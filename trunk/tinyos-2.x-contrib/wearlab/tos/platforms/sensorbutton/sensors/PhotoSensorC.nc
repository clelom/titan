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
 *
 *
 * @author Andreas Bubenhofer
 */


#include <sensors.h>
generic configuration PhotoSensorC()
{
  provides {
    interface Read<uint16_t> as Read;
    interface ReadNow<uint16_t> as ReadNow;
    interface Resource as ReadNowResource;
  }
}
implementation
{
  components SensorSettingsC as Settings;
             
  components new AdcReadClientC() as AdcReadClient;
  Read = AdcReadClient;
  AdcReadClient.Msp430Adc12Config -> Settings.Msp430Adc12Config[LIGHT_CHANNEL];
  
  components new AdcReadNowClientC() as AdcReadNowClient;
  ReadNow = AdcReadNowClient;
  ReadNowResource = AdcReadNowClient;
  AdcReadNowClient.Msp430Adc12Config -> Settings.Msp430Adc12Config[LIGHT_CHANNEL];

  }

