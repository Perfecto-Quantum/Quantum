package com.quantum.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import org.testng.TestNG;

public class TestNGExecutor {


	public static void main(String[] suiteFiles) {
		
		if(suiteFiles.length==0) {
			System.out.println("No TestNG File provided!!");
			return;
		}
		
		String[] tempTestNGFilesPath = new String[suiteFiles.length];
		
		try {
			
			String tempFileName;
			
			File currentDirectory = new File("temp-files");
			
			if(!currentDirectory.exists()) {
				currentDirectory.mkdir();
			}
			
			for(int index=0;index<suiteFiles.length;++index) {
				
				tempFileName = suiteFiles[index];
				
				File tempTestNGFile = new File(currentDirectory.getAbsolutePath() + File.separator +  tempFileName);

				tempTestNGFilesPath[index] = tempTestNGFile.getPath();
				
				InputStream inputStream = TestNGExecutor.class.getClassLoader().getResourceAsStream(tempFileName);
				
				System.out.println(inputStream);
				
				System.out.println(Files.copy(inputStream, tempTestNGFile.toPath()));
				
//				try(BufferedWriter writer = new BufferedWriter(new FileWriter(tempTestNGFile))){
//					writer.write(tempFileName);
//				}
				
//				tempTestNGFile.deleteOnExit();
			}
			
			System.out.println("Im here!!");
			
			List<String> testSuites = Arrays.asList(suiteFiles);
			
			TestNG testNG = new TestNG();
			testNG.setTestSuites(Arrays.asList(tempTestNGFilesPath));
			testNG.run();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally {
			
		}
	}

}
