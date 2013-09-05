package no.nav.innholdshenter.common;

import org.jgroups.protocols.TCP;
import org.jgroups.protocols.TCPPING;
import org.jgroups.protocols.VERIFY_SUSPECT;
import org.jgroups.protocols.pbcast.GMS;
import org.jgroups.protocols.pbcast.NAKACK;
import org.jgroups.protocols.pbcast.STABLE;
import org.jgroups.stack.IpAddress;
import org.jgroups.stack.ProtocolStack;
import org.jgroups.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;
import java.util.List;

/**
 * Klasse som programatisk konfigurerer JGroups for blant annet
 * å kunne sette initial_hosts og bind_port dynamisk
 *
 * Tilsvarer følgende xml:
 *
 * <pre>
 * {@code
 *     <config>
 *         <TCP bind_port="7800" />
 *         <TCPPING timeout="3000"
 *                  initial_hosts="HostA[7800],HostB[7800]"
 *                  port_range="1"
 *                  num_initial_members="2"/>
 *         <VERIFY_SUSPECT timeout="1500"  />
 *         <pbcast.NAKACK use_mcast_xmit="false"
 *                        retransmit_timeout="300,600,1200,2400,4800"
 *                        discard_delivered_msgs="true"/>
 *         <pbcast.STABLE stability_delay="1000" desired_avg_gossip="50000" max_bytes="400000"/>
 *         <pbcast.GMS print_local_addr="true" join_timeout="5000" view_bundling="true"/>
 *     </config>
 * }
 * </pre>
 **/
public class JGroupsConfig {

    private static Logger logger = LoggerFactory.getLogger(JGroupsConfig.class);
    private int jGroupsBindPort;
    private String jGroupsHosts;

    public JGroupsConfig(String jGroupsHosts, int jGroupsBindPort) {
        this.jGroupsHosts = jGroupsHosts;
        this.jGroupsBindPort = jGroupsBindPort;
    }

    /**
     * Genererer og returnerer en <code>ProtocolStack</code>
     * som kan settes på en JChannel
     * @return <code>ProtocolStack</code>
     * @throws UnknownHostException
     */
    public ProtocolStack getProtocolStack() throws UnknownHostException {
        ProtocolStack stack = new ProtocolStack();

        String initalHostsString = createInitialHostsString(jGroupsHosts, jGroupsBindPort);
        logger.debug("Initial hosts (host name) {}", initalHostsString);
        List<IpAddress> initalHosts = Util.parseCommaDelimitedHosts(initalHostsString, 0);
        logger.debug("Inital hosts (IP-address) {}", initalHosts);
        int[] retransmitTimeouts = Util.parseCommaDelimitedInts("300,600,1200,2400,4800");

        stack.addProtocol(new TCP()
                .setValue("bind_port", jGroupsBindPort));

        stack.addProtocol(new TCPPING()
                .setValue("timeout", 3000)
                .setValue("initial_hosts", initalHosts)
                .setValue("port_range", 0)
                .setValue("num_initial_members", initalHosts.size()));

        stack.addProtocol(new VERIFY_SUSPECT()
                .setValue("timeout", 1500));

        stack.addProtocol(new NAKACK()
                .setValue("use_mcast_xmit", false)
                .setValue("retransmit_timeouts", retransmitTimeouts)
                .setValue("discard_delivered_msgs", true));

        stack.addProtocol(new STABLE()
                .setValue("stability_delay", 1000)
                .setValue("desired_avg_gossip", 50000)
                .setValue("max_bytes", 400000));

        stack.addProtocol(new GMS()
                .setValue("print_local_addr", true)
                .setValue("join_timeout", 5000)
                .setValue("view_bundling", true));

        return stack;
    }

    private static String createInitialHostsString(String jGroupsHosts, int jGroupsBindPort) {
        StringBuilder result = new StringBuilder();
        String[] hosts = jGroupsHosts.split(",");

        if (hosts.length == 0) {
            throw new IllegalArgumentException("jGroupsHosts må inneholde minst én host. Faktisk verdi er: " + jGroupsHosts);
        }

        String hostString = String.format("%s[%s]", hosts[0], jGroupsBindPort);
        result.append(hostString);

        for (int i = 1; i < hosts.length; i++) {
            hostString = String.format("%s[%s]", hosts[i], jGroupsBindPort);
            result.append(",");
            result.append(hostString);
        }

        return result.toString();
    }

}
