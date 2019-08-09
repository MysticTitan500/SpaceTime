package com.example.SpaceTime.android;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.webkit.URLUtil;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Random;

import org.appspot.apprtc.ui.SettingsActivity;
import org.json.JSONArray;
import org.json.JSONException;


public class ConnectActivity extends AppCompatActivity {
  private static final String TAG = "ConnectActivity";
  private static final int CONNECTION_REQUEST = 1;
  private static final int REMOVE_FAVORITE_INDEX = 0;
  private static boolean commandLineRun = false;

  private ImageButton addFavButton;
  private EditText roomEditText;
  private ListView roomListView;
  private SharedPreferences sharedPref;
  private String keyPrefResolution;
  private String keyPrefFps;
  private String keyPrefVidBitrateType;
  private String keyPrefVidBitrateValue;
  private String keyPrefAudioBitrateType;
  private String keyPrefAudioBitrateValue;
  private String keyPrefRoomServerUrl;
  private String keyPrefRoom;
  private String keyPrefRoomList;
  private ArrayList<String> roomList;
  private ArrayAdapter<String> adapter;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    PreferenceManager.setDefaultValues(this, org.appspot.apprtc.R.xml.preferences, false);
    sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
    keyPrefResolution = getString(org.appspot.apprtc.R.string.pref_resolution_key);
    keyPrefFps = getString(org.appspot.apprtc.R.string.pref_fps_key);
    keyPrefVidBitrateType = getString(org.appspot.apprtc.R.string.pref_maxvideobitrate_key);
    keyPrefVidBitrateValue = getString(org.appspot.apprtc.R.string.pref_maxvideobitratevalue_key);
    keyPrefAudioBitrateType = getString(org.appspot.apprtc.R.string.pref_startaudiobitrate_key);
    keyPrefAudioBitrateValue = getString(org.appspot.apprtc.R.string.pref_startaudiobitratevalue_key);
    keyPrefRoomServerUrl = getString(org.appspot.apprtc.R.string.pref_room_server_url_key);
    keyPrefRoom = getString(org.appspot.apprtc.R.string.pref_room_key);
    keyPrefRoomList = getString(org.appspot.apprtc.R.string.pref_room_list_key);

    setContentView(R.layout.activity_connect);

    roomEditText = findViewById(R.id.room_edittext);
    roomEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
      @Override
      public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
        if (i == EditorInfo.IME_ACTION_DONE) {
          addFavButton.performClick();
          return true;
        }
        return false;
      }
    });
    roomEditText.requestFocus();

    roomListView = findViewById(R.id.room_listview);
    roomListView.setEmptyView(findViewById(android.R.id.empty));
    roomListView.setOnItemClickListener(roomListClickListener);
    registerForContextMenu(roomListView);
    ImageButton connectButton = findViewById(R.id.connect_button);
    connectButton.setOnClickListener(connectListener);
    addFavButton = findViewById(R.id.add_favorite_button);
    addFavButton.setOnClickListener(addFavoriteListener);

    final Intent intent = getIntent();
    if ("android.intent.action.VIEW".equals(intent.getAction()) && !commandLineRun) {
      boolean loopback = intent.getBooleanExtra(CallActivity.EXTRA_LOOPBACK, false);
      int runTimeMs = intent.getIntExtra(CallActivity.EXTRA_RUNTIME, 0);
      boolean useValuesFromIntent =
          intent.getBooleanExtra(CallActivity.EXTRA_USE_VALUES_FROM_INTENT, false);
      String room = sharedPref.getString(keyPrefRoom, "");
      connectToRoom(room, true, loopback, useValuesFromIntent, runTimeMs);
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.connect_menu, menu);
    return true;
  }

  @Override
  public void onCreateContextMenu(ContextMenu conMenu, View v, ContextMenu.ContextMenuInfo menuInfo) {
    if (v.getId() == R.id.room_listview) {
      AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
      conMenu.setHeaderTitle(roomList.get(info.position));
      String[] menuItems = getResources().getStringArray(org.appspot.apprtc.R.array.roomListContextMenu);
      for (int i = 0; i < menuItems.length; i++) {
        conMenu.add(Menu.NONE, i, i, menuItems[i]);
      }
    } else {
      super.onCreateContextMenu(conMenu, v, menuInfo);
    }
  }

  @Override
  public boolean onContextItemSelected(MenuItem item) {
    if (item.getItemId() == REMOVE_FAVORITE_INDEX) {
      AdapterView.AdapterContextMenuInfo info =
          (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
      roomList.remove(info.position);
      adapter.notifyDataSetChanged();
      return true;
    }

    return super.onContextItemSelected(item);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == R.id.action_settings) {
      Intent intent = new Intent(this, SettingsActivity.class);
      startActivity(intent);
      return true;
    } else if (item.getItemId() == R.id.action_loopback) {
      connectToRoom(null, false, true, false, 0);
      return true;
    } else {
      return super.onOptionsItemSelected(item);
    }
  }


  @Override
  public void onResume() {
    super.onResume();
    String room = sharedPref.getString(keyPrefRoom, "");
    roomEditText.setText(room);
    roomList = new ArrayList<>();
    String roomListJson = sharedPref.getString(keyPrefRoomList, null);
    if (roomListJson != null) {
      try {
        JSONArray jsonArray = new JSONArray(roomListJson);
        for (int i = 0; i < jsonArray.length(); i++) {
          roomList.add(jsonArray.get(i).toString());
        }
      } catch (JSONException e) {
        Log.e(TAG, "Failed to load room list: " + e.toString());
      }
    }
    adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, roomList);
    roomListView.setAdapter(adapter);
    if (adapter.getCount() > 0) {
      roomListView.requestFocus();
      roomListView.setItemChecked(0, true);
    }
  }
  @Override
  public void onPause() {
    super.onPause();
    String room = roomEditText.getText().toString();
    String roomListJson = new JSONArray(roomList).toString();
    SharedPreferences.Editor editor = sharedPref.edit();
    editor.putString(keyPrefRoom, room);
    editor.putString(keyPrefRoomList, roomListJson);
    editor.commit();
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == CONNECTION_REQUEST && commandLineRun) {
      Log.d(TAG, "Return: " + resultCode);
      setResult(resultCode);
      commandLineRun = false;
      finish();
    }
  }


  private boolean sharedPrefGetBoolean(
      int attributeId, String intentName, int defaultId, boolean useFromIntent) {
    boolean defaultValue = Boolean.parseBoolean(getString(defaultId));
    if (useFromIntent) {
      return getIntent().getBooleanExtra(intentName, defaultValue);
    } else {
      String attributeName = getString(attributeId);
      return sharedPref.getBoolean(attributeName, defaultValue);
    }
  }

  private int sharedPrefGetInteger(
      int attributeId, String intentName, int defaultId, boolean useFromIntent) {
    String defaultString = getString(defaultId);
    int defaultValue = Integer.parseInt(defaultString);
    if (useFromIntent) {
      return getIntent().getIntExtra(intentName, defaultValue);
    } else {
      String attributeName = getString(attributeId);
      String value = sharedPref.getString(attributeName, defaultString);
      try {
        return Integer.parseInt(value);
      } catch (NumberFormatException e) {
        Log.e(TAG, "Wrong setting for: " + attributeName + ":" + value);
        return defaultValue;
      }
    }
  }
  private String sharedPrefGetString(
          int attributeId, String intentName, int defaultId, boolean useFromIntent) {
    String defaultValue = getString(defaultId);
    if (useFromIntent) {
      String value = getIntent().getStringExtra(intentName);
      if (value != null) {
        return value;
      }
      return defaultValue;
    } else {
      String attributeName = getString(attributeId);
      return sharedPref.getString(attributeName, defaultValue);
    }

  }
  @SuppressWarnings("StringSplitter")
  private void connectToRoom(String roomId, boolean commandLineRun, boolean loopback,
      boolean useValuesFromIntent, int runTimeMs) {
    ConnectActivity.commandLineRun = commandLineRun;

    if (loopback) {
      roomId = Integer.toString((new Random()).nextInt(100000000));
    }

    String roomUrl = sharedPref.getString(
            keyPrefRoomServerUrl, getString(org.appspot.apprtc.R.string.pref_room_server_url_default));

    boolean vidCallEnabled = sharedPrefGetBoolean(org.appspot.apprtc.R.string.pref_videocall_key,
        CallActivity.EXTRA_VIDEO_CALL, org.appspot.apprtc.R.string.pref_videocall_default, useValuesFromIntent);

    boolean useScreenCap = sharedPrefGetBoolean(org.appspot.apprtc.R.string.pref_screencapture_key,
        CallActivity.EXTRA_SCREENCAPTURE, org.appspot.apprtc.R.string.pref_screencapture_default, useValuesFromIntent);

    boolean useCamera2 = sharedPrefGetBoolean(org.appspot.apprtc.R.string.pref_camera2_key, CallActivity.EXTRA_CAMERA2,
        org.appspot.apprtc.R.string.pref_camera2_default, useValuesFromIntent);

    String vidCodec = sharedPrefGetString(org.appspot.apprtc.R.string.pref_videocodec_key,
        CallActivity.EXTRA_VIDEOCODEC, org.appspot.apprtc.R.string.pref_videocodec_default, useValuesFromIntent);
    String audioCodec = sharedPrefGetString(org.appspot.apprtc.R.string.pref_audiocodec_key,
        CallActivity.EXTRA_AUDIOCODEC, org.appspot.apprtc.R.string.pref_audiocodec_default, useValuesFromIntent);

    boolean hwCodec = sharedPrefGetBoolean(org.appspot.apprtc.R.string.pref_hwcodec_key,
        CallActivity.EXTRA_HWCODEC_ENABLED, org.appspot.apprtc.R.string.pref_hwcodec_default, useValuesFromIntent);

    boolean capToText = sharedPrefGetBoolean(org.appspot.apprtc.R.string.pref_capturetotexture_key,
        CallActivity.EXTRA_CAPTURETOTEXTURE_ENABLED, org.appspot.apprtc.R.string.pref_capturetotexture_default,
        useValuesFromIntent);

    boolean flexfecEnabled = sharedPrefGetBoolean(org.appspot.apprtc.R.string.pref_flexfec_key,
        CallActivity.EXTRA_FLEXFEC_ENABLED, org.appspot.apprtc.R.string.pref_flexfec_default, useValuesFromIntent);

    boolean noAudioProcessing = sharedPrefGetBoolean(org.appspot.apprtc.R.string.pref_noaudioprocessing_key,
        CallActivity.EXTRA_NOAUDIOPROCESSING_ENABLED, org.appspot.apprtc.R.string.pref_noaudioprocessing_default,
        useValuesFromIntent);

    boolean aecDump = sharedPrefGetBoolean(org.appspot.apprtc.R.string.pref_aecdump_key,
        CallActivity.EXTRA_AECDUMP_ENABLED, org.appspot.apprtc.R.string.pref_aecdump_default, useValuesFromIntent);

    boolean saveInputAudioToFile =
        sharedPrefGetBoolean(org.appspot.apprtc.R.string.pref_enable_save_input_audio_to_file_key,
            CallActivity.EXTRA_SAVE_INPUT_AUDIO_TO_FILE_ENABLED,
            org.appspot.apprtc.R.string.pref_enable_save_input_audio_to_file_default, useValuesFromIntent);

    boolean useOpenSLES = sharedPrefGetBoolean(org.appspot.apprtc.R.string.pref_opensles_key,
        CallActivity.EXTRA_OPENSLES_ENABLED, org.appspot.apprtc.R.string.pref_opensles_default, useValuesFromIntent);

    boolean disableBuiltInAEC = sharedPrefGetBoolean(org.appspot.apprtc.R.string.pref_disable_built_in_aec_key,
        CallActivity.EXTRA_DISABLE_BUILT_IN_AEC, org.appspot.apprtc.R.string.pref_disable_built_in_aec_default,
        useValuesFromIntent);

    boolean disableBuiltInAGC = sharedPrefGetBoolean(org.appspot.apprtc.R.string.pref_disable_built_in_agc_key,
        CallActivity.EXTRA_DISABLE_BUILT_IN_AGC, org.appspot.apprtc.R.string.pref_disable_built_in_agc_default,
        useValuesFromIntent);

    boolean disableBuiltInNS = sharedPrefGetBoolean(org.appspot.apprtc.R.string.pref_disable_built_in_ns_key,
        CallActivity.EXTRA_DISABLE_BUILT_IN_NS, org.appspot.apprtc.R.string.pref_disable_built_in_ns_default,
        useValuesFromIntent);

    boolean disableWebRtcAGCAndHPF = sharedPrefGetBoolean(
        org.appspot.apprtc.R.string.pref_disable_webrtc_agc_and_hpf_key, CallActivity.EXTRA_DISABLE_WEBRTC_AGC_AND_HPF,
        org.appspot.apprtc.R.string.pref_disable_webrtc_agc_and_hpf_key, useValuesFromIntent);

    int vidWidth = 0;
    int vidHeight = 0;
    if (useValuesFromIntent) {
      vidWidth = getIntent().getIntExtra(CallActivity.EXTRA_VIDEO_WIDTH, 0);
      vidHeight = getIntent().getIntExtra(CallActivity.EXTRA_VIDEO_HEIGHT, 0);
    }
    if (vidWidth == 0 && vidHeight == 0) {
      String resolution =
          sharedPref.getString(keyPrefResolution, getString(org.appspot.apprtc.R.string.pref_resolution_default));
      String[] dimensions = resolution.split("[ x]+");
      if (dimensions.length == 2) {
        try {
          vidWidth = Integer.parseInt(dimensions[0]);
          vidHeight = Integer.parseInt(dimensions[1]);
        } catch (NumberFormatException e) {
          vidWidth = 0;
          vidHeight = 0;
          Log.e(TAG, "Wrong video resolution setting: " + resolution);
        }
      }
    }

    int cameraFps = 0;
    if (useValuesFromIntent) {
      cameraFps = getIntent().getIntExtra(CallActivity.EXTRA_VIDEO_FPS, 0);
    }
    if (cameraFps == 0) {
      String fps = sharedPref.getString(keyPrefFps, getString(org.appspot.apprtc.R.string.pref_fps_default));
      String[] fpsValues = fps.split("[ x]+");
      if (fpsValues.length == 2) {
        try {
          cameraFps = Integer.parseInt(fpsValues[0]);
        } catch (NumberFormatException e) {
          cameraFps = 0;
          Log.e(TAG, "Wrong camera fps setting: " + fps);
        }
      }
    }

    boolean captureQualitySlider = sharedPrefGetBoolean(org.appspot.apprtc.R.string.pref_capturequalityslider_key,
        CallActivity.EXTRA_VIDEO_CAPTUREQUALITYSLIDER_ENABLED,
        org.appspot.apprtc.R.string.pref_capturequalityslider_default, useValuesFromIntent);

    int vidStartBitrate = 0;
    if (useValuesFromIntent) {
      vidStartBitrate = getIntent().getIntExtra(CallActivity.EXTRA_VIDEO_BITRATE, 0);
    }
    if (vidStartBitrate == 0) {
      String bitrateTypeDefault = getString(org.appspot.apprtc.R.string.pref_maxvideobitrate_default);
      String bitrateType = sharedPref.getString(keyPrefVidBitrateType, bitrateTypeDefault);
      if (!bitrateType.equals(bitrateTypeDefault)) {
        String bitrateValue = sharedPref.getString(
                keyPrefVidBitrateValue, getString(org.appspot.apprtc.R.string.pref_maxvideobitratevalue_default));
        vidStartBitrate = Integer.parseInt(bitrateValue);
      }
    }

    int audioStartBitrate = 0;
    if (useValuesFromIntent) {
      audioStartBitrate = getIntent().getIntExtra(CallActivity.EXTRA_AUDIO_BITRATE, 0);
    }
    if (audioStartBitrate == 0) {
      String bitrateTypeDefault = getString(org.appspot.apprtc.R.string.pref_startaudiobitrate_default);
      String bitrateType = sharedPref.getString(keyPrefAudioBitrateType, bitrateTypeDefault);
      if (!bitrateType.equals(bitrateTypeDefault)) {
        String bitrateValue = sharedPref.getString(
                keyPrefAudioBitrateValue, getString(org.appspot.apprtc.R.string.pref_startaudiobitratevalue_default));
        audioStartBitrate = Integer.parseInt(bitrateValue);
      }
    }

    boolean displayHud = sharedPrefGetBoolean(org.appspot.apprtc.R.string.pref_displayhud_key,
        CallActivity.EXTRA_DISPLAY_HUD, org.appspot.apprtc.R.string.pref_displayhud_default, useValuesFromIntent);

    boolean tracing = sharedPrefGetBoolean(org.appspot.apprtc.R.string.pref_tracing_key, CallActivity.EXTRA_TRACING,
        org.appspot.apprtc.R.string.pref_tracing_default, useValuesFromIntent);

    boolean rtcEventLogEnabled = sharedPrefGetBoolean(org.appspot.apprtc.R.string.pref_enable_rtceventlog_key,
        CallActivity.EXTRA_ENABLE_RTCEVENTLOG, org.appspot.apprtc.R.string.pref_enable_rtceventlog_default,
        useValuesFromIntent);

    boolean useLegacyAudioDevice = sharedPrefGetBoolean(org.appspot.apprtc.R.string.pref_use_legacy_audio_device_key,
        CallActivity.EXTRA_USE_LEGACY_AUDIO_DEVICE, org.appspot.apprtc.R.string.pref_use_legacy_audio_device_default,
        useValuesFromIntent);

    // Get datachannel options
    boolean dataChannelEnabled = sharedPrefGetBoolean(org.appspot.apprtc.R.string.pref_enable_datachannel_key,
        CallActivity.EXTRA_DATA_CHANNEL_ENABLED, org.appspot.apprtc.R.string.pref_enable_datachannel_default,
        useValuesFromIntent);
    boolean ordered = sharedPrefGetBoolean(org.appspot.apprtc.R.string.pref_ordered_key, CallActivity.EXTRA_ORDERED,
        org.appspot.apprtc.R.string.pref_ordered_default, useValuesFromIntent);
    boolean negotiated = sharedPrefGetBoolean(org.appspot.apprtc.R.string.pref_negotiated_key,
        CallActivity.EXTRA_NEGOTIATED, org.appspot.apprtc.R.string.pref_negotiated_default, useValuesFromIntent);
    int maxRetrMs = sharedPrefGetInteger(org.appspot.apprtc.R.string.pref_max_retransmit_time_ms_key,
        CallActivity.EXTRA_MAX_RETRANSMITS_MS, org.appspot.apprtc.R.string.pref_max_retransmit_time_ms_default,
        useValuesFromIntent);
    int maxRetr =
        sharedPrefGetInteger(org.appspot.apprtc.R.string.pref_max_retransmits_key, CallActivity.EXTRA_MAX_RETRANSMITS,
            org.appspot.apprtc.R.string.pref_max_retransmits_default, useValuesFromIntent);
    int id = sharedPrefGetInteger(org.appspot.apprtc.R.string.pref_data_id_key, CallActivity.EXTRA_ID,
        org.appspot.apprtc.R.string.pref_data_id_default, useValuesFromIntent);
    String protocol = sharedPrefGetString(org.appspot.apprtc.R.string.pref_data_protocol_key,
        CallActivity.EXTRA_PROTOCOL, org.appspot.apprtc.R.string.pref_data_protocol_default, useValuesFromIntent);

    Log.d(TAG, "Connecting to room " + roomId + " at URL " + roomUrl);
    if (validateUrl(roomUrl)) {
      Uri uri = Uri.parse(roomUrl);
      Intent intent = new Intent(this, CallActivity.class);
      intent.setData(uri);
      intent.putExtra(CallActivity.EXTRA_ROOMID, roomId);
      intent.putExtra(CallActivity.EXTRA_LOOPBACK, loopback);
      intent.putExtra(CallActivity.EXTRA_VIDEO_CALL, vidCallEnabled);
      intent.putExtra(CallActivity.EXTRA_SCREENCAPTURE, useScreenCap);
      intent.putExtra(CallActivity.EXTRA_CAMERA2, useCamera2);
      intent.putExtra(CallActivity.EXTRA_VIDEO_WIDTH, vidWidth);
      intent.putExtra(CallActivity.EXTRA_VIDEO_HEIGHT, vidHeight);
      intent.putExtra(CallActivity.EXTRA_VIDEO_FPS, cameraFps);
      intent.putExtra(CallActivity.EXTRA_VIDEO_CAPTUREQUALITYSLIDER_ENABLED, captureQualitySlider);
      intent.putExtra(CallActivity.EXTRA_VIDEO_BITRATE, vidStartBitrate);
      intent.putExtra(CallActivity.EXTRA_VIDEOCODEC, vidCodec);
      intent.putExtra(CallActivity.EXTRA_HWCODEC_ENABLED, hwCodec);
      intent.putExtra(CallActivity.EXTRA_CAPTURETOTEXTURE_ENABLED, capToText);
      intent.putExtra(CallActivity.EXTRA_FLEXFEC_ENABLED, flexfecEnabled);
      intent.putExtra(CallActivity.EXTRA_NOAUDIOPROCESSING_ENABLED, noAudioProcessing);
      intent.putExtra(CallActivity.EXTRA_AECDUMP_ENABLED, aecDump);
      intent.putExtra(CallActivity.EXTRA_SAVE_INPUT_AUDIO_TO_FILE_ENABLED, saveInputAudioToFile);
      intent.putExtra(CallActivity.EXTRA_OPENSLES_ENABLED, useOpenSLES);
      intent.putExtra(CallActivity.EXTRA_DISABLE_BUILT_IN_AEC, disableBuiltInAEC);
      intent.putExtra(CallActivity.EXTRA_DISABLE_BUILT_IN_AGC, disableBuiltInAGC);
      intent.putExtra(CallActivity.EXTRA_DISABLE_BUILT_IN_NS, disableBuiltInNS);
      intent.putExtra(CallActivity.EXTRA_DISABLE_WEBRTC_AGC_AND_HPF, disableWebRtcAGCAndHPF);
      intent.putExtra(CallActivity.EXTRA_AUDIO_BITRATE, audioStartBitrate);
      intent.putExtra(CallActivity.EXTRA_AUDIOCODEC, audioCodec);
      intent.putExtra(CallActivity.EXTRA_DISPLAY_HUD, displayHud);
      intent.putExtra(CallActivity.EXTRA_TRACING, tracing);
      intent.putExtra(CallActivity.EXTRA_ENABLE_RTCEVENTLOG, rtcEventLogEnabled);
      intent.putExtra(CallActivity.EXTRA_CMDLINE, commandLineRun);
      intent.putExtra(CallActivity.EXTRA_RUNTIME, runTimeMs);
      intent.putExtra(CallActivity.EXTRA_USE_LEGACY_AUDIO_DEVICE, useLegacyAudioDevice);

      intent.putExtra(CallActivity.EXTRA_DATA_CHANNEL_ENABLED, dataChannelEnabled);

      if (dataChannelEnabled) {
        intent.putExtra(CallActivity.EXTRA_ORDERED, ordered);
        intent.putExtra(CallActivity.EXTRA_MAX_RETRANSMITS_MS, maxRetrMs);
        intent.putExtra(CallActivity.EXTRA_MAX_RETRANSMITS, maxRetr);
        intent.putExtra(CallActivity.EXTRA_PROTOCOL, protocol);
        intent.putExtra(CallActivity.EXTRA_NEGOTIATED, negotiated);
        intent.putExtra(CallActivity.EXTRA_ID, id);
      }

      if (useValuesFromIntent) {
        if (getIntent().hasExtra(CallActivity.EXTRA_VIDEO_FILE_AS_CAMERA)) {
          String videoFileAsCamera =
              getIntent().getStringExtra(CallActivity.EXTRA_VIDEO_FILE_AS_CAMERA);
          intent.putExtra(CallActivity.EXTRA_VIDEO_FILE_AS_CAMERA, videoFileAsCamera);
        }

        if (getIntent().hasExtra(CallActivity.EXTRA_SAVE_REMOTE_VIDEO_TO_FILE)) {
          String saveRemoteVideoToFile =
              getIntent().getStringExtra(CallActivity.EXTRA_SAVE_REMOTE_VIDEO_TO_FILE);
          intent.putExtra(CallActivity.EXTRA_SAVE_REMOTE_VIDEO_TO_FILE, saveRemoteVideoToFile);
        }

        if (getIntent().hasExtra(CallActivity.EXTRA_SAVE_REMOTE_VIDEO_TO_FILE_WIDTH)) {
          int videoOutWidth =
              getIntent().getIntExtra(CallActivity.EXTRA_SAVE_REMOTE_VIDEO_TO_FILE_WIDTH, 0);
          intent.putExtra(CallActivity.EXTRA_SAVE_REMOTE_VIDEO_TO_FILE_WIDTH, videoOutWidth);
        }

        if (getIntent().hasExtra(CallActivity.EXTRA_SAVE_REMOTE_VIDEO_TO_FILE_HEIGHT)) {
          int videoOutHeight =
              getIntent().getIntExtra(CallActivity.EXTRA_SAVE_REMOTE_VIDEO_TO_FILE_HEIGHT, 0);
          intent.putExtra(CallActivity.EXTRA_SAVE_REMOTE_VIDEO_TO_FILE_HEIGHT, videoOutHeight);
        }
      }

      startCallActivity(intent);
    }
  }

  private static final String[] PERMISSIONS_START_CALL = {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};//WRITE_EXTERNAL_STORAGE, CAPTURE_VIDEO_OUTPUT
  private static final int PERMISSIONS_REQUEST_START_CALL = 101;
  private Intent startCallIntent;

  private void startCallActivity(Intent intent) {
    if(!hasPermissions(this, PERMISSIONS_START_CALL)){
      startCallIntent = intent;
      ActivityCompat.requestPermissions(this, PERMISSIONS_START_CALL, PERMISSIONS_REQUEST_START_CALL);
      return;
    }
    startActivityForResult(intent, CONNECTION_REQUEST);
  }

  private static boolean hasPermissions(Context context, String... permissions) {
    if (context != null && permissions != null) {
      for (String permission : permissions) {
        if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
          return false;
        }
      }
    }
    return true;
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    switch (requestCode) {
      case PERMISSIONS_REQUEST_START_CALL: {
        if (hasPermissions(this, PERMISSIONS_START_CALL)) {
          // permission was granted, yay!
          if (startCallIntent != null) startActivityForResult(startCallIntent, CONNECTION_REQUEST);
        } else {
          Toast.makeText(this, "Required permissions denied.", Toast.LENGTH_LONG).show();
        }
        return;
      }
    }
  }

  @Override
  protected void onRestoreInstanceState(Bundle savedInstanceState) {
    super.onRestoreInstanceState(savedInstanceState);
    startCallIntent = savedInstanceState.getParcelable("startCallIntent");
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putParcelable("startCallIntent", startCallIntent);
  }

  private boolean validateUrl(String url) {
    if (URLUtil.isHttpsUrl(url) || URLUtil.isHttpUrl(url)) {
      return true;
    }

    new AlertDialog.Builder(this)
        .setTitle(getText(org.appspot.apprtc.R.string.invalid_url_title))
        .setMessage(getString(org.appspot.apprtc.R.string.invalid_url_text, url))
        .setCancelable(false)
        .setNeutralButton(org.appspot.apprtc.R.string.ok,
            new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
              }
            })
        .create()
        .show();
    return false;
  }

  private final AdapterView.OnItemClickListener roomListClickListener =
      new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
          String roomId = ((TextView) view).getText().toString();
          connectToRoom(roomId, false, false, false, 0);
        }
      };

  private final OnClickListener addFavoriteListener = new OnClickListener() {
    @Override
    public void onClick(View view) {
      String newRoom = roomEditText.getText().toString();
      if (newRoom.length() > 0 && !roomList.contains(newRoom)) {
        adapter.add(newRoom);
        adapter.notifyDataSetChanged();
      }
    }
  };

  private final OnClickListener connectListener = new OnClickListener() {
    @Override
    public void onClick(View view) {
      connectToRoom(roomEditText.getText().toString(), false, false, false, 0);
    }
  };
}
