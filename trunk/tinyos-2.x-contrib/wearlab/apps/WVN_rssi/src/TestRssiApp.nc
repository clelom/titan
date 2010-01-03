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
 * This is a test application for the Wireless Voice Nodes paper. It is set up to measure RSSI values around the puppet.
 * 
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 * 
 */

#include "TestRssi.h"

configuration TestRssiApp{
}


implementation{
  components MainC, LedsC, TestRssiP, SerialActiveMessageC, CC2420ActiveMessageC as ActiveMessageC;
  components new TimerMilliC();
  
  components new AMSenderC(AM_WVN_RSSI_MEASUREMENT) as RSSISender;
  components new AMReceiverC(AM_WVN_RSSI_MEASUREMENT) as RSSIReceiver;
  
  components new AMSenderC(AM_WVN_COLLECT) as CollectSender;
  components new AMReceiverC(AM_WVN_COLLECT) as CollectReceiver;

  TestRssiP.Boot  -> MainC;
  TestRssiP.Leds  -> LedsC;  
  TestRssiP.Timer -> TimerMilliC;

  TestRssiP.Packet -> ActiveMessageC;
  TestRssiP.SerialPacket  -> SerialActiveMessageC;
  TestRssiP.CC2420Packet  -> ActiveMessageC;
  
  TestRssiP.SerialSender  -> SerialActiveMessageC.AMSend[AM_SERIALMSG];
  
  TestRssiP.RssiSender   -> RSSISender;
  TestRssiP.RssiReceiver -> RSSIReceiver;
  
  TestRssiP.AMControl -> ActiveMessageC;
  TestRssiP.SerialControl -> SerialActiveMessageC;
  
  TestRssiP.CollectSender -> CollectSender;
  TestRssiP.CollectReceiver -> CollectReceiver;
  
}
