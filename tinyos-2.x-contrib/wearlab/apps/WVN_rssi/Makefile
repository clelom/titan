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

COMPONENT=src/TestRssiApp



PFLAGS+= -Isrc

# for the switchmodule, TOS_NODE_ID needs to be set manually
PFLAGS+= -DDEFINED_TOS_AM_ADDRESS=2

# use a non-standard communication channel to avoid interruptions
PFLAGS+= -DTITAN_COMM_CHANNEL=8

include ../../support/make/Makerules
