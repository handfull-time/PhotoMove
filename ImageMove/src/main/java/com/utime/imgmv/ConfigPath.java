package com.utime.imgmv;

import java.util.List;

/**
 * 환경 설정
 * @author Spring
 *
 */
public class ConfigPath {
	/** 알 수 없는 파일 저장 폴더 */
	private String unknown;
	/** 분류 결과 폴더 */
	private String dest;
	/** 분류 대상 폴더 */
	private List<String> sourceList;
	
	/**
	 * 사진을 복사 시킬 것인가?
	 */
	private boolean fileCopy = true;
	
	public String getUnknown() {
		return unknown;
	}
	public void setUnknown(String unknown) {
		this.unknown = unknown;
	}
	public String getDest() {
		return dest;
	}
	public void setDest(String dest) {
		this.dest = dest;
	}
	public List<String> getSourceList() {
		return sourceList;
	}
	public void setSourceList(List<String> sourceList) {
		this.sourceList = sourceList;
	}
	public boolean isFileCopy() {
		return fileCopy;
	}
	public void setFileCopy(boolean fileCopy) {
		this.fileCopy = fileCopy;
	}
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ConfigPath [");
		if (unknown != null)
			builder.append("unknown=").append(unknown).append(", ");
		if (dest != null)
			builder.append("dest=").append(dest).append(", ");
		if (sourceList != null)
			builder.append("sourceList=").append(sourceList);
		builder.append("]");
		return builder.toString();
	}
	
}
