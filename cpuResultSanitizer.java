/*
 * Author: Noor Saif Qasim
 * Author: Shweelan Samson
 * Date: Dec 5, 2017
 */

package nbm.common;
import java.io.*;
import java.util.*;

public class cpuResultSanitizer {

  public static void main(String[] args) throws IOException {
		File inputDir = new File(args[0]);
    String logOutputDir = args[0];
    File[] files = inputDir.listFiles();
    BufferedWriter resultWriter = new BufferedWriter(new FileWriter(logOutputDir + "/final_result_cpu_medians"+ ".csv", true));
    resultWriter.write("TestNumber");
    resultWriter.append(",");
    resultWriter.write("JVMMedian");
    resultWriter.append(",");
    resultWriter.write("WholeSysMedian");
    resultWriter.append("\n");
    for(File file : files) {
			if (file.getName().startsWith("final_result_cpu")) {
				readCpu(resultWriter, inputDir + "/" + file.getName(), file.getName(), logOutputDir);
			}
    }
    resultWriter.flush();
    resultWriter.close();
  }

  public static void readCpu(BufferedWriter resultWriter, String outFilePath, String fileName, String logOutputDir){
    ArrayList<Float> jvmValues= new ArrayList<Float>();
    ArrayList<Float> wholeSysValues= new ArrayList<Float>();
    try {
      BufferedReader fileStream = new BufferedReader(new FileReader(outFilePath));
      String logLine;
      while ((logLine = fileStream.readLine()) != null) {
        if(logLine.trim().startsWith("Cpu(s)")) {
          wholeSysValues.add(Float.valueOf(logLine.substring(logLine.indexOf("s):") + 4 , logLine.indexOf("%us,")).trim()));
        }
				else if (logLine.trim().endsWith("java")) {
					String[] tokens = logLine.trim().replaceAll(" +", " ").split(" ");
          jvmValues.add(Float.valueOf(tokens[8]));
        }
      }
      fileStream.close();
			if (jvmValues.size() == 0 && wholeSysValues.size() == 0) {
				return;
			}
      //jvm output --> .log comma separated
      String jvmFilename = fileName.substring(0, fileName.indexOf(".log"));
      jvmFilename = jvmFilename+"_jvm"+ ".log";
      logWriter(jvmFilename, jvmValues, logOutputDir);
      //whole sys --> .log comma separated
      String wholeListFN = fileName.substring(0, fileName.indexOf(".log"));
      wholeListFN = wholeListFN+"_whole_sys" + ".log";
      logWriter(wholeListFN, wholeSysValues, logOutputDir);
			//get median of jvm values
      Float jvmMedian = getMedian(jvmValues);
      //get median of whole sys
      Float wholeMedian = getMedian(wholeSysValues);
      //CSV
			String[] split = fileName.split("_");
      resultWriter.append(split[split.length - 1].replace(".log", ""));
      resultWriter.append(",");
      resultWriter.append(String.valueOf(jvmMedian));
      resultWriter.append(",");
      resultWriter.append(String.valueOf(wholeMedian));
      resultWriter.append("\n");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static Float getMedian (ArrayList<Float> floatArrayList){
		if (floatArrayList.size() == 0) {
			return new Float(0.0);
		}
    Collections.sort(floatArrayList);
    int mid = floatArrayList.size() / 2;
    Float median = floatArrayList.get(mid);
    return median;
  }

  public static void logWriter (String fileNameLoc, ArrayList<Float> filters, String logOutputDir){
    try {
      File wholeFile = new File(logOutputDir + "/" + fileNameLoc);
      BufferedWriter brwhole = new BufferedWriter(new FileWriter(wholeFile));
      for(Float value : filters) {
        brwhole.write(String.valueOf(value));
				brwhole.newLine();
      }
      brwhole.flush();
      brwhole.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
