package cli.command;

import app.AppConfig;
import app.ChordState;
import app.FileType;
import app.MyFile;
import servent.message.MessageType;
import servent.message.TokenMessage;
import servent.message.TokenRequestMessage;
import servent.message.util.MessageUtil;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

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

					synchronized (AppConfig.chordState.chordSync) {
						// Request token for critical section
						int serventNum = AppConfig.myServentInfo.getId();
						synchronized (AppConfig.chordState.tokenRequestsLock) {
							AppConfig.chordState.tokenRequests.put(serventNum, AppConfig.chordState.tokenRequests.get(serventNum) + 1);
							if ((AppConfig.chordState.token == null)) {
								for (int i = 0; i < AppConfig.SERVENT_COUNT; i++) {
									if (i == serventNum) continue;
									System.out.println("FROM DHTPUT SENDING TOKEN REQUEST TO " + AppConfig.getInfoById(i).getListenerPort() + " NUMBER " + AppConfig.chordState.tokenRequests.get(serventNum) + " AND I AM ID " + serventNum);
									TokenRequestMessage tokenRequestMessage = new TokenRequestMessage(MessageType.TOKEN_REQUEST, AppConfig.myServentInfo.getListenerPort(), AppConfig.getInfoById(i).getListenerPort(), serventNum, AppConfig.chordState.tokenRequests.get(serventNum));
									MessageUtil.sendMessage(tokenRequestMessage);
								}
							}
						}
						// Wait for the token
						System.out.println("FROM DHTPUT GETTING TOKEN");
						synchronized (AppConfig.chordState.tokenRequestsLock) {
							while (AppConfig.chordState.token == null) {
								try {
									System.out.println("FROM DHTPUT WAITING FOR TOKEN");
									AppConfig.chordState.tokenRequestsLock.wait();
								} catch (InterruptedException e) {
									throw new RuntimeException(e);
								}
							}
						}
						// Token received!
						System.out.println("FROM DHTPUT GOT TOKEN");

						// Critical section
						AppConfig.chordState.putValue(key, value);

						synchronized (AppConfig.chordState.tokenRequestsLock) {
							AppConfig.chordState.token.tokenRequests.put(serventNum, AppConfig.chordState.tokenRequests.get(serventNum));
							// Append requests in order
							for (Map.Entry<Integer, Integer> tokenRequest : AppConfig.chordState.tokenRequests.entrySet()) {
								if (tokenRequest.getValue() == AppConfig.chordState.token.tokenRequests.get(tokenRequest.getKey()) + 1
										&& !AppConfig.chordState.token.queue.contains(tokenRequest.getKey())) {
									AppConfig.chordState.token.queue.add(tokenRequest.getKey());
								}
							}


							// Give token to the first chord in queue
							if (!AppConfig.chordState.token.queue.isEmpty()) {
								int sendTokenTo = AppConfig.chordState.token.queue.remove();
								System.out.println("GIVING TOKEN TO " + AppConfig.getInfoById(sendTokenTo).getListenerPort());
								TokenMessage tokenMessage = new TokenMessage(MessageType.TOKEN, AppConfig.myServentInfo.getListenerPort(), sendTokenTo, AppConfig.chordState.token);
								MessageUtil.sendMessage(tokenMessage);
								// I sent the token and don't have it anymore
								AppConfig.chordState.token = null;
							}
						}
					}
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
