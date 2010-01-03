/*
 * "Copyright (c) 2008 The Regents of the University  of California.
 * All rights reserved."
 *
 * Permission to use, copy, modify, and distribute this software and its
 * documentation for any purpose, without fee, and without written agreement is
 * hereby granted, provided that the above copyright notice, the following
 * two paragraphs and the author appear in all copies of this software.
 *
 * IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY FOR
 * DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES ARISING OUT
 * OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF THE UNIVERSITY OF
 * CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS FOR A PARTICULAR PURPOSE.  THE SOFTWARE PROVIDED HEREUNDER IS
 * ON AN "AS IS" BASIS, AND THE UNIVERSITY OF CALIFORNIA HAS NO OBLIGATION TO
 * PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS."
 *
 */

#include <6lowpan.h>

#define SAMPLES_PER_PACKET 10
#define TARGET_PORT 2000    /* sockaddr_in6 byte order im /opt/tinyos-2.x-contrib/berkeley/b6lowpan/support/sdk/c/lib6lowpan/ip.h mit nx_... */
//#define TARGET_PORT 0xD007    /* sockaddr_in6 byte order im /opt/tinyos-2.x-contrib/berkeley/b6lowpan/support/sdk/c/lib6lowpan/ip.h mit nx_... */
/*
für telosb: port 2000 = 0xD007 als #define

2000 = 0x07D0
0xD007 = 53255
*/


configuration UDPShellC {
  uses interface UDP;
  uses interface Boot;

} implementation {

  components UDPShellP, LedsC;

  UDPShellP.UDP    = UDP;
  UDPShellP.Boot   = Boot;

  UDPShellP.Leds -> LedsC;
  components ICMPResponderC;
  UDPShellP.ICMPPing -> ICMPResponderC.ICMPPing[unique("PING")];

#ifdef DELUGE
  components new DelugeMetadataClientC();
  UDPShellP.DelugeMetadata -> DelugeMetadataClientC;
#endif

#if defined(PLATFORM_TELOSB) || defined(PLATFORM_EPIC) || defined(CURRENTSWITCH)
  components CounterMilli32C;
  UDPShellP.Uptime -> CounterMilli32C;
#endif

#if defined(VOICEMODULE)
  components new VoiceSynthC();
  UDPShellP.VStamp -> VoiceSynthC;
#endif

#if defined(CURRENTSWITCH)
  components CurrentSwitchC;
  UDPShellP.CurrentSwitch -> CurrentSwitchC;
  
#else 
  
  components new AccelerationC();
  UDPShellP.Read -> AccelerationC;

#endif



  // sensor interface
  components new TimerMilliC();
  UDPShellP.MilliTimer -> TimerMilliC;

}
