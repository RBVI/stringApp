package edu.ucsf.rbvi.stringApp.internal.tasks;

import java.util.HashMap;
import java.util.Map;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.FinishStatus;
import org.cytoscape.work.FinishStatus.Type;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.TaskObserver;
import org.cytoscape.work.Tunable;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;

public class MCLClusterTask extends AbstractTask implements TaskObserver {

	private final StringManager manager;
	private TaskMonitor taskMonitor;
	
	@Tunable(description = "Granularity parameter (inflation value)", 
	         //longDescription = "",
					 exampleStringValue = "4.0",
					 gravity = 1.0)
	public double infl_param = 4.0;

	
	public MCLClusterTask(final StringManager manager, final CyNetwork network) {
		this.manager = manager;
		this.taskMonitor = null;
	}

	@Override
	public void run(TaskMonitor arg0) throws Exception {
		this.taskMonitor = arg0;
		if (!manager.haveClusterMaker()) {
			this.taskMonitor.setStatusMessage("Installing clusterMaker2");
			Map<String, Object> args = new HashMap<>();
			args.put("app", "clusterMaker2");
			manager.executeCommand("apps", "install", args, this);
		} else {
			doClustering();	
		}
	}

	@ProvidesTitle
	public String getTitle() {
		return "Cluster network using MCL";
	}

	@Override
	public void taskFinished(ObservableTask task) {
		doClustering();
	}

	@Override
	public void allFinished(FinishStatus finishStatus) {
	}


	public void doClustering() {
		this.taskMonitor.setStatusMessage("Clustering network using MCL ...");
		Map<String, Object> args = new HashMap<>();
		args.put("inflation_parameter", infl_param);
		args.put("attribute", "stringdb::score");
		args.put("network", "current");
		args.put("showUI", "true");
		insertTasksAfterCurrentTask(manager.getCommandTaskIterator("cluster", "mcl", args, null));		
	}
	
}
