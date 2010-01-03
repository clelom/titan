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
 * @author Clemens Lombriser
 */
 
module CurrentSwitchP {
  uses interface Boot;
  uses interface HplMsp430GeneralIO as SwitchPin0;
  uses interface HplMsp430GeneralIO as SwitchPin1;
  provides interface CurrentSwitch;
}

implementation {

  /**
   * Set up the pin direction and stuff
   */
  event void Boot.booted() {
    call SwitchPin0.makeOutput();
    call SwitchPin1.makeOutput();
    call SwitchPin0.clr();
    call SwitchPin1.clr();
  }

  /**
   * Opens the switch -- no current flows
   */
  async void command CurrentSwitch.setOpen() {
    call SwitchPin0.set();
    call SwitchPin1.set();
  }
  
  /**
   * Closes the switch -- current flows
   */
  async void command CurrentSwitch.setClose() {
    call SwitchPin0.clr();
    call SwitchPin1.clr();
  }
  
  /**
   * Closes the switch if bClose is true -- current flows
   */
  async void command CurrentSwitch.set( bool bClose ) {
    bClose? call SwitchPin0.set() : call SwitchPin0.clr();
    bClose? call SwitchPin1.set() : call SwitchPin1.clr();
  }
  
  /**
   * Toggles the state of the switch
   */
  async void command CurrentSwitch.toggle() {
    call SwitchPin0.toggle();
    call SwitchPin1.toggle();
  }
  
  /**
   * returns the state of the switch
   * @return true if switch is closed -- current flows
   */
  async bool command CurrentSwitch.getState() {
    return (call SwitchPin0.get() != 0);
  }

}
