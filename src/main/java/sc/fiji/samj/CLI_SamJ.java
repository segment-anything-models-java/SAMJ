package sc.fiji.samj;

import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import picocli.CommandLine;
import java.io.File;

public class CLI_SamJ {

	@CommandLine.Option(names = {"-j","--job","jobFile"}, description = "Path to a job file, the variant with weights.")
	private File filePath;

	@CommandLine.Option(names = {"-t","--tps","timepoints"}, description = "Timepoints to be processed (e.g. 1-9,23,25).")
	private String fileIdxStr = "0-9";

	@CommandLine.Option(names = {"-o","--out","outputFiles"}, description = "Output filename pattern, don't forget to include TTT or TTTT into the filename.")
	private File outputPath = new File("CHANGE THIS PATH/mergedTTT.tif");

	@CommandLine.Option(names = {"-n","--threads"}, description = "Level of parallelism as the no. of threads.")
	int noOfThreads = 1;

	@CommandLine.Option(names = {"-g","--seg","segGtFolder"}, description = "Provide a valid path to the SEG folder for evaluations.")
	String SEGfolder = "CHANGE THIS PATH/dataset/video_GT/SEG";

	@CommandLine.Option(names = {"-s","--save","saveFusionResults"}, description = "Use the output format parameter and do create the output files.")
	boolean saveFusionResults = false;


	private <IT extends RealType<IT>, LT extends IntegerType<LT>>
	void worker()
	{
		System.out.println("hello from the worker...");
	}


	// ================================ CLI ==========================
	@CommandLine.Option(names = {"-h", "--help"}, usageHelp = true, description = "Display this help message.")
	boolean usageHelpRequested;

	private void printHelp() {
		System.out.println("Some explicit help...");
		System.out.println(); //intentional line separator
		CommandLine.usage(this, System.out);
	}

	public static void main(String[] args) {
		CLI_SamJ samj;

		//parse the command line and fill the object's attributes
		try { samj = CommandLine.populateCommand(new CLI_SamJ(), args); }
		catch (CommandLine.ParameterException pe) {
			System.out.println(pe.getMessage());
			System.out.println(); //intentional line separator
			new CLI_SamJ().printHelp();
			return;
		}

		//parsing went well:
		//no params given or an explicit cry for help?
		if (args.length == 0 || samj.usageHelpRequested) {
			samj.printHelp();
			return;
		}

		samj.worker();
	}
}