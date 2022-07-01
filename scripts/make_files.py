#! /usr/local/bin/python3

#
# This scripts creates the two files used by the stringApp to support Species and
# Species pull-downs as well as information about cross-species PPIs
#
# To run it, cd into the data/species directory and run ../../scripts/make_files.py
Version = "v11.5"
ModelSpecies = "model_species_"
StringSpecies = "species.string."
VirusSpecies = "virus.species."
VirusHostPairs = "virus_host_pairs_"
PathogenHostPairs = "pathogen_host_pairs_"

def main():
    species = {}

    # Start by reading in our string species
    for line in open(StringSpecies+Version+".tsv"):
        if line.startswith("#"):
            continue
        v = line.strip().split("\t")
        species[v[0]] = (v[1], v[2], v[3])

    # Now read in our virus file
    for line in open(VirusSpecies+Version+".tsv"):
        if line.startswith("#"):
            continue
        v = line.strip().split("\t")
        species[v[0]] = ("virus", v[1], v[2])

    # Read in the model organisms file
    for line in open(ModelSpecies+Version+".tsv"):
        if line.startswith("#") or len(line) <= 1:
            continue
        (taxid, common) = line.strip().split("\t")
        if taxid in species:
            (string_type, name1, name2) = species[taxid]
            species[taxid] = (string_type, name1, name2, common)

    # Output species file
    output = open("../../src/main/resources/species_"+Version+".tsv", "w")
    for taxid in species:
        v = species[taxid]
        if len(v) == 3:
            (string_type, name1, name2) = species[taxid]
            output.write("%s\t%s\t%s\t%s\n"%(taxid, string_type, name1, name2))
        elif len(v) == 4:
            (string_type, name1, name2, common) = species[taxid]
            output.write("%s\t%s\t%s\t%s\t%s\n"%(taxid, string_type, name1, name2, common))
    output.close()

    # Read in the Virus-Host pairs file
    pairs = {}
    for line in open(VirusHostPairs+Version+".tsv"):
        if line.startswith("#"):
            continue
        (source, target) = line.strip().split("\t")
        if (source == target):
            continue
        if not source in pairs:
            pairs[source] = set()

        pairs[source].add(target)

    # Read in the Virus-Host pairs file
    for line in open(VirusHostPairs+Version+".tsv"):
        if line.startswith("#"):
            continue
        (source, target) = line.strip().split("\t")
        if (source == target):
            continue
        if not source in pairs:
            pairs[source] = set()

        pairs[source].add(target)

    # Output the interacting pairs file
    output = open("../../src/main/resources/pairs_"+Version+".tsv", "w")
    for tax in pairs:
        output.write("%s\t%s\n"%(tax, ",".join(pairs[tax])))
    output.close()

if __name__ == "__main__":
    main()
