package com.utime.imgmv;


import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.coremedia.iso.IsoFile;
import com.coremedia.iso.boxes.MovieBox;
import com.coremedia.iso.boxes.MovieHeaderBox;
import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.imaging.jpeg.JpegMetadataReader;
import com.drew.imaging.jpeg.JpegProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.mov.QuickTimeDirectory;
import com.drew.metadata.mov.media.QuickTimeVideoDirectory;
import com.drew.metadata.mp4.Mp4Directory;
import com.drew.metadata.mp4.media.Mp4VideoDirectory;

public class MediaUtil {
	
	private static Pattern patternKorean = Pattern.compile("[가-힣]");
	
	private static final List<SimpleDateFormat> dateFormatList = new ArrayList<>();
	
	private static final Set<String> rawFileSet = new HashSet<>();

	private static void _listAdd(List<SimpleDateFormat> list, String format) {
		final SimpleDateFormat dateFormat = new SimpleDateFormat(format);
		dateFormat.setLenient(false);
		list.add(dateFormat);
	}
	
	private static final int MaxDateFormatNameLen;

	static {
		_listAdd(dateFormatList, "yyyyMMddHHmmss");
		_listAdd(dateFormatList, "yyyyMMdd");
		_listAdd(dateFormatList, "HHmmssyyyyMMdd");
		_listAdd(dateFormatList, "yyMMddHHmmss");
		_listAdd(dateFormatList, "yyMMdd");
		_listAdd(dateFormatList, "yyyyMMddHHmm");
		
		int len = 999;
		for( SimpleDateFormat df : dateFormatList ) {
			if( len > df.toPattern().length() ) {
				len = df.toPattern().length();
			}
		}
		
		MaxDateFormatNameLen = len;
		
		rawFileSet.add(".crw");
		rawFileSet.add(".cr2");
		rawFileSet.add(".cr3");
		rawFileSet.add(".arw");
		rawFileSet.add(".srf");
		rawFileSet.add(".sr2");
		rawFileSet.add(".nef");
		rawFileSet.add(".nrw");
	}

	private static final java.util.Calendar cal = java.util.Calendar.getInstance(Locale.KOREA);
	private static Date after, before;
	
	public static boolean isWithinRange() {
        // Start date is set to 1995-01-01
        Calendar startDate = Calendar.getInstance();
        startDate.set(1995, Calendar.JANUARY, 1, 0, 0, 0);
        startDate.set(Calendar.MILLISECOND, 0);
        
        // End date is 10 days after today
        Calendar endDate = Calendar.getInstance();
        endDate.add(Calendar.DATE, 10);
        
        // Current date for comparison
        Date currentDate = new Date();
        
        // Check if current date is after start date and before end date
        return currentDate.after(startDate.getTime()) && currentDate.before(endDate.getTime());
    }
	
	static {
		MediaUtil.before = cal.getTime();
		cal.set(1996, java.util.Calendar.JANUARY, 1);
		MediaUtil.after = cal.getTime();
	}
	
	public static boolean isIncludeKorean( String s) {
		final Matcher matcher = MediaUtil.patternKorean.matcher(s);
		return matcher.find();
	}
	
	/**
	 * 사용 가능 날짜 인가?
	 * @param dt
	 * @return
	 */
	private static boolean isPossibleDate(Date dt) {
		if( dt == null )
			return false;
		
		return dt.before(MediaUtil.before) && dt.after(MediaUtil.after);
	}
	
	private static final Date getDateForDirectoryOfValue( Directory dir, int tagType ) {
		if( dir == null || tagType < 0 )
			return null;
		
		if( ! dir.containsTag(tagType) )
			return null;
		
		final Date res = dir.getDate(tagType);
		
		return MediaUtil.isPossibleDate(res)? res : null;
	}

	private static Date getMovieCreateTime( File srcFile ) {
		// 생성 날짜 얻기
		Date result = null;
		
		final Metadata metadata;
		try {
			metadata = ImageMetadataReader.readMetadata(srcFile);
		} catch (ImageProcessingException | IOException e) {
			System.err.println( srcFile.getName() + " - " + e.getMessage() );
			return null;
		}
		
		if( metadata.containsDirectoryOfType(QuickTimeDirectory.class) ){
			
			final List<QuickTimeDirectory> quickTimeList = (List<QuickTimeDirectory>) metadata.getDirectoriesOfType(QuickTimeDirectory.class);
			for( QuickTimeDirectory quickTime : quickTimeList ) {
				if( result == null ) {
					result = MediaUtil.getDateForDirectoryOfValue(quickTime, QuickTimeDirectory.TAG_CREATION_TIME );
					break;
				}
			}
			
		}

		if( metadata.containsDirectoryOfType(QuickTimeVideoDirectory.class) ){
			final List<QuickTimeVideoDirectory> quickTimeVideoList = (List<QuickTimeVideoDirectory>) metadata.getDirectoriesOfType(QuickTimeVideoDirectory.class);
			for( QuickTimeVideoDirectory quickTimeVideo : quickTimeVideoList ) {
				if( result == null ) {
					result = MediaUtil.getDateForDirectoryOfValue(quickTimeVideo, QuickTimeVideoDirectory.TAG_CREATION_TIME );
					break;
				}
			}
		}
		
		if( metadata.containsDirectoryOfType(Mp4Directory.class) ){
			final List<Mp4Directory> mp4List = (List<Mp4Directory>) metadata.getDirectoriesOfType(Mp4Directory.class);
			for( Mp4Directory mp4 : mp4List ) {
				if( result == null ) {
					result = MediaUtil.getDateForDirectoryOfValue(mp4, Mp4Directory.TAG_CREATION_TIME );
					break;
				}
			}
		}
		
		if( metadata.containsDirectoryOfType(Mp4VideoDirectory.class) ){
			final List<Mp4VideoDirectory> mp4VideoList = (List<Mp4VideoDirectory>) metadata.getDirectoriesOfType(Mp4VideoDirectory.class);
			for( Mp4VideoDirectory mp4Video : mp4VideoList ) {
				if( result == null ) {
					result = MediaUtil.getDateForDirectoryOfValue(mp4Video, Mp4VideoDirectory.TAG_CREATION_TIME );
					break;
				}
			}
		}
	
		return result;
	}
	
	private static Date getImageCreateTime( File srcFile ) {
		Date result = null;
		
		final Metadata metadata;
		try {
			metadata = ImageMetadataReader.readMetadata(srcFile);
		} catch (ImageProcessingException | IOException e) {
			System.err.println( srcFile.getName() + " - " + e.getMessage() );
			return result;
		}

		if( metadata.containsDirectoryOfType(ExifIFD0Directory.class) ){
			final Collection<ExifIFD0Directory> exifIFD0List = metadata.getDirectoriesOfType(ExifIFD0Directory.class);
			for( ExifIFD0Directory exifIFD0 : exifIFD0List ) {

				if( result == null ) {
					result = MediaUtil.getDateForDirectoryOfValue(exifIFD0, ExifIFD0Directory.TAG_DATETIME );
					if( result != null )
						break;
				}
				
				if( result == null ) {
					result = MediaUtil.getDateForDirectoryOfValue(exifIFD0, ExifIFD0Directory.TAG_DATETIME_DIGITIZED );
					if( result != null )
						break;
				}
				
				if( result == null ) {
					result = MediaUtil.getDateForDirectoryOfValue(exifIFD0, ExifIFD0Directory.TAG_DATETIME_ORIGINAL );
					if( result != null )
						break;
				}
			}
		}
		
		// SubIFD 에서 정보 얻기
		if( result == null && metadata.containsDirectoryOfType(ExifSubIFDDirectory.class) ){
			final Collection<ExifSubIFDDirectory> exifSubIFDList = metadata.getDirectoriesOfType(ExifSubIFDDirectory.class);
			for( ExifSubIFDDirectory exifSubIFD : exifSubIFDList ) {
				if( result == null ) {
					result = MediaUtil.getDateForDirectoryOfValue(exifSubIFD, ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL );
					if( result != null )
						break;
				}
				
				if( result == null ) {
					result = MediaUtil.getDateForDirectoryOfValue(exifSubIFD, ExifSubIFDDirectory.TAG_DATETIME );
					if( result != null )
						break;
				}
				
				if( result == null ) {
					result = MediaUtil.getDateForDirectoryOfValue(exifSubIFD, ExifSubIFDDirectory.TAG_DATETIME_DIGITIZED );
					if( result != null )
						break;
				}
			}
		}
		
		return result;
	}
	
	public static final String getExName( String fileName ) {
		final int index = fileName.lastIndexOf(".");
		if( index > -1 && index < fileName.length() ) {
			return fileName.substring( index, fileName.length()).toLowerCase();
		} else {
			return fileName.substring( fileName.length()-3).toLowerCase();
		}
	}
	
	public static final String getOriginName( String fileName ) {
		
		final int index = fileName.lastIndexOf(".");
		if( index > -1 ) {
			return fileName.substring(0, index);
		}else {
			return fileName.substring(0, fileName.length()-4).toLowerCase();
		}
	}
	
	public static final String getExName( File file ) {
		return MediaUtil.getExName(file.getName());
	}

	/**
	 * 시간이 0 일 경우 파일 기반 시간으로 세팅함.
	 * @param srcFile
	 * @param date
	 * @return
	 */
	public static final Date checkTime( File srcFile, Date date ) {
		
		Date result = null;
		if( date == null ) {
			
			result = MediaUtil.getDateFormFileAttributes( srcFile );
			
			return MediaUtil.isPossibleDate(result)? result:null;
		}

		LocalDateTime localDateTime = date.toInstant()
	            .atZone(ZoneId.systemDefault())
	            .toLocalDateTime();
		
	    // 시, 분, 초만 추출
		LocalTime time = localDateTime.toLocalTime();
		if (time.getHour() == 0 && time.getMinute() == 0 && time.getSecond() == 0) {
			Date fileDate = MediaUtil.getDateFormFileAttributes( srcFile );
			
			localDateTime = fileDate.toInstant()
		            .atZone(ZoneId.systemDefault())
		            .toLocalDateTime();
			
			time = localDateTime.toLocalTime();
			
			Calendar calendar = Calendar.getInstance();
	        calendar.setTime(date);

	        // 시, 분, 초만 변경
	        calendar.set(Calendar.HOUR_OF_DAY, time.getHour());
	        calendar.set(Calendar.MINUTE, time.getMinute());
	        calendar.set(Calendar.SECOND, time.getSecond());

	        result = calendar.getTime();
			
	    } else {
	    	result = date;
	    }
		
		return MediaUtil.isPossibleDate(result)? result:null;
	}

	public static Date getCreateTime( File srcFile ) {
		
		Date result = null;
		
		final String name = srcFile.getName();
		
		final String numFileName = MediaUtil.getOriginName( name ).replaceAll("[^0-9]","");

		// 파일 이름으로부터 날짜 정보를 얻는다.
		final int numFileNameLen = numFileName.length();
		if( MaxDateFormatNameLen <= numFileNameLen ) {
			
			for(SimpleDateFormat dateFormat : MediaUtil.dateFormatList) {
				final int len = dateFormat.toPattern().length();
				if( numFileNameLen < len ) {
					continue;
				}
				
				final String fname = numFileName.substring(0, len);
				try {
		        	result = dateFormat.parse( fname );
					if( ! MediaUtil.isPossibleDate(result) ) {
						result = null;
					}
				} catch (Exception e) {
					result = null;
				}
				
				if( result != null ) {
					break;
				}
			}
		}
		
		if( result != null) {
			return MediaUtil.checkTime(srcFile, result);
		}

		final String exName = MediaUtil.getExName(name);
		
		// EXIF로 부터 파일 정보를 읽는다.
		if( ".jpeg".equals(exName) || ".jpg".equals(exName) ){
			// 사진이다.
			try {
				final Metadata metadata = JpegMetadataReader.readMetadata(srcFile);
				if( metadata != null ) {
					final ExifSubIFDDirectory directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
					if( directory != null ) {
						result = directory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
					}
				}
			} catch (JpegProcessingException | IOException e) {
				System.err.println( name + " - " + e.getMessage() );
				result = null;
			}
			
			if( result == null ) {
				result = MediaUtil.getImageCreateTime(srcFile);
			}
			
		} else if( rawFileSet.contains(exName) ){
			// 이미지 생성 날짜 얻기
			result = MediaUtil.getImageCreateTime(srcFile);
			
		} else if( ".mov".equals(exName) || ".mp4".equals(exName) ){
			IsoFile iso = null; 
			try {
				iso = new IsoFile(srcFile.getPath());
				final MovieBox mBox = iso.getMovieBox();
				final MovieHeaderBox mHeaderBox = mBox.getMovieHeaderBox();
				
				result = mHeaderBox.getCreationTime();
				mBox.close();
				
			} catch (IOException e) {
				System.err.println( name + " - " + e.getMessage() );
				result = null;
			}finally {
				if( iso != null )
					try {
						iso.close();
					} catch (IOException e) {
						System.err.println( name + " - " + e.getMessage() );
					}
			}
			
			if( result == null ) {
				result = MediaUtil.getMovieCreateTime( srcFile );
			}
		}
		
		return MediaUtil.checkTime(srcFile, result);
	}
	
	/**
	 * 파일 속성으로부터 날짜를 얻는다.
	 * @param file
	 * @return
	 */
	private static final Date getDateFormFileAttributes(File file) {
		
		Date result = null;
		
		if( file.exists() )
			try {
				final BasicFileAttributes attrs = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
				
				
				// 파일의 생성 시간, 마지막 수정 시간, 마지막 액세스 시간을 가져옵니다.
	            final FileTime creationTime = attrs.creationTime();
	            final FileTime lastModifiedTime = attrs.lastModifiedTime();
	            final FileTime lastAccessTime = attrs.lastAccessTime();

	            // 가장 오래된 날짜를 찾습니다.
	            FileTime oldest = creationTime;
	            if (lastModifiedTime.compareTo(oldest) < 0) {
	                oldest = lastModifiedTime;
	            }
	            if (lastAccessTime.compareTo(oldest) < 0) {
	                oldest = lastAccessTime;
	            }
				
				
				result = new Date( oldest.toMillis() );
			} catch (IOException e) {
				e.printStackTrace();
				result = null;
			}
		
		return result;
	}
	
	
//	/**
//	 * 둘다 같은 파일인지 검사.
//	 * @param source
//	 * @param target
//	 * @return
//	 * @throws IOException
//	 */
//	public final static boolean isSameFile(Path source, Path target) throws Exception{
//		final String srcChecksum = MediaUtil.getHash(source);
//		
//		final String trgChecksum = MediaUtil.getHash(target);
//		
//		return srcChecksum.equals(trgChecksum);
//	}
	
//	/**
//	 * Hash 값 얻기
//	 * @param path
//	 * @return
//	 * @throws IOException
//	 */
//	public final static String getHash(Path path) throws IOException{
//		final InputStream pathIs = Files.newInputStream(path);
//		final String result = DigestUtils.md5Hex(pathIs);
//		pathIs.close();
//		
//		return result;
//	}

//	public static String getHash(Path path) throws Exception {
//        MessageDigest md = MessageDigest.getInstance("SHA-256");
//        try (DigestInputStream dis = new DigestInputStream(new FileInputStream(path.toFile()), md)) {
//            byte[] buffer = new byte[1024*1024];
//            while (dis.read(buffer) != -1);
//            md = dis.getMessageDigest();
//        }
//
//        StringBuilder result = new StringBuilder();
//        for (byte b : md.digest()) {
//            result.append(String.format("%02x", b));
//        }
//        return result.toString();
//    }

	
	private static final int SAMPLE_SIZE = 64; // 샘플링할 크기
	
	private static boolean isSameFile(Path source, Path target, int count) throws Exception {
        // 파일 크기 비교
    	final File sourceFile = source.toFile(), targetFile = target.toFile();
    	
        if (sourceFile.length() != targetFile.length()) {
            return false;
        }

        boolean result = true;
        final byte[] buffer1 = new byte[SAMPLE_SIZE];
        final byte[] buffer2 = new byte[SAMPLE_SIZE];
        try (RandomAccessFile raf1 = new RandomAccessFile(sourceFile, "r");
             RandomAccessFile raf2 = new RandomAccessFile(targetFile, "r")) {
            
            
            final MessageDigest mdSource = MessageDigest.getInstance("SHA-256");
            final MessageDigest mdTarget = MessageDigest.getInstance("SHA-256");
            
//            long numberOfSamples = sourceFile.length() / SAMPLE_SIZE;
            for (int i = 0; i < count ; i++) {
            	final long position = (long) (Math.random() * sourceFile.length());
                
                raf1.seek(position);
                raf1.read(buffer1);
                mdSource.update(buffer1);

                raf2.seek(position);
                raf2.read(buffer2);
                mdTarget.update(buffer2);
                
                if( ! java.util.Arrays.equals(mdSource.digest(), mdTarget.digest()) ) {
                	result = false;
                	break;
                };
            }
            
            raf1.close();
            raf2.close();
        }
        return result;
		
	}

    public static boolean isSameFile(Path source, Path target) throws Exception {
    	return MediaUtil.isSameFile(source, target, 1);
    }

    public static boolean isSimpleSameFile(Path source, Path target) throws Exception {
    	return MediaUtil.isSameFile(source, target, 0);
    }

}
