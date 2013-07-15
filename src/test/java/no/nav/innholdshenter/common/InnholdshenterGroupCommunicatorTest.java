package no.nav.innholdshenter.common;

import org.jgroups.JChannel;
import org.jgroups.Message;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class InnholdshenterGroupCommunicatorTest {

    private static String identifingGroupName = "innholdshenterCacheSyncGroup";
    private static String TEST_STRING = "Test!";
    InnholdshenterGroupCommunicator innholdshenterGroupCommunicator;
    @Mock
    EnonicContentRetriever innholdshenter;
    @Mock
    JChannel jChannel;

    @Before
    public void setUp() throws Exception {
        innholdshenterGroupCommunicator = new InnholdshenterGroupCommunicator(identifingGroupName, innholdshenter);
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
    public void updateAllNodes_should_call_send_with_a_message() throws Exception {
        innholdshenterGroupCommunicator.sendUpdateToNodes();
        verify(jChannel).send(any(Message.class));
    }

    @Test
    public void should_call_resetCache_function_message_is_received() {
        Message m = new Message(null, TEST_STRING);
        innholdshenterGroupCommunicator.receive(m);
        verify(innholdshenter).refreshCache();
    }

}
