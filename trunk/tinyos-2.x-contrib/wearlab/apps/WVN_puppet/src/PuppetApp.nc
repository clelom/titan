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
 * PuppetApp
 * 
 * This is a test application for the puppet. It will check how the reading works
 * 
 * 
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 * 
 */

#include "Puppet.h"

configuration PuppetApp {
}

implementation{
  components LedsC, MainC, CC2420ActiveMessageC as ActiveMessageC, RandomC;
  components PuppetP;

  components new AMReceiverC(AM_PUPPETMSG) as PuppetReceiver;
  components new AMReceiverC(AM_IDMSG) as IDReceiver;

  components new TimerMilliC(); 
  components new VoiceSynthC();
	
  PuppetP.Boot -> MainC;
  PuppetP.Leds -> LedsC;
  PuppetP.AMControl -> ActiveMessageC;
  PuppetP.CC2420Packet -> ActiveMessageC;
  PuppetP.VStamp -> VoiceSynthC; 
  PuppetP.Timer -> TimerMilliC;
  PuppetP.PuppetReceive -> PuppetReceiver;
  PuppetP.IDReceive -> IDReceiver;
  PuppetP.Random -> RandomC;
}
