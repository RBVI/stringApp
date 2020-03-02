package edu.ucsf.rbvi.stringApp.internal.ui;

import org.cytoscape.work.TaskIterator;
import org.cytoscape.io.webservice.NetworkImportWebServiceClient;
import org.cytoscape.io.webservice.SearchWebServiceClient;
import org.cytoscape.io.webservice.swing.AbstractWebServiceGUIClient;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;

// TODO: [Optional] Improve non-gui mode
public class TextMiningWebServiceClient extends AbstractWebServiceGUIClient 
                                        implements NetworkImportWebServiceClient, SearchWebServiceClient {
	StringManager manager;

	public TextMiningWebServiceClient(StringManager manager) {
		super(manager.getNetworkURL(), "STRING: PubMed query", 
						"<html>The PubMed query retrieves a STRING network pertaining <br />"
						+ "to any topic of interest based on text mining of PubMed abstracts. <br />"
						+ "STRING is a database of known and predicted protein interactions for <br />"
						+ "thousands of organisms, which are integrated from several sources, <br />"
						+ "scored, and transferred across orthologs. The network includes both <br />"
						+ "physical interactions and functional associations.</html>");
		this.manager = manager;
		super.gui = new PubMedQueryPanel(manager);
	}

	public TaskIterator createTaskIterator(Object query) {
		if (query == null)
			throw new NullPointerException("null query");
		return new TaskIterator();
	}

}
