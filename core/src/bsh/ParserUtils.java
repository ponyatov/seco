/*
 * This file is part of the Scriba source distribution. This is free, open-source 
 * software. For full licensing information, please see the LicensingInformation file
 * at the root level of the distribution.
 *
 * Copyright (c) 2006-2007 Kobrix Software, Inc.
 */
package bsh;

import javax.swing.text.Element;

public class ParserUtils
{
	static SimpleNode getParentOfType(SimpleNode n, Class<?> clazz)
	{
		if(n == null) return null;
		if(n.getClass().equals(clazz))	return n;
		SimpleNode par = (SimpleNode) n.parent;
		while(par != null)
		{
			if(par.getClass().equals(clazz))
				return par;
			par = (SimpleNode) par.parent;
		}
		return null;
	}
	
	static SimpleNode getASTNodeAtOffset(Element el, SimpleNode root, int offset)
	{
		int line = el.getElementIndex(offset);
		if(line < 0) return null;
		Element inner = el.getElement(line);
		int line_offset = offset - inner.getStartOffset();
        //parser line starts from 1
		line++;
		return getASTNode(root, line, line_offset);
	}
	
	static SimpleNode getASTNode(SimpleNode root, int line, int line_offset)
	{
		int begLine = 0;
		if(root.children == null || root.children.length == 0) return null;
	       
		for(int i = 0; i< root.children.length; i++)
		{
			begLine = ((SimpleNode)root.children[i]).firstToken.beginLine;
			if(begLine == line)
				return getInnerNode(((SimpleNode)root.children[i]), line_offset);
			if(begLine > line)
				return (i==0) ? null : getASTNode(((SimpleNode)root.children[i-1]), line,line_offset);
	    }
		 //check the last node from the loop
		SimpleNode last = ((SimpleNode)root.children[root.children.length-1]);
		if(last.lastToken.endLine > line)
		   return getASTNode(last, line, line_offset);
		return null;
	}
	
	private static SimpleNode getInnerNode(SimpleNode root, int pos) {
		if(root.children == null || root.children.length == 0)
			return root;
		for (Node e : root.children) 
		{
			int index = getElementIndex((SimpleNode) e, pos);
			if(index != -1)
			{
		        SimpleNode in = getInnerNode((SimpleNode) ((SimpleNode)e).children[index], pos);
		        return (in != null) ? in : (SimpleNode) e;
		     }
		}
		return root;
	}
	
	static int getElementIndex(SimpleNode n, int offset)
	{
		if (n == null || n.children == null ||
				n.children.length == 0 || offset > getEndOffset(n))
			return -1;
		
		for(int i = 0; i < n.children.length; i++)
		{
			SimpleNode e = (SimpleNode) n.children[i];
			if(getEndOffset(e) >= offset && getStartOffset(e) <= offset)
				return i;
		}
		return -1;
	}
	
	private static int getStartOffset(SimpleNode n)
	{
		return n.firstToken.beginColumn;
	}
	
	private static int getEndOffset(SimpleNode n)
	{
		return n.lastToken.endColumn;
	}
	
    private ParserUtils()
    {
    }
	
	
}
