package edu.ucsf.rbvi.stringApp.internal.model;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Species implements Comparable<Species> {
	private static List<Species> allSpecies;
	private static Map<Integer, Species> taxIdSpecies;
	private int taxon_id;
	private String type;
	private String compactName;
	private String officialName;

	public Species(int tax, String type, String name, String oName) {
		init(tax, type, name, oName);
	}

	public Species(String line) {
		if (line.startsWith("#"))
			return;
		String columns[] = line.trim().split("\t");
		if (columns.length != 4)
			throw new IllegalArgumentException("Can't parse line: "+line);
		try {
			int tax = Integer.parseInt(columns[0]);
			init(tax, columns[1].trim(), columns[2].trim(), columns[3].trim());
		} catch (NumberFormatException nfe) {
			nfe.printStackTrace();
			init(0, columns[1].trim(), columns[2].trim(), columns[3].trim());
		}
	}

	public String toString() { return compactName; }

	public String getName() { return compactName; }

	public int getTaxId() { return taxon_id; }

	public String getType() { return type; }

	public String getOfficialName() { return officialName; }

	public int compareTo(Species t) {
		if (t.toString() == null) return 1;
		return this.toString().compareTo(t.toString());
	}

	private void init(int tax, String type, String name, String oName) {
		this.taxon_id = tax;
		this.type = type;
		this.compactName = name;
		this.officialName = oName;
		// System.out.println("Created species: "+taxon_id+" "+type+" "+compactName+" "+officialName);
	}

	public static List<Species> getSpecies() {
		return allSpecies;
	}

	public static List<Species> readSpecies(StringManager manager) throws Exception {
		allSpecies = new ArrayList<Species>();
		taxIdSpecies = new HashMap<Integer, Species>();

		InputStream stream = null;
		try {
			URL resource = Species.class.getResource("/species.txt");
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
				}
			}
 
			scanner.close();
 
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		Collections.sort(allSpecies);
		return allSpecies;
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

	public static int getSpeciesTaxId(String speciesName) {
		for (Species sp : allSpecies) {
			if (sp.getName().equals(speciesName)) {
				return sp.getTaxId();
			}
		}
		return -1;
	}
	
}
