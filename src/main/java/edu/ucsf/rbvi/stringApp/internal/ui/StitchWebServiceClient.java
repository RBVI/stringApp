package edu.ucsf.rbvi.stringApp.internal.ui;

import org.cytoscape.work.TaskIterator;
import org.cytoscape.io.webservice.NetworkImportWebServiceClient;
import org.cytoscape.io.webservice.SearchWebServiceClient;
import org.cytoscape.io.webservice.swing.AbstractWebServiceGUIClient;

import edu.ucsf.rbvi.stringApp.internal.model.Databases;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;

// TODO: [Optional] Improve non-gui mode
public class StitchWebServiceClient extends AbstractWebServiceGUIClient 
                                    implements NetworkImportWebServiceClient, SearchWebServiceClient {
	StringManager manager;

	public StitchWebServiceClient(StringManager manager) {
		super(manager.getNetworkURL(), "STITCH: protein/compound query", 
				"<html>The compound query retrieves a STITCH network for one or more <br />"
				+ "proteins or compounds. STITCH is a resource to explore known and <br />"
				+ "predicted interactions of chemicals and proteins. Chemicals are <br />"
				+ "linked to other chemicals and proteins by evidence derived from <br />"
				+ "experiments, databases and the literature.</html>");
		this.manager = manager;
		super.gui = new GetTermsPanel(manager, Databases.STITCH.getAPIName(), false);
	}

	public TaskIterator createTaskIterator(Object query) {
		if (query == null)
			throw new NullPointerException("null query");
		return new TaskIterator();
	}

}
