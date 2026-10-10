package edu.ucsf.rbvi.stringApp.internal.tasks;

import java.util.List;

import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskMonitor;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.model.StringNetwork;
import edu.ucsf.rbvi.stringApp.internal.model.TextMiningResult;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;

/**
 * Task that writes a list of {@link TextMiningResult}s into the node/edge table columns of a
 * {@link StringNetwork}, via {@link ModelUtils#addTextMiningResults}, adding the text mining
 * evidence (e.g. publication counts) retrieved from STRING as new network table columns.
 */
public class AddTextMiningResultsTask extends AbstractTask {
	final StringNetwork stringNet;
	final List<TextMiningResult> tmResults;

	public AddTextMiningResultsTask(final StringNetwork stringNet, final List<TextMiningResult> tmResults) {
		this.stringNet = stringNet;
		this.tmResults = tmResults;
	}

	public void run(TaskMonitor monitor) {
		StringManager manager = stringNet.getManager();
		ModelUtils.addTextMiningResults(manager, tmResults, stringNet.getNetwork());
	}

	@ProvidesTitle
	public String getTitle() { return "Adding text mining columns"; }
}
