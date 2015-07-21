package edu.ucsf.rbvi.stringApp.internal.utils;

import java.awt.Color;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.model.View;
import org.cytoscape.view.model.VisualLexicon;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.presentation.RenderingEngineManager;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.presentation.property.NodeShapeVisualProperty;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualPropertyDependency;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.view.vizmap.VisualStyleFactory;
import org.cytoscape.view.vizmap.mappings.BoundaryRangeValues;
import org.cytoscape.view.vizmap.mappings.ContinuousMapping;
import org.cytoscape.view.vizmap.mappings.PassthroughMapping;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;

public class ViewUtils {
	public static String STYLE_NAME = "String Style";
	public static CyNetworkView styleNetwork(StringManager manager, CyNetwork network) {
		// First, let's get a network view
		CyNetworkView netView = manager.createNetworkView(network);
		VisualStyle stringStyle = createStyle(manager);

		// Set the node colors around the color wheel
		float h = 0.0f;
		float stepSize = 1.0f/(float)network.getNodeCount();
		for (View<CyNode> nv: netView.getNodeViews()) {
			Color c = Color.getHSBColor(h, 1.0f, 1.0f);
			h += stepSize;
			nv.setLockedValue(BasicVisualLexicon.NODE_FILL_COLOR, c);
		}

		VisualMappingManager vmm = manager.getService(VisualMappingManager.class);
		vmm.setVisualStyle(stringStyle, netView);
		vmm.setCurrentVisualStyle(stringStyle);
		manager.getService(CyNetworkViewManager.class).addNetworkView(netView);
		
		return netView;
	}

	public static VisualStyle createStyle(StringManager manager) {
		VisualMappingManager vmm = manager.getService(VisualMappingManager.class);
		for (VisualStyle style: vmm.getAllVisualStyles()) {
			if (style.getTitle().equals(STYLE_NAME)) {
				return style;
			}
		}

		VisualStyleFactory vsf = manager.getService(VisualStyleFactory.class);

		VisualStyle stringStyle = vsf.createVisualStyle(vmm.getCurrentVisualStyle());
		stringStyle.setTitle(STYLE_NAME);

		// Set the shape to an ellipse
		stringStyle.setDefaultValue(BasicVisualLexicon.NODE_SHAPE, NodeShapeVisualProperty.ELLIPSE);

		// Lock node width and height
		for(VisualPropertyDependency<?> vpd: stringStyle.getAllVisualPropertyDependencies()) {
			if (vpd.getIdString().equals("nodeSizeLocked"))
				vpd.setDependency(true);
		}

		// Set up the passthrough mapping for the glass style
		VisualMappingFunctionFactory passthroughFactory = 
		                 manager.getService(VisualMappingFunctionFactory.class, "(mapping.type=passthrough)");
		VisualLexicon lex = manager.getService(RenderingEngineManager.class).getDefaultVisualLexicon();
		VisualProperty customGraphics = lex.lookup(CyNode.class, "NODE_CUSTOMGRAPHICS_1");
		PassthroughMapping pMapping = 
			(PassthroughMapping) passthroughFactory.createVisualMappingFunction("String Style", String.class, customGraphics);
		stringStyle.addVisualMappingFunction(pMapping);

		// Finally, set the edge width to be dependent on the total score
		VisualMappingFunctionFactory continuousFactory = 
		                 manager.getService(VisualMappingFunctionFactory.class, "(mapping.type=continuous)");
		{
			ContinuousMapping<Double,Double> cMapping = 
				(ContinuousMapping) continuousFactory.createVisualMappingFunction("score", Double.class, 
											                                                  BasicVisualLexicon.EDGE_WIDTH);
			cMapping.addPoint(0.5, new BoundaryRangeValues<Double>(0.5,1.0,1.0));
			cMapping.addPoint(0.8, new BoundaryRangeValues<Double>(2.0,2.0,2.0));
			cMapping.addPoint(1.0, new BoundaryRangeValues<Double>(5.0,5.0,5.0));
			stringStyle.addVisualMappingFunction(cMapping);
		}

		{
			ContinuousMapping<Double,Integer> cMapping = 
				(ContinuousMapping) continuousFactory.createVisualMappingFunction("score", Double.class, 
											   	                                               BasicVisualLexicon.EDGE_TRANSPARENCY);
			cMapping.addPoint(0.5, new BoundaryRangeValues<Integer>(50,50,50));
			cMapping.addPoint(0.8, new BoundaryRangeValues<Integer>(100,100,100));
			cMapping.addPoint(1.0, new BoundaryRangeValues<Integer>(255,255,255));
			stringStyle.addVisualMappingFunction(cMapping);
		}

		vmm.addVisualStyle(stringStyle);
		return stringStyle;
	}

}
