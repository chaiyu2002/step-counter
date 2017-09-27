package com.base.basepedo.pojo;

import com.litesuits.orm.db.annotation.Column;
import com.litesuits.orm.db.annotation.PrimaryKey;
import com.litesuits.orm.db.annotation.Table;
import com.litesuits.orm.db.enums.AssignType;

import java.io.Serializable;

/**
 * @创建者 倪军
 * @创建时间 26/09/2017
 * @描述
 */

@Table("gyroscope")
public class Gyroscope implements Serializable {

    @PrimaryKey(AssignType.AUTO_INCREMENT)
    private int id;

    @Column("axisX")
    private float axisX;
    @Column("axisY")
    private float axisY;
    @Column("axisZ")
    private float axisZ;
    @Column("average")
    private float average;
    @Column("timestamp")
    private long timestamp;

    @Override
    public String toString() {
        return "Gyroscope     " + "average=" + average;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public float getAxisX() {
        return axisX;
    }

    public void setAxisX(float axisX) {
        this.axisX = axisX;
    }

    public float getAxisY() {
        return axisY;
    }

    public void setAxisY(float axisY) {
        this.axisY = axisY;
    }

    public float getAxisZ() {
        return axisZ;
    }

    public void setAxisZ(float axisZ) {
        this.axisZ = axisZ;
    }

    public float getAverage() {
        return average;
    }

    public void setAverage(float average) {
        this.average = average;
    }
}
