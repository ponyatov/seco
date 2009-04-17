package seco.talk;

import java.awt.Rectangle;
import java.util.Map;
import java.util.UUID;

import org.hypergraphdb.HGHandle;
import org.hypergraphdb.HGPersistentHandle;
import org.hypergraphdb.peer.HGPeerIdentity;
import org.hypergraphdb.peer.HyperGraphPeer;
import org.hypergraphdb.peer.Message;
import org.hypergraphdb.peer.Messages;
import org.hypergraphdb.peer.protocol.Performative;
import org.hypergraphdb.peer.workflow.Activity;
import org.hypergraphdb.peer.workflow.WorkflowState;

import seco.ThisNiche;
import seco.U;
import seco.events.EventDispatcher;
import seco.gui.GUIHelper;
import seco.things.CellGroup;
import static org.hypergraphdb.peer.Messages.*;
import static org.hypergraphdb.peer.Structs.*;

/**
 * <p>
 * This activity handles communication b/w 
 * </p>
 * 
 * @author Borislav Iordanov
 *
 */
public class TalkActivity extends Activity
{
    public static final String TYPENAME = "seco-talk";
    
    private HGPeerIdentity friend;
    private TalkPanel talkPanel;
    
    void openPanel()
    {
        talkPanel = new TalkPanel(this);
        talkPanel.setFriend(friend);
        CellGroup group = ThisNiche.hg.get(ThisNiche.TOP_CELL_GROUP_HANDLE);
        HGHandle h = ThisNiche.hg.add(talkPanel);
        GUIHelper.addToCellGroup(h, 
                                    group,  
                                    null, 
                                    null, 
                                    new Rectangle(500, 200, 200, 100), 
                                    true);         
    }
    
    private void initFriend(Message msg)
    {
        HGPeerIdentity id = getThisPeer().getIdentity(getSender(msg));
        if (friend == null)
        {
            friend = id;
            openPanel();
        }
        else if (!friend.equals(id))
            throw new RuntimeException("Wrong activity for Talk msg, received from " + 
                                       id + ", but expecting " + friend);
    }
    
    public TalkActivity(HyperGraphPeer thisPeer)
    {
        super(thisPeer);
    }

    public TalkActivity(HyperGraphPeer thisPeer, HGPeerIdentity friend)
    {
        this(thisPeer);
        this.friend = friend;
        openPanel();
    }
    
    public TalkActivity(HyperGraphPeer thisPeer, UUID id)
    {
        super(thisPeer, id);
    }

    public TalkActivity(HyperGraphPeer thisPeer, UUID id, HGPeerIdentity friend)
    {
        super(thisPeer, id);
        this.friend = friend;
        openPanel();
    }

    public void chat(String text)
    {
        assert friend == null : new RuntimeException("No destination for TalkActivity.");
        Message msg = makeReply(this, Performative.QueryRef, null);
        combine(msg, struct(Messages.CONTENT, 
          struct("type", "chat", "text", text)));
        post(friend, msg);
    }
    
    public void sendAtom(HGHandle atom)
    {
        assert friend == null : new RuntimeException("No destination for TalkActivity.");
        Message msg = makeReply(this, Performative.QueryRef, null);
        combine(msg, struct(Messages.CONTENT, 
          struct("type", "atom", "atom", atom)));
        post(friend, msg);
    }
    
    public void close()
    {
        getState().assign(WorkflowState.Completed);
    }
    
    @Override
    public void handleMessage(Message msg)
    {
        initFriend(msg);
        if (msg.getPerformative() == Performative.AcceptProposal)
            return;
        Map<String, Object> content = getPart(msg, CONTENT);
        String type = (String)content.get("type");
        assert type != null : new RuntimeException("No type in TalkActivity content.");
        if ("start-chat".equals(type))
        {
            Message reply = getReply(msg, Performative.AcceptProposal);
            send(friend, reply);            
        }
        else if ("chat".equals(type))
        {
            String text = getPart(content, "text");
            assert text != null : new RuntimeException("No text in TalkActivity chat.");            
//            EventDispatcher.dispatch(U.hgType(ChatEvent.class), 
//                                     friend.getId(), 
//                                     new ChatEvent(friend, text));
            talkPanel.chatFrom(friend, text);
        }
        else if ("atom".equals(type))
        {
            HGPersistentHandle atomHandle = getPart(content, "atom");
            EventDispatcher.dispatch(U.hgType(ChatEvent.class), 
                                     friend.getId(), 
                                     new AtomProposedEvent(friend, atomHandle));            
        }
        else
            throw new RuntimeException("Unreadable TalkActivity message content " + content);
    }

    @Override
    public void initiate()
    {
        assert friend == null : new RuntimeException("No destination for TalkActivity.");
        Message msg = createMessage(Performative.Propose, TalkActivity.this);
        combine(msg, struct(Messages.CONTENT, struct("type", "start-chat")));
        post(friend, msg);
    }
     
    @Override
    public String getType()
    {
        return TYPENAME;
    }
    
    
}