/*
 * This file is part of the Scriba source distribution. This is free, open-source 
 * software. For full licensing information, please see the LicensingInformation file
 * at the root level of the distribution.
 *
 * Copyright (c) 2006-2007 Kobrix Software, Inc.
 */
package seco.notebook;

import static seco.notebook.ElementType.cellGroup;
import static seco.notebook.ElementType.cellGroupBox;
import static seco.notebook.ElementType.cellHandle;
import static seco.notebook.ElementType.charContent;
import static seco.notebook.ElementType.commonCell;
import static seco.notebook.ElementType.htmlCell;
import static seco.notebook.ElementType.inputCellBox;
import static seco.notebook.ElementType.insertionPoint;
import static seco.notebook.ElementType.notebook;
import static seco.notebook.ElementType.outputCellBox;
import static seco.notebook.ElementType.paragraph;

import java.awt.Font;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EventListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;

import javax.script.ScriptContext;
import javax.script.ScriptEngineManager;
import javax.script.SimpleScriptContext;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Element;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.Position;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.undo.CompoundEdit;
import javax.swing.undo.UndoableEdit;

import org.hypergraphdb.HGHandle;
import org.hypergraphdb.HGQuery.hg;

import seco.ThisNiche;
import seco.events.AttributeChangeEvent;
import seco.events.CellGroupChangeEvent;
import seco.events.CellTextChangeEvent;
import seco.events.CellTextChangeEvent.EventType;
import seco.events.EvalCellEvent;
import seco.events.EvalResult;
import seco.events.EventDispatcher;
import seco.events.handlers.AttributeChangeHandler;
import seco.events.handlers.CellGroupChangeHandler;
import seco.notebook.html.HTMLEditor;
import seco.notebook.style.NBStyle;
import seco.notebook.style.StyleAttribs;
import seco.notebook.style.StyleType;
import seco.notebook.syntax.ScriptSupport;
import seco.notebook.syntax.ScriptSupportFactory;
import seco.notebook.syntax.SyntaxStyle;
import seco.notebook.syntax.SyntaxStyleBean;
import seco.notebook.syntax.SyntaxUtilities;
import seco.rtenv.EvaluationContext;
import seco.things.Cell;
import seco.things.CellGroup;
import seco.things.CellGroupMember;
import seco.things.CellUtils;
import seco.util.Log;

/**
 * 
 * @author bizi
 */
public class NotebookDocument extends DefaultStyledDocument
{
    private static final String LABEL_EVALUATING = "Evaluating...";
    static final boolean DIRECT_EVENTING = true;
    private static final long serialVersionUID = -428517122037400179L;
    static final String ATTR_CELL = "CELL";
    public static final String ATTR_SCRIPT_SUPPORT = "scriptSupport";
    public static final String ATTR_HTML_EDITOR = "HTML_EDITOR";
    static SimpleAttributeSet contentAttrSet = new SimpleAttributeSet();
    static MutableAttributeSet charAttrSet = new SimpleAttributeSet();
    static
    {
        contentAttrSet.addAttribute(StyleConstants.NameAttribute, charContent);
        charAttrSet.addAttribute(StyleConstants.NameAttribute, paragraph);
    }
    public static Font DEFAULT_FONT = new Font("Default", Font.PLAIN, 12);
    protected final HGHandle bookH;

    protected boolean modified;
    protected ScriptEngineManager scriptManager;
    protected ScriptContext context = new SimpleScriptContext();
    protected EvaluationContext evalContext;
    protected Vector<UndoableEdit> removeUndo = new EditVector<UndoableEdit>();
    protected Vector<UndoableEdit> insertUndo = new EditVector<UndoableEdit>();
    // reusable object for compound events
    protected CompoundEdit cEdit = new NamedCompoundEdit(null);
    // stack for nested compound events
    protected Stack<String> editStack = new Stack<String>();
    boolean supressEvents = false;
    public ArrayList<SyntaxStyle> styles;
    private Map<String, SyntaxStyle[]> syntaxStyleMap = new HashMap<String, SyntaxStyle[]>();
    private Font outputCellFont = DEFAULT_FONT;
    private Font inputCellFont =  DEFAULT_FONT;

    protected Position TOP_INDEX_POS;
    protected Map<HGHandle, Position> indexes = new HashMap<HGHandle, Position>();

    public NotebookDocument(HGHandle h)
    {
        this(h, ThisNiche.getTopContext());
    }

    public NotebookDocument(HGHandle h, EvaluationContext evalContext)
    {
        bookH = h;
        this.evalContext = evalContext;
    }

    protected boolean inited;

    @SuppressWarnings("unchecked")
    public void init()
    {
        DocumentListener[] ls = listenerList.getListeners(DocumentListener.class);
        for(int i = 0; i < ls.length; i++)
            removeDocumentListener(ls[i]);
        if (inited) return;
        CellGroup book = ThisNiche.graph.get(bookH);
        Map<StyleType, NBStyle> map = (Map<StyleType, NBStyle>) book.getAttribute(XMLConstants.CELL_STYLE);
        if (map != null) for (NBStyle s : map.values())
            addStyle(s);
        Vector<ElementSpec> vec = new Vector<ElementSpec>();
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute(StyleConstants.Alignment, StyleConstants.ALIGN_LEFT);
        attr.addAttribute(ATTR_CELL, bookH);
        DocUtil.startTag(notebook, attr, 0, vec);
        DocUtil.createInsertionPoint(attr, vec);
        for (int i = 0; i < book.getArity(); i++)
            DocUtil.createCellGroupMember(this, book.getTargetAt(i), attr, vec);
        DocUtil.endTag(vec);
        create(vec.toArray(new ElementSpec[vec.size()]));
        setModified(false);
        if (NotebookDocument.DIRECT_EVENTING)
        {
            CellUtils.addEventPubSub(AttributeChangeEvent.HANDLE, bookH,
                    getHandle(), AttributeChangeHandler.getHandle());
            CellUtils.addEventPubSub(CellGroupChangeEvent.HANDLE, bookH,
                    getHandle(), CellGroupChangeHandler.getHandle());
        }
        else
        {
            CellUtils.addMutualEventPubSub(AttributeChangeEvent.HANDLE, bookH,
                    getHandle(), AttributeChangeHandler.getHandle());
            CellUtils.addMutualEventPubSub(CellGroupChangeEvent.HANDLE, bookH,
                    getHandle(), CellGroupChangeHandler.getHandle());
        }
        update(UpdateAction.tokenize);
        // update(UpdateAction.evalInitCells);
        inited = true;
    }

    protected HGHandle handle = null;

    public HGHandle getHandle()
    {
        if (handle != null) return handle;
        handle = ThisNiche.handleOf(this);
        if (handle != null) return handle;

        Set<HGHandle> list = CellUtils.findAll(ThisNiche.graph, hg
                .type(getClass()));
        for (HGHandle h : list)
            if (bookH.equals(((NotebookDocument) ThisNiche.graph.get(h)).bookH))
                return handle = h;
        handle = ThisNiche.graph.add(this);
        // System.out.println("Adding DOC: " + this);
        return handle;
    }

    @Override
    protected void create(ElementSpec[] data)
    {
        try
        {
            super.create(data);
            indexes.clear();
            indexes.put(bookH, TOP_INDEX_POS = createPosition(0));
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    protected AbstractElement createDefaultRoot()
    {
        BranchElement section = null;
        try
        {
            writeLock();
            SimpleAttributeSet attr = new SimpleAttributeSet();
            attr.addAttribute(StyleConstants.NameAttribute,
                    ElementType.fakeParagraph);
            section = new BlockElement(null, attr);
            BranchElement paragraph = new BranchElement(section, attr);
            LeafElement brk = new LeafElement(paragraph, null, 0, 1);
            Element[] buff = new Element[1];
            buff[0] = brk;
            paragraph.replace(0, 0, buff);
            buff[0] = paragraph;
            section.replace(0, 0, buff);
        }
        finally
        {
            writeUnlock();
        }
        return section;
    }

    public ScriptContext getScriptingContext()
    {
        return context;
    }

    public EvaluationContext getEvaluationContext()
    {
        if (evalContext == null)
        {
            HGHandle ctxH = CellUtils.getEvalContextH(getBook());
            if (ctxH == null) ctxH = ThisNiche.TOP_CONTEXT_HANDLE;
            evalContext = ThisNiche.getEvaluationContext(ctxH);
        }
        return evalContext;
    }

    // public void setEvaluationContext(EvaluationContext ctx)
    // {
    // evalContext = ctx;
    // }

    public CellGroupMember getBook()
    {
        return (CellGroupMember) ThisNiche.graph.get(bookH);
    }

    public HGHandle getBookHandle()
    {
        return bookH;
    }

    // last param could be null, passed for speed and used only with
    // UpdateAction.evalInitCells
    public void updateCell(Element inner, UpdateAction action, Object param)
            throws BadLocationException
    {
        // if (!(getNBElement(inner) instanceof Cell)) return;
        boolean out = isOutputCell(inner);
        if (UpdateAction.index == action) indexes.put(getNBElementH(inner),
                createPosition(inner.getStartOffset()));
        else if ((UpdateAction.syncronize == action
                || UpdateAction.tokenize == action || UpdateAction.resetTokenMarker == action)
                && !out)
        {
            Cell cell = (Cell) getNBElement(inner);
            if (UpdateAction.tokenize == action) createScriptSupport(
                    getLowerElement(inner, inputCellBox), false);
            else if (UpdateAction.resetTokenMarker == action) resetScriptSupport(getLowerElement(
                    inner, inputCellBox));
            else
            {
                Element html = getLowerElement(inner, htmlCell);
                if (html != null)
                {
                    HTMLEditor ed = (HTMLEditor) html.getAttributes()
                            .getAttribute(ATTR_HTML_EDITOR);
                    if (ed.isModified())
                    {
                        CellUtils.setCellText(cell, ed.getContent());
                        ed.setModified(false);
                    }
                }
            }
        }
    }

    void update(UpdateAction action)
    {
        Element root = getDefaultRootElement();// getRootElements()[0];
        for (int i = 0; i < root.getElementCount(); i++)
            update(root.getElement(i), action);
        setModified(true);
    }

    void update(Element el, UpdateAction action)
    {
        try
        {
            Object param = CellUtils.isInitCell(getBook());
            if (cellGroupBox == getElementType(el)) updateGroup(
                    getLowerElement(el, cellGroup), action, param);
            else if (inputCellBox == getElementType(el)
                    || outputCellBox == getElementType(el))
            {
                updateCell(el, action, param);
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    public void updateStyles()
    {
        syntaxStyleMap.clear();
        update(UpdateAction.resetTokenMarker);
        updateElement(getRootElements()[0]);
    }

    void updateGroup(Element el, UpdateAction action, Object param)
            throws BadLocationException
    {
        if (cellGroup != getElementType(el)) return;
        if (UpdateAction.index == action)
            indexes.put(getNBElementH(el), createPosition(el.getStartOffset()));

        for (int i = 0; i < el.getElementCount(); i++)
        {
            Element inner = el.getElement(i);
            if (cellGroupBox == getElementType(inner)) updateGroup(
                    getLowerElement(inner, cellGroup), action, param);
            else if (getWholeCellElement(inner.getStartOffset()) != null)
            {
                updateCell(inner, action, param);
            }
        }
    }

    void createScriptSupport(Element el, boolean force)
    {
        if (el == null) return;
        ScriptSupport sup = getScriptSupport(el.getStartOffset());
        CellGroupMember nb = getNBElement(el);
        if (!(nb instanceof Cell)) return;
        Cell cell = (Cell) nb;
        String name = DocUtil.getEngineName(this, cell);
        if (sup != null && sup.getFactory().getEngineName().equals(name)
                && !force) return;
        el = getLowerElement(el, inputCellBox);

        sup = null;
        try
        {
            ScriptSupportFactory factory = getEvaluationContext()
                    .getLanguageDescriptor(name).getEditSupportFactory();
            sup = factory.createScriptSupport(el.getElement(0));
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        if (sup == null)
        {
            if (force) removeAttribute(el, ATTR_SCRIPT_SUPPORT);
            return;
        }

        addAttribute(el, ATTR_SCRIPT_SUPPORT, sup);

    }

    public void addAttribute(Element el, String name, Object value)
    {
        try
        {
            writeLock();
            MutableAttributeSet set = (MutableAttributeSet) el.getAttributes();
            set.addAttribute(name, value);
        }
        finally
        {
            writeUnlock();
        }
    }

    private void removeAttribute(Element el, String name)
    {
        try
        {
            writeLock();
            MutableAttributeSet set = (MutableAttributeSet) el.getAttributes();
            set.removeAttribute(name);
        }
        finally
        {
            writeUnlock();
        }
    }

    private void resetScriptSupport(Element el)
    {
        ScriptSupport sup = getScriptSupport(el.getStartOffset());
        if (sup == null) return;
        el = getLowerElement(el, inputCellBox);
        if (el == null) return;
        sup = sup.getFactory().createScriptSupport(el.getElement(0));
    }

    public ScriptSupport getScriptSupport(int offset)
    {
        Element el = getUpperElement(getParagraphElement(offset), inputCellBox);
        if (el == null) return null;
        return (ScriptSupport) el.getAttributes().getAttribute(
                ATTR_SCRIPT_SUPPORT);
    }

    public void atomicLock()
    {
        writeLock();
    }

    public void atomicUnlock()
    {
        writeUnlock();
    }

    public enum UpdateAction
    {
        syncronize, tokenize, resetTokenMarker, index
    };

    public void evalGroup(CellGroup group)
    {
        ClassLoader save = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(this.evalContext.getClassLoader());
        try
        {
            CellUtils.evalGroup(group, getDefaultEngineName(), getEvaluationContext());
        }
        finally
        {
            Thread.currentThread().setContextClassLoader(save);    
        }
    }

    public void evalCellInAuxThread(Element el) throws BadLocationException
    {
        Cell cell = (Cell) getNBElement(el);

        // check if we already have an output cell. if not, add one
        if (!maybe_create_output_cell(getNBElementH(el), LABEL_EVALUATING))
        {
            EvalResult e = new EvalResult();
            e.setText(LABEL_EVALUATING);
            List<HGHandle> list = CellUtils
                    .getOutCellHandles(getNBElementH(el));
            for (HGHandle o : list)
            {
                int off = findElementOffset(o);
                if (off < 0) continue;
                try
                {
                    supressEvents = true;
                    update_output_cell(e, off);
                }
                finally
                {
                    supressEvents = false;
                }
            }
        }
        DocUtil.eval_result_in_aux_thread(this, cell);
    }

    // called from evaluation thread upon finish
    void handle_delayed_evaluation(final EvalResult res, final HGHandle cellH)
    {
        Runnable r = new Runnable() {
            public void run()
            {
                EvalCellEvent e = CellUtils.create_eval_event(cellH, res);
                fireUndoableEditUpdate(new UndoableEditEvent(this, e));
                supressEvents = true;
                if (DIRECT_EVENTING) EventDispatcher.dispatch(
                        EvalCellEvent.HANDLE, e.getCellHandle(), e);
                else
                    EventDispatcher.dispatch(EvalCellEvent.HANDLE, getHandle(),
                            e);
                supressEvents = false;
            }
        };
        SwingUtilities.invokeLater(r);
    }

    private boolean maybe_create_output_cell(HGHandle cellH, String text)
    {
        int offset = findElementOffset(cellH);
        if (offset < 0) return false;
        // TODO: should decide the problem with output cell
        // residing in deeper levels,
        // or floating around in HG if we adopt the other approach with:
        List<HGHandle> list = CellUtils.getOutCellHandles(cellH);
        if (!list.isEmpty()) return false;
        // insert empty output cell, which will populate itself later
        HGHandle outH = CellUtils.createOutputCellH(cellH, text, null, false);
        Element el = getUpperElement(offset, inputCellBox);
        CellGroup gr = (CellGroup) ThisNiche.graph.get(getContainerH(el));
        CellGroupChangeEvent ev = new CellGroupChangeEvent(ThisNiche
                .handleOf(gr), gr.indexOf(cellH) + 1, new HGHandle[] { outH },
                new HGHandle[0]);
        fireCellGroupChanged(ev);
        return true;
    }

    public void cellEvaled(EvalCellEvent e)
    {
        HGHandle cellH = e.getCellHandle();
        int offset = findElementOffset(cellH);
        if (offset < 0) return;
        try
        {
            supressEvents = true;
            List<HGHandle> list = CellUtils.getOutCellHandles(cellH);
            for (HGHandle o : list)
            {
                int off = findElementOffset(o);
                if (off < 0) continue;
                update_output_cell(e.getValue(), off);
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        finally
        {
            supressEvents = false;
        }
    }

    private void update_output_cell(final EvalResult e, int offset)
            throws BadLocationException
    {
        Element el = getUpperElement(offset, outputCellBox);
        int off = el.getStartOffset();
        Vector<ElementSpec> vec = new Vector<ElementSpec>();
        SimpleAttributeSet attr = new SimpleAttributeSet();
        super.remove(off, (el.getEndOffset() - off) + 1);
        DocUtil.endTag(vec);
        DocUtil.endTag(vec);
        DocUtil.createOutputCell(this, getNBElementH(el), attr, vec, true, e);
        insert(off, vec.toArray(new ElementSpec[vec.size()]));
        //on error it's better to stay in the input cell
        if(e.isError())
          fireCaretMoved(off - 3);
        else
          fireCaretMoved(findNextInsPoint(off));
    }

    // Element at offset should be InsertionPoint
    // Object String or NBElement
    // return the pos for the caret
    int insPointInsert(int offset, Object obj)
    {
        Element insP = getUpperElement(offset, insertionPoint);
        if (insP == null) return offset;
        int i = 0;
        CellGroup par = (CellGroup) getBook();
        if (offset > 0)
        {
            Element p = getEnclosingCellElement(offset);
            if (p == getRootElements()[0])
            {
                HGHandle prev_gr = (isOutputCell(offset - 1)) ? getNBElementH(getWholeCellElement(offset - 1))
                        : getNBElementH(getParagraphElement(offset - 1));
                i = par.indexOf(prev_gr) + 1;
            }
            else
            {
                par = (CellGroup) getNBElement(p);
                int n = 1;
                if (isInsertionPoint(offset - 1)) n++;
                Element temp = p.getElement(0);
                int ind = temp.getElementIndex(offset - n);
                HGHandle prev_gr = getNBElementH(temp.getElement(ind));
                par = (CellGroup) getNBElement(p);
                i = par.indexOf(prev_gr) + 1;
            }
        }

        HGHandle child = (obj instanceof HGHandle) ? (HGHandle) obj : null;
        int result = insP.getEndOffset();
        if (child == null)
        {
            String str = (String) obj;
            if (str == null) str = "\n";
            if (!str.endsWith("\n")) str += "\n";
            result = result + str.length(); // - 1;
            if (par == getBook())
            {
                child = CellUtils.createGroupHandle();
                CellGroup group = (CellGroup) ThisNiche.graph.get(child);
                group.insert(0, CellUtils.createCellHandle(this, str));
            }
            else
            {
                child = CellUtils.createCellHandle(this, str);
                result--;
            }
        }

        Object childAtom = ThisNiche.graph.get(child);

        if (!(childAtom instanceof CellGroup) && par == getBook())
        {
            HGHandle enc_gr = CellUtils.createGroupHandle();
            CellGroup group = (CellGroup) ThisNiche.graph.get(enc_gr);
            group.insert(0, child);
            // par.insert(i, enc_gr);
            fireCellGroupChanged(new CellGroupChangeEvent(ThisNiche
                    .handleOf(par), i, new HGHandle[] { enc_gr },
                    new HGHandle[0]));
        }
        else
        {
            // par.insert(i, child);
            fireCellGroupChanged(new CellGroupChangeEvent(ThisNiche
                    .handleOf(par), i, new HGHandle[] { child },
                    new HGHandle[0]));
        }
        return result;
    }

    private int findNextInsPoint(int pos)
    {
        Element el = getWholeCellElement(pos);
        if (el != null) return el.getEndOffset() + 1;
        return pos;
    }

    boolean insertElements(int offset, Vector<Element> elements)
            throws BadLocationException
    {
        Element c_el = getWholeCellElement(offset);
        Element gr_el = getUpperElement(offset, cellGroup);
        CellGroup cg = (CellGroup) getNBElement(gr_el);
        boolean isInsP = isInsertionPoint(offset);
        int ind = 0;
        if (!isInsP) ind = cg.indexOf(getNBElementH(c_el));
        for (int i = elements.size() - 1; i >= 0; i--)
        {
            Element el = elements.get(i);
            HGHandle child = getNBElementH(el);
            if(CellUtils.isBackuped(child)) 
                CellUtils.restoreCell(child, false);
            HGHandle copy = CellUtils.makeCopy(child);
            if (isInsP) insPointInsert(offset, copy);
            else
                fireCellGroupChanged(new CellGroupChangeEvent(ThisNiche
                        .handleOf(cg), ind + 1, new HGHandle[] { copy },
                        new HGHandle[0]));
        }
        return true;
    }

    void insert(HGHandle child, HGHandle parentH, int ind)
            throws BadLocationException
    {
        if (ind == -1) throw new BadLocationException("Wrong index", ind);
        int offset = findElementOffset(parentH);
        CellGroup parent = (CellGroup) ThisNiche.graph.get(parentH);
        if (ind == 0) offset++;
        else
        {
            HGHandle h = parent.getTargetAt(ind - 1);
            Element prev = getUpperElement(findElementOffset(h), h, true);
            offset = prev.getEndOffset() + 1;
        }

        Vector<ElementSpec> vec = new Vector<ElementSpec>();
        SimpleAttributeSet attr = new SimpleAttributeSet();

        DocUtil.endTag(vec);
        DocUtil.endTag(vec);
        DocUtil.createCellGroupMember(this, child, attr, vec);
        insert(offset, vec.toArray(new ElementSpec[vec.size()]));
        update(UpdateAction.tokenize);
    }

    public Element getUpperElement(int offset, ElementType type)
    {
        return getUpperElement(getParagraphElement(offset), type);
    }

    public static Element getUpperElement(Element el, ElementType type,
            boolean check_passed_in_el)
    {
        if (el == null) return null;
        if (check_passed_in_el && type == getElementType(el)) return el;
        return getUpperElement(el.getParentElement(), type, true);
    }

    public static Element getUpperElement(Element el, ElementType type)
    {
        return getUpperElement(el, type, true);
    }

    private Element getUpperElement(int offset, HGHandle type, boolean topmost)
    {
        return getUpperElement(getParagraphElement(offset), type, topmost);
    }

    private Element getUpperElement(Element el, HGHandle type, boolean topmost)
    {
        boolean found = false;
        while (el != null && type != null)
        {
            found = type.equals(getNBElementH(el))
                    && getElementType(el) != insertionPoint;
            if (found)
            {
                if (!topmost) return el;
                Element par = el.getParentElement();
                if (par != null)
                {
                    if (!type.equals(getNBElementH(par))
                            && getElementType(par) != insertionPoint)
                        return el;
                }
                else
                    return el;
            }
            el = el.getParentElement();
        }
        return getDefaultRootElement();
    }

    public Element getLowerElement(int offset, ElementType type)
    {
        return getLowerElement(getParagraphElement(offset), type);
    }

    public static Element getLowerElement(Element parent, ElementType type)
    {
        if (parent == null) return null;
        if (getElementType(parent) == type) return parent;
        for (int i = 0; i < parent.getElementCount(); i++)
        {
            Element child = parent.getElement(i);
            if (getElementType(child) == type) return child;
            Element e = getLowerElement(child, type);
            if (e != null) return e;
        }
        return null;
    }

    @Override
    public void insertString(int offset, String str, AttributeSet a)
            throws BadLocationException
    {
        if (isOutputCell(offset)) return;
        if (isInsertionPoint(offset)) insPointInsert(offset, str);
        else if (!isReadOnlyEl(getEnclosingCellElement(offset)))
        // !CellUtils
        // .isReadonly(getNBElement(getEnclosingCellElement(offset))))
        {
            Element el = getEnclosingCellElement(offset);
            CellTextChangeEvent e = new CellTextChangeEvent(getNBElementH(el),
                    EventType.INSERT, str, offset - el.getStartOffset(), str
                            .length());
            fireCellTextChanged(e);
        }
    }

    // this should be faster than searching CGM parents in HG
    public boolean isReadOnlyEl(Element el)
    {
        CellGroupMember cgm = getNBElement(el);
        if (CellUtils.isReadonly(cgm)) return true;
        Element outer = getContainerEl(el, false);
        if (outer == getRootElements()[0])
            return CellUtils.isReadonly(getBook());
        return isReadOnlyEl(outer);
    }

    // hack due to a possible bug in insert(int, ElementSpec[]), which calls
    // super.insertUpdate() thus bypassing our insertUpdate() hook,
    // in which we would normally attach our UndoableEdits.
    // The next problem is the implicit condition for accepting only
    // special types of UndoableEdits so we package our removeUndos here too
    protected void fireUndoableEditUpdate(UndoableEditEvent e)
    {
        setModified(true);
        if (supressEvents) return;
        if (!(e.getEdit() instanceof DefaultDocumentEvent))
        {
            super.fireUndoableEditUpdate(e);
            return;
        }
        DefaultDocumentEvent dde = (DefaultDocumentEvent) e.getEdit();
        if (isCompoundEditInProgress())
        {
            cEdit.addEdit(dde);
            return;
        }
        CompoundEdit cE = null;
        if (DocumentEvent.EventType.INSERT.equals(dde.getType())) cE = makeCompoundEdit(
                insertUndo, dde);
        else if (DocumentEvent.EventType.REMOVE.equals(dde.getType()))
            cE = makeCompoundEdit(removeUndo, dde);
        if (cE != null) super.fireUndoableEditUpdate(new UndoableEditEvent(e
                .getSource(), cE));
        else
            super.fireUndoableEditUpdate(e);
    }

    void beginCompoundEdit(final String name)
    {
        cEdit = new NamedCompoundEdit(name);
        editStack.push(name);
    }

    void endCompoundEdit()
    {
        editStack.pop();
        if (!editStack.isEmpty()) return;
        cEdit.end();
        super.fireUndoableEditUpdate(new UndoableEditEvent(this, cEdit));
    }

    private boolean isCompoundEditInProgress()
    {
        return editStack != null && !editStack.isEmpty();
    }

    private CompoundEdit makeCompoundEdit(Vector<UndoableEdit> edits,
            DefaultDocumentEvent dde)
    {
        if (edits == null || edits.isEmpty()) return null;
        CompoundEdit cE = new CompoundEdit();
        if (dde != null) cE.addEdit(dde);
        for (UndoableEdit ed : edits)
            if (ed != null) cE.addEdit(ed);
        cE.end();
        edits.clear();
        return cE;
    }

    @Override
    protected void removeUpdate(DefaultDocumentEvent e)
    {
        setModified(true);
        super.removeUpdate(e);
    }

    protected void insertUpdate(DefaultDocumentEvent e, AttributeSet attr)
    {
        setModified(true);
        if (attr == null) attr = contentAttrSet;
        super.insertUpdate(e, attr);
    }

    public boolean isModified()
    {
        return modified;
    }

    public void setModified(boolean mod)
    {
        if (modified != mod)
        {
            modified = mod;
            fireModificationChange(null);
        }
    }

    // public boolean equals(Object o)
    // {
    // if (!(o instanceof NotebookDocument)) return false;
    // NotebookDocument el = (NotebookDocument) o;
    // return bookH.equals(el.bookH);
    // }

    protected Element createLeafElement(Element par, AttributeSet a, int p0,
            int p1)
    {
        return new RunElement(par, a, p0, p1);
    }

    protected Element createBranchElement(Element parent, AttributeSet a)
    {
        return new BlockElement(parent, a);
    }

    public class RunElement extends LeafElement
    {
        private static final long serialVersionUID = -2929479563928351717L;

        public RunElement(Element parent, AttributeSet a, int offs0, int offs1)
        {
            super(parent, a, offs0, offs1);
        }

        public AttributeSet getResolveParent()
        {
            return null;
        }

        public String toString()
        {
            return "PEX: " + getName() + ":" + super.toString();
        }
    }

    public class BlockElement extends BranchElement
    {
        private static final long serialVersionUID = 9021560368694138280L;

        public BlockElement(Element parent, AttributeSet a)
        {
            super(parent, a);
        }

        public boolean equals(Object o)
        {
            if (!(o instanceof BlockElement)) return false;
            BlockElement el = (BlockElement) o;
            if (this.getChildCount() == 0 || el.getChildCount() == 0)
                return super.equals(o);
            if (this.getDocument().equals(el.getDocument()))
                if (this.getStartOffset() == el.getStartOffset()
                        && this.getEndOffset() == el.getEndOffset()
                        && ((getName() != null && getName()
                                .equals(el.getName())) || (getName() == null && el
                                .getName() == null))) return true;
            return false;
        }

        public String getName()
        {
            Object o = getAttribute(ATTR_CELL);
            Object o1 = getAttribute(StyleConstants.NameAttribute);
            if (o != null) return o.toString() + "/" + o1;
            return super.getName();
        }

        public AttributeSet getResolveParent()
        {
            return null;
        }

        public String toString()
        {
            return "PEX: " + getElementType(this) + ":" + super.toString();
        }
    }

    public void updateElement(Element el)
    {
        fireChangedUpdate(new DefaultDocumentEvent(el.getStartOffset(), el
                .getEndOffset(), DocumentEvent.EventType.CHANGE));
    }

    @Override
    public void remove(int offset, int len) throws BadLocationException
    {
        removeEx(offset, len);
    }

    int removeEx(int offset, int len) throws BadLocationException
    {
        return removeEx(offset, len, true);
    }

    // one necessary workaround leads to another...
    // this method is used to show the correct caret pos, when we are
    // forced to make insert/remove to preserve the element structure
    // on newline deletes and not introducing dependency on NotebookUI
    int removeEx(int offset, int len, boolean fire_event)
            throws BadLocationException
    {
        int res = -1;
        if (isInsertionPoint(offset)) return res;
        Element el = getParagraphElement(offset);
        if (isCellHandle(offset))
        {
            removeCellBoxElement(getEnclosingCellElement(offset));
            return res;
        }
        if (isOutputCell(offset)) return res;
        if (this.isReadOnlyEl(getEnclosingCellElement(offset))) return res;
        Element cell = getUpperElement(el, commonCell);
        if (cell != null && offset + len == cell.getEndOffset()) return res;
        if (el.getParentElement().getElementCount() == 1)
        {
            Element el0 = getEnclosingCellElement(offset);
            if (getNBElement(el0) instanceof Cell)
            {
                if (fire_event)
                {
                    CellTextChangeEvent e = new CellTextChangeEvent(
                            getNBElementH(el0), EventType.REMOVE, getText(
                                    offset, len),
                            offset - el0.getStartOffset(), len);
                    fireCellTextChanged(e);
                }
                else
                    super.remove(offset, len);
            }
            else
                // insPoint
                super.remove(offset, len);
            return res;
        }
        try
        {
            if (fire_event) beginCompoundEdit("");
            Element el0 = getEnclosingCellElement(offset);
            if (fire_event)
            {
                CellTextChangeEvent e = new CellTextChangeEvent(
                        getNBElementH(el0), EventType.REMOVE, getText(offset,
                                len), offset - el0.getStartOffset(), len);
                fireCellTextChanged(e);
            }
            else
                super.remove(offset, len);
            Element elem = getUpperElement(offset, paragraph);
            // newline was deleted, which results in two content elements
            // in the paragraph
            if (elem != null && elem.getElementCount() > 1)
            {
                int start = elem.getStartOffset();
                int end = elem.getEndOffset();
                res = elem.getElement(0).getEndOffset();
                String s = getText(start, end - start - 1);
                // keep last char to preserve the element structure
                supressEvents = true;
                super.remove(start, end - start - 1);
                super.insertString(start, s, contentAttrSet);
                supressEvents = false;
                fireCaretMoved(res);
                // Log.Trace("remove - replace: " + start + " length: "
                // + (end - start - 1) + ":" + s);
            }
        }
        finally
        {
            if (fire_event) endCompoundEdit();
        }
        return res;
    }

    void group(Collection<Element> elems) throws BadLocationException
    {
        for (Element el : elems)
            update(el, UpdateAction.syncronize);
        HGHandle gr_h = CellUtils.createGroupHandle();
        CellGroup gr = (CellGroup) ThisNiche.graph.get(gr_h);
        for (Element el : elems)
            gr.insert(gr.getArity(), getNBElementH(el));
        Element first_el = elems.iterator().next();
        HGHandle first_child = getNBElementH(first_el);
        CellGroup par = (CellGroup) getContainer(first_el);
        int index = par.indexOf(first_child);
        HGHandle[] removed = new HGHandle[elems.size()];
        int i = 0;
        for (Element el : elems)
        {
            removed[i] = getNBElementH(el);
            i++;
        }

        par.batchProcess(new CellGroupChangeEvent(ThisNiche.handleOf(par),
                index, new HGHandle[] { gr_h }, removed), false);
    }

    void ungroup(Element el) throws BadLocationException
    {
        ElementType type = getElementType(el);
        if (cellGroupBox != type) return;
        update(el, UpdateAction.syncronize);
        // beginCompoundEdit("Cell Ungroup");
        CellGroup group = (CellGroup) getNBElement(el);
        CellGroup parent = (CellGroup) getContainer(el);
        int ind = parent.indexOf(getNBElementH(el));
        if (ind < 0)
        {
            System.err.println("Unable to ungroup: " + CellUtils.getName(group)
                    + ". Parent: " + CellUtils.getName(parent));
            return;
        }

        HGHandle[] added = new HGHandle[group.getArity()];
        for (int i = group.getArity() - 1; i >= 0; i--)
            added[group.getArity() - 1 - i] = group.getTargetAt(i);
        CellGroupChangeEvent e = new CellGroupChangeEvent(ThisNiche
                .handleOf(parent), ind, added, new HGHandle[] { ThisNiche
                .handleOf(group) });
        fireCellGroupChanged(e);
        // endCompoundEdit();
    }

    private void remove_cell_group_member(Element nb_el)
            throws BadLocationException
    {
        CellGroupMember nb = getNBElement(nb_el);
        nb_el = (nb == null || nb instanceof Cell) ? getWholeCellElement(nb_el
                .getStartOffset()) : getUpperElement(nb_el, cellGroupBox);
        if (nb_el == null) return;
        //Element par = nb_el.getParentElement();
        // + the insPoint after the cell
        super.remove(nb_el.getStartOffset(), (nb_el.getEndOffset() - nb_el
                .getStartOffset()) + 1);
        setModified(true);
        fireCaretMoved(nb_el.getStartOffset() - 1);
        //updateElement(par);
        
    }

    public void removeCellBoxElement(Element el) throws BadLocationException
    {
        removeCellBoxElement(el, true);
    }

    public void removeCellBoxElement(Element el, boolean backup)
            throws BadLocationException
    {
        HGHandle nb = getNBElementH(el);
        Element nb_el = getEnclosingCellElement(el);
        Element gr_el = getContainerEl(nb_el, false);
        while (gr_el != getRootElements()[0])
        {
            CellGroup gr = (CellGroup) getNBElement(gr_el);
            if (gr.getArity() != 1) break;
            nb_el = gr_el;
            nb = getNBElementH(gr_el);
            gr_el = getContainerEl(gr_el, false);
        }
        CellGroup gr = (CellGroup) getNBElement(gr_el);
        if (backup) fireCellGroupChanged(new CellGroupChangeEvent(
                getNBElementH(gr_el), gr.indexOf(nb), new HGHandle[0],
                new HGHandle[] { nb }));
        else
            gr.remove(gr.indexOf(nb), false);
        updateElement(getRootElements()[0]);
    }

    public String getTitle()
    {
        if (getBook() == null) return "<NULL>?";
        String text = CellUtils.getName(getBook());
        if (text == null) text = "";
        return text;
    }

    public void setTitle(String t)
    {
        CellUtils.setName(getBook(), t);
    }

    public static ElementType getElementType(Element el)
    {
        // TODO: some "p" tag is present in doc, so until
        // its removal, we use this rather silly approach
        if (el == null) return null;
        Object obj = el.getAttributes().getAttribute(
                StyleConstants.NameAttribute);
        if (obj instanceof ElementType) return (ElementType) obj;
        return null;
    }

    public static CellGroupMember getNBElement(Element el)
    {
        if (el == null) return null;
        HGHandle h = getNBElementH(el);
        if (h == null) return null;
        return (CellGroupMember) ThisNiche.graph.get(h);
    }

    public static HGHandle getNBElementH(Element el)
    {
        if (el == null) return null;
        return (HGHandle) el.getAttributes().getAttribute(ATTR_CELL);
    }

    public NBStyle getStyle(StyleType type)
    {
        CellGroupMember book = ThisNiche.graph.get(bookH);
        return CellUtils.getStyle(book, type);
    }

    public void addStyle(NBStyle style)
    {
        CellUtils.addStyle(getBook(), style);
        Style doc_style = DocUtil.getDocStyle(this, style.getStyleType());
        DocUtil.populateDocStyle(doc_style, style);
        if (style.getStyleType() == StyleType.outputCell)
        {
            outputCellFont = getFont(doc_style);
            updateStyles();
        }
        else if (style.getStyleType() == StyleType.inputCell)
        {
            inputCellFont = getFont(doc_style);
            updateStyles();
        }
//        else if (style.getStyleType() == StyleType.global)
//        {
//            getStyle(StyleContext.DEFAULT_STYLE).addAttributes(doc_style);
//            inputCellFont = getFont(DocUtil.getDocStyle(this, StyleType.inputCell));
//            outputCellFont = getFont(DocUtil.getDocStyle(this, StyleType.outputCell));
//            updateStyles();
//        }
//        updateElement(getRootElements()[0]);
    }
    
    public Font getInputCellFont()
    {
        return inputCellFont;
    }

    public Font getOutputCellFont()
    {
        return outputCellFont;
    }

    // return the cell element which enclose the given offset
    public Element getWholeCellElement(int offset)
    {
        Element el = getUpperElement(offset, outputCellBox);
        if (el == null) el = getUpperElement(offset, inputCellBox);
        return el;
    }

    public boolean isInputCell(int offset)
    {
        return getUpperElement(offset, inputCellBox) != null;
    }

    public boolean isOutputCell(int offset)
    {
        return getUpperElement(offset, outputCellBox) != null;
    }

    public static boolean isOutputCell(Element el)
    {
        return getElementType(el) == outputCellBox;
    }

    public boolean isCellHandle(int offset)
    {
        return getUpperElement(offset, cellHandle) != null;
    }

    public boolean isInsertionPoint(int offset)
    {
        return getUpperElement(offset, insertionPoint) != null;
    }

    public Element getEnclosingCellElement(int offset)
    {
        return getEnclosingCellElement(getParagraphElement(offset));
    }

    public static Element getEnclosingCellElement(Element el)
    {
        return getContainerEl(el, true);
    }

    static Element getContainerEl(Element el, boolean check_passed_in_el)
    {
        if (el == null) return null;
        ElementType type = getElementType(el);
        if (check_passed_in_el
                && (type == outputCellBox || type == inputCellBox || type == cellGroupBox))
            return el;
        Element top = getUpperElement(el, outputCellBox, check_passed_in_el);
        if (top == null)
            top = getUpperElement(el, inputCellBox, check_passed_in_el);
        if (top == null)
            top = getUpperElement(el, cellGroupBox, check_passed_in_el);
        if (top == null) top = el.getDocument().getRootElements()[0];
        return top;
    }

    CellGroupMember getContainer(Element el)
    {
        Element e = getContainerEl(el, false);
        if (e == null) return (CellGroup) ThisNiche.graph.get(bookH);
        return getNBElement(e);
    }

    HGHandle getContainerH(Element el)
    {
        Element e = getContainerEl(el, false);
        if (e == null) return bookH;
        return getNBElementH(e);
    }

    public String getDefaultEngineName()
    {
        String name = CellUtils.getEngine(getBook());
        return name != null ? name : ThisNiche.defaultLanguage();
    }

    public void setDefaultEngineName(String name)
    {
        CellGroup book = (CellGroup) ThisNiche.graph.get(bookH);
        CellUtils.setEngine(book, name);
        update(UpdateAction.tokenize);
    }

    public void attributeChanged(AttributeChangeEvent evt)
    {
        HGHandle c = evt.getCellGroupMember();
        int offset = findElementOffset(c);
        if (offset < 0) return;
        Object key = evt.getName();
        try
        {
            // beginCompoundEdit("" + key);
            supressEvents = true;
            if (key.equals(XMLConstants.ATTR_ENGINE)) setCellEngine(
                    (String) evt.getValue(), offset);
            else if (key.equals(XMLConstants.ATTR_ERROR)) toggleErrorCell(offset);
            // DO NOTHING
            // else if(key.equals(XMLConstants.ATTR_COLLAPSED))
            // this.collapse(el);
            // else if (key.equals(XMLConstants.ATTR_INIT_CELL))
            // toggleInitCell(offset, c instanceof Cell);
            // else if(key.equals(XMLConstants.ATTR_READONLY))
            // toggleReadonlyCell(el, c instanceof Cell);
            else if (key.equals(XMLConstants.ATTR_HTML))
                toggleHTMLCell(offset);
        }
        finally
        {
            supressEvents = false;
            // endCompoundEdit();
            fireUndoableEditUpdate(new UndoableEditEvent(this, evt));
        }
    }

    public void cellGroupChanged(CellGroupChangeEvent e)
    {
        HGHandle par = e.getCellGroup();
        int offset = findElementOffset(par);
        if (offset < 0) return;
        HGHandle[] added = e.getChildrenAdded();
        HGHandle[] removed = e.getChildrenRemoved();
        int index = e.getIndex();
        try
        {
            // beginCompoundEdit("");
            supressEvents = true;
            if (removed != null && removed.length > 0)
            {
                for (int i = 0; i < removed.length; i++)
                {
                    int rem_offset = findElementOffset(removed[i]);
                    Element el = getUpperElement(rem_offset, removed[i], true);
                    if (el != null) remove_cell_group_member(el);
                }
            }
            if (added != null && added.length > 0)
            {
                for (int i = 0; i < added.length; i++)
                    insert(added[i], par, index);
            }
        }
        catch (BadLocationException ex)
        {
            ex.printStackTrace();
        }
        finally
        {
            supressEvents = false;
            // endCompoundEdit();
        }
    }

    private void setCellEngine(String engine, int offset)
    {
        Element el = getEnclosingCellElement(offset);
        if (el == null) return;
        CellGroupMember nb = getNBElement(el);
        if (!(nb instanceof Cell)) return;

        Cell cell = (Cell) nb;
        if (CellUtils.isHTML(cell)) toggleHTMLCell(offset);
        createScriptSupport(getEnclosingCellElement(offset), true);
        updateElement(el);
        if (engine == null) engine = ThisNiche.defaultLanguage();
        if (engine.equals("html")) toggleHTMLCell(offset);
    }

    private void toggleHTMLCell(int offset)
    {
        Element el = getWholeCellElement(offset);
        if (el == null) return;
        Cell c = (Cell) getNBElement(el);
        HGHandle cell = getNBElementH(el);
        HGHandle parH = getContainerH(el);
        CellGroup par = (CellGroup) ThisNiche.graph.get(parH);
        int ind = par.indexOf(cell);
        try
        {
            updateCell(el, UpdateAction.syncronize, null);
            remove_cell_group_member(el);
            if (!CellUtils.isHTML(c)) // without firing event
                c.getAttributes().put(XMLConstants.ATTR_ENGINE, "html");

            insert(cell, parH, ind);
            this.fireCaretMoved(offset);
        }
        catch (BadLocationException ex)
        {
            ex.printStackTrace();
        }
    }

    private void toggleErrorCell(int offset)
    {
        Element el = getEnclosingCellElement(offset);
        if (el == null) return;
        Cell cell = (Cell) getNBElement(el);
        el = getUpperElement(offset, commonCell);
        try
        {
            Style attr = (CellUtils.isError(cell)) ? DocUtil.getDocStyle(this,
                    StyleType.error) : DocUtil.getDocStyle(this,
                    StyleType.outputCell);
            //should re-apply this attribute...
            attr.addAttribute(StyleConstants.NameAttribute, commonCell);
            writeLock();
            ((AbstractDocument.AbstractElement) el).addAttributes(attr);
            updateElement(getUpperElement(offset, outputCellBox));
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        finally
        {
            writeUnlock();
        }
    }

    public ScriptEngineManager getScriptManager()
    {
        return scriptManager;
    }

    public SyntaxStyle[] getSyntaxStyles(ScriptSupport support)
    {
        SyntaxStyle[] styles = syntaxStyleMap.get(support.getFactory()
                .getEngineName());
        if (styles == null)
        {
            ArrayList<SyntaxStyleBean> main = SyntaxUtilities
                    .getSyntaxStyles(support);
            int n = main.size();
            styles = new SyntaxStyle[n];
            Font f = getInputCellFont();
            for (int i = 0; i < n; i++)
            {
                SyntaxStyle s = main.get(i).makeSyntaxStyle(f);
                styles[i] = s;
            }
            syntaxStyleMap.put(support.getFactory().getEngineName(), styles);
        }
        return styles;
    }

    private void fireCellTextChanged(CellTextChangeEvent e)
    {
        fireUndoableEditUpdate(new UndoableEditEvent(this, e));
        supressEvents = true;
        if (DIRECT_EVENTING) CellUtils
                .processCelTextChangeEvent(e.getCell(), e);
        else
            EventDispatcher
                    .dispatch(CellTextChangeEvent.HANDLE, getHandle(), e);
        supressEvents = false;
    }

    void fireCellGroupChanged(CellGroupChangeEvent e)
    {
        fireUndoableEditUpdate(new UndoableEditEvent(this, e));
        supressEvents = true;
        if (DIRECT_EVENTING)
        {
            CellGroup g = ThisNiche.graph.get(e.getCellGroup());
            g.batchProcess(e);
        }
        else
            EventDispatcher.dispatch(CellGroupChangeEvent.HANDLE, getHandle(),
                    e);
        supressEvents = false;
    }

    public void cellTextChanged(CellTextChangeEvent e)
    {
        int offset = findElementOffset(e.getCell());
        if (offset < 0)
        {
            Log.Trace("Cell: " + e.getCell() + " not found");
            return;
        }

        try
        {
            beginCompoundEdit("");
            supressEvents = true;
            if (e.getType() == CellTextChangeEvent.EventType.INSERT) super
                    .insertString(offset + e.getOffset(), e.getText(), null);
            else
                removeEx(offset + +e.getOffset(), e.getLength(), false);
            // super.remove(offset + +e.getOffset(), e.getLength());
        }
        catch (BadLocationException ex)
        {
            ex.printStackTrace();
        }
        finally
        {
            supressEvents = false;
            endCompoundEdit();
        }
    }

    protected void superRemove(int offs, int len) throws BadLocationException
    {
        super.remove(offs, len);
    }

    private int findElementOffset(HGHandle e)
    {
        indexes.clear();
        indexes.put(bookH, TOP_INDEX_POS);
        update(UpdateAction.index);
        if (indexes.containsKey(e))
            return (indexes.get(e) != null) ? indexes.get(e).getOffset() : -1;
        return -1;
    }

    public static class NamedCompoundEdit extends CompoundEdit
    {
        private static final long serialVersionUID = 1543257476369786620L;
        private String name;

        public NamedCompoundEdit(String n)
        {
            super();
            name = n;
        }

        public String getRedoPresentationName()
        {
            return name != null ? "Redo " + getPresentationName() : super
                    .getRedoPresentationName();
        }

        public String getUndoPresentationName()
        {
            return name != null ? "Undo " + getPresentationName() : super
                    .getUndoPresentationName();
        }

        public String getPresentationName()
        {
            return name != null ? name : super.getPresentationName();
        }
    }

    private class EditVector<T extends UndoableEdit> extends Vector<T>
    {
        private static final long serialVersionUID = 7714752524867250421L;

        public synchronized boolean add(T edit)
        {
            if (edit == null) return false;
            return isCompoundEditInProgress() ? cEdit.addEdit(edit) : super
                    .add(edit);
        }
    }

    public void addModificationListener(ModificationListener l)
    {
        listenerList.add(ModificationListener.class, l);
    }

    public void removeModificationListener(ModificationListener l)
    {
        listenerList.remove(ModificationListener.class, l);
    }

    protected void fireModificationChange(Object o)
    {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2)
            if (listeners[i] == ModificationListener.class)
                ((ModificationListener) listeners[i + 1]).documentModified(o);
    }

    public void addCaretMoveListener(CaretMoveListener l)
    {
        listenerList.add(CaretMoveListener.class, l);
    }

    public void removeCaretMoveListener(CaretMoveListener l)
    {
        listenerList.remove(CaretMoveListener.class, l);
    }

    protected void fireCaretMoved(int pos)
    {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2)
            if (listeners[i] == CaretMoveListener.class)
                ((CaretMoveListener) listeners[i + 1]).caretMoved(pos);
    }

    public static interface ModificationListener extends EventListener
    {
        void documentModified(Object o);
    }

    public static interface CaretMoveListener extends EventListener
    {
        void caretMoved(int pos);
    }

    @Override
    public String toString()
    {
        return "NBDOC: " + getTitle() + ":" + getHandle();
    }
}
