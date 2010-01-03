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
 * Test application for smart toys communication.
 *
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 */
 
#include "Puppet.h"

configuration SmartObjectApp{
}
implementation{
  components MainC, LedsC, CC2420ActiveMessageC as ActiveMessageC;
  components new TimerMilliC(), RandomC;
  
  components new AMSenderC(AM_IDMSG) as IDSender;
  components new AMSenderC(AM_PUPPETMSG) as PuppetSender;
  
  components SmartObjectP;

  SmartObjectP.Boot  -> MainC;
  SmartObjectP.Leds  -> LedsC;  
  SmartObjectP.Timer -> TimerMilliC;
  SmartObjectP.Random -> RandomC;

  SmartObjectP.Packet -> ActiveMessageC;
  SmartObjectP.CC2420Packet  -> ActiveMessageC;
  
  
  SmartObjectP.IDSend   -> IDSender;
  SmartObjectP.PuppetSend   -> PuppetSender;
  
  SmartObjectP.AMControl -> ActiveMessageC;
  
}
