package com.nikhu.fitness.app;

import java.util.Date;

/**
 * Created by bujji on 10-05-2014.
 */
public class StepsInfo {
    private Date sessionDate;
    private int steps;
    private float calories;
    private float distance;

    public Date getSessionDate() {
        return sessionDate;
    }

    public void setSessionDate(Date sessionDate) {
        this.sessionDate = sessionDate;
    }

    public int getSteps() {
        return steps;
    }

    public void setSteps(int steps) {
        this.steps = steps;
    }

    public float getCalories() {
        return calories;
    }

    public void setCalories(float calories) {
        this.calories = calories;
    }

    public float getDistance() {
        return distance;
    }

    public void setDistance(float distance) {
        this.distance = distance;
    }
}
