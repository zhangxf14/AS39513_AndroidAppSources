/*
 *****************************************************************************
 * Copyright by ams AG                                                       *
 * All rights are reserved.                                                  *
 *                                                                           *
 * IMPORTANT - PLEASE READ CAREFULLY BEFORE COPYING, INSTALLING OR USING     *
 * THE SOFTWARE.                                                             *
 *                                                                           *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS       *
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT         *
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS         *
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT  *
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,     *
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT          *
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,     *
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY     *
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT       *
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE     *
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.      *
 *****************************************************************************
 */
 /* @file AS39513AppActivity.java
 *
 *  @author Florian Hofer (florian.hofer@ams.com)
 *
 *  @brief Main Activity for AS39513 Application
 *
 */
package com.ams.as39513App;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcV;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ams.as39513.AS39513;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import java.io.IOException;

public class AS39513AppActivity extends ActionBarActivity {
    private static final String TAG = "AS39513AppActivity";
    private static final int color_red = Color.argb(0xFF,0xDF,0x53,0x53);               // AMS Red
    private static final int color_graph = Color.argb(0xFF,0,117,176);                  // AMS Blue
    private static final int color_inactive = Color.argb(0xFF, 172, 178, 183);          // AMS Gray

    private NfcV nfcv;
    private TextView textView_uid_value;
    private TextView textView_temperature_value;
    private TextView textView_batteryVoltage_value;
    private TextView externalSensorTextView;
    private TextView textView_loggingForm_value;
    private TextView textView_storageRule_value;
    private TextView textView_interval_value;
    private TextView textView_sensor_value;
    private TextView textView_batteryCheck_value;
    private TextView textView_txhilim_value;
    private TextView textView_thilim_value;
    private TextView textView_tlolim_value;
    private TextView textView_txlolim_value;
    private TextView textView_measStatus_value;
    private TextView textView_measCnt_value;
    private TextView textView_status_value;
    private Handler handler = new Handler();
    private boolean data_rate_low = false;

    /** variables for graph */
    private XYSeries series;
    private XYMultipleSeriesRenderer mRenderer;
    private XYSeriesRenderer mSeriesRenderer;
    private XYMultipleSeriesDataset dataset;
    private int currentChartPosition = 0;

    protected NfcAdapter mNfcAdapter;
    protected PendingIntent mPendingIntent;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {    	
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        textView_uid_value = (TextView) findViewById(R.id.textView_uid_value);
        textView_temperature_value = (TextView) findViewById(R.id.textView_temperature_value);
        textView_batteryVoltage_value = (TextView) findViewById(R.id.textView_batteryVoltage_value);
        // externalSensorTextView = (TextView) findViewById(R.id.externalSensorTextView);
        textView_loggingForm_value = (TextView) findViewById(R.id.textView_loggingForm_value);
        textView_storageRule_value = (TextView) findViewById(R.id.textView_storageRule_value);
        textView_interval_value = (TextView) findViewById(R.id.textView_interval_value);
        textView_sensor_value = (TextView) findViewById(R.id.textView_sensor_value);
        textView_batteryCheck_value = (TextView) findViewById(R.id.textView_batteryCheck_value);
        textView_txhilim_value = (TextView) findViewById(R.id.textView_txhilim_value);
        textView_thilim_value = (TextView) findViewById(R.id.textView_thilim_value);
        textView_tlolim_value = (TextView) findViewById(R.id.textView_tlolim_value);
        textView_txlolim_value = (TextView) findViewById(R.id.textView_txlolim_value);
        textView_measStatus_value = (TextView) findViewById(R.id.textView_measStatus_value);
        textView_measCnt_value = (TextView) findViewById(R.id.textView_measCnt_value);
        textView_status_value = (TextView) findViewById(R.id.textView_status);

        textView_status_value.setTextColor(color_red);
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        onNewIntent(getIntent());

        initializeGraph();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.datarate:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);

                builder.setTitle("Choose Tag Data Rate");
                CharSequence[] items = {"High","Low"};
                builder.setSingleChoiceItems(items, (data_rate_low? 1 : 0) , new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1)
                    {
                        switch (arg1)
                        {
                            case 0:
                                data_rate_low = false;
                                break;
                            case 1:
                                data_rate_low = true;
                                break;
                        }
                    }
                });
                
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int which)
                    {
                        // here you can add functions
                    }
                });

                AlertDialog alertDialog = builder.create();
                alertDialog.show();
                return true;
            case R.id.help:
                builder = new AlertDialog.Builder(this);

                builder.setTitle("Help");
                builder.setMessage("Place the phone on top of a AS39513 Tag. Some basic tag information along with the current temperature will automatically be read out and displayed in a graph.\n\nLow data rate may be chosen in the app settings. Please be aware that only a few phones support that.\n\nPredependencies: \nNFC enabled in the Android settings.\n\nVersion 1.0.1");
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int which)
                    {
                        // here you can add functions
                    }
                });

                alertDialog = builder.create();
                alertDialog.show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void initializeGraph ()
    {
        dataset = new XYMultipleSeriesDataset();
        series = new XYSeries("");

        // Now we create the renderer
        mSeriesRenderer = new XYSeriesRenderer();
        mSeriesRenderer.setLineWidth(7);
        mSeriesRenderer.setColor(color_graph);

        // Include low and max value
        mSeriesRenderer.setDisplayBoundingPoints(true);

        // we add point markers
        mSeriesRenderer.setPointStyle(PointStyle.CIRCLE);
        mSeriesRenderer.setPointStrokeWidth(7);

        mRenderer = new XYMultipleSeriesRenderer();
        mRenderer.addSeriesRenderer(mSeriesRenderer);

        mRenderer.setShowLegend(false);

        // We want to avoid black border
        mRenderer.setMarginsColor(Color.argb(0x00, 0xff, 0x00, 0x00));
        // transparent margins
        // Disable Pan on two axis
        mRenderer.setPanEnabled(false, false);
        mRenderer.setShowGrid(true);

        mRenderer.setLabelsTextSize(50);
        mRenderer.setLabelsColor(Color.BLACK);
        mRenderer.setYLabelsAlign(Paint.Align.LEFT);

        dataset.addSeries(series);
        GraphicalView chartView = ChartFactory.getLineChartView(this, dataset, mRenderer);

        LinearLayout chart = (LinearLayout)findViewById(R.id.graph_temp);
        chart.addView(chartView, 0);

        addGraphData(0.00);
    }

    private void setGraphInactive ()
    {
        if(mSeriesRenderer == null)
            return;

        // set graph color to inactive
        mSeriesRenderer.setColor(color_inactive);
        // update layout
        GraphicalView chartView = ChartFactory.getLineChartView(this, dataset, mRenderer);
        LinearLayout chart = (LinearLayout) findViewById(R.id.graph_temp);
        chart.addView(chartView, 0);
    }

    private void addGraphData (double value)
    {
        if(mSeriesRenderer == null)
            return;

        if((value != 0.00) || currentChartPosition>0)
        {
            // shift chart data to the left if final position reached
            if(currentChartPosition == 11)
            {
                XYSeries series_new = new XYSeries("");
                int i;
                for (i = 1; i < 11; i++)
                {
                    series_new.add(i - 1, series.getY(i));
                }
                series.clear();
                for (i = 0; i < 10; i++)
                    series.add(i, series_new.getY(i));
            }

            // add new data point to the right
            if (currentChartPosition < 11)
            {
                series.add(currentChartPosition, Double.valueOf(value));
                currentChartPosition++;
            }
            else
            {
                series.add(10, Double.valueOf(value));
            }

            // set new margin above and below chart data
            mRenderer.setYAxisMax(series.getMaxY() + 1);
            mRenderer.setYAxisMin(series.getMinY() - 1);

            int i;
            mRenderer.clearXTextLabels();
            mRenderer.setXLabels(0);
            if(currentChartPosition == 11)
            {
                mRenderer.addXTextLabel(0,"-10");
                mRenderer.addXTextLabel(2,"-8");
                mRenderer.addXTextLabel(4,"-6");
                mRenderer.addXTextLabel(6,"-4");
                mRenderer.addXTextLabel(8,"-2");
                mRenderer.addXTextLabel(10,"t");
            }
            else if(series.getItemCount()>1 && series.getItemCount()<7)
            {
                for(i=0;i<series.getItemCount();i++)
                {
                    mRenderer.addXTextLabel((double) i-1, String.format("-%d", series.getItemCount()-i));
                }
                mRenderer.addXTextLabel(0,String.format("-%d",series.getItemCount()-1));
                mRenderer.addXTextLabel(series.getItemCount()-1,"t");
            }
            else if(series.getItemCount()>1)
            {
                for(i=0;i<series.getItemCount();i+=2)
                {
                    if(series.getItemCount()%2!=0 && i==2)
                        i--;
                    mRenderer.addXTextLabel((double) i-1, String.format("-%d", series.getItemCount()-i));
                }
                mRenderer.addXTextLabel(0,String.format("-%d",series.getItemCount()-1));
                mRenderer.addXTextLabel(series.getItemCount()-1,"t");
            }
        }
        mSeriesRenderer.setColor(color_graph);

        // update layout
        GraphicalView chartView = ChartFactory.getLineChartView(this, dataset, mRenderer);
        LinearLayout chart = (LinearLayout) findViewById(R.id.graph_temp);
        chart.addView(chartView, 0);
    }

    @Override
    public void onResume() {
        super.onResume();
        mNfcAdapter.enableForegroundDispatch(this, mPendingIntent, null, null);
    }

    @Override
    public void onPause() {
        super.onPause();
        mNfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    public void startActivity (Intent intent) {
        Log.i(TAG, "startActivity()");
        handleIntent(intent);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Log.i(TAG, "onNewIntent()");
        handleIntent(intent);
    }

    protected void handleIntent(Intent intent) {
        if (intent.hasExtra(NfcAdapter.EXTRA_TAG)) {
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            updateDisplayedUid(tag.getId());

            nfcv = android.nfc.tech.NfcV.get(tag);
            if (nfcv == null) {
                Log.e(TAG, "NfcV technology not supported\n");
                return;
            } else {
                Log.i(TAG, "NfcV technology supported\n");
            }

            try {
                Log.i(TAG, "NfcV trying to connect... ");
                nfcv.connect();
                Log.i(TAG, "done");
            } catch (IOException e) {
                Log.e(TAG, "Failed to connect to NfcV Technology", e);
                return;
            }

            byte[] uid = tag.getId();
            int compare0 = uid[7];
            int compare1 = uid[6];
            if(compare0<0)
                compare0+=256;
            if(compare1<0)
                compare1+=256;
            if(compare0==0xE0 && compare1==0x36)
            {
                handler.removeCallbacks(updateSensorData);
                handler.postDelayed(updateSensorData, 500);
            }
            else
            {
                setTagNotSupported();
            }
        }
    }

    /**
     * \brief Update the displayed UID
     *
     * Updates the displayed UID from a byte array from tag.getId().
     * Corrects the byte order of the UID to MSB first.
     *
     * @param uid UID retrieved from tag.getId()
     */
    private void updateDisplayedUid(byte[] uid) {
        String uidString = new String();
        for (int index = uid.length - 1; index >= 0; --index)
            uidString += String.format("%02X", uid[index]);
        updateDisplayedUid(uidString);
    }

    private void setTagAvailable()
    {
        textView_uid_value.setTextColor(Color.BLACK);
        textView_temperature_value.setTextColor(Color.BLACK);
        textView_batteryVoltage_value.setTextColor(Color.BLACK);
        // externalSensorTextView.setTextColor(Color.BLACK);
        textView_loggingForm_value.setTextColor(Color.BLACK);
        textView_storageRule_value.setTextColor(Color.BLACK);
        textView_interval_value.setTextColor(Color.BLACK);
        textView_sensor_value.setTextColor(Color.BLACK);
        textView_batteryCheck_value.setTextColor(Color.BLACK);
        textView_txhilim_value.setTextColor(Color.BLACK);
        textView_thilim_value.setTextColor(Color.BLACK);
        textView_tlolim_value.setTextColor(Color.BLACK);
        textView_txlolim_value.setTextColor(Color.BLACK);
        textView_measStatus_value.setTextColor(Color.BLACK);
        textView_measCnt_value.setTextColor(Color.BLACK);

        textView_status_value.setText("");
    }

    private void setTagNotSupported()
    {
        textView_temperature_value.setText("");
        textView_temperature_value.setText("N/A");
        textView_batteryVoltage_value.setText("N/A");
        // externalSensorTextView.setText("N/A");
        textView_loggingForm_value.setText("N/A");
        textView_storageRule_value.setText("N/A");
        textView_interval_value.setText("N/A");
        textView_sensor_value.setText("N/A");
        textView_batteryCheck_value.setText("N/A");
        textView_txhilim_value.setText("N/A");
        textView_thilim_value.setText("N/A");
        textView_tlolim_value.setText("N/A");
        textView_txlolim_value.setText("N/A");
        textView_measStatus_value.setText("N/A");
        textView_measCnt_value.setText("N/A");
        textView_status_value.setText("No AS39513 Tag");

        setGraphInactive();
    }

    private void setTagNotAvailable()
    {
        textView_uid_value.setTextColor(color_inactive);
        textView_temperature_value.setTextColor(color_inactive);
        textView_batteryVoltage_value.setTextColor(color_inactive);
        // externalSensorTextView.setTextColor(color_inactive);
        textView_loggingForm_value.setTextColor(color_inactive);
        textView_storageRule_value.setTextColor(color_inactive);
        textView_interval_value.setTextColor(color_inactive);
        textView_sensor_value.setTextColor(color_inactive);
        textView_batteryCheck_value.setTextColor(color_inactive);
        textView_txhilim_value.setTextColor(color_inactive);
        textView_thilim_value.setTextColor(color_inactive);
        textView_tlolim_value.setTextColor(color_inactive);
        textView_txlolim_value.setTextColor(color_inactive);
        textView_measStatus_value.setTextColor(color_inactive);
        textView_measCnt_value.setTextColor(color_inactive);
        textView_status_value.setText("Tag lost");

        setGraphInactive();
    }

    private void updateDisplayedUid(String uid) {
        textView_uid_value.setText(uid);
    }

    /**
     * Update the displayed temperature sensor data.
     *
     * @param code Temperature measurement result (ADC value).
     * @param temperature Temperauter measurement result (degree celsius).
     */
    private void updateDisplayedTemperature(int code, double temperature)
    {
        if(code == 0xA3)
        {
            textView_temperature_value.setTextColor(Color.RED);
            textView_temperature_value.setText("ADC Error");
        }
        else
        {
            textView_temperature_value.setTextColor(Color.BLACK);
            textView_temperature_value.setText(String.format("%.1f \u00B0C", temperature));
            addGraphData(temperature);
        }
    }

    /**
     * Update the displayed external sensor data.
     *
     * @param code External sensor measurement result (ADC value).
     * @param voltage External sensor measurement result (Voltage).
     */
    private void updateDisplayedExternalSensor(int code, double voltage) {
        // externalSensorTextView.setText(String.format("%.2f V", voltage));
    }

    /**
     * Update the displayed battery data.
     *
     * @param code Battery measurement result (ADC value).
     * @param batteryVoltage Battery measurement result (Voltage).
     */
    private void updateDisplayedBattery(int code, double batteryVoltage) {
        if(code == 0xA3 || code == 0x00)
            textView_batteryVoltage_value.setText("No Battery");
        else
            textView_batteryVoltage_value.setText(String.format("%.2f V", batteryVoltage));
    }

    /**
     * Update the displayed logging mode.
     */
    private void updateDisplayedLogMode(AS39513.LogMode logMode) {
        switch (logMode.logmd) {
            case 0: textView_loggingForm_value.setText("Dense"); break;
            case 1: textView_loggingForm_value.setText("Interrupt"); break;
            case 2: textView_loggingForm_value.setText("Out of Limits"); break;
            case 3: textView_loggingForm_value.setText("Limitcrossing"); break;
            default:
                Log.w(TAG,String.format("Unhandled LogMode: logmd = %d",(int)logMode.logmd));
        }

        switch (logMode.strmd) {
            case 0: textView_storageRule_value.setText("Normal"); break;
            case 1: textView_storageRule_value.setText("Rolling"); break;
            default:
                Log.w(TAG,String.format("Unhandled LogMode: strmd = %d",(int)logMode.strmd));
        }

        textView_interval_value.setText(String.valueOf(logMode.logint + 1));
        if(logMode.tsmeas == 0x01 && logMode.exmeas == 0x01)
            textView_sensor_value.setText("Both");
        else if(logMode.tsmeas == 0x01)
            textView_sensor_value.setText("Temperature");
        else if(logMode.exmeas == 0x01)
            textView_sensor_value.setText("External");
        else
            Log.w(TAG,String.format("Unhandled LogMode: tsmeas = %d, exmeas = %d",(int)logMode.tsmeas,(int)logMode.exmeas));

        if (logMode.batchk == 0x01)
            textView_batteryCheck_value.setText("Enabled");
        else
            textView_batteryCheck_value.setText("Disabled");
    }

    /**
     * Update the displayed log limits.
     */
    private void udpateDisplayedLogLimits(AS39513.LogLimits logLimits) {
        AS39513 as39513 = new AS39513(nfcv,data_rate_low);
        try
        {
            textView_txhilim_value.setText(String.format("%.1f \u00B0C", as39513.convertTemperatureCodeToCelsius(logLimits.txhilim<<2)));
            textView_thilim_value.setText(String.format("%.1f \u00B0C", as39513.convertTemperatureCodeToCelsius(logLimits.thilim<<2)));
            textView_tlolim_value.setText(String.format("%.1f \u00B0C", as39513.convertTemperatureCodeToCelsius(logLimits.tlolim<<2 | 0x03)));
            textView_txlolim_value.setText(String.format("%.1f \u00B0C", as39513.convertTemperatureCodeToCelsius(logLimits.txlolim<<2 | 0x03)));
        }
        catch(java.io.IOException e)
        {
        }
    }

    private void updateDisplayedMeasurementStatus(AS39513.MeasurementStatus measurementStatus) {
        if (measurementStatus.active)
            textView_measStatus_value.setText("active");
        else
            textView_measStatus_value.setText("inactive");

        textView_measCnt_value.setText(String.valueOf(measurementStatus.meascnt));
    }
    
    private Runnable updateSensorData = new Runnable() {
        public void run() {
            if ((nfcv == null) || !nfcv.isConnected()) {
                Log.i(TAG, "No NfcV connection available or connection lost");
                setTagNotAvailable();
                return;
            }
            else
            {
                setTagAvailable();
            }

            int startTime = 0;
            int delayTime = 0;
            AS39513.LogLimits logLimits = new AS39513.LogLimits();
            AS39513.LogMode logMode = new AS39513.LogMode();
            AS39513 as39513 = new AS39513(nfcv,data_rate_low);

            try {
                AS39513.CalibrationData calibrationData = new AS39513.CalibrationData();
                as39513.getCalibrationData(calibrationData);
                //as39513.setInternalCalibrationData(calibrationData);
            } catch (IOException e) {
                Log.e(TAG, "Failed to update internal calibration data: " + e.getMessage());
                Log.e(TAG, "Skipping updateSensorData cycle");
                handler.postDelayed(updateSensorData, 500);
                return;
            }

            try {
                as39513.getMeasurementSetup(startTime, logLimits, logMode, delayTime);
                updateDisplayedLogMode(logMode);
                udpateDisplayedLogLimits(logLimits);
            } catch (IOException e) {
                Log.e(TAG, "Failed to read measurement setup: " + e.getMessage());
                Log.e(TAG, "Skipping updateSensorData cycle");
                handler.postDelayed(updateSensorData, 500);
                return;
            }

            try {
                AS39513.MeasurementStatus measurementStatus = new AS39513.MeasurementStatus();
                AS39513.LimitCounter limitCounter = new AS39513.LimitCounter();
                as39513.getLogState(measurementStatus, limitCounter);
                updateDisplayedMeasurementStatus(measurementStatus);
            } catch (IOException e) {
                Log.e(TAG, "Failed to read log state: " + e.getMessage());
                Log.e(TAG, "Skipping updateSensorData cycle");
                handler.postDelayed(updateSensorData, 500);
                return;
            }

            try {
                logMode.tsmeas = 1;
                logMode.exmeas = 0;
                // AS39513.setLogMode(logLimits, logMode);
                int temperatureCode = as39513.getTemperature();
                double temperature = as39513.convertTemperatureCodeToCelsius(temperatureCode);
                updateDisplayedTemperature(temperatureCode, temperature);
            } catch (IOException e) {
                Log.e(TAG, "Failed to read temepratuer value:" + e.getMessage());
                Log.e(TAG, "Skipping updateSensorData cycle");
                handler.postDelayed(updateSensorData, 500);
                return;
            }

            try {
                int batteryCode = as39513.getBatteryLevel();
                double batteryVoltage = as39513.convertBatteryCodeToVoltage(batteryCode);
                updateDisplayedBattery(batteryCode, batteryVoltage);
            } catch (IOException e) {
                Log.e(TAG, "Failed to read battery voltage: " + e.getMessage());
                Log.e(TAG, "Skipping updateSensorData cycle");
                handler.postDelayed(updateSensorData, 500);
                return;
            }

            try {
                byte data[] = as39513.readMultipleBlocks((char)0x00,(char)0x03);
                Log.i(TAG, "User memory block 0:" + data.toString());
            } catch (IOException e) {
                Log.e(TAG, "Failed to read user memory: " + e.getMessage());
                Log.e(TAG, "Skipping updateSensorData cycle");
                handler.postDelayed(updateSensorData, 500);
                return;
            }
			
			/* External sensor readout is currently not supported
			try {
    			logMode.sensor = AS39513.LogMode.Sensor.EXTERNAL;
    			AS39513.setLogMode(logLimits, logMode);
    			int externalSensorCode = AS39513.getTemperature();
    			double externalSensorValue = AS39513.convertTemperatureCodeToCelsius(externalSensorCode);
    			updateDisplayedExternalSensor(externalSensorCode, externalSensorValue);
    		} catch (IOException e)  {
    			Log.e(TAG, "Failed to read external sensor values:" +  e.getMessage());
    		}*/

            handler.postDelayed(updateSensorData, 600);
        }
    };
}