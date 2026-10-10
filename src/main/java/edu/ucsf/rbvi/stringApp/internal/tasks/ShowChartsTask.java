package edu.ucsf.rbvi.stringApp.internal.tasks;

import java.util.Arrays;
import java.util.List;
import javax.swing.SwingUtilities;

import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.json.JSONResult;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.ui.EnrichmentCytoPanel;

/**
 * Task that tells the {@link EnrichmentCytoPanel} to redraw its enrichment
 * charts, invoked on the Swing event thread after the panel's underlying
 * enrichment term list has changed.
 */
public class ShowChartsTask extends AbstractTask implements ObservableTask {

	private StringManager manager;
	private EnrichmentCytoPanel cytoPanel;
	
	public ShowChartsTask(StringManager manager, EnrichmentCytoPanel cytoPanel) {
		this.manager = manager;
		this.cytoPanel = cytoPanel;
	}

	@Override
	public void run(TaskMonitor arg0) throws Exception {
		arg0.setTitle("Show enrichment charts");
		// Filter the current list
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				cytoPanel.drawCharts();
			}
		});
	}

	@Override
	@SuppressWarnings("unchecked")
	public <R> R getResults(Class<? extends R> clzz) {
		if (clzz.equals(String.class)) {
			return (R)"";
		} else if (clzz.equals(JSONResult.class)) {
			JSONResult res = () -> {
				return "{}";
			};
			return (R)res;
		}
		return null;
	}

	@Override
	public List<Class<?>> getResultClasses() {
		return Arrays.asList(JSONResult.class, String.class);
	}

}
