package com.example.myapplication;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

public class SoundActivity extends Activity {

    private Button button;

    // originally from http://marblemice.blogspot.com/2010/04/generate-and-play-tone-in-android.html
    // and modified by Steve Pomeroy <steve@staticfree.info>
    private final int duration = 10; // seconds
    private final int sampleRate = 8000;
    private final int numSamples = duration * sampleRate;
    private final double sample[] = new double[numSamples];
    private double freqOfTone; // hz
    private final byte generatedSnd[] = new byte[2 * numSamples];
    Handler handler = new Handler();
    private SeekBar freqBar;
    private TextView progess;
    private EditText input;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sound_layout);
        //qui ci sono componenti in più ma non da errore, eliminarle e verificare perchè non è un errore, è possibile ottenere componenti da aggiornare di altre view?
        button = (Button) findViewById(R.id.button);
        freqBar = (SeekBar) findViewById(R.id.seekBar);
        progess = (TextView) findViewById(R.id.progress);
        input = (EditText) findViewById(R.id.progress_input);

        button.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {

                button.setText(input.getText());    //prendere il testo con valueOf
                /*
                int val = Integer.valueOf(String.valueOf(input.getText()));

                if(val > 0 & val < 100)
                    freqBar.setProgress(val);  //è bruttissimo

                */
                genTone();
                playSound();
            }
        });


        freqBar.setMax(100);
        freqBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                // TODO Auto-generated method stub
                //progess = (TextView)findViewById(R.id.progress);
                progess.setText(String.valueOf(progress));
            }
        });
    }


    @Override
    protected void onResume() {

        super.onResume();


        // Use a new tread as this can take a while
            /*
            final Thread thread = new Thread(new Runnable() {
                public void run() {
                    genTone();
                    handler.post(new Runnable() {

                        public void run() {
                            playSound();
                        }
                    });
                }
            });
            thread.start();
             */
    }


    @Override
    protected void onStart() {
        // TODO Auto-generated method stub
        super.onStart();
    }

    void genTone() {
        // fill out the array
        freqOfTone = (double) freqBar.getProgress();    //vedere se esite un metodo più fine
        System.out.println(freqOfTone);

        for (int i = 0; i < numSamples; ++i) {
            sample[i] = Math.sin(2 * Math.PI * i / (sampleRate / freqOfTone));
        }

        // convert to 16 bit pcm sound array
        // assumes the sample buffer is normalised.
        int idx = 0;
        for (final double dVal : sample) {
            // scale to maximum amplitude
            final short val = (short) ((dVal * 32767));
            // in 16 bit wav PCM, first byte is the low order byte
            generatedSnd[idx++] = (byte) (val & 0x00ff);
            generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);

        }
    }

    void playSound() {
        final AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                sampleRate, AudioFormat.CHANNEL_OUT_MONO,//CHANNEL_CONFIGURATION_MONO,
                AudioFormat.ENCODING_PCM_16BIT, numSamples,
                AudioTrack.MODE_STATIC);
        audioTrack.write(generatedSnd, 0, generatedSnd.length);
        audioTrack.play();
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

}
