package edu.ucsf.rbvi.stringApp.internal.tasks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cytoscape.model.CyColumn;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.json.JSONResult;
import org.cytoscape.work.util.ListSingleSelection;

import edu.ucsf.rbvi.stringApp.internal.model.Species;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;

public class GetSelectedSpeciesTask extends AbstractTask implements ObservableTask {
	final StringManager manager;

	@Tunable(description="Category of species to list", 
	         longDescription="Select which category to list from these: all, core, periphery, mapped, and viral species.",
	         exampleStringValue="core",
	        context="nogui", required=false)
	 public ListSingleSelection<String> category;

	
	public GetSelectedSpeciesTask(StringManager manager) {
		this.manager = manager;
		category = new ListSingleSelection<>(Species.category);
		category.setSelectedValue("all");
	}

	public void run(TaskMonitor monitor) throws Exception {
		monitor.setTitle(this.getTitle());

    while (!Species.haveSpecies()) {
      Thread.sleep(100);
    }
	}

	@ProvidesTitle
	public String getTitle() {
		return "List species";
	}

	@Override
	@SuppressWarnings("unchecked")
	public <R> R getResults(Class<? extends R> clzz) {
		List<Species> stringSpecies = new ArrayList<Species>();
		if (category.getSelectedValue().equals("core"))
			stringSpecies = Species.getCoreSpecies();
		else if (category.getSelectedValue().equals("periphery"))
			stringSpecies = Species.getPeripherySpecies();
		else if (category.getSelectedValue().equals("mapped"))
			stringSpecies = Species.getMappedSpecies();
		else if (category.getSelectedValue().equals("viral"))
			stringSpecies = Species.getVirusSpecies();
		else 
			stringSpecies = Species.getSpecies();
		
		List<Species> speciesList = stringSpecies;
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
