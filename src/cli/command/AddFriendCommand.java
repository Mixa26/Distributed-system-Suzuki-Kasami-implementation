package cli.command;

import app.AppConfig;
import app.ServentInfo;
import servent.message.AddFriendMessage;
import servent.message.util.MessageUtil;

public class AddFriendCommand implements CLICommand{
    @Override
    public String commandName() {
        return "add_friend";
    }

    @Override
    public void execute(String args) {
        try {
            int port = Integer.parseInt(args);

            boolean found = false;

            for (ServentInfo servent : AppConfig.getServentInfoList()) {
                if (servent.getListenerPort() == port){
                    found = true;
                }
            }

            if (!found){
                AppConfig.timestampedStandardPrint("No such port: " + port);
            }

            System.out.println("SENDING FRIEND MESSAGE TO " + port);
            AddFriendMessage addFriendMessage = new AddFriendMessage(AppConfig.myServentInfo.getListenerPort(), port, false);
            MessageUtil.sendMessage(addFriendMessage);

        } catch (NumberFormatException e) {
            AppConfig.timestampedErrorPrint("Invalid argument for add_friend: " + args + ". Should be port, which is an int.");
        }
    }
}
