package cli.command;


import app.AppConfig;
import servent.message.ListFilesMessage;

public class ListFilesCommand implements CLICommand {

    @Override
    public String commandName() {
        return "view_files";
    }

    @Override
    public void execute(String args) {
        try {
            int port = Integer.parseInt(args);
            new ListFilesMessage(AppConfig.myServentInfo.getListenerPort(), port, null);
        } catch (NumberFormatException e){
            AppConfig.timestampedErrorPrint("Wrong format of port provided " + args + " in view_files command.");
        }
    }
}
