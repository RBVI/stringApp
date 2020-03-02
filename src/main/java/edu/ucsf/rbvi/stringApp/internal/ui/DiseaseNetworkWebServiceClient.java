package edu.ucsf.rbvi.stringApp.internal.ui;

import org.cytoscape.work.TaskIterator;
import org.cytoscape.io.webservice.NetworkImportWebServiceClient;
import org.cytoscape.io.webservice.SearchWebServiceClient;
import org.cytoscape.io.webservice.swing.AbstractWebServiceGUIClient;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;

// TODO: [Optional] Improve non-gui mode
public class DiseaseNetworkWebServiceClient extends AbstractWebServiceGUIClient 
                                            implements NetworkImportWebServiceClient, SearchWebServiceClient {
	StringManager manager;

	public DiseaseNetworkWebServiceClient(StringManager manager) {
		super(manager.getNetworkURL(), "STRING: disease query", 
							"<html>The disease query retrieves a STRING network for the top-N <br />"
						  + "human proteins associated with the queried disease in the DISEASES <br />"
						  + "database. <br />"
						  + "DISEASES is a weekly updated web resource that integrates evidence <br />"
						  + "on disease-gene associations from automatic text mining, manually <br />"
						  + "curated literature, cancer mutation data, and genome-wide association <br />"
						  + "studies. <br />"
						  + "STRING is a database of known and predicted protein interactions for <br />"
						  + "thousands of organisms, which are integrated from several sources, <br />"
						  + "scored, and transferred across orthologs. The network includes both <br />"
						  + "physical interactions and functional associations.</html>");
		this.manager = manager;
		super.gui = new DiseaseQueryPanel(manager);
	}

	public TaskIterator createTaskIterator(Object query) {
		if (query == null)
			throw new NullPointerException("null query");
		return new TaskIterator();
	}

}
