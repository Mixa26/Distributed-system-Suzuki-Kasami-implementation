package cli.command;

import app.AppConfig;
import app.FileType;
import app.MyFile;
import servent.message.MessageType;
import servent.message.TokenMessage;
import servent.message.TokenRequestMessage;
import servent.message.util.MessageUtil;

import java.util.Map;

public class DHTGetCommand implements CLICommand {

	@Override
	public String commandName() {
		return "get_file";
	}

	@Override
	public void execute(String args) {
		try {
			String[] splitArgs = args.split("\\.");
			int key = Integer.parseInt(splitArgs[0]);

			MyFile val = null;

			System.out.println("TRYING TO GET FILE " + args);
			synchronized (AppConfig.chordState.successorLock) {
				System.out.println("GETTING FILE " + args);
				val = AppConfig.chordState.getValue(key);
				if (val != null) {
					val = new MyFile(val);
				}
			}

			if (val == null) {
				AppConfig.timestampedStandardPrint("Please wait...");
			} else if (val.getAccess() == null) {
				AppConfig.timestampedStandardPrint("I haven't been added to the system yet!");
			} else if (val.getFile() == null) {
				AppConfig.timestampedStandardPrint("No such key: " + key);
			} else {
				AppConfig.timestampedStandardPrint(key + ": " + val);
			}

		} catch (NumberFormatException e) {
			AppConfig.timestampedErrorPrint("Invalid argument for get_file: " + args + ". Should be key, which is an int.");
		}
	}

}
