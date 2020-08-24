package edu.ucsf.rbvi.stringApp.internal.tasks;

import java.util.HashMap;
import java.util.Map;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;

public class MCLClusterTask extends AbstractTask {

	private final StringManager manager;
	
	public MCLClusterTask(final StringManager manager, final CyNetwork network) {
		this.manager = manager;
	}

	@Override
	public void run(TaskMonitor arg0) throws Exception {
		Map<String, Object> args = new HashMap<>();
		args.put("inflation_parameter", "4");
		args.put("attribute", "stringdb::score");
		args.put("network", "current");
		args.put("showUI", "true");
		insertTasksAfterCurrentTask(manager.getCommandTaskIterator("cluster", "mcl", args, null));
	}

}
