package ai.nets.samj.ui;

public class NoOutputSAMJLogger implements SAMJLogger {
	@Override
	public void info(String text) {  /* intentionally empty */ }
	@Override
	public void warn(String text) {  /* intentionally empty */ }
	@Override
	public void error(String text) {  /* intentionally empty */ }
}
