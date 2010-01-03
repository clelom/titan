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
 * VoiceApp
 * 
 * This is a test application for the Wireless Voice Nodes paper. It is set up to measure the energy consumption of the voice output.
 * 
 * 
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 * 
 */


configuration VoiceApp{
}


implementation{
	
	
    components LedsC, MainC, VoiceTestP;

    components new TimerMilliC(); 
    components new VoiceSynthC();
    
    VoiceTestP.Boot -> MainC;
    VoiceTestP.Leds -> LedsC;
    VoiceTestP.VStamp -> VoiceSynthC; 
    VoiceTestP.Timer -> TimerMilliC; 

	
	
}
