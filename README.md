# Validate PacBio-detected SVs in Illumina WGS data

Determine whether structural variants (SVs) detected in PacBio data is present in matched Illumina WGS data.

## How do I run it?

First, align PacBio sequencing data with NGMLR (https://github.com/philres/ngmlr), call SVs using Sniffles (https://github.com/fritzsedlazeck/Sniffles), annotate Sniffles output with AnnotSV (https://github.com/lgmgeo/AnnotSV) and, possibly, annotate and filter final Sniffles results with annotateSniffles (https://github.com/MathijsSanders/annotateSniffles).

Next, provide the Illumina WGS BAM files and filtered Sniffles output, tweak the parameters to your liking and learn whether the SV is detectable in the Illumina WGS BAM file and how many read-pairs support it.

### The recommended way

The pre-compiled JAR file is included with the repository, but in case the package needs to be recompiled, please run:

```bash
mvn package clean
```

The following command adds two columns to the annotated Sniffles file.

- Illumina present: Whether the SV is detectable in the Illumina WGS BAM file (true: present, false: absent)
- SupportingPairs: How many read-pairs support the SV

```bash
java -Xmx20G -jar validateSVIllumina.jar --input-bam-file input_bam_file --sniffles-file annotated_sniffles_file --output-file output_file --search_width window_size --discordant-distance abnormal_read_pair_distance --threads threads
```

- --input-bam-file*: Input Illumina WGS BAM file.
- --sniffles-file*: Input annotated Sniffles file.
- --output-file*: Output file.
- --search-width: Window around the SV breakpoints for extracting reads (default: 250nt).
- --discordant-distance: Distance between read-pairs to be considered discordant (default: 1000nt).
- --threads: Comma-separated list of decoy names (default: 1).    
- --help, -help: Get usage information.
- --version, -version: Get current version.
- \* Required.

*Dependencies*
- Maven version 3+ (For compiling only).
- Java JDK 11+
