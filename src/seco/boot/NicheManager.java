package seco.boot;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComponent;

import org.hypergraphdb.HGHandle;
import org.hypergraphdb.HGHandleFactory;
import org.hypergraphdb.HGPersistentHandle;
import org.hypergraphdb.HGTypeSystem;
import org.hypergraphdb.HyperGraph;
import org.hypergraphdb.event.HGListenerAtom;
import org.hypergraphdb.event.HGOpenedEvent;
import org.hypergraphdb.indexing.ByPartIndexer;
import org.hypergraphdb.type.HGAtomType;

import seco.ThisNiche;
import seco.U;
import seco.gui.CellContainerVisual;
import seco.gui.GUIHelper;
import seco.gui.JComponentVisual;
import seco.gui.NBUIVisual;
import seco.gui.TabbedPaneVisual;
import seco.notebook.OutputCellDocument;
import seco.notebook.OutputCellDocumentType;
import seco.notebook.NotebookDocument;
import seco.notebook.NotebookDocumentType;
import seco.notebook.NotebookUI;
import seco.notebook.NotebookUIType;
import seco.notebook.ScriptletDocument;
import seco.notebook.ScriptletDocumentType;
import seco.notebook.storage.swing.SwingTypeMapper;
import seco.notebook.storage.swing.types.SwingType;
import seco.notebook.storage.swing.types.SwingTypeConstructor;
import seco.rtenv.RuntimeContext;
import seco.things.AvailableVisual;
import seco.things.Cell;
import seco.things.CellGroup;
import seco.things.CellGroupMember;
import seco.things.CellGroupType;
import seco.things.CellType;
import seco.things.DefaultVisual;
import seco.things.HGClassType;

/**
 * <p>
 * This class deals solely with the creation and initialization of new niches. All predefined, hard-coded
 * data and logic is embedded in this class.
 * </p>
 * 
 * @author Borislav Iordanov
 *
 */
public class NicheManager
{
    public static final String NICHELIST = ".scribaNiches";
    static File nichesFile = new File(new File(U.findUserHome()), NICHELIST);
    
    public static boolean firstTime = false;
    
    static Map<String, File> readNiches()
    {
        HashMap<String, File> niches = new HashMap<String, File>();
        try
        {
            niches.clear();
            if (!nichesFile.exists())
            {
            	firstTime = true;
                return niches;
            }
            FileReader reader = new FileReader(nichesFile);
            BufferedReader in = new BufferedReader(reader);
            for (String line = in.readLine(); line != null; line = in.readLine())
            {
                line = line.trim();
                if (line.length() == 0)
                    continue;
                String [] tokens = line.split(",");
                if (tokens.length != 2)
                    continue;
                File location = new File(U.unquote(tokens[1]));
                niches.put(U.unquote(tokens[0]), location);
            }
            in.close();
            reader.close();
            return niches;
        }
        catch (Throwable t)
        {
            t.printStackTrace(System.err);
            throw new RuntimeException(t);
        }
    }

    static void saveNiches(Map<String, File> niches)
    {
        try
        {
        	FileWriter out = new FileWriter(nichesFile);
            for (Map.Entry<String, File> e : niches.entrySet())
            {
                out.write(U.quote(e.getKey()));
                out.write(",");
                out.write(U.quote(e.getValue().getAbsolutePath()));
                out.write("\n");
            }
            out.close();
        }
        catch (Throwable t)
        {
            t.printStackTrace(System.err);
            throw new RuntimeException(t);
        }
    }

    /**
     * <p>Recursively delete a directory with all its contents.</p>
     * @param dir
     */
    static void deleteDirectory(File dir)
    {
        try
        {
            for (File f : dir.listFiles())
                if (f.isDirectory())
                    deleteDirectory(f);
                else
                    f.delete();
            dir.delete();
        }
        catch (Throwable t)
        {
            t.printStackTrace(System.err);            
        }
    }
    static boolean isLocationOk(File location)
    {
        if (!location.exists())
            return true;
        else if (!location.isDirectory())
            return false;
        else if (location.list().length > 0)
            return false;
        else
            return true;
    }
    
    /**
     * <p>Test whether a given location on the file system is a "niche" HyperGraphDB.</p>
     */
    static boolean isNicheLocation(File location)
    {
        if (!new File(location, "hgstore_idx_HGATOMTYPE").exists())
        	return false;
        else
        {
        	HyperGraph hg = null;
        	try
        	{
        		hg = new HyperGraph(location.getAbsolutePath());
        		return hg.get(ThisNiche.NICHE_NAME_HANDLE) != null &&
        			   hg.get(ThisNiche.TOP_CONTEXT_HANDLE) != null;
        	}
        	catch (Throwable T)
        	{
        		return false;
        	}
        	finally
        	{
        		if (hg != null)
        			try { hg.close(); } catch (Throwable t) {}        		
        	}
        }
    }
	
    public static void loadPredefinedTypes(HyperGraph graph)
    {
        HGPersistentHandle handle = HGHandleFactory
                .makeHandle("0b4503c0-dcd5-11dd-acb1-0002a5d5c51b");
        HGAtomType type = new HGClassType();
        type.setHyperGraph(graph);
        graph.getTypeSystem().addPredefinedType(handle, type, Class.class);
        graph.getIndexManager().register(new ByPartIndexer(handle, "name"));
        
        //
        // Handling of swing types.
        //
        SwingTypeMapper stm = new SwingTypeMapper();
        stm.setHyperGraph(graph);
        graph.getTypeSystem().getJavaTypeFactory().getMappers().add(0, stm);      
        HGPersistentHandle pHandle = HGHandleFactory.makeHandle("ae9e93e7-07c9-11da-831d-8d375c1471ff");
        if (graph.get(pHandle) == null)
        {
        	type = new SwingTypeConstructor();
            type.setHyperGraph(graph);
            graph.getTypeSystem().addPredefinedType(pHandle, type, SwingType.class);
        }        
    }

    public static String getNicheName(HyperGraph hg)
    {
        return (String) hg.get(ThisNiche.NICHE_NAME_HANDLE);
    }
    
    static void populateDefaultVisuals(HyperGraph graph)
    {
    	graph.define(JComponentVisual.getHandle(), new JComponentVisual());
    	graph.define(CellContainerVisual.getHandle(), new CellContainerVisual());
    	graph.define(TabbedPaneVisual.getHandle(), new TabbedPaneVisual());
    	graph.define(NBUIVisual.getHandle(), new NBUIVisual());
    	HGHandle typeHandle = graph.getTypeSystem().getTypeHandle(JComponent.class);
    	HGHandle visualHandle = JComponentVisual.getHandle();
    	graph.add(new DefaultVisual(typeHandle, visualHandle));
    	graph.add(new AvailableVisual(typeHandle, visualHandle));
    	
        typeHandle = CellGroupType.HGHANDLE;
        visualHandle = NBUIVisual.getHandle();
        graph.add(new DefaultVisual(typeHandle, visualHandle));
        graph.add(new AvailableVisual(typeHandle, visualHandle));
    }
    
    static void populateDefaultSecoUI(HyperGraph hg)
    {
        HGTypeSystem ts = hg.getTypeSystem();
        if (ts.getType(CellGroupType.HGHANDLE) == null)
        {
            HGAtomType type = new CellGroupType();
            type.setHyperGraph(hg);
            ts.addPredefinedType(CellGroupType.HGHANDLE, type, CellGroup.class);
            type = new CellType();
            type.setHyperGraph(hg);
            ts.addPredefinedType(CellType.HGHANDLE, type, Cell.class);
            type = new NotebookDocumentType();
            type.setHyperGraph(hg);
            ts.addPredefinedType(NotebookDocumentType.HGHANDLE, type,
                    NotebookDocument.class);
            type = new NotebookUIType();
            type.setHyperGraph(hg);
            ts.addPredefinedType(NotebookUIType.HGHANDLE, type,
                    NotebookUI.class);
            type = new OutputCellDocumentType();
            type.setHyperGraph(hg);
            ts.addPredefinedType(OutputCellDocumentType.HGHANDLE, type,
                    OutputCellDocument.class);
            type = new ScriptletDocumentType();
            type.setHyperGraph(hg);
            ts.addPredefinedType(ScriptletDocumentType.HGHANDLE, type,
                    ScriptletDocument.class);
        }
        if(hg.get(ThisNiche.TOP_CELL_GROUP_HANDLE) == null)
           GUIHelper.makeTopCellGroup(hg);
    }   
        
    static void populateThisNiche()
    {
    	populateDefaultVisuals(ThisNiche.hg);
    	populateDefaultSecoUI(ThisNiche.hg);        
    }
    
    static void createNiche(String name, File path)
    {
        int levelsToDeleteOnFail = 0;
        for (File existing = path; !existing.exists(); existing = existing.getParentFile())
            levelsToDeleteOnFail++;
        HyperGraph hg = null;
        try
        {
            hg = new HyperGraph(path.getAbsolutePath());
            // Scriptlet s = new Scriptlet("jscheme", "(load \"jscheme/scribaui.scm\")(install-runtime-menu)");            
          //  hg.add(new HGValueLink("on-load", new HGHandle[] {ThisNiche.TOP_CONTEXT_HANDLE, hg.add(s)}));
            HyperGraph saveHG = ThisNiche.hg; // likely, this is null, but just in case
            try
            {                            	   	
                hg.add(new HGListenerAtom(HGOpenedEvent.class.getName(), 
        				  seco.boot.NicheBootListener.class.getName()));
                hg.define(ThisNiche.NICHE_NAME_HANDLE, name);
                hg.define(ThisNiche.TOP_CONTEXT_HANDLE, new RuntimeContext("top"));
                ThisNiche.bindNiche(hg);
                populateThisNiche();            	
            }
            finally
            {
            	if (saveHG != null)
            		ThisNiche.bindNiche(saveHG);
            }
            hg.close();
        }
        catch (Throwable t)
        {
            if (hg != null) try { hg.close(); } catch (Throwable ex) { }
            for (int i = 0; i < levelsToDeleteOnFail; i++)
            {
                path.delete();
                path = path.getParentFile();
            }
            if (t instanceof RuntimeException)
                throw (RuntimeException)t;
            else
                throw new RuntimeException(t);
        }
    }	
}