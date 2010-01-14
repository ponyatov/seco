package seco.notebook.javascript;

import java.awt.Component;
import java.io.CharArrayReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JTree;
import javax.swing.event.EventListenerList;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.Segment;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import org.mozilla.javascript.Scriptable;
import org.mozilla.nb.javascript.Node;

//import org.mozilla.javascript.CompilerEnvirons;
//import org.mozilla.javascript.Context;
//import org.mozilla.javascript.ErrorReporter;
//import org.mozilla.javascript.Node;
//import org.mozilla.javascript.Parser;
//import org.mozilla.javascript.ScriptOrFnNode;

import seco.notebook.csl.ParseException;
import seco.notebook.csl.StructureItem;
import seco.notebook.javascript.JsAnalyzer.AnalysisResult;
import seco.notebook.javascript.jsr.RhinoScriptEngine;
import seco.notebook.syntax.ScriptSupport;
import seco.notebook.syntax.completion.NBParser;
import seco.notebook.util.SegmentCache;

public class JSParser0 extends NBParser
{
    boolean evaled_or_guessed = true;
    ParserRunnable parserRunnable;
    JsParser parser;
    Node astRoot;
    boolean implicitExecutionOn = false;
    // private List<Node> nodes = new LinkedList<Node>();
    // Ruby runtime;
    RhinoScriptEngine engine;

    public JSParser0(final ScriptSupport support)
    {
        super(support);
        engine = (RhinoScriptEngine) support.getDocument()
                .getEvaluationContext().getEngine("javascript");
    }

    public Node getRootNode()
    {
        return astRoot;
    }

    public Object resolveVar(String s, int offset)
    {
        if (s == null || s.length() == 0) return null;
        if (getRootNode() == null) return null;
        
        Scriptable scope = engine.getRuntimeScope(engine.getContext());
        Object o = scope.get(s, scope);
        if (o != null) return o;
       
        JsAnalyzer an = new JsAnalyzer();
        JsParseResult info = parser.getResult();
        List<? extends StructureItem> list = an.scan(info);
        AnalysisResult res = JsAnalyzer.analyze(info);
        // AstUtilities.getExpressionType(node)
        // offset = offset - support.getElement().getStartOffset()-1;
        // final AstPath path = new AstPath(astRoot, offset);
        // final Node closest = path.leaf();
        // String type = JsTypeAnalyzer.getCallFqn(info, closest, true);
        // String type1 = AstUtilities.getExpressionType(closest);

        // {
        Node n = ParserUtils.getASTNodeAtOffset(support.getElement(),
                getRootNode(), offset - 1);
        // AstPath ap = new AstPath();
        // // Node n1 = ap.findPathTo(getRootNode(), offset-1);
        // // System.out.println("RubyParser - resolveVar: " + n + ":" + n1
        // // + ":" + (offset-1) + ":" + n.equals(n1));
        // try
        // {
        JsTypeAnalyzer a = new JsTypeAnalyzer(info, JsIndex.EMPTY,
                getRootNode(), n, offset - 1, offset - 1);
        String type2 = a.getType(s);

        // System.out.println("Type: " + s + ":" + type);
        // if (type == null)
        // type = a.expressionType(n);
        // System.out.println("Type1: " + n + ":" + type);
        // if (type == null)
        // return null;
        // evaled_or_guessed = false;
        // if(type.indexOf('.') > 0)
        // {
        // Class[] info = ClassRepository.getInstance()
        // .findClass(type);
        // // System.out.println("Info: " + info + ":" + info.length);
        // return (info.length > 0) ? info[0] : null;
        // }else{
        // return evalType(type);
        // }
        // }
        // catch (Exception ex)
        // {
        // ex.printStackTrace();
        // }
        // }
        return null;
    }

    // Object evalType(String name)
    // {
    // System.out.println("evalType: " + name + ":" + astRoot);
    // if(astRoot == null) return null;
    // StaticScope scope = astRoot.getStaticScope();
    // DynamicScope dyn = astRoot.getScope();
    // if(dyn == null)
    // dyn = new ManyVarsDynamicScope(scope, null);
    // Node node = new ConstNode(new IDESourcePosition(), name);
    // ThreadContext ctx = runtime.getCurrentContext();
    // ctx.preEvalScriptlet(dyn);
    // try{
    // return engine.evalNode(node, new SimpleScriptContext());
    // }catch(ScriptException ex){
    // ex.printStackTrace();
    // return null;
    // }
    // }

    private JsParser getParser()
    {
        if (parser == null)
        {
            // CompilerEnvirons compilerEnv = new CompilerEnvirons();
            // compilerEnv.initFromContext(Context.enter());
            // ErrorReporter compilationErrorReporter = null; // TODO:
            // if (compilationErrorReporter == null)
            // {
            // compilationErrorReporter = compilerEnv.getErrorReporter();
            // }

            // parser = new Parser(compilerEnv, compilationErrorReporter);
            parser = new JsParser();
        }
        return parser;
    }

    private Reader makeReader(Segment seg, int offset, int length)
    {
        CharArrayReader r = (seg.array.length > length) ? new CharArrayReader(
                seg.array, offset, length) : new CharArrayReader(seg.array);
        return r;
    }

    public NBParser.ParserRunnable getParserRunnable()
    {
        if (parserRunnable != null) return parserRunnable;
        parserRunnable = new NBParser.ParserRunnable() {
            public boolean doJob()
            {
                Segment seg = SegmentCache.getSharedInstance().getSegment();
                try
                {
                    Element el = support.getElement();
                    int offset = el.getStartOffset();
                    int length = el.getEndOffset() - el.getStartOffset();
                    support.getDocument().getText(offset, length, seg);
                    // Reader r = makeReader(seg, offset, length);
                    parser = getParser();
                    //System.out.println("parser.start()..." + length);
                  //  long start = System.currentTimeMillis();
                    // astRoot = parser.parse(r, "<Unknown Source>", 1);
                    parser.parse(seg.toString());
                    astRoot = parser.getResult().getRootNode();
                   // System.out.println("Time: "
                   //         + (System.currentTimeMillis() - start));
                   // System.out.println("Root: " + astRoot);

                    support.unMarkErrors();
                    return true;
                }
                catch (ParseException ex)
                {
                    ScriptSupport.ErrorMark mark = new ScriptSupport.ErrorMark(
                            stripMsg(ex.getMessage()), -1, // ex.getLineNumber()
                                                           // - 1,
                            -1// ex.getColumnNumber()
                    );
                    support.markError(mark);
                    // System.err.println(ex.getClass() + ":" +
                    // ex.getMessage());
                    return false;
                }
                catch (Throwable e)
                {
                    // e.printStackTrace();
                    // System.err.println(e.getClass() + ":" + e.getMessage());
                    return false;
                }
                finally
                {
                    SegmentCache.getSharedInstance().releaseSegment(seg);
                }
            }
        };
        return parserRunnable;
    }

    // TODO: following 2 methods could be further fine tuned
    private static String stripMsg(String s)
    {
        if (s == null) return s;
        s = s.substring(s.indexOf(":") + 1);
        s = s.substring(s.indexOf(":") + 1);
        // strip the rest after 2nd ":"
        int i = s.indexOf(":");
        i = s.indexOf(":", i + 1);
        return s.substring(0, i);
    }

    public JTree getAstTree()
    {
        if (astRoot == null) return null;
        JTree astTree = new JTree();
        JSTreeModel treeModel = new JSTreeModel(astRoot);
        astTree.setModel(treeModel);
        // astTree.setCellRenderer(new JSNodeRenderer());

        return astTree;
    }

    class JSTreeModel implements TreeModel
    {
        Node root = null;
        protected EventListenerList listenerList = new EventListenerList();

        public JSTreeModel(Node t)
        {
            if (t == null) { throw new IllegalArgumentException("root is null"); }
            root = t;
        }

        public Object getChild(Object parent, int index)
        {
            if (parent == null) { return null; }
            Node p = (Node) parent;
            if (!p.hasChildren()) return null;
            Node c = (Node) p.getFirstChild();
            if (index == 0) return c;
            int i = 0;
            while (c != null && i <= index)
            {
                c = (Node) c.getNext();
                i++;
                if (c != null && i == index) return c;
            }
            // return c;
            // if (index >= c.size())
            throw new ArrayIndexOutOfBoundsException("node " + p
                    + " has no child with index: " + index);
            // return c.get(index);
        }

        public int getChildCount(Object parent)
        {
            if (parent == null) { throw new IllegalArgumentException(
                    "root is null"); }
            if (parent instanceof String) return 0;
            Node p = (Node) parent;
            if (!p.hasChildren()) return 0;
            Node n = p.getFirstChild();
            int count = 1;
            if (n == p.getLastChild()) return count;
            while (n != null)
            {
                n = n.getNext();
                if (n != null) count++;
            }
            return count;

        }

        public int getIndexOfChild(Object parent, Object child)
        {
            if (parent == null || child == null) { throw new IllegalArgumentException(
                    "root or child is null"); }
            int i = 0;
            Node p = (Node) parent;
            Node c = (Node) p.getFirstChild();
            while (c != null && c != child)
            {
                c = (Node) c.getNext();
                i++;
            }
            if (c == child) { return i; }
            throw new java.util.NoSuchElementException("node is not a child");
        }

        public Object getRoot()
        {
            return root;
        }

        public boolean isLeaf(Object node)
        {
            if (node == null) { throw new IllegalArgumentException(
                    "node is null"); }
            if (node instanceof String) return true;
            else
            {
                Node t = (Node) node;
                return !t.hasChildren();
            }
        }

        /**
         * Adds a listener for the TreeModelEvent posted after the tree changes.
         * 
         * @see #removeTreeModelListener
         * @param l
         *            the listener to add
         */
        public void addTreeModelListener(TreeModelListener l)
        {
            listenerList.add(TreeModelListener.class, l);
        }

        /**
         * Removes a listener previously added with
         * <B>addTreeModelListener()</B>.
         * 
         * @see #addTreeModelListener
         * @param l
         *            the listener to remove
         */
        public void removeTreeModelListener(TreeModelListener l)
        {
            listenerList.remove(TreeModelListener.class, l);
        }

        /**
         * Returns an array of all the tree model listeners registered on this
         * model.
         * 
         * @return all of this model's <code>TreeModelListener</code>s or an
         *         empty array if no tree model listeners are currently
         *         registered
         * 
         * @see #addTreeModelListener
         * @see #removeTreeModelListener
         * 
         * @since 1.4
         */
        public TreeModelListener[] getTreeModelListeners()
        {
            return (TreeModelListener[]) listenerList
                    .getListeners(TreeModelListener.class);
        }

        public void valueForPathChanged(TreePath path, Object newValue)
        {
            // System.out.println("\nvalueForPathChanged ... \n");
            fireTreeStructureChanged(path.getLastPathComponent(), path);
        }

        /*
         * ==================================================================
         * 
         * Borrowed from javax.swing.tree.DefaultTreeModel
         * 
         * ==================================================================
         */
        /*
         * Notifies all listeners that have registered interest for notification
         * on this event type. The event instance is lazily created using the
         * parameters passed into the fire method.
         * 
         * @param source the node where the tree model has changed @param path
         * the path to the root node
         * 
         * @see EventListenerList
         */
        private void fireTreeStructureChanged(Object source, TreePath path)
        {
            // Guaranteed to return a non-null array
            Object[] listeners = listenerList.getListenerList();
            TreeModelEvent e = null;
            // Process the listeners last to first, notifying
            // those that are interested in this event
            for (int i = listeners.length - 2; i >= 0; i -= 2)
            {
                if (listeners[i] == TreeModelListener.class)
                {
                    // Lazily create the event:
                    if (e == null) e = new TreeModelEvent(source, path);
                    ((TreeModelListener) listeners[i + 1])
                            .treeStructureChanged(e);
                }
            }
        }

        /**
         * @param nodeBefore
         * @return
         */
        public TreePath getTreePath(Node node)
        {
            Node[] nodes = getTreePathNodes((Node) getRoot(), node, 0);
            if (nodes == null) return null;
            else
                return new TreePath(nodes);
        }

        public Node[] getTreePathNodes(Node root, Node node, int depth)
        {
            if (node == null) return null;
            depth++;
            Node[] retNodes = null;
            if (node == root)
            {
                retNodes = new Node[depth];
                retNodes[depth - 1] = root;
            }
            else
            {
                int n = getChildCount(root); // .getNumberOfChildren();
                loop: for (int i = 0; i < n; i++)
                {
                    retNodes = getTreePathNodes((Node) getChild(root, i), node,
                            depth);
                    if (retNodes != null)
                    {
                        retNodes[depth - 1] = root;
                        break loop;
                    }
                }
            }
            return retNodes;
        }
    }
}