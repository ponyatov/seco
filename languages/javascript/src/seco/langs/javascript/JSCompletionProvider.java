package seco.langs.javascript;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.script.ScriptContext;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JToolTip;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.Position;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.IdScriptableObject;
import org.mozilla.javascript.NativeFunction;
import org.mozilla.javascript.NativeJavaPackage;
import org.mozilla.javascript.ScriptableObject;

import seco.notebook.NotebookDocument;
import seco.langs.javascript.jsr.ExternalScriptable;
import seco.notebook.syntax.ScriptSupport;
import seco.notebook.syntax.completion.AsyncCompletionQuery;
import seco.notebook.syntax.completion.AsyncCompletionTask;
import seco.notebook.syntax.completion.BaseAsyncCompletionQuery;
import seco.notebook.syntax.completion.Completion;
import seco.notebook.syntax.completion.CompletionDocumentation;
import seco.notebook.syntax.completion.CompletionProvider;
import seco.notebook.syntax.completion.CompletionResultSet;
import seco.notebook.syntax.completion.CompletionTask;
import seco.notebook.syntax.completion.CompletionU;
import seco.notebook.syntax.completion.JavaDocManager;
import seco.notebook.syntax.completion.CompletionU.DBPackageInfo;
import seco.notebook.syntax.java.JavaPaintComponent;
import seco.notebook.syntax.java.JavaResultItem;
import seco.notebook.syntax.java.JavaPaintComponent.MethodPaintComponent;
import seco.notebook.syntax.util.JMIUtils;
import seco.storage.ClassRepository;
import seco.storage.NamedInfo;
import seco.storage.PackageInfo;
import seco.util.DocumentUtilities;

public class JSCompletionProvider implements CompletionProvider
{
    public int getAutoQueryTypes(JTextComponent component, String typedText)
    {
        if (".".equals(typedText)) return COMPLETION_QUERY_TYPE;
        if (" ".equals(typedText)) return TOOLTIP_QUERY_TYPE;
        return 0;
    }

    public CompletionTask createTask(int queryType, JTextComponent component)
    {
        int offset = component.getCaret().getDot();
        ScriptSupport sup = ((NotebookDocument) component.getDocument())
                .getScriptSupport(offset);
        if (sup.isCommentOrLiteral(offset - 1)) return null;
        if (queryType == COMPLETION_QUERY_TYPE)
            return new AsyncCompletionTask(new Query(component.getCaret()
                    .getDot()), component);
        // else if (queryType == DOCUMENTATION_QUERY_TYPE) return new
        // AsyncCompletionTask(
        // new DocQuery(null), component);
        // else if (queryType == TOOLTIP_QUERY_TYPE)
        // return new AsyncCompletionTask(new ToolTipQuery(), component);
        return null;
    }

    static final class Query extends BaseAsyncCompletionQuery
    {
        public Query(int caretOffset)
        {
            super(caretOffset);
        }

        protected void query(CompletionResultSet resultSet,
                NotebookDocument doc, int offset)
        {
            ScriptSupport sup = doc.getScriptSupport(offset);
            queryCaretOffset = offset;
            queryAnchorOffset = offset;
            JavaScriptParser p = (JavaScriptParser) sup.getParser();
            String s = sup.getCommandBeforePt(offset);
            // TODO: should consider inner scopes, etc.
            if ("this".equals(s))
            {
                populateThis(resultSet, p);
                queryResult = resultSet;
                resultSet.finish();
                return;
            }

            Object obj = p.resolveVar(s, offset);
            if (obj == null)
            {
                resultSet.finish();
                return;
            }
            // Class<?> cls = obj.getClass();
            if (obj instanceof ScriptableObject)
            {
                String name = ((ScriptableObject) obj).getClassName();
                if (BuiltIns.isBuiltInType(name)
                        && !BuiltIns.OBJECT.equals(name)) populateBuiltInObject(
                        resultSet, name);
                else if (obj instanceof IdScriptableObject) populateNativeObject(
                        resultSet, (IdScriptableObject) obj);
                else if (obj instanceof NativeJavaPackage)
                {
                    NamedInfo[] info = null;
                    if (s.startsWith("Packages"))
                        s = (s.length() == 8) ? "" : s.substring(9);
                    if (s.length() == 0)
                    {
                        Set<PackageInfo> set = ClassRepository.getInstance()
                                .getTopPackages();
                        info = set.toArray(new NamedInfo[set.size()]);
                    }
                    else
                    {
                        info = ClassRepository.getInstance().findSubElements(s);
                    }

                    if (info.length > 0)
                        CompletionU.populatePackage(resultSet,
                                new CompletionU.DBPackageInfo(info, s),
                                queryCaretOffset);
                }
            }else if(obj instanceof JavaScriptParser.JsType)
            {
                String type = ((JavaScriptParser.JsType) obj).getType();
                if(type != null && BuiltIns.isBuiltInType(type))
                    populateBuiltInObject(resultSet, type);
                else       //TODO: find better way instead of fallback object
                   populateBuiltInObject(resultSet, BuiltIns.OBJECT);
            }
            else if (Object.class == obj) 
                populateBuiltInObject(resultSet, BuiltIns.OBJECT);
            else if (tryPopulatingJsEquivalent(obj, resultSet)) ;
            else
            {
                Class<?> cls = (Class<?>) obj.getClass();
                CompletionU.populateClass(resultSet, cls, Modifier.PUBLIC,
                        queryCaretOffset);
            }
            queryResult = resultSet;
            resultSet.finish();
        }

        private boolean tryPopulatingJsEquivalent(Object o, CompletionResultSet resultSet)
        {
            String name = null;
            if (o instanceof String) name = BuiltIns.STRING;
            else if (o instanceof Number) name = BuiltIns.NUM;
            else if (o instanceof Date) name = BuiltIns.DATE;
            else if (o instanceof Boolean) name = BuiltIns.BOOL;
            // else if(o.getClass().isArray())
            // name = BUILDINS.ARRAY;
            if (name != null) populateBuiltInObject(resultSet, name);
            return name != null;
        }

        private void populateNativeObject(CompletionResultSet resultSet,
                IdScriptableObject obj)
        {
            for (JavaResultItem item : BuiltIns.getParams(BuiltIns.OBJECT))
            {
                item.setSubstituteOffset(queryCaretOffset);
                resultSet.addItem(item);
            }
            Object[] ids = obj.getAllIds();
            if (ids != null) for (Object id : ids)
            {
                JSProperty item = new JSProperty("" + id, "Object");
                item.setSubstituteOffset(queryCaretOffset);
                resultSet.addItem(item);
            }

            resultSet.setTitle(obj.getClassName());
        }

        private void populateBuiltInObject(CompletionResultSet resultSet,
                String class_name)
        {
            List<JavaResultItem> params = BuiltIns.getParams(class_name);
            if (params != null)
            {
                for (JavaResultItem item : params)
                {
                    item.setSubstituteOffset(queryCaretOffset);
                    resultSet.addItem(item);
                }
                resultSet.setTitle(class_name);
            }
        }

        private void populateThis(CompletionResultSet resultSet,
                JavaScriptParser p)
        {
            // TODO: add all vars from the RuntimeContext.
            List<JavaResultItem> params = BuiltIns.getThisParams();

            for (JavaResultItem item : params)
            {
                item.setSubstituteOffset(queryCaretOffset);
                resultSet.addItem(item);
            }

            ScriptContext ctx = p.engine.getContext();
            ExternalScriptable scope = (ExternalScriptable) p.engine
                    .getRuntimeScope(ctx);
            Context.enter();
            for (Object k : scope.getIds())
            {
                String key = "" + k;
                Object o = scope.get(key, scope.getPrototype());
                JavaResultItem item = (o instanceof NativeFunction) ?
                    BuiltIns.make_func(key, (NativeFunction) o):
                    new JSProperty(key, JMIUtils.getTypeName(
                            o.getClass(), false, false));
                item.setSubstituteOffset(queryCaretOffset);
                resultSet.addItem(item);   
            }
            Context.exit();
            resultSet.setTitle("Global");
        }

    }

   
    static class ToolTipQuery extends AsyncCompletionQuery
    {
        private JTextComponent component;
        private int queryCaretOffset;
        private int queryAnchorOffset;
        private JToolTip queryToolTip;
        /**
         * Method/constructor '(' position for tracking whether the method is
         * still being completed.
         */
        private Position queryMethodParamsStartPos = null;
        private boolean otherMethodContext;

        protected void query(CompletionResultSet resultSet,
                NotebookDocument doc, int caretOffset)
        {
            queryMethodParamsStartPos = null;
            ScriptSupport sup = doc.getScriptSupport(caretOffset);
            if (sup == null || !(sup.getParser() instanceof JavaScriptParser))
                return;
            JavaScriptParser p = (JavaScriptParser) sup.getParser();
            // TODO:
            if (p.getRootNode() == null)
            {
                resultSet.finish();
                return;
            }
        }

        protected void prepareQuery(JTextComponent component)
        {
            this.component = component;
        }

        protected boolean canFilter(JTextComponent component)
        {
            CharSequence text = null;
            int textLength = -1;
            int caretOffset = component.getCaretPosition();
            Document doc = component.getDocument();
            try
            {
                if (caretOffset - queryCaretOffset > 0) text = DocumentUtilities
                        .getText(doc, queryCaretOffset, caretOffset
                                - queryCaretOffset);
                else if (caretOffset - queryCaretOffset < 0) text = DocumentUtilities
                        .getText(doc, caretOffset, queryCaretOffset
                                - caretOffset);
                else
                    textLength = 0;
            }
            catch (BadLocationException e)
            {
            }
            if (text != null)
            {
                textLength = text.length();
            }
            else if (textLength < 0) { return false; }
            boolean filter = true;
            int balance = 0;
            for (int i = 0; i < textLength; i++)
            {
                char ch = text.charAt(i);
                switch (ch)
                {
                case ',':
                    filter = false;
                    break;
                case '(':
                    balance++;
                    filter = false;
                    break;
                case ')':
                    balance--;
                    filter = false;
                    break;
                }
                if (balance < 0) otherMethodContext = true;
            }
            if (otherMethodContext && balance < 0) otherMethodContext = false;
            if (queryMethodParamsStartPos == null
                    || caretOffset <= queryMethodParamsStartPos.getOffset())
                filter = false;
            return otherMethodContext || filter;
        }

        protected void filter(CompletionResultSet resultSet)
        {
            if (!otherMethodContext)
            {
                resultSet.setAnchorOffset(queryAnchorOffset);
                resultSet.setToolTip(queryToolTip);
            }
            resultSet.finish();
        }
    }

    static class JSVarArgMethod extends JavaResultItem.MethodItem
    {
        public JSVarArgMethod(String mtdName, String type, int modifiers)
        {
            super(mtdName, type);
            this.modifiers = modifiers;
        }

        public JSVarArgMethod(String mtdName, String type, String[] types,
                String[] names, int modifiers)
        {
            super(mtdName, type);
            this.modifiers = modifiers;
            populateParams0(types, names);
        }

        protected boolean isAddParams()
        {
            return true;
        }

        void populateParams0(String[] prms, String names[])
        {
            for (int i = 0; i < prms.length; i++)
                params.add(new ParamStr(prms[i], prms[i], names[i], true,
                        getTypeColor(prms[i])));
        }
    }
    static class JSProperty extends JavaResultItem.FieldResultItem
    {

        public JSProperty(String name, String type)
        {
            super(name, type, Modifier.PUBLIC);
        }

        public JSProperty(String name, String type, int modifiers)
        {
            super(name, type, modifiers);
        }
    }

}
