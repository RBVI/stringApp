package edu.ucsf.rbvi.stringApp.internal.model;

import java.awt.Color;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Species implements Comparable<Species> {
	private static List<Species> allSpecies;
	private static List<Species> guiSpecies;
	private static List<Species> coreSpecies;
	private static List<Species> peripherySpecies;
	private static List<Species> mappedSpecies;
	private static List<Species> virusSpecies;
	private static List<Species> modelSpecies;
	private static Map<Integer, Species> taxIdSpecies;
	private static Map<String, Species> nameSpecies;
	private int taxon_id;
	private String type;
	private String compactName;
	private String officialName;
	private String nodeColor;
	private String alias;
	private List<String> interactionPartners;
	private static Species humanSpecies; 

	public static String[] category = { "all", "core", "periphery", "mapped", "viral"};

	public Species(int tax, String type, String name, String oName, String nodeColor, List<String> intPartners, String alias) {
		init(tax, type, name, oName, nodeColor, intPartners, alias);
	}

	public Species(String line) {
		if (line.startsWith("#"))
			return;
		String columns[] = line.trim().split("\t");

		// TODO: add synonyms (alias)
		if (columns.length < 4)
			throw new IllegalArgumentException("Can't parse line: "+line + "\n" + columns.length);
		try {
			int tax = Integer.parseInt(columns[0]);
			List<String> intPartnersList = new ArrayList<String>();
			String nodeColor = "#92B4AF";
			if (columns.length == 6 || columns.length == 7) {
				String[] intPartnersArray = columns[4].trim().split(",");
				for (String intPartner : intPartnersArray) {
					intPartnersList.add(intPartner.trim());
				}
				nodeColor = columns[5].trim();
				if (columns.length == 7) {
					alias = columns[6].trim();
				}
			}
			init(tax, columns[1].trim(), columns[2].trim(), columns[3].trim(), nodeColor, intPartnersList, alias);
		} catch (NumberFormatException nfe) {
			nfe.printStackTrace();
			init(0, columns[1].trim(), columns[2].trim(), columns[3].trim(), "#92B4AF",
					new ArrayList<String>(), null);
		}
	}

	public String toString() { return compactName; }

	public String getName() { return compactName; }

	public int getTaxId() { return taxon_id; }

	public String getType() { return type; }

	public String getOfficialName() { return officialName; }
	
	public String getColor() { return nodeColor; }

	public String getAlias() { return alias; }
	
	public List<String> getInteractionPartners() { return interactionPartners; }

	public int compareTo(Species t) {
		if (t.toString() == null) return 1;
		return this.toString().compareTo(t.toString());
	}

	public static List<Species> search(String str) {
		List<Species> retValue = new ArrayList<Species>();
		for (String s: nameSpecies.keySet()) {
			if (s.regionMatches(true, 0, str, 0, str.length())) { 
				retValue.add(nameSpecies.get(s));
			}
		}
		return retValue;
	}

	private void init(int tax, String type, String name, String oName, String nodeColor, List<String> intPartners,
									  String alias) {
		this.taxon_id = tax;
		this.type = type;
		this.compactName = name;
		this.officialName = oName;
		this.nodeColor = nodeColor;
		this.interactionPartners = intPartners;
		this.alias = alias;
		// System.out.println("Created species: "+taxon_id+" "+type+" "+compactName+" "+officialName);
	}

	public static List<Species> getSpecies() {
		return allSpecies;
	}

	public static List<Species> getGUISpecies() {
		return guiSpecies;
	}
	
	public static List<Species> getCoreSpecies() {
		return coreSpecies;
	}

	public static List<Species> getPeripherySpecies() {
		return peripherySpecies;
	}

	public static List<Species> getMappedSpecies() {
		return mappedSpecies;
	}

	public static List<Species> getVirusSpecies() {
		return virusSpecies;
	}

	public static Species getHumanSpecies() {
		return humanSpecies;
	}

	public static List<Species> getModelSpecies() {
		return modelSpecies;
	}

	public static List<Species> readSpecies(StringManager manager) throws Exception {
		allSpecies = new ArrayList<Species>();
		coreSpecies = new ArrayList<Species>();
		peripherySpecies = new ArrayList<Species>();
		mappedSpecies = new ArrayList<Species>();
		virusSpecies = new ArrayList<Species>();
		guiSpecies = new ArrayList<Species>();
		modelSpecies = new ArrayList<Species>();
		taxIdSpecies = new HashMap<Integer, Species>();
		nameSpecies = new HashMap<String, Species>();

		InputStream stream = null;
		// TODO: get this from the web, if possible
		try {
			URL resource = Species.class.getResource("/species_string11-5.txt");
			if (manager.isVirusesEnabled())
				resource = Species.class.getResource("/species_viruses_string11-5.txt");				
			stream = resource.openConnection().getInputStream();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		try (Scanner scanner = new Scanner(stream)) {
 
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				Species s = new Species(line);
				if (s != null && s.toString() != null && s.toString().length() > 0) {
					allSpecies.add(s);
					taxIdSpecies.put(new Integer(s.getTaxId()), s);
					nameSpecies.put(s.toString(), s);
					if (s.getAlias() != null) {
						nameSpecies.put(s.getAlias(), s);
						modelSpecies.add(s);
					}
					if (s.toString().equals("Homo sapiens"))
						humanSpecies = s;
					if (s.getType().equals("core")) 
						coreSpecies.add(s);
					if (s.getType().equals("periphery")) 
						peripherySpecies.add(s);
					if (s.getType().equals("mapped")) 
						mappedSpecies.add(s);
					if (!s.getType().equals("mapped") && !s.getType().equals("periphery") && !s.getType().equals("core")) 
						virusSpecies.add(s);
				}
			}
 
			scanner.close();
 
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		// add species that would be displayed in GUI
		guiSpecies.addAll(coreSpecies);
		guiSpecies.addAll(peripherySpecies);
		guiSpecies.addAll(virusSpecies);

		// sort all collections
		Collections.sort(allSpecies);
		//System.out.println("all species: " + allSpecies.size());
		Collections.sort(guiSpecies);
		//System.out.println("gui species: " + guiSpecies.size());
		Collections.sort(coreSpecies);
		//System.out.println("core species: " + coreSpecies.size());
		Collections.sort(peripherySpecies);
		//System.out.println("periphery species: " + peripherySpecies.size());
		Collections.sort(mappedSpecies);
		//System.out.println("mapped species: " + mappedSpecies.size());
		Collections.sort(virusSpecies);
		//System.out.println("virus species: " + virusSpecies.size());
		return guiSpecies;
	}

	public static Species getSpecies(String speciesName) {
		if (nameSpecies.containsKey(speciesName))
			return nameSpecies.get(speciesName);

		for (Species s: allSpecies) {
			if (s.getName().equalsIgnoreCase(speciesName))
				return s;
		}
		return null;
	}

	public static List<String> getSpeciesPartners(String speciesName) {
		List<String> partners = new ArrayList<String>();
		for (Species sp : allSpecies) {
			if (sp.getName().equals(speciesName)) {
				for (String spPartner : sp.getInteractionPartners()) {
					try {
						Integer intTaxId = Integer.valueOf(spPartner);
						if (taxIdSpecies.containsKey(intTaxId)) {
							partners.add(taxIdSpecies.get(intTaxId).getName());
						}
					} catch (Exception e) {
						// ignore
					}
				}
			}
		}
		return partners;
	}

	public static String getSpeciesName(String taxId) {
		try {
			Integer intTaxId = Integer.valueOf(taxId);
			if (taxIdSpecies.containsKey(intTaxId)) {
				return taxIdSpecies.get(intTaxId).getName();
			}
		} catch (Exception e) {
			// ignore
		}		
		return "";
	}

	public static String getSpeciesOfficialName(String taxId) {
		try {
			Integer intTaxId = Integer.valueOf(taxId);
			if (taxIdSpecies.containsKey(intTaxId)) {
				return taxIdSpecies.get(intTaxId).getOfficialName();
			}
		} catch (Exception e) {
			// ignore
		}		
		return "";
	}

	public static int getSpeciesTaxId(String speciesName) {
		for (Species sp : allSpecies) {
			if (sp.getName().equals(speciesName)) {
				return sp.getTaxId();
			}
		}
		return -1;
	}
	
	public static String getSpeciesColor(String speciesName) {
		for (Species sp : allSpecies) {
			if (sp.getName().equals(speciesName)) {
				return sp.getColor();
			}
		}
		return "#FFFFFF";
	}

}
