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
 * lmxActiveMessage.nc
 *
 * Provides interfaces for the Bluetooth message structure.
 *
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 * @date April 8 2008
 */

 #include "lmx9820a.h"

module lmxActiveMessageC {
  provides interface AMPacket;
}

implementation {


  //////////////////////////////////////
  // AMPacket interface
  
  /** Gateway bluetooth address is always zero */
  command am_addr_t AMPacket.address() {
    return 0;
  }

  /** Destination address */
  command am_addr_t AMPacket.destination(message_t* amsg) {
    return ((lmx9820a_header_t*)amsg->header)->dest;
  }
  /** Destination address */
  command void AMPacket.setDestination(message_t* amsg, am_addr_t addr) {
    ((lmx9820a_header_t*)amsg->header)->dest = addr;
  }

  /** Source address */
  command am_addr_t AMPacket.source(message_t* amsg) {
    return ((lmx9820a_header_t*)amsg->header)->src;
  }
  /** Source address */
  command void AMPacket.setSource(message_t* amsg, am_addr_t addr) {
    ((lmx9820a_header_t*)amsg->header)->src = addr;;
  }

  /** This is a gateway - it is always true */
  command bool AMPacket.isForMe(message_t* amsg) {
    return TRUE;
  }
  
  /** Active Message Type */
  command am_id_t AMPacket.type(message_t* amsg) {
    return ((lmx9820a_header_t*)amsg->header)->type;
  }
  /** Active Message Type */
  command void AMPacket.setType(message_t* amsg, am_id_t t) {
    ((lmx9820a_header_t*)amsg->header)->type = t;
  }

  /** No group information available */
  command am_group_t AMPacket.group(message_t* amsg) {
    return 0;
  }
  /** No group information available */
  command void AMPacket.setGroup(message_t* amsg, am_group_t grp) {}
  /** No group information available */
  command am_group_t AMPacket.localGroup() {
    return 0;
  }

}
