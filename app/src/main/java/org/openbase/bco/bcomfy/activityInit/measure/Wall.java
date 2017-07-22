package org.openbase.bco.bcomfy.activityInit.measure;

import org.rajawali3d.math.vector.Vector3;

import java.util.ArrayList;

import java8.util.stream.StreamSupport;

public class Wall {

    private ArrayList<Plane> measurements;
    private Plane finishedWall;
    private int measurementsNeeded;
    private boolean finished;

    public Wall(int measurementsNeeded) {
        measurements = new ArrayList<>();
        this.measurementsNeeded = measurementsNeeded;
        finished = false;
    }

    public boolean addMeasurement(Plane measurement) {
        measurements.add(measurement);

        if (measurements.size() == measurementsNeeded) {
            Vector3 meanPosition = new Vector3();
            Vector3 meanNormal = new Vector3();

            StreamSupport.stream(measurements).forEach(plane -> {
                meanPosition.add(plane.getPosition());
                meanNormal.add(plane.getNormal());
            });

            meanPosition.divide(getMeasurementCount());
            meanNormal.normalize();

            finishedWall = new Plane(meanPosition, meanNormal);

            finished = true;
        }

        return finished;
    }

    public int getMeasurementCount() {
        return measurements.size();
    }

    public boolean isFinished() {
        return finished;
    }

    public Plane getFinishedWall() {
        return finishedWall;
    }

}
