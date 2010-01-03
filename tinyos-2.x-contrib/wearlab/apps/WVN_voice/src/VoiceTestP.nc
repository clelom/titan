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
 * VoiceTestP
 * 
 * Implements the code to run the voice energy measurement test.
 * 
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 * 
 */


module VoiceTestP{
	uses interface Leds;
	uses interface Boot;
	uses interface Timer<TMilli>;
	uses interface VStamp;
}


implementation{
	
	bool m_vStampReady;
  uint8_t m_counter;
	
	const unsigned int TO_ASCII = 48;
	
	event void Boot.booted() {
		m_vStampReady = FALSE;
		call VStamp.init(FALSE);
		
    m_counter = 0;
    
		call Timer.startPeriodic(4000);

		call Leds.led0On(); // turn it off
    call Leds.led1On();

    }
	
	event void Timer.fired() {
		
		
		if (m_vStampReady == TRUE) {
			//call VStamp.setSpeed(TO_ASCII, 5+TO_ASCII);
			//call VStamp.setVoice(TO_ASCII,TO_ASCII);

			P4OUT |= BIT0;
			
			call VStamp.stop();

#define VOLUME_TEST
#ifdef VOLUME_TEST
			call VStamp.setVolume(m_counter+TO_ASCII);
			m_counter = (m_counter < 9)? m_counter+1 : 0;
			call VStamp.sendText( "Hello, what is your name?");
#elif defined(SYNTHESIS_TEST)

			call VStamp.setVolume(4 + TO_ASCII); // equalize output amplitude to flash test
			call VStamp.playSound(TO_ASCII + 0, TO_ASCII + 0); // play sound 00
#elif defined(FLASH_TEST)
			call VStamp.setVolume(9 + TO_ASCII);
			call VStamp.sendText("Hello, what is your name?"); 
#endif

			P4OUT &= ~BIT0;
		}
		
	}
	
	event void VStamp.uartReady() {
		m_vStampReady = TRUE;
	}
	
}
