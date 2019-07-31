package edu.ucsf.rbvi.stringApp.internal.tasks;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.cytoscape.application.CyUserLog;
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;

public class AddNamespacesTask extends AbstractTask {

	final StringManager manager;
	private Set<CyNetwork> networks;
	private final Logger logger = Logger.getLogger(CyUserLog.NAME);
	private TaskMonitor monitor;

	@Tunable(description = "Add column namespaces to current STRING networks", 
			longDescription = "Upgrade loaded STRING networks to work with the new side panel of stringApp v1.5 by adding column namespaces to the column names", 
			exampleStringValue = "true", gravity = 1.0, required = true)
	public boolean upgrade = true;

	// @Tunable(description = "Test tuanbles",
	// longDescription = "Test tuanbles",
	// exampleStringValue = "0", required = true, params="slider=true")
	// public int test = 0;

	public AddNamespacesTask(final StringManager manager, final Set<CyNetwork> networks) {
		this.manager = manager;
		this.networks = networks;
	}

	public AddNamespacesTask(final StringManager manager, final CyNetwork net) {
		this.manager = manager;
		networks = new HashSet<CyNetwork>();
		networks.add(net);
	}

	public AddNamespacesTask(final StringManager manager) {
		this.manager = manager;
		networks = new HashSet<CyNetwork>();
	}

	public void run(TaskMonitor aMonitor) {
		this.monitor = aMonitor;
		monitor.setTitle("Add namespaces to column names");
		// System.out.println("running task add namespaces");
		for (CyNetwork net : networks) {
			// System.out.println("checking network: " + net.toString());
			// Set old data version for each network
			ModelUtils.setDataVersion(net, manager.getOldDataVersion());
			ModelUtils.setNetURI(net, "");

			// If user wants to upgrade, add namespaces to the node and edge columns
			if (upgrade) {
				System.out.println("Adding namespaces to old STRING network: " + net.toString());
				monitor.setStatusMessage("Adding namespaces to old STRING network: " + net.toString());
	
				// Get node columns to copy
				CyTable nodeTable = net.getDefaultNodeTable();
				HashMap<CyColumn, String> fromToColumns = new HashMap<CyColumn, String>();
				for (CyColumn col : nodeTable.getColumns()) {
					String columnName = col.getName();
					if (ModelUtils.namespacedNodeAttributes.contains(columnName)) {
						// add STRINGDB namespace
						fromToColumns.put(col, ModelUtils.STRINGDB_NAMESPACE
								+ ModelUtils.NAMESPACE_SEPARATOR + columnName);
						continue;
					}
					if (columnName.startsWith(ModelUtils.TISSUE_NAMESPACE)
							|| columnName.startsWith(ModelUtils.COMPARTMENT_NAMESPACE)) {
						// add tissues or compartments namespace
						fromToColumns.put(col,
								columnName.replaceFirst(" ", ModelUtils.NAMESPACE_SEPARATOR));
					}
				}
	
				// Copy data for selected columns
				for (CyNode node : net.getNodeList()) {
					for (CyColumn oldCol : fromToColumns.keySet()) {
						ModelUtils.createColumnIfNeeded(nodeTable, oldCol.getType(),
								fromToColumns.get(oldCol));
						Object v = nodeTable.getRow(node.getSUID()).getRaw(oldCol.getName());
						nodeTable.getRow(node.getSUID()).set(fromToColumns.get(oldCol), v);
					}
				}
	
				// delete old columns
				// for (CyColumn oldCol : fromToColumns.keySet()) {
				// ModelUtils.deleteColumnIfExisting(nodeTable, oldCol.getName());
				// }
	
				// Edge columns
				CyTable edgeTable = net.getDefaultEdgeTable();
				fromToColumns = new HashMap<CyColumn, String>();
				for (CyColumn col : edgeTable.getColumns()) {
					if (ModelUtils.namespacedEdgeAttributes.contains(col.getName())) {
						// add STRINGDB namespace
						fromToColumns.put(col, ModelUtils.STRINGDB_NAMESPACE
								+ ModelUtils.NAMESPACE_SEPARATOR + col.getName());
					}
				}
	
				// Copy data for selected columns
				for (CyEdge edge : net.getEdgeList()) {
					for (CyColumn oldCol : fromToColumns.keySet()) {
						ModelUtils.createColumnIfNeeded(edgeTable, oldCol.getType(),
								fromToColumns.get(oldCol));
						Object v = edgeTable.getRow(edge.getSUID()).getRaw(oldCol.getName());
						edgeTable.getRow(edge.getSUID()).set(fromToColumns.get(oldCol), v);
					}
				}
			}
		}
	}

	@Override
	public void cancel() {

	}

}
