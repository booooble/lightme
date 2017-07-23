package com.litvinenko.tony.candle;

import android.annotation.TargetApi;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;

import static android.Manifest.permission.CAMERA;
import static android.content.pm.PackageManager.FEATURE_CAMERA_FLASH;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.hardware.Camera.Parameters.FLASH_MODE_OFF;
import static android.hardware.Camera.Parameters.FLASH_MODE_ON;
import static android.hardware.Camera.Parameters.FLASH_MODE_TORCH;
import static android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION;
import static android.media.AudioAttributes.USAGE_GAME;
import static android.media.AudioManager.STREAM_MUSIC;
import static android.os.Build.VERSION_CODES.LOLLIPOP;
import static android.view.Gravity.BOTTOM;
import static android.view.Gravity.CENTER_HORIZONTAL;
import static com.litvinenko.tony.candle.R.color.colorBackground;

public class MainActivity extends AppCompatActivity implements SoundPool.OnLoadCompleteListener, SurfaceHolder.Callback {
    private int sound;
    private SoundPool soundPool;
    private Camera camera;
    private Parameters parameters;
    private Switch switcher;
    private FloatingActionButton muteButton;
    private FloatingActionButton unMuteButton;
    private boolean isMuted = false;
    private SurfaceHolder holder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        SurfaceView preview = (SurfaceView)findViewById(R.id.preview);
        holder = preview.getHolder();
        holder.addCallback(this);

        processMuteButtons();
        createSoundPull();
        processSwitcher();

        if (!hasFlashLight()) {
            showCameraAlert(R.string.flashlight_not_available);
        } else {
            if (ContextCompat.checkSelfPermission(this, CAMERA) !=  PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{CAMERA}, 1);
            } else {
                camera = Camera.open();
            }
        }


    }

    private void processSwitcher() {
        switcher = (Switch) findViewById(R.id.switcher);
        switcher.setChecked(false);
        final View view = this.getWindow().getDecorView();
        switcher.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                    if (isChecked)
                    {
                        view.setBackgroundResource(R.drawable.candle_bg_on);
                        turnOnFlashlight();
                    }
                    else
                    {
                        view.setBackgroundResource(colorBackground);
                        turnOffFlashlight();
                    }

            }
        });
    }

    private void createSoundPull() {
        if (Build.VERSION.SDK_INT >= LOLLIPOP) {
            createSoundPoolWithBuilder();
        } else {
            createSoundPoolWithConstructor();
        }

        soundPool.setOnLoadCompleteListener(this);
        sound = soundPool.load(this, R.raw.flashlight_switch, 1);
    }

    private void processMuteButtons() {
        muteButton = (FloatingActionButton) findViewById(R.id.mute);
        unMuteButton = (FloatingActionButton) findViewById(R.id.unmute);

        unMuteButton.hide();

        muteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                muteOnClick();
            }
        });
        unMuteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                muteOnClick();
            }
        });
    }

    private void muteOnClick() {
        if (!isMuted) {
            isMuted = true;
            muteButton.hide();
            unMuteButton.show();
            showToast(R.string.mute);
        }
        else {
            isMuted = false;
            unMuteButton.hide();
            muteButton.show();
            showToast(R.string.unmute);
        }
    }

    private void showToast(int textId) {
        String text = getResources().getString(textId);
        Toast toast = Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT);
        toast.setGravity(BOTTOM | CENTER_HORIZONTAL, 0, 40);
        toast.show();
    }

    @SuppressWarnings("deprecation")
    private void createSoundPoolWithConstructor() {
        soundPool = new SoundPool(1, STREAM_MUSIC, 0);
    }

    @TargetApi(LOLLIPOP)
    private void createSoundPoolWithBuilder() {
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(USAGE_GAME)
                .setContentType(CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder()
            .setAudioAttributes(attributes)
            .setMaxStreams(1)
            .build();
    }

    private boolean hasFlashLight() {
        return getApplicationContext().getPackageManager().hasSystemFeature(FEATURE_CAMERA_FLASH);
    }

    private void showCameraAlert(int messageId) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.alert)
                .setMessage(messageId)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .show();
    }

    private void playClickSound() {
        if(!isMuted) {
            soundPool.play(sound, 1, 1, 0, 0, 1);
        }
    }

    private void turnOnFlashlight() {
        playClickSound();

        new Thread(new Runnable() {
            @Override
            public void run() {
                if (camera != null) {
                    parameters = camera.getParameters();

                    if (parameters != null) {
                        List supportedFlashModes = parameters.getSupportedFlashModes();

                        if(supportedFlashModes.contains(FLASH_MODE_TORCH)) {
                            parameters.setFlashMode(FLASH_MODE_TORCH);
                        }
                        else if (supportedFlashModes.contains(FLASH_MODE_ON)) {
                            parameters.setFlashMode(FLASH_MODE_ON);
                        } else {
                            camera = null;
                        }

                        if(camera != null) {
                            camera.setParameters(parameters);
                            camera.startPreview();
                            try {
                                camera.setPreviewDisplay(holder);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                    }
                }
            }
        }).start();
    }

    private void turnOffFlashlight() {
        playClickSound();

        new Thread(new Runnable() {
            @Override
            public void run() {
                if (camera != null) {
                    parameters.setFlashMode(FLASH_MODE_OFF);
                    camera.setParameters(parameters);
                    camera.stopPreview();
                }
            }
        }).start();
    }

    private void releaseCamera() {
        if (camera != null) {
            camera.release();
            camera = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    camera = Camera.open();
                } else {
                    showCameraAlert(R.string.permissions_denied);
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_about) {
            this.startActivity(new Intent (this, AboutActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onLoadComplete(SoundPool soundPool, int i, int i1) {

    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        releaseCamera();
        soundPool.release();
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

    }
}
