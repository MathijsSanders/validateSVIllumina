package com.sanger.validateSVIllumina;

import java.io.*;
import java.util.*;
import com.beust.jcommander.*;
import com.beust.jcommander.validators.PositiveInteger;

public class validateSVIllumina {

	private static String versionNumber = "0.2";
	@Parameter
	private List<String> parameters = new ArrayList<String>();
	
	@Parameter(names = "--input-bam-file", description = "Input BAM file.", required = true, converter = FileConverter.class, validateWith = FileValidator.class, order=0)
	public File input_bam_file = null;
	
	@Parameter(names = "--sniffles-file", description = "Filtered AnnotSV-annotated Sniffles file.", required = true, converter = FileConverter.class, validateWith=FileValidator.class, order=1)
	public File sniffles_file = null;
	
	@Parameter(names = "--output-file", description = "Output file to store results.", required = true, order=2)
	public String output_bed_file = null;
	
	@Parameter(names = "--search-width", description = "Window to extract reads (mutation_position +- width).", validateWith = PositiveInteger.class, order=3)
	public Integer search_width = 500;
	
	@Parameter(names = "--discordant-distance", description = "Distance threshold for discordant read-pair.", validateWith = PositiveInteger.class, order=4)
	public Integer discordant_distance = 1000;
	
	@Parameter(names = "--threads", description = "Number of threads.", validateWith = PositiveInteger.class, order=5)
	public Integer threads = 1;
	
	@Parameter(names = {"--help","-help"}, help = true, description = "Get usage information", order=6)
	private boolean help;
	
	@Parameter(names = {"--version","-version"}, description = "Get current version", order=7)
	private Boolean version = null;
	
	public static void main(String[] args) {
		var vsv  = new validateSVIllumina();
		var jCommander = new JCommander(vsv);
		jCommander.setProgramName("validateSVIllumina.jar");
		JCommander.newBuilder().addObject(vsv).build().parse(args);
		if(vsv.version != null && vsv.version) {
			System.out.println("Search SV based on filtered Sniffles output: " + versionNumber);
			System.exit(0);
		}
		else if(vsv.help) {
			jCommander.usage();
			System.exit(0);
		} else  {
			var nThreads = Runtime.getRuntime().availableProcessors();
			if(vsv.threads > nThreads)
				System.out.println("Warning: Number of threads exceeds number of available cores");
			new validateSVIlluminaCore(vsv.input_bam_file, vsv.sniffles_file, vsv.output_bed_file, vsv.search_width, vsv.discordant_distance, vsv.threads);	
		}
	}
}
