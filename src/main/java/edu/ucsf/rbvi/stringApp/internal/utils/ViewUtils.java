package edu.ucsf.rbvi.stringApp.internal.utils;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyEdge;
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
import org.cytoscape.view.presentation.property.values.NodeShape;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualPropertyDependency;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.view.vizmap.VisualStyleFactory;
import org.cytoscape.view.vizmap.mappings.BoundaryRangeValues;
import org.cytoscape.view.vizmap.mappings.ContinuousMapping;
import org.cytoscape.view.vizmap.mappings.DiscreteMapping;
import org.cytoscape.view.vizmap.mappings.PassthroughMapping;

import edu.ucsf.rbvi.stringApp.internal.model.Species;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;

public class ViewUtils {
	public static String STYLE_NAME = "STRING style";
	public static String STYLE_NAME_ORG = "Organism STRING style";
	public static String STYLE_ORG = "Organism ";

	public static CyNetworkView styleNetwork(StringManager manager, CyNetwork network,
	                                         CyNetworkView netView) {
		boolean useStitch = false;
		if (network.getDefaultNodeTable().getColumn(ModelUtils.TYPE) != null)
			useStitch = true;
		VisualStyle stringStyle = createStyle(manager, network, useStitch);

		// if (useHost)
		//	updateColorMapHost(manager, stringStyle, netView);
		// else
		updateColorMap(manager, stringStyle, network);
		updateEnhancedLabels(manager, stringStyle, network, manager.showEnhancedLabels());
		
		VisualMappingManager vmm = manager.getService(VisualMappingManager.class);
		vmm.setCurrentVisualStyle(stringStyle);

		if (netView != null) {
			vmm.setVisualStyle(stringStyle, netView);
			manager.getService(CyNetworkViewManager.class).addNetworkView(netView);
			manager.getService(CyApplicationManager.class).setCurrentNetworkView(netView);
		}
		
		return netView;
	}

	public static void reapplyStyle(StringManager manager, CyNetworkView view) {
		VisualMappingManager vmm = manager.getService(VisualMappingManager.class);
		VisualStyle style = vmm.getVisualStyle(view);
		style.apply(view);
	}

	public static void updateNodeStyle(StringManager manager, 
	                                   CyNetworkView view, List<CyNode> nodes) {
		// manager.flushEvents();
		VisualMappingManager vmm = manager.getService(VisualMappingManager.class);
		VisualStyle style = vmm.getVisualStyle(view);
		for (CyNode node: nodes) {
			style.apply(view.getModel().getRow(node), view.getNodeView(node));
		}
		// style.apply(view);
	}

	public static void updateEdgeStyle(StringManager manager, CyNetworkView view, List<CyEdge> edges) {
		// manager.flushEvents();
		VisualMappingManager vmm = manager.getService(VisualMappingManager.class);
		VisualStyle style = vmm.getVisualStyle(view);
		for (CyEdge edge: edges) {
			style.apply(view.getModel().getRow(edge), view.getEdgeView(edge));
		}
		// style.apply(view);
	}

	public static VisualStyle createStyle(StringManager manager, CyNetwork network, boolean useStitch) {
		String styleName = getStyleName(manager, network);

		VisualMappingManager vmm = manager.getService(VisualMappingManager.class);
		for (VisualStyle style: vmm.getAllVisualStyles()) {
			if (style.getTitle().equals(styleName)) {
				return style;
			}
		}

		VisualStyleFactory vsf = manager.getService(VisualStyleFactory.class);

		VisualStyle stringStyle = vsf.createVisualStyle(vmm.getCurrentVisualStyle());
		stringStyle.setTitle(styleName);

		// Set the default node size
		stringStyle.setDefaultValue(BasicVisualLexicon.NODE_WIDTH, 45.0);
		stringStyle.setDefaultValue(BasicVisualLexicon.NODE_HEIGHT, 45.0);

		// Set the shape to an ellipse
		stringStyle.setDefaultValue(BasicVisualLexicon.NODE_SHAPE, NodeShapeVisualProperty.ELLIPSE);

		// And set the color to white
		stringStyle.setDefaultValue(BasicVisualLexicon.NODE_FILL_COLOR, Color.WHITE);

		// And set the edge color to blue
		stringStyle.setDefaultValue(BasicVisualLexicon.EDGE_STROKE_UNSELECTED_PAINT, new Color(31,41,61));

		// And set the label color to black
		stringStyle.setDefaultValue(BasicVisualLexicon.NODE_LABEL_COLOR, Color.BLACK);

		// And set the node border width to zero
		stringStyle.setDefaultValue(BasicVisualLexicon.NODE_BORDER_WIDTH, 0.0);

		// And set the label color to black
		stringStyle.setDefaultValue(BasicVisualLexicon.NODE_LABEL_FONT_SIZE, 10);

		// Lock node width and height
		for(VisualPropertyDependency<?> vpd: stringStyle.getAllVisualPropertyDependencies()) {
			if (vpd.getIdString().equals("nodeSizeLocked"))
				vpd.setDependency(false);
		}

		// Get all of the factories we'll need
		VisualMappingFunctionFactory continuousFactory = 
		                 manager.getService(VisualMappingFunctionFactory.class, "(mapping.type=continuous)");
		VisualMappingFunctionFactory discreteFactory = 
	                 manager.getService(VisualMappingFunctionFactory.class, "(mapping.type=discrete)");
		VisualMappingFunctionFactory passthroughFactory = 
		                 manager.getService(VisualMappingFunctionFactory.class, "(mapping.type=passthrough)");
		VisualLexicon lex = manager.getService(RenderingEngineManager.class).getDefaultVisualLexicon();

		// Set up the passthrough mapping for the glass style
		{
			VisualProperty customGraphics = lex.lookup(CyNode.class, "NODE_CUSTOMGRAPHICS_1");
			PassthroughMapping pMapping = 
				(PassthroughMapping) passthroughFactory.createVisualMappingFunction(ModelUtils.STYLE, 
				                                                                    String.class, customGraphics);
			stringStyle.addVisualMappingFunction(pMapping);
		}

		// Set the edge width to be dependent on the total score
		{
			ContinuousMapping<Double,Double> cMapping = 
				(ContinuousMapping) continuousFactory.createVisualMappingFunction(ModelUtils.SCORE, Double.class, 
				                                                                  BasicVisualLexicon.EDGE_WIDTH);
			cMapping.addPoint(0.2, new BoundaryRangeValues<Double>(0.8,0.8,0.8));
			cMapping.addPoint(0.5, new BoundaryRangeValues<Double>(2.0,2.0,2.0));
			cMapping.addPoint(1.0, new BoundaryRangeValues<Double>(4.0,4.0,4.0));
			stringStyle.addVisualMappingFunction(cMapping);
		}

		{
			ContinuousMapping<Double,Integer> cMapping = 
				(ContinuousMapping) continuousFactory.createVisualMappingFunction(ModelUtils.SCORE, Double.class, 
				                                                                  BasicVisualLexicon.EDGE_TRANSPARENCY);
			cMapping.addPoint(0.2, new BoundaryRangeValues<Integer>(34,34,34));
			cMapping.addPoint(0.5, new BoundaryRangeValues<Integer>(85,85,85));
			cMapping.addPoint(1.0, new BoundaryRangeValues<Integer>(170,170,170));
			stringStyle.addVisualMappingFunction(cMapping);
		}


		// If we have enhancedGrahpics loaded, automatically use it
		// if (manager.haveEnhancedGraphics() && manager.showEnhancedLabels()) {
		// // Set up the passthrough mapping for the label
		// {
		// VisualProperty customGraphics = lex.lookup(CyNode.class, "NODE_CUSTOMGRAPHICS_3");
		// PassthroughMapping pMapping =
		// (PassthroughMapping)
		// passthroughFactory.createVisualMappingFunction(ModelUtils.ELABEL_STYLE,
		// String.class, customGraphics);
		// stringStyle.addVisualMappingFunction(pMapping);
		// }
		//
		// // Set up our labels to be in the upper right quadrant
		// {
		// VisualProperty customGraphicsP = lex.lookup(CyNode.class,
		// "NODE_CUSTOMGRAPHICS_POSITION_3");
		// Object upperRight = customGraphicsP.parseSerializableString("NE,C,c,0.00,0.00");
		// stringStyle.setDefaultValue(customGraphicsP, upperRight);
		// if (useStitch) {
		// Object top = customGraphicsP.parseSerializableString("N,C,c,0.00,-8.00");
		// DiscreteMapping<String,Object> dMapping =
		// (DiscreteMapping) discreteFactory.createVisualMappingFunction(ModelUtils.TYPE,
		// String.class,
		// customGraphicsP);
		// dMapping.putMapValue("compound", top);
		// dMapping.putMapValue("protein", upperRight);
		// stringStyle.addVisualMappingFunction(dMapping);
		// }
		// }
		//
		// // Finally, disable the "standard" label passthrough
		// {
		// stringStyle.removeVisualMappingFunction(BasicVisualLexicon.NODE_LABEL);
		// }
		// }

		// Set up all of our special mappings if we have a stitch network
		if (useStitch) {

			// Increase our font size to 12pt
			stringStyle.setDefaultValue(BasicVisualLexicon.NODE_LABEL_FONT_SIZE, 12);

			// Set the node to be transparent if it's a compound.  We
			// need to do this because Cytoscape doesn't have a "pill" shape
			{
				DiscreteMapping<String,Integer> dMapping = 
					(DiscreteMapping) discreteFactory.createVisualMappingFunction(ModelUtils.TYPE, String.class, 
												   	                                            BasicVisualLexicon.NODE_TRANSPARENCY);
				dMapping.putMapValue("compound", 0);
				dMapping.putMapValue("protein", 255);
				stringStyle.addVisualMappingFunction(dMapping);
			}

			// Set the appropriate width
			{
				DiscreteMapping<String,Double> dMapping = 
					(DiscreteMapping) discreteFactory.createVisualMappingFunction(ModelUtils.TYPE, String.class, 
											   	                                            BasicVisualLexicon.NODE_WIDTH);
				dMapping.putMapValue("compound", 100.0);
				dMapping.putMapValue("protein", 50.0);
				stringStyle.addVisualMappingFunction(dMapping);
			}

			// Set the appropriate height
			{
				DiscreteMapping<String,Double> dMapping = 
					(DiscreteMapping) discreteFactory.createVisualMappingFunction(ModelUtils.TYPE, String.class, 
											   	                                            BasicVisualLexicon.NODE_HEIGHT);
				dMapping.putMapValue("compound", 40.0);
				dMapping.putMapValue("protein", 50.0);
				stringStyle.addVisualMappingFunction(dMapping);
			}

			// Set the appropriate shape
			{
				DiscreteMapping<String,NodeShape> dMapping = 
					(DiscreteMapping) discreteFactory.createVisualMappingFunction(ModelUtils.TYPE, String.class, 
											   	                                            BasicVisualLexicon.NODE_SHAPE);
				dMapping.putMapValue("compound", NodeShapeVisualProperty.ROUND_RECTANGLE);
				dMapping.putMapValue("protein", NodeShapeVisualProperty.ELLIPSE);
				stringStyle.addVisualMappingFunction(dMapping);
			}

			// TODO: Set the label position
			// We need to export ObjectPosition in the API in order to be able to do this, unfortunately
			// if (!manager.haveEnhancedGraphics() || !manager.showEnhancedLabels()) {
			// VisualProperty labelPosition = lex.lookup(CyNode.class, "NODE_LABEL_POSITION");
			// DiscreteMapping<String,Object> dMapping =
			// (DiscreteMapping) discreteFactory.createVisualMappingFunction(ModelUtils.TYPE,
			// String.class,
			// labelPosition);
			// Object top = labelPosition.parseSerializableString("N,S,c,0.00,0.00");
			// Object upperRight = labelPosition.parseSerializableString("NE,S,c,0.00,0.00");
			// dMapping.putMapValue("compound", top);
			// dMapping.putMapValue("protein", upperRight);
			// stringStyle.addVisualMappingFunction(dMapping);
			// }

			// Set up a passthrough for chemViz
			{
				VisualProperty customGraphics = lex.lookup(CyNode.class, "NODE_CUSTOMGRAPHICS_2");
				PassthroughMapping pMapping = 
					(PassthroughMapping) passthroughFactory.createVisualMappingFunction(ModelUtils.CV_STYLE, 
					                                                                    String.class, customGraphics);
				stringStyle.addVisualMappingFunction(pMapping);
			}

			// Now, set colors for edges based on the edge type
			{
				DiscreteMapping<String,Color> dMapping = 
					(DiscreteMapping) discreteFactory.createVisualMappingFunction(CyEdge.INTERACTION, String.class, 
											   	                                    BasicVisualLexicon.EDGE_STROKE_UNSELECTED_PAINT);
				dMapping.putMapValue("pp", new Color(31,41,61));
				dMapping.putMapValue("cc", new Color(255,0,0));
				dMapping.putMapValue("pc", new Color(0,128,0));
				stringStyle.addVisualMappingFunction(dMapping);
			}
		}

		vmm.addVisualStyle(stringStyle);
		return stringStyle;
	}

	public static void updateEnhancedLabels(StringManager manager, VisualStyle stringStyle, 
	                                        CyNetwork net, boolean show) {

		boolean useStitch = false;
		if (net.getDefaultNodeTable().getColumn(ModelUtils.TYPE) != null)
			useStitch = true;

		VisualMappingFunctionFactory discreteFactory = 
            manager.getService(VisualMappingFunctionFactory.class, "(mapping.type=discrete)");
		VisualMappingFunctionFactory passthroughFactory = 
            manager.getService(VisualMappingFunctionFactory.class, "(mapping.type=passthrough)");
		VisualLexicon lex = manager.getService(RenderingEngineManager.class).getDefaultVisualLexicon();
		// Set up the passthrough mapping for the label
		if (show && manager.haveEnhancedGraphics()) {
			{
				VisualProperty customGraphics = lex.lookup(CyNode.class, "NODE_CUSTOMGRAPHICS_3");
				PassthroughMapping pMapping = 
					(PassthroughMapping) passthroughFactory.createVisualMappingFunction(ModelUtils.ELABEL_STYLE, 
					                                                                    String.class, customGraphics);
				stringStyle.addVisualMappingFunction(pMapping);
			}
	
			// Set up our labels to be in the upper right quadrant
			{
				VisualProperty customGraphicsP = lex.lookup(CyNode.class, "NODE_CUSTOMGRAPHICS_POSITION_3");
				Object upperRight = customGraphicsP.parseSerializableString("NE,C,c,0.00,0.00");
				stringStyle.setDefaultValue(customGraphicsP, upperRight);
				if (useStitch) {
					Object top = customGraphicsP.parseSerializableString("N,C,c,0.00,-5.00");
					DiscreteMapping<String,Object> dMapping = 
						(DiscreteMapping) discreteFactory.createVisualMappingFunction(ModelUtils.TYPE, String.class, 
											   	                                              customGraphicsP);
					dMapping.putMapValue("compound", top);
					dMapping.putMapValue("protein", upperRight);
					stringStyle.addVisualMappingFunction(dMapping);
				}
			}
	
			// Finally, disable the "standard" label passthrough and position
			{
				stringStyle.removeVisualMappingFunction(BasicVisualLexicon.NODE_LABEL);
				// stringStyle.removeVisualMappingFunction(lex.lookup(CyNode.class, "NODE_LABEL_POSITION"));
			}
		} else {
			stringStyle
					.removeVisualMappingFunction(lex.lookup(CyNode.class, "NODE_CUSTOMGRAPHICS_3"));
			stringStyle.removeVisualMappingFunction(
					lex.lookup(CyNode.class, "NODE_CUSTOMGRAPHICS_POSITION_3"));

			{
				PassthroughMapping pMapping = (PassthroughMapping) passthroughFactory
						.createVisualMappingFunction(ModelUtils.DISPLAY, String.class,
								BasicVisualLexicon.NODE_LABEL);
				stringStyle.addVisualMappingFunction(pMapping);
			}
			
			// {
			// VisualProperty labelPosition = lex.lookup(CyNode.class, "NODE_LABEL_POSITION");
			// DiscreteMapping<String,Object> dMapping =
			// (DiscreteMapping) discreteFactory.createVisualMappingFunction(ModelUtils.TYPE,
			// String.class,
			// labelPosition);
			// Object top = labelPosition.parseSerializableString("N,S,c,0.00,0.00");
			// Object upperRight = labelPosition.parseSerializableString("NE,S,c,0.00,0.00");
			// dMapping.putMapValue("compound", top);
			// dMapping.putMapValue("protein", upperRight);
			// stringStyle.addVisualMappingFunction(dMapping);
			// }
		}
	}

	private static void updateColorMap(StringManager manager, VisualStyle style, CyNetwork network) {
		// Build the color list
		VisualMappingFunctionFactory discreteFactory = 
		                 manager.getService(VisualMappingFunctionFactory.class, "(mapping.type=discrete)");
		{
			DiscreteMapping<String,Color> dMapping = 
				(DiscreteMapping) discreteFactory.createVisualMappingFunction("Name", String.class, 
				                                                              BasicVisualLexicon.NODE_FILL_COLOR);

			// Set the node colors around the color wheel
			float h = 0.0f;
			float s = 1.0f;
			float stepSize = 1.0f/(float)network.getNodeCount();
			for (CyNode node: network.getNodeList()) {
				Color c = Color.getHSBColor(h, s, 1.0f);
				h += stepSize;
				if (s == 1.0f)
					s = 0.5f;
				else
					s = 1.0f;
				String name = network.getRow(node).get(CyNetwork.NAME, String.class);
				dMapping.putMapValue(name, c);
			}
			style.addVisualMappingFunction(dMapping);
		}
	}

	private static void updateColorMapHost(StringManager manager, VisualStyle style, CyNetwork net) {
		VisualMappingFunctionFactory discreteFactory = manager
				.getService(VisualMappingFunctionFactory.class, "(mapping.type=discrete)");

		// get previous mapping
		DiscreteMapping<String, Color> dMapping = (DiscreteMapping) style
				.getVisualMappingFunction(BasicVisualLexicon.NODE_FILL_COLOR);
		List<String> species = ModelUtils.getAllNetSpecies(net);
		// get network species
		Map<String, Color> mapValues = new HashMap<String, Color>();

		// save previous color mapping
		if (dMapping != null) {
			Map<String, Color> mappedValues = dMapping.getAll();
			for (String spKey : mappedValues.keySet()) {
				if (species.contains(spKey)) {
					mapValues.put(spKey, mappedValues.get(spKey));
				}
			}
		}
		// make the new mapping after removing the old one
		style.removeVisualMappingFunction(BasicVisualLexicon.NODE_FILL_COLOR);
		dMapping = (DiscreteMapping) discreteFactory.createVisualMappingFunction(
				ModelUtils.SPECIES, String.class, BasicVisualLexicon.NODE_FILL_COLOR);
		// Set the species colors
		for (String sp : species) {
			if (!mapValues.containsKey(sp)) {
				dMapping.putMapValue(sp, Color.decode(Species.getSpeciesColor(sp)));
			} else {
				dMapping.putMapValue(sp, mapValues.get(sp));
			}
		}

		// DiscreteMapping<String,Color> dMapping =
		// (DiscreteMapping) discreteFactory.createVisualMappingFunction("Name", String.class,
		// BasicVisualLexicon.NODE_FILL_COLOR);
		//
		// // Set the node colors around the color wheel
		// for (View<CyNode> nv: view.getNodeViews()) {
		// Color c =
		// Color.decode(view.getModel().getRow(nv.getModel()).get(ModelUtils.SPECIES_COLOR,
		// String.class));
		// String name = view.getModel().getRow(nv.getModel()).get(CyNetwork.NAME,
		// String.class);
		// dMapping.putMapValue(name, c);
		// }
		style.addVisualMappingFunction(dMapping);
	}
	
	public static void updateNodeColorsHost(StringManager manager, 
	                                        CyNetwork net, CyNetworkView view) {
		// manager.flushEvents();
		VisualMappingManager vmm = manager.getService(VisualMappingManager.class);
		VisualMappingFunctionFactory discreteFactory = manager
				.getService(VisualMappingFunctionFactory.class, "(mapping.type=discrete)");

		VisualStyle style = null;
	 	if (view != null)
			style	= vmm.getVisualStyle(view);
		else {
			String styleName = getStyleName(manager, net);
			for (VisualStyle s: vmm.getAllVisualStyles()) {
				if (s.getTitle().equals(styleName)) {
					style = s;
					break;
				}
			}
		}

		// Worst case -- can't find a style, so er just bail
		if (style == null) return;

		if (!style.getTitle().startsWith(STYLE_NAME_ORG)) {
			VisualStyleFactory vsf = manager.getService(VisualStyleFactory.class);

			VisualStyle stringStyle = vsf.createVisualStyle(vmm.getCurrentVisualStyle());
			stringStyle.setTitle(STYLE_ORG + style.getTitle());
			vmm.addVisualStyle(stringStyle);
			style = stringStyle;
		}
		updateColorMapHost(manager, style, net);
		if (view != null)
			vmm.setVisualStyle(style, view);
		vmm.setCurrentVisualStyle(style);
	}

	private static String getStyleName(StringManager manager, CyNetwork network) {
		String networkName = manager.getNetworkName(network);
		String styleName = STYLE_NAME;
		if (networkName.startsWith("String Network")) {
			String[] parts = networkName.split("_");
			if (parts.length == 1) {
				String[] parts2 = networkName.split(" - ");
				if (parts2.length == 2)
					styleName = styleName +" - "+parts2[1];
			} else if (parts.length == 2)
				styleName = styleName + "_"+parts[1];
		}
		return styleName;
	}
}
