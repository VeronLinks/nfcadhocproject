package com.example.nfcbomb;

public class BombData {
    private static final String SEPARATOR = ";";

    public String m_PhoneID;
    public boolean m_HasBomb;
    public Long m_StartingTime;
    public Long m_Endingtime;

    public String RawData() {
        return m_PhoneID + SEPARATOR +
                (m_HasBomb ? 1 : 0) + SEPARATOR +
                m_StartingTime + SEPARATOR +
                m_Endingtime;
    }

    public void LoadData(String rawData) {
        String[] allData = rawData.split(SEPARATOR);
        m_PhoneID = allData[0];
        m_HasBomb = Integer.parseInt(allData[1]) == 1;
        m_StartingTime = Long.parseLong(allData[2]);
        m_Endingtime = Long.parseLong(allData[3]);
    }
}
