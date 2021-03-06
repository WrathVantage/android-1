/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.tools.profilers;

import com.android.tools.adtui.model.AspectModel;
import com.android.tools.adtui.model.Range;
import com.android.tools.adtui.model.updater.Updatable;
import com.android.tools.adtui.model.updater.Updater;
import com.google.common.annotations.VisibleForTesting;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

/**
 * A helper object that manages the current view and selection ranges for the Studio Profilers.
 */
public final class ProfilerTimeline extends AspectModel<ProfilerTimeline.Aspect> implements Updatable {

  public enum Aspect {
    STREAMING
  }

  @VisibleForTesting
  public static final long DEFAULT_VIEW_LENGTH_US = TimeUnit.SECONDS.toMicros(30);

  @NotNull private final Range myDataRangeUs;
  @NotNull private final Range myViewRangeUs;
  @NotNull private final Range mySelectionRangeUs;
  @NotNull private final Range myTooltipRangeUs;

  private boolean myStreaming;

  private float myStreamingFactor;
  private double myZoomLeft;

  private boolean myCanStream = true;
  private long myDataStartTimeNs;
  private long myDataLengthNs;
  private boolean myIsPaused = false;
  private long myPausedTime;

  public ProfilerTimeline() {
    myDataRangeUs = new Range(0, 0);
    myViewRangeUs = new Range(0, 0);
    mySelectionRangeUs = new Range(); // Empty range
    myTooltipRangeUs = new Range(); // Empty range
  }

  /**
   * Change the streaming mode of this timeline. If canStream is currently set to false (e.g. while user is scrolling or zooming), then
   * nothing happens if the caller tries to enable streaming.
   */
  public void setStreaming(boolean isStreaming) {
    if (!myCanStream && isStreaming) {
      isStreaming = false;
    }

    if (myStreaming == isStreaming) {
      return;
    }
    assert myCanStream || !isStreaming;

    myStreaming = isStreaming;
    // Update the ranges as if no time has passed.
    update(0);

    changed(Aspect.STREAMING);
  }

  public boolean isStreaming() {
    return myStreaming && !myIsPaused;
  }

  /**
   * Sets whether the timeline can turn on streaming. If this is set to false and the timeline is currently streaming, streaming mode
   * will be toggled off.
   */
  public void setCanStream(boolean canStream) {
    myCanStream = canStream;
    if (!myCanStream && myStreaming) {
      setStreaming(false);
    }
  }

  public boolean canStream() {
    return myCanStream && !myIsPaused;
  }

  public void toggleStreaming() {
    myZoomLeft = 0.0;
    setStreaming(!isStreaming());
  }

  public boolean isPaused() {
    return myIsPaused;
  }

  public void setIsPaused(boolean paused) {
    myIsPaused = paused;
    if (myIsPaused) {
      myPausedTime = myDataLengthNs;
    }
  }

  @NotNull
  public Range getDataRange() {
    return myDataRangeUs;
  }

  @NotNull
  public Range getViewRange() {
    return myViewRangeUs;
  }

  public Range getSelectionRange() {
    return mySelectionRangeUs;
  }

  public Range getTooltipRange() {
    return myTooltipRangeUs;
  }

  @Override
  public void update(long elapsedNs) {
    myDataLengthNs += elapsedNs;
    long maxTimelineTimeNs = myDataLengthNs;
    if (myIsPaused) {
      maxTimelineTimeNs = myPausedTime;
    }

    long deviceNowNs = myDataStartTimeNs + maxTimelineTimeNs;
    long deviceNowUs = TimeUnit.NANOSECONDS.toMicros(deviceNowNs);
    myDataRangeUs.setMax(deviceNowUs);
    double viewUs = myViewRangeUs.getLength();
    if (myStreaming) {
      myStreamingFactor = Updater.lerp(myStreamingFactor, 1.0f, 0.95f, elapsedNs, Float.MIN_VALUE);
      double min = Updater.lerp(myViewRangeUs.getMin(), deviceNowUs - viewUs, myStreamingFactor);
      double max = Updater.lerp(myViewRangeUs.getMax(), deviceNowUs, myStreamingFactor);
      myViewRangeUs.set(min, max);
    }
    else {
      myStreamingFactor = 0.0f;
    }
    double left = Updater.lerp(myZoomLeft, 0.0, 0.95f, elapsedNs, myViewRangeUs.getLength() * 0.0001f);
    zoom(myZoomLeft - left, myStreaming ? 1.0 : 0.5f);
    myZoomLeft = left;
  }

  public void zoom(double deltaUs, double percent) {
    if (deltaUs == 0.0) {
      return;
    }
    if (deltaUs < 0 && percent < 1.0) {
      setStreaming(false);
    }
    double minUs = myViewRangeUs.getMin() - deltaUs * percent;
    double maxUs = myViewRangeUs.getMax() + deltaUs * (1 - percent);
    // When the view range is not fully covered, reset minUs to data range could change zoomLeft from zero to a large number.
    boolean isDataRangeCoverViewRange = myDataRangeUs.getMin() <= myViewRangeUs.getMin();
    if (isDataRangeCoverViewRange && minUs < myDataRangeUs.getMin()) {
      maxUs += myDataRangeUs.getMin() - minUs;
      minUs = myDataRangeUs.getMin();
    }
    if (maxUs > myDataRangeUs.getMax()) {
      minUs -= maxUs - myDataRangeUs.getMax();
      maxUs = myDataRangeUs.getMax();
    }
    // minUs could have gone past again.
    if (isDataRangeCoverViewRange) {
      minUs = Math.max(minUs, myDataRangeUs.getMin());
    }
    myViewRangeUs.set(minUs, maxUs);
  }

  public void zoomOut() {
    myZoomLeft += myViewRangeUs.getLength() * 0.1f;
  }

  public void zoomIn() {
    myZoomLeft -= myViewRangeUs.getLength() * 0.1f;
  }

  public void resetZoom() {
    myZoomLeft = DEFAULT_VIEW_LENGTH_US - myViewRangeUs.getLength();
  }

  public void pan(double deltaUs) {
    if (deltaUs < 0) {
      setStreaming(false);
    }
    if (myViewRangeUs.getMin() + deltaUs < myDataRangeUs.getMin()) {
      deltaUs = myDataRangeUs.getMin() - myViewRangeUs.getMin();
    }
    else if (myViewRangeUs.getMax() + deltaUs > myDataRangeUs.getMax()) {
      deltaUs = myDataRangeUs.getMax() - myViewRangeUs.getMax();
    }
    myViewRangeUs.shift(deltaUs);
  }

  /**
   * This function resets the internal state to the timeline.
   *
   * @param startTimeNs the time which should be the 0 value on the timeline.
   * @param lengthNs    the initial length of the timeline. This accounts for the time delay between when a session starts and the device's
   *                    current time when the timeline is reset.
   */
  public void reset(long startTimeNs, long lengthNs) {
    myDataStartTimeNs = startTimeNs;
    myDataLengthNs = lengthNs;
    myIsPaused = false;
    double startTimeUs = TimeUnit.NANOSECONDS.toMicros(myDataStartTimeNs);
    long deviceNowNs = myDataStartTimeNs + myDataLengthNs;
    long deviceNowUs = TimeUnit.NANOSECONDS.toMicros(deviceNowNs);
    myDataRangeUs.set(startTimeUs, deviceNowUs);
    myViewRangeUs.set(deviceNowUs - DEFAULT_VIEW_LENGTH_US, deviceNowUs);
    setStreaming(true);
  }

  public long getDataStartTimeNs() {
    return myDataStartTimeNs;
  }

  /**
   * @param absoluteTimeNs the device time in nanoseconds.
   * @return time relative to the data start time (e.g. zero on the timeline), in microseconds.
   */
  public long convertToRelativeTimeUs(long absoluteTimeNs) {
    return TimeUnit.NANOSECONDS.toMicros(absoluteTimeNs - myDataStartTimeNs);
  }
}
