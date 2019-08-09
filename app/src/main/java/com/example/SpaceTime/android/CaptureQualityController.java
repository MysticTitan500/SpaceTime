package com.example.SpaceTime.android;

import android.widget.SeekBar;
import android.widget.TextView;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.webrtc.CameraEnumerationAndroid.CaptureFormat;


public class CaptureQualityController implements SeekBar.OnSeekBarChangeListener {
  private final List<CaptureFormat> formats =
      Arrays.asList(new CaptureFormat(1280, 720, 0, 30000), new CaptureFormat(960, 540, 0, 30000),
          new CaptureFormat(640, 480, 0, 30000), new CaptureFormat(480, 360, 0, 30000),
          new CaptureFormat(320, 240, 0, 30000), new CaptureFormat(256, 144, 0, 30000));


  private static final int FRAMERATE_THRESHOLD = 15;
  private TextView capTextForm;
  private CallFragment.OnCallEvents callEvents;
  private int width = 0;
  private int height = 0;
  private int fPS = 0;
  private double targetBandwidth = 0;

  public CaptureQualityController(
      TextView captureFormatText, CallFragment.OnCallEvents callEvents) {
    this.capTextForm = captureFormatText;
    this.callEvents = callEvents;
  }

  private final Comparator<CaptureFormat> compareFormats = new Comparator<CaptureFormat>() {
    @Override
    public int compare(CaptureFormat first, CaptureFormat second) {
      int firstFps = calculateFramerate(targetBandwidth, first);
      int secondFps = calculateFramerate(targetBandwidth, second);

      if ((firstFps >= FRAMERATE_THRESHOLD && secondFps >= FRAMERATE_THRESHOLD)
          || firstFps == secondFps) {
        return first.width * first.height - second.width * second.height;
      }
      else {
        return firstFps - secondFps;
      }
    }
  };

  @Override
  public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
    if (progress == 0) {
      width = 0;
      height = 0;
      fPS = 0;
      capTextForm.setText(org.appspot.apprtc.R.string.muted);
      return;
    }

    long maxCaptureBandwidth = Long.MIN_VALUE;
    for (CaptureFormat format : formats) {
      maxCaptureBandwidth =
          Math.max(maxCaptureBandwidth, (long) format.width * format.height * format.framerate.max);
    }

    double bandwidthFraction = (double) progress / 100.0;
    final double kExpConstant = 3.0;

    bandwidthFraction =(Math.exp(kExpConstant * bandwidthFraction) - 1) / (Math.exp(kExpConstant) - 1);
    targetBandwidth = bandwidthFraction * maxCaptureBandwidth;

    final CaptureFormat bestFormat = Collections.max(formats, compareFormats);
    width = bestFormat.width;
    height = bestFormat.height;
    fPS = calculateFramerate(targetBandwidth, bestFormat);
    capTextForm.setText(
        String.format(capTextForm.getContext().getString(org.appspot.apprtc.R.string.format_description), width,
            height, fPS));
  }

  @Override
  public void onStartTrackingTouch(SeekBar seekBar) {}

  @Override
  public void onStopTrackingTouch(SeekBar seekBar) {
    callEvents.onCaptureFormatChange(width, height, fPS);
  }
  private int calculateFramerate(double bandwidth, CaptureFormat format) {
    return (int) Math.round(
        Math.min(format.framerate.max, (int) Math.round(bandwidth / (format.width * format.height)))
        / 1000.0);
  }
}
