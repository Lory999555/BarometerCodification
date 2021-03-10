package com.example.myapplication;

/*
 * Copyright (C) 2014 Francesco Azzola - Surviving with Android (http://www.survivingwithandroid.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.app.Activity;
import android.app.Service;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import java.util.LinkedList;


public class PressActivity extends Activity {

    public enum Stato {CAMPIONAMENTO, RILEVAMENTO}

    ;
    private TextView pressView;
    private TextView hzView;
    private Button button;
    private int counter_camp = 0;
    private int counter_ril_out = 0;    //out of range quindi segnale modificato
    private int counter_ril_in = 0;     //in range quindi segnale normale
    private float maxVal = Float.MIN_VALUE;
    private float minVal = Float.MAX_VALUE;
    LinkedList<Float> rilevazioni = new LinkedList<Float>();    // rilevazioni
    private Stato stato = Stato.CAMPIONAMENTO;  //stato dell'app
    private float offset = 0.005f;   //studiarsi qualcosa per sceglierlo meglio sto valore magari si può ricavare direttamente dallo scostamento con la media
    private float pivot;    //media dei rilevamenti (basata sul num_campionamento)
    private int num_capionamento = 300; //num di campionamenti per decidere il pivot, maxVal e minVal
    private int num_rilevazioni = 15;   //num necessario per decidere se si è dentro il range o fuori
    private float data; //variabile che contiene il valore del sensore


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.press_layout);
        pressView = (TextView) findViewById(R.id.pressTxt);
        hzView = (TextView) findViewById(R.id.hz_text);
        button = (Button) findViewById(R.id.button);


        // Look for pressure sensor
        SensorManager snsMgr = (SensorManager) getSystemService(Service.SENSOR_SERVICE);

        Sensor pS = snsMgr.getDefaultSensor(Sensor.TYPE_PRESSURE);

        //vorrei usare l'interpolazione trigonometrica se riesco
        snsMgr.registerListener(new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                //magari cablarlo n una funzione che prende lo stato e basta
                data = event.values[0];
                String s;
                switch (stato) {
                    case CAMPIONAMENTO:
                        //aggiornamento della grafica
                        s = Float.toString(data);
                        pressView.setText(s);
                        hzView.setText("Sto campionando il segnale: " + counter_camp);

                        //logica del campionamento
                        counter_camp++;
                        rilevazioni.addFirst(data);
                        if (maxVal < data)
                            maxVal = data;
                        if (minVal > data)
                            minVal = data;
                        if (counter_camp == num_capionamento) {
                            stato = Stato.RILEVAMENTO;
                            float sum = 0f;
                            for (float rilevazione : rilevazioni) {
                                sum += rilevazione;
                            }
                            rilevazioni.clear();
                            pivot = sum / num_capionamento;

                            //forse non sarebbe male calcolare anche una media dei valori massimi e dei valori minimi per evitare che dei picchi atomici mi disturbino troppo il segnale
                            maxVal = maxVal + offset;
                            minVal = minVal + offset;

                            //potrei anche usare una sorta di euristica (o media pesata) per scegliere i limiti ad esempio far valore la media 60% e il valore del max e min un tot
                        }
                        
                        break;


                    case RILEVAMENTO:
                        //stampare dentro il range o fuori dal range se un tot di rilevazioni superano sta cosa dove sto tot è modulare
                        //accuracy

                        //aggiornamento della grafica
                        s = Float.toString(data);
                        pressView.setText(s);
                        //hzView.setText("sto analizzando");

                        //logica rilevamento
                        //per prova voglio che per cambiare stato da modificato a normale servano 10 rilevazioni (dentro o fuori il range) consecutive
                        if (data > maxVal || data < minVal) {
                            counter_ril_out++;
                            counter_ril_in = 0;

                        } else {
                            counter_ril_in++;
                            counter_ril_out = 0;
                        }
                        if (counter_ril_out >= num_rilevazioni)
                            hzView.setText("segnale modificato");
                        else if (counter_ril_in >= num_rilevazioni)
                            hzView.setText("segnale normale");
                        break;
                }
            }



            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        }, pS, SensorManager.SENSOR_DELAY_FASTEST);

    }

    //per pulizia del codice futuro
    private void calcolaScostamento() {


    }

    @Override
    protected void onResume() {

        super.onResume();

    }


    @Override
    protected void onStart() {
        // TODO Auto-generated method stub
        super.onStart();
    }


    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }
}
