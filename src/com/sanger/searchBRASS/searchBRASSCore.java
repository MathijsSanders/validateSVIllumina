package com.sanger.searchBRASS;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.regex.Pattern;
import java.util.stream.*;
import htsjdk.samtools.*;

public class searchBRASSCore {
	
	private String header = null;
	private final Pattern pattern = Pattern.compile("[\\]\\[](.*):");
	private final Pattern locPat = Pattern.compile(":([0-9]+)");
	
	public searchBRASSCore(File inputBam, File inputBed, String outputBed, int sw, int dd, int threads) {
		System.out.println("Loading SVs...");
		var svList = retrieveStructuralVariants(inputBed, dd);
		System.out.println("First annotation step...");
		firstAnnotationStep(svList, inputBam, sw, threads);
		Collections.sort(svList, Comparator.comparing(svInformation::getChromLeft).thenComparing(svInformation::getStartLeft));
		try {
			writeResults(svList, outputBed);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-11);
		}
	}
	private void writeResults(ArrayList<svInformation> primary, String output) throws IOException {
		var data = new ArrayList<String>(Arrays.asList(header));
		data.addAll(primary.stream().map(i -> i.getLine()).collect(Collectors.toCollection(ArrayList::new)));
		Files.write(Paths.get(output), data, Charset.defaultCharset());
	}
	private ArrayList<svInformation> retrieveStructuralVariants(File bed, int dd) {
		Supplier<Stream<String>> streamSupplier = () -> {try{return Files.lines(bed.toPath());}catch(IOException e){e.printStackTrace();} return null;};
		header = String.join("\t", streamSupplier.get().findFirst().get(), "Illumina Present", "SupportingPairs");
		return streamSupplier.get().skip(1).map(i -> i.split("\t")).map(i -> extractStructuralVariant(i, dd)).collect(Collectors.toCollection(ArrayList::new));
	}
	private svInformation extractStructuralVariant(String[] tokens, int dd) {
		if(tokens[5].contains("BND")) {
			var match = pattern.matcher(tokens[8]);
			var matchLoc = locPat.matcher(tokens[8]);
			var secPos = (matchLoc.find()) ? Integer.parseInt(matchLoc.group(1)) : -1;
			return new svInformation(String.join("\t", tokens), tokens[1], Integer.parseInt(tokens[2]), Integer.parseInt(tokens[2]), (match.find()) ? match.group(1) : "NA", secPos, secPos, (x,y) -> (y-x) <= dd);
		}
		return new svInformation(String.join("\t", tokens), tokens[1], Integer.parseInt(tokens[2]), Integer.parseInt(tokens[2]), tokens[1], Integer.parseInt(tokens[3]), Integer.parseInt(tokens[3]), (x,y) -> (y-x) <= dd);
	}
	private void firstAnnotationStep(ArrayList<svInformation> svList, File bam, int sw, int threads) {
		var forkJoinPool = new ForkJoinPool(threads);
		try {
			forkJoinPool.submit(() -> svList.parallelStream().forEach(i -> annotateFirstSV(i, bam, sw))).get();
		} catch(InterruptedException | ExecutionException e) {
			e.printStackTrace();
			System.exit(-3);
		}
	}
	private void annotateFirstSV(svInformation sv, File bam, int sw) {
		System.out.println(String.join(" - ", "Processing ", sv.getChromLeft(), sv.getChromRight(), Integer.toString(sw)));
		SAMRecordIterator it = null;
		Boolean controlPresent = false;
		Integer supportingPairs = 0;
		HashMap<String, SAMRecord> pairedMap = new HashMap<String, SAMRecord>(1000,0.9999f);
		var inputSam = SamReaderFactory.make().enable(SamReaderFactory.Option.DONT_MEMORY_MAP_INDEX).validationStringency(ValidationStringency.LENIENT).samRecordFactory(DefaultSAMRecordFactory.getInstance()).open(bam);
		try {
			var intervals = QueryInterval.optimizeIntervals(new QueryInterval[]{new QueryInterval(inputSam.getFileHeader().getSequenceIndex(sv.getChromLeft()), sv.getStartLeft() - sw, sv.getEndLeft() + sw), new QueryInterval(inputSam.getFileHeader().getSequenceIndex(sv.getChromRight()), sv.getStartRight() - sw, sv.getEndRight() + sw)});
			it = inputSam.queryOverlapping(intervals);
			SAMRecord currentRecord = null;
			SAMRecord pairedRecord = null;
			while(it.hasNext()) {
				currentRecord = it.next();
				if(!pairedMap.containsKey(currentRecord.getReadName()))
					pairedMap.put(currentRecord.getReadName(),currentRecord);
				else if(!pairedMap.get(currentRecord.getReadName()).getPairedReadName().equals(currentRecord.getPairedReadName())) {
					pairedRecord = pairedMap.get(currentRecord.getReadName());
					pairedMap.remove(currentRecord.getReadName());
					if(!sv.getChromLeft().equals(sv.getChromRight())) {
						if(!currentRecord.getReferenceName().equals(pairedRecord.getReferenceName())) {
							controlPresent = true;
							supportingPairs++;
						}
					} else if(fitsStructuralVariant(currentRecord, pairedRecord, sv, sw) && fitsDirectionSmall(currentRecord, pairedRecord, sv)) {
							controlPresent = true;
							supportingPairs++;
					}
				}
			}
			it.close();
			inputSam.close();
		} catch (IOException e) {
			System.out.println(sv.getChromLeft() + " - " + sv.getChromRight());
			e.printStackTrace();
			System.exit(-10);
		}
		sv.setStatistics(controlPresent, supportingPairs);
	}
	private boolean fitsDirectionSmall(SAMRecord first, SAMRecord second, svInformation info) {
		SAMRecord tmp = null;
		if(info.getBelowDiscordant()) {
			if(first.getAlignmentStart() > second.getAlignmentStart()) {
				tmp = first;
				first = second;
				second = tmp;
			}
			if(first.getReadNegativeStrandFlag() != second.getReadNegativeStrandFlag())
				return false;
		}
		return true;
	}	
	private boolean fitsStructuralVariant(SAMRecord first, SAMRecord second, svInformation info, int search_width) {
		Boolean firstLeft, firstRight, secondLeft, secondRight;
		firstLeft = (!first.getReadNegativeStrandFlag()) ? (first.getAlignmentStart() <= info.getStartLeft() && Math.abs(info.getStartLeft() - first.getAlignmentEnd()) < search_width) : (first.getAlignmentEnd() >= info.getStartLeft() && Math.abs(first.getAlignmentStart() - info.getStartLeft()) < search_width);
		firstRight = (!first.getReadNegativeStrandFlag()) ? (first.getAlignmentStart() <= info.getStartRight() && Math.abs(info.getStartRight() - first.getAlignmentEnd()) < search_width) : (first.getAlignmentEnd() >= info.getStartRight() && Math.abs(first.getAlignmentStart() - info.getStartRight()) < search_width);
		secondLeft = (!second.getReadNegativeStrandFlag()) ? (second.getAlignmentStart() <= info.getStartLeft() && Math.abs(info.getStartLeft() - second.getAlignmentEnd()) < search_width) : (second.getAlignmentEnd() >= info.getStartLeft() && Math.abs(second.getAlignmentStart() - info.getStartLeft()) < search_width);
		secondRight = (!second.getReadNegativeStrandFlag()) ? (second.getAlignmentStart() <= info.getStartRight() && Math.abs(info.getStartRight() - second.getAlignmentEnd()) < search_width) : (second.getAlignmentEnd() >= info.getStartRight() && Math.abs(second.getAlignmentStart() - info.getStartRight()) < search_width);
		if((!firstLeft && !firstRight) || (!secondLeft && !secondRight))
			return false;
		else if((firstLeft && firstRight) || (secondLeft && secondRight))
			return false;
		else if((firstLeft == secondLeft) || (firstRight == secondRight))
			return false;
		return true;
	}
}
