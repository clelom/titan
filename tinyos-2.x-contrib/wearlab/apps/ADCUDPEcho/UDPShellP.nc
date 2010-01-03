/*
 * "Copyright (c) 2008 The Regents of the University  of California.
 * All rights reserved."
 *
 * Permission to use, copy, modify, and distribute this software and its
 * documentation for any purpose, without fee, and without written agreement is
 * hereby granted, provided that the above copyright notice, the following
 * two paragraphs and the author appear in all copies of this software.
 *
 * IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY FOR
 * DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES ARISING OUT
 * OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF THE UNIVERSITY OF
 * CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS FOR A PARTICULAR PURPOSE.  THE SOFTWARE PROVIDED HEREUNDER IS
 * ON AN "AS IS" BASIS, AND THE UNIVERSITY OF CALIFORNIA HAS NO OBLIGATION TO
 * PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS."
 *
 */

#include <ip.h>
#include <IPDispatch.h>
#include <ICMP.h>

#include "sensors.h"

//#include </usr/include/arpa/inet.h>


#ifdef DELUGE
#include "Deluge.h"
#include "imgNum2volumeId.h"
#endif

module UDPShellP {
  uses {
    interface Boot;
    interface UDP;
    interface Leds;
    
    interface ICMPPing;
#if defined(PLATFORM_TELOSB) || defined(PLATFORM_EPIC) || defined(CURRENTSWITCH)
    interface Counter<TMilli, uint32_t> as Uptime;
#endif

#ifdef DELUGE
    interface DelugeMetadata;
    interface BootImage;
#endif
#ifdef FLASH
    interface BlockRead;
    interface BlockWrite;
#endif
    interface Timer<TMilli> as MilliTimer;
    
#if defined(VOICEMODULE)
    interface VStamp;
#endif 

#if defined(CURRENTSWITCH)
    interface CurrentSwitch;
#else
    interface Read<acc_read_t*>;
#endif
    
    
  }

} implementation {
  
  // data buffers
  uint16_t m_dataX[SAMPLES_PER_PACKET];   // nx_uint nutzen
  uint16_t m_dataY[SAMPLES_PER_PACKET];
  uint16_t m_dataZ[SAMPLES_PER_PACKET];
  uint8_t m_dataIndex;
  uint16_t m_counter;
  

  // bool session_active;
  struct sockaddr_in6 m_session_endpoint;     // endpoint-address of connection (netcat6-connection)
  struct sockaddr_in6 m_adc_server_ip;        // endpoint-address for sending the ADC-packets
  uint32_t m_boot_time;
  uint64_t m_uptime;

  event void Boot.booted() {
    m_uptime = 0;
#if defined(PLATFORM_TELOSB) || defined(PLATFORM_EPIC) || defined(CURRENTSWITCH)
    m_boot_time = call Uptime.get();
#endif
    m_counter = 0;
    m_dataIndex = 0;
  }

#if defined(PLATFORM_TELOSB) || defined(PLATFORM_EPIC) || defined(CURRENTSWITCH)
  async event void Uptime.overflow() {
    atomic
      m_uptime += 0xffffffff;
  }
#endif

  
  /**
   * Periodic sampling timer has fired - start the sampling process
   */
  event void MilliTimer.fired() {
    dbg("TestSmartObjC", "TestSmartObjC: timer fired\n");
    
#ifndef CURRENTSWITCH
    call Read.read();
#endif
  }

  // array for storing the X,Y,Z-information
  // uint16_t, because storing 14bit ADC values
  // space for packet-counter uint16_t and sample_period uint16_t (that means 2 extra uint16_t)  
  uint16_t m_data_msg[SAMPLES_PER_PACKET*3 + 2];
  uint16_t sample_period;
  
  /**
   * Reading ADC done
   */
#ifndef CURRENTSWITCH
  event void Read.readDone(error_t result, acc_read_t* val) {
    int i;
    
    // ZU KURZ... überschreibt m_data_msg
    //char results[SAMPLES_PER_PACKET*4*3 + 4*4];
    

    dbg("TestSmartObjC", "TestSmartObjC: read done\n");
    
    // buffer data
    m_dataX[m_dataIndex] = val->x;
    m_dataY[m_dataIndex] = val->y;
    m_dataZ[m_dataIndex] = val->z;
    m_dataIndex++;

    // wait until buffer full
    if ( m_dataIndex < SAMPLES_PER_PACKET ) return;
    m_dataIndex=0;

    // create packet
    // Generate a array with all X/Y/Z-Values
    // SAMPLES_PER_PACKET X/Y/Z-triples
    // SAMPLES_PER_PACKET*3 values
    // SAMPLES_PER_PACKET*3*2 bytes
    // order: (X.H X.L)*SAMPLES_PER_PACKET (Y.H Y.L)*SAMPLES_PER_PACKET (Z.H Z.L)*SAMPLES_PER_PACKET
    for ( i=0; i<SAMPLES_PER_PACKET; i++) {
      m_data_msg[i]                     = m_dataX[i];
      m_data_msg[i+SAMPLES_PER_PACKET]  = m_dataY[i];
      m_data_msg[i+2*SAMPLES_PER_PACKET]= m_dataZ[i];
    }
    
    // add packet-counter
    // store packet counter: higher word of counter, lower word of counter
    // then increase counter
    m_data_msg[SAMPLES_PER_PACKET*3] = m_counter;
    m_data_msg[SAMPLES_PER_PACKET*3 + 1] = sample_period;
    m_counter++;
    
    // send package
    // length of packet is already known, it is = (number of elements in array) * (size of one element)
    call UDP.sendto(&m_adc_server_ip, m_data_msg, (SAMPLES_PER_PACKET*3 + 2)*sizeof(uint16_t) );
  }
#endif

  struct cmd_str {
    uint8_t c_len;
    char    c_name[10];
    void (*action)(int, char **);
  };

  // and corresponding indeces
  enum {
    // the maximum number of arguments a command can take
    N_ARGS = 10,
    MAX_REPLY_LEN = 128,
    CMD_HELP = 0,
    CMD_ECHO = 1,
    CMD_PING6 = 2,
    CMD_TRACERT6 = 3,

    CMD_NO_CMD = 0xfe,
  };
  
  char reply_buf[MAX_REPLY_LEN];
  char *help_str = "sdsh-0.0\tbuiltins: [help, ping6, startadc, stopadc]\n";
  const char *ping_fmt = "%x%02x:%x%02x:%x%02x:%x%02x:%x%02x:%x%02x:%x%02x:%x%02x: icmp_seq=%i ttl=%i time=%i ms\n";
  const char *ping_summary = "%i packets transmitted, %i received\n";

  void action_test(int argc, char **argv) {
    char *test_str = "Hello World!\n";
    call UDP.sendto(&m_session_endpoint, test_str, strlen(test_str));
  }


  char sampling=0;  /* set to true, when started sampling */
  
  /*
   * parse target IP, and start sampling with given frequency
   * Timer.fired will read ADC and send values over UDP to the target IP
   */
  void action_startadc(int argc, char **argv) {

    char *start_msg = "ADC sampling started. Sending to address ";
    char *err_msg = "ADC sampling failed. Usage: startadc sampling-period target-address\n";
    char ipv6_address[50];/* TODO: remove this, debug only */
    nx_uint16_t target_port;
    char *already_msg = "ADC already sampling. Use stopadc first.\n";
    
    if ( sampling==1 ) {
        call UDP.sendto(&m_session_endpoint, already_msg, strlen(already_msg));
        return;
    } else {
        sampling = 1;
    }
    
    // set default values
    memcpy(&m_adc_server_ip,&m_session_endpoint, sizeof(struct sockaddr_in6));
    
    switch(argc) {
      default: // ignore too many arguments
      case 4: m_adc_server_ip.sin_port  = hton16(atoi(argv[3])); // destination port
      // second argument is the target address
      // problem with long address? (2001:620:8:1002:210:dcff:fe45:e3fb got 2001:620:8:1002:210:dcff:fe45:0! )
      //     yep... bug reported. as workaround, put a : after the IPv6 address
       /* convert the argument (string) to an IPv6-address (array) and save it into m_adc_server_ip.sin_addr */
      case 3: inet6_aton(argv[2], m_adc_server_ip.sin_addr); // convert and save destination address
      case 2: sample_period = atoi(argv[1]); break;// sampling period
      case 1: 
              call UDP.sendto(&m_session_endpoint, err_msg, strlen(err_msg));
              sampling = 0;
              return;
    }

    // we're starting with first packet
    m_counter = 0;
    
    call MilliTimer.startPeriodic(sample_period);   // start timer with given frequency => will read ADC when timer fired
    call UDP.sendto(&m_session_endpoint, start_msg, strlen(start_msg));   // send message to endpoint, telling that everything started
    call UDP.sendto(&m_session_endpoint, argv[2], strlen(argv[2]));
    call UDP.sendto(&m_session_endpoint, "\n", strlen("\n"));
    sprintf(ipv6_address, "%02X%02X:%02X%02X:%02X%02X:%02X%02X:%02X%02X:%02X%02X:%02X%02X:%02X%02X\n", m_adc_server_ip.sin_addr[0], m_adc_server_ip.sin_addr[1], m_adc_server_ip.sin_addr[2], m_adc_server_ip.sin_addr[3], 
                                                                     m_adc_server_ip.sin_addr[4], m_adc_server_ip.sin_addr[5], m_adc_server_ip.sin_addr[6], m_adc_server_ip.sin_addr[7], 
                                                                     m_adc_server_ip.sin_addr[8], m_adc_server_ip.sin_addr[9], m_adc_server_ip.sin_addr[10], m_adc_server_ip.sin_addr[11],
                                                                     m_adc_server_ip.sin_addr[12], m_adc_server_ip.sin_addr[13], m_adc_server_ip.sin_addr[14], m_adc_server_ip.sin_addr[15]); /* TODO: remove this, debug only */
    call UDP.sendto(&m_session_endpoint, ipv6_address, strlen(ipv6_address));
  }
  
  /**
   * stop sampling ADC values
   */
  void action_stopadc(int argc, char **argv) {
    char *stop_msg = "ADC sampling stopped\n";
    call MilliTimer.stop();                         // stop timer
    m_counter = 0;                                    // reset packet counter
    sampling = 0;
    call UDP.sendto(&m_session_endpoint, stop_msg, strlen(stop_msg));     // send message to endpoint and tell that ADC was stopped
  }

  void action_help(int argc, char **argv) {
    call UDP.sendto(&m_session_endpoint, help_str, strlen(help_str));
  } 

  void action_ping6(int argc, char **argv) {
    ip6_addr_t dest;

    if (argc < 2) return;
    inet6_aton(argv[1], dest);
    call ICMPPing.ping(dest, 1024, 10);
  } 


#if defined(VOICEMODULE)
  char m_strSpeechBuffer[256];
  // copy the speech data and issue command to speak it
  void action_say(int argc, char **argv) {
    char *test_str = "done talking\n";
    int i,j=0;
    char* pCurByte;
    for ( i=1; i<argc; i++ ) {
      pCurByte = argv[i];
      while( *pCurByte != '\0' ) {
        m_strSpeechBuffer[j++] = *(pCurByte++);
        if ( j >= sizeof(m_strSpeechBuffer)-1 ) break;
      }
      m_strSpeechBuffer[j++] = ' ';
      if ( j >= sizeof(m_strSpeechBuffer)-1 ) break;
    }
    m_strSpeechBuffer[j] = '\0';
    call VStamp.stop();
    call VStamp.sendText(m_strSpeechBuffer);

    call UDP.sendto(&m_session_endpoint, test_str, strlen(test_str));    
  }
  
	event void VStamp.uartReady() {
	}
#endif 

#if defined(CURRENTSWITCH)

  void action_switch(int argc, char **argv) {
    char *test_str = "Switching on";
    
    call CurrentSwitch.toggle();
    call Leds.led0Toggle();
    
    call UDP.sendto(&m_session_endpoint, test_str, strlen(test_str));    
  }
#endif  
  
  
  // m_commands 
  // (!!!) N_COMMANDS muss angepasst werden, wenn ein Command hinzugefügt wird!
  struct cmd_str m_commands[] = {
                                         {4, "help", action_help},
                                         {5, "ping6", action_ping6},
                                         {4, "test", action_test},
                                         {8, "startadc", action_startadc},
                                         {7, "stopadc", action_stopadc},
#if defined(VOICEMODULE)
                                         {3, "say", action_say},
#endif
#if defined(CURRENTSWITCH)
                                         {6, "switch", action_switch},
#endif
                                        };

  // break up a command given as a string into a sequence of null terminated
  // strings, and initialize the argv array to point into it.
  void init_argv(char *cmd, uint16_t len, char **argv, int *argc) {
    int inArg = 0;
    *argc = 0;
    while (len > 0 && *argc < N_ARGS) {
      if (*cmd == ' ' || *cmd == '\n' || *cmd == '\t' || *cmd == '\0' || len == 1){
        if (inArg) {
          *argc = *argc + 1;
          inArg = 0;
          *cmd = '\0';
        }
      } else if (!inArg) {
        argv[*argc] = cmd;
        inArg = 1;
      }
      cmd ++;
      len --;
    }
  }

  int lookup_cmd(char *cmd) {
    int i;
    for (i = 0; i < sizeof(m_commands); i++) {
      if (memcmp(cmd, m_commands[i].c_name, m_commands[i].c_len) == 0 
          && cmd[m_commands[i].c_len] == '\0')
        return i;
    }
    return CMD_NO_CMD;
  }

  event void UDP.recvfrom(struct sockaddr_in6 *from, void *data, 
                          uint16_t len, struct ip_metadata *meta) {
    char *argv[N_ARGS];
    int argc, cmd;

    if (len < 4) return;
    
    memcpy(&m_session_endpoint, from, sizeof(struct sockaddr_in6));
    init_argv((char *)data, len, argv, &argc);

    if (argc > 0) {
      cmd = lookup_cmd(argv[0]);
      if (cmd != CMD_NO_CMD) {
        m_commands[cmd].action(argc, argv);
      }
    }
  }

  event void ICMPPing.pingReply(ip6_addr_t source, struct icmp_stats *stats) {
    int len;
    len = snprintf(reply_buf, MAX_REPLY_LEN, ping_fmt,
                   source[0],source[1],source[2],source[3],
                   source[4],source[5],source[6],source[7],
                   source[8],source[9],source[10],source[11],
                   source[12],source[13],source[14],source[15],
                   stats->seq, stats->ttl, stats->rtt);
    call UDP.sendto(&m_session_endpoint, reply_buf, len);

  }

  event void ICMPPing.pingDone(uint16_t ping_rcv, uint16_t ping_n) {
    int len;
    len = snprintf(reply_buf, MAX_REPLY_LEN, ping_summary, ping_n, ping_rcv);
    call UDP.sendto(&m_session_endpoint, reply_buf, len);
  }
#ifdef DELUGE
  uint8_t volumeID2imgNum(uint8_t volumeID) {
    switch(volumeID) {
    case VOLUME_GOLDENIMAGE: return 0;
    case VOLUME_DELUGE1: return 1;
    case VOLUME_DELUGE2: return 2;
    case VOLUME_DELUGE3: return 3;
    }
  }
  event void DelugeMetadata.readDone(uint8_t imgNum, DelugeIdent* ident, error_t error) {
    int len;
    if (error == SUCCESS) {
      if (ident->uidhash != DELUGE_INVALID_UID) {
        len = snprintf(reply_buf, MAX_REPLY_LEN,
                       "image: %i\n\t[size: %li]\n\t[name: %s]\n\t[user: %s]\n\t[host: %s]\n\t[arch: %s]\n\t[time: 0x%lx]\n",
                       volumeID2imgNum(imgNum), ident->size, (char *)ident->appname, (char *) ident->username,
                       (char *)ident->hostname, (char *)ident->platform, (uint32_t)ident->timestamp);
        nwprog_validvols++;
        call UDP.sendto(&m_session_endpoint, reply_buf, len);
      }
      
    }
    if (++nwprog_currentvol < DELUGE_NUM_VOLUMES) {
      call DelugeMetadata.read(imgNum2volumeId(nwprog_currentvol));
    } else {
      len = snprintf(reply_buf, MAX_REPLY_LEN,
                     "%i valid image(s)\n", nwprog_validvols);
      call UDP.sendto(&m_session_endpoint, reply_buf, len);
    }
  }
#endif


}
