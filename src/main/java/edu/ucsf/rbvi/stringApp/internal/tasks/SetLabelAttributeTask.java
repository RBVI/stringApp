package edu.ucsf.rbvi.stringApp.internal.tasks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.util.ListSingleSelection;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.utils.ColumnNames;

public class SetLabelAttributeTask extends AbstractTask {

	final StringManager manager;
	final CyNetwork network;
	final CyTable nodeTable;

	@Tunable(description = "Choose attribute to be set as label", gravity = 1.0)
	public ListSingleSelection<String> attributes = new ListSingleSelection<String>("");

	public SetLabelAttributeTask(StringManager manager, CyNetwork network) {
		this.manager = manager;
		this.network = network;
		nodeTable = network.getDefaultNodeTable();
		initTunable();
	}

	public void run(TaskMonitor monitor) throws Exception {
		monitor.setTitle("Set STRING label attribute");
		// System.out.println("use " + attributes.getSelectedValue());
		if (nodeTable.getColumn(ColumnNames.ELABEL_STYLE) == null
				|| attributes.getSelectedValue() == null)
			return;
		for (CyRow row : nodeTable.getAllRows()) {
			setLabelAttribute(row, attributes.getSelectedValue());
		}
	}

	private void initTunable() {
		List<String> stringCol = new ArrayList<String>();
		for (CyColumn col : nodeTable.getColumns()) {
			if (col.getType().equals(String.class)) {
				stringCol.add(col.getName());
			}
		}
		Collections.sort(stringCol);
		String currSelected = attributes.getSelectedValue();
		attributes = new ListSingleSelection<>(stringCol);
		// select the currently selected one
		if (currSelected != "") {
			attributes.setSelectedValue(currSelected);
			return;
		}
		// select the attribute actually used in the label
		if (nodeTable.getAllRows().size() > 0) {
			currSelected = getLabelAttribute(nodeTable.getAllRows().get(0));
			if (currSelected != "" && stringCol.contains(currSelected)) {
				attributes.setSelectedValue(currSelected);
				return;
			}
		}
		// select a default one, e.g. name
		if (stringCol.contains(CyNetwork.NAME)) {
			attributes.setSelectedValue(CyNetwork.NAME);
		}
	}

	private void setLabelAttribute(CyRow row, String labelAttribute) {
		String enhancedLabel = row.get(ColumnNames.ELABEL_STYLE, String.class);
		String newEnhancedLabel = "";
		if (enhancedLabel != null) {
			// System.out.println(enhancedLabel.length());
			int index1 = enhancedLabel.indexOf("attribute=") + 10;
			// System.out.println(index1);
			int index2 = 0;
			if (enhancedLabel.substring(index1).startsWith("\"")) {
				index2 = index1 + 1 + enhancedLabel.substring(index1 + 1).indexOf("\"") + 1;
			} else {
				index2 = index1 + enhancedLabel.substring(index1).indexOf(" ");
			}
			newEnhancedLabel = enhancedLabel.substring(0, index1) + "\"" + labelAttribute + "\""
					+ enhancedLabel.substring(index2);
			// System.out.println(index2);
			// System.out.println("attribute: " + enhancedLabel.substring(index1, index2));
			// String regex = "attribute=.*\\s+";
			// Matcher m = Pattern.compile(regex).matcher(enhancedLabel);
			// while (m.find()) {
			// System.out.println(enhancedLabel.substring(m.start(), m.end()));
			// }
			// String[] eLabelParts = enhancedLabel.split("\\s+");
			// for (String part : eLabelParts) {
			// part = part.trim();
			// if (part.startsWith("attribute=")) {
			// newEnhancedLabel += " attribute=" + "\"" + labelAttribute + "\"";
			// } else {
			// newEnhancedLabel += part;
			// }
			// newEnhancedLabel += " ";
			// }
		}
		row.set(ColumnNames.ELABEL_STYLE, newEnhancedLabel);
	}

	private String getLabelAttribute(CyRow row) {
		String enhancedLabel = row.get(ColumnNames.ELABEL_STYLE, String.class);
		if (enhancedLabel != null) {
			int index1 = enhancedLabel.indexOf("attribute=") + 10;
			// System.out.println(index1);
			int index2 = 0;
			if (enhancedLabel.substring(index1).startsWith("\"")) {
				index2 = index1 + 1 + enhancedLabel.substring(index1 + 1).indexOf("\"") + 1;
			} else {
				index2 = index1 + enhancedLabel.substring(index1).indexOf(" ");
			}
			// System.out.println(index2);
			// System.out.println("attribute: " + enhancedLabel.substring(index1, index2));
			return enhancedLabel.substring(index1 + 1, index2 - 1);
			// String[] eLabelParts = enhancedLabel.split(" ");
			// for (String part : eLabelParts) {
			// if (part.startsWith("attribute=")) {
			// String[] partParts = part.split("=");
			// if (partParts.length == 2) {
			// if (partParts[1].startsWith("\""))
			// return partParts[1].substring(1, partParts[1].length() - 2);
			// }
			// }
			// }
		}
		return "";
	}

}
