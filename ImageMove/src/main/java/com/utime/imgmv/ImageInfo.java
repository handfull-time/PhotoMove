package com.utime.imgmv;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ImageInfo {
	private File srcFile;
	private File dstFile;
	private Date date = null;
	
	private static SimpleDateFormat fileDateTimeFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");

	private static SimpleDateFormat fileYearFormat = new SimpleDateFormat("yyyy년");
	
	private static SimpleDateFormat fileDateFormat = new SimpleDateFormat("yyyy년MM월");
	
	public ImageInfo(File f, String targetPath) {
		this.srcFile = f;
		
		Date date = MediaUtil.getCreateTime(this.srcFile);
		
		if( date != null ) {
			this.setDate( date, targetPath, MediaUtil.getExName(this.srcFile) );
		}
		
	}
	
	
	private void setDate(Date date, String targetPath, String ext) {
		this.date = date;
		
		final String parent = targetPath + fileYearFormat.format(this.date) + "\\" +  fileDateFormat.format(this.date);
		
		String comment = "";
		final String orgName = MediaUtil.getOriginName( this.srcFile.getName() );
		if( orgName.length() > 9 ) {
			final String cutName = orgName.substring(9);
			if( MediaUtil.isIncludeKorean(cutName) ) {
				comment = "_" + cutName;
			}
		}
		
		final String child = fileDateTimeFormat.format(this.date) + comment + ext;
		
		this.dstFile = new File(parent, child);
	}

	public File getSrcFile() {
		return srcFile;
	}
	

	public File getDstFile() {
		return dstFile;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ImageInfo [");
		if (srcFile != null) {
			builder.append("srcFile=");
			builder.append(srcFile);
			builder.append(", ");
		}
		if (dstFile != null) {
			builder.append("dstFile=");
			builder.append(dstFile);
			builder.append(", ");
		}
		if (date != null) {
			builder.append("date=");
			builder.append(fileDateTimeFormat.format(date));
		}
		builder.append("]\n");
		return builder.toString();
	}
}

