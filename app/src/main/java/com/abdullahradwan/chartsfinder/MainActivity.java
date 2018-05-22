package com.abdullahradwan.chartsfinder;

// Import main libraries
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.StrictMode;
import android.support.constraint.Group;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnErrorListener;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    // Define main variables
    private EditText icaoEdit;

    private Group pdfGroup;

    private ConfigEditor config;

    private String spinnerIcaoCode;

    private Spinner chartSpinner;

    private PDFView pdfView;

    private Downloader downloader;

    private static final ArrayList<String> chartSpinnerItems = new ArrayList<>();

    private static ArrayAdapter<String> chartSpinnerAdapter;

    static HashMap<String, ArrayList<FilesItems>> filesMap = new HashMap<>();

    static ArrayAdapter<String> fileSpinnerAdapter;

    static ArrayList<String> fileSpinnerItems = new ArrayList<>();

    static String[] icaoCode;

    static String path;

    static ArrayList<ResourcesItem> resources = new ArrayList<>();

    static ArrayList<FilesItems> files = new ArrayList<>();

    static boolean showNotify = true;

    static boolean openChart = false;

    static boolean internalPDF = true;

    static boolean modifyRes = false;

    // On start
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // Read config
        config = new ConfigEditor(getPreferences(Context.MODE_PRIVATE));

        config.readConfig();

        // Show activity
        setContentView(R.layout.activity_main);

        // Set Toolbar as action bar
        setSupportActionBar((Toolbar) findViewById(R.id.toolBar));

        // Set main objects
        icaoEdit = findViewById(R.id.icaoEdit);

        pdfGroup = findViewById(R.id.pdfGroup);

        pdfView = findViewById(R.id.pdfView);

        Spinner fileSpinner = findViewById(R.id.fileSpinner);

        chartSpinner = findViewById(R.id.chartSpinner);

        // Set file spinner

        // Make adapter with list
        fileSpinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, fileSpinnerItems);

        // Set spinner style
        fileSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Link adapter with spinner
        fileSpinner.setAdapter(fileSpinnerAdapter);

        // When clicked
        fileSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                final int filePos = position;

                // Get chart from files ArrayList

                // If chart type is normal
                if(files.get(filePos).chartType.equals("Normal")) {

                    // Hide chart spinner
                    chartSpinner.setVisibility(View.GONE);

                    // Load pdf file from list
                    pdfView.fromFile(files.get(filePos).chartFile)
                            // If file was removed
                            .onError(new OnErrorListener() {
                                @Override
                                public void onError(Throwable t) {

                                    // Remove from file list
                                    files.remove(filePos);

                                    // Remove from spinner list
                                    fileSpinnerItems.remove(filePos);

                                    // Notify list changed
                                    fileSpinnerAdapter.notifyDataSetChanged();
                                }
                            })
                            .load();

                // If it's folder chart
                } else {

                    // Clear chart spinner items
                    chartSpinnerItems.clear();

                    // Set ICAO code, to be used by chart spinner
                    spinnerIcaoCode = files.get(position).chartName;

                    for (int i = 0; i < filesMap.get(spinnerIcaoCode).size(); i++) {

                        // Add child charts
                        chartSpinnerItems.add(filesMap.get(spinnerIcaoCode).get(i).chartName);

                    }

                    // If chart spinner items isn't empty
                    if(!chartSpinnerItems.isEmpty()){

                        // Notify adapter data has changed
                        chartSpinnerAdapter.notifyDataSetChanged();

                        // Make it visible
                        chartSpinner.setVisibility(View.VISIBLE);

                    // If empty
                    } else {

                        // Remove ICAO from file list
                        files.remove(filePos);

                        // Remove from HashMap
                        filesMap.remove(spinnerIcaoCode);

                        // Remove from spinner list
                        fileSpinnerItems.remove(filePos);

                        // Refresh view
                        fileSpinnerAdapter.notifyDataSetChanged();

                    }

                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Set chart spinner
        // Make spinner with its list
        chartSpinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, chartSpinnerItems);

        // Set drop down style
        chartSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Link adapter with spinner
        chartSpinner.setAdapter(chartSpinnerAdapter);

        chartSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                final int filePos = position;

                // Load pdf (using ICAO code) from HashMap
                pdfView.fromFile(filesMap.get(spinnerIcaoCode).get(filePos).chartFile)
                        // Chart file was removed
                        .onError(new OnErrorListener() {
                            @Override
                            public void onError(Throwable t) {

                                // Remove from files list in HashMap
                                filesMap.get(spinnerIcaoCode).remove(filePos);

                                // Remove from spinner items
                                chartSpinnerItems.remove(filePos);

                                // Refresh view
                                chartSpinnerAdapter.notifyDataSetChanged();

                            }
                        })
                        .load();

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        if(Build.VERSION.SDK_INT>=21) {

            // Check if write external storage permission is gave

            // If permission isn't granted
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    !=PackageManager.PERMISSION_GRANTED){

                // Request it
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);

            }

        }

        // Allow using URI to open chart above API 24
        if(Build.VERSION.SDK_INT>=24){

            try{

                Method m = StrictMode.class.getMethod("disableDeathOnFileUriExposure");

                m.invoke(null);

            } catch(Exception ignored){}
        }

        // Set internal pdf visibility
        setPdfGroup();

    }

    // On back from settings activity
    @Override
    protected void onStart() {super.onStart(); setPdfGroup();}

    // On exit from the program
    @Override
    protected void onStop() {

        super.onStop();

        try{downloader.cancel = true;} catch (Exception ignored){}

        config.writeConfig();

    }

    // Set menu bar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu_main,menu);

        return true;

    }

    // 'Get charts' button on click
    public void getChart(View view) {

        // Get all of codes in a list, 1 code per space
        icaoCode = icaoEdit.getText().toString().toUpperCase().split("\\s+");

        // Run downloader
        downloader = new Downloader(this);

        downloader.execute();

    }

    // Cancel button on click
    public void cancelDown(View view) {view.setEnabled(false); downloader.cancel = true;}

    // 'Settings' menu item on click
    public void showSettings(MenuItem item) {

        // Start activity
        startActivity(new Intent(this, SettingsActivity.class));

        // Set animation
        overridePendingTransition(R.anim.enter, R.anim.exit);

    }

    public void showAbout(MenuItem item) {

        View aboutView = getLayoutInflater().inflate(R.layout.dialog_about, null);

        new AlertDialog.Builder(this)
                .setIcon(R.mipmap.ic_launcher)
                .setTitle(R.string.app_name)
                .setView(aboutView)
                .create()
                .show();

    }

    // Set internal pdf
    private void setPdfGroup(){

        // If internal pdf viewer option enabled
        if(internalPDF){

            // Set visibility for pdf group (TextView, Spinner and PdfView)
            pdfGroup.setVisibility(View.VISIBLE);

        // If not
        } else {

            // Set group visibility
            pdfGroup.setVisibility(View.GONE);

            // Hide chart spinner
            chartSpinner.setVisibility(View.GONE);

        }

    }

    // Reset resource to default
    static public void resetRes(){

        // Clear resources list
        resources.clear();

        // Get airac in correct format (1805 for example)
        String airac = new SimpleDateFormat("yyMM", Locale.US).format(Calendar.getInstance().getTime());

        // Set airac to fltplan server
        String fltplan = String.format("http://imageserver.fltplan.com/merge/merge%s/", airac);

        // Add resources with their types
        resources.add(new ResourcesItem("http://www.armats.com/arm/aviation/products/eAIP/pdf/UD-AD-2.%s-en-GB.pdf", "Normal"));

        resources.add(new ResourcesItem("http://www.sia-enna.dz/PDF/AIP/AD/AD2/%s/","Folder"));

        resources.add(new ResourcesItem(fltplan + "%s.pdf", "Normal"));

        resources.add(new ResourcesItem("http://vau.aero/navdb/chart/%s.pdf", "Normal"));

        resources.add(new ResourcesItem("http://ottomanva.com/lib/charts/%s.pdf", "Normal"));

        resources.add(new ResourcesItem("http://sa-ivao.net/charts_file/%s.pdf","Normal"));

        resources.add(new ResourcesItem("http://www.fly-sea.com/charts/%s.pdf","Normal"));
		
		resources.add(new ResourcesItem("http://www.europlanet.de/vaFsP/charts/%s.pdf","Normal"));

        resources.add(new ResourcesItem("http://uvairlines.com/admin/resources/charts/%s.pdf","Normal"));

        resources.add(new ResourcesItem("https://www.virtualairlines.eu/charts/%s.pdf","Normal"));

    }

}
