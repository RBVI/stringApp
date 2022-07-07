package edu.ucsf.rbvi.stringApp.internal.ui;

import org.cytoscape.work.TaskIterator;
import org.cytoscape.io.webservice.NetworkImportWebServiceClient;
import org.cytoscape.io.webservice.SearchWebServiceClient;
import org.cytoscape.io.webservice.swing.AbstractWebServiceGUIClient;

import edu.ucsf.rbvi.stringApp.internal.model.Databases;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;

// TODO: [Optional] Improve non-gui mode
public class CrossSpeciesWebServiceClient extends AbstractWebServiceGUIClient 
                                    implements NetworkImportWebServiceClient, SearchWebServiceClient {
	StringManager manager;

	public CrossSpeciesWebServiceClient(StringManager manager) {
		super(manager.getNetworkURL(), "STRING: cross-species interactions", 
										"<html>The protein query retrieves a STRING network for one or more proteins. <br />"
										+ "STRING is a database of known and predicted protein interactions for <br />"
										+ "thousands of organisms, which are integrated from several sources, <br />"
										+ "scored, and transferred across orthologs. The network includes both <br />"
										+ "physical interactions and functional associations.</html>");
		this.manager = manager;
		super.gui = new CrossSpeciesPanel(manager, Databases.STRING.getAPIName(), false);
	}

	public TaskIterator createTaskIterator(Object query) {
		if (query == null)
			throw new NullPointerException("null query");
		return new TaskIterator();
	}

}
