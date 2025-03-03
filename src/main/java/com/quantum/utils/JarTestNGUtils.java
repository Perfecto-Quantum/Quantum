package com.quantum.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.impl.LogFactoryImpl;

public class JarTestNGUtils {
	
	private static final Log logger = LogFactoryImpl.getLog(JarTestNGUtils.class);

	public static File createTempFolder() {

		File currentDirectory = new File("temp-files");

		if (!currentDirectory.exists()) {
			currentDirectory.mkdir();
		}
		
		currentDirectory.deleteOnExit();

		return currentDirectory;

	}

	public static List<String> moveTestNGFiles(String[] insideTestNGFileNames) throws IOException {

		List<String> outerFilePaths = new ArrayList<String>(insideTestNGFileNames.length);

		File currentTempDirectory = createTempFolder();

		String currentTempDirPath = currentTempDirectory.getAbsolutePath() + File.separator;

		for (String insideFileName : insideTestNGFileNames) {

			InputStream inputStream = TestNGExecutor.class.getClassLoader().getResourceAsStream(insideFileName);

			String[] splittedFileName = insideFileName.split("/");
			String fileName = splittedFileName[splittedFileName.length - 1];

			File tempTestNGFile = new File(currentTempDirPath + fileName);

			Path testNGFilePathOutsideJar = tempTestNGFile.toPath();
			
			long noOfBytes = Files.copy(inputStream,testNGFilePathOutsideJar);

			String outsideTestNGPath = testNGFilePathOutsideJar.toString();
			
			if(noOfBytes > 0) {
				outerFilePaths.add(outsideTestNGPath);
				logger.info("Copied TestNG file outside of jar - " + outsideTestNGPath);
			}else {
				logger.error("Coping TestNG file outside of jar failed - " + outsideTestNGPath);
			}
		}

		return outerFilePaths;

	}

}
