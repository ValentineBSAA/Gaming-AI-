package com.valentinebsaa.gamingai;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.provider.Settings;
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
import android.widget.ListView;
import android.graphics.drawable.ColorDrawable;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
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
    private static final int REQUEST_CHECK_VOICE_DATA = 202;
    private final int background = Color.rgb(15, 21, 35);
    private final int foreground = Color.rgb(242, 236, 244);
    private final int muted = Color.rgb(185, 176, 194);
    private TextToSpeech speech;
    private SpeechRecognizer recognizer;
    private boolean speechReady;
    private boolean listening;
    private ListView voices;
    private ArrayAdapter<String> voiceItems;
    private ScrollView labScroll;
    private Spinner voiceFilter;
    private TextView voiceDetails;
    private TextView guide;
    private final List<Voice> offlineVoices = new ArrayList<>();
    private final List<Voice> visibleVoices = new ArrayList<>();
    private boolean refreshAfterSettings;
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
        labScroll = TouchUi.page(this);
        LinearLayout root = TouchUi.root(this, labScroll);
        setContentView(labScroll);
        labScroll.requestFocus();

        addText(root, "Voice Lab · $0 API cost", 25, foreground);
        addText(root, "Touch-ready offline voice library • " + game, 13, muted);
        addText(root, "A scripted voice demo, not a generative AI. No game screen access, "
            + "background microphone, paid credits or automatic downloads.", 13, foreground);
        status = addText(root, "Initializing Android's voice engine...", 13, muted);
        diagnostics = addText(root, "Checking installed offline voices...", 12, muted);

        LinearLayout shortcuts = new LinearLayout(this);
        shortcuts.setOrientation(LinearLayout.HORIZONTAL);
        TouchUi.rail(this, root).addView(shortcuts);
        final LinearLayout setupCard = TouchUi.card(this, root);
        final LinearLayout voicesCard = TouchUi.card(this, root);
        final LinearLayout conversationCard = TouchUi.card(this, root);
        final LinearLayout privacyCard = TouchUi.card(this, root);
        TouchUi.chip(this, shortcuts, "Setup",
            v -> TouchUi.jumpTo(this, labScroll, setupCard));
        TouchUi.chip(this, shortcuts, "Voices",
            v -> TouchUi.jumpTo(this, labScroll, voicesCard));
        TouchUi.chip(this, shortcuts, "Talk",
            v -> TouchUi.jumpTo(this, labScroll, conversationCard));
        TouchUi.chip(this, shortcuts, "Privacy",
            v -> TouchUi.jumpTo(this, labScroll, privacyCard));

        addText(setupCard, "GUIDED OFFLINE VOICE SETUP", 15, foreground);
        guide = addText(setupCard,
            "1. Check available voice data. 2. If needed, open Android voice settings. "
            + "3. Approve any optional offline pack yourself. 4. Return and refresh. "
            + "Actual voice quality must be tested on this device.", 12, foreground);
        button(setupCard, "Step 1 · Check installed voice data", this::checkVoiceData);
        button(setupCard, "Step 2 · Manage/download voice packs", this::installVoiceData);
        button(setupCard, "Alternative · Android speech settings", this::openTtsSettings);
        button(setupCard, "Step 3 · Refresh installed voice catalog",
            view -> refreshSpeechEngine());

        addText(voicesCard, "SCROLLABLE OFFLINE VOICES", 15, foreground);
        addText(voicesCard, "Pick a region, swipe the list and tap a voice to select. "
            + "Voice quality is engine-reported; gender is not inferred.", 12, muted);
        voiceFilter = new Spinner(this);
        String[] groups = {"All English voices", "US English", "UK English",
            "Other English", "My favorites"};
        ArrayAdapter<String> groupsAdapter = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item, groups);
        groupsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        voiceFilter.setAdapter(groupsAdapter);
        voiceFilter.setMinimumHeight(dp(52));
        voicesCard.addView(voiceFilter, new LinearLayout.LayoutParams(-1, dp(56)));
        voiceFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent,
                    View view, int position, long id) {
                refreshVisibleVoices();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        voices = new ListView(this);
        voices.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        voices.setVerticalScrollBarEnabled(true);
        voices.setSmoothScrollbarEnabled(true);
        voices.setVerticalFadingEdgeEnabled(true);
        voices.setFadingEdgeLength(dp(12));
        voices.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        voices.setNestedScrollingEnabled(true);
        voices.setDivider(new ColorDrawable(0xff384960));
        voices.setDividerHeight(dp(1));
        voicesCard.addView(voices, new LinearLayout.LayoutParams(-1,
            dp(SquareLayoutPolicy.voiceListHeightDp(TouchUi.widthDp(this)))));
        voices.setOnItemClickListener((parent, row, position, id) -> {
            if (position >= 0 && position < visibleVoices.size()) {
                voices.setItemChecked(position, true);
                Voice selected = visibleVoices.get(position);
                getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                    .putString("voice", selected.getName()).apply();
                if (voiceItems != null) voiceItems.notifyDataSetChanged();
                showVoiceDetails();
                setStatus("Voice selected. Tap Preview to hear it.");
            }
        });

        voiceDetails = addText(voicesCard, "Choose a voice to see details.", 12, muted);
        button(voicesCard, "★ Add/remove selected voice from favorites",
            view -> toggleFavorite());
        addText(voicesCard, "VOICE SPEED & PITCH", 14, foreground);
        rateBar = settingSlider(voicesCard, "Speed", preferences.getInt("rate", 30));
        pitchBar = settingSlider(voicesCard, "Pitch", preferences.getInt("pitch", 30));
        levels = addText(voicesCard, "", 12, muted);
        SeekBar.OnSeekBarChangeListener adjust = new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int value, boolean user) {
                refreshLevels();
            }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {
                getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                    .putInt("rate", rateBar.getProgress())
                    .putInt("pitch", pitchBar.getProgress()).apply();
            }
        };
        rateBar.setOnSeekBarChangeListener(adjust);
        pitchBar.setOnSeekBarChangeListener(adjust);
        refreshLevels();

        mute = new CheckBox(this);
        mute.setText("Mute spoken replies (text only)");
        mute.setTextColor(foreground);
        mute.setMinHeight(dp(48));
        mute.setChecked(preferences.getBoolean("muted", false));
        voicesCard.addView(mute);
        mute.setOnCheckedChangeListener((button, checked) -> {
            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putBoolean("muted", checked).apply();
            if (checked) stopSpeaking();
            setStatus(checked ? "Muted. Replies remain on-screen."
                : "Unmuted. Voice only speaks when requested.");
        });
        button(voicesCard, "Preview selected offline voice", v -> speak(
            "Hey, I'm here when you need me. Enjoy the game!", true));
        button(voicesCard, "Stop speech now", v -> {
            stopSpeaking();
            setStatus("Stopped speaking.");
        });

        addText(conversationCard, "TRY A SCRIPTED CONVERSATION", 15, foreground);
        prompt = new EditText(this);
        prompt.setSingleLine(false);
        prompt.setMinLines(2);
        prompt.setMaxLines(3);
        prompt.setTextColor(foreground);
        prompt.setHintTextColor(muted);
        prompt.setHint("Try: What do you remember? Can you see my game?");
        prompt.setTextSize(14);
        conversationCard.addView(prompt);
        button(conversationCard, "Ask companion · offline scripted reply",
            v -> answer(prompt.getText().toString()));
        button(conversationCard, "Push to talk · ON-DEVICE ONLY",
            v -> startOfflineListening());
        button(conversationCard, "Stop microphone",
            v -> stopListening("Microphone stopped."));
        response = addText(conversationCard, "Responses will appear here.", 14, foreground);

        addText(privacyCard, "PRIVACY AND COST", 15, foreground);
        addText(privacyCard, "No API key, subscription, internet permission or Piper. "
            + "No raw audio or transcripts saved. Preferences are in Android settings; "
            + "selected notes remain on your microSD. Voice engines have their own terms.",
            12, muted);
        button(privacyCard, "Back to Gaming AI", v -> finish());

        initSpeechEngine();
        updateDiagnostics();
        TouchUi.reveal(this, root);
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
        TouchUi.button(this, root, label, listener);
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
            String engine = "Unknown";
            try {
                if (speech != null) engine = speech.getDefaultEngine();
            } catch (Exception ignored) {}
            diagnostics.setText("Android voice engine: " + engine
                + "\nOffline English voices advertised: " + offlineVoices.size()
                + "  •  On-device recognition: " + recognizerMode
                + "\nA voice marked offline may still need its data installed; preview it "
                + "in Airplane mode before relying on it.");
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
        offlineVoices.sort((left, right) ->
            VoiceCatalog.compare(left.getLocale().toLanguageTag(), left.getQuality(),
                left.getName(), right.getLocale().toLanguageTag(),
                right.getQuality(), right.getName()));
        refreshVisibleVoices();
        updateDiagnostics();
        setStatus(offlineVoices.isEmpty()
            ? "No English voice advertised for offline use. Follow steps 1–3 above, or use text."
            : offlineVoices.size() + " offline English voice options found. "
                + "Preview to verify any voice pack is installed.");
    }


    /** Only metadata and user preferences are organized; no voice packages are downloaded. */
    private void refreshVisibleVoices() {
        if (voices == null) return;
        visibleVoices.clear();
        int category = voiceFilter == null ? 0 : voiceFilter.getSelectedItemPosition();
        Set<String> favorites = favorites();
        for (Voice voice : offlineVoices) {
            if (VoiceCatalog.matches(category, voice.getLocale().toLanguageTag(),
                    favorites.contains(voice.getName()))) {
                visibleVoices.add(voice);
            }
        }
        ArrayList<String> labels = new ArrayList<>();
        for (Voice voice : visibleVoices) {
            labels.add((favorites.contains(voice.getName()) ? "★ " : "")
                + VoiceCatalog.section(voice.getLocale().toLanguageTag())
                + "  •  " + VoiceCatalog.qualityLabel(voice.getQuality())
                + "\n" + voice.getName());
        }
        if (labels.isEmpty()) labels.add("No offline voices in this category");
        voiceItems = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, labels) {
            @Override public View getView(int index, View previous, android.view.ViewGroup parent) {
                TextView cell = (TextView) super.getView(index, previous, parent);
                cell.setTextSize(13);
                cell.setTextColor(foreground);
                cell.setMinHeight(dp(65));
                cell.setPadding(dp(12), dp(9), dp(12), dp(9));
                cell.setSingleLine(false);
                cell.setMaxLines(4);
                cell.setBackgroundColor(index == voices.getCheckedItemPosition()
                    ? 0xff3b4b67 : 0xff26344b);
                return cell;
            }
        };
        configuring = true;
        voices.setAdapter(voiceItems);
        String selected = getSharedPreferences(PREFS, MODE_PRIVATE)
            .getString("voice", "");
        int position = 0;
        for (int i = 0; i < visibleVoices.size(); i++) {
            if (visibleVoices.get(i).getName().equals(selected)) {
                position = i;
                break;
            }
        }
        if (!visibleVoices.isEmpty()) {
            voices.setItemChecked(position, true);
            voices.setSelection(position);
        } else {
            voices.clearChoices();
        }
        configuring = false;
        if (voiceItems != null) voiceItems.notifyDataSetChanged();
        showVoiceDetails();
    }

    private Set<String> favorites() {
        return new HashSet<>(getSharedPreferences(PREFS, MODE_PRIVATE)
            .getStringSet("favorite_voices", Collections.emptySet()));
    }

    private Voice chosenVoice() {
        int index = voices == null ? -1 : voices.getCheckedItemPosition();
        if (index < 0 || index >= visibleVoices.size()) return null;
        return visibleVoices.get(index);
    }

    private void toggleFavorite() {
        Voice selected = chosenVoice();
        if (selected == null) {
            setStatus("Choose an available voice to favorite.");
            return;
        }
        Set<String> names = favorites();
        boolean added = names.add(selected.getName());
        if (!added) names.remove(selected.getName());
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
            .putStringSet("favorite_voices", names)
            .putString("voice", selected.getName()).apply();
        refreshVisibleVoices();
        setStatus(added ? "Saved to favorites." : "Removed from favorites.");
    }

    private void showVoiceDetails() {
        if (voiceDetails == null) return;
        Voice selected = chosenVoice();
        if (selected == null) {
            voiceDetails.setText("No offline voice in this category. "
                + "Switch to All English or open the setup guide above.");
            return;
        }
        voiceDetails.setText(selected.getName() + "\n"
            + selected.getLocale().getDisplayName(Locale.US) + "  •  "
            + VoiceCatalog.qualityLabel(selected.getQuality())
            + "\nOffline-capable according to the engine. "
            + (favorites().contains(selected.getName()) ? "★ Favorite. " : "")
            + "Actual local playback must be tested.");
    }

    private String enginePackage() {
        try { return speech == null ? null : speech.getDefaultEngine(); }
        catch (Exception ignored) { return null; }
    }

    private boolean launchEngineIntent(String action, boolean needsResult) {
        Intent actionIntent = new Intent(action);
        String engine = enginePackage();
        if (engine == null || engine.trim().isEmpty()) {
            setStatus("No installed voice engine available. Open Android speech settings.");
            return false;
        }
        actionIntent.setPackage(engine);
        try {
            if (needsResult) startActivityForResult(actionIntent, REQUEST_CHECK_VOICE_DATA);
            else startActivity(actionIntent);
            refreshAfterSettings = !needsResult;
            return true;
        } catch (ActivityNotFoundException | SecurityException e) {
            setStatus("Your voice engine has no compatible setup screen. "
                + "Try Android speech settings below.");
            return false;
        }
    }

    private void checkVoiceData(View ignored) {
        if (launchEngineIntent(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA, true)) {
            setStatus("Checking installed voice data with your Android speech engine...");
        }
    }

    private void installVoiceData(View ignored) {
        if (launchEngineIntent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA, false)) {
            setStatus("Opening your voice engine's setup. "
                + "Choose/download an offline pack there if offered. Return and refresh.");
        }
    }

    /** Android skins differ; always offer accessible Settings fallback. */
    private void openTtsSettings(View ignored) {
        String[] actions = {"com.android.settings.TTS_SETTINGS",
            "android.settings.TTS_SETTINGS", Settings.ACTION_ACCESSIBILITY_SETTINGS};
        for (String action : actions) {
            try {
                startActivity(new Intent(action));
                refreshAfterSettings = true;
                setStatus("In Android settings, find Text-to-speech output, "
                    + "choose an engine and download an offline English voice if offered. "
                    + "Return here and tap Refresh.");
                return;
            } catch (ActivityNotFoundException | SecurityException ignoredAction) {
                // Different firmware may only expose the generic Accessibility page.
            }
        }
        setStatus("No supported speech-settings shortcut on this firmware. "
            + "Open Android Settings manually and search for Text-to-speech output.");
    }

    private void refreshSpeechEngine() {
        stopSpeaking();
        speechReady = false;
        offlineVoices.clear();
        visibleVoices.clear();
        if (speech != null) {
            speech.shutdown();
            speech = null;
        }
        setStatus("Rechecking Android text-to-speech voices...");
        initSpeechEngine();
    }

    @Override protected void onResume() {
        super.onResume();
        if (refreshAfterSettings) {
            refreshAfterSettings = false;
            refreshSpeechEngine();
        }
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != REQUEST_CHECK_VOICE_DATA) return;
        if (result == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
            setStatus("The engine reports installed voice data. "
                + "Select an offline English voice and preview it.");
        } else {
            setStatus("Some speech data may be missing or the engine cannot check it. "
                + "Tap Manage/download voice packs, then return and refresh.");
        }
        refreshSpeechEngine();
    }

    private void stopSpeaking() {
        if (speech != null && speechReady) speech.stop();
    }

    private void speak(String text, boolean preview) {
        if (mute != null && mute.isChecked()) {
            setStatus("Muted. Uncheck Mute to hear the voice.");
            return;
        }
        if (!speechReady || speech == null || visibleVoices.isEmpty()) {
            setStatus("No offline voice ready. You can still read the response.");
            return;
        }
        int index = voices.getCheckedItemPosition();
        if (index < 0 || index >= visibleVoices.size()) {
            setStatus("Select an offline voice first.");
            return;
        }
        Voice voice = visibleVoices.get(index);
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
