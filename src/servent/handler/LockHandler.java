package servent.handler;

import app.AppConfig;
import servent.message.LockMessage;
import servent.message.Message;

public class LockHandler implements MessageHandler{

    private Message clientMessage;

    public LockHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        // If the lock message is telling a chord to lock itself.
        if (!((LockMessage)clientMessage).isLockConfirmation()) {
            // Wait to acquire the chord lock, someone else
            // is using the chord at the moment
            synchronized (AppConfig.chordState.chordSync) {
                while (AppConfig.chordState.chordLock.get()) {
                    try {
                        AppConfig.chordState.chordSync.wait();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                AppConfig.chordState.chordLock.set(true);
            }
        } else {
            //If the lock message is confirming that that my predecessor chord has been locked.
            AppConfig.chordState.predecessorLock.set(false);
            AppConfig.chordState.predecessorSync.notify();
        }
    }
}
