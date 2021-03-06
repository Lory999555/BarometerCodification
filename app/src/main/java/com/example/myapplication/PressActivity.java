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
import android.widget.TextView;

import java.util.LinkedList;
import java.util.concurrent.TimeUnit;


public class PressActivity extends Activity {

    public enum Stato {CAMPIONAMENTO, RILEVAMENTO}

    //grafica
    private TextView pressView;
    private TextView hzView;
    private TextView titleView;

    //logica
    private int counter_camp = 0;
    private int counter_ril_out = 0;    //out of range quindi segnale modificato
    private int counter_ril_in = 0;     //in range quindi segnale normale
    private float maxVal = Float.MIN_VALUE;
    private float minVal = Float.MAX_VALUE;
    LinkedList<Float> rilevazioni = new LinkedList<Float>();    // rilevazioni
    private Stato stato = Stato.CAMPIONAMENTO;  //stato dell'app
    private float offset = 0.025f;   //studiarsi qualcosa per sceglierlo meglio sto valore magari si può ricavare direttamente dallo scostamento con la media
    private float pivot;    //media dei rilevamenti (basata sul num_campionamento)
    private int num_capionamento = 50; //num di campionamenti per decidere il pivot, maxVal e minVal
    private int num_rilevazioni = 15;   //num necessario per decidere se si è dentro il range o fuori
    private int num_ril_rumore = 4; //sono necessarie un tot di rilevazioni consecutive per azzerare il contatore
    private float data; //variabile che contiene il valore del sensore
    private long nowTime;
    private long limitTime;
    private boolean reset_rilevamento = true;
    private boolean reset_campionamento = true;
    private int time_slot = 5;
    private int num_time_slot_before_recompute = 5;
    private int count_time_slot = 0;
    private boolean upOrDown; //true se i sensori rilevano un valore oltre la soglia e false se rilevano un valore al di sotto della soglia (pivot)
    private int countUpDown = 0;
    private int countPivot = 0;     //logica per resettare il valore del pivot se i rilevamenti sono sempre sopra o sotto il pivot
    private int limitPivot =  0;      //num di rilevamenti consecutivi chde servono per far scattare il ricalcolo del pivot
                                    //si potrebbe mettere anche una logica tipo ogni tot time_slot ricalcola il pivot
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.press_layout);
        titleView = (TextView) findViewById(R.id.title_pressure_text);
        pressView = (TextView) findViewById(R.id.pressTxt);
        hzView = (TextView) findViewById(R.id.hz_text);

        SensorManager snsMgr = (SensorManager) getSystemService(Service.SENSOR_SERVICE);
        Sensor pS = snsMgr.getDefaultSensor(Sensor.TYPE_PRESSURE);
        snsMgr.registerListener(new SensorEventListener() { //vorrei usare l'interpolazione trigonometrica se riesco

            @Override
            public void onSensorChanged(SensorEvent event) {
                //magari cablarlo n una funzione che prende lo stato e basta
                data = event.values[0];
                String s;
                switch (stato) {
                    case CAMPIONAMENTO:
                        if (reset_campionamento) {
                            nowTime = System.currentTimeMillis();
                            limitTime = nowTime + TimeUnit.SECONDS.toMillis(time_slot);
                            reset_campionamento=false;

                        }
                        //aggiornamento della grafica
                        s = Float.toString(data);
                        pressView.setText(s);
                        hzView.setText("Sto campionando il segnale: \n" + counter_camp);

                        //logica del campionamento
                        counter_camp++;
                        rilevazioni.addFirst(data);
                        if (maxVal < data)
                            maxVal = data;
                        if (minVal > data)
                            minVal = data;
                        nowTime = System.currentTimeMillis();
                        if (nowTime >= limitTime) {
                            stato = Stato.RILEVAMENTO;

                            //calcolaScostamento();
                            calcolaScostamento_3();

                            titleView.setText(" pressure (mbar) \n pivot : " + pivot + "\n maxVal : " + maxVal + "\n minVal : " + minVal);

                            //potrei anche usare una sorta di euristica (o media pesata) per scegliere i limiti ad esempio far valore la media 60% e il valore del max e min un tot
                        }

                        break;


                    case RILEVAMENTO:
                        if (reset_rilevamento) {
                            nowTime = System.currentTimeMillis();
                            limitTime = nowTime + TimeUnit.SECONDS.toMillis(time_slot);
                            counter_ril_out = 0;
                            counter_ril_in = 0;
                            reset_rilevamento = false;
                            countUpDown = 0;
                            count_time_slot++;
                            if (count_time_slot == num_time_slot_before_recompute){
                                count_time_slot=0;
                                stato = Stato.CAMPIONAMENTO;
                                reset_campionamento=true;
                                counter_camp=0;
                            }
                        }
                        //stampare dentro il range o fuori dal range se un tot di rilevazioni superano sta cosa dove sto tot è modulare
                        //accuracy

                        //aggiornamento della grafica
                        s = Float.toString(data);
                        pressView.setText(s);
                        //hzView.setText("sto analizzando");

                        logicaRilevamento_3();
                        break;
                }
            }


            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        }, pS, SensorManager.SENSOR_DELAY_FASTEST);

    }



    private void calcolaScostamento() {
        float sum = 0f;
        for (float rilevazione : rilevazioni) {
            sum += rilevazione;
        }
        rilevazioni.clear();
        pivot = sum / num_capionamento;

        minVal = minVal - offset;
        maxVal = maxVal + offset;

    }


    //per pulizia del codice futuro
    //seconda logica implementata
    private void calcolaScostamento_2() {
        float sum = 0f;
        for (float rilevazione : rilevazioni) {
            sum += rilevazione;
        }
        rilevazioni.clear();
        pivot = sum / num_capionamento;
        //offset = (maxVal - minVal) / 2;
        minVal = pivot - offset;
        maxVal = pivot + offset;
        if (data > pivot)
            upOrDown = true;
        else
            upOrDown = false;
        //se rimane questa la logica potrei anche contare il numero di volte che si cambia il massimo ed il minimo e dare più peso ad un o ad un'altro

    }

    private void calcolaScostamento_3() {
        float sum = 0f;
        for (float rilevazione : rilevazioni) {
            sum += rilevazione;
        }
        rilevazioni.clear();
        pivot = sum / counter_camp;
        //offset = (maxVal - minVal) / 2;
        minVal = pivot - offset;
        maxVal = pivot + offset;
        if (data > pivot)
            upOrDown = true;
        else
            upOrDown = false;
        //se rimane questa la logica potrei anche contare il numero di volte che si cambia il massimo ed il minimo e dare più peso ad un o ad un'altro

    }

    private void logicaRilevamento() {
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


    }

    private void logicaRilevamento_2() {
        //logica rilevamento
        //per prova voglio che per cambiare stato da modificato a normale servano 10 rilevazioni (dentro o fuori il range) consecutive
        if (data > maxVal || data < minVal) {
            counter_ril_out++;

        } else {
            counter_ril_in++;
        }
        if (System.currentTimeMillis() > limitTime) {
            reset_rilevamento = true;
            if (counter_ril_out >= counter_ril_in)
                hzView.setText("segnale modificato");
            else
                hzView.setText("segnale normale");
        }
    }

    //time_slot
    //dai vari esperimenti si nota come cambia il valore medio della pressione quindi sarebbe buono ricalcolare il pivot dopo un tot di rilevamenti che passano magari i limiti.
    private void logicaRilevamento_3() {
        //logica rilevamento
        //per prova voglio che per cambiare stato da modificato a normale servano 10 rilevazioni (dentro o fuori il range) consecutive
        if (data > pivot && upOrDown == false) {
            countUpDown++;
            upOrDown = true;

        } else if (data < pivot && upOrDown == true) {
            countUpDown++;
            upOrDown = false;
        }
        if (System.currentTimeMillis() >= limitTime) {
            reset_rilevamento = true;
            hzView.setText("countUpDown : " + countUpDown);
        }
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
