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
 * WIGA message headers.
 *
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 * @date April 8 2008
 */


#ifndef PLATFORM_MESSAGE_H
#define PLATFORM_MESSAGE_H

#include <CC2420.h>
#include <Serial.h>
#include <lmx9820a.h>

typedef union message_header {
  cc2420_header_t cc2420;
  serial_header_t serial;
  lmx9820a_header_t lmx;
} message_header_t;

typedef union TOSRadioFooter {
  cc2420_footer_t cc2420;
} message_footer_t;

typedef union TOSRadioMetadata {
  cc2420_metadata_t cc2420;
} message_metadata_t;

#endif
