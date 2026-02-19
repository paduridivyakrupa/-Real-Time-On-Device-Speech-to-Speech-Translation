package com.example.speechtranslator;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    Button btnStart, btnStop;
    TextView txtLangA, txtLangB, txtStatus;
    Spinner spinnerLangA, spinnerLangB;

    SpeechRecognizer speechRecognizer;
    Intent speechIntent;
    Translator translator;
    TextToSpeech textToSpeech;

    String finalText = "";
    boolean userPressedStop = false;

    String langACode = TranslateLanguage.ENGLISH;
    String langBCode = TranslateLanguage.TELUGU;

    Locale asrLocale = Locale.ENGLISH;
    Locale ttsLocale = new Locale("te", "IN");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // UI
        btnStart = findViewById(R.id.btnStart);
        btnStop = findViewById(R.id.btnStop);
        txtLangA = findViewById(R.id.txtLangA);
        txtLangB = findViewById(R.id.txtLangB);
        txtStatus = findViewById(R.id.txtStatus);
        spinnerLangA = findViewById(R.id.spinnerLangA);
        spinnerLangB = findViewById(R.id.spinnerLangB);

        // ---------- SPINNER ----------
        String[] languages = {"English", "Telugu", "Hindi"};

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_item,
                languages
        ) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView tv = (TextView) super.getView(position, convertView, parent);
                tv.setTextColor(Color.BLACK);
                tv.setTextSize(16);
                return tv;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView tv = (TextView) super.getDropDownView(position, convertView, parent);
                tv.setTextColor(Color.BLACK);
                tv.setBackgroundColor(Color.parseColor("#EEEEEE"));
                return tv;
            }
        };

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLangA.setAdapter(adapter);
        spinnerLangB.setAdapter(adapter);

        // ---------- INPUT LANGUAGE (ASR) ----------
        spinnerLangA.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                if (position == 0) {
                    langACode = TranslateLanguage.ENGLISH;
                    asrLocale = Locale.ENGLISH;
                } else if (position == 1) {
                    langACode = TranslateLanguage.TELUGU;
                    asrLocale = new Locale("te", "IN");
                } else {
                    langACode = TranslateLanguage.HINDI;
                    asrLocale = new Locale("hi", "IN");
                }
                setupTranslator();
            }

            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // ---------- OUTPUT LANGUAGE (TTS) ----------
        spinnerLangB.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                if (position == 0) {
                    langBCode = TranslateLanguage.ENGLISH;
                    ttsLocale = Locale.ENGLISH;
                } else if (position == 1) {
                    langBCode = TranslateLanguage.TELUGU;
                    ttsLocale = new Locale("te", "IN");
                } else {
                    langBCode = TranslateLanguage.HINDI;
                    ttsLocale = new Locale("hi", "IN");
                }

                setupTranslator();
                if (textToSpeech != null) {
                    textToSpeech.setLanguage(ttsLocale);
                }
            }

            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // ---------- SPEECH RECOGNIZER ----------
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

        // ---------- TTS ----------
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(ttsLocale);
            }
        });

        // ---------- LISTENER ----------
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) {
                txtStatus.setText("Listening...");
            }
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEvent(int eventType, Bundle params) {}

            @Override
            public void onEndOfSpeech() {
                if (!userPressedStop) speechRecognizer.startListening(speechIntent);
            }

            @Override
            public void onError(int error) {
                if (!userPressedStop) speechRecognizer.startListening(speechIntent);
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches =
                        results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    finalText = (finalText + " " + matches.get(0)).trim();
                    txtLangA.setText(finalText);
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                ArrayList<String> partial =
                        partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (partial != null && !partial.isEmpty()) {
                    txtLangA.setText(partial.get(0));
                }
            }
        });

        // ---------- BUTTONS ----------
        btnStart.setOnClickListener(v -> {
            userPressedStop = false;
            finalText = "";
            txtLangA.setText("");
            txtLangB.setText("");
            txtStatus.setText("Listening...");

            speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, asrLocale);
            speechRecognizer.startListening(speechIntent);
        });

        btnStop.setOnClickListener(v -> {
            userPressedStop = true;
            speechRecognizer.cancel();
            txtStatus.setText("Translating...");
            translateAndSpeak(finalText);
        });

        setupTranslator();
    }

    private void setupTranslator() {
        if (translator != null) translator.close();

        TranslatorOptions options =
                new TranslatorOptions.Builder()
                        .setSourceLanguage(langACode)
                        .setTargetLanguage(langBCode)
                        .build();

        translator = Translation.getClient(options);
        translator.downloadModelIfNeeded();
    }

    private void translateAndSpeak(String text) {
        if (text == null || text.trim().isEmpty()) return;

        translator.translate(text)
                .addOnSuccessListener(translated -> {
                    txtLangB.setText(translated);
                    txtStatus.setText("Done");
                    textToSpeech.speak(translated,
                            TextToSpeech.QUEUE_FLUSH,
                            null,
                            "TTS");
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) speechRecognizer.destroy();
        if (textToSpeech != null) textToSpeech.shutdown();
        if (translator != null) translator.close();
    }
}
