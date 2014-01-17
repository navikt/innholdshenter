package no.nav.innholdshenter.common;

import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class InnholdshenterGroupCommunicatorTest {

    private static String TEST_STRING = "updateCache";
    InnholdshenterGroupCommunicator innholdshenterGroupCommunicator;
    @Mock
    EnonicContentRetriever innholdshenter;
    @Mock
    JChannel jChannel;

    @Before
    public void setUp() throws Exception {
        innholdshenterGroupCommunicator = new InnholdshenterGroupCommunicator("innholdshenterCacheSyncGroup", "localhost", 7800, innholdshenter);
        innholdshenterGroupCommunicator.setJChannel(jChannel);
    }

    @Test
    public void channel_setup_test() throws Exception {
        JChannel jc = mock(JChannel.class);
        innholdshenterGroupCommunicator.setJChannel(jc);
        verify(jc).connect(anyString());
        verify(jc).setReceiver(innholdshenterGroupCommunicator);
    }

    @Test
    public void should_not_call_update_when_wrong_message_received() {
        Message m = new Message(null, "feiler " + TEST_STRING);
        innholdshenterGroupCommunicator.receive(m);
        verify(innholdshenter, never()).refreshCache(false);
    }

    @Test
    public void updateAllNodes_should_call_send_with_a_message() throws Exception {
        innholdshenterGroupCommunicator.sendUpdateToNodes();
        verify(jChannel).send(any(Message.class));
    }

    @Test
    public void should_call_refreshCache_function_when_message_is_received() {
        Message m = new Message(null, TEST_STRING);
        innholdshenterGroupCommunicator.receive(m);
        verify(innholdshenter).refreshCache(false);
        verify(innholdshenter, never()).refreshCache(true);
    }

    @Test
    public void should_not_broadcast_refreshCache_when_message_is_received_to_avoid_infinite_loop() throws Exception {
        Message m = new Message(null, TEST_STRING);
        innholdshenterGroupCommunicator.receive(m);
        verify(innholdshenter).refreshCache(false);
        verify(innholdshenter, never()).refreshCache(true);
        verify(jChannel, never()).send(any(Message.class));
        verify(jChannel, never()).send(any(Address.class), any());
    }

}
