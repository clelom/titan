COMPONENT=UDPEchoC

# uncomment this for network programming support
# BOOTLOADER=tosboot
SENSORBOARD=ifeext

# radio opts
CFLAGS += -DCC2420_DEF_CHANNEL=21
# CFLAGS += -DCC2420_DEF_RFPOWER=4

# if this is set, motes will send debugging information to the address
# listed.
CFLAGS += -DREPORT_DEST=\"2001:41e0:ff00:d5::2\"

#PFLAGS+= -DDEFINED_TOS_AM_ADDRESS=5


# sim/test harness
# CFLAGS += -I../IPBaseStation
# CFLAGS += -DDBG_TRACK_FLOWS -DDBG_FLOWS_REPORT

# printf debugs.  works only on telosb/tmote sky
# CFLAGS += -DPRINTFUART_ENABLED

#somehow this does not work just like that
include /opt/tinyos-2.x-contrib/berkeley/b6lowpan/support/make/lowpan.extra

include ../../support/make/Makerules

