package seco.gui.piccolo;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.geom.Point2D;

import seco.ThisNiche;
import seco.gui.PSwingNode;
import seco.things.CellGroupMember;
import seco.things.CellUtils;
import edu.umd.cs.piccolo.event.PInputEvent;

public class MinimizeHandle extends PSmallBoundsHandle
{
    private int PREF_DIM = 10;
    protected PSwingNode node;
    
    public MinimizeHandle(PSwingNode node, int side, Point offsetP)
    {
        super(new OffsetPBoundsLocator(node, side, offsetP));
        this.node = node;
        setPaint(Color.orange);
        
        setWidth(PREF_DIM);
        setHeight(PREF_DIM);
        this.setShape(PNodeEx.ELLIPSE);
        this.setToolTip("Minimize");
    }
    
    public void endHandleDrag(Point2D aLocalPoint, PInputEvent aEvent)
    {
        relocateHandle();
        //setPaint(Color.yellow);
        CellGroupMember cgm = ThisNiche.hg.get(node.getHandle());
        CellUtils.toggleMinimized(cgm);
    }
    
    public Cursor getCursorFor(int side)
    {
        return new Cursor(Cursor.CROSSHAIR_CURSOR);
    }
    @Override
    public void relocateHandle()
    {
        // DO NOTHING WHEN DRAGING
        super.relocateHandle();
    }
   
}