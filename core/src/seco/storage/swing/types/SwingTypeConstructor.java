package seco.storage.swing.types;

import org.hypergraphdb.HGException;
import org.hypergraphdb.HGHandle;
import org.hypergraphdb.HGHandleFactory;
import org.hypergraphdb.HGPersistentHandle;
import org.hypergraphdb.IncidenceSetRef;
import org.hypergraphdb.LazyRef;
import org.hypergraphdb.type.HGAtomTypeBase;

public class SwingTypeConstructor extends HGAtomTypeBase {
	public SwingTypeConstructor() {

	}

	public Object make(HGPersistentHandle handle,
			LazyRef<HGHandle[]> targetSet, IncidenceSetRef incidenceSet) 
	{
		HGPersistentHandle[] layout = graph.getStore().getLink(handle);
		Class<?> cls = null;
		try{
		//	System.out.println("Class.forName: " + graph.get(layout[0]));
		  cls = graph.getTypeSystem().loadClass(((String)graph.get(layout[0])));
		}catch(HGException ex)
		{
		    //Trying with the context class loader then give up
		    ClassLoader loader = Thread.currentThread().getContextClassLoader();
	        if (loader == null)
	            loader = getClass().getClassLoader();
	        try{
	            loader.loadClass((String)graph.get(layout[0]));
	        }catch(ClassNotFoundException ex1){
		       throw new HGException(ex1.toString());
		    }
		}
		SwingType result = new SwingType(cls);
		
		result.setHyperGraph(graph);
		result.setCtrHandle(layout[1]);
		result.setAddOnsHandle(layout[2]);
		for (int i = 3; i < layout.length; i++) {
			result.addSlot(layout[i]);
		}
		return result;
	}

	public HGPersistentHandle store(Object instance) 
	{
		SwingType recordType = (SwingType) instance;
		HGPersistentHandle[] layout = new HGPersistentHandle[recordType
				.slotCount() + 3];
		layout[0] = graph.getPersistentHandle(graph.add(recordType.getJavaClass().getName()));
		layout[1] = recordType.getCtrHandle() == null ?
		        graph.getHandleFactory().nullHandle() : graph.getPersistentHandle(recordType.getCtrHandle());
		layout[2] = recordType.getAddOnsHandle() == null ? 
		        graph.getHandleFactory().nullHandle() : graph.getPersistentHandle(recordType.getAddOnsHandle());
		
		for (int i = 0; i < recordType.slotCount(); i++) {
			layout[i+3] = graph.getPersistentHandle(recordType.getAt(i));
		}
		return graph.getStore().store(layout);
	}

	public void release(HGPersistentHandle handle) {
		graph.getStore().removeLink(handle);
	}

	
}
