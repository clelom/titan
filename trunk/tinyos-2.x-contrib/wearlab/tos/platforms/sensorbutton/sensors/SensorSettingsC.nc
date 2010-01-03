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
 * @author Andreas Bubenhofer
 * 
 */

#include <sensors.h>
module SensorSettingsC {
    provides interface Msp430Adc12Config[uint8_t type];  
}
implementation
{
    async command msp430adc12_channel_config_t Msp430Adc12Config.getChannelSettings[uint8_t type]() {
        msp430adc12_channel_config_t config;
        if(type < DEFAULT_CHANNEL) {
            config = sensorconfigurations[type];
        } else {
            config = sensorconfigurations[DEFAULT_CHANNEL];
        }
        return config;
    }
}

