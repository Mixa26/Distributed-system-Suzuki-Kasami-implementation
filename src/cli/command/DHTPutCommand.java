package cli.command;

import app.AppConfig;
import app.ChordState;
import app.FileType;
import app.MyFile;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DHTPutCommand implements CLICommand {

	@Override
	public String commandName() {
		return "add_file";
	}

	@Override
	public void execute(String args) {
		String[] splitArgs = args.split(" ");
		
		if (splitArgs.length == 2) {
			Path filePath = Paths.get(System.getProperty("user.dir") + splitArgs[0]);
			File file = new File(filePath.toAbsolutePath().toUri());

			if (file.exists()) {
				int key = 0;
				MyFile value;
				try {
					key = ChordState.chordHash(Integer.parseInt(splitArgs[0].split(".")[0]));
					if (splitArgs[0].equalsIgnoreCase("public")) {
						value = new MyFile(file, FileType.PUBLIC);
					} else if (splitArgs[0].equalsIgnoreCase("private")) {
						value = new MyFile(file, FileType.PRIVATE);
					} else {
						AppConfig.timestampedErrorPrint("Invalid file sharing option. Setting file to public sharing.");
						value = new MyFile(file, FileType.PUBLIC);
					}

					if (key < 0 || key >= ChordState.CHORD_SIZE) {
						throw new NumberFormatException();
					}

					AppConfig.chordState.putValue(key, value);
				} catch (NumberFormatException e) {
					AppConfig.timestampedErrorPrint("Invalid key and value pair.");
				}
			} else {
				AppConfig.timestampedErrorPrint("File " + filePath.toAbsolutePath() + " doesn't exist!");
			}
		} else {
			AppConfig.timestampedErrorPrint("Invalid arguments for put");
		}

	}

}
