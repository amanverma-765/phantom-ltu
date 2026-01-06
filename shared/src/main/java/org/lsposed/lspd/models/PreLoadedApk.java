package org.lsposed.lspd.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.SharedMemory;

import java.util.ArrayList;
import java.util.List;

public class PreLoadedApk implements Parcelable {
    public List<SharedMemory> preLoadedDexes = new ArrayList<>();
    public List<String> moduleClassNames = new ArrayList<>();
    public List<String> moduleLibraryNames = new ArrayList<>();

    public PreLoadedApk() {
    }

    protected PreLoadedApk(Parcel in) {
        in.readList(preLoadedDexes, SharedMemory.class.getClassLoader());
        moduleClassNames = in.createStringArrayList();
        moduleLibraryNames = in.createStringArrayList();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeList(preLoadedDexes);
        dest.writeStringList(moduleClassNames);
        dest.writeStringList(moduleLibraryNames);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<PreLoadedApk> CREATOR = new Creator<PreLoadedApk>() {
        @Override
        public PreLoadedApk createFromParcel(Parcel in) {
            return new PreLoadedApk(in);
        }

        @Override
        public PreLoadedApk[] newArray(int size) {
            return new PreLoadedApk[size];
        }
    };
}
