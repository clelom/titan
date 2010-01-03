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

/************************************************************************************
* Interface
*
* @author Andreas Breitenmoser
*
* Commands for controlling the V-Stamp voice synthesiser module. 
*
* Created: 11.01.2008
* Last modified: 11.01.2008
*
************************************************************************************/

interface VStamp
{
 /**
  * Initialise the V-Stamp module.
  * @param baudRateDetect: enables baud rate auto-detect if TRUE. If FALSE pin strapping is used.
  * @return SUCCESS is returned if UART 1 is initialised properly, FAIL otherwise.  
  */
  command error_t init(bool baudAutoDetect);

 /**
  * Select Baud-Rate by command after baud rate was set once at the beginning.
  * @param baudRateN1, baudRateN2: number 0 - 11 as ASCII-symbols, one of the baud rates provided by V-Stamp. E.g. 6 => baudRateN1 = 0x30, baudRateN2 = 0x36, 
  * @see datasheet V-Stamp
  */
  command void setBaudRate(uint8_t baudRateN1, uint8_t baudRateN2);
  
 /**
  * Adjust synthesiser's speed.
  * @param speechRate1, speechRate2: number 0 - 13 as ASCII-symbols. E.g. 6 => speechRate1 = 0x30, speechRate2 = 0x36
  * @see datasheet V-Stamp
  */
  command void setSpeed(uint8_t speechRate1, uint8_t speechRate2);
  
 /**
  * Adjust synthesiser's output volume. 
  * @param volume: number 0 - 9 as ASCII-symbol.
  * @see datasheet V-Stamp
  */
  command void setVolume(uint8_t volume);
  
 /**
  * Select one of the standard voices.  
 * @param voice1, voice2: number 0 - 10 as ASCII-symbols. E.g. 6 => voice1 = 0x30, voice2 = 0x36
  * @see datasheet V-Stamp
  */
  command void setVoice(uint8_t voice1, uint8_t voice2);  
  
 /**
  * Send a text segment to be spoken. 
  * @param text: string that should be spoken by V-Stamp
  */
  command void sendText(const char* text);

 /**
  * Play a sound file which was recorded or downloaded to V-Stamp.
  * @param fileN1, file2: specify the sound file that should be played, input as ASCII-symbols fileN1*10+fileN2
  */
  command void playSound(uint8_t fileN1, uint8_t fileN2);
  
 /**
  * Playback, text-to-speech or recording is suspended.
  */
  command void suspend();
  
 /**
  * Playback, text-to-speech or recording is resumed.
  */
  command void resume();
  
 /**
  * Resets the V-Stamp module.
  */
  command void reset();
  
  /**
   * The V-Stamp stops whatever it is doing and flushes the input buffer 
   * of all text and commands.
   */
   command void stop();
 

 /**
   * Signals that the UART-interface is set up.
   */
  event void uartReady();
}
