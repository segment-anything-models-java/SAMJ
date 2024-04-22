package ai.nets.samj.ui;

public class ConsoleSAMJLogger implements SAMJLogger {
	@Override
	public void info(String text) {
		System.out.println("INFO: "+text);
	}

	@Override
	public void warn(String text) {
		System.out.println("WARN: "+text);
	}

	@Override
	public void error(String text) {
		System.out.println("ERROR: "+text);
	}
}
