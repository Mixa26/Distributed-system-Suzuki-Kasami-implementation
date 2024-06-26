package app;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.sql.Time;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import servent.handler.FailureChordHandler;
import servent.message.*;
import servent.message.util.MessageUtil;

/**
 * This class implements all the logic required for Chord to function.
 * It has a static method <code>chordHash</code> which will calculate our chord ids.
 * It also has a static attribute <code>CHORD_SIZE</code> that tells us what the maximum
 * key is in our system.
 * 
 * Other public attributes and methods:
 * <ul>
 *   <li><code>chordLevel</code> - log_2(CHORD_SIZE) - size of <code>successorTable</code></li>
 *   <li><code>successorTable</code> - a map of shortcuts in the system.</li>
 *   <li><code>predecessorInfo</code> - who is our predecessor.</li>
 *   <li><code>valueMap</code> - DHT values stored on this node.</li>
 *   <li><code>init()</code> - should be invoked when we get the WELCOME message.</li>
 *   <li><code>isCollision(int chordId)</code> - checks if a servent with that Chord ID is already active.</li>
 *   <li><code>isKeyMine(int key)</code> - checks if we have a key locally.</li>
 *   <li><code>getNextNodeForKey(int key)</code> - if next node has this key, then return it, otherwise returns the nearest predecessor for this key from my successor table.</li>
 *   <li><code>addNodes(List<ServentInfo> nodes)</code> - updates the successor table.</li>
 *   <li><code>putValue(int key, int value)</code> - stores the value locally or sends it on further in the system.</li>
 *   <li><code>getValue(int key)</code> - gets the value locally, or sends a message to get it from somewhere else.</li>
 * </ul>
 * @author bmilojkovic
 *
 */
public class ChordState implements Runnable{

	public static int CHORD_SIZE;

	public static int chordHash(int value) {
		return 61 * value % CHORD_SIZE;
	}

	private int chordLevel; //log_2(CHORD_SIZE)

	private ServentInfo[] successorTable;

	private ServentInfo predecessorInfo;

	public AtomicBoolean predecessorHealth;

	public AtomicBoolean predecessorConfirmedDead;

	public AtomicBoolean alreadySent;

	public AtomicInteger healthPort;

	private Long lastPredecessorHealthCheck;

	private final int pingEverySeconds = 5;

	//we DO NOT use this to send messages, but only to construct the successor table
	private List<ServentInfo> allNodeInfo;

	private Map<Integer, MyFile> valueMap;

	// key-port, value-backup data
	public Map<Integer, BackupData> backup;

	public AtomicInteger backupSequence;

	public final Object restructureSync = new Object();
	public final Object chordSync = new Object();


	public final Object successorLock = new Object();

	public final Object tokenRequestsLock = new Object();

	public Map<Integer, Integer> tokenRequests;

	public Token token;

	public List<Integer> friendChords;

	public List<MyFile> myFilesInSystem;

	public final Object updatesSync = new Object();

	public AtomicBoolean added;

	public Object addedSync = new Object();

	public ChordState() {
		this.chordLevel = 1;
		int tmp = CHORD_SIZE;
		while (tmp != 2) {
			if (tmp % 2 != 0) { //not a power of 2
				throw new NumberFormatException();
			}
			tmp /= 2;
			this.chordLevel++;
		}

		successorTable = new ServentInfo[chordLevel];
		for (int i = 0; i < chordLevel; i++) {
			successorTable[i] = null;
		}

		predecessorInfo = null;
		valueMap = new ConcurrentHashMap<>();
		allNodeInfo = new CopyOnWriteArrayList<>();
		tokenRequests = new HashMap<>();
		for (int i = 0; i < AppConfig.SERVENT_COUNT; i++){
			tokenRequests.put(i, 0);
		}
		token = null;
		friendChords = new CopyOnWriteArrayList<>();
		myFilesInSystem = new CopyOnWriteArrayList<>();
		added = new AtomicBoolean(false);
		backup = new ConcurrentHashMap<>();
		backupSequence = new AtomicInteger(0);
		predecessorHealth = new AtomicBoolean(true);
		predecessorConfirmedDead = new AtomicBoolean(false);
		healthPort = new AtomicInteger(-1);
		alreadySent = new AtomicBoolean(false);
	}

	/**
	 * This should be called once after we get <code>WELCOME</code> message.
	 * It sets up our initial value map and our first successor so we can send <code>UPDATE</code>.
	 * It also lets bootstrap know that we did not collide.
	 */
	public void init(WelcomeMessage welcomeMsg) {
		//set a temporary pointer to next node, for sending of update message
		successorTable[0] = new ServentInfo("localhost", welcomeMsg.getSenderPort(), AppConfig.getIdByPort(welcomeMsg.getSenderPort()));
		this.valueMap = welcomeMsg.getValues();

		//tell bootstrap this node is not a collider
		try {
			Socket bsSocket = new Socket("localhost", AppConfig.BOOTSTRAP_PORT);

			PrintWriter bsWriter = new PrintWriter(bsSocket.getOutputStream());
			bsWriter.write("New\n" + AppConfig.myServentInfo.getListenerPort() + "\n");

			bsWriter.flush();
			bsSocket.close();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public int getChordLevel() {
		return chordLevel;
	}

	public ServentInfo[] getSuccessorTable() {
		return successorTable;
	}

	public int getNextNodePort() {
		return Integer.valueOf(successorTable[0].getListenerPort());
	}

	public ServentInfo getPredecessor() {
		return predecessorInfo;
	}

	public void setPredecessor(ServentInfo newNodeInfo) {
		this.predecessorInfo = newNodeInfo;
	}

	public Map<Integer, MyFile> getValueMap() {
		return valueMap;
	}

	public List<ServentInfo> getAllNodeInfo() {
		return allNodeInfo;
	}

	public Map<Integer, MyFile> getValueMapCopy() {
		Map<Integer, MyFile> valueMapCopy = new HashMap<>();
		for (Map.Entry<Integer, MyFile> file : valueMap.entrySet()){
			valueMapCopy.put(file.getKey(), file.getValue());
		}
		return valueMapCopy;
	}

	public void setValueMap(Map<Integer, MyFile> valueMap) {
		this.valueMap = valueMap;
	}

	public boolean isCollision(int chordId) {
		if (chordId == AppConfig.myServentInfo.getChordId()) {
			return true;
		}
		for (ServentInfo serventInfo : allNodeInfo) {
			if (serventInfo.getChordId() == chordId) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns true if we are the owner of the specified key.
	 */
	public boolean isKeyMine(int key) {
		if (predecessorInfo == null) {
			return true;
		}

		int predecessorChordId = predecessorInfo.getChordId();
		int myChordId = AppConfig.myServentInfo.getChordId();

		if (predecessorChordId < myChordId) { //no overflow
			if (key <= myChordId && key > predecessorChordId) {
				return true;
			}
		} else { //overflow
			if (key <= myChordId || key > predecessorChordId) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Main chord operation - find the nearest node to hop to to find a specific key.
	 * We have to take a value that is smaller than required to make sure we don't overshoot.
	 * We can only be certain we have found the required node when it is our first next node.
	 */
	public ServentInfo getNextNodeForKey(int key) {
		if (isKeyMine(key)) {
			return AppConfig.myServentInfo;
		}

		//normally we start the search from our first successor
		int startInd = 0;

		//if the key is smaller than us, and we are not the owner,
		//then all nodes up to CHORD_SIZE will never be the owner,
		//so we start the search from the first item in our table after CHORD_SIZE
		//we know that such a node must exist, because otherwise we would own this key
		if (key < AppConfig.myServentInfo.getChordId()) {
			int skip = 1;
			while (successorTable[skip].getChordId() > successorTable[startInd].getChordId()) {
				startInd++;
				skip++;
			}
		}

		int previousId = successorTable[startInd].getChordId();

		for (int i = startInd + 1; i < successorTable.length; i++) {
			if (successorTable[i] == null) {
				AppConfig.timestampedErrorPrint("Couldn't find successor for " + key);
				break;
			}

			int successorId = successorTable[i].getChordId();

			if (successorId >= key) {
				return successorTable[i-1];
			}
			if (key > previousId && successorId < previousId) { //overflow
				return successorTable[i-1];
			}
			previousId = successorId;
		}
		//if we have only one node in all slots in the table, we might get here
		//then we can return any item
		return successorTable[0];
	}

	private void updateSuccessorTable() {
		//first node after me has to be successorTable[0]

		int currentNodeIndex = 0;
		ServentInfo currentNode = allNodeInfo.get(currentNodeIndex);
		successorTable[0] = currentNode;

		int currentIncrement = 2;

		ServentInfo previousNode = AppConfig.myServentInfo;

		//i is successorTable index
		for(int i = 1; i < chordLevel; i++, currentIncrement *= 2) {
			//we are looking for the node that has larger chordId than this
			int currentValue = (AppConfig.myServentInfo.getChordId() + currentIncrement) % CHORD_SIZE;

			int currentId = currentNode.getChordId();
			int previousId = previousNode.getChordId();

			//this loop needs to skip all nodes that have smaller chordId than currentValue
			while (true) {
				if (currentValue > currentId) {
					//before skipping, check for overflow
					if (currentId > previousId || currentValue < previousId) {
						//try same value with the next node
						previousId = currentId;
						currentNodeIndex = (currentNodeIndex + 1) % allNodeInfo.size();
						currentNode = allNodeInfo.get(currentNodeIndex);
						currentId = currentNode.getChordId();
					} else {
						successorTable[i] = currentNode;
						break;
					}
				} else { //node id is larger
					ServentInfo nextNode = allNodeInfo.get((currentNodeIndex + 1) % allNodeInfo.size());
					int nextNodeId = nextNode.getChordId();
					//check for overflow
					if (nextNodeId < currentId && currentValue <= nextNodeId) {
						//try same value with the next node
						previousId = currentId;
						currentNodeIndex = (currentNodeIndex + 1) % allNodeInfo.size();
						currentNode = allNodeInfo.get(currentNodeIndex);
						currentId = currentNode.getChordId();
					} else {
						successorTable[i] = currentNode;
						break;
					}
				}
			}
		}
	}

	/**
	 * This method constructs an ordered list of all nodes. They are ordered by chordId, starting from this node.
	 * Once the list is created, we invoke <code>updateSuccessorTable()</code> to do the rest of the work.
	 *
	 */
	public void addNodes(List<ServentInfo> newNodes) {
		allNodeInfo.addAll(newNodes);

		allNodeInfo.sort(new Comparator<ServentInfo>() {

			@Override
			public int compare(ServentInfo o1, ServentInfo o2) {
				return o1.getChordId() - o2.getChordId();
			}

		});

		List<ServentInfo> newList = new ArrayList<>();
		List<ServentInfo> newList2 = new ArrayList<>();

		int myId = AppConfig.myServentInfo.getChordId();
		for (ServentInfo serventInfo : allNodeInfo) {
			if (serventInfo.getChordId() < myId) {
				newList2.add(serventInfo);
			} else {
				newList.add(serventInfo);
			}
		}

		allNodeInfo.clear();
		allNodeInfo.addAll(newList);
		allNodeInfo.addAll(newList2);
		if (newList2.size() > 0) {
			predecessorInfo = newList2.get(newList2.size()-1);
		} else {
			predecessorInfo = newList.get(newList.size()-1);
		}

		updateSuccessorTable();
	}

	private void updateRemoveSuccessorTable(ServentInfo deadNode) {

		List<ServentInfo> allChords = new ArrayList<>(AppConfig.serventInfoList);
		allChords.sort(new Comparator<ServentInfo>() {
			@Override
			public int compare(ServentInfo o1, ServentInfo o2) {
				return o1.getChordId() - o2.getChordId();
			}

		});
		int j = 1;
		int g;
		for (int i = 0; i < successorTable.length; i++) {
			int cur = j + AppConfig.myServentInfo.getChordId();
			cur %= CHORD_SIZE;
			for (g = 0; g < allChords.size(); g++){
				if (cur <= allChords.get(g).getChordId() && allChords.get(g).getChordId() != deadNode.getChordId()){
					break;
				}
			}
			if (g >= allChords.size()){
				g = 0;
				if (allChords.get(g).getChordId() == deadNode.getChordId()) {
					g = 1;
				}
			}
			System.out.println("i " + i + " g " + g + " all: " + allChords.size());
			successorTable[i] = allChords.get(g);
			j *= 2;
		}
		System.out.print("Successor: ");
		for (int i = 0; i < successorTable.length; i++){
			System.out.print(successorTable[i] + " | ");
		}
		System.out.println();
	}

	public void removeNode(ServentInfo deadNode) {
		allNodeInfo.remove(deadNode);

		allNodeInfo.sort(new Comparator<ServentInfo>() {

			@Override
			public int compare(ServentInfo o1, ServentInfo o2) {
				return o1.getChordId() - o2.getChordId();
			}

		});

		List<ServentInfo> newList = new ArrayList<>();
		List<ServentInfo> newList2 = new ArrayList<>();

		int myId = AppConfig.myServentInfo.getChordId();
		for (ServentInfo serventInfo : allNodeInfo) {
			if (serventInfo.getChordId() < myId) {
				newList2.add(serventInfo);
			} else {
				newList.add(serventInfo);
			}
		}

		allNodeInfo.clear();
		allNodeInfo.addAll(newList);
		allNodeInfo.addAll(newList2);
		if (newList2.size() > 0) {
			predecessorInfo = newList2.get(newList2.size()-1);
		} else {
			predecessorInfo = newList.get(newList.size()-1);
		}

		updateRemoveSuccessorTable(deadNode);
		System.out.println("ZABOD");
	}

	/**
	 * The Chord put operation. Stores locally if key is ours, otherwise sends it on.
	 */
	public void putValue(int key, MyFile value) {
		if (!AppConfig.chordState.added.get()) {
			try {
				System.out.println("WAITING TO BE ADDED...");
				AppConfig.chordState.successorLock.wait();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
		System.out.println("PROCEEDING TO ADD THE FILE");
		myFilesInSystem.add(value);
		System.out.println("AFTER ADDING MY FILE SYSTEM " + myFilesInSystem);
		if (isKeyMine(key)) {
			valueMap.put(key, value);
			doBackup();
		} else {
			ServentInfo nextNode = getNextNodeForKey(key);
			PutMessage pm = new PutMessage(AppConfig.myServentInfo.getListenerPort(), nextNode.getListenerPort(), key, value);
			MessageUtil.sendMessage(pm);
		}
	}

	public void removeValue(int key) {
		if (!AppConfig.chordState.added.get()) {
			try {
				System.out.println("WAITING TO BE REMOVED...");
				AppConfig.chordState.successorLock.wait();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
		System.out.println("PROCEEDING TO REMOVE THE FILE");
		if (isKeyMine(key)) {
			System.out.println("IM REMOVING THE FILE !");
			valueMap.remove(key);
			doBackup();
		} else {
			ServentInfo nextNode = getNextNodeForKey(key);
			System.out.println("IM NOT REMOVING THE FILE, PASSING TO " + nextNode.getListenerPort());
			RemoveFileMessage rm = new RemoveFileMessage(AppConfig.myServentInfo.getListenerPort(), nextNode.getListenerPort(), key);
			MessageUtil.sendMessage(rm);
		}
	}

	private void doBackup() {
		// Update backups on successor and predecessor
		int newBackupSequence = backupSequence.incrementAndGet();
		BackupData newBackup = new BackupData(
				AppConfig.myServentInfo.getListenerPort(),
				newBackupSequence,
				getPredecessor(),
				getValueMapCopy()
		);
		// Send backup to his successor
		if (successorTable[0] != null) {
			Message backupS = new BackupMessage(AppConfig.myServentInfo.getListenerPort(), getNextNodePort(), newBackup);
			MessageUtil.sendMessage(backupS);
		}
		// Don't send backup to myself
		if (getPredecessor() != null && getPredecessor().getListenerPort() != AppConfig.myServentInfo.getListenerPort()) {
			// Send backup to his predecessor
			Message backupP = new BackupMessage(AppConfig.myServentInfo.getListenerPort(), getPredecessor().getListenerPort(), newBackup);
			MessageUtil.sendMessage(backupP);
		}
	}

	/**
	 * The chord get operation. Gets the value locally if key is ours, otherwise asks someone else to give us the value.
	 * @return <ul>
	 *			<li>The value, if we have it</li>
	 *			<li>-1 if we own the key, but there is nothing there</li>
	 *			<li>-2 if we asked someone else</li>
	 *		   </ul>
	 */
	public MyFile getValue(int key) {
		if (!AppConfig.chordState.added.get()) {
			try {
				System.out.println("WAITING TO BE ADDED...");
				AppConfig.chordState.successorLock.wait();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
		System.out.println("PROCEEDING TO GET THE FILE");
		System.out.println("MY PREDECESSOR " + predecessorInfo.getChordId() + " MY CHORD ID " + AppConfig.myServentInfo.getChordId());
		if (isKeyMine(key)) {
			if (valueMap.containsKey(key)) {
				System.out.println("FIRST?");
				return valueMap.get(key);
			} else {
				System.out.println("SECOND?");
				return new MyFile(null, FileType.PUBLIC);
			}
		}

		ServentInfo nextNode = getNextNodeForKey(key);
		System.out.println("ASKING " + nextNode.getListenerPort() + " FOR FILE");
		AskGetMessage agm = new AskGetMessage(AppConfig.myServentInfo.getListenerPort(), nextNode.getListenerPort(), String.valueOf(key));
		MessageUtil.sendMessage(agm);

		return null;
	}

	// Ping predecessor for health check
	@Override
	public void run() {
		while (true) {
			int port = AppConfig.chordState.healthPort.get();
			// Don't check the health for myself!
			if (port != -1 && port != AppConfig.myServentInfo.getListenerPort()) {
				if (AppConfig.chordState.predecessorHealth.get()) {
					AppConfig.chordState.predecessorHealth.set(false);
					MessageUtil.sendMessage(new PingMessage(AppConfig.myServentInfo.getListenerPort(), port));
					lastPredecessorHealthCheck = System.currentTimeMillis();
				} else {
					if (System.currentTimeMillis() >= lastPredecessorHealthCheck + AppConfig.strongLimit && AppConfig.chordState.predecessorConfirmedDead.get()) {
						// Restructure the system and inform others!
						System.out.println("DEEEEEEEEEEEAAAAAAAAAAAAAAAAAAAAAATHHHHHHHHHHHHHHH " + port);
						AppConfig.timestampedStandardPrint("Hazard! Predecessor " + port + " died!");
						AppConfig.chordState.predecessorConfirmedDead.set(false);

						System.out.println("DEADDDDDDZOOOOOOOOOOOOONEEEEEEEEEEEEEEEEEEEEEEEEEEE " + AppConfig.chordState.getPredecessor().getListenerPort());
						FailureChordHandler.restructure(AppConfig.chordState.backup.get(AppConfig.chordState.getPredecessor().getListenerPort()), AppConfig.chordState.getPredecessor());

					} else if (!AppConfig.chordState.alreadySent.get() && System.currentTimeMillis() >= lastPredecessorHealthCheck + AppConfig.weakLimit) {
						AppConfig.timestampedStandardPrint("Warning! Predecessor " + port + " passed weak limit for health check!");
						AppConfig.chordState.alreadySent.set(true);
						AppConfig.chordState.predecessorConfirmedDead.set(true);
						MessageUtil.sendMessage(new IsReallyDeadMessage(AppConfig.myServentInfo.getListenerPort(), AppConfig.chordState.backup.get(port).getPredecessorInfo().getListenerPort(), port, false, false));
					}
				}
			}

			try {
				Thread.sleep(pingEverySeconds * 1000);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
