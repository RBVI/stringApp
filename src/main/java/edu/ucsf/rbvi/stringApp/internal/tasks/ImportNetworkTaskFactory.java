package edu.ucsf.rbvi.stringApp.internal.tasks;

import java.util.List;
import java.util.Map;

import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.stringApp.internal.model.Databases;
import edu.ucsf.rbvi.stringApp.internal.model.NetworkType;
import edu.ucsf.rbvi.stringApp.internal.model.Species;
import edu.ucsf.rbvi.stringApp.internal.model.StringNetwork;

public class ImportNetworkTaskFactory extends AbstractTaskFactory {
	final StringNetwork stringNet;
	final String speciesName;
	final Species species;
	int confidence;
	int additionalNodes;
	final List<String> stringIds;
	final Map<String, String> queryTermMap;
	String netName;
	String useDATABASE;
	NetworkType netType;

	public ImportNetworkTaskFactory(final StringNetwork stringNet, final String speciesName,
            Species species, int confidence, int additional_nodes,
            final List<String> stringIds, final Map<String, String> queryTermMap,
			final String netName, final String useDATABASE, final NetworkType netType) {
		this(stringNet, speciesName, species, confidence, additional_nodes, stringIds, queryTermMap, netName, useDATABASE);
		this.netType = netType;
	}
	
	public ImportNetworkTaskFactory(final StringNetwork stringNet, final String speciesName,
	                                Species species, int confidence, int additional_nodes,
	                                final List<String> stringIds,
																	final Map<String, String> queryTermMap,
																	final String netName,
																	final String useDATABASE) {
		this.stringNet = stringNet;
		this.species = species;
		this.confidence = confidence;
		this.additionalNodes = additional_nodes;
		this.stringIds = stringIds;
		this.speciesName = speciesName;
		this.queryTermMap = queryTermMap;
		this.netName = netName;
		this.useDATABASE = useDATABASE;
	}

	public TaskIterator createTaskIterator() {
		if (stringIds == null) {
			//System.out.println("Calling LoadSpeciesInteractions");
			return new TaskIterator(
					new LoadSpeciesInteractions(stringNet, speciesName, species, confidence,
					                            species.getName(),
					                            useDATABASE, netType));
		} else if (stringNet.getNetwork() == null) {
			if (useDATABASE.equals(Databases.STITCH.getAPIName())) {
				System.out.println("Calling LoadInteractions2");
				if (queryTermMap.size() == 1 && queryTermMap.keySet().iterator().next().startsWith("CID")) {
					useDATABASE = Databases.STRING.getAPIName();
				}
				return new TaskIterator(new LoadInteractions2(stringNet, speciesName, species,
																										  confidence, additionalNodes, stringIds,
																										  queryTermMap, netName, useDATABASE, netType));
			} else {
				System.out.println("Calling LoadInteractions");
				return new TaskIterator(new LoadInteractions(stringNet, speciesName, species,
																										 confidence, additionalNodes, stringIds,
																										 queryTermMap, netName, useDATABASE, netType));
			}
		}
		//System.out.println("Calling LoadTermsTask");
		return new TaskIterator(new LoadTermsTask(stringNet, speciesName, species, confidence,
		                                          additionalNodes, stringIds, queryTermMap, useDATABASE, netType));
	}

	public boolean isReady() {
		return stringNet.getManager().haveURIs();
	}
}
