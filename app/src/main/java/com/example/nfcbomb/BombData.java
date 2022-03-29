package com.example.nfcbomb;

public class BombData {
    public static final String START_CHECK = "BOMBDATA";
    private static final String SEPARATOR = ";";

    public String m_PhoneID;
    public boolean m_HasBomb;
    public Long m_Endingtime;

    public String RawData() {
        return START_CHECK + SEPARATOR +
                m_PhoneID + SEPARATOR +
                (m_HasBomb ? 1 : 0) + SEPARATOR +
                m_Endingtime;
    }

    public boolean LoadData(String rawData) {
        String[] allData = rawData.split(SEPARATOR);
        if (allData[0].equals(START_CHECK)) {
            m_PhoneID = allData[1];
            m_HasBomb = Integer.parseInt(allData[2]) == 1;
            m_Endingtime = Long.parseLong(allData[3]);

            return true;
        }

        return false;
    }
}
