package com.utime.imgmv;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ImageMoveMainApplication {
	
	private static Logger log = LogManager.getLogger(ImageMoveMainApplication.class);

	public static void main(String[] args) {
		
		final String confName = (args.length < 1 )? "config.json":args[0];
		
		final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
		final File configFile = new File( confName );
		if( ! configFile.exists() ) {
			final ConfigPath conf = new ConfigPath();
			conf.setDest("C:\\Dest\\");
			conf.setUnknown("C:\\Unknown\\");
			
			final List<String> lst = new ArrayList<>();
			lst.add("C:\\User\\Photo");
			lst.add("C:\\Temp\\Other");
			lst.add("D:\\PtotoData");
			
			conf.setSourceList(lst);
			
			System.out.println("\n\n------------------------------------------------------\n");
			System.out.println("아래와 같은 파일을 생성해 주세요.");
			System.out.println("파일 이름 : config.json");
			System.out.println("");
			System.out.println("파일 내용");
			System.out.println( gson.toJson(conf) );
			System.out.println("\n------------------------------------------------------\n");
			return;
		}
		
		final ConfigPath conf;
		try {
			FileReader reader = new FileReader(configFile);
			conf = gson.fromJson(reader, ConfigPath.class);
			reader.close();
		} catch (FileNotFoundException e ) {
			e.printStackTrace();
			return;
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		String destPath;
		{
			destPath = conf.getDest();
			if( destPath.charAt( destPath.length()-1 ) != '\\') {
				destPath += "\\";
			}
			final File dest = new File( destPath );
			if( ! dest.exists() ) {
				dest.mkdirs();
			}
			
		}
		
		String unknownPath;
		{
			unknownPath = conf.getUnknown();
			if( unknownPath.charAt( unknownPath.length()-1 ) != '\\') {
				unknownPath += "\\";
			}
			final File unknown = new File( unknownPath );
			if( ! unknown.exists() ) {
				unknown.mkdirs();
			}
		}
		
		for( String srcPath : conf.getSourceList() ) {
			
			final File srcFile = new File(srcPath);
			
			if( ! Files.exists(srcFile.toPath(), LinkOption.NOFOLLOW_LINKS) ) {
				continue;
			}
			
			try {
				ImageMoveMainApplication.getFileSubList( srcFile, destPath, unknownPath);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		
		System.out.println("End");

	}
	
	private static void getFileSubList( File srcFilePath, String destPath, String unknownPath ) throws Exception{

		
		final File[] srcFileArray = srcFilePath.listFiles( new FileFilter() {
			
			public boolean accept(File pathname) {
				
				if( pathname.isDirectory() ){
					return true;
				}
				
				final String exName = MediaUtil.getExName(pathname);
				
				return (exName.indexOf("gif")>0) 
						|| (exName.indexOf("jpg")>0)  
						|| (exName.indexOf("jpeg")>0)  
						|| (exName.indexOf("mp4")>0)
						|| (exName.indexOf("mov")>0)
						|| (exName.indexOf("crw")>0) || (exName.indexOf("cr2")>0) || (exName.indexOf("cr3")>0)
						|| (exName.indexOf("arw")>0) || (exName.indexOf("srf")>0) || (exName.indexOf("sr2")>0)
						|| (exName.indexOf("nef")>0) || (exName.indexOf("nrw")>0);
			}
		} );
		
		for(File file : srcFileArray ){

			if(file.isFile()){
				
				final ImageInfo info = new ImageInfo(file, destPath);
				
				if( info.getDstFile() == null ) {
					final File unFile =  new File( unknownPath, info.getSrcFile().getName());
					ImageMoveMainApplication.moveWithUniqueName(info.getSrcFile().toPath(), unFile.toPath());
					continue;
				}
				
				if( ! ImageMoveMainApplication.moveWithUniqueName(info.getSrcFile().toPath(), info.getDstFile().toPath()) ) {
//					final File unFile =  new File( unknownPath,"Exists_"+ info.getSrcFile().getName());
//					ImageMoveMain.moveWithUniqueName(info.getSrcFile().toPath(), unFile.toPath());
				};

			}else if(file.isDirectory()){

				getFileSubList(file, destPath, unknownPath); 

			}

		}
	}

    private static boolean moveWithUniqueName(Path source, Path target) throws Exception {
        if (!Files.exists(source)) {
            throw new IOException("Source file does not exist: " + source);
        }

        // Check if target file already exists.
        if (Files.exists(target)) {
        	
        	if( MediaUtil.isSameFile(source, target) ) {
//        		log.info( "E," + source.toString() + "," + target.toString());
        		return false;
        	}
        	
            int counter = 1;
            while (true) {
                Path newTarget = target.resolveSibling(ImageMoveMainApplication.getNewFilename(target.getFileName().toString(), counter));
                if (!Files.exists(newTarget)) {
                	ImageMoveMainApplication.move(source, newTarget);
                    return true;
                }else {
                	if( MediaUtil.isSameFile(source, newTarget) ) {
//                		log.info( "E," + source.toString() + "," + newTarget.toString());
                		return false;
                	}
                }
                counter++;
            }
        } else {
        	ImageMoveMainApplication.move(source, target);
        }
        return true;
    }
    
    private static void move(Path source, Path target) throws Exception {
    	if( ! Files.exists(target.getParent()) ){
    		final File f = new File( target.getParent().toUri() );
    		System.out.println(f + " 폴더 생성 - " + f.mkdirs());
    		
    		if( ! Files.exists(target.getParent()) ){
    			System.err.println(target.getParent() + " 폴더 생성 실패");
    			throw new IOException(target.getParent() + " 폴더 생성 실패");
    		}
    	}
    	
    	final Path res = Files.move(source, target);
    	if( Files.exists(res) ) {
    		log.info( "T," + source.toString() + "," + target.toString());
		}else {
			log.info( "F," + source.toString() + "," + target.toString());
		}
//    	final Path res = Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
//    	if( Files.exists(res) ) {
//    		
//    		if( MediaUtil.isSimpleSameFile(source, target) ) {
//    			Files.deleteIfExists(source);
//    			log.info( "T," + source.toString() + "," + target.toString());
//    		}else {
//    			log.info( "F," + source.toString() + "," + target.toString());
//    		}
//    	}
    	
    	
    }

    private static String getNewFilename(String originalName, int counter) {
        int dotIndex = originalName.lastIndexOf(".");
        if (dotIndex == -1) {
            return originalName + "(" + counter + ")";
        } else {
            String namePart = originalName.substring(0, dotIndex);
            String extensionPart = originalName.substring(dotIndex);
            return namePart + "(" + counter + ")" + extensionPart;
        }
    }

}
