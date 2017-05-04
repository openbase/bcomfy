package org.openbase.bco.bcomfy.utils;

import android.os.AsyncTask;
import android.util.Log;

import com.projecttango.tangosupport.TangoSupport;

import org.openbase.bco.bcomfy.interfaces.OnTaskFinishedListener;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.rajawali3d.math.vector.Vector3;

import java.util.ArrayList;

import java8.util.stream.StreamSupport;
import rsb.introspection.LacksOsInformationException;
import rsb.util.os.RuntimeOsUtilities;
import rst.domotic.unit.UnitConfigType;
import rst.geometry.PoseType;
import rst.geometry.RotationType;
import rst.geometry.TranslationType;
import rst.math.Vec3DDoubleType;
import rst.spatial.PlacementConfigType;
import rst.spatial.ShapeType;

public final class LocationUtils {

    private static final String TAG = LocationUtils.class.getSimpleName();

    public static class updateLocationShapeTask extends AsyncTask<Void, Void, Void> {
        private static final String TAG = LocationUtils.updateLocationShapeTask.class.getSimpleName();
        private String locationId;
        ArrayList<Vector3> ground;
        double[] glToBcoTransform;
        private OnTaskFinishedListener listener;

        public updateLocationShapeTask(String locationId, ArrayList<Vector3> ground, double[] glToBcoTransform, OnTaskFinishedListener listener) {
            this.locationId = locationId;
            this.ground = ground;
            this.glToBcoTransform = glToBcoTransform;
            this.listener = listener;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            // Transform vertices into BCO world
            ArrayList<Vector3> transformedGround = new ArrayList<>();
            StreamSupport.stream(ground).forEach(vector3 -> transformedGround.add(
                    new Vector3(TangoSupport.doubleTransformPoint(glToBcoTransform, vector3.toArray()))
            ));


            // Calculate the origin of the location (i.e. the most north western vertex)
            int originVertexIndex = -1;
            double originVertexCoordinateSum = Double.MAX_VALUE;
            for (int currentVertexIndex = 0; currentVertexIndex < transformedGround.size(); currentVertexIndex++) {
                double currentVertexCoordinateSum = transformedGround.get(currentVertexIndex).x + transformedGround.get(currentVertexIndex).z;

                if (currentVertexCoordinateSum < originVertexCoordinateSum) {
                    originVertexIndex = currentVertexIndex;
                    originVertexCoordinateSum = currentVertexCoordinateSum;
                }
            }
            Vector3 originVertex = transformedGround.get(originVertexIndex).clone();

            // Align all the vertices according to the origin
            for (Vector3 vertex : transformedGround) vertex.subtract(originVertex);

            // Publish the result to the locationRegistry
            try {
                // Fetch the UnitConfig of the target location
                UnitConfigType.UnitConfig locationConfig = Registries.getLocationRegistry().getLocationConfigById(locationId);

                // Build the pose
                TranslationType.Translation.Builder translationBuilder = TranslationType.Translation.getDefaultInstance().toBuilder();
//                translationBuilder.setX(originVertex.z).setY(originVertex.x).setZ(originVertex.y);
                translationBuilder.setX(originVertex.x).setY(originVertex.y).setZ(originVertex.z);
                RotationType.Rotation.Builder rotationBuilder = RotationType.Rotation.getDefaultInstance().toBuilder();
                rotationBuilder.setQw(1.0).setQx(0.0).setQy(0.0).setQz(0.0);

                PoseType.Pose pose = locationConfig.getPlacementConfig().getPosition().toBuilder()
                        .setRotation(rotationBuilder.build()).setTranslation(translationBuilder.build()).build();

                // Build the shape
                ShapeType.Shape.Builder shapeBuilder = ShapeType.Shape.getDefaultInstance().toBuilder();
                for (Vector3 vector3 : transformedGround) {
                    shapeBuilder.addFloor(Vec3DDoubleType.Vec3DDouble.getDefaultInstance().toBuilder()
//                            .setX(vector3.z).setY(vector3.x).setZ(vector3.y).build());
                            .setX(vector3.x).setY(vector3.y).setZ(vector3.z).build());
                }

                // Build the locationConfig
                PlacementConfigType.PlacementConfig placementConfig = locationConfig.getPlacementConfig().toBuilder().clearShape().setShape(shapeBuilder.build()).setPosition(pose).build();
                UnitConfigType.UnitConfig newLocationConfig = locationConfig.toBuilder().clearPlacementConfig().setPlacementConfig(placementConfig).build();

                // Update the locationConfig
                Registries.getLocationRegistry().updateLocationConfig(newLocationConfig);
            } catch (InterruptedException | CouldNotPerformException ex) {
                ex.printStackTrace();
            } catch (LacksOsInformationException | RuntimeOsUtilities.RuntimeNotAvailableException e) {
                Log.w(TAG, "No PID information available.");
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void v) {
            listener.taskFinishedCallback();
        }
    }

}
