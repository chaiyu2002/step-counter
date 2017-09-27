package com.base.basepedo.pojo;

import com.litesuits.orm.db.annotation.Column;
import com.litesuits.orm.db.annotation.PrimaryKey;
import com.litesuits.orm.db.annotation.Table;
import com.litesuits.orm.db.enums.AssignType;

/**
 * @创建者 倪军
 * @创建时间 27/09/2017
 * @描述
 */

@Table("stepcounter")
public class StepCounter {
    @PrimaryKey(AssignType.AUTO_INCREMENT)
    private int id;
    @Column("step")
    private int step;

    @Override
    public String toString() {
        return "StepCounter{" +
                "id=" + id +
                ", step=" + step +
                '}';
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getStep() {
        return step;
    }

    public void setStep(int step) {
        this.step = step;
    }
}
