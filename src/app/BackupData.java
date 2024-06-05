package app;

import java.io.Serializable;
import java.util.Map;

public class BackupData implements Serializable {

    private int port;

    private int sequenceNumber;


    private ServentInfo predecessorInfo;

    private Map<Integer, MyFile> valueMap;

    public BackupData(int port, int sequenceNumber, ServentInfo predecessorInfo, Map<Integer, MyFile> valueMap) {
        this.port = port;
        this.sequenceNumber = sequenceNumber;
        this.predecessorInfo = predecessorInfo;
        this.valueMap = valueMap;
    }

    public int getPort() {
        return port;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public ServentInfo getPredecessorInfo() {
        return predecessorInfo;
    }

    public Map<Integer, MyFile> getValueMap() {
        return valueMap;
    }
}
