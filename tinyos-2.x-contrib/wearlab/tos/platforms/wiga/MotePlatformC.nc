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
 * @author Fabian Schenkel
 */

module MotePlatformC {
  provides interface Init;
}
implementation {

  command error_t Init.init() {
    // reset all of the ports to be input and using i/o functionality
    atomic
      {
	P1SEL = 0;
	P2SEL = 0;
	P3SEL = 0;
	P4SEL = 0;
	P5SEL = 0;
	P6SEL = 0;

	P1DIR = 0xe0;
	P1OUT = 0x00;
 
	P2DIR = 0x7b;
	P2OUT = 0x30;

	P3DIR = 0xf1;
	P3OUT = 0x00;

	P4DIR = 0xfd;
	P4OUT = 0xdd;

	P5DIR = 0xff;
	P5OUT = 0xff;

	P6DIR = 0xff;
	P6OUT = 0x00;

	P1IE = 0;
	P2IE = 0;

      }//atomic
    return SUCCESS;
  }
}
