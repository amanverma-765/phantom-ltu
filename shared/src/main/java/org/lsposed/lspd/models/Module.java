package org.lsposed.lspd.models;

import android.os.Parcel;
import android.os.Parcelable;

public class Module implements Parcelable {
    public String packageName;
    public String apkPath;
    public PreLoadedApk file;

    public Module() {
    }

    protected Module(Parcel in) {
        packageName = in.readString();
        apkPath = in.readString();
        file = in.readParcelable(PreLoadedApk.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(packageName);
        dest.writeString(apkPath);
        dest.writeParcelable(file, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Module> CREATOR = new Creator<Module>() {
        @Override
        public Module createFromParcel(Parcel in) {
            return new Module(in);
        }

        @Override
        public Module[] newArray(int size) {
            return new Module[size];
        }
    };

    @Override
    public String toString() {
        return "Module{" +
                "packageName='" + packageName + '\'' +
                ", apkPath='" + apkPath + '\'' +
                '}';
    }
}
