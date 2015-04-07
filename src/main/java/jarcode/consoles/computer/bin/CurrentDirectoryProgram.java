package jarcode.consoles.computer.bin;

public class CurrentDirectoryProgram extends FSProvidedProgram {
	@Override
	public void run(String str, Computer computer) throws Exception {
		str = handleEncapsulation(str.trim());
		if (str.isEmpty()) {
			return;
		}
		Terminal terminal = computer.getTerminal(this);
		FSBlock block = computer.getBlock(str, terminal.getCurrentDirectory());
		if (block == null)
			println("cd: " + str + ": No such file or directory");
		else if (block instanceof FSFolder)
			terminal.setCurrentDirectory(str);
		else
			println("cd: " + str + ": No such file or directory");
	}
}
