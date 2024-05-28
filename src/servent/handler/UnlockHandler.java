package servent.handler;

import app.AppConfig;
import servent.message.LockMessage;
import servent.message.Message;

public class UnlockHandler implements MessageHandler{

    private Message clientMessage;

    public UnlockHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        // If the unlock message is telling a chord to unlock itself.
        if (!((LockMessage)clientMessage).isLockConfirmation()) {
            // Wait to acquire the chord lock, someone else
            // is using the chord at the moment
            synchronized (AppConfig.chordState.chordSync) {
                if (!AppConfig.chordState.chordLock.get()) {
                    AppConfig.timestampedErrorPrint("Unlock called without the deleteChordLock being set. Check UnlockHandler.");
                }
                AppConfig.chordState.chordLock.set(false);
                AppConfig.chordState.chordSync.notify();
            }
        } else {
            //If the unlock message is confirming that my predecessor chord has been unlocked.
            AppConfig.chordState.predecessorLock.set(false);
            AppConfig.chordState.predecessorSync.notify();
        }
    }
}
