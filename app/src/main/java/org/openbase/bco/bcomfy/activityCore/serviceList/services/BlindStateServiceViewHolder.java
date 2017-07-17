package org.openbase.bco.bcomfy.activityCore.serviceList.services;

import android.app.Activity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import org.openbase.bco.bcomfy.R;
import org.openbase.bco.bcomfy.activityCore.DrawerDisablingOnTouchListener;
import org.openbase.bco.dal.lib.layer.service.Service$;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.jul.exception.CouldNotPerformException;

import rst.domotic.service.ServiceConfigType.ServiceConfig;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.ActivationStateType.ActivationState;
import rst.domotic.state.BlindStateType;
import rst.domotic.state.BlindStateType.BlindState;

public class BlindStateServiceViewHolder extends AbstractServiceViewHolder {

    private SeekBar blindStateSeekBar;
    private TextView blindStateTextView;
    private Button blindStateButtonDown;
    private Button blindStateButtonStop;
    private Button blindStateButtonUp;

    private Animation blink;

    private static final String TAG = BlindStateServiceViewHolder.class.getSimpleName();

    public BlindStateServiceViewHolder(Activity activity, ViewGroup viewParent, UnitRemote unitRemote, ServiceConfig serviceConfig, boolean operation, boolean provider, boolean consumer) throws CouldNotPerformException, InterruptedException {
        super(activity, viewParent, unitRemote, serviceConfig, operation, provider, consumer);
    }

    @Override
    protected void initServiceView() {
        serviceView = (RelativeLayout) LayoutInflater.from(activity).inflate(R.layout.service_blind_state, viewParent, false);
        blindStateSeekBar = serviceView.findViewById(R.id.blindStateSeekBar);
        blindStateTextView = serviceView.findViewById(R.id.blindStateTextView);
        blindStateButtonDown = serviceView.findViewById(R.id.blindStateButtonDown);
        blindStateButtonStop = serviceView.findViewById(R.id.blindStateButtonStop);
        blindStateButtonUp = serviceView.findViewById(R.id.blindStateButtonUp);

        blindStateSeekBar.setThumb(null);

        blink = AnimationUtils.loadAnimation(serviceView.getContext(), R.anim.blink);
        blink.setFillAfter(true);

        if (operation) {
            blindStateButtonDown.setOnClickListener(v -> {
                try {
                    Service$.invokeOperationServiceMethod(ServiceType.BLIND_STATE_SERVICE, unitRemote,
                            BlindState.newBuilder().setMovementState(BlindState.MovementState.DOWN).build());
                } catch (CouldNotPerformException e) {
                    Log.e(TAG, "Error while invoking BlindStateOperation on unit: " + serviceConfig.getUnitId());
                    e.printStackTrace();
                }
            });

            blindStateButtonStop.setOnClickListener(v -> {
                try {
                    Service$.invokeOperationServiceMethod(ServiceType.BLIND_STATE_SERVICE, unitRemote,
                            BlindState.newBuilder().setMovementState(BlindState.MovementState.STOP).build());
                } catch (CouldNotPerformException e) {
                    Log.e(TAG, "Error while invoking BlindStateOperation on unit: " + serviceConfig.getUnitId());
                    e.printStackTrace();
                }
            });

            blindStateButtonUp.setOnClickListener(v -> {
                try {
                    Service$.invokeOperationServiceMethod(ServiceType.BLIND_STATE_SERVICE, unitRemote,
                            BlindState.newBuilder().setMovementState(BlindState.MovementState.UP).build());
                } catch (CouldNotPerformException e) {
                    Log.e(TAG, "Error while invoking BlindStateOperation on unit: " + serviceConfig.getUnitId());
                    e.printStackTrace();
                }
            });
        } else {
            blindStateButtonDown.setEnabled(false);
            blindStateButtonStop.setEnabled(false);
            blindStateButtonUp.setEnabled(false);
        }
    }

    @Override
    public void updateDynamicContent() {
        try {
            BlindState blindState = (BlindState) Service$.invokeProviderServiceMethod(ServiceType.BLIND_STATE_SERVICE, unitRemote);

            activity.runOnUiThread(() -> {
                if (blindState.hasOpeningRatio()) {
                    blindStateSeekBar.setProgress((int) blindState.getOpeningRatio());
                    blindStateTextView.setText(((int) blindState.getOpeningRatio()) + "%");
                }

                switch (blindState.getMovementState()) {
                    case UP:
                        blindStateButtonUp.startAnimation(blink);
                        blindStateButtonStop.setEnabled(true);
                        blindStateButtonDown.clearAnimation();
                        break;
                    case STOP:
                        blindStateButtonUp.clearAnimation();
                        blindStateButtonStop.setEnabled(false);
                        blindStateButtonDown.clearAnimation();
                        break;
                    case DOWN:
                        blindStateButtonUp.clearAnimation();
                        blindStateButtonStop.setEnabled(true);
                        blindStateButtonDown.startAnimation(blink);
                        break;
                    default:
                        blindStateButtonUp.clearAnimation();
                        blindStateButtonStop.setEnabled(true);
                        blindStateButtonDown.clearAnimation();
                }
            });
        } catch (CouldNotPerformException e) {
            e.printStackTrace();
        }
    }
}