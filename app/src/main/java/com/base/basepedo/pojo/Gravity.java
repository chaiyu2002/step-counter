package com.base.basepedo.pojo;

import com.litesuits.orm.db.annotation.Column;
import com.litesuits.orm.db.annotation.PrimaryKey;
import com.litesuits.orm.db.annotation.Table;
import com.litesuits.orm.db.enums.AssignType;

import java.io.Serializable;

/**
 * @创建者 倪军
 * @创建时间 27/09/2017
 * @描述
 */
@Table("gravity")
public class Gravity implements Serializable{

    @PrimaryKey(AssignType.AUTO_INCREMENT)
    private int id;

    @Column("x")
    private float x;
    @Column("y")
    private float y;
    @Column("z")
    private float z;

    @Override
    public String toString() {
        return "Gravity{" +
                "id=" + id +
                ", x=" + x +
                ", y=" + y +
                ", z=" + z +
                '}';
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getZ() {
        return z;
    }

    public void setZ(float z) {
        this.z = z;
    }
}
