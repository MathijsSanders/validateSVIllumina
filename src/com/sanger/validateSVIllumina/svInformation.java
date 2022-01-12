package com.sanger.validateSVIllumina;

import java.util.function.BiFunction;

public class svInformation {
	private String line = null;
	private String chromLeft = null;
	private Integer startLeft = null;
	private Integer endLeft = null;
	private String chromRight = null;
	private Integer startRight = null;
	private Integer endRight = null;
	private Boolean controlPresent = null;
	private Integer supportingPairs = null;
	private Boolean belowDiscordant = null;
	public svInformation (String line, String chromLeft, Integer startLeft, Integer endLeft, String chromRight, Integer startRight, Integer endRight, BiFunction<Integer,Integer,Boolean> measureDiscordant) {
		this.line = line;
		this.chromLeft = chromLeft.replace("chr", "");
		this.startLeft = startLeft;
		this.endLeft = endLeft;
		this.chromRight = chromRight.replace("chr", "");
		this.startRight = startRight;
		this.endRight = endRight;
		belowDiscordant = (chromLeft.equals(chromRight)) ? measureDiscordant.apply(startLeft, startRight) : false;
	}
	public void setStatistics(Boolean controlPresent, int supportingPairs) {
		this.controlPresent = controlPresent;
		this.supportingPairs = supportingPairs;
	}

	public String getLine() {
		return String.join("\t", line, controlPresent.toString(), supportingPairs.toString());
	}
	public String getChromLeft() {
		return chromLeft;
	}
	public Integer getStartLeft() {
		return startLeft;
	}
	public Integer getEndLeft() {
		return endLeft;
	}
	public String getChromRight() {
		return chromRight;
	}
	public Integer getStartRight() {
		return startRight;
	}
	public Integer getEndRight() {
		return endRight;
	}
	public Boolean getBelowDiscordant() {
		return belowDiscordant;
	}
	public Boolean isPresent() {
		return controlPresent;
	}
	public Integer getSupportingPairs() {
		return supportingPairs;
	}
}
