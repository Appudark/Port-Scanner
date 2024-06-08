package com.example.myapplication;

import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private EditText ipAddressEditText;
    private EditText startPortEditText;
    private EditText endPortEditText;
    private Button scanButton;
    private Button cancelButton;
    private TextView resultTextView;
    private ProgressBar progressBar;

    private PortScanTask portScanTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ipAddressEditText = findViewById(R.id.ip_address_edit_text);
        startPortEditText = findViewById(R.id.start_port_edit_text);
        endPortEditText = findViewById(R.id.end_port_edit_text);
        scanButton = findViewById(R.id.scan_button);
        cancelButton = findViewById(R.id.cancel_button);
        resultTextView = findViewById(R.id.result_text_view);
        progressBar = findViewById(R.id.progress_bar);

        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String ipAddress = ipAddressEditText.getText().toString();
                String startPortStr = startPortEditText.getText().toString();
                String endPortStr = endPortEditText.getText().toString();

                if (TextUtils.isEmpty(ipAddress) || TextUtils.isEmpty(startPortStr) || TextUtils.isEmpty(endPortStr)) {
                    Toast.makeText(MainActivity.this, "Please enter valid IP address and port range", Toast.LENGTH_SHORT).show();
                    return;
                }

                int startPort = Integer.parseInt(startPortStr);
                int endPort = Integer.parseInt(endPortStr);

                if (startPort > endPort) {
                    Toast.makeText(MainActivity.this, "Start port cannot be greater than end port", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (startPort > 65535 || endPort > 65535) {
                    Toast.makeText(MainActivity.this, "port cannot be greater than 65535", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (startPort < 1 || endPort < 1) {
                    Toast.makeText(MainActivity.this, "port must be greater than 0", Toast.LENGTH_SHORT).show();
                    return;
                }

                portScanTask = new PortScanTask();
                portScanTask.execute(ipAddress, String.valueOf(startPort), String.valueOf(endPort));
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (portScanTask != null) {
                    portScanTask.cancel(true);
                }
            }
        });
    }

    private class PortScanTask extends AsyncTask<String, Integer, List<Integer>> {

        private int totalPorts;
        private int scannedPorts;
        private List<Integer> openPorts;

        @Override
        protected void onPreExecute() {
            totalPorts = 0;
            scannedPorts = 0;
            progressBar.setProgress(0);
            resultTextView.setText("");
            scanButton.setEnabled(false);
            cancelButton.setEnabled(true);
            openPorts = new ArrayList<>();
        }

        @Override
        protected List<Integer> doInBackground(String... params) {
            String ipAddress = params[0];
            int startPort = Integer.parseInt(params[1]);
            int endPort = Integer.parseInt(params[2]);


            totalPorts = endPort - startPort + 1;

            for (int port = startPort; port <= endPort; port++) {
                if (isCancelled()) {
                    break;
                }

                try {
                    Socket socket = new Socket();
                    socket.connect(new InetSocketAddress(ipAddress, port), 2000);
                    socket.close();
                    openPorts.add(port);
                } catch (IOException e) {
                    // Port is closed or an error occurred
                }

                scannedPorts++;
                int progress = (int) ((scannedPorts / (float) totalPorts) * 100);
                publishProgress(progress);
            }

            return openPorts;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            progressBar.setProgress(values[0]);
            resultTextView.setText("Scanning port " + scannedPorts + "/" + totalPorts);
        }


        @Override
        protected void onPostExecute(List<Integer> openPorts) {
            if (openPorts.isEmpty()) {
                resultTextView.setText("All ports are closed");
            } else {
                StringBuilder output = new StringBuilder("Open ports are:\n\n");
                int i=1;
                for (int port : openPorts) {
                    output.append(i+"  |  port  :  ").append(port+"\n");
                    i++;
                }
                resultTextView.setText(output.toString());
            }

            scanButton.setEnabled(true);
            cancelButton.setEnabled(false);
        }


        @Override
        protected void onCancelled() {
            resultTextView.setText("Scan cancelled");
            progressBar.setProgress(0);
            scanButton.setEnabled(true);
            cancelButton.setEnabled(false);
        }
    }

}
