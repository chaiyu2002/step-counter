package com.base.basepedo.service;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.util.Log;

import com.base.basepedo.base.StepMode;
import com.base.basepedo.callback.StepCallBack;
import com.base.basepedo.pojo.Acceleration;
import com.base.basepedo.pojo.Gravity;
import com.base.basepedo.utils.CountDownTimer;
import com.litesuits.orm.LiteOrm;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by base on 2016/8/17.
 */
public class StepInAcceleration extends StepMode {
    private final String TAG = "StepInAcceleration";
    private Context context;
    //存放三轴数据
    final int valueNum = 5;
    //用于存放计算阈值的波峰波谷差值
    float[] tempValue = new float[valueNum];
    int tempCount = 0;
    //是否上升的标志位
    boolean isDirectionUp = false;
    //持续上升次数
    int continueUpCount = 0;
    //上一点的持续上升的次数，为了记录波峰的上升次数
    int continueUpFormerCount = 0;
    //上一点的状态，上升还是下降
    boolean lastStatus = false;
    //波峰值
    float peakOfWave = 0;
    //波谷值
    float valleyOfWave = 0;
    //此次波峰的时间
    long timeOfThisPeak = 0;
    //上次波峰的时间
    long timeOfLastPeak = 0;
    //当前的时间
    long timeOfNow = 0;
    //当前传感器的值
    float gravityNew = 0;
    //上次传感器的值
    float gravityOld = 0;
    //动态阈值需要动态的数据，这个值用于这些动态数据的阈值
    final float initialValue = (float) 1.7;
    //初始阈值
    float ThreadValue = (float) 2.0;

    //初始范围
    float minValue = 11f;
    float maxValue = 19.6f;

    /**
     * 0-准备计时   1-计时中  2-正常计步中
     */
    private int CountTimeState = 0;
    public static int TEMP_STEP = 0;
    private int lastStep = -1;
    //用x、y、z轴三个维度算出的平均值
    public static float average = 0;
    private Timer timer;
    // 倒计时3.5秒，3.5秒内不会显示计步，用于屏蔽细微波动
    private long duration = 3500;
    private TimeCount time;
    private LiteOrm liteOrm;

    public StepInAcceleration(Context context, StepCallBack stepCallBack) {
        super(context, stepCallBack);
        this.context = context;
        liteOrm = LiteOrm.newCascadeInstance(context, "acceleration.db");
        liteOrm.setDebugged(false);
        TimeCounter counter = new TimeCounter(10000, 1000);
        counter.start();
        acceleration = new Acceleration();
        gravity = new Gravity();
    }

    @Override
    protected void registerSensor() {
        addBasePedoListener();
    }

    private void addBasePedoListener() {
        // 获得传感器的类型，这里获得的类型是加速度传感器
        // 此方法用来注册，只有注册过才会生效，参数：SensorEventListener的实例，Sensor的实例，更新速率
        Sensor sensor = sensorManager
                .getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        // sensorManager.unregisterListener(stepDetector);
        isAvailable = sensorManager.registerListener(this, sensor,
                SensorManager.SENSOR_DELAY_UI);
        if (isAvailable) {
            Log.v(TAG, "加速度传感器可以使用");
        } else {
            Log.v(TAG, "加速度传感器无法使用");
        }

        Sensor detectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        isAvailable = sensorManager.registerListener(this, detectorSensor, SensorManager.SENSOR_DELAY_UI);
        if (isAvailable) {
            Log.v(TAG, "STEP_DETECTOR传感器可以使用");
        } else {
            Log.v(TAG, "STEP_DETECTOR传感器无法使用");
        }

        Sensor gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        isAvailable = sensorManager.registerListener(this, gravitySensor, SensorManager.SENSOR_DELAY_UI);
        if (isAvailable) {
            Log.v(TAG, "GRAVITY传感器可以使用");
        } else {
            Log.v(TAG, "GRAVITY传感器无法使用");
        }

        Sensor gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        isAvailable = sensorManager.registerListener(this, gyroscopeSensor, SensorManager.SENSOR_DELAY_UI);
        if (isAvailable) {
            Log.v(TAG, "GYROSCOPE传感器可以使用");
        } else {
            Log.v(TAG, "GYROSCOPE传感器无法使用");
        }

        // Sensor countSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        // if (detectorSensor != null) {
        // } else if (countSensor != null) {
        //     sensorManager.registerListener(this, countSensor, SensorManager.SENSOR_DELAY_UI);
        //     isAvailable = true;
        //TODO 注册所有传感器。

    }

    public void onAccuracyChanged(Sensor arg0, int arg1) {
    }

    public Acceleration acceleration = null;
    public Gravity gravity = null;
    public static List<Acceleration> accelerationList = new ArrayList<>();

    private double gravitys[] = new double[3];
    private double linear_acceleration[] = new double[3];

    private static final float NS2S = 1.0f / 1000000000.0f;
    public final float[] deltaRotationVector = new float[4];
    private float timestamp;

    public void onSensorChanged(SensorEvent event) {

        Sensor sensor = event.sensor;
        synchronized (this) {
            switch (sensor.getType()) {
                case Sensor.TYPE_ACCELEROMETER:
                    calc_step(event);
                    // Log.d("StepInAcceleration", "event.values[0]:" + event.values[0]);
                    // Log.d("StepInAcceleration", "event.values[1]:" + event.values[1]);
                    // Log.d("StepInAcceleration", "event.values[2]:" + event.values[2]);

                    float alpha = 0.8f;

                    gravitys[0] = alpha * gravitys[0] + (1 - alpha) * event.values[0];
                    gravitys[1] = alpha * gravitys[1] + (1 - alpha) * event.values[1];
                    gravitys[2] = alpha * gravitys[2] + (1 - alpha) * event.values[2];

                    linear_acceleration[0] = event.values[0] - gravitys[0];
                    linear_acceleration[1] = event.values[1] - gravitys[1];
                    linear_acceleration[2] = event.values[2] - gravitys[2];

                    acceleration = new Acceleration();
                    acceleration.setX((float) linear_acceleration[0]);
                    acceleration.setY((float) linear_acceleration[1]);
                    acceleration.setZ((float) linear_acceleration[2]);
                    accelerationList.add(acceleration);
                    // liteOrm.insert(acceleration);
                    break;
                case Sensor.TYPE_GRAVITY:
                    gravitys[0] = event.values[0];
                    gravitys[1] = event.values[1];
                    gravitys[2] = event.values[2];

                    gravity.setX(event.values[0]);
                    gravity.setY(event.values[1]);
                    gravity.setZ(event.values[2]);
                    break;
                case Sensor.TYPE_STEP_DETECTOR:
                    synchronized (this) {
                        save();
                    }
                    break;
                case Sensor.TYPE_GYROSCOPE:
                    // This time step's delta rotation to be multiplied by the current rotation
                    // after computing it from the gyro sample data.
                    if (timestamp != 0) {
                        final float dT = (event.timestamp - timestamp) * NS2S;
                        // Axis of the rotation sample, not normalized yet.
                        float axisX = event.values[0];
                        float axisY = event.values[1];
                        float axisZ = event.values[2];

                        // Calculate the angular speed of the sample
                        float omegaMagnitude = (float) Math.sqrt(axisX*axisX + axisY*axisY + axisZ*axisZ);

                        // Normalize the rotation vector if it's big enough to get the axis
                        // TODO
                        // if (omegaMagnitude > EPSILON) {
                        //     axisX /= omegaMagnitude;
                        //     axisY /= omegaMagnitude;
                        //     axisZ /= omegaMagnitude;
                        // }

                        float thetaOverTwo = omegaMagnitude * dT / 2.0f;
                        float sinThetaOverTwo = (float) Math.sin(thetaOverTwo);
                        float cosThetaOverTwo = (float) Math.cos(thetaOverTwo);
                        deltaRotationVector[0] = sinThetaOverTwo * axisX;
                        deltaRotationVector[1] = sinThetaOverTwo * axisY;
                        deltaRotationVector[2] = sinThetaOverTwo * axisZ;
                        deltaRotationVector[3] = cosThetaOverTwo;
                    }
                    timestamp = event.timestamp;
                    float[] deltaRotationMatrix = new float[9];
                    SensorManager.getRotationMatrixFromVector(deltaRotationMatrix, deltaRotationVector);
                    // User code should concatenate the delta rotation we computed with the current rotation
                    // in order to get the updated rotation.
                    // rotationCurrent = rotationCurrent * deltaRotationMatrix;
                    break;
            }
        }
    }


    public void save() {
        StringBuilder sb = new StringBuilder();
        for (Acceleration item : accelerationList) {
            sb.append(item.toString());
        }


        File file = context.getFilesDir();
        Log.d("StepInAcceleration", file.getAbsolutePath());
        // FileOutputStream fos = null;
        // try {
        //     fos = new FileOutputStream(file);
        //     fos.write(sb.toString().getBytes());
        // } catch (FileNotFoundException e) {
        //     e.printStackTrace();
        // } catch (IOException e) {
        //     e.printStackTrace();
        // } finally {
        //     accelerationList.clear();
        //     try {
        //         fos.close();
        //     } catch (IOException e) {
        //         e.printStackTrace();
        //     }
        // }
    }

    class TimeCounter extends CountDownTimer {

        public TimeCounter(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onTick(long millisUntilFinished) {
            Log.d("TimeCounter", "millisUntilFinished:" + millisUntilFinished);
            // acceleration = new Acceleration();
            // acceleration.setX(x);
            // acceleration.setY(y);
            // acceleration.setZ(z);
            // liteOrm.insert(acceleration);
        }

        @Override
        public void onFinish() {

        }
    }

    synchronized private void calc_step(SensorEvent event) {
        average = (float) Math.sqrt(Math.pow(event.values[0], 2)
                + Math.pow(event.values[1], 2) + Math.pow(event.values[2], 2));
        detectorNewStep(average);
    }

    /*
     * 检测步子，并开始计步
	 * 1.传入sersor中的数据
	 * 2.如果检测到了波峰，并且符合时间差以及阈值的条件，则判定为1步
	 * 3.符合时间差条件，波峰波谷差值大于initialValue，则将该差值纳入阈值的计算中
	 * */
    public void detectorNewStep(float values) {
        if (gravityOld == 0) {
            gravityOld = values;
        } else {
            if (DetectorPeak(values, gravityOld)) {
                timeOfLastPeak = timeOfThisPeak;
                timeOfNow = System.currentTimeMillis();

                // TODO
                if (timeOfNow - timeOfLastPeak >= 200
                        && (peakOfWave - valleyOfWave >= ThreadValue) && (timeOfNow - timeOfLastPeak) <= 2000) {
                    timeOfThisPeak = timeOfNow;
                    //更新界面的处理，不涉及到算法
                    preStep();
                }
                if (timeOfNow - timeOfLastPeak >= 200
                        && (peakOfWave - valleyOfWave >= initialValue)) {
                    timeOfThisPeak = timeOfNow;
                    ThreadValue = Peak_Valley_Thread(peakOfWave - valleyOfWave);
                }
            }
        }
        gravityOld = values;
    }

    private void preStep() {
        if (CountTimeState == 0) {
            // 开启计时器
            time = new TimeCount(duration, 700);
            time.start();
            CountTimeState = 1;
            Log.v(TAG, "开启计时器");
        } else if (CountTimeState == 1) {
            TEMP_STEP++;
            Log.v(TAG, "计步中 TEMP_STEP:" + TEMP_STEP);
        } else if (CountTimeState == 2) {
            CURRENT_SETP++;
            if (stepCallBack != null) {
                stepCallBack.Step(CURRENT_SETP);
            }
        }
    }


    /**
     * 检测波峰
     * 以下四个条件判断为波峰：
     * 1.目前点为下降的趋势：isDirectionUp为false
     * 2.之前的点为上升的趋势：lastStatus为true
     * 3.到波峰为止，持续上升大于等于2次
     * 4.波峰值大于1.2g,小于2g
     * 记录波谷值
     * 1.观察波形图，可以发现在出现步子的地方，波谷的下一个就是波峰，有比较明显的特征以及差值
     * 2.所以要记录每次的波谷值，为了和下次的波峰做对比
     *
     * @return true is peak,false is valley
     */
    public boolean DetectorPeak(float newValue, float oldValue) {
        lastStatus = isDirectionUp;
        if (newValue >= oldValue) {
            isDirectionUp = true;
            continueUpCount++;
        } else {
            continueUpFormerCount = continueUpCount;
            continueUpCount = 0;
            isDirectionUp = false;
        }

        //        Log.v(TAG, "oldValue:" + oldValue);
        if (!isDirectionUp && lastStatus
                && (continueUpFormerCount >= 2 && (oldValue >= minValue && oldValue < maxValue))) {
            peakOfWave = oldValue;
            return true;
        } else if (!lastStatus && isDirectionUp) {
            valleyOfWave = oldValue;
            return false;
        } else {
            return false;
        }
    }

    /*
     * 阈值的计算
     * 1.通过波峰波谷的差值计算阈值
     * 2.记录4个值，存入tempValue[]数组中
     * 3.在将数组传入函数averageValue中计算阈值
     * */
    public float Peak_Valley_Thread(float value) {
        float tempThread = ThreadValue;
        if (tempCount < valueNum) {
            tempValue[tempCount] = value;
            // tempCount 不重置吗？
            tempCount++;
        } else {
            tempThread = averageValue(tempValue, valueNum);
            for (int i = 1; i < valueNum; i++) {
                tempValue[i - 1] = tempValue[i];
            }
            tempValue[valueNum - 1] = value;
        }
        return tempThread;

    }

    /*
     * 梯度化阈值
     * 1.计算数组的均值
     * 2.通过均值将阈值梯度化在一个范围里
     * */
    public float averageValue(float value[], int n) {
        float ave = 0;
        for (int i = 0; i < n; i++) {
            ave += value[i];
        }
        ave = ave / valueNum;
        if (ave >= 8) {
            //            Log.v(TAG, "超过8");
            ave = (float) 4.3;
        } else if (ave >= 7 && ave < 8) {
            //            Log.v(TAG, "7-8");
            ave = (float) 3.3;
        } else if (ave >= 4 && ave < 7) {
            //            Log.v(TAG, "4-7");
            ave = (float) 2.3;
        } else if (ave >= 3 && ave < 4) {
            //            Log.v(TAG, "3-4");
            ave = (float) 2.0;
        } else {
            //            Log.v(TAG, "else");
            ave = (float) 1.7;
        }
        return ave;
    }

    class TimeCount extends CountDownTimer {
        public TimeCount(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onFinish() {
            // 如果计时器正常结束，则开始计步
            time.cancel();
            CURRENT_SETP += TEMP_STEP;
            lastStep = -1;
            Log.v(TAG, "计时正常结束");

            timer = new Timer(true);
            TimerTask task = new TimerTask() {
                public void run() {
                    // 步数没有变化
                    if (lastStep == CURRENT_SETP) {
                        timer.cancel();
                        CountTimeState = 0;
                        lastStep = -1;
                        TEMP_STEP = 0;
                        Log.v(TAG, "停止计步：" + CURRENT_SETP);
                    } else {
                        lastStep = CURRENT_SETP;
                    }
                }
            };
            timer.schedule(task, 0, 2000);
            CountTimeState = 2;
        }

        @Override
        public void onTick(long millisUntilFinished) {
            if (lastStep == TEMP_STEP) {
                Log.v(TAG, "onTick 计时停止:" + TEMP_STEP);
                time.cancel();
                CountTimeState = 0;
                lastStep = -1;
                TEMP_STEP = 0;
            } else {
                lastStep = TEMP_STEP;
            }
        }
    }

}
