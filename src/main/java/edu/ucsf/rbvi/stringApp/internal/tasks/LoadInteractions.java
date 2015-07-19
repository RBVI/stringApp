package edu.ucsf.rbvi.stringApp.internal.tasks;

import java.net.URLEncoder;
import java.util.List;

import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

import edu.ucsf.rbvi.stringApp.internal.io.HttpUtils;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;

public class LoadInteractions extends AbstractTask {
	final StringManager manager;
	final int taxonId;
	final int confidence;
	final int additionalNodes;
	final List<String> stringIds;

	public LoadInteractions(final StringManager manager, final int taxonId, 
	                        final int confidence, final int additionalNodes,
													final List<String>stringIds) {
		this.manager = manager;
		this.taxonId = taxonId;
		this.additionalNodes = additionalNodes;
		this.confidence = confidence;
		this.stringIds = stringIds;
	}

	public void run(TaskMonitor monitor) {
		String ids = null;
		for (String id: stringIds) {
			if (ids == null)
				ids = id;
			else
				ids += "\n"+id;
		}

		String conf = "0."+confidence;
		if (confidence == 100) 
			conf = "1.0";
		String url = manager.getURL()+"psi-mi-tab/interactionsList?species="+taxonId+
		            "&required_score="+conf+
		            "&additional_network_nodes="+additionalNodes+
		            "&identifiers="+URLEncoder.encode(ids.trim());
		System.out.println("URL = "+url);
		String results = HttpUtils.fetchText(url, manager);
		System.out.println("Results: "+results);
	}
}
