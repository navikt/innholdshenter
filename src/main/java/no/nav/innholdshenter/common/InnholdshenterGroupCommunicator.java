package no.nav.innholdshenter.common;

import org.jgroups.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

public class InnholdshenterGroupCommunicator extends ReceiverAdapter {
    private static Logger logger = LoggerFactory.getLogger(InnholdshenterGroupCommunicator.class);
    private EnonicContentRetriever innholdshenter;
    private String identifingGroupName = "innholdshenterCacheSyncGroup-";
    private JChannel channel;

    /**
     * identifyingAppName is used to identify the group of apps this app will communicate with.
     *
     * @param identifyingAppName - group identifier.
     * @param innholdshenter
     */
    public InnholdshenterGroupCommunicator(String identifyingAppName, EnonicContentRetriever innholdshenter) throws Exception {
        this.identifingGroupName += identifyingAppName;
        this.innholdshenter = innholdshenter;
        this.setupChannel();
    }

    @Override
    public void receive(Message message) {
        logger.debug("got message {} from: {}", message.getObject(), message.getSrc());
        if (message.getObject().equals("updateCache")) {
            innholdshenter.refreshCache(false);
        }
    }

    /**
     * Called when a new node enters the group.
     *
     * @param view
     */
    @Override
    public void viewAccepted(View view) {
        logger.info("New node entered group. View: {}", view);
    }

    public void sendUpdateToNodes() throws Exception {
        /* null in first param is a broadcast to all nodes in group */
        Message m = new Message(null, "updateCache");
        channel.send(m);
    }

    public void setJChannel(JChannel jChannel) throws Exception {
        this.channel = jChannel;
        setupChannel();
    }

    private void setupChannel() throws Exception {
        if (channel == null) {
            channel = new JChannel("jgroups.xml");
        }
        channel.setReceiver(this);
        channel.connect(this.identifingGroupName);
    }

    public List<Address> getMembers() {
        if(!channel.isConnected()) {
            return new LinkedList<Address>();
        }
        View view = channel.getView();
        return view.getMembers();
    }
}
