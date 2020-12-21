package edu.ucsf.rbvi.stringApp.internal.tasks;

import java.util.List;
import java.util.Map;

import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.stringApp.internal.model.NetworkType;
import edu.ucsf.rbvi.stringApp.internal.model.Species;
import edu.ucsf.rbvi.stringApp.internal.model.StringNetwork;

public class ImportNetworkTaskFactory extends AbstractTaskFactory {
	final StringNetwork stringNet;
	final String species;
	int taxon;
	int confidence;
	int additionalNodes;
	final List<String> stringIds;
	final Map<String, String> queryTermMap;
	String netName;
	String useDATABASE;
	NetworkType netType;

	public ImportNetworkTaskFactory(final StringNetwork stringNet, final String species,
            int taxon, int confidence, int additional_nodes,
            final List<String> stringIds, final Map<String, String> queryTermMap,
			final String netName, final String useDATABASE, final NetworkType netType) {
		this(stringNet, species, taxon, confidence, additional_nodes, stringIds, queryTermMap, netName, useDATABASE);
		this.netType = netType;
	}
	
	public ImportNetworkTaskFactory(final StringNetwork stringNet, final String species,
	                                int taxon, int confidence, int additional_nodes,
	                                final List<String> stringIds,
																	final Map<String, String> queryTermMap,
																	final String netName,
																	final String useDATABASE) {
		this.stringNet = stringNet;
		this.taxon = taxon;
		this.confidence = confidence;
		this.additionalNodes = additional_nodes;
		this.stringIds = stringIds;
		this.species = species;
		this.queryTermMap = queryTermMap;
		this.netName = netName;
		this.useDATABASE = useDATABASE;
	}

	public TaskIterator createTaskIterator() {
		if (stringIds == null) {
			return new TaskIterator(
					new LoadSpeciesInteractions(stringNet, species, taxon, confidence,
					                            Species.getSpeciesOfficialName(String.valueOf(taxon)),
					                            useDATABASE, netType));
		} else if (stringNet.getNetwork() == null) {
			return new TaskIterator(new LoadInteractions(stringNet, species, taxon,
			                                             confidence, additionalNodes, stringIds,
			                                             queryTermMap, netName, useDATABASE, netType));
		}
		return new TaskIterator(new LoadTermsTask(stringNet, species, taxon, confidence,
		                                          additionalNodes, stringIds, queryTermMap, useDATABASE, netType));
	}

	public boolean isReady() {
		return stringNet.getManager().haveURIs();
	}
}
