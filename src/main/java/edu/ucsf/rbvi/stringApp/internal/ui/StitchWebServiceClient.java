package edu.ucsf.rbvi.stringApp.internal.ui;

import org.cytoscape.work.TaskIterator;
import org.cytoscape.io.webservice.NetworkImportWebServiceClient;
import org.cytoscape.io.webservice.SearchWebServiceClient;
import org.cytoscape.io.webservice.swing.AbstractWebServiceGUIClient;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;

// TODO: [Optional] Improve non-gui mode
public class StitchWebServiceClient extends AbstractWebServiceGUIClient 
                                    implements NetworkImportWebServiceClient, SearchWebServiceClient {
	StringManager manager;

	public StitchWebServiceClient(StringManager manager) {
		super(manager.getNetworkURL(), "STITCH: protein/compound query", "<html>STITCH is a resource to explore known and predicted interactions of chemicals and proteins. Chemicals are linked to other chemicals and proteins by evidence derived from experiments, databases and the literature.  <p>STITCH contains interactions for between 300,000 small molecules and 2.6 million proteins from 1133 organisms.</p></html>");
		this.manager = manager;
		super.gui = new GetTermsPanel(manager, StringManager.STITCHDB);
	}

	public TaskIterator createTaskIterator(Object query) {
		if (query == null)
			throw new NullPointerException("null query");
		return new TaskIterator();
	}

}
