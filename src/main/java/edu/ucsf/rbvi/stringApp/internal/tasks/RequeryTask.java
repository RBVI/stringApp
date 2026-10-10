package edu.ucsf.rbvi.stringApp.internal.tasks;

import org.apache.log4j.Logger;
import org.cytoscape.application.CyUserLog;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskMonitor;

import edu.ucsf.rbvi.stringApp.internal.model.NetworkType;
import edu.ucsf.rbvi.stringApp.internal.model.Species;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;

import edu.ucsf.rbvi.stringApp.internal.utils.ColumnNames;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;

public class RequeryTask extends AbstractTask {

	final StringManager manager;
	final CyNetwork network;
	private final Logger logger = Logger.getLogger(CyUserLog.NAME);
	private TaskMonitor monitor;


	public RequeryTask(final StringManager manager, CyNetwork network) {
		this.manager = manager;
		this.network = network;
	}

	public void run(TaskMonitor aMonitor) {
		this.monitor = aMonitor;
		monitor.setTitle("Re-query network");

		String sp = ModelUtils.getNetSpecies(network);
		Double conf = ModelUtils.getConfidence(network);
		NetworkType type = NetworkType.getType(ModelUtils.getNetworkType(network));
		if (type == null)
			type = manager.getDefaultNetworkType();
		StringifyTask strTask = new StringifyTask(manager, network, conf.doubleValue(),
				Species.getSpecies(sp), ColumnNames.CANONICAL, type);
		strTask.run(monitor);
	}

	@ProvidesTitle
	public String getTitle() {
		return "Re-query network";
	}

	@Override
	public void cancel() {

	}

}
