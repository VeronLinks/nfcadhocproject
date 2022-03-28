package com.example.nfcbomb;

import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity {

    public static final String ERROR_DETECTED = "No NFC Tag detected.";
    public static final String WRITE_SUCCESS = "Text written successfully.";
    public static final String WRITE_ERROR = "Error writing. Try again.";
    private NfcAdapter m_NFCAdapter;
    private PendingIntent m_PendingIntent;
    private IntentFilter[] m_WritingTagFilters;
    private boolean m_IsInWriteMode;
    private Tag m_CurrentTag;
    private Context m_Context;

    private TextView m_EditMessage;
    private TextView m_NFCContents;
    private Button m_ActivateButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        m_EditMessage = findViewById(R.id.etMessage);
        m_NFCContents = findViewById(R.id.txtNFCContents);
        m_ActivateButton = findViewById(R.id.btnActivate);
        m_Context = this;

        m_ActivateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    if (m_CurrentTag == null) {
                        Toast.makeText(m_Context, ERROR_DETECTED, Toast.LENGTH_LONG).show();
                    }
                    else {
                        Write("PlainText|"+m_EditMessage.getText().toString(), m_CurrentTag);
                        Toast.makeText(m_Context, WRITE_SUCCESS, Toast.LENGTH_LONG).show();
                    }
                }
                catch (IOException | FormatException e) {
                    Toast.makeText(m_Context, WRITE_ERROR, Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            }
        });

        m_NFCAdapter = NfcAdapter.getDefaultAdapter(this);
        if (m_NFCAdapter == null) {
            Toast.makeText(this, "This device does not support NFC.", Toast.LENGTH_SHORT).show();
            finish();
        }

        ReadFromIntent(getIntent());
        m_PendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        tagDetected.addCategory(Intent.CATEGORY_DEFAULT);
        m_WritingTagFilters = new IntentFilter[] {tagDetected};
    }

    private void ReadFromIntent(Intent intent) {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action) ||
            NfcAdapter.ACTION_TECH_DISCOVERED.equals(action) ||
            NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage[] msgs = null;
            if (rawMsgs != null) {
                msgs = new NdefMessage[rawMsgs.length];
                for (int i = 0; i < msgs.length; ++i) {
                    msgs[i] = (NdefMessage) rawMsgs[i];
                }
            }
            BuildTagViews(msgs);
        }
    }

    private void BuildTagViews(NdefMessage[] msgs) {
        if (msgs == null || msgs.length == 0) return;

        String text = "";
        byte[] payload = msgs[0].getRecords()[0].getPayload();
        String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";
        int languageCodeLength = payload[0] & 0063; // Language code (e.g.: "en")

        try {
            text = new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
        }
        catch (UnsupportedEncodingException e) {
            Log.e("Unsupported encoding: ", e.toString());
        }

        m_NFCContents.setText("NFC Content: " + text);
    }

    private void Write(String text, Tag tag) throws IOException, FormatException {
        NdefRecord[] records = { CreateRecord(text) };
        NdefMessage message = new NdefMessage(records);

        Ndef tagNdef = Ndef.get(tag);
        tagNdef.connect();
        tagNdef.writeNdefMessage(message);
        tagNdef.close();
    }

    private NdefRecord CreateRecord(String text) throws UnsupportedEncodingException {
        String lang = "en";
        byte[] textBytes = text.getBytes();
        byte[] langBytes = lang.getBytes("US-ASCII");
        int langLength = langBytes.length;
        int textLength = textBytes.length;
        byte[] payload = new byte[1 + langLength + textLength];

        payload[0] = (byte) langLength;

        System.arraycopy(langBytes, 0, payload, 1, langLength);
        System.arraycopy(textBytes, 0, payload, 1 + langLength, textLength);

        NdefRecord recordNFC = new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], payload);

        return recordNFC;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        ReadFromIntent(intent);

        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction()) ||
                NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction()) ||
                NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            m_CurrentTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        WriteMode(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        WriteMode(true);
    }

    private void WriteMode(boolean on) {
        m_IsInWriteMode = on;
        if (on) {
            m_NFCAdapter.enableForegroundDispatch(this, m_PendingIntent, m_WritingTagFilters, null);
        }
        else {
            m_NFCAdapter.disableForegroundDispatch(this);
        }
    }
}