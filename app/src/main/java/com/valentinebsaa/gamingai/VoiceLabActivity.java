package com.valentinebsaa.gamingai;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Cost-free VOICE LAB, NOT a cloud model or full-game conversation service.
 * Requires a local TTS voice. Mic uses ONLY createOnDeviceSpeechRecognizer,
 * when the device exposes one; no network fallback or INTERNET permission.
 */
public final class VoiceLabActivity extends Activity {
    public static final String EXTRA_GAME = "game";
    private static final String PREFS = "voice_lab_settings";
    private static final int REQUEST_MIC = 201;
    private final int background = Color.rgb(15, 21, 35);
    private final int foreground = Color.rgb(242, 236, 244);
    private final int muted = Color.rgb(185, 176, 194);
    private TextToSpeech speech;
    private SpeechRecognizer recognizer;
    private boolean speechReady;
    private boolean listening;
    private Spinner voices;
    private final List<Voice> offlineVoices = new ArrayList<>();
    private EditText prompt;
    private TextView status;
    private TextView response;
    private TextView diagnostics;
    private TextView levels;
    private SeekBar rateBar;
    private SeekBar pitchBar;
    private CheckBox mute;
    private String game;
    private boolean configuring;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        game = getIntent().getStringExtra(EXTRA_GAME);
        if (game == null || game.trim().isEmpty()) game = "General";
        SharedPreferences preferences = getSharedPreferences(PREFS, MODE_PRIVATE);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(background);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(18);
        root.setPadding(pad, pad, pad, pad);
        scroll.addView(root);
        setContentView(scroll);

        addText(root, "Voice Lab · $0 API cost", 25, foreground);
        addText(root, "Offline voice audition • " + game, 13, muted);
        addText(root, "This is a small scripted companion for testing voices, not a "
            + "generative AI. It does not see the game, record conversations, "
            + "speak over games, or use credits.", 13, foreground);

        status = addText(root, "Initializing Android's voice engine...", 13, muted);
        diagnostics = addText(root, "Checking offline voice and microphone availability...",
            12, muted);

        addText(root, "CHOOSE AN INSTALLED OFFLINE VOICE", 14, foreground);
        voices = new Spinner(this);
        ArrayAdapter<String> empty = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item, new String[]{"Checking offline voices..."});
        empty.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        voices.setAdapter(empty);
        root.addView(voices, new LinearLayout.LayoutParams(-1, dp(55)));
        voices.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!configuring && position >= 0 && position < offlineVoices.size()) {
                    getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                        .putString("voice", offlineVoices.get(position).getName()).apply();
                    setStatus("Voice selected. Tap Preview voice to hear it.");
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        addText(root, "SPEAKING SPEED AND PITCH", 14, foreground);
        rateBar = settingSlider(root, "Speed", preferences.getInt("rate", 30));
        pitchBar = settingSlider(root, "Pitch", preferences.getInt("pitch", 30));
        levels = addText(root, "", 12, muted);
        SeekBar.OnSeekBarChangeListener settingsListener = new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int value, boolean fromUser) {
                refreshLevels();
            }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {
                getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                    .putInt("rate", rateBar.getProgress())
                    .putInt("pitch", pitchBar.getProgress()).apply();
            }
        };
        rateBar.setOnSeekBarChangeListener(settingsListener);
        pitchBar.setOnSeekBarChangeListener(settingsListener);
        refreshLevels();

        mute = new CheckBox(this);
        mute.setText("Mute spoken replies (text only)");
        mute.setTextColor(foreground);
        mute.setChecked(preferences.getBoolean("muted", false));
        root.addView(mute);
        mute.setOnCheckedChangeListener((view, checked) -> {
            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putBoolean("muted", checked).apply();
            if (checked) stopSpeaking();
            setStatus(checked ? "Muted. Replies stay on-screen." : "Unmuted. I only speak after you ask.");
        });

        button(root, "Preview voice · sample phrase", v -> speak(
            "Hey, I'm here when you need me. Enjoy the game!", true));
        button(root, "Stop speaking now", v -> {
            stopSpeaking();
            setStatus("Stopped speaking.");
        });

        addText(root, "TRY A CONVERSATION", 14, foreground);
        prompt = new EditText(this);
        prompt.setSingleLine(false);
        prompt.setMinLines(2);
        prompt.setMaxLines(3);
        prompt.setTextColor(foreground);
        prompt.setHintTextColor(muted);
        prompt.setHint("Try: What do you remember? Can you see my game?");
        prompt.setTextSize(14);
        root.addView(prompt);
        button(root, "Ask companion · offline scripted reply", v ->
            answer(prompt.getText().toString()));
        button(root, "Push to talk · ON-DEVICE ONLY", v -> startOfflineListening());
        button(root, "Stop microphone", v -> stopListening("Microphone stopped."));
        response = addText(root, "Responses will appear here.", 14, foreground);
        addText(root, "PRIVACY AND COST", 14, foreground);
        addText(root, "No API key, subscription, internet permission or Piper. "
            + "No raw audio or transcripts are saved. Only voice preferences are stored "
            + "in Android settings; existing explicit notes remain on the selected microSD. "
            + "Android's own speech engines may have their own privacy terms.", 12, muted);
        button(root, "Back to Gaming AI", v -> finish());
        initSpeechEngine();
        updateDiagnostics();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private TextView addText(LinearLayout root, String content, int size, int color) {
        TextView label = new TextView(this);
        label.setText(content);
        label.setTextSize(size);
        label.setTextColor(color);
        label.setPadding(0, dp(8), 0, dp(8));
        root.addView(label, new LinearLayout.LayoutParams(-1, -2));
        return label;
    }

    private void button(LinearLayout root, String label, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(14);
        button.setAllCaps(false);
        button.setTextColor(foreground);
        button.setMinHeight(dp(50));
        button.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(82, 44, 79)));
        button.setOnClickListener(listener);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(5);
        root.addView(button, params);
    }

    private SeekBar settingSlider(LinearLayout root, String name, int value) {
        addText(root, name, 12, muted);
        SeekBar slider = new SeekBar(this);
        slider.setMax(80);
        slider.setProgress(Math.max(0, Math.min(80, value)));
        root.addView(slider, new LinearLayout.LayoutParams(-1, dp(38)));
        return slider;
    }

    private void refreshLevels() {
        if (levels != null) {
            levels.setText(String.format(Locale.US, "Speed %.2fx    Pitch %.2fx",
                scale(rateBar), scale(pitchBar)));
        }
    }

    private float scale(SeekBar slider) { return 0.7f + slider.getProgress() / 100f; }

    private void setStatus(String text) {
        if (status != null) status.setText(text);
    }

    private boolean hasOfflineRecognition() {
        return Build.VERSION.SDK_INT >= 31 &&
            SpeechRecognizer.isOnDeviceRecognitionAvailable(this);
    }

    private void updateDiagnostics() {
        String recognizerMode;
        try {
            recognizerMode = hasOfflineRecognition()
                ? "Available. Microphone permission requested only on tap."
                : "Unavailable on this Android installation. Type instead.";
        } catch (Exception e) {
            recognizerMode = "Not accessible on this device. Type instead.";
        }
        if (diagnostics != null) {
            diagnostics.setText("Offline text-to-speech voices: " + offlineVoices.size()
                + "  •  On-device recognition: " + recognizerMode);
        }
    }

    private void initSpeechEngine() {
        try {
            speech = new TextToSpeech(this, result -> {
                if (result != TextToSpeech.SUCCESS) {
                    speechReady = false;
                    setStatus("Android could not initialize a speech engine. Text mode still works.");
                    updateDiagnostics();
                    return;
                }
                speechReady = true;
                loadOfflineVoices();
            });
        } catch (Exception e) {
            setStatus("No usable Android TTS engine. Text mode still works.");
        }
    }

    private void loadOfflineVoices() {
        offlineVoices.clear();
        try {
            if (speech != null && speech.getVoices() != null) {
                for (Voice voice : speech.getVoices()) {
                    if (!voice.isNetworkConnectionRequired()
                        && "en".equalsIgnoreCase(voice.getLocale().getLanguage())) {
                        offlineVoices.add(voice);
                    }
                }
            }
        } catch (Exception e) {
            setStatus("Unable to enumerate offline voices: " + e.getMessage());
        }
        Collections.sort(offlineVoices, Comparator.comparing(Voice::getName));
        ArrayList<String> labels = new ArrayList<>();
        for (Voice voice : offlineVoices) {
            labels.add(voice.getName() + " · " + voice.getLocale().toLanguageTag()
                + " · quality " + voice.getQuality());
        }
        if (labels.isEmpty()) labels.add("No English offline TTS voice installed");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        configuring = true;
        voices.setAdapter(adapter);
        String saved = getSharedPreferences(PREFS, MODE_PRIVATE).getString("voice", "");
        int selected = 0;
        for (int i = 0; i < offlineVoices.size(); i++) {
            if (offlineVoices.get(i).getName().equals(saved)) { selected = i; break; }
        }
        voices.setSelection(selected);
        configuring = false;
        updateDiagnostics();
        setStatus(offlineVoices.isEmpty()
            ? "No installed offline English voice. Install one in Android TTS settings, or use text."
            : offlineVoices.size() + " offline English voices found. Preview and choose your favorite.");
    }

    private void stopSpeaking() {
        if (speech != null && speechReady) speech.stop();
    }

    private void speak(String text, boolean preview) {
        if (mute != null && mute.isChecked()) {
            setStatus("Muted. Uncheck Mute to hear the voice.");
            return;
        }
        if (!speechReady || speech == null || offlineVoices.isEmpty()) {
            setStatus("No offline voice ready. You can still read the response.");
            return;
        }
        int index = voices.getSelectedItemPosition();
        if (index < 0 || index >= offlineVoices.size()) {
            setStatus("Select an offline voice first.");
            return;
        }
        Voice voice = offlineVoices.get(index);
        if (voice.isNetworkConnectionRequired()
            || speech.setVoice(voice) == TextToSpeech.ERROR) {
            setStatus("This offline voice is not usable on this device.");
            return;
        }
        speech.setSpeechRate(scale(rateBar));
        speech.setPitch(scale(pitchBar));
        int queued = speech.speak(text, TextToSpeech.QUEUE_FLUSH, null,
            preview ? "voice-preview" : "offline-response");
        setStatus(queued == TextToSpeech.ERROR
            ? "Speech playback failed. Try another offline voice."
            : "Speaking with installed offline voice. Tap Stop whenever you like.");
    }

    private List<String> memoriesForThisGame() {
        ArrayList<String> notes = new ArrayList<>();
        try {
            String folder = getSharedPreferences("permission_locator_only", MODE_PRIVATE)
                .getString("sd_tree_uri", null);
            if (folder == null) return notes;
            Uri uri = Uri.parse(folder);
            if (!StorageRules.isRemovableSdSubfolder(uri.getAuthority(),
                    DocumentsContract.getTreeDocumentId(uri))) return notes;
            List<MemoryRecord> records = new MemoryStore(this, uri).load();
            for (int i = records.size() - 1; i >= 0 && notes.size() < 3; i--) {
                MemoryRecord record = records.get(i);
                if (game.equals(record.game)) notes.add(record.note);
            }
        } catch (Exception ignored) {
            // No SD / revoked permission: speak no guessed memories.
        }
        return notes;
    }

    private void answer(String question) {
        if (question == null || question.trim().isEmpty()) {
            setStatus("Type something or use on-device push-to-talk.");
            return;
        }
        String reply = CompanionBrain.reply(question, game, memoriesForThisGame());
        response.setText(reply);
        speak(reply, false);
    }

    private void startOfflineListening() {
        if (listening) {
            stopListening("Microphone stopped.");
            return;
        }
        try {
            if (!hasOfflineRecognition()) {
                setStatus("No Android on-device recognizer is installed. I won't fall back to cloud speech.");
                return;
            }
        } catch (Exception e) {
            setStatus("Cannot access an offline speech recognizer. Type instead.");
            return;
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_MIC);
            return;
        }
        try {
            if (recognizer == null) {
                recognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(this);
                recognizer.setRecognitionListener(new RecognitionListener() {
                    @Override public void onReadyForSpeech(Bundle params) {
                        setStatus("Microphone active. Say a short question.");
                    }
                    @Override public void onBeginningOfSpeech() { setStatus("Listening now..."); }
                    @Override public void onRmsChanged(float rms) {}
                    @Override public void onBufferReceived(byte[] buffer) {}
                    @Override public void onEndOfSpeech() { setStatus("Processing speech locally..."); }
                    @Override public void onError(int error) {
                        listening = false;
                        setStatus(error == SpeechRecognizer.ERROR_NO_MATCH
                            || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT
                            ? "No speech recognized. Try again or type."
                            : "On-device recognition error " + error + ". Try typing.");
                    }
                    @Override public void onResults(Bundle results) {
                        listening = false;
                        ArrayList<String> recognized =
                            results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                        if (recognized == null || recognized.isEmpty()) {
                            setStatus("Nothing recognized. Try typing.");
                            return;
                        }
                        prompt.setText(recognized.get(0));
                        answer(recognized.get(0));
                    }
                    @Override public void onPartialResults(Bundle results) {}
                    @Override public void onEvent(int type, Bundle params) {}
                });
            }
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toLanguageTag());
            intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
            listening = true;
            recognizer.startListening(intent);
            setStatus("Starting on-device microphone. Tap Stop microphone to cancel.");
        } catch (Exception e) {
            listening = false;
            setStatus("On-device microphone unavailable: " + e.getClass().getSimpleName());
            if (recognizer != null) { recognizer.destroy(); recognizer = null; }
        }
    }

    private void stopListening(String message) {
        if (recognizer != null) recognizer.cancel();
        listening = false;
        setStatus(message);
    }

    @Override public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_MIC) {
            if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startOfflineListening();
            } else {
                setStatus("Microphone permission denied. Typing and offline speech still work.");
            }
        }
    }

    @Override protected void onStop() {
        super.onStop();
        if (listening) stopListening("Voice Lab paused; microphone stopped.");
        stopSpeaking();
    }

    @Override protected void onDestroy() {
        if (recognizer != null) {
            recognizer.cancel();
            recognizer.destroy();
            recognizer = null;
        }
        if (speech != null) {
            speech.stop();
            speech.shutdown();
            speech = null;
        }
        super.onDestroy();
    }
}
