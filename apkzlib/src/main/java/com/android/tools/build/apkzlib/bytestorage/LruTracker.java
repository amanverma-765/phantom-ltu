package com.android.tools.build.apkzlib.bytestorage;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import java.util.TreeSet;

import javax.annotation.Nullable;

class LruTracker<T> {

  private final BiMap<T, Integer> objectToAccessTime;

  private final TreeSet<Integer> accessTimes;

  private int currentTime;

  LruTracker() {
    currentTime = 1;
    objectToAccessTime = HashBiMap.create();
    accessTimes = new TreeSet<>((i0, i1) -> i1 - i0);
  }

  synchronized void track(T object) {
    Preconditions.checkState(!objectToAccessTime.containsKey(object));
    objectToAccessTime.put(object, currentTime);
    accessTimes.add(currentTime);
    currentTime++;
  }

  synchronized void untrack(T object) {
    Preconditions.checkState(objectToAccessTime.containsKey(object));
    accessTimes.remove(objectToAccessTime.get(object));
    objectToAccessTime.remove(object);
  }

  synchronized void access(T object) {
    untrack(object);
    track(object);
  }

  synchronized int positionOf(T object) {
    Preconditions.checkState(objectToAccessTime.containsKey(object));
    int lastAccess = objectToAccessTime.get(object);
    return accessTimes.headSet(lastAccess).size();
  }

  @Nullable
  synchronized T last() {
    if (accessTimes.isEmpty()) {
      return null;
    }

    return objectToAccessTime.inverse().get(accessTimes.last());
  }
}