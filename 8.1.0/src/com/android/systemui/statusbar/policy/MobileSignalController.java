/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.systemui.statusbar.policy;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemProperties;
import android.provider.Settings.Global;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.cdma.EriInfo;
import com.android.systemui.R;
import com.android.systemui.statusbar.phone.SignalDrawable;
import com.android.systemui.statusbar.policy.NetworkController.IconState;
import com.android.systemui.statusbar.policy.NetworkController.SignalCallback;
import com.android.systemui.statusbar.policy.NetworkControllerImpl.Config;
import com.android.systemui.statusbar.policy.NetworkControllerImpl.SubscriptionDefaults;

import java.io.PrintWriter;
import java.util.BitSet;
import java.util.Objects;


public class MobileSignalController extends SignalController<
        MobileSignalController.MobileState, MobileSignalController.MobileIconGroup> {
    private final TelephonyManager mPhone;
    private final SubscriptionDefaults mDefaults;
    private final String mNetworkNameDefault;
    private final String mNetworkNameSeparator;
    private final ContentObserver mObserver;
    @VisibleForTesting
    final PhoneStateListener mPhoneStateListener;
    // Save entire info for logging, we only use the id.
    final SubscriptionInfo mSubscriptionInfo;

    // @VisibleForDemoMode
    final SparseArray<MobileIconGroup> mNetworkToIconLookup;

    // Since some pieces of the phone state are interdependent we store it locally,
    // this could potentially become part of MobileState for simplification/complication
    // of code.
    private int mDataNetType = TelephonyManager.NETWORK_TYPE_UNKNOWN;
    private int mDataState = TelephonyManager.DATA_DISCONNECTED;
    private ServiceState mServiceState;
    private SignalStrength mSignalStrength;
    private MobileIconGroup mDefaultIcons;
    private Config mConfig;

    private final int STATUS_BAR_STYLE_ANDROID_DEFAULT = 0;
    private final int STATUS_BAR_STYLE_CDMA_1X_COMBINED = 1;
    private final int STATUS_BAR_STYLE_DEFAULT_DATA = 2;
    private final int STATUS_BAR_STYLE_DATA_VOICE = 3;
    private int mStyle = STATUS_BAR_STYLE_ANDROID_DEFAULT;
    private boolean mDualBar = false;

    // TODO: Reduce number of vars passed in, if we have the NetworkController, probably don't
    // need listener lists anymore.
    public MobileSignalController(Context context, Config config, boolean hasMobileData,
            TelephonyManager phone, CallbackHandler callbackHandler,
            NetworkControllerImpl networkController, SubscriptionInfo info,
            SubscriptionDefaults defaults, Looper receiverLooper) {
        super("MobileSignalController(" + info.getSubscriptionId() + ")", context,
                NetworkCapabilities.TRANSPORT_CELLULAR, callbackHandler,
                networkController);
        mNetworkToIconLookup = new SparseArray<>();
        mConfig = config;
        mPhone = phone;
        mDefaults = defaults;
        mSubscriptionInfo = info;
        mPhoneStateListener = new MobilePhoneStateListener(info.getSubscriptionId(),
                receiverLooper);
        mNetworkNameSeparator = getStringIfExists(R.string.status_bar_network_name_separator);
        mNetworkNameDefault = getStringIfExists(
                com.android.internal.R.string.lockscreen_carrier_default);

        if (config.readIconsFromXml) {
            TelephonyIcons.readIconsFromXml(context);
            mDefaultIcons = !mConfig.showAtLeast3G ? TelephonyIcons.G : TelephonyIcons.THREE_G;
        } else {
            mapIconSets();
        }


        mStyle = context.getResources().getInteger(R.integer.status_bar_style);

        String networkName = info.getCarrierName() != null ? info.getCarrierName().toString()
                : mNetworkNameDefault;
        mLastState.networkName = mCurrentState.networkName = networkName;
        mLastState.networkNameData = mCurrentState.networkNameData = networkName;
        mLastState.enabled = mCurrentState.enabled = hasMobileData;
        mLastState.iconGroup = mCurrentState.iconGroup = mDefaultIcons;
        // Get initial data sim state.
        updateDataSim();
        mObserver = new ContentObserver(new Handler(receiverLooper)) {
            @Override
            public void onChange(boolean selfChange) {
                updateTelephony();
            }
        };
    }

    public void setConfiguration(Config config) {
        mConfig = config;
        if (!config.readIconsFromXml) {
            mapIconSets();
        }
        updateTelephony();
    }

    public int getDataContentDescription() {
        return getIcons().mDataContentDescription;
    }

    public void setAirplaneMode(boolean airplaneMode) {
        mCurrentState.airplaneMode = airplaneMode;
        notifyListenersIfNecessary();
    }

    public void setUserSetupComplete(boolean userSetup) {
        mCurrentState.userSetup = userSetup;
        notifyListenersIfNecessary();
    }

    @Override
    public void updateConnectivity(BitSet connectedTransports, BitSet validatedTransports) {
        boolean isValidated = validatedTransports.get(mTransportType);
        mCurrentState.isDefault = connectedTransports.get(mTransportType);
        // Only show this as not having connectivity if we are default.
        mCurrentState.inetCondition = (isValidated || !mCurrentState.isDefault) ? 1 : 0;
        notifyListenersIfNecessary();
    }

    public void setCarrierNetworkChangeMode(boolean carrierNetworkChangeMode) {
        mCurrentState.carrierNetworkChangeMode = carrierNetworkChangeMode;
        updateTelephony();
    }

    /**
     * Start listening for phone state changes.
     */
    public void registerListener() {
        mPhone.listen(mPhoneStateListener,
                PhoneStateListener.LISTEN_SERVICE_STATE
                        | PhoneStateListener.LISTEN_SIGNAL_STRENGTHS
                        | PhoneStateListener.LISTEN_CALL_STATE
                        | PhoneStateListener.LISTEN_DATA_CONNECTION_STATE
                        | PhoneStateListener.LISTEN_DATA_ACTIVITY
                        | PhoneStateListener.LISTEN_CARRIER_NETWORK_CHANGE);
        mContext.getContentResolver().registerContentObserver(Global.getUriFor(Global.MOBILE_DATA),
                true, mObserver);
        mContext.getContentResolver().registerContentObserver(Global.getUriFor(
                Global.MOBILE_DATA + mSubscriptionInfo.getSubscriptionId()),
                true, mObserver);
    }

    /**
     * Stop listening for phone state changes.
     */
    public void unregisterListener() {
        mPhone.listen(mPhoneStateListener, 0);
        mContext.getContentResolver().unregisterContentObserver(mObserver);
    }

    /**
     * Produce a mapping of data network types to icon groups for simple and quick use in
     * updateTelephony.
     */
    private void mapIconSets() {
        mNetworkToIconLookup.clear();

        mNetworkToIconLookup.put(TelephonyManager.NETWORK_TYPE_EVDO_0, TelephonyIcons.THREE_G);
        mNetworkToIconLookup.put(TelephonyManager.NETWORK_TYPE_EVDO_A, TelephonyIcons.THREE_G);
        mNetworkToIconLookup.put(TelephonyManager.NETWORK_TYPE_EVDO_B, TelephonyIcons.THREE_G);
        mNetworkToIconLookup.put(TelephonyManager.NETWORK_TYPE_EHRPD, TelephonyIcons.THREE_G);
        mNetworkToIconLookup.put(TelephonyManager.NETWORK_TYPE_UMTS, TelephonyIcons.THREE_G);
        mNetworkToIconLookup.put(TelephonyManager.NETWORK_TYPE_TD_SCDMA, TelephonyIcons.THREE_G);

        if (!mConfig.showAtLeast3G) {
            mNetworkToIconLookup.put(TelephonyManager.NETWORK_TYPE_UNKNOWN,
                    TelephonyIcons.UNKNOWN);
            mNetworkToIconLookup.put(TelephonyManager.NETWORK_TYPE_EDGE, TelephonyIcons.E);
            mNetworkToIconLookup.put(TelephonyManager.NETWORK_TYPE_CDMA, TelephonyIcons.ONE_X);
            mNetworkToIconLookup.put(TelephonyManager.NETWORK_TYPE_1xRTT, TelephonyIcons.ONE_X);

            mDefaultIcons = TelephonyIcons.G;
        } else {
            mNetworkToIconLookup.put(TelephonyManager.NETWORK_TYPE_UNKNOWN,
                    TelephonyIcons.THREE_G);
            mNetworkToIconLookup.put(TelephonyManager.NETWORK_TYPE_EDGE,
                    TelephonyIcons.THREE_G);
            mNetworkToIconLookup.put(TelephonyManager.NETWORK_TYPE_CDMA,
                    TelephonyIcons.THREE_G);
            mNetworkToIconLookup.put(TelephonyManager.NETWORK_TYPE_1xRTT,
                    TelephonyIcons.THREE_G);
            mDefaultIcons = TelephonyIcons.THREE_G;
        }

        MobileIconGroup hGroup = TelephonyIcons.THREE_G;
        if (mConfig.hspaDataDistinguishable) {
            hGroup = TelephonyIcons.H;
        }
        mNetworkToIconLookup.put(TelephonyManager.NETWORK_TYPE_HSDPA, hGroup);
        mNetworkToIconLookup.put(TelephonyManager.NETWORK_TYPE_HSUPA, hGroup);
        mNetworkToIconLookup.put(TelephonyManager.NETWORK_TYPE_HSPA, hGroup);
        mNetworkToIconLookup.put(TelephonyManager.NETWORK_TYPE_HSPAP, hGroup);

        if (mConfig.show4gForLte) {
            mNetworkToIconLookup.put(TelephonyManager.NETWORK_TYPE_LTE, TelephonyIcons.FOUR_G);
            if (mConfig.hideLtePlus) {
                mNetworkToIconLookup.put(TelephonyManager.NETWORK_TYPE_LTE_CA,
                        TelephonyIcons.FOUR_G);
            } else {
                mNetworkToIconLookup.put(TelephonyManager.NETWORK_TYPE_LTE_CA,
                        TelephonyIcons.FOUR_G_PLUS);
            }
        } else {
            mNetworkToIconLookup.put(TelephonyManager.NETWORK_TYPE_LTE, TelephonyIcons.LTE);
            if (mConfig.hideLtePlus) {
                mNetworkToIconLookup.put(TelephonyManager.NETWORK_TYPE_LTE_CA,
                        TelephonyIcons.LTE);
            } else {
                mNetworkToIconLookup.put(TelephonyManager.NETWORK_TYPE_LTE_CA,
                        TelephonyIcons.LTE_PLUS);
            }
        }
        mNetworkToIconLookup.put(TelephonyManager.NETWORK_TYPE_IWLAN, TelephonyIcons.WFC);
    }

    private int getNumLevels() {
        if (mConfig.inflateSignalStrengths) {
            return SignalStrength.NUM_SIGNAL_STRENGTH_BINS + 1;
        }
        return SignalStrength.NUM_SIGNAL_STRENGTH_BINS;
    }

    @Override
    public int getCurrentIconId() {
        if (mCurrentState.iconGroup == TelephonyIcons.CARRIER_NETWORK_CHANGE) {
            return SignalDrawable.getCarrierChangeState(getNumLevels());
        } else if (mCurrentState.connected) {
            int level = mCurrentState.level;
            if (mConfig.inflateSignalStrengths) {
                level++;
            }
            if (mConfig.readIconsFromXml) {
                return getIcons().mSingleSignalIcon;
            } else {
                return SignalDrawable.getState(level, getNumLevels(),
                    mCurrentState.inetCondition == 0);
            }
        } else if (mCurrentState.enabled) {
            if (mConfig.readIconsFromXml) {
                return getIcons().mSbDiscState;
            } else {
                return SignalDrawable.getEmptyState(getNumLevels());
            }
        } else {
            return 0;
        }
    }

    @Override
    public int getQsCurrentIconId() {
        if (mCurrentState.airplaneMode) {
            return SignalDrawable.getAirplaneModeState(getNumLevels());
        } else if (mCurrentState.iconGroup == TelephonyIcons.CARRIER_NETWORK_CHANGE) {
            return SignalDrawable.getCarrierChangeState(getNumLevels());
        } else if (mCurrentState.connected) {
            return SignalDrawable.getState(mCurrentState.level, getNumLevels(),
                    mCurrentState.inetCondition == 0);
        } else if (mCurrentState.enabled) {
            return SignalDrawable.getEmptyState(getNumLevels());
        } else {
            return 0;
        }
    }

    @Override
    public void notifyListeners(SignalCallback callback) {
        if (mConfig.readIconsFromXml) {
            generateIconGroup();
        }
        MobileIconGroup icons = getIcons();

        String contentDescription = getStringIfExists(getContentDescription());
        String dataContentDescription = getStringIfExists(icons.mDataContentDescription);
        final boolean dataDisabled = mCurrentState.iconGroup == TelephonyIcons.DATA_DISABLED
                && mCurrentState.userSetup;

        // Show icon in QS when we are connected or data is disabled.
        boolean showDataIcon = mCurrentState.dataConnected || dataDisabled;
        IconState statusIcon = new IconState(mCurrentState.enabled && !mCurrentState.airplaneMode,
                getCurrentIconId(), contentDescription);

        int qsTypeIcon = 0;
        IconState qsIcon = null;
        String description = null;
        // Only send data sim callbacks to QS.
        if (mCurrentState.dataSim) {
            qsTypeIcon = showDataIcon ? icons.mQsDataType : 0;
            qsIcon = new IconState(mCurrentState.enabled
                    && !mCurrentState.isEmergency, getQsCurrentIconId(), contentDescription);
            description = mCurrentState.isEmergency ? null : mCurrentState.networkName;
        }
        boolean activityIn = mCurrentState.dataConnected
                && !mCurrentState.carrierNetworkChangeMode
                && mCurrentState.activityIn;
        boolean activityOut = mCurrentState.dataConnected
                && !mCurrentState.carrierNetworkChangeMode
                && mCurrentState.activityOut;
        showDataIcon &= mCurrentState.isDefault || dataDisabled;
        if (SystemProperties.getBoolean("persist.vendor.radio.L_L_4G", false)
                && (mDataNetType == TelephonyManager.NETWORK_TYPE_LTE_CA
                       || mDataNetType == TelephonyManager.NETWORK_TYPE_LTE)) showDataIcon = true;
        showDataIcon &= mStyle == STATUS_BAR_STYLE_ANDROID_DEFAULT;
        int typeIcon = showDataIcon ? icons.mDataType : 0;

        int dataActivityId = showMobileActivity() ? 0 : icons.mActivityId;
        callback.setMobileDataIndicators(statusIcon, qsIcon, typeIcon, qsTypeIcon,
                activityIn, activityOut, dataActivityId,
                icons.mStackedDataIcon, icons.mStackedVoiceIcon,
                dataContentDescription, description, icons.mIsWide,
                mSubscriptionInfo.getSubscriptionId(), mCurrentState.roaming);
    }

    @Override
    protected MobileState cleanState() {
        return new MobileState();
    }

    private boolean hasService() {
        if (mServiceState != null) {
            // Consider the device to be in service if either voice or data
            // service is available. Some SIM cards are marketed as data-only
            // and do not support voice service, and on these SIM cards, we
            // want to show signal bars for data service as well as the "no
            // service" or "emergency calls only" text that indicates that voice
            // is not available.
            switch (mServiceState.getVoiceRegState()) {
                case ServiceState.STATE_POWER_OFF:
                    return false;
                case ServiceState.STATE_OUT_OF_SERVICE:
                case ServiceState.STATE_EMERGENCY_ONLY:
                    return mServiceState.getDataRegState() == ServiceState.STATE_IN_SERVICE;
                default:
                    return true;
            }
        } else {
            return false;
        }
    }

    private int getDataRegState() {
        if (mServiceState == null) {
            if (DEBUG) {
                Log.d(mTag, "getDataRegState dataRegState:STATE_OUT_OF_SERVICE");
            }
            return ServiceState.STATE_OUT_OF_SERVICE;
        }
        return mServiceState.getDataRegState();
    }

    private int getVoiceRegState() {
        if (mServiceState == null) {
            if (DEBUG) {
                Log.d(mTag, "getVoiceRegState voiceRegState:STATE_OUT_OF_SERVICE");
            }
            return ServiceState.STATE_OUT_OF_SERVICE;
        }
        return mServiceState.getVoiceRegState();
    }

    private boolean isCdma() {
        return (mSignalStrength != null) && !mSignalStrength.isGsm();
    }

    public boolean isEmergencyOnly() {
        return (mServiceState != null && mServiceState.isEmergencyOnly());
    }

    private boolean isRoaming() {
        // During a carrier change, roaming indications need to be supressed.
        if (isCarrierNetworkChangeActive()) {
            return false;
        }
        if (isCdma() && mServiceState != null) {
            final int iconMode = mServiceState.getCdmaEriIconMode();
            return mServiceState.getCdmaEriIconIndex() != EriInfo.ROAMING_INDICATOR_OFF
                    && (iconMode == EriInfo.ROAMING_ICON_MODE_NORMAL
                    || iconMode == EriInfo.ROAMING_ICON_MODE_FLASH);
        } else {
            return mServiceState != null && mServiceState.getRoaming();
        }
    }

    private boolean isCarrierNetworkChangeActive() {
        return mCurrentState.carrierNetworkChangeMode;
    }

    public void handleBroadcast(Intent intent) {
        String action = intent.getAction();
        if (action.equals(TelephonyIntents.SPN_STRINGS_UPDATED_ACTION)) {
            updateNetworkName(intent.getBooleanExtra(TelephonyIntents.EXTRA_SHOW_SPN, false),
                    intent.getStringExtra(TelephonyIntents.EXTRA_SPN),
                    intent.getStringExtra(TelephonyIntents.EXTRA_DATA_SPN),
                    intent.getBooleanExtra(TelephonyIntents.EXTRA_SHOW_PLMN, false),
                    intent.getStringExtra(TelephonyIntents.EXTRA_PLMN));
            notifyListenersIfNecessary();
        } else if (action.equals(TelephonyIntents.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED)) {
            updateDataSim();
            notifyListenersIfNecessary();
        }
    }

    private void updateDataSim() {
        int defaultDataSub = mDefaults.getDefaultDataSubId();
        if (SubscriptionManager.isValidSubscriptionId(defaultDataSub)) {
            mCurrentState.dataSim = defaultDataSub == mSubscriptionInfo.getSubscriptionId();
        } else {
            // There doesn't seem to be a data sim selected, however if
            // there isn't a MobileSignalController with dataSim set, then
            // QS won't get any callbacks and will be blank.  Instead
            // lets just assume we are the data sim (which will basically
            // show one at random) in QS until one is selected.  The user
            // should pick one soon after, so we shouldn't be in this state
            // for long.
            mCurrentState.dataSim = true;
        }
    }

    /**
     * Updates the network's name based on incoming spn and plmn.
     */
    void updateNetworkName(boolean showSpn, String spn, String dataSpn,
            boolean showPlmn, String plmn) {
        if (CHATTY) {
            Log.d("CarrierLabel", "updateNetworkName showSpn=" + showSpn
                    + " spn=" + spn + " dataSpn=" + dataSpn
                    + " showPlmn=" + showPlmn + " plmn=" + plmn);
        }
        StringBuilder str = new StringBuilder();
        StringBuilder strData = new StringBuilder();
        if (showPlmn && plmn != null) {
            str.append(plmn);
            strData.append(plmn);
        }
        if (showSpn && spn != null) {
            if (str.length() != 0) {
                str.append(mNetworkNameSeparator);
            }
            str.append(spn);
        }
        if (str.length() != 0) {
            mCurrentState.networkName = str.toString();
        } else {
            mCurrentState.networkName = mNetworkNameDefault;
        }
        if (showSpn && dataSpn != null) {
            if (strData.length() != 0) {
                strData.append(mNetworkNameSeparator);
            }
            strData.append(dataSpn);
        }
        if (strData.length() != 0) {
            mCurrentState.networkNameData = strData.toString();
        } else {
            mCurrentState.networkNameData = mNetworkNameDefault;
        }
    }

    /**
     * Updates the current state based on mServiceState, mSignalStrength, mDataNetType,
     * mDataState, and mSimState.  It should be called any time one of these is updated.
     * This will call listeners if necessary.
     */
    private final void updateTelephony() {
        if (DEBUG) {
            Log.d(mTag, "updateTelephony: hasService=" + hasService()
                    + " ss=" + mSignalStrength);
        }
        mCurrentState.connected = hasService() && mSignalStrength != null;
        if (mCurrentState.connected) {
            if (!mSignalStrength.isGsm() && mConfig.alwaysShowCdmaRssi) {
                mCurrentState.level = mSignalStrength.getCdmaLevel();
            } else {
                mCurrentState.level = mSignalStrength.getLevel();
                if (mConfig.showRsrpSignalLevelforLTE) {
                    int dataType = mServiceState.getDataNetworkType();
                    if (dataType == TelephonyManager.NETWORK_TYPE_LTE ||
                            dataType == TelephonyManager.NETWORK_TYPE_LTE_CA) {
                        mCurrentState.level = getAlternateLteLevel(mSignalStrength);
                    }
                }
            }
        }
        if (mNetworkToIconLookup.indexOfKey(mDataNetType) >= 0) {
            mCurrentState.iconGroup = mNetworkToIconLookup.get(mDataNetType);
        } else {
            mCurrentState.iconGroup = mDefaultIcons;
        }
        mCurrentState.dataConnected = mCurrentState.connected
                && mDataState == TelephonyManager.DATA_CONNECTED;

        mCurrentState.roaming = isRoaming();
        if (isCarrierNetworkChangeActive()) {
            mCurrentState.iconGroup = TelephonyIcons.CARRIER_NETWORK_CHANGE;
        } else if (isDataDisabled()) {
            mCurrentState.iconGroup = TelephonyIcons.DATA_DISABLED;
        }
        if (isEmergencyOnly() != mCurrentState.isEmergency) {
            mCurrentState.isEmergency = isEmergencyOnly();
            mNetworkController.recalculateEmergency();
        }
        // Fill in the network name if we think we have it.
        if (mCurrentState.networkName == mNetworkNameDefault && mServiceState != null
                && !TextUtils.isEmpty(mServiceState.getOperatorAlphaShort())) {
            mCurrentState.networkName = mServiceState.getOperatorAlphaShort();
        }

        if (mConfig.readIconsFromXml) {
            mCurrentState.voiceLevel = getVoiceSignalLevel();
            mCurrentState.voiceNetType = getVoiceNetworkType();
            mCurrentState.voiceRegState = getVoiceRegState();
        }

        mCurrentState.dataNetType = getDataNetworkType();
        mCurrentState.dataRegState = getDataRegState();

        notifyListenersIfNecessary();
    }

    private boolean isDataDisabled() {
        return !mPhone.getDataEnabled(mSubscriptionInfo.getSubscriptionId());
    }

    private void generateIconGroup() {
        final int level = mCurrentState.level;
        final int voiceLevel = mCurrentState.voiceLevel;
        final int inet = mCurrentState.inetCondition;
        final boolean dataConnected = mCurrentState.dataConnected;
        final boolean roaming = isRoaming();
        final int voiceType = getVoiceNetworkType();
        int dataType =  getDataNetworkType();

        if (dataType == TelephonyManager.NETWORK_TYPE_LTE && mServiceState != null &&
                    mServiceState.isUsingCarrierAggregation()) {
              dataType = TelephonyManager.NETWORK_TYPE_LTE_CA;
        }


        int[] contentDesc = AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH;
        int discContentDesc = AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH[0];
        int dataContentDesc, dataTypeIcon, qsDataTypeIcon, dataActivityId;
        int singleSignalIcon, stackedDataIcon = 0, stackedVoiceIcon = 0;

        final int slotId = getSimSlotIndex();
        if (slotId < 0 || slotId > mPhone.getPhoneCount()) {
            Log.e(mTag, "generateIconGroup invalid slotId:" + slotId);
            return;
        }

        if (DEBUG) Log.d(mTag, "generateIconGroup slot:" + slotId + " style:" + mStyle
                + " connected:" + mCurrentState.connected + " inetCondition:" + inet
                + " roaming:" + roaming + " level:" + level + " voiceLevel:" + voiceLevel
                + " dataConnected:" + dataConnected
                + " dataActivity:" + mCurrentState.dataActivity
                + " CS:" + voiceType
                + "/" + TelephonyManager.getNetworkTypeName(voiceType)
                + ", PS:" + dataType
                + "/" + TelephonyManager.getNetworkTypeName(dataType));

        // Update data icon set
        int chosenNetworkType = ((dataType == TelephonyManager.NETWORK_TYPE_UNKNOWN)
                ? voiceType : dataType);
        TelephonyIcons.updateDataType(slotId, chosenNetworkType, mConfig.showAtLeast3G,
                mConfig.show4gForLte, mConfig.hspaDataDistinguishable, inet);

        // Update signal strength icons
        singleSignalIcon = TelephonyIcons.getSignalStrengthIcon(slotId, inet, level, roaming);
        if (DEBUG) {
            Log.d(mTag, "singleSignalIcon:" + getResourceName(singleSignalIcon));
        }

        dataActivityId = (mCurrentState.dataConnected && slotId >= 0) ?
                TelephonyIcons.getDataActivity(slotId, mCurrentState.dataActivity) : 0;

        // Convert the icon to unstacked if necessary.
        int unstackedSignalIcon = TelephonyIcons.convertMobileStrengthIcon(singleSignalIcon);
        if (DEBUG) {
            Log.d(mTag, "unstackedSignalIcon:" + getResourceName(unstackedSignalIcon));
        }
        if (singleSignalIcon != unstackedSignalIcon) {
            stackedDataIcon = singleSignalIcon;
            singleSignalIcon = unstackedSignalIcon;
        }

        int[] subId = SubscriptionManager.getSubId(getSimSlotIndex());
        if (subId != null && subId.length >= 1) {
            mDualBar = SubscriptionManager.getResourcesForSubId(mContext,
                        subId[0]).getBoolean(com.android.internal.R.bool.config_dual_bar);
        }

        if (DEBUG) {
            Log.d(mTag, "mDualBar:" + mDualBar);
            Log.d(mTag, "mStyle:" + mStyle);
        }

        if (mStyle == STATUS_BAR_STYLE_CDMA_1X_COMBINED
                || (mStyle == STATUS_BAR_STYLE_DATA_VOICE && mDualBar)) {
            if (!roaming && showDataAndVoice()) {
                stackedVoiceIcon = TelephonyIcons.getStackedVoiceIcon(voiceLevel);
            } else if (roaming && dataActivityId != 0) {
                // Remove data type indicator if already shown in data activity icon.
                singleSignalIcon = TelephonyIcons.getRoamingSignalIconId(level, inet);
            }
        }

        // Clear satcked data icon if no satcked voice icon.
        if (stackedVoiceIcon == 0) stackedDataIcon = 0;

        contentDesc = TelephonyIcons.getSignalStrengthDes(slotId);
        int sbDiscState = TelephonyIcons.getSignalNullIcon(slotId);
        if (DEBUG) {
            Log.d(mTag, "singleSignalIcon=" + getResourceName(singleSignalIcon)
                    + " dataActivityId=" + getResourceName(dataActivityId)
                    + " stackedDataIcon=" + getResourceName(stackedDataIcon)
                    + " stackedVoiceIcon=" + getResourceName(stackedVoiceIcon));
        }

        // Update data net type icons
        if (dataType == TelephonyManager.NETWORK_TYPE_IWLAN) {
            // wimax is a special 4g network not handled by telephony
            dataTypeIcon = TelephonyIcons.ICON_4G;
            qsDataTypeIcon = TelephonyIcons.ICON_4G;
            dataContentDesc = R.string.accessibility_data_connection_4g;
        } else {
            dataTypeIcon = TelephonyIcons.getDataTypeIcon(slotId);
            dataContentDesc = TelephonyIcons.getDataTypeDesc(slotId);
            qsDataTypeIcon = TelephonyIcons.getQSDataTypeIcon(slotId);
        }

        if (DEBUG) {
            Log.d(mTag, "updateDataNetType, dataTypeIcon=" + getResourceName(dataTypeIcon)
                    + " qsDataTypeIcon=" + getResourceName(qsDataTypeIcon)
                    + " dataContentDesc=" + dataContentDesc);
        }
        mCurrentState.iconGroup = new MobileIconGroup(
                TelephonyManager.getNetworkTypeName(dataType),
                null, null, contentDesc, 0, 0, sbDiscState, 0, discContentDesc,
                dataContentDesc, dataTypeIcon, false, qsDataTypeIcon,
                singleSignalIcon, stackedDataIcon, stackedVoiceIcon, dataActivityId);
    }

    private int getSimSlotIndex() {
        int slotId = -1;
        if (mSubscriptionInfo != null) {
            slotId = mSubscriptionInfo.getSimSlotIndex();
        }
        if (DEBUG) Log.d(mTag, "getSimSlotIndex, slotId: " + slotId);
        return slotId;
    }

    private boolean showMobileActivity() {
        return (mStyle == STATUS_BAR_STYLE_DEFAULT_DATA)
                || (mStyle == STATUS_BAR_STYLE_ANDROID_DEFAULT);
    }

    private int getVoiceNetworkType() {
        if (mServiceState == null) {
            return TelephonyManager.NETWORK_TYPE_UNKNOWN;
        }
        return mServiceState.getVoiceNetworkType();
    }

    private int getDataNetworkType() {
        if (mServiceState == null) {
            return TelephonyManager.NETWORK_TYPE_UNKNOWN;
        }
        return mServiceState.getDataNetworkType();
    }

    private int getVoiceSignalLevel() {
        if (mSignalStrength == null) {
            return SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN;
        }
        boolean isCdma = TelephonyManager.PHONE_TYPE_CDMA == TelephonyManager.getDefault()
                .getCurrentPhoneType(mSubscriptionInfo.getSubscriptionId());
        return isCdma ? mSignalStrength.getCdmaLevel() : mSignalStrength.getGsmLevel();
    }

    private boolean showDataAndVoice() {
        if (!(mStyle == STATUS_BAR_STYLE_CDMA_1X_COMBINED
                || (mStyle == STATUS_BAR_STYLE_DATA_VOICE && mDualBar))) {
            return false;
        }

        int dataType = getDataNetworkType();
        int voiceType = getVoiceNetworkType();

        if ((dataType == TelephonyManager.NETWORK_TYPE_EVDO_0
                || dataType == TelephonyManager.NETWORK_TYPE_EVDO_0
                || dataType == TelephonyManager.NETWORK_TYPE_EVDO_A
                || dataType == TelephonyManager.NETWORK_TYPE_EVDO_B
                || dataType == TelephonyManager.NETWORK_TYPE_EHRPD
                || dataType == TelephonyManager.NETWORK_TYPE_LTE
                || dataType == TelephonyManager.NETWORK_TYPE_LTE_CA)
                && (voiceType == TelephonyManager.NETWORK_TYPE_GSM
                    || voiceType == TelephonyManager.NETWORK_TYPE_1xRTT
                    || voiceType == TelephonyManager.NETWORK_TYPE_CDMA)) {
            return true;
        }
        return false;
    }

    private boolean show1xOnly() {
        int dataType = getDataNetworkType();
        if (dataType == TelephonyManager.NETWORK_TYPE_1xRTT
                || dataType == TelephonyManager.NETWORK_TYPE_CDMA) {
            return true;
        }
        return false;
    }

    private int getAlternateLteLevel(SignalStrength signalStrength) {
        int lteRsrp = signalStrength.getLteDbm();
        int rsrpLevel = SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN;
        if (lteRsrp > -44) rsrpLevel = SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN;
        else if (lteRsrp >= -97) rsrpLevel = SignalStrength.SIGNAL_STRENGTH_GREAT;
        else if (lteRsrp >= -105) rsrpLevel = SignalStrength.SIGNAL_STRENGTH_GOOD;
        else if (lteRsrp >= -113) rsrpLevel = SignalStrength.SIGNAL_STRENGTH_MODERATE;
        else if (lteRsrp >= -120) rsrpLevel = SignalStrength.SIGNAL_STRENGTH_POOR;
        else if (lteRsrp >= -140) rsrpLevel = SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN;
        if (DEBUG) {
            Log.d(mTag, "getAlternateLteLevel lteRsrp:" + lteRsrp + " rsrpLevel = " + rsrpLevel);
        }
        return rsrpLevel;
    }

    private String getResourceName(int resId) {
        if (resId != 0) {
            final Resources res = mContext.getResources();
            try {
                return res.getResourceName(resId);
            } catch (android.content.res.Resources.NotFoundException ex) {
                return "(unknown)";
            }
        } else {
            return "(null)";
        }
    }

    @VisibleForTesting
    void setActivity(int activity) {
        mCurrentState.activityIn = activity == TelephonyManager.DATA_ACTIVITY_INOUT
                || activity == TelephonyManager.DATA_ACTIVITY_IN;
        mCurrentState.activityOut = activity == TelephonyManager.DATA_ACTIVITY_INOUT
                || activity == TelephonyManager.DATA_ACTIVITY_OUT;
        if (mConfig.readIconsFromXml) {
            mCurrentState.dataActivity = activity;
        }
        notifyListenersIfNecessary();
    }

    @Override
    public void dump(PrintWriter pw) {
        super.dump(pw);
        pw.println("  mSubscription=" + mSubscriptionInfo + ",");
        pw.println("  mServiceState=" + mServiceState + ",");
        pw.println("  mSignalStrength=" + mSignalStrength + ",");
        pw.println("  mDataState=" + mDataState + ",");
        pw.println("  mDataNetType=" + mDataNetType + ",");
    }

    class MobilePhoneStateListener extends PhoneStateListener {
        public MobilePhoneStateListener(int subId, Looper looper) {
            super(subId, looper);
        }

        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            if (DEBUG) {
                Log.d(mTag, "onSignalStrengthsChanged signalStrength=" + signalStrength +
                        ((signalStrength == null) ? "" : (" level=" + signalStrength.getLevel())));
            }
            mSignalStrength = signalStrength;
            updateTelephony();
        }

        @Override
        public void onServiceStateChanged(ServiceState state) {
            if (DEBUG) {
                Log.d(mTag, "onServiceStateChanged voiceState=" + state.getVoiceRegState()
                        + " dataState=" + state.getDataRegState());
            }
            mServiceState = state;
            if (state != null) {
                mDataNetType = state.getDataNetworkType();
                if (mDataNetType == TelephonyManager.NETWORK_TYPE_LTE && mServiceState != null &&
                        mServiceState.isUsingCarrierAggregation()) {
                    mDataNetType = TelephonyManager.NETWORK_TYPE_LTE_CA;
                }
            }
            updateTelephony();
        }

        @Override
        public void onDataConnectionStateChanged(int state, int networkType) {
            if (DEBUG) {
                Log.d(mTag, "onDataConnectionStateChanged: state=" + state
                        + " type=" + networkType);
            }
            mDataState = state;
            mDataNetType = networkType;
            if (mDataNetType == TelephonyManager.NETWORK_TYPE_LTE && mServiceState != null &&
                    mServiceState.isUsingCarrierAggregation()) {
                mDataNetType = TelephonyManager.NETWORK_TYPE_LTE_CA;
            }
            updateTelephony();
        }

        @Override
        public void onDataActivity(int direction) {
            if (DEBUG) {
                Log.d(mTag, "onDataActivity: direction=" + direction);
            }
            setActivity(direction);
        }

        @Override
        public void onCarrierNetworkChange(boolean active) {
            if (DEBUG) {
                Log.d(mTag, "onCarrierNetworkChange: active=" + active);
            }
            mCurrentState.carrierNetworkChangeMode = active;

            updateTelephony();
        }
    };

    static class MobileIconGroup extends SignalController.IconGroup {
        final int mDataContentDescription; // mContentDescriptionDataType
        final int mDataType;
        final boolean mIsWide;
        final int mQsDataType;
        final int mSingleSignalIcon;
        final int mStackedDataIcon;
        final int mStackedVoiceIcon;
        final int mActivityId;

        public MobileIconGroup(String name, int[][] sbIcons, int[][] qsIcons, int[] contentDesc,
                int sbNullState, int qsNullState, int sbDiscState, int qsDiscState,
                int discContentDesc, int dataContentDesc, int dataType, boolean isWide,
                int qsDataType) {
                this(name, sbIcons, qsIcons, contentDesc, sbNullState, qsNullState, sbDiscState,
                        qsDiscState, discContentDesc, dataContentDesc, dataType, isWide,
                        qsDataType, 0, 0, 0, 0);
        }

        public MobileIconGroup(String name, int[][] sbIcons, int[][] qsIcons, int[] contentDesc,
                int sbNullState, int qsNullState, int sbDiscState, int qsDiscState,
                int discContentDesc, int dataContentDesc, int dataType, boolean isWide,
                int qsDataType, int singleSignalIcon, int stackedDataIcon,
                int stackedVoicelIcon, int activityId) {
            super(name, sbIcons, qsIcons, contentDesc, sbNullState, qsNullState, sbDiscState,
                    qsDiscState, discContentDesc);
            mDataContentDescription = dataContentDesc;
            mDataType = dataType;
            mIsWide = isWide;
            mQsDataType = qsDataType;
            mSingleSignalIcon = singleSignalIcon;
            mStackedDataIcon = stackedDataIcon;
            mStackedVoiceIcon = stackedVoicelIcon;
            mActivityId = activityId;
        }
    }

    static class MobileState extends SignalController.State {
        String networkName;
        String networkNameData;
        boolean dataSim;
        boolean dataConnected;
        boolean isEmergency;
        boolean airplaneMode;
        boolean carrierNetworkChangeMode;
        boolean isDefault;
        boolean userSetup;
        boolean roaming;
        int dataActivity;
        int voiceLevel;
        int dataNetType;
        int voiceNetType;
        int dataRegState;
        int voiceRegState;

        @Override
        public void copyFrom(State s) {
            super.copyFrom(s);
            MobileState state = (MobileState) s;
            dataSim = state.dataSim;
            networkName = state.networkName;
            networkNameData = state.networkNameData;
            dataConnected = state.dataConnected;
            isDefault = state.isDefault;
            isEmergency = state.isEmergency;
            airplaneMode = state.airplaneMode;
            carrierNetworkChangeMode = state.carrierNetworkChangeMode;
            userSetup = state.userSetup;
            roaming = state.roaming;
            dataActivity = state.dataActivity;
            voiceLevel = state.voiceLevel;
            dataNetType = state.dataNetType;
            voiceNetType = state.voiceNetType;
            dataRegState = state.dataRegState;
            voiceRegState = state.voiceRegState;
        }

        @Override
        protected void toString(StringBuilder builder) {
            super.toString(builder);
            builder.append(',');
            builder.append("dataSim=").append(dataSim).append(',');
            builder.append("networkName=").append(networkName).append(',');
            builder.append("networkNameData=").append(networkNameData).append(',');
            builder.append("dataConnected=").append(dataConnected).append(',');
            builder.append("roaming=").append(roaming).append(',');
            builder.append("isDefault=").append(isDefault).append(',');
            builder.append("isEmergency=").append(isEmergency).append(',');
            builder.append("airplaneMode=").append(airplaneMode).append(',');
            builder.append("carrierNetworkChangeMode=").append(carrierNetworkChangeMode)
                    .append(',');
            builder.append("userSetup=").append(userSetup).append(',');
            builder.append("voiceLevel=").append(voiceLevel).append(',');
            builder.append("dataActivity=").append(dataActivity);
            builder.append("dataNetType=").append(dataNetType);
            builder.append("voiceNetType=").append(voiceNetType);
            builder.append("dataRegState=").append(dataRegState);
            builder.append("voiceRegState=").append(voiceRegState);
        }

        @Override
        public boolean equals(Object o) {
            return super.equals(o)
                    && Objects.equals(((MobileState) o).networkName, networkName)
                    && Objects.equals(((MobileState) o).networkNameData, networkNameData)
                    && ((MobileState) o).dataSim == dataSim
                    && ((MobileState) o).dataConnected == dataConnected
                    && ((MobileState) o).isEmergency == isEmergency
                    && ((MobileState) o).airplaneMode == airplaneMode
                    && ((MobileState) o).carrierNetworkChangeMode == carrierNetworkChangeMode
                    && ((MobileState) o).userSetup == userSetup
                    && ((MobileState) o).isDefault == isDefault
                    && ((MobileState) o).roaming == roaming
                    && ((MobileState) o).voiceLevel == voiceLevel
                    && ((MobileState) o).dataActivity == dataActivity
                    && ((MobileState) o).dataNetType == dataNetType
                    && ((MobileState) o).voiceNetType == voiceNetType
                    && ((MobileState) o).dataRegState == dataRegState
                    && ((MobileState) o).voiceRegState == voiceRegState;
        }
    }
}
