#
#    This file is part of Titan.
#
#    Titan is free software: you can redistribute it and/or modify
#    it under the terms of the GNU General Public License as 
#    published by the Free Software Foundation, either version 3 of 
#    the License, or (at your option) any later version.
#
#    Titan is distributed in the hope that it will be useful,
#    but WITHOUT ANY WARRANTY; without even the implied warranty of
#    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
#    GNU General Public License for more details.
#
#    You should have received a copy of the GNU General Public License
#    along with Titan. If not, see <http://www.gnu.org/licenses/>.
#
##############################################################################
#
# Titan Makefile
#
# This file compiles Titan into a TinyOS executable
#
# @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
#

# Select main component
COMPONENT=TestTitan

# specify sensorboard (if available) - needed e.g. for acceleration sensors
SENSORBOARD=ifeext

# Define the AM_ADDRESS of a node (this is used if the JTAG loader is used)
# TODO: find out how to use tos-set-symbols to shorten adaptation time
PFLAGS+= -DDEFINED_TOS_AM_ADDRESS=0

# allows snooping on the CC2420
CFLAGS+= -DCC2420_NO_ADDRESS_RECOGNITION

# use the Packet Link layer included into the TinyOS CC2420 stack. See http://docs.tinyos.net/index.php/PacketLink
# CAUTION: seems to create some instabilities in the Titan reconfiguration process
PFLAGS+= -DPACKET_LINK

# Set custom message length. Standard is around 28 bytes.
# TOSH_DATA_LENGTH - a packet can be a maximum of 128 bytes long -> on Mica2 about 120 bytes max
# CAUTION: Setting this increases the RAM requirements of the FIFO buffers
PFLAGS+= -DTOSH_DATA_LENGTH=100

# import Makerules (works for contrib as well as original folder)
include ../../support/make/Makerules
