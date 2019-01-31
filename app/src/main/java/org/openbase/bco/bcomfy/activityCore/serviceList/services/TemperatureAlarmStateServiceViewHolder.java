package org.openbase.bco.bcomfy.activityCore.serviceList.services;

import android.app.Activity;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;

import org.openbase.bco.bcomfy.R;
import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.jul.exception.CouldNotPerformException;

import org.openbase.type.domotic.service.ServiceConfigType.ServiceConfig;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.AlarmStateType.AlarmState;

public class TemperatureAlarmStateServiceViewHolder extends AbstractServiceViewHolder {

    private static final String TAG = TemperatureAlarmStateServiceViewHolder.class.getSimpleName();

    private TextView textView;
    private ImageView imageView;

    private Animation pulse;

    TableRow.LayoutParams layoutHighWeight;
    TableRow.LayoutParams layoutLowWeight;

    public TemperatureAlarmStateServiceViewHolder(Activity activity, ViewGroup parent, UnitRemote unitRemote, ServiceConfig serviceConfig, boolean operation, boolean provider, boolean consumer) throws CouldNotPerformException, InterruptedException {
        super(activity, parent, unitRemote, serviceConfig, operation, provider, consumer);
    }

    @Override
    protected void initServiceView() {
        serviceView = (RelativeLayout) LayoutInflater.from(activity).inflate(R.layout.service_temperature_alarm_state, viewParent, false);
        textView = serviceView.findViewById(R.id.temperatureAlarmStateTextView);
        imageView = serviceView.findViewById(R.id.temperatureAlarmStateImageView);

        pulse = AnimationUtils.loadAnimation(serviceView.getContext(), R.anim.pulse);
        pulse.setFillAfter(true);

        imageView.setImageDrawable(new IconicsDrawable(serviceView.getContext(), GoogleMaterial.Icon.gmd_warning).color(Color.RED).sizeDp(24));

        layoutHighWeight = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 10f);
        layoutLowWeight  = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 7.5f);
    }

    @Override
    public void updateDynamicContent() {
        try {
            AlarmState alarmState = (AlarmState) Services.invokeProviderServiceMethod(ServiceType.TEMPERATURE_ALARM_STATE_SERVICE, unitRemote);

            activity.runOnUiThread(() -> {

                switch (alarmState.getValue()) {
                    case ALARM:
                        textView.setText("TEMPERATURE ALARM");
                        textView.setTextColor(Color.RED);
                        textView.setLayoutParams(layoutLowWeight);
                        imageView.setVisibility(View.VISIBLE);
                        imageView.startAnimation(pulse);
                        break;
                    case NO_ALARM:
                        textView.setText("No temperature alarm");
                        textView.setTextColor(Color.GRAY);
                        textView.setLayoutParams(layoutHighWeight);
                        imageView.setVisibility(View.GONE);
                        imageView.clearAnimation();
                        break;
                }
            });

        } catch (CouldNotPerformException | NullPointerException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }
}
