package org.openbase.bco.bcomfy.activityCore.serviceList;


import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.openbase.bco.bcomfy.R;
import org.openbase.bco.bcomfy.activityCore.serviceList.units.AbstractUnitViewHolder;
import org.openbase.bco.bcomfy.activityCore.serviceList.units.GenericUnitViewHolder;
import org.openbase.bco.bcomfy.activityCore.serviceList.units.UnitViewHolderFactory;
import org.openbase.bco.bcomfy.interfaces.OnTaskFinishedListener;
import org.openbase.bco.registry.device.lib.DeviceRegistry;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.unit.lib.UnitRegistry;
import org.openbase.jul.exception.CouldNotPerformException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import java8.util.stream.StreamSupport;
import rst.domotic.unit.UnitConfigType;
import rst.domotic.unit.UnitTemplateType;

public class UnitListViewHolder implements OnTaskFinishedListener<Void> {

    private static final String TAG = UnitListViewHolder.class.getSimpleName();

    private LinearLayout serviceList;
    private LinearLayout unitList;
    private TextView labelText;
    private TextView typeText;
    private ProgressBar deviceLoadingProgressBar;
    private Activity activity;
    private UnitConfigType.UnitConfig unitConfig;

    private List<AbstractUnitViewHolder> unitViewHolderList;

    private DeviceRegistry deviceRegistry;
    private UnitConfigType.UnitConfig deviceConfig;

    public UnitListViewHolder(LinearLayout serviceList) {
        this.serviceList = serviceList;
        unitList  = serviceList.findViewById(R.id.unit_list);
        labelText = serviceList.findViewById(R.id.device_label);
        typeText  = serviceList.findViewById(R.id.device_type);
        deviceLoadingProgressBar = serviceList.findViewById(R.id.device_loading_progress_bar);

        unitViewHolderList = new ArrayList<>();
    }

    public void displayUnit(Activity activity, UnitConfigType.UnitConfig unitConfig) {
        this.activity = activity;
        this.unitConfig = unitConfig;

        labelText.setText(R.string.loading_device);
        typeText.setText("");

        activity.runOnUiThread(() -> unitList.removeAllViews());
        unitViewHolderList.clear();

        deviceLoadingProgressBar.setVisibility(View.VISIBLE);

        new displayUnitTask().execute(this);
    }

    @Override
    public void taskFinishedCallback(Void returnObject) {
        deviceLoadingProgressBar.setVisibility(View.GONE);
    }

    private static class displayUnitTask extends AsyncTask<UnitListViewHolder, Void, Void> {
        private UnitListViewHolder unitListViewHolder;

        @Override
        protected Void doInBackground(UnitListViewHolder... unitListViewHolders) {
            unitListViewHolder = unitListViewHolders[0];

            try {
                unitListViewHolder.deviceRegistry = Registries.getDeviceRegistry();

                // Distinguish whether to display a device or a single unit
                if (unitListViewHolder.unitConfig.getUnitType() == UnitTemplateType.UnitTemplate.UnitType.DEVICE) {
                    // Display a device
                    unitListViewHolder.deviceConfig =
                            unitListViewHolder.deviceRegistry.getDeviceConfigById(unitListViewHolder.unitConfig.getId());

                    // Get label and device class
                    String labelText = unitListViewHolder.deviceConfig.getLabel();
                    String typeText = unitListViewHolder.deviceRegistry.getDeviceClassById(
                            unitListViewHolder.deviceConfig.getDeviceConfig().getDeviceClassId()).getLabel();
                    unitListViewHolder.activity.runOnUiThread(() -> {
                        unitListViewHolder.labelText.setText(labelText);
                        unitListViewHolder.typeText.setText(typeText);
                    });

                    // Create and add UnitViewHolders for every unit that is bound by the device
                    for (String unitId : unitListViewHolder.deviceConfig.getDeviceConfig().getUnitIdList()) {
                        UnitConfigType.UnitConfig unitConfig = Registries.getUnitRegistry().getUnitConfigById(unitId);
                        AbstractUnitViewHolder unitViewHolder = UnitViewHolderFactory.createUnitViewHolder(unitListViewHolder.activity, unitConfig, unitListViewHolder.unitList);
                        unitListViewHolder.unitViewHolderList.add(unitViewHolder);

                        unitListViewHolder.activity.runOnUiThread(() ->
                                unitListViewHolder.unitList.addView(unitViewHolder.getCardView()));
                    }
                }
                else {
                    // Display a single unit
                    AbstractUnitViewHolder unitViewHolder = UnitViewHolderFactory.createUnitViewHolder(unitListViewHolder.activity, unitListViewHolder.unitConfig, unitListViewHolder.unitList);
                    unitListViewHolder.unitViewHolderList.add(unitViewHolder);

                    unitListViewHolder.activity.runOnUiThread(() -> {
                        unitListViewHolder.labelText.setText(unitListViewHolder.unitConfig.getLabel());
                        unitListViewHolder.typeText.setText(unitListViewHolder.unitConfig.getDescription());
                        unitListViewHolder.unitList.addView(unitViewHolder.getCardView());
                    });
                }

            } catch (CouldNotPerformException | InterruptedException | ExecutionException | TimeoutException e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            unitListViewHolder.taskFinishedCallback(null);
        }
    }
}
