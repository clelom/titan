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
 * Defining the platform-independently named packet structures to be the
 * chip-specific nRF2401 packet structures.
 *
 * @author Christoph Walser
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 */


#ifndef PLATFORM_MESSAGE_H
#define PLATFORM_MESSAGE_H

#include <nrf2401.h>

typedef union message_header {
  nrf2401_header_t nrf2401;
} message_header_t;

typedef union TOSRadioFooter {
  nrf2401_footer_t nrf2401;
} message_footer_t;

typedef union TOSRadioMetadata {
  nrf2401_metadata_t nrf2401;
} message_metadata_t;

#endif
