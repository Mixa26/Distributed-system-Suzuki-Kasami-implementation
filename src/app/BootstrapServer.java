package app;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

public class BootstrapServer {

	private volatile boolean working = true;
	private List<Integer> activeServents;

	private final Object nodeAddedSync = new Object();

	private AtomicBoolean nodeAddedLock;

	private class CLIWorker implements Runnable {
		@Override
		public void run() {
			Scanner sc = new Scanner(System.in);

			String line;
			while(true) {
				line = sc.nextLine();

				if (line.equals("stop")) {
					working = false;
					break;
				}
			}

			sc.close();
		}
	}

	private class ChordAdder implements Runnable {

		private Socket newServentSocket;

		private Random rand;

		public ChordAdder(Socket newServentSocket) {
			this.newServentSocket = newServentSocket;
			rand = new Random(System.currentTimeMillis());
		}

		@Override
		public void run() {
			try {
				Scanner socketScanner = new Scanner(newServentSocket.getInputStream());
				String message = socketScanner.nextLine();

				/*
				 * New servent has hailed us. He is sending us his own listener port.
				 * He wants to get a listener port from a random active servent,
				 * or -1 if he is the first one.
				 */
				if (message.equals("Hail")) {
					// Wait for the previous node to be added
					System.out.println("LOCKING NODE ADDER");
					synchronized (nodeAddedSync) {
						while (nodeAddedLock.get()) {
							try {
								nodeAddedSync.wait();
							} catch (InterruptedException e) {
								throw new RuntimeException(e);
							}
						}
						// Set that we're adding a new node
						nodeAddedLock.set(true);

						int newServentPort = socketScanner.nextInt();

						System.out.println("got " + newServentPort);
						PrintWriter socketWriter = new PrintWriter(newServentSocket.getOutputStream());

						if (activeServents.size() == 0) {
							socketWriter.write(String.valueOf(-1) + "\n");
							System.out.println("adding " + newServentPort);
							activeServents.add(newServentPort); //first one doesn't need to confirm
							nodeAddedLock.set(false);
							System.out.println("UNLOCKING NODE ADDER FROM FIRST!!!");
						} else {
							int randServent = activeServents.get(rand.nextInt(activeServents.size()));
							socketWriter.write(String.valueOf(randServent) + "\n");
						}

						socketWriter.flush();
						newServentSocket.close();
					}
				} else if (message.equals("New")) {
					/**
					 * When a servent is confirmed not to be a collider, we add him to the list.
					 */
					// Signal the previous node has been added.
					System.out.println("BEFORE NEW");
					synchronized (nodeAddedSync) {
						System.out.println("IN NEW");
						if (!nodeAddedLock.get()) {
							System.err.println("NODE LOCK WASN'T SET IN BOOTSTRAP WHILE ADDING NODE!");
						}
						int newServentPort = socketScanner.nextInt();

						System.out.println("adding " + newServentPort);

						activeServents.add(newServentPort);
						newServentSocket.close();

						// Tell bootstrap to continue adding nodes.
						nodeAddedLock.set(false);
						nodeAddedSync.notify();
					}
					System.out.println("UNLOCKING NODE ADDER");
				} else if (message.equals("Sorry")) {
					System.out.println("BEFORE SORRY");
					// Signal the previous node hasn't been added.
					synchronized (nodeAddedSync) {
						System.out.println("IN SORRY");
						if (!nodeAddedLock.get()) {
							System.err.println("NODE LOCK WASN'T SET IN BOOTSTRAP WHILE ADDING NODE!");
						}

						// Tell bootstrap to continue adding nodes.
						nodeAddedLock.set(false);
						nodeAddedSync.notify();
					}
					System.out.println("UNLOCKING NODE ADDER");
				}
			}catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public BootstrapServer() {
		activeServents = new ArrayList<>();
		nodeAddedLock = new AtomicBoolean(false);
	}

	public void doBootstrap(int bsPort) {
		Thread cliThread = new Thread(new CLIWorker());
		cliThread.start();

		ServerSocket listenerSocket = null;
		try {
			listenerSocket = new ServerSocket(bsPort, 100);
			//listenerSocket.setSoTimeout(1000);
		} catch (IOException e1) {
			AppConfig.timestampedErrorPrint("Problem while opening listener socket.");
			System.exit(0);
		}

		while (working) {
			try {
				Socket newServentSocket = listenerSocket.accept();

				Thread chordAdder = new Thread(new ChordAdder(newServentSocket));

				chordAdder.start();

			} catch (SocketTimeoutException e) {

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Expects one command line argument - the port to listen on.
	 */
	public static void main(String[] args) {
		if (args.length != 1) {
			AppConfig.timestampedErrorPrint("Bootstrap started without port argument.");
		}

		int bsPort = 0;
		try {
			bsPort = Integer.parseInt(args[0]);
		} catch (NumberFormatException e) {
			AppConfig.timestampedErrorPrint("Bootstrap port not valid: " + args[0]);
			System.exit(0);
		}

		AppConfig.timestampedStandardPrint("Bootstrap server started on port: " + bsPort);

		BootstrapServer bs = new BootstrapServer();
		bs.doBootstrap(bsPort);
	}
}
