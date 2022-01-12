package com.sanger.searchBRASS;

import java.io.*;
import java.util.*;
import com.beust.jcommander.*;
import com.beust.jcommander.validators.PositiveInteger;


public class searchBRASS {
	private static String versionNumber = "0.2";
	@Parameter
	private List<String> parameters = new ArrayList<String>();
	
	@Parameter(names = "--input-bam-file", description = "Input BAM file.", required = true, converter = FileConverter.class, validateWith = FileValidator.class, order=0)
	public File input_bam_file = null;
	
	@Parameter(names = "--brass-bed-file", description = "BRASS BED output file to filter.", required = true, converter = FileConverter.class, validateWith=FileValidator.class, order=1)
	public File brass_bed_file = null;
	
	@Parameter(names = "--output-bed-file", description = "Output BED file to store results.", required = true, order=2)
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
		var sb  = new searchBRASS();
		var jCommander = new JCommander(sb);
		jCommander.setProgramName("SearchBRASS.jar");
		JCommander.newBuilder().addObject(sb).build().parse(args);
		if(sb.version != null && sb.version) {
			System.out.println("Search SV based on input BED: " + versionNumber);
			System.exit(0);
		}
		else if(sb.help) {
			jCommander.usage();
			System.exit(0);
		} else  {
			var nThreads = Runtime.getRuntime().availableProcessors();
			if(sb.threads > nThreads)
				System.out.println("Warning: Number of threads exceeds number of available cores");
			new searchBRASSCore(sb.input_bam_file, sb.brass_bed_file, sb.output_bed_file, sb.search_width, sb.discordant_distance, sb.threads);	
		}
	}
}
