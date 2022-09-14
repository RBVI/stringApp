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
import java.util.TreeMap;

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
  private static boolean haveSpecies = false;
	private int taxon_id;
	private String type;
	private String compactName;
	private String officialName;
	private String alias;
	private List<String> interactionPartners;
	private static Species humanSpecies; 

	public static String[] category = { "all", "core", "periphery", "mapped", "viral"};

	/*
	public Species(int tax, String type, String name, String oName, String alias) {
		init(tax, type, oName, name, alias);
	}
	*/

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

			if (columns.length == 4 || columns.length == 5) {
				if (columns.length == 5) {
					alias = columns[4].trim();
				}
			}
			init(tax, columns[1].trim(), columns[2].trim(), columns[3].trim(), alias);
		} catch (NumberFormatException nfe) {
			nfe.printStackTrace();
			init(0, columns[1].trim(), columns[2].trim(), columns[3].trim(), null);
		}
	}

	public String toString() { return compactName; }

	public String getName() { return compactName; }

	public int getTaxId() { return taxon_id; }

	public String getType() { return type; }

	public String getOfficialName() { return officialName; }
	
	public String getAlias() { return alias; }
	
	public List<String> getInteractionPartners() { return interactionPartners; }
	public void setInteractionPartners(List<String> partners) { interactionPartners = partners; }

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

	private void init(int tax, String type, String oName, String name, String alias) {
		this.taxon_id = tax;
		this.type = type;
		this.compactName = name;
		this.officialName = oName;
		this.interactionPartners = new ArrayList<String>();
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

  public static boolean haveSpecies() { return haveSpecies; }

	public static List<Species> readPairs(StringManager manager) throws Exception {
		InputStream stream = null;
		try {
			URL pairsURL = new URL(StringManager.PairsURI);
			stream = pairsURL.openConnection().getInputStream();
		} catch (Exception e) {
			e.printStackTrace();
			URL pairsURL = new URL(Species.class.getResource("/pairs_v11.5.tsv").toString());
			stream = pairsURL.openConnection().getInputStream();	
		}

		try (Scanner scanner = new Scanner(stream)) {
 
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				if (line.startsWith("#")) 
					continue;
				String[] ids = line.split("\t");
				Integer sourceTax = Integer.valueOf(ids[0]);
				List<String> pairs = Arrays.asList(ids[1].split(","));
				if (taxIdSpecies.containsKey(sourceTax)) {
					taxIdSpecies.get(sourceTax).setInteractionPartners(pairs);
				}

			}

			scanner.close();

		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}

		return allSpecies;
	}

	public static List<Species> readSpecies(StringManager manager) throws Exception {
    haveSpecies = false;
		allSpecies = new ArrayList<Species>();
		coreSpecies = new ArrayList<Species>();
		peripherySpecies = new ArrayList<Species>();
		mappedSpecies = new ArrayList<Species>();
		virusSpecies = new ArrayList<Species>();
		guiSpecies = new ArrayList<Species>();
		modelSpecies = new ArrayList<Species>();
		taxIdSpecies = new HashMap<Integer, Species>();
		nameSpecies = new TreeMap<String, Species>();

		InputStream stream = null;
		try {
			URL speciesURL = new URL(StringManager.SpeciesURI);
			stream = speciesURL.openConnection().getInputStream();
		} catch (Exception e) {
			e.printStackTrace();
			URL speciesURL = new URL(Species.class.getResource("/species_v11.5.tsv").toString());
			stream = speciesURL.openConnection().getInputStream();
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
		Collections.sort(modelSpecies);

    haveSpecies = true;
		return guiSpecies;
	}

	public static Species getSpecies(String speciesName) {
		if (nameSpecies == null || speciesName == null) return null;
		if (nameSpecies.containsKey(speciesName))
			return nameSpecies.get(speciesName);

		if (allSpecies == null) return null;
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
        return partners;
			}
		}

    // This will be empty
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

	public static String abbreviate(String speciesName) {
		if (speciesName.length() <= 20) return speciesName;
		String[] names = speciesName.split(" ", 2);
		String newName = names[0].substring(0,1)+". "+names[1];
		newName = newName.substring(0, Math.min(newName.length(), 20));
		return newName;
	}
	
}
