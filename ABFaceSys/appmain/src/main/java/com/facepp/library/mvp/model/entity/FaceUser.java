package com.facepp.library.mvp.model.entity;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.NotNull;
import org.greenrobot.greendao.annotation.Property;
import org.greenrobot.greendao.annotation.Transient;
import org.greenrobot.greendao.annotation.Generated;

/**
 * Created by Administrator on 2017/9/29 0029.
 */

@Entity
public class FaceUser {
    @Id(autoincrement = true)
    private Long id;

    @NotNull
    @Property
    private String faceToken;
    private String gender;
    private int age;
    private String name;
    @Transient
    private int tempUsageCount;
    @Generated(hash = 743659282)
    public FaceUser(Long id, @NotNull String faceToken, String gender, int age,
            String name) {
        this.id = id;
        this.faceToken = faceToken;
        this.gender = gender;
        this.age = age;
        this.name = name;
    }
    @Generated(hash = 1024976930)
    public FaceUser() {
    }
    public Long getId() {
        return this.id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getFaceToken() {
        return this.faceToken;
    }
    public void setFaceToken(String faceToken) {
        this.faceToken = faceToken;
    }
    public String getGender() {
        return this.gender;
    }
    public void setGender(String gender) {
        this.gender = gender;
    }
    public int getAge() {
        return this.age;
    }
    public void setAge(int age) {
        this.age = age;
    }
    public String getName() {
        return this.name;
    }
    public void setName(String name) {
        this.name = name;
    }
}
