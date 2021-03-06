package seco.gui;

import java.awt.Component;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.hypergraphdb.HGHandle;
//import org.wonderly.swing.tabs.TabCloseEvent;
//import org.wonderly.swing.tabs.TabCloseListener;

import seco.ThisNiche;
import seco.actions.CGMActionsHelper;
import seco.actions.CommonActions;
import seco.events.AttributeChangeEvent;
import seco.events.CellGroupChangeEvent;
import seco.events.EvalCellEvent;
import seco.gui.common.SecoTabbedPane;
import seco.gui.menu.EnhancedMenu;
import seco.gui.menu.ScriptEngineProvider;
import seco.gui.visual.CellContainerVisual;
import seco.notebook.NotebookUI;
import seco.things.CellGroup;
import seco.things.CellGroupMember;
import seco.things.CellUtils;
import seco.util.GUIUtil;

public class TabbedPaneU
{
    public static final String CHILD_HANDLE_KEY = "CHILD_HANDLE";
    static final boolean DRAGGABLE_TABS = !TopFrame.PICCOLO;
    private static final String TAB_INDEX = "tab_index";
    private static final String UNTITLED = "Untitled";
    static JPopupMenu tabPopupMenu;
    private static JTabbedPane currentTP;

    public static HGHandle getHandleAt(JTabbedPane tp, int i)
    {
        Component c =  tp.getComponentAt(i);
        if(!(c instanceof JComponent)) return null;
        return (HGHandle) ((JComponent) c).getClientProperty(CHILD_HANDLE_KEY);
    }
     
    public static int getTabIndexForH(JTabbedPane tp, HGHandle h)
    {
        for (int i = 0; i < tp.getTabCount(); i++)
        {
            HGHandle inH = TabbedPaneU.getHandleAt(tp, i);
            if (h.equals(inH)) return i;
        }
        return -1;
    }
    
    private static void closeAt(JTabbedPane tp, int i)
    {
        if(i < 0 || i >= tp.getTabCount()) return;
        HGHandle h = getHandleAt(tp, i);
        ThisNiche.graph.unfreeze(h);
        CellGroup top = CellUtils.getParentGroup(h);
        top.remove(i, false);
       
        HGHandle visH = CellContainerVisual.getHandle();
        CellUtils.removeEventPubSub(CellGroupChangeEvent.HANDLE, h, visH, visH);
        CellUtils.removeEventPubSub(EvalCellEvent.HANDLE, h, visH, visH);
        CellUtils.removeEventPubSub(AttributeChangeEvent.HANDLE, h, visH, visH);
       
        if (tp.getTabCount() == 0 && ThisNiche.guiController.getFrame() != null) 
            ThisNiche.guiController.setTitle("Seco");
        else
            TabbedPaneU.updateFrameTitle(
                    getHandleAt(tp, tp.getSelectedIndex()));
    }

    private static int promptAndSaveDoc()
    {
        NotebookUI ui = NotebookUI.getFocusedNotebookUI();
        if (ui != null) ui.close();
        return JOptionPane.OK_OPTION;
    }

    private static JPopupMenu getTabPopupMenu()
    {
        if (tabPopupMenu != null) return tabPopupMenu;
        tabPopupMenu = new JPopupMenu();
        Action act = new AbstractAction("Close") {
            public void actionPerformed(ActionEvent e)
            {
                int res = promptAndSaveDoc();
                if (res == JOptionPane.CANCEL_OPTION
                        || res == JOptionPane.CLOSED_OPTION) return;
                int i = ((Integer) tabPopupMenu.getClientProperty(TAB_INDEX));
                closeAt(currentTP, i);
            }
        };

        tabPopupMenu.add(new JMenuItem(act));
        act = new AbstractAction("Close All") {
            public void actionPerformed(ActionEvent e)
            {
                currentTP.setSelectedIndex(currentTP.getTabCount() - 1);
                for (int i = currentTP.getTabCount() - 1; i >= 0; i--)
                {
                    int res = promptAndSaveDoc();
                    if (res == JOptionPane.CANCEL_OPTION
                            || res == JOptionPane.CLOSED_OPTION) continue;
                    closeAt(currentTP, i);
                }
            }
        };
        tabPopupMenu.add(new JMenuItem(act));
        act = new AbstractAction("Close All But Active") {
            public void actionPerformed(ActionEvent e)
            {
                currentTP.setSelectedIndex(currentTP.getTabCount() - 1);
                int index = ((Integer) tabPopupMenu
                        .getClientProperty(TAB_INDEX));
                for (int i = currentTP.getTabCount() - 1; i >= 0; i--)
                {
                    int res = promptAndSaveDoc();
                    if (res == JOptionPane.CANCEL_OPTION
                            || res == JOptionPane.CLOSED_OPTION || i == index)
                        continue;
                    closeAt(currentTP, i);
                }
            }
        };
        tabPopupMenu.add(new JMenuItem(act));
        tabPopupMenu.add(new EnhancedMenu("Set Default Language",
                new ScriptEngineProvider()));
        tabPopupMenu.add(new EnhancedMenu("Set Runtime Context",
                new CGMActionsHelper.RCListProvider(CGMActionsHelper.Scope.book)));
        act = new AbstractAction("Rename") {
            public void actionPerformed(ActionEvent e)
            {
                HGHandle h = getHandleAt(currentTP,currentTP.getSelectedIndex());
                if(h == null) return;
                if(CommonActions.renameCellGroupMember(h))
                {
                    currentTP.setTitleAt(currentTP.getSelectedIndex(), 
                            makeTabTitle(CellUtils.getName(
                                    (CellGroupMember) ThisNiche.graph.get(h))));
                    TabbedPaneU.updateFrameTitle(h);
                }
            }
        };
        tabPopupMenu.add(new JMenuItem(act));
        return tabPopupMenu;
    }

    public static String makeTabTitle(String title)
    {
        if (title == null || title.length() == 0) title = UNTITLED;
        else
        {
            int ind = title.lastIndexOf('/');
            ind = Math.max(ind, title.lastIndexOf('\\'));
            if (ind > 0) title = title.substring(ind + 1);
        }
        // System.out.println("makeTabTitle: " + title);
        return title;
    }

    public static final class TabbedPaneMouseListener extends MouseAdapter
    {
        protected JTabbedPane tabbedPane;

        public TabbedPaneMouseListener()
        {
        }

        public TabbedPaneMouseListener(JTabbedPane tabbedPane)
        {
            this.tabbedPane = tabbedPane;
        }

        public void mouseClicked(MouseEvent e)
        {
            if (SwingUtilities.isRightMouseButton(e))
            {
                Point pt = e.getPoint();
                for (int i = 0; i < tabbedPane.getTabCount(); i++)
                {
                    final Rectangle r = tabbedPane.getBoundsAt(i);
                    if (r == null || !r.contains(pt)) continue;
                    currentTP = tabbedPane;
                    NotebookUI.setFocusedNotebookUI(getNotebookUIAt(tabbedPane, i));
                    getTabPopupMenu().putClientProperty(TAB_INDEX, i);
                    if (TopFrame.PICCOLO)
                    {
                        Frame f = GUIUtil.getFrame(e.getComponent());
                        pt = getPoint(e, f);
                    }
                    getTabPopupMenu().show(tabbedPane, pt.x, pt.y);
                    break;
                }
            }
            if(GUIUtil.getFrame() != null)
                GUIUtil.getFrame().repaint();
            e.consume();
        }
        
        private Point getPoint(MouseEvent e, Frame f)
        {
            Point pt =  new Point(e.getX(), e.getY());
            if (e.getComponent() instanceof JComponent)
                return GUIUtil.adjustPointInPicollo((JComponent) e.getComponent(), pt);
            return pt;
        }
    }

    public static final class TabbedPaneChangeListener implements
            ChangeListener
    {
        protected JTabbedPane tabbedPane;

        public TabbedPaneChangeListener(JTabbedPane tabbedPane)
        {
            this.tabbedPane = tabbedPane;
        }

        public TabbedPaneChangeListener()
        {
            super();
        }

        public void stateChanged(ChangeEvent e)
        {
            if (tabbedPane.getSelectedIndex() == -1) return;
            NotebookUI.setFocusedNotebookUI(
                    getNotebookUIAt(tabbedPane, tabbedPane.getSelectedIndex()));
            TabbedPaneU.updateFrameTitle(
                    getHandleAt(tabbedPane,tabbedPane.getSelectedIndex()));
        }
    }

//    public static final class TabbedPaneCloseListener implements  TabCloseListener
//    {
//        public void tabClosed(TabCloseEvent evt)
//        {
//            int res = promptAndSaveDoc();
//            if (res == JOptionPane.CANCEL_OPTION
//                    || res == JOptionPane.CLOSED_OPTION) return;
//            closeAt((JTabbedPane) evt.getSource(), evt.getClosedTab());
//        }
//    }
    
    private static NotebookUI getNotebookUIAt(JTabbedPane tp, int index)
    {
        if (tp.getSelectedIndex() == -1) return null;
        Component c = tp.getComponentAt(index);
        if(c instanceof JScrollPane)
        {
           JScrollPane comp =  (JScrollPane) c; 
           Component inner = comp.getViewport().getView();
           if (inner instanceof NotebookUI) 
               return (NotebookUI) inner;
        }else if(c instanceof NotebookUI)
            return (NotebookUI) c;
        return null;
    }

    public static JTabbedPane createTabbedPane(CellGroup group)
    {
		JTabbedPane tabbedPane = new JTabbedPane();
		if (TopFrame.PICCOLO) {
//			if (!DRAGGABLE_TABS) {
//				tabbedPane = new CloseableDnDTabbedPane();
//				((CloseableDnDTabbedPane) tabbedPane).setPaintGhost(true);
//				// ((CloseableDnDTabbedPane) tabbedPane)
//				// .addTabCloseListener(new TabbedPaneCloseListener());
//			} else
				tabbedPane = new SecoTabbedPane(ThisNiche.handleOf(group));
			//tabbedPane.setDoubleBuffered(GUIUtil.getFrame().doubleBuffer());
			tabbedPane.putClientProperty(com.jgoodies.looks.Options.NO_CONTENT_BORDER_KEY, Boolean.TRUE);
			//tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		}
        tabbedPane.addMouseListener(new TabbedPaneMouseListener(tabbedPane));
        tabbedPane.addChangeListener(new TabbedPaneChangeListener(tabbedPane));
        return tabbedPane;
    }

    public static void updateFrameTitle(HGHandle h)
    {
        CellGroupMember book = ThisNiche.graph.get(h);
        String name = CellUtils.getName(book);
        if (name == null) name = "";
        // RuntimeContext rcInstance = (RuntimeContext)
        // ThisNiche.graph.get(TopFrame
        // .getInstance().getCurrentRuntimeContext());
        String title = "[" + ThisNiche.graph.getLocation() + "] "
        /* + rcInstance.getName() + " " */+ name;
        if (ThisNiche.guiController != null)
            ThisNiche.guiController.setTitle(title);
        // ThisNiche.guiController.showHTMLToolBar(false);
        GUIHelper.getHTMLToolBar().setEnabled(false);
    }
}
