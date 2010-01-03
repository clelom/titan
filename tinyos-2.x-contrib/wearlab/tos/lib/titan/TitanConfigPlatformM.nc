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

/*
 * TitanConfigPlatformM.nc
 *
 * This module configures the platform for Titan.
 *
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 */

#include "TitanInternal.h"

module TitanConfigPlatformM {

  uses interface Boot;
  uses interface CC2420Config;
}

implementation {

  event void Boot.booted() {
    call CC2420Config.setChannel(TITAN_COMM_CHANNEL);
    call CC2420Config.sync();
  }

  event void CC2420Config.syncDone(error_t error) {
  
  }
  
}
