package seco.gui;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.text.Element;

import org.hypergraphdb.HGHandle;
import org.hypergraphdb.HGHandleFactory;
import org.hypergraphdb.util.Pair;

import edu.umd.cs.piccolo.util.PBounds;

import seco.ThisNiche;
import seco.gui.visual.JComponentVisual;
import seco.gui.visual.NBUIVisual;
import seco.gui.visual.VisualAttribs;
import seco.notebook.NotebookDocument;
import seco.notebook.NotebookTransferHandler;
import seco.things.CellGroup;
import seco.things.CellGroupMember;
import seco.things.CellUtils;

/*
 * PiccoloCanvas's TransferHandler.
 */
public class PCTransferHandler extends TransferHandler
{
    private PiccoloCanvas canvas;

    public PCTransferHandler(PiccoloCanvas canvas)
    {
        this.canvas = canvas;
    }

    @Override
    public boolean importData(TransferSupport support)
    {
        Transferable t = support.getTransferable();
        if (!hasFlavor(t.getTransferDataFlavors())) return false;
        Point pt = support.getDropLocation().getDropPoint();
        try
        {
            if(!support.isDrop()) return false;
            if (support.isDataFlavorSupported(SecoTransferable.FLAVOR))
            {
                if (is_nested(support)) return false;
            }
            
            Pair<Boolean, Boolean> pair = check_and_handle_in_nodes(support);
            if (pair.getFirst()) 
                return pair.getSecond();
            if (support.isDataFlavorSupported(SecoTransferable.FLAVOR))
                return handle_seco_transfer(support);
            // Notebook Elements transferable
            Vector<Element> els = (Vector<Element>) t
                    .getTransferData(NotebookTransferHandler.FLAVOR);
            boolean move = (support.getDropAction() == MOVE);
            for (Element e : els)
            {
                HGHandle nbH = NotebookDocument.getNBElementH(e);
                if(CellUtils.isBackuped(nbH))
                    CellUtils.restoreCell(nbH, false);
                boolean outputC = NotebookDocument.isOutputCell(e);
                HGHandle vis = (outputC) ? JComponentVisual.getHandle()
                        : NBUIVisual.getHandle();
                CellGroup group = ThisNiche.graph.get(canvas.getGroupH());
                Map<Object, Object> attribs = new HashMap<Object, Object>();
                attribs.put(VisualAttribs.showTitle, true);
                if (move) GUIHelper.addToCellGroup(nbH, group, vis, null,
                        new Rectangle(pt.x, pt.y, 200, 200), false, attribs, -1);
                else
                    GUIHelper.addToCellGroup(CellUtils.makeCopy(nbH), group,
                            vis, null, new Rectangle(pt.x, pt.y, 200, 200),
                            false, attribs, -1);
            }
            return true;
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        return false;
    }
    
    private HGHandle getTransferedHandle(TransferSupport support)
    {
        try
        {
            return (HGHandle) support.getTransferable().getTransferData(
                    SecoTransferable.FLAVOR);
        }catch(Exception ex)
        {
            return ThisNiche.graph.getHandleFactory().nullHandle();
        }
    }

    // Pair /handled, result/
    private Pair<Boolean, Boolean> check_and_handle_in_nodes(
            TransferSupport support) throws UnsupportedFlavorException,
            IOException
    {
        Point pt = support.getDropLocation().getDropPoint();
        HGHandle data = getTransferedHandle(support);
        HGHandle parent_group = (data.equals(ThisNiche.graph.getHandleFactory().nullHandle())) ? null :
            ThisNiche.handleOf(CellUtils.getParentGroup(data));
        for (PSwingNode node : canvas.getNodes())
        {
            if (node.getFullBounds().contains(pt))
            {
                if (data.equals(node.getHandle()))
                    return new Pair<Boolean, Boolean>(true, false);
                TransferHandler handler = node.getComponent()
                        .getTransferHandler();
                if (handler != null)
                {
                    boolean res = handler.importData(support);
                    System.out.println("PCTransferHandler - inner done: " + res
                            + ":" + node.getComponent() + ":"
                            + support.getComponent());
                    //PCTransferHandler removes itself the moved handle
                    //this hacky approach is due to the fact that importDone()
                    //is protected
                    if (res && parent_group != null &&
                            !(handler instanceof PCTransferHandler) &&
                            support.getDropAction() == MOVE)
                    {
                         GUIHelper.removeFromCellGroup(parent_group, data, false);
                    }
                    return new Pair<Boolean, Boolean>(true, res);
                }else //no handler, do nothing
                    return new Pair<Boolean, Boolean>(true, false);
            }
        }
        return new Pair<Boolean, Boolean>(false, false);
    }

    // can't copy some group inside itself
    private boolean is_nested(TransferSupport support) throws IOException, UnsupportedFlavorException
    {
        HGHandle data = getTransferedHandle(support);
        CellGroupMember cgm = ThisNiche.graph.get(data);
        if (!(cgm instanceof CellGroup)) return false;
        CellGroup group = (CellGroup) cgm;
        CellGroup top = ThisNiche.graph.get(canvas.getGroupH());
        while (top != null)
        {
            if (group == top) return true;
            top = CellUtils.getParentGroup(ThisNiche.handleOf(top));
        }
        return false;
    }

    boolean handle_seco_transfer(TransferSupport support)
            throws UnsupportedFlavorException, IOException
    {
        Point pt = support.getDropLocation().getDropPoint();
        HGHandle data = (HGHandle) support.getTransferable().getTransferData(
                SecoTransferable.FLAVOR);
        boolean move = (support.getDropAction() == MOVE);
        CellGroup old_group = CellUtils.getParentGroup(data);
        CellGroup top = ThisNiche.graph.get(canvas.getGroupH());
        if (old_group == top) return false;
        if (move)
        {
            old_group.remove((CellGroupMember) ThisNiche.graph.get(data), false);
            add_to_top_group(top, data, pt);
        }
        else
            add_to_top_group(top, CellUtils.makeCopy(data), pt);
        return true;
    }

    private void add_to_top_group(CellGroup top, HGHandle h, Point pt)
    {
        CellGroupMember cgm = ThisNiche.graph.get(h);
        Rectangle r = CellUtils.getAppropriateBounds(cgm);
        if (r == null) 
            r = new Rectangle(pt.x, pt.y, 300, 200);
        else
        {
            //the point is given in main canvas coords
            //so we should transform it... 
            if(canvas.nested && GUIHelper.getPSwingNode(canvas) != null)
            {
                PSwingNode canv_node = GUIHelper.getPSwingNode(canvas);
                PBounds fb = canv_node.getFullBounds();
                System.out.println("adjust_bounds: " + r + ":" + fb);
                r.x = (int) (pt.x - fb.x);
                r.y = (int) (pt.y - fb.y);
            }else
            {
                r.x = pt.x;
                r.y = pt.y;
            }
        }
        CellUtils.setAppropriateBounds(cgm, r, true);
        cgm.getAttributes().put(VisualAttribs.showTitle, true);
        top.insert(top.getArity(), h);
    }

    protected boolean hasFlavor(DataFlavor[] flavors)
    {
        for (int i = 0; i < flavors.length; i++)
            if (flavors[i].equals(SecoTransferable.FLAVOR)
                    || flavors[i].equals(NotebookTransferHandler.FLAVOR))
                return true;
        return false;
    }

    public boolean canImport(JComponent c, DataFlavor[] flavors)
    {
        return hasFlavor(flavors);
    }

    public int getSourceActions(JComponent c)
    {
        return COPY_OR_MOVE;
    }

    protected Transferable createTransferable(JComponent comp)
    {
        PiccoloCanvas canvas = (PiccoloCanvas) comp;
        PSwingNode node = canvas.getSelectedPSwingNode();
        return new SecoTransferable(node.getHandle());
    }

}