package cli.command;

import app.AppConfig;
import app.MyFile;

public class DHTGetCommand implements CLICommand {

	@Override
	public String commandName() {
		return "dht_get";
	}

	@Override
	public void execute(String args) {
		try {
			int key = Integer.parseInt(args);
			
			MyFile val = AppConfig.chordState.getValue(key);
			
			if (val == null) {
				AppConfig.timestampedStandardPrint("Please wait...");
			} else if (val.getFile() == null) {
				AppConfig.timestampedStandardPrint("No such key: " + key);
			} else {
				AppConfig.timestampedStandardPrint(key + ": " + val);
			}
		} catch (NumberFormatException e) {
			AppConfig.timestampedErrorPrint("Invalid argument for dht_get: " + args + ". Should be key, which is an int.");
		}
	}

}
