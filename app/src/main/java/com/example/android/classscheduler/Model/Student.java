package com.example.android.classscheduler.Model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by jonathanbarrera on 6/7/18.
 */

public class Student implements Parcelable{

    private String mName;
    private int mSex;
    private int mAge;
    private int mGrade;
    private String mClasses;
    private String mPhotoUrl;
    private String mStudentId;

    public Student() {
    }

    public Student(String name, int sex, int age, int grade, String classes, String photoUrl,
                   String studentId) {
        this.mName = name;
        this.mSex = sex;
        this.mAge = age;
        this.mGrade = grade;
        this.mClasses = classes;
        this.mPhotoUrl = photoUrl;
        this.mStudentId = studentId;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        this.mName = name;
    }

    public int getSex() { return mSex; }

    public void setSex(int sex) { this.mSex = sex; }

    public int getAge() { return mAge; }

    public void setAge(int age) { this.mAge = age; }

    public int getGrade() { return mGrade; }

    public void setGrade(int grade) {this.mGrade = grade; }

    public String getClasses() { return mClasses; }

    public void setClasses(String classes) { this.mClasses = classes; }

    public String getPhotoUrl() {
        return mPhotoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.mPhotoUrl = photoUrl;
    }

    public String getStudentId() { return mStudentId; }

    public void setStudentId(String studentId) { this.mStudentId = studentId; }

    public static final Creator<Student> CREATOR = new Creator<Student>() {
        @Override
        public Student createFromParcel(Parcel in) {
            return new Student(in);
        }

        @Override
        public Student[] newArray(int size) {
            return new Student[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mName);
        dest.writeInt(mSex);
        dest.writeInt(mAge);
        dest.writeInt(mGrade);
        dest.writeString(mClasses);
        dest.writeString(mPhotoUrl);
        dest.writeString(mStudentId);
    }

    protected Student(Parcel in) {
        mName = in.readString();
        mSex = in.readInt();
        mAge = in.readInt();
        mGrade = in.readInt();
        mClasses = in.readString();
        mPhotoUrl = in.readString();
        mStudentId = in.readString();
    }
}