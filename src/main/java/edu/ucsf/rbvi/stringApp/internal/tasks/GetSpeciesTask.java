package edu.ucsf.rbvi.stringApp.internal.tasks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.json.JSONResult;

import edu.ucsf.rbvi.stringApp.internal.model.Species;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;

public class GetSpeciesTask extends AbstractTask implements ObservableTask {
	final StringManager manager;

	public GetSpeciesTask(StringManager manager) {
		this.manager = manager;
	}

	public void run(TaskMonitor monitor) throws Exception {
		monitor.setTitle(this.getTitle());
	}

	@ProvidesTitle
	public String getTitle() {
		return "List species";
	}

	@Override
	@SuppressWarnings("unchecked")
	public <R> R getResults(Class<? extends R> clzz) {
		List<Species> speciesList = Species.getSpecies();
		if (clzz.equals(List.class)) {
			List<Map<String,String>> speciesMap = new ArrayList<>();
			for (Species species: speciesList) {
				Map<String,String> map = new HashMap<>();
				map.put("taxonomyId", ""+species.getTaxId());
				map.put("scientificName", species.getOfficialName());
				map.put("abbreviatedName", species.getName());
				speciesMap.add(map);
			}
			return (R)speciesMap;
		} else if (clzz.equals(String.class)) {
			StringBuilder sb = new StringBuilder();
			for (Species species: speciesList) {
				sb.append("Species: "+species.getName()+", Tax ID: "+species.getTaxId()+"\n");
			}
			return (R)sb.toString();
		} else if (clzz.equals(JSONResult.class)) {
			JSONResult res = () -> {
				StringBuilder sb = new StringBuilder();
				sb.append("[");
				int count = speciesList.size();
				int index = 0;
				for (Species species: speciesList) {
					sb.append("{\"scientificName\":\""+species.getOfficialName()+
					          "\", \"abbreviatedName\":\""+species.getName()+
		                "\", \"taxonomyId\":"+species.getTaxId()+"}");
					index++;
					if (index < count)
						sb.append(",");
				}
				sb.append("]");
				return sb.toString();
      };
      return (R)res;
		}
		return null;
	}

	@Override
	public List<Class<?>> getResultClasses() {
		return Arrays.asList(JSONResult.class, String.class, List.class);
	}
}
