package edu.ucsf.rbvi.stringApp.internal.tasks;

import java.util.Collections;
import java.util.List;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;

public class AssignCompartmentTask extends AbstractTask {

	final StringManager manager;
	final CyNetwork network;
	
	public AssignCompartmentTask(StringManager manager, CyNetwork network) {
		this.manager = manager;
		this.network = network;
	}
	
	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {

		// save analyzed nodes in network table
		CyTable nodeTable = network.getDefaultNodeTable();
		ModelUtils.createColumnIfNeeded(nodeTable, String.class, ModelUtils.NODE_COMPARTMENT);
		List<String> compartmentCols = ModelUtils.getCompartmentList(network);
		Collections.sort(compartmentCols);
		
		for (CyNode node : network.getNodeList()) {
			Double maxScore = 0.0;
			String maxScoreCol = "";
			for (String col : compartmentCols) {
				Double compScore = network.getRow(node).get(ModelUtils.COMPARTMENT_NAMESPACE, col, Double.class);
				if (compScore != null && compScore > maxScore) {
					maxScore = compScore;
					maxScoreCol = col;
					// System.out.println("found max score for comp " + col);
				}
			}
			if (maxScore > 0.0 && !maxScoreCol.equals("")) {
				network.getRow(node).set(ModelUtils.NODE_COMPARTMENT, maxScoreCol);
			}
		}

	}

}
