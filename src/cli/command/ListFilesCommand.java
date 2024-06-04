package cli.command;


import app.AppConfig;
import servent.message.ListFilesMessage;
import servent.message.Message;
import servent.message.util.MessageUtil;

public class ListFilesCommand implements CLICommand {

    @Override
    public String commandName() {
        return "view_files";
    }

    @Override
    public void execute(String args) {
        try {
            int port = Integer.parseInt(args);
            System.out.println("ASKING FOR LIST OF ALL FILES FROM " + port);
            Message listFilesMessage = new ListFilesMessage(AppConfig.myServentInfo.getListenerPort(), port, null);
            MessageUtil.sendMessage(listFilesMessage);
        } catch (NumberFormatException e){
            AppConfig.timestampedErrorPrint("Wrong format of port provided " + args + " in view_files command.");
        }
    }
}
