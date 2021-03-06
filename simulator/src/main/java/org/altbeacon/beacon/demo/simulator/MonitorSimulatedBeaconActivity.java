package org.altbeacon.beacon.demo.simulator;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.Region;

import java.util.List;

import hugo.weaving.DebugLog;

@DebugLog
public class MonitorSimulatedBeaconActivity extends AppCompatActivity
        implements MonitorNotifier, BeaconConsumer, TimedBeaconSimulator.Callback {

    private static final int REQUEST_TURN_ON_BLUETOOTH = 1;
    private TextView tv_log;
    private Button btn_simulate;
    private Button btn_enable_bluetooth;
    private TimedBeaconSimulator timedBeaconSimulator;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simulator);
        tv_log = findViewById(R.id.tv_log);
        btn_simulate = findViewById(R.id.btn_simulate);
        btn_enable_bluetooth = findViewById(R.id.btn_enable_bluetooth);
        initUI();

    }

    private void initUI() {
        if (bluetoothAvailable()) {
            boolean bluetoothEnabled = bluetoothEnabled();
            initBluetoothStatus(bluetoothEnabled);
            updateSimulatorUI(bluetoothEnabled);
        } else {
            btn_simulate.setEnabled(false);
            btn_enable_bluetooth.setEnabled(false);
            tv_log.setText(R.string.bluetooth_not_available);
        }

    }

    private boolean bluetoothAvailable() {
        return BluetoothAdapter.getDefaultAdapter() != null;
    }

    private void updateSimulatorUI(boolean bluetoothEnabled) {
        btn_simulate.setEnabled(bluetoothEnabled);
        if (bluetoothEnabled) {
            initSimulator();
            scanBeacon();
        }
    }

    private void initBluetoothStatus(boolean bluetoothEnabled) {
        if (bluetoothEnabled) {
            btn_enable_bluetooth.setText(R.string.bluetooth_enabled);
            btn_enable_bluetooth.setEnabled(false);
            btn_enable_bluetooth.setVisibility(View.INVISIBLE);
        } else {
            btn_enable_bluetooth.setText(R.string.enable_bluetooth);
            btn_enable_bluetooth.setEnabled(true);
            btn_enable_bluetooth.setVisibility(View.VISIBLE);
        }

    }

    private boolean bluetoothEnabled() {
        BluetoothManager manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = manager.getAdapter();
        return bluetoothAdapter.isEnabled();
    }

    private void enableBluetooth() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_TURN_ON_BLUETOOTH);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_TURN_ON_BLUETOOTH:
                updateSimulatorUI(bluetoothEnabled());
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }


    private void scanBeacon() {
        BeaconManager beaconManager = BeaconManager.getInstanceForApplication(this);
        beaconManager.addMonitorNotifier(this);
        beaconManager.bind(this);
    }

    public void append(final String log) {
        runOnUiThread(() -> tv_log.append(log));
    }

    public void toggleSimulate(View view) {
        append("\nBegin to simulate beacon signal.\n");
        begin();
        btn_simulate.setEnabled(false);
    }

    private void begin() {
        timedBeaconSimulator.createTimedSimulatedBeacons();
    }

    private void initSimulator() {
        BeaconManager.setBeaconSimulator(new TimedBeaconSimulator(this));
        timedBeaconSimulator = (TimedBeaconSimulator) BeaconManager.getBeaconSimulator();
    }

    @Override
    public void didEnterRegion(Region region) {
        append("enter:" + region.getUniqueId() + "\n");
    }

    @Override
    public void didExitRegion(Region region) {
        append("exit:" + region.getUniqueId() + "\n");
    }

    @Override
    public void didDetermineStateForRegion(int state, Region region) {

    }

    @Override
    public void onBeaconServiceConnect() {
        registerBeaconToBeMonitored(UuidProvider.regionToBeSimulated());
    }

    private void registerBeaconToBeMonitored(List<String> beacons) {
        try {
            BeaconManager beaconManager = BeaconManager.getInstanceForApplication(this);
            for (String beacon : beacons) {
                beaconManager.startMonitoringBeaconsInRegion(UuidMapper.constructRegion(beacon));
            }
            tv_log.setText(R.string.ready_to_simulate_beacon);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }

    }

    public void enableBluetooth(View view) {
        enableBluetooth();
    }

    @Override
    public void onShutdown() {
        btn_simulate.post(this::updateUI);
    }

    private void updateUI() {
        append(getString(R.string.pending_exit_event));
    }
}
