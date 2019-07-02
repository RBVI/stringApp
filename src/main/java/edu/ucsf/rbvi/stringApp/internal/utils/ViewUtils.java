package edu.ucsf.rbvi.stringApp.internal.utils;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.model.View;
import org.cytoscape.view.model.VisualLexicon;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.presentation.RenderingEngineManager;
import org.cytoscape.view.presentation.customgraphics.CyCustomGraphics;
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

import edu.ucsf.rbvi.stringApp.internal.model.EnrichmentTerm;
import edu.ucsf.rbvi.stringApp.internal.model.Species;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.model.ChartType;

public class ViewUtils {
	public static String STYLE_NAME = "STRING style v1.5";
	public static String STYLE_NAME_ORG = "Organism STRING style v1.5";
	public static String STYLE_ORG = "Organism ";

	// Our chart strings
	static String PIE_CHART = "piechart: attributelist=\"enrichmentTermsIntegers\" showlabels=\"false\" colorlist=\"";
	static String CIRCOS_CHART = "circoschart: firstarc=1.0 arcwidth=0.4 attributelist=\"enrichmentTermsIntegers\" showlabels=\"false\" colorlist=\"";
	static String CIRCOS_CHART2 = "circoschart: borderwidth=0 firstarc=1.0 arcwidth=0.4 attributelist=\"enrichmentTermsIntegers\" showlabels=\"false\" colorlist=\"";

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
		updateGlassBallEffect(manager, stringStyle, network, manager.showGlassBallEffect());
		
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
			if (view.getNodeView(node) != null)
				style.apply(view.getModel().getRow(node), view.getNodeView(node));
		}
		// style.apply(view);
	}

	public static void updateEdgeStyle(StringManager manager, CyNetworkView view, List<CyEdge> edges) {
		// manager.flushEvents();
		VisualMappingManager vmm = manager.getService(VisualMappingManager.class);
		VisualStyle style = vmm.getVisualStyle(view);
		for (CyEdge edge: edges) {
			if (view.getEdgeView(edge) != null)
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
		stringStyle.setDefaultValue(BasicVisualLexicon.NODE_FILL_COLOR, Color.LIGHT_GRAY);

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

	public static void updateGlassBallEffect(StringManager manager, VisualStyle stringStyle, 
            CyNetwork net, boolean show) {

		VisualMappingFunctionFactory passthroughFactory = manager
				.getService(VisualMappingFunctionFactory.class, "(mapping.type=passthrough)");
		VisualLexicon lex = manager.getService(RenderingEngineManager.class)
				.getDefaultVisualLexicon();
		
		// Set up the passthrough mapping for the glass ball effect
		if (show) {
			{
				VisualProperty customGraphics = lex.lookup(CyNode.class, "NODE_CUSTOMGRAPHICS_1");
				PassthroughMapping pMapping = 
					(PassthroughMapping) passthroughFactory.createVisualMappingFunction(ModelUtils.STYLE, 
					                                                                    String.class, customGraphics);
				stringStyle.addVisualMappingFunction(pMapping);
			}

		} else {
			stringStyle
					.removeVisualMappingFunction(lex.lookup(CyNode.class, "NODE_CUSTOMGRAPHICS_1"));
			stringStyle.removeVisualMappingFunction(
					lex.lookup(CyNode.class, "NODE_CUSTOMGRAPHICS_POSITION_1"));
		}
	}

	private static void updateColorMap(StringManager manager, VisualStyle style, CyNetwork network) {
		// Build the color list
		VisualMappingFunctionFactory discreteFactory = 
		                 manager.getService(VisualMappingFunctionFactory.class, "(mapping.type=discrete)");
		{
			DiscreteMapping<String,Color> dMapping = 
				(DiscreteMapping) discreteFactory.createVisualMappingFunction(CyNetwork.NAME, String.class, 
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
	
	public static void updateNodeColors(StringManager manager, 
	                                    CyNetwork net, CyNetworkView view, boolean host) {
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

		if (host) {
			updateColorMapHost(manager, style, net);
		} else {
			updateColorMap(manager, style, net);
		}
		if (view != null)
			vmm.setVisualStyle(style, view);
		vmm.setCurrentVisualStyle(style);
	}

	public static void updatePieCharts(StringManager manager, VisualStyle stringStyle,
			CyNetwork net, boolean show) {

		VisualMappingFunctionFactory passthroughFactory = manager
				.getService(VisualMappingFunctionFactory.class, "(mapping.type=passthrough)");
		VisualLexicon lex = manager.getService(RenderingEngineManager.class)
				.getDefaultVisualLexicon();
		// Set up the passthrough mapping for the label
		if (show) {
			{
				VisualProperty customGraphics = lex.lookup(CyNode.class, "NODE_CUSTOMGRAPHICS_4");
				PassthroughMapping pMapping = (PassthroughMapping) passthroughFactory
						.createVisualMappingFunction(EnrichmentTerm.colEnrichmentPassthrough, String.class,
								customGraphics);
				stringStyle.addVisualMappingFunction(pMapping);
			}
		} else {
			stringStyle
					.removeVisualMappingFunction(lex.lookup(CyNode.class, "NODE_CUSTOMGRAPHICS_4"));

			// Restore the glass ball, if appropriate
			if (manager.showGlassBallEffect()) {
				CyNetworkView netView = manager.getCurrentNetworkView();
				VisualMappingManager vmm = manager.getService(VisualMappingManager.class);
				ViewUtils.updateGlassBallEffect(manager, vmm.getVisualStyle(netView), net, true);
			}
		}
	}
	
	public static void drawCharts(StringManager manager, 
	                              Map<EnrichmentTerm, String> selectedTerms, 
																ChartType type) {
		CyNetwork network = manager.getCurrentNetwork();
		if (network == null || selectedTerms.size() == 0)
			return;

		CyTable nodeTable = network.getDefaultNodeTable();
		createColumns(nodeTable);

		List<String> colorList = getColorList(selectedTerms);
		List<String> shownTermNames = getTermNames(network, nodeTable, selectedTerms);

		for (CyNode node: network.getNodeList()) {
			List<Integer> nodeTermsIntegers = 
							nodeTable.getRow(node.getSUID()).getList(EnrichmentTerm.colEnrichmentTermsIntegers,Integer.class);
			String nodeColor = nodeColors(colorList, nodeTermsIntegers, type);
			nodeTable.getRow(node.getSUID()).set(EnrichmentTerm.colEnrichmentPassthrough, nodeColor);
			nodeTable.getRow(node.getSUID()).set(EnrichmentTerm.colEnrichmentTermsIntegers,nodeTermsIntegers);
		}

		// System.out.println(selectedTerms);
		VisualMappingManager vmm = manager.getService(VisualMappingManager.class);
		CyNetworkView netView = manager.getCurrentNetworkView();
		if (netView != null) {
			ViewUtils.updatePieCharts(manager, vmm.getVisualStyle(netView), network, true);

			// Don't override the user if they have specifically disabled the glass ball effect
			if (manager.showGlassBallEffect()) {
				if (ChartType.PIE.equals(type) || ChartType.SPLIT_PIE.equals(type)) {
					ViewUtils.updateGlassBallEffect(manager, vmm.getVisualStyle(netView), network, false);
					// manager.setShowGlassBallEffect(false);
				} else {
					ViewUtils.updateGlassBallEffect(manager, vmm.getVisualStyle(netView), network, true);
					// manager.setShowGlassBallEffect(true);
				}
				// manager.getShowGlassBallEffectTaskFactory().reregister();
			}
			netView.updateView();
		}		
		// save in network table
		CyTable netTable = network.getDefaultNetworkTable();
		ModelUtils.createListColumnIfNeeded(netTable, String.class, ModelUtils.NET_ENRICHMENT_VISTEMRS);
		netTable.getRow(network.getSUID()).set(ModelUtils.NET_ENRICHMENT_VISTEMRS, shownTermNames);
		
		ModelUtils.createListColumnIfNeeded(netTable, String.class, ModelUtils.NET_ENRICHMENT_VISCOLORS);
		netTable.getRow(network.getSUID()).set(ModelUtils.NET_ENRICHMENT_VISCOLORS, colorList);
	}

	private static void createColumns(CyTable nodeTable) {
		// replace columns
		ModelUtils.replaceListColumnIfNeeded(nodeTable, String.class,
				EnrichmentTerm.colEnrichmentTermsNames);
		ModelUtils.replaceListColumnIfNeeded(nodeTable, Integer.class,
				EnrichmentTerm.colEnrichmentTermsIntegers);
		ModelUtils.replaceColumnIfNeeded(nodeTable, String.class,
				EnrichmentTerm.colEnrichmentPassthrough);
	}

	public static void highlight(StringManager manager, CyNetworkView view, CyNode node) {
		View<CyNode> nodeView = view.getNodeView(node);
		CyNetwork net = view.getModel();

		List<CyEdge> edges = net.getAdjacentEdgeList(node, CyEdge.Type.ANY);
		List<CyNode> nodes = net.getNeighborList(node, CyEdge.Type.ANY);

		VisualLexicon lex = manager.getService(RenderingEngineManager.class).getDefaultVisualLexicon();
		VisualProperty customGraphics1 = lex.lookup(CyNode.class, "NODE_CUSTOMGRAPHICS_1");
		VisualProperty customGraphics2 = lex.lookup(CyNode.class, "NODE_CUSTOMGRAPHICS_2");
		VisualProperty customGraphics3 = lex.lookup(CyNode.class, "NODE_CUSTOMGRAPHICS_3");

		CyCustomGraphics cg = new EmptyCustomGraphics();

		// Override our current style through overrides
		for (View<CyNode> nv: view.getNodeViews()) {
			if (nv.getModel().equals(node) || nodes.contains(nv.getModel())) {
				nv.setLockedValue(BasicVisualLexicon.NODE_TRANSPARENCY, 255);
			} else {
				nv.setLockedValue(customGraphics1, cg);
				nv.setLockedValue(customGraphics2, cg);
				nv.setLockedValue(customGraphics3, cg);
				nv.setLockedValue(BasicVisualLexicon.NODE_TRANSPARENCY, 20);
			}
		}
		for (View<CyEdge> ev: view.getEdgeViews()) {
			if (edges.contains(ev.getModel())) {
				ev.setLockedValue(BasicVisualLexicon.EDGE_TRANSPARENCY, 255);
			} else {
				ev.setLockedValue(BasicVisualLexicon.EDGE_TRANSPARENCY, 20);
			}
		}
	}

	public static void clearHighlight(StringManager manager, CyNetworkView view, CyNode node) {
		if (node == null) return;
		View<CyNode> nodeView = view.getNodeView(node);

		VisualLexicon lex = manager.getService(RenderingEngineManager.class).getDefaultVisualLexicon();
		VisualProperty customGraphics1 = lex.lookup(CyNode.class, "NODE_CUSTOMGRAPHICS_1");
		VisualProperty customGraphics2 = lex.lookup(CyNode.class, "NODE_CUSTOMGRAPHICS_2");
		VisualProperty customGraphics3 = lex.lookup(CyNode.class, "NODE_CUSTOMGRAPHICS_3");

		for (View<CyNode> nv: view.getNodeViews()) {
			nv.clearValueLock(customGraphics1);
			nv.clearValueLock(customGraphics2);
			nv.clearValueLock(customGraphics3);
			nv.clearValueLock(BasicVisualLexicon.NODE_TRANSPARENCY);
		}

		for (View<CyEdge> ev: view.getEdgeViews()) {
			ev.clearValueLock(BasicVisualLexicon.EDGE_TRANSPARENCY);
		}
	}

	public static void hideStringColors(StringManager manager, CyNetworkView view, boolean show) {
		VisualMappingManager vmm = manager.getService(VisualMappingManager.class);
		VisualStyle style = null;
	 	if (view != null)
			style	= vmm.getVisualStyle(view);
		else {
			String styleName = getStyleName(manager, view.getModel());
			for (VisualStyle s: vmm.getAllVisualStyles()) {
				if (s.getTitle().equals(styleName)) {
					style = s;
					break;
				}
			}
		}

		if (style == null) return;

		if (show) {
			updateColorMap(manager, style, view.getModel());
		} else {
			style.removeVisualMappingFunction(BasicVisualLexicon.NODE_FILL_COLOR);
		}
	}

	public static void hideSingletons(CyNetworkView view, boolean show) {
		CyNetwork net = view.getModel();
		for (View<CyNode> nv: view.getNodeViews()) {
			CyNode node = nv.getModel();
			List<CyEdge> edges = net.getAdjacentEdgeList(node, CyEdge.Type.ANY);
			if (edges != null && edges.size() > 0) continue;
			if (!show)
				nv.setLockedValue(BasicVisualLexicon.NODE_VISIBLE, false);
			else
				nv.clearValueLock(BasicVisualLexicon.NODE_VISIBLE);
		}
	}

	private static List<String> getColorList(Map<EnrichmentTerm, String> selectedTerms) {
		List<String> colorList = new ArrayList<String>();
		for (EnrichmentTerm term : selectedTerms.keySet()) {
			// Color color = selectedTerms.get(term);
			String color = selectedTerms.get(term);
			if (color != null) {
				//String hex = String.format("#%02x%02x%02x", color.getRed(), color.getGreen(),
				//		color.getBlue());
				//colorList += hex + ",";
				colorList.add(color);
			} else {
				colorList.add("");
			}
		}
		return colorList;
	}
	
	private static List<String> getTermNames(CyNetwork network, CyTable nodeTable,
	                                         Map<EnrichmentTerm, String> selectedTerms) { 
		List<String> shownTermNames = new ArrayList<>();
		boolean firstTerm = true;
		for (EnrichmentTerm term : selectedTerms.keySet()) {
			String selTerm = term.getName();
			shownTermNames.add(selTerm);
			List<Long> enrichedNodeSUIDs = term.getNodesSUID();
			for (CyNode node : network.getNodeList()) {
				List<Integer> nodeTermsIntegers = nodeTable.getRow(node.getSUID())
						.getList(EnrichmentTerm.colEnrichmentTermsIntegers, Integer.class);
				List<String> nodeTermsNames = nodeTable.getRow(node.getSUID())
						.getList(EnrichmentTerm.colEnrichmentTermsNames, String.class);
				if (firstTerm || nodeTermsIntegers == null)
					nodeTermsIntegers = new ArrayList<Integer>();
				if (firstTerm || nodeTermsNames == null) {
					nodeTermsNames = new ArrayList<String>();
				}
				if (enrichedNodeSUIDs.contains(node.getSUID())) {
					nodeTermsNames.add(selTerm);
					nodeTermsIntegers.add(new Integer(1));
				} else {
					nodeTermsNames.add("");
					nodeTermsIntegers.add(new Integer(0));
				}
				nodeTable.getRow(node.getSUID()).set(EnrichmentTerm.colEnrichmentTermsIntegers, nodeTermsIntegers);
				nodeTable.getRow(node.getSUID()).set(EnrichmentTerm.colEnrichmentTermsNames, nodeTermsNames);
			}
			if (firstTerm) firstTerm = false;
		}
		return shownTermNames;
	}

	private static String nodeColors(List<String> colors, List<Integer> nodeTermFlags, ChartType type) {
		boolean foundTerm = false;
		for (Integer term: nodeTermFlags) {
			if (term > 0) {
				foundTerm = true;
				break;
			}
		}
		if (!foundTerm) return null;

		String colorString = "";
		if (type.equals(ChartType.FULL)|| type.equals(ChartType.PIE)) {
			for (String color: colors) {
				colorString += color+",";
			}
		} else {
			for (int i = 0; i < colors.size(); i++) {
				if (nodeTermFlags.get(i) > 0) {
					if (type.equals(ChartType.TEETH))
						colorString += colors.get(i)+"ff,";
					else
						colorString += colors.get(i)+",";
				} else {
					if (type.equals(ChartType.TEETH))
						colorString += "#ffffff00,";
					else
						colorString += "#ffffff,";
					nodeTermFlags.set(i, new Integer(1));
				}
			}
			if (!foundTerm) return null;
		}
		if (type.equals(ChartType.PIE) || type.equals(ChartType.SPLIT_PIE))
			return PIE_CHART+colorString.substring(0, colorString.length()-1)+"\"";
		if (type.equals(ChartType.TEETH))
			return CIRCOS_CHART2+colorString.substring(0, colorString.length()-1)+"\"";
		return CIRCOS_CHART+colorString.substring(0, colorString.length()-1)+"\"";
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
