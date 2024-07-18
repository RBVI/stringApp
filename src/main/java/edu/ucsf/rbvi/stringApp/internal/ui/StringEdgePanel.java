package edu.ucsf.rbvi.stringApp.internal.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.Border;

import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;

import edu.ucsf.rbvi.stringApp.internal.model.EvidenceType;
import edu.ucsf.rbvi.stringApp.internal.model.NetworkType;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.tasks.ChangeNetTypeTaskFactory;

import edu.ucsf.rbvi.stringApp.internal.utils.ColumnNames;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;
import edu.ucsf.rbvi.stringApp.internal.utils.ViewUtils;

/**
 * Displays information about a protein taken from STRING
 * @author Scooter Morris
 *
 */
public class StringEdgePanel extends AbstractStringPanel {
	JPanel subScorePanel;
	JPanel scorePanel;
	JPanel edgesPanel;
	JButton fetchEdges;
	JButton deleteEdges;
	private Map<CyNetwork, Map<String, Boolean>> colors;

	public StringEdgePanel(final StringManager manager) {
		super(manager);
		filters.get(currentNetwork).put("score", new HashMap<>());

		colors = new HashMap<>();
		colors.put(currentNetwork, new HashMap<>());

		init();
		revalidate();
		repaint();
	}

	private void init() {
		setLayout(new GridBagLayout());
		EasyGBC c = new EasyGBC();
		// add(new JSeparator(SwingConstants.HORIZONTAL), c.anchor("west").expandHoriz());
		JComponent scoreSlider = createFilterSlider("score", "Score", createScoreLabel(), currentNetwork, true, 100.0);
		{
			scorePanel = new JPanel();
			scorePanel.setLayout(new GridBagLayout());
			EasyGBC d = new EasyGBC();
			scorePanel.add(scoreSlider, d.anchor("northwest").expandHoriz());
			
			JPanel controlPanel = createControlPanel();
			controlPanel.setBorder(BorderFactory.createEmptyBorder(0,10,0,0));
			scorePanel.add(controlPanel, d.anchor("west").down().noExpand());				
		}
		add(scorePanel, c.down().anchor("west").noExpand());

		
		JPanel mainPanel = new JPanel();
		{
			mainPanel.setLayout(new GridBagLayout());
			// mainPanel.setBackground(defaultBackground);
			EasyGBC d = new EasyGBC();

			{
				subScorePanel = new JPanel();
				subScorePanel.setLayout(new GridBagLayout());
				EasyGBC e = new EasyGBC();
				subScorePanel.add(createSubScorePanel(), e.anchor("west").expandHoriz());
				subScorePanel.add(new JPanel(), e.down().anchor("west").expandBoth());
			}
			
			mainPanel.add(subScorePanel, d.anchor("west").expandHoriz());

			{
				edgesPanel = new JPanel();
				edgesPanel.setLayout(new GridBagLayout());
				EasyGBC e = new EasyGBC();

				if (currentNetwork != null) {
					List<CyEdge> edges = CyTableUtil.getEdgesInState(currentNetwork, CyNetwork.SELECTED, true);
					for (CyEdge edge : edges) {
						JPanel newPanel = createEdgePanel(edge);
						newPanel.setAlignmentX( Component.LEFT_ALIGNMENT );
			
						edgesPanel.add(newPanel, e.anchor("west").down().expandHoriz());
					}
				}
				edgesPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
			}

			mainPanel.add(edgesPanel, d.down().anchor("west").expandHoriz());
			mainPanel.add(new JLabel(""), d.down().anchor("west").expandBoth());
		}
		JScrollPane scrollPane = new JScrollPane(mainPanel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
		                                         JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
		add(scrollPane, c.down().anchor("west").expandBoth());


		// add(new JPanel(), c.down().anchor("west").expandBoth());
	}

	private String createScoreLabel() {
		String scoreLabel = "Score";
		if (ModelUtils.getNetworkType(currentNetwork) != null) {
			String netType = ModelUtils.getNetworkType(currentNetwork);	
			if (netType.equals(NetworkType.PHYSICAL.toString()))
				scoreLabel += " (" +  NetworkType.PHYSICAL.getAPIName() + ")";
			else if (netType.equals(NetworkType.FUNCTIONAL.toString()))
				scoreLabel += " (" +  NetworkType.FUNCTIONAL.getAPIName() + ")";
		}
		return scoreLabel;
	}
	
	private JPanel createControlPanel() {
		JPanel controlPanel = new JPanel();
		GridLayout layout = new GridLayout(2,2);
		//layout.setVgap(0);
		controlPanel.setLayout(layout);
		{
			fetchEdges = new JButton("Fetch extra edges");
			fetchEdges.setToolTipText("Decrease the network confidence to the chosen score.");
			fetchEdges.setFont(labelFont);
			fetchEdges.setEnabled(false);
			controlPanel.add(fetchEdges);
			fetchEdges.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (filters.containsKey(currentNetwork)
							&& filters.get(currentNetwork).containsKey("score")
							&& filters.get(currentNetwork).get("score").containsKey("Score")) {
						Map<String, Object> args = new HashMap<>();
						args.put("network", "current");
						args.put("confidence", String.valueOf(filters.get(currentNetwork).get("score").get("Score").doubleValue()));
						manager.executeCommand("string", "change confidence", args, null);
						fetchEdges.setEnabled(false);
					}
				}
			});
		}
		{
			deleteEdges = new JButton("Delete hidden edges");
			deleteEdges.setToolTipText("Increase the network confidence to the chosen score.");
			deleteEdges.setFont(labelFont);
			deleteEdges.setEnabled(false);
			controlPanel.add(deleteEdges);
			deleteEdges.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					//ChangeConfidenceTaskFactory tf = new ChangeConfidenceTaskFactory(manager);
					if (filters.containsKey(currentNetwork)
							&& filters.get(currentNetwork).containsKey("score")
							&& filters.get(currentNetwork).get("score").containsKey("Score")) {
						Map<String, Object> args = new HashMap<>();
						args.put("network", "current");
						args.put("confidence", String.valueOf(filters.get(currentNetwork).get("score").get("Score").doubleValue()));
						manager.executeCommand("string", "change confidence", args, null);
						deleteEdges.setEnabled(false);
					}
				}
			});
		}

		{
			JButton changeNetworkType = new JButton("Change network type");
			changeNetworkType.setToolTipText("Switch between functional association and physical interaction egdes.");
			changeNetworkType.setFont(labelFont);
			controlPanel.add(changeNetworkType);
			changeNetworkType.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					ChangeNetTypeTaskFactory tf = new ChangeNetTypeTaskFactory(manager);
					manager.execute(tf.createTaskIterator(currentNetwork), false);
				}
			});
		}
		controlPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		controlPanel.setMaximumSize(new Dimension(100,100));
		return controlPanel;
	}
		
	private JPanel createSubScorePanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new GridBagLayout());
		EasyGBC c = new EasyGBC();

		//List<String> subScoreList = ModelUtils.getSubScoreList(currentNetwork);
		List<String> subScoreList = EvidenceType.getOrderedEvidenceTypes(ModelUtils.getNetworkType(currentNetwork));

		// OK, now we want to create 3 panels: Color, Label, and Filter
		{
			JPanel colorPanel = new JPanel();
			colorPanel.setMinimumSize(new Dimension(25,30));
			colorPanel.setLayout(new GridBagLayout());
			EasyGBC d = new EasyGBC();
			JLabel lbl = new JLabel("Color");
			lbl.setToolTipText("Color edges with this type of evidence.");
			lbl.setFont(labelFont);
			lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
			colorPanel.add(lbl, d.anchor("north").noExpand());

			for (String subScore: subScoreList) {
				colorPanel.add(createScoreCheckBox(subScore), d.down().expandVert());
			}

			//colorPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
			panel.add(colorPanel, c.anchor("northwest").expandVert());
		}

		{
			JPanel labelPanel = new JPanel();
			labelPanel.setLayout(new GridBagLayout());
			EasyGBC d = new EasyGBC();
			JLabel lbl = new JLabel("Evidence");
			lbl.setFont(labelFont);
			lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
			labelPanel.add(lbl, d.anchor("north").noExpand());
			for (String subScore: subScoreList) {
				JLabel scoreLabel = new JLabel(subScore);
				scoreLabel.setFont(textFont);
				scoreLabel.setMinimumSize(new Dimension(100,30));
				scoreLabel.setMaximumSize(new Dimension(100,30));
				labelPanel.add(scoreLabel, d.down().expandVert());
			}
			labelPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
			panel.add(labelPanel, c.right().expandVert());
		}

		{
			JPanel filterPanel = new JPanel();
			filterPanel.setLayout(new GridBagLayout());
			EasyGBC d = new EasyGBC();
			JLabel lbl = new JLabel("Filters");
			lbl.setToolTipText("Hide edges with a confidence score below the chosen subscore.");
			lbl.setFont(labelFont);
			lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
			filterPanel.add(lbl, d.anchor("north").noExpand());
			for (String subScore: subScoreList) {
				JComponent scoreSlider = createFilterSlider("score", subScore, subScore, currentNetwork, false, 100.0);
				scoreSlider.setMinimumSize(new Dimension(100,30));
				// scoreSlider.setMaximumSize(new Dimension(100,30));
				filterPanel.add(scoreSlider, d.down().expandBoth());
			}
			//filterPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
			panel.add(filterPanel, c.right().expandBoth());
		}

		CollapsablePanel collapsablePanel = new CollapsablePanel(iconFont, "Subscores", panel, false, 10);
		collapsablePanel.setBorder(BorderFactory.createEtchedBorder());
		return collapsablePanel;

	}

	private JPanel createEdgePanel(CyEdge edge) {
		JPanel panel = new JPanel();
		EasyGBC c = new EasyGBC();
		panel.setLayout(new GridBagLayout());

		List<String> subScoreList = EvidenceType.getOrderedEvidenceTypes(ModelUtils.getNetworkType(currentNetwork));
		Map<String, Double> subScoresMap = ModelUtils.getSubScores(currentNetwork, edge);
		{
			JPanel labelPanel = new JPanel();
			labelPanel.setLayout(new GridBagLayout());
			EasyGBC d = new EasyGBC();
			JLabel lbl = new JLabel("Evidence");
			lbl.setFont(labelFont);
			labelPanel.add(lbl, d.anchor("west").down().noExpand());
			for (String subScore: subScoreList) {
				JLabel scoreLabel = new JLabel(subScore);
				scoreLabel.setFont(textFont);
				scoreLabel.setMinimumSize(new Dimension(50,30));
				scoreLabel.setMaximumSize(new Dimension(50,30));
				labelPanel.add(scoreLabel, d.anchor("west").down().expandVert());
			}
			// add combined score as the last row
			JLabel scoreLabel = new JLabel("combined");
			scoreLabel.setFont(labelFont);
			scoreLabel.setMinimumSize(new Dimension(50,30));
			scoreLabel.setMaximumSize(new Dimension(50,30));
			labelPanel.add(scoreLabel, d.anchor("west").down().expandVert());
			// add border, etc. 
			labelPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 30));
			panel.add(labelPanel, c.right().expandVert());
		}

		{
			JPanel valuesPanel = new JPanel();
			valuesPanel.setLayout(new GridBagLayout());
			EasyGBC d = new EasyGBC();
			JLabel lbl = new JLabel("Score");
			lbl.setFont(labelFont);
			valuesPanel.add(lbl, d.anchor("west").down().noExpand());
			for (String subScore: subScoreList) {
				JLabel valueLabel;
				if (subScoresMap.get(subScore) == null)
					valueLabel = new JLabel("--");
				else
					valueLabel = new JLabel(subScoresMap.get(subScore).toString());
				valueLabel.setFont(textFont);
				valueLabel.setMinimumSize(new Dimension(50,30));
				valueLabel.setMaximumSize(new Dimension(50,30));
				valuesPanel.add(valueLabel, d.anchor("west").down().expandBoth());				
			}
			// add combined score
			JLabel valueLabel = new JLabel("--");
			Double score = ModelUtils.getDouble(currentNetwork, edge, ColumnNames.SCORE);
			if (score != null) {
				valueLabel = new JLabel(score.toString());
			}			
			valueLabel.setFont(labelFont);
			valueLabel.setMinimumSize(new Dimension(50,30));
			valueLabel.setMaximumSize(new Dimension(50,30));
			valuesPanel.add(valueLabel, d.anchor("west").down().expandBoth());
			panel.add(valuesPanel, c.right().expandHoriz());
			
		}
		
		{
	  		JLabel link = new SwingLinkOSBrowser("See STRING interaction page", getInteractionURL(edge));
			link.setFont(textFont);
			panel.add(link, c.right().expandBoth());
		}
		

		
		// String name = ModelUtils.getName(currentNetwork, edge);
		String name = ModelUtils.getString(currentNetwork, edge.getSource(), ColumnNames.DISPLAY) + " - "
				+ ModelUtils.getString(currentNetwork, edge.getTarget(), ColumnNames.DISPLAY);
		
		CollapsablePanel collapsablePanel = new CollapsablePanel(iconFont, name, panel, false, 10);
		Border etchedBorder = BorderFactory.createEtchedBorder();
		Border emptyBorder = BorderFactory.createEmptyBorder(0,5,0,0);
		collapsablePanel.setBorder(BorderFactory.createCompoundBorder(emptyBorder, etchedBorder));
		return collapsablePanel;
	}
	
	private String getInteractionURL(CyEdge edge) {
		
		
		String sourceNode = ModelUtils.getName(currentNetwork, edge.getSource());
		String targetNode = ModelUtils.getName(currentNetwork, edge.getTarget());
		String netType = "interaction";
		if (ModelUtils.getString(currentNetwork, currentNetwork, ColumnNames.NETWORK_TYPE) != null
				&& ModelUtils.getString(currentNetwork, currentNetwork, ColumnNames.NETWORK_TYPE)
						.equals(NetworkType.PHYSICAL.toString()))
			netType = "interaction-physical";

		if (sourceNode == null || targetNode == null || netType == null) 
			return "";

		CyNetworkView view = manager.getCurrentNetworkView();
		if (view == null || view.getNodeView(edge.getSource()) == null || view.getNodeView(edge.getSource()) == null)
			return "";
		Color sourceNodeColor = (Color)view.getNodeView(edge.getSource()).getVisualProperty(BasicVisualLexicon.NODE_FILL_COLOR);
		Color targetNodeColor = (Color)view.getNodeView(edge.getTarget()).getVisualProperty(BasicVisualLexicon.NODE_FILL_COLOR);
		String colors = "?c1=" + Integer.toHexString(sourceNodeColor.getRGB()).substring(2);
		// String.format("%02x%02x%02x", sourceNodeColor.getRed(), sourceNodeColor.getGreen(), sourceNodeColor.getBlue()); 
		colors += "&c2=" + Integer.toHexString(targetNodeColor.getRGB()).substring(2);
		// String.format("%02x%02x%02x", targetNodeColor.getRed(), targetNodeColor.getGreen(), targetNodeColor.getBlue());
		
		String url = "https://string-db.org/" + netType + "/" + sourceNode + "/" + targetNode + colors;		
		return url;
	}
	
	private void updateEdgesPanel() {
		if (edgesPanel == null) return;
		edgesPanel.removeAll();
		EasyGBC c = new EasyGBC();

		List<CyEdge> edges = CyTableUtil.getEdgesInState(currentNetwork, CyNetwork.SELECTED, true);
		if (edges.size() > ModelUtils.MAX_EDGE_PANELS) {
			JPanel newPanel = new JPanel();
			newPanel.setLayout(new GridLayout(1,0));
			JLabel label = new JLabel("  Select less than "+ ModelUtils.MAX_EDGE_PANELS + " edges to see edge panels.");
			newPanel.add(label);
			newPanel.setAlignmentX( Component.LEFT_ALIGNMENT );
			edgesPanel.add(newPanel, c.anchor("west").down().expandHoriz());
			return;
		}
		for (CyEdge edge: edges) {
			JPanel newPanel = createEdgePanel(edge);
			newPanel.setAlignmentX( Component.LEFT_ALIGNMENT );
			edgesPanel.add(newPanel, c.anchor("west").down().expandHoriz());
		}
		return ;
	}
	

	private JComponent createScoreCheckBox(String subScore) {
		Map<String, Color> colorMap = manager.getChannelColors();
		JCheckBox cb = new JCheckBox("");
		cb.setMinimumSize(new Dimension(20,30));
		cb.setMaximumSize(new Dimension(20,30));
		cb.setBackground(colorMap.get(subScore));
		cb.setOpaque(true);
		if (colors.containsKey(currentNetwork) && colors.get(currentNetwork).containsKey(subScore) 
				&& colors.get(currentNetwork).get(subScore))
			cb.setSelected(true);
		
		cb.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				Boolean selected = Boolean.FALSE;
				if (e.getStateChange() == ItemEvent.SELECTED)
					selected = Boolean.TRUE;

				colors.get(currentNetwork).put(subScore, selected);

				doColors();
			}
		});
		return cb;
	}

	double initFilter(String type, String label) {
		double minValue = 1.0;
		for (CyEdge edge: currentNetwork.getEdgeList()) {
			CyRow edgeRow = currentNetwork.getRow(edge);
			Double edgeScore = edgeRow.get(ColumnNames.SCORE, Double.class);
			if (edgeScore == null) 
				continue;
			Double v = edgeRow.get(ColumnNames.STRINGDB_NAMESPACE, label, Double.class);
			if (v == null) {
				minValue = 0.0;
				break;
			} else if (v < minValue) 
				minValue = v.doubleValue();
		}
		return minValue;
	}
	
	void doFilter(String type) {
		Map<String, Double> filter = filters.get(currentNetwork).get(type);
		// hack needed in order to avoid mistakes in the comparison of the numbers
		int netConf = (int)(ModelUtils.getConfidence(currentNetwork)*1000);
		int score_filter = netConf;
		CyNetworkView view = manager.getCurrentNetworkView();
		for (CyEdge edge: currentNetwork.getEdgeList()) {
			CyRow edgeRow = currentNetwork.getRow(edge);
			Double edgeScore = edgeRow.get(ColumnNames.SCORE, Double.class);
			if (edgeScore == null) 
				continue;
			boolean show = true;
			for (String lbl: filter.keySet()) {
				Double v = edgeRow.get(ColumnNames.STRINGDB_NAMESPACE, lbl.toLowerCase(), Double.class);
				// hack needed in order to avoid mistakes in the comparison of the numbers
				int nv = (int)(filter.get(lbl)*1000);
				if (lbl.equals("Score"))
					score_filter = nv;
				if ((v == null && nv > 0) || (int)(v*1000) < nv) {
					show = false;
					break;
				}
			}
			if (show) {
				// view.getEdgeView(edge).setVisualProperty(BasicVisualLexicon.EDGE_VISIBLE, true);
				view.getEdgeView(edge).clearValueLock(BasicVisualLexicon.EDGE_VISIBLE);
			} else {
				// view.getEdgeView(edge).setVisualProperty(BasicVisualLexicon.EDGE_VISIBLE, false);
				view.getEdgeView(edge).setLockedValue(BasicVisualLexicon.EDGE_VISIBLE, false);
				view.getModel().getRow(edge).set(CyNetwork.SELECTED, false);
			}
		}
		if (score_filter < netConf) { 
			fetchEdges.setEnabled(true);
			deleteEdges.setEnabled(false);
		} else if (score_filter > netConf) {
			fetchEdges.setEnabled(false);
			deleteEdges.setEnabled(true);
		} else {
			fetchEdges.setEnabled(false);
			deleteEdges.setEnabled(false);			
		}
	}

	void undoFilters() {
		CyNetworkView view = manager.getCurrentNetworkView();
		if (view != null) {
			for (View<CyEdge> edge: view.getEdgeViews()) {
				edge.clearValueLock(BasicVisualLexicon.EDGE_VISIBLE);
			}
		}
	}

	void doColors() {
		Map<String, Boolean> color = colors.get(currentNetwork);
		Map<String, Color> colorMap = manager.getChannelColors();
		CyNetworkView view = manager.getCurrentNetworkView();
		for (CyEdge edge: currentNetwork.getEdgeList()) {
			CyRow edgeRow = currentNetwork.getRow(edge);
			double max = -1;
			Color clr = null;
			for (String lbl: color.keySet()) {
				if (!color.get(lbl))
					continue;
				Double v = edgeRow.get(ColumnNames.STRINGDB_NAMESPACE, lbl, Double.class);
				if (v != null && v > max) {
					max = v;
					clr = colorMap.get(lbl);
				}
			}
			if (clr == null)
				view.getEdgeView(edge).clearValueLock(BasicVisualLexicon.EDGE_UNSELECTED_PAINT);
			else
				view.getEdgeView(edge).setLockedValue(BasicVisualLexicon.EDGE_UNSELECTED_PAINT, clr);
		}
	}

	public void updateScore() {
		scorePanel.removeAll();
		EasyGBC d = new EasyGBC();
		JComponent scoreSlider = createFilterSlider("score", "Score", createScoreLabel(), currentNetwork, true, 100.0);
		scorePanel.add(scoreSlider, d.anchor("northwest").expandHoriz());

		JPanel controlPanel = createControlPanel();
		controlPanel.setBorder(BorderFactory.createEmptyBorder(0,10,0,0));
		scorePanel.add(controlPanel, d.anchor("west").down().noExpand());

	}

	public void updateSubPanel() {
		subScorePanel.removeAll();
		EasyGBC d = new EasyGBC();
		subScorePanel.add(createSubScorePanel(), d.anchor("west").expandHoriz());
		subScorePanel.add(new JPanel(), d.down().anchor("west").expandBoth());
	}

	public void networkChanged(CyNetwork newNetwork) {
		this.currentNetwork = newNetwork;
		if (!filters.containsKey(currentNetwork)) {
			filters.put(currentNetwork, new HashMap<>());
			filters.get(currentNetwork).put("score", new HashMap<>());
		}
		if (!colors.containsKey(currentNetwork)) {
			colors.put(currentNetwork, new HashMap<>());
		}

		updateSubPanel();
		updateScore();
		updateEdgesPanel();
	}

	public void selectedEdges(Collection<CyEdge> edges) {
		edgesPanel.removeAll();
		EasyGBC c = new EasyGBC();
		ViewUtils.clearHighlight(manager, manager.getCurrentNetworkView());

		if (edges.size() <= ModelUtils.MAX_EDGE_PANELS) {
			for (CyEdge edge : edges) {
				JPanel newPanel = createEdgePanel(edge);
				newPanel.setAlignmentX( Component.LEFT_ALIGNMENT );
				edgesPanel.add(newPanel, c.anchor("west").down().expandHoriz());
			}
		} else {
			JPanel newPanel = new JPanel();
			newPanel.setLayout(new GridLayout(1,0));
			JLabel label = new JLabel("  Select less than "+ ModelUtils.MAX_EDGE_PANELS + " edges to see edge panels.");
			newPanel.add(label);
			newPanel.setAlignmentX( Component.LEFT_ALIGNMENT );
			edgesPanel.add(newPanel, c.anchor("west").down().expandHoriz());
		}
		
		revalidate();
		repaint();

	}
}
