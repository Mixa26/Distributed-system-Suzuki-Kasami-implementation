package cli.command;

import app.AppConfig;
import app.MyFile;
import servent.message.Message;

public class RemoveFileCommand implements CLICommand{

    @Override
    public String commandName() {
        return "remove_file";
    }

    @Override
    public void execute(String args) {
        synchronized (AppConfig.chordState.successorLock) {
            boolean found = false;
            MyFile fileForDeletion = null;

            for (MyFile file : AppConfig.chordState.myFilesInSystem) {
                System.out.println("$$$$$$$$$$$$$" + file.getFile().getAbsolutePath());
                if (file.getFile().getAbsolutePath().equals(AppConfig. ROOT_PATH + "\\" + args)){
                    found = true;
                    fileForDeletion = file;
                    break;
                }
            }

            if (!found){
                AppConfig.timestampedStandardPrint("No file with provided name " + args + " found, aborting removal.");
            }

            AppConfig.chordState.myFilesInSystem.remove(fileForDeletion);

            String[] argsSplit = args.split("\\.");
            try {
                int key = Integer.parseInt(argsSplit[0]);
                AppConfig.chordState.removeValue(key);
            } catch (NumberFormatException e){
                AppConfig.timestampedErrorPrint("Key for removal is not of type int, make sure you're file name is a number (exp. 25.txt)!");
            }
        }
    }
}
