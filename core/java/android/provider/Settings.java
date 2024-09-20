/*
 * Copyright (C) 2006 The Android Open Source Project
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

package android.provider;

import android.Manifest;
import android.annotation.CallbackExecutor;
import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.PermissionMethod;
import android.annotation.PermissionName;
import android.annotation.RequiresPermission;
import android.annotation.SdkConstant;
import android.annotation.SdkConstant.SdkConstantType;
import android.annotation.SuppressLint;
import android.annotation.SystemApi;
import android.annotation.TestApi;
import android.annotation.UserIdInt;
import android.app.Activity;
import android.app.ActivityThread;
import android.app.AppOpsManager;
import android.app.Application;
import android.app.AutomaticZenRule;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.SearchManager;
import android.app.WallpaperManager;
import android.compat.annotation.UnsupportedAppUsage;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.IContentProvider;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.SQLException;
import android.location.ILocationManager;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkScoreManager;
import android.net.Uri;
import android.net.wifi.SoftApConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.DropBoxManager;
import android.os.IBinder;
import android.os.LocaleList;
import android.os.PowerManager;
import android.os.PowerManager.AutoPowerSaveModeTriggers;
import android.os.Process;
import android.os.RemoteCallback;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.speech.tts.TextToSpeech;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.AndroidException;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import android.util.MemoryIntArray;
import android.util.Slog;
import android.view.Display;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.Editor;

import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.Preconditions;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import com.android.internal.util.crdroid.DeviceConfigUtils;

/**
 * The Settings provider contains global system-level device preferences.
 */
public final class Settings {
    /** @hide */
    public static final boolean DEFAULT_OVERRIDEABLE_BY_RESTORE = false;

    // Intent actions for Settings

    /**
     * Activity Action: Show system settings.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_SETTINGS = "android.settings.SETTINGS";

    /**
     * Activity Action: Show settings to provide guide about carrier satellite messaging.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you
     * safeguard against this.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    @FlaggedApi(com.android.internal.telephony.flags.Flags.FLAG_CARRIER_ENABLED_SATELLITE_FLAG)
    public static final String ACTION_SATELLITE_SETTING = "android.settings.SATELLITE_SETTING";

    /**
     * Activity Action: Show settings to allow configuration of APNs.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     *
     * <p class="note">
     * In some cases, a matching Activity may not exist, so ensure you
     * safeguard against this.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_APN_SETTINGS = "android.settings.APN_SETTINGS";

    /**
     * Activity Action: Show settings to allow configuration of current location
     * sources.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you
     * safeguard against this.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_LOCATION_SOURCE_SETTINGS =
            "android.settings.LOCATION_SOURCE_SETTINGS";

    /**
     * Activity Action: Show settings to allow configuration of location controller extra package.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you
     * safeguard against this.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     *
     * @hide
     */
    @SystemApi
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_LOCATION_CONTROLLER_EXTRA_PACKAGE_SETTINGS =
            "android.settings.LOCATION_CONTROLLER_EXTRA_PACKAGE_SETTINGS";

    /**
     * Activity Action: Show scanning settings to allow configuration of Wi-Fi
     * and Bluetooth scanning settings.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you
     * safeguard against this.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     * @hide
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_LOCATION_SCANNING_SETTINGS =
            "android.settings.LOCATION_SCANNING_SETTINGS";

    /**
     * Activity Action: Show settings to manage creation/deletion of cloned apps.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you
     * safeguard against this.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     * @hide
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_MANAGE_CLONED_APPS_SETTINGS =
            "android.settings.MANAGE_CLONED_APPS_SETTINGS";

    /**
     * Activity Action: Show settings to allow configuration of users.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you
     * safeguard against this.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     * @hide
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    @SystemApi
    public static final String ACTION_USER_SETTINGS =
            "android.settings.USER_SETTINGS";

    /**
     * Activity Action: Show settings to allow configuration of wireless controls
     * such as Wi-Fi, Bluetooth and Mobile networks.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you
     * safeguard against this.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_WIRELESS_SETTINGS =
            "android.settings.WIRELESS_SETTINGS";

    /**
     * Activity Action: Show tether provisioning activity.
     *
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you
     * safeguard against this.
     * <p>
     * Input: {@link ConnectivityManager#EXTRA_TETHER_TYPE} should be included to specify which type
     * of tethering should be checked. {@link ConnectivityManager#EXTRA_PROVISION_CALLBACK} should
     * contain a {@link ResultReceiver} which will be called back with a tether result code.
     * <p>
     * Output: The result of the provisioning check.
     * {@link ConnectivityManager#TETHER_ERROR_NO_ERROR} if successful,
     * {@link ConnectivityManager#TETHER_ERROR_PROVISION_FAILED} for failure.
     *
     * @hide
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    @SystemApi
    public static final String ACTION_TETHER_PROVISIONING_UI =
            "android.settings.TETHER_PROVISIONING_UI";

    /**
     * Activity Action: Show a dialog activity to notify tethering is NOT supported by carrier.
     *
     * When {@link android.telephony.CarrierConfigManager#KEY_CARRIER_SUPPORTS_TETHERING_BOOL}
     * is false, and tethering is started by Settings, this dialog activity will be started to
     * tell the user that tethering is not supported by carrier.
     *
     * @hide
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    @SystemApi
    public static final String ACTION_TETHER_UNSUPPORTED_CARRIER_UI =
            "android.settings.TETHER_UNSUPPORTED_CARRIER_UI";

    /**
     * Activity Action: Show settings to allow entering/exiting airplane mode.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you
     * safeguard against this.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_AIRPLANE_MODE_SETTINGS =
            "android.settings.AIRPLANE_MODE_SETTINGS";

    /**
     * Activity Action: Show enabled eSim profile in Settings
     * <p>
     * This opens the Settings page for the currently enabled eSim profile
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     * @hide
     */
    public static final String ACTION_SHOW_ENABLED_ESIM_PROFILE =
            "android.settings.SHOW_ENABLED_ESIM_PROFILE";

    /**
     * Activity Action: Show mobile data usage list.
     * <p>
     * Input: {@link EXTRA_NETWORK_TEMPLATE} and {@link EXTRA_SUB_ID} should be included to specify
     * how and what mobile data statistics should be collected.
     * <p>
     * Output: Nothing
     * @hide
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_MOBILE_DATA_USAGE =
            "android.settings.MOBILE_DATA_USAGE";

    /** @hide */
    public static final String EXTRA_NETWORK_TEMPLATE = "network_template";

    /**
     * Activity Action: Show One-handed mode Settings page.
     * <p>
     * Input: Nothing
     * <p>
     * Output: Nothing
     * @hide
     */
    public static final String ACTION_ONE_HANDED_SETTINGS =
            "android.settings.action.ONE_HANDED_SETTINGS";
    /**
     * The return values for {@link Settings.Config#set}
     * @hide
     */
    @IntDef(prefix = "SET_ALL_RESULT_",
            value = { SET_ALL_RESULT_FAILURE, SET_ALL_RESULT_SUCCESS, SET_ALL_RESULT_DISABLED })
    @Retention(RetentionPolicy.SOURCE)
    @Target({ElementType.TYPE_PARAMETER, ElementType.TYPE_USE})
    public @interface SetAllResult {}

    /**
     * A return value for {@link #KEY_CONFIG_SET_ALL_RETURN}, indicates failure.
     * @hide
     */
    public static final int SET_ALL_RESULT_FAILURE = 0;

    /**
     * A return value for {@link #KEY_CONFIG_SET_ALL_RETURN}, indicates success.
     * @hide
     */
    public static final int SET_ALL_RESULT_SUCCESS = 1;

    /**
     * A return value for {@link #KEY_CONFIG_SET_ALL_RETURN}, indicates a set all is disabled.
     * @hide
     */
    public static final int SET_ALL_RESULT_DISABLED = 2;

    /** @hide */
    public static final String KEY_CONFIG_SET_ALL_RETURN = "config_set_all_return";

    /** @hide */
    public static final String KEY_CONFIG_GET_SYNC_DISABLED_MODE_RETURN =
            "config_get_sync_disabled_mode_return";

    /**
     * An int extra specifying a subscription ID.
     *
     * @see android.telephony.SubscriptionInfo#getSubscriptionId
     */
    public static final String EXTRA_SUB_ID = "android.provider.extra.SUB_ID";

    /**
     * Activity Action: Modify Airplane mode settings using a voice command.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you safeguard against this.
     * <p>
     * This intent MUST be started using
     * {@link android.service.voice.VoiceInteractionSession#startVoiceActivity
     * startVoiceActivity}.
     * <p>
     * Note: The activity implementing this intent MUST verify that
     * {@link android.app.Activity#isVoiceInteraction isVoiceInteraction} returns true before
     * modifying the setting.
     * <p>
     * Input: To tell which state airplane mode should be set to, add the
     * {@link #EXTRA_AIRPLANE_MODE_ENABLED} extra to this Intent with the state specified.
     * If the extra is not included, no changes will be made.
     * <p>
     * Output: Nothing.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_VOICE_CONTROL_AIRPLANE_MODE =
            "android.settings.VOICE_CONTROL_AIRPLANE_MODE";

    /**
     * Activity Action: Show settings for accessibility modules.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you
     * safeguard against this.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_ACCESSIBILITY_SETTINGS =
            "android.settings.ACCESSIBILITY_SETTINGS";

    /**
     * Activity Action: Show detail settings of a particular accessibility service.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you safeguard against this.
     * <p>
     * Input: {@link Intent#EXTRA_COMPONENT_NAME} must specify the accessibility service component
     * name to be shown.
     * <p>
     * Output: Nothing.
     * @hide
     **/
    @SystemApi
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_ACCESSIBILITY_DETAILS_SETTINGS =
            "android.settings.ACCESSIBILITY_DETAILS_SETTINGS";

    /**
     * Activity Action: Show settings to allow configuration of an accessibility
     * shortcut belonging to an accessibility feature or features.
     * <p>
     * Input: ":settings:show_fragment_args" must contain "targets" denoting the services to edit.
     * <p>
     * Output: Nothing.
     * @hide
     **/
    public static final String ACTION_ACCESSIBILITY_SHORTCUT_SETTINGS =
            "android.settings.ACCESSIBILITY_SHORTCUT_SETTINGS";

    /**
     * Activity Action: Show settings to allow configuration of accessibility color and motion.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you
     * safeguard against this.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     * @hide
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_ACCESSIBILITY_COLOR_MOTION_SETTINGS =
            "android.settings.ACCESSIBILITY_COLOR_MOTION_SETTINGS";

    /**
     * Activity Action: Show settings to allow configuration of accessibility color contrast.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you
     * safeguard against this.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     * @hide
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_ACCESSIBILITY_COLOR_CONTRAST_SETTINGS =
            "android.settings.ACCESSIBILITY_COLOR_CONTRAST_SETTINGS";

    /**
     * Activity Action: Show settings to allow configuration of Reduce Bright Colors.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you
     * safeguard against this.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     * @hide
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_REDUCE_BRIGHT_COLORS_SETTINGS =
            "android.settings.REDUCE_BRIGHT_COLORS_SETTINGS";

    /**
     * Activity Action: Show settings to allow configuration of Color correction.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you
     * safeguard against this.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     * @hide
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_COLOR_CORRECTION_SETTINGS =
            "com.android.settings.ACCESSIBILITY_COLOR_SPACE_SETTINGS";

    /**
     * Activity Action: Show settings to allow configuration of Color inversion.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you
     * safeguard against this.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     * @hide
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_COLOR_INVERSION_SETTINGS =
            "android.settings.COLOR_INVERSION_SETTINGS";

    /**
     * Activity Action: Show settings to allow configuration of text reading.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you
     * safeguard against this.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     * @hide
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_TEXT_READING_SETTINGS =
            "android.settings.TEXT_READING_SETTINGS";

    /**
     * Activity Action: Show settings to control access to usage information.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you
     * safeguard against this.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_USAGE_ACCESS_SETTINGS =
            "android.settings.USAGE_ACCESS_SETTINGS";

    /**
     * Activity Category: Show application settings related to usage access.
     * <p>
     * An activity that provides a user interface for adjusting usage access related
     * preferences for its containing application. Optional but recommended for apps that
     * use {@link android.Manifest.permission#PACKAGE_USAGE_STATS}.
     * <p>
     * The activity may define meta-data to describe what usage access is
     * used for within their app with {@link #METADATA_USAGE_ACCESS_REASON}, which
     * will be displayed in Settings.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     */
    @SdkConstant(SdkConstantType.INTENT_CATEGORY)
    public static final String INTENT_CATEGORY_USAGE_ACCESS_CONFIG =
            "android.intent.category.USAGE_ACCESS_CONFIG";

    /**
     * Metadata key: Reason for needing usage access.
     * <p>
     * A key for metadata attached to an activity that receives action
     * {@link #INTENT_CATEGORY_USAGE_ACCESS_CONFIG}, shown to the
     * user as description of how the app uses usage access.
     * <p>
     */
    public static final String METADATA_USAGE_ACCESS_REASON =
            "android.settings.metadata.USAGE_ACCESS_REASON";

    /**
     * Activity Action: Show settings to allow configuration of security and
     * location privacy.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you
     * safeguard against this.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_SECURITY_SETTINGS =
            "android.settings.SECURITY_SETTINGS";

    /**
     * Activity Action: Show settings to allow configuration of trusted external sources
     *
     * Input: Optionally, the Intent's data URI can specify the application package name to
     * directly invoke the management GUI specific to the package name. For example
     * "package:com.my.app".
     * <p>
     * Output: Nothing.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_MANAGE_UNKNOWN_APP_SOURCES =
            "android.settings.MANAGE_UNKNOWN_APP_SOURCES";

    /**
     * Activity Action: Show settings to allow configuration of
     * {@link Manifest.permission#SCHEDULE_EXACT_ALARM} permission
     *
     * Input: Optionally, the Intent's data URI can specify the application package name to
     * directly invoke the management GUI specific to the package name. For example
     * "package:com.my.app".
     * <p>
     * Output: When a package data uri is passed as input, the activity result is set to
     * {@link android.app.Activity#RESULT_OK} if the permission was granted to the app. Otherwise,
     * the result is set to {@link android.app.Activity#RESULT_CANCELED}.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_REQUEST_SCHEDULE_EXACT_ALARM =
            "android.settings.REQUEST_SCHEDULE_EXACT_ALARM";

    /**
     * Activity Action: Show settings to allow configuration of
     * {@link Manifest.permission#MANAGE_MEDIA} permission
     *
     * Input: Optionally, the Intent's data URI can specify the application package name to
     * directly invoke the management GUI specific to the package name. For example
     * "package:com.my.app".
     * <p>
     * Output: Nothing.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_REQUEST_MANAGE_MEDIA =
            "android.settings.REQUEST_MANAGE_MEDIA";

    /**
     * Activity Action: Show settings to allow configuration of
     * {@link Manifest.permission#MEDIA_ROUTING_CONTROL} permission.
     *
     * Input: Optionally, the Intent's data URI can specify the application package name to
     * directly invoke the management GUI specific to the package name. For example
     * "package:com.my.app". However, modifying this permission setting for any package is allowed
     * only when that package holds an appropriate companion device profile such as
     * {@link android.companion.AssociationRequest#DEVICE_PROFILE_WATCH}.
     * <p>
     * Output: Nothing.
     */
    @FlaggedApi("com.android.media.flags.enable_privileged_routing_for_media_routing_control")
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_REQUEST_MEDIA_ROUTING_CONTROL =
            "android.settings.REQUEST_MEDIA_ROUTING_CONTROL";

    /**
     * Activity Action: Show settings to allow configuration of
     * {@link Manifest.permission#RUN_USER_INITIATED_JOBS} permission
     *
     * Input: Optionally, the Intent's data URI can specify the application package name to
     * directly invoke the management GUI specific to the package name. For example
     * "package:com.my.app".
     * <p>
     * Output: When a package data uri is passed as input, the activity result is set to
     * {@link android.app.Activity#RESULT_OK} if the permission was granted to the app. Otherwise,
     * the result is set to {@link android.app.Activity#RESULT_CANCELED}.
     *
     * @hide
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_MANAGE_APP_LONG_RUNNING_JOBS =
            "android.settings.MANAGE_APP_LONG_RUNNING_JOBS";

    /**
     * Activity Action: Show settings to allow configuration of
     * {@link Manifest.permission#RUN_BACKUP_JOBS} permission.
     *
     * Input: Optionally, the Intent's data URI can specify the application package name to
     * directly invoke the management GUI specific to the package name. For example
     * "package:com.my.app".
     * <p>
     * Output: When a package data uri is passed as input, the activity result is set to
     * {@link android.app.Activity#RESULT_OK} if the permission was granted to the app. Otherwise,
     * the result is set to {@link android.app.Activity#RESULT_CANCELED}.
     */
    @FlaggedApi(Flags.FLAG_BACKUP_TASKS_SETTINGS_SCREEN)
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_REQUEST_RUN_BACKUP_JOBS =
            "android.settings.REQUEST_RUN_BACKUP_JOBS";

    /**
     * Activity Action: Show settings to allow configuration of cross-profile access for apps
     *
     * Input: Optionally, the Intent's data URI can specify the application package name to
     * directly invoke the management GUI specific to the package name. For example
     * "package:com.my.app".
     * <p>
     * Output: Nothing.
     *
     * @hide
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_MANAGE_CROSS_PROFILE_ACCESS =
            "android.settings.MANAGE_CROSS_PROFILE_ACCESS";

    /**
     * Activity Action: Show the "Open by Default" page in a particular application's details page.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you safeguard against this.
     * <p>
     * Input: The Intent's data URI specifies the application package name
     * to be shown, with the "package" scheme. That is "package:com.my.app".
     * <p>
     * Output: Nothing.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_APP_OPEN_BY_DEFAULT_SETTINGS =
            "android.settings.APP_OPEN_BY_DEFAULT_SETTINGS";

    /**
     * Activity Action: Show trusted credentials settings, opening to the user tab,
     * to allow management of installed credentials.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you
     * safeguard against this.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     * @hide
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    @UnsupportedAppUsage
    public static final String ACTION_TRUSTED_CREDENTIALS_USER =
            "com.android.settings.TRUSTED_CREDENTIALS_USER";

    /**
     * Activity Action: Show dialog explaining that an installed CA cert may enable
     * monitoring of encrypted network traffic.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you
     * safeguard against this. Add {@link #EXTRA_NUMBER_OF_CERTIFICATES} extra to indicate the
     * number of certificates.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     * @hide
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_MONITORING_CERT_INFO =
            "com.android.settings.MONITORING_CERT_INFO";

    /**
     * Activity Action: Show settings to allow configuration of privacy options, i.e. permission
     * manager, privacy dashboard, privacy controls and more.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you
     * safeguard against this.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_PRIVACY_SETTINGS =
            "android.settings.PRIVACY_SETTINGS";

    /**
     * Activity Action: Show privacy controls sub-page, i.e. privacy (camera/mic) toggles and more.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you
     * safeguard against this.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     * @hide
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_PRIVACY_CONTROLS =
            "android.settings.PRIVACY_CONTROLS";

    /**
     * Activity Action: Show settings to allow configuration of VPN.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you
     * safeguard against this.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_VPN_SETTINGS =
            "android.settings.VPN_SETTINGS";

    /**
     * Activity Action: Show settings to allow configuration of Wi-Fi.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you
     * safeguard against this.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_WIFI_SETTINGS =
            "android.settings.WIFI_SETTINGS";

    /**
     * Activity Action: Show settings to allow configuration of Advanced memory protection.
     * <p>
     * Memory Tagging Extension (MTE) is a CPU extension that allows to protect against certain
     * classes of security problems at a small runtime performance cost overhead.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you safeguard against this.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_ADVANCED_MEMORY_PROTECTION_SETTINGS =
            "android.settings.ADVANCED_MEMORY_PROTECTION_SETTINGS";

    /**
     * Activity Action: Show settings to allow configuration of a static IP
     * address for Wi-Fi.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you safeguard
     * against this.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_WIFI_IP_SETTINGS =
            "android.settings.WIFI_IP_SETTINGS";

    /**
     * Activity Action: Show setting page to process a Wi-Fi Easy Connect (aka DPP) URI and start
     * configuration. This intent should be used when you want to use this device to take on the
     * configurator role for an IoT/other device. When provided with a valid DPP URI
     * string, Settings will open a Wi-Fi selection screen for the user to indicate which network
     * they would like to configure the device specified in the DPP URI string and
     * carry them through the rest of the flow for provisioning the device.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure to safeguard against this by
     * checking {@link WifiManager#isEasyConnectSupported()}.
     * <p>
     * Input: The Intent's data URI specifies bootstrapping information for authenticating and
     * provisioning the peer, and uses a "DPP" scheme. The URI should be attached to the intent
     * using {@link Intent#setData(Uri)}. The calling app can obtain a DPP URI in any
     * way, e.g. by scanning a QR code or other out-of-band methods. The calling app may also
     * attach the {@link #EXTRA_EASY_CONNECT_BAND_LIST} extra to provide information
     * about the bands supported by the enrollee device.
     * <p>
     * Output: After calling {@link android.app.Activity#startActivityForResult}, the callback
     * {@code onActivityResult} will have resultCode {@link android.app.Activity#RESULT_OK} if
     * the Wi-Fi Easy Connect configuration succeeded and the user tapped the 'Done' button, or
     * {@link android.app.Activity#RESULT_CANCELED} if the operation failed and user tapped the
     * 'Cancel' button. In case the operation has failed, a status code from
     * {@link android.net.wifi.EasyConnectStatusCallback} {@code EASY_CONNECT_EVENT_FAILURE_*} will
     * be returned as an Extra {@link #EXTRA_EASY_CONNECT_ERROR_CODE}. Easy Connect R2
     * Enrollees report additional details about the error they encountered, which will be
     * provided in the {@link #EXTRA_EASY_CONNECT_ATTEMPTED_SSID},
     * {@link #EXTRA_EASY_CONNECT_CHANNEL_LIST}, and {@link #EXTRA_EASY_CONNECT_BAND_LIST}.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_PROCESS_WIFI_EASY_CONNECT_URI =
            "android.settings.PROCESS_WIFI_EASY_CONNECT_URI";

    /**
     * Activity Extra: The Easy Connect operation error code
     * <p>
     * An extra returned on the result intent received when using the
     * {@link #ACTION_PROCESS_WIFI_EASY_CONNECT_URI} intent to launch the Easy Connect Operation.
     * This extra contains the integer error code of the operation - one of
     * {@link android.net.wifi.EasyConnectStatusCallback} {@code EASY_CONNECT_EVENT_FAILURE_*}. If
     * there is no error, i.e. if the operation returns {@link android.app.Activity#RESULT_OK},
     * then this extra is not attached to the result intent.
     * <p>
     * Use the {@link Intent#hasExtra(String)} to determine whether the extra is attached and
     * {@link Intent#getIntExtra(String, int)} to obtain the error code data.
     */
    public static final String EXTRA_EASY_CONNECT_ERROR_CODE =
            "android.provider.extra.EASY_CONNECT_ERROR_CODE";

    /**
     * Activity Extra: The SSID that the Enrollee tried to connect to.
     * <p>
     * An extra returned on the result intent received when using the {@link
     * #ACTION_PROCESS_WIFI_EASY_CONNECT_URI} intent to launch the Easy Connect Operation. This
     * extra contains the SSID of the Access Point that the remote Enrollee tried to connect to.
     * This value is populated only by remote R2 devices, and only for the following error codes:
     * {@link android.net.wifi.EasyConnectStatusCallback#EASY_CONNECT_EVENT_FAILURE_CANNOT_FIND_NETWORK}
     * {@link android.net.wifi.EasyConnectStatusCallback#EASY_CONNECT_EVENT_FAILURE_ENROLLEE_AUTHENTICATION}.
     * Therefore, always check if this extra is available using {@link Intent#hasExtra(String)}. If
     * there is no error, i.e. if the operation returns {@link android.app.Activity#RESULT_OK}, then
     * this extra is not attached to the result intent.
     * <p>
     * Use the {@link Intent#getStringExtra(String)} to obtain the SSID.
     */
    public static final String EXTRA_EASY_CONNECT_ATTEMPTED_SSID =
            "android.provider.extra.EASY_CONNECT_ATTEMPTED_SSID";

    /**
     * Activity Extra: The Channel List that the Enrollee used to scan a network.
     * <p>
     * An extra returned on the result intent received when using the {@link
     * #ACTION_PROCESS_WIFI_EASY_CONNECT_URI} intent to launch the Easy Connect Operation. This
     * extra contains the channel list that the Enrollee scanned for a network. This value is
     * populated only by remote R2 devices, and only for the following error code: {@link
     * android.net.wifi.EasyConnectStatusCallback#EASY_CONNECT_EVENT_FAILURE_CANNOT_FIND_NETWORK}.
     * Therefore, always check if this extra is available using {@link Intent#hasExtra(String)}. If
     * there is no error, i.e. if the operation returns {@link android.app.Activity#RESULT_OK}, then
     * this extra is not attached to the result intent. The list is JSON formatted, as an array
     * (Wi-Fi global operating classes) of arrays (Wi-Fi channels).
     * <p>
     * Use the {@link Intent#getStringExtra(String)} to obtain the list.
     */
    public static final String EXTRA_EASY_CONNECT_CHANNEL_LIST =
            "android.provider.extra.EASY_CONNECT_CHANNEL_LIST";

    /**
     * Activity Extra: The Band List that the Enrollee supports.
     * <p>
     * This extra contains the bands the Enrollee supports, expressed as the Global Operating
     * Class, see Table E-4 in IEEE Std 802.11-2016 Global operating classes. It is used both as
     * input, to configure the Easy Connect operation and as output of the operation.
     * <p>
     * As input: an optional extra to be attached to the
     * {@link #ACTION_PROCESS_WIFI_EASY_CONNECT_URI}. If attached, it indicates the bands which
     * the remote device (enrollee, device-to-be-configured) supports. The Settings operation
     * may take this into account when presenting the user with list of networks configurations
     * to be used. The calling app may obtain this information in any out-of-band method. The
     * information should be attached as an array of raw integers - using the
     * {@link Intent#putExtra(String, int[])}.
     * <p>
     * As output: an extra returned on the result intent received when using the
     * {@link #ACTION_PROCESS_WIFI_EASY_CONNECT_URI} intent to launch the Easy Connect Operation
     * . This value is populated only by remote R2 devices, and only for the following error
     * codes:
     * {@link android.net.wifi.EasyConnectStatusCallback#EASY_CONNECT_EVENT_FAILURE_CANNOT_FIND_NETWORK},
     * {@link android.net.wifi.EasyConnectStatusCallback#EASY_CONNECT_EVENT_FAILURE_ENROLLEE_AUTHENTICATION},
     * or
     * {@link android.net.wifi.EasyConnectStatusCallback#EASY_CONNECT_EVENT_FAILURE_ENROLLEE_REJECTED_CONFIGURATION}.
     * Therefore, always check if this extra is available using {@link Intent#hasExtra(String)}.
     * If there is no error, i.e. if the operation returns {@link android.app.Activity#RESULT_OK}
     * , then this extra is not attached to the result intent.
     * <p>
     * Use the {@link Intent#getIntArrayExtra(String)} to obtain the list.
     */
    public static final String EXTRA_EASY_CONNECT_BAND_LIST =
            "android.provider.extra.EASY_CONNECT_BAND_LIST";

    /**
     * Activity Action: Show settings to allow configuration of data and view data usage.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you
     * safeguard against this.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_DATA_USAGE_SETTINGS =
            "android.settings.DATA_USAGE_SETTINGS";

    /**
     * Activity Action: Show settings to allow configuration of Bluetooth.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you
     * safeguard against this.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_BLUETOOTH_SETTINGS =
            "android.settings.BLUETOOTH_SETTINGS";

    /**
     * Activity action: Show Settings app search UI when this action is available for device.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_APP_SEARCH_SETTINGS = "android.settings.APP_SEARCH_SETTINGS";

    /**
     * Activity Action: Show settings to allow configuration of Assist Gesture.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you
     * safeguard against this.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     * @hide
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_ASSIST_GESTURE_SETTINGS =
            "android.settings.ASSIST_GESTURE_SETTINGS";

    /**
     * Activity Action: Show settings to enroll fingerprints, and setup PIN/Pattern/Pass if
     * necessary.
     * @deprecated See {@link #ACTION_BIOMETRIC_ENROLL}.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     */
    @Deprecated
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_FINGERPRINT_ENROLL =
            "android.settings.FINGERPRINT_ENROLL";

    /**
     * Activity Action: Show settings to enroll biometrics, and setup PIN/Pattern/Pass if
     * necessary. By default, this prompts the user to enroll biometrics with strength
     * Weak or above, as defined by the CDD. Only biometrics that meet or exceed Strong, as defined
     * in the CDD are allowed to participate in Keystore operations.
     * <p>
     * Input: extras {@link #EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED} as an integer, with
     * constants defined in {@link android.hardware.biometrics.BiometricManager.Authenticators},
     * e.g. {@link android.hardware.biometrics.BiometricManager.Authenticators#BIOMETRIC_STRONG}.
     * If not specified, the default behavior is
     * {@link android.hardware.biometrics.BiometricManager.Authenticators#BIOMETRIC_WEAK}.
     * <p>
     * Output: Nothing. Note that callers should still check
     * {@link android.hardware.biometrics.BiometricManager#canAuthenticate(int)}
     * afterwards to ensure that the user actually completed enrollment.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_BIOMETRIC_ENROLL =
            "android.settings.BIOMETRIC_ENROLL";

    /**
     * Activity Extra: The minimum strength to request enrollment for.
     * <p>
     * This can be passed as an extra field to the {@link #ACTION_BIOMETRIC_ENROLL} intent to
     * indicate that only enrollment for sensors that meet these requirements should be shown. The
     * value should be a combination of the constants defined in
     * {@link android.hardware.biometrics.BiometricManager.Authenticators}.
     */
    public static final String EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED =
            "android.provider.extra.BIOMETRIC_AUTHENTICATORS_ALLOWED";

    /**
     * Activity Action: Show settings to allow configuration of cast endpoints.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you
     * safeguard against this.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_CAST_SETTINGS =
            "android.settings.CAST_SETTINGS";

    /**
     * Activity Action: Show settings to allow configuration of date and time.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you
     * safeguard against this.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_DATE_SETTINGS =
            "android.settings.DATE_SETTINGS";

    /**
     * Activity Action: Show settings to allow configuration of sound and volume.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you
     * safeguard against this.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_SOUND_SETTINGS =
            "android.settings.SOUND_SETTINGS";

    /**
     * Activity Action: Show settings to allow configuration of display.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you
     * safeguard against this.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_DISPLAY_SETTINGS =
            "android.settings.DISPLAY_SETTINGS";

    /**
     * Activity Action: Show Auto Rotate configuration settings.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_AUTO_ROTATE_SETTINGS =
            "android.settings.AUTO_ROTATE_SETTINGS";

    /**
     * Activity Action: Show settings to allow configuration of Night display.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you
     * safeguard against this.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_NIGHT_DISPLAY_SETTINGS =
            "android.settings.NIGHT_DISPLAY_SETTINGS";

    /**
     * Activity Action: Show settings to allow configuration of Dark theme.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you
     * safeguard against this.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     *
     * @hide
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_DARK_THEME_SETTINGS =
            "android.settings.DARK_THEME_SETTINGS";

    /**
     * Activity Action: Show settings to allow configuration of locale.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you
     * safeguard against this.
     * <p>
     * Input: The optional {@code #EXTRA_EXPLICIT_LOCALES} with language tags that contains locales
     * to limit available locales. This is only supported when device is under demo mode.
     * If intent does not contain this extra, it will show system supported locale list.
     * <br/>
     * If {@code #EXTRA_EXPLICIT_LOCALES} contain a unsupported locale, it will still show this
     * locale on list, but may not be supported by the devcie.
     *
     * Output: Nothing.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_LOCALE_SETTINGS =
            "android.settings.LOCALE_SETTINGS";

    /**
     * Activity Extra: Show explicit locales in launched locale picker activity.
     *
     * This can be passed as an extra field in an Activity Intent with one or more language tags
     * as a {@link LocaleList}. This must be passed as an extra field to the
     * {@link #ACTION_LOCALE_SETTINGS}.
     *
     * @hide
     */
    public static final String EXTRA_EXPLICIT_LOCALES =
            "android.provider.extra.EXPLICIT_LOCALES";

    /**
     * Activity Action: Show settings to allow configuration of per application locale.
     * <p>
     * Input: The Intent's data URI can specify the application package name to directly invoke the
     * app locale details GUI specific to the package name.
     * For example "package:com.my.app".
     * <p>
     * Output: Nothing.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_APP_LOCALE_SETTINGS =
            "android.settings.APP_LOCALE_SETTINGS";

    /**
     * Activity Action: Show settings to allow configuration of regional preferences
     * <p>
     * Input: Nothing
     * <p>
     * Output: Nothing.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_REGIONAL_PREFERENCES_SETTINGS =
            "android.settings.REGIONAL_PREFERENCES_SETTINGS";

    /**
     * Activity Action: Show settings to allow configuration of lockscreen.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you
     * safeguard against this.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     *
     * @hide
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_LOCKSCREEN_SETTINGS = "android.settings.LOCK_SCREEN_SETTINGS";

    /**
     * Activity Action: Show settings to allow pairing bluetooth devices.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you
     * safeguard against this.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     *
     * @hide
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_BLUETOOTH_PAIRING_SETTINGS =
            "android.settings.BLUETOOTH_PAIRING_SETTINGS";

    /**
     * Activity Action: Show settings to configure input methods, in particular
     * allowing the user to enable input methods.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you
     * safeguard against this.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_VOICE_INPUT_SETTINGS =
            "android.settings.VOICE_INPUT_SETTINGS";

    /**
     * Activity Action: Show settings to configure input methods, in particular
     * allowing the user to enable input methods.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you
     * safeguard against this.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_INPUT_METHOD_SETTINGS =
            "android.settings.INPUT_METHOD_SETTINGS";

    /**
     * Activity Action: Show settings to enable/disable input method subtypes.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you
     * safeguard against this.
     * <p>
     * To tell which input method's subtypes are displayed in the settings, add
     * {@link #EXTRA_INPUT_METHOD_ID} extra to this Intent with the input method id.
     * If there is no extra in this Intent, subtypes from all installed input methods
     * will be displayed in the settings.
     *
     * @see android.view.inputmethod.InputMethodInfo#getId
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_INPUT_METHOD_SUBTYPE_SETTINGS =
            "android.settings.INPUT_METHOD_SUBTYPE_SETTINGS";

    /**
     * Activity Action: Show settings to manage the user input dictionary.
     * <p>
     * Starting with {@link android.os.Build.VERSION_CODES#KITKAT},
     * it is guaranteed there will always be an appropriate implementation for this Intent action.
     * In prior releases of the platform this was optional, so ensure you safeguard against it.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_USER_DICTIONARY_SETTINGS =
            "android.settings.USER_DICTIONARY_SETTINGS";

    /**
     * Activity Action: Show settings to configure the hardware keyboard.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you
     * safeguard against this.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_HARD_KEYBOARD_SETTINGS =
            "android.settings.HARD_KEYBOARD_SETTINGS";

    /**
     * Activity Action: Adds a word to the user dictionary.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you
     * safeguard against this.
     * <p>
     * Input: An extra with key <code>word</code> that contains the word
     * that should be added to the dictionary.
     * <p>
     * Output: Nothing.
     *
     * @hide
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public static final String ACTION_USER_DICTIONARY_INSERT =
            "com.android.settings.USER_DICTIONARY_INSERT";

    /**
     * Activity Action: Show settings to allow configuration of application-related settings.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you
     * safeguard against this.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_APPLICATION_SETTINGS =
            "android.settings.APPLICATION_SETTINGS";

    /**
     * Activity Action: Show settings to allow configuration of application
     * development-related settings.  As of
     * {@link android.os.Build.VERSION_CODES#JELLY_BEAN_MR1} this action is
     * a required part of the platform.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_APPLICATION_DEVELOPMENT_SETTINGS =
            "android.settings.APPLICATION_DEVELOPMENT_SETTINGS";

    /**
     * Activity Action: Show settings to allow configuration of quick launch shortcuts.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you
     * safeguard against this.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_QUICK_LAUNCH_SETTINGS =
            "android.settings.QUICK_LAUNCH_SETTINGS";

    /**
     * Activity Action: Show settings to manage installed applications.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you
     * safeguard against this.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_MANAGE_APPLICATIONS_SETTINGS =
            "android.settings.MANAGE_APPLICATIONS_SETTINGS";

    /**
     * Activity Action: Show settings to manage all applications.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you
     * safeguard against this.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS =
            "android.settings.MANAGE_ALL_APPLICATIONS_SETTINGS";

    /**
     * Activity Action: Show settings to manage all SIM profiles.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you
     * safeguard against this.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_MANAGE_ALL_SIM_PROFILES_SETTINGS =
            "android.settings.MANAGE_ALL_SIM_PROFILES_SETTINGS";

    /**
     * Activity Action: Show screen for controlling which apps can draw on top of other apps.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you safeguard against this.
     * <p>
     * Input: Optionally, in versions of Android prior to {@link android.os.Build.VERSION_CODES#R},
     * the Intent's data URI can specify the application package name to directly invoke the
     * management GUI specific to the package name.
     * For example "package:com.my.app".
     * <p>
     * Output: Nothing.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_MANAGE_OVERLAY_PERMISSION =
            "android.settings.action.MANAGE_OVERLAY_PERMISSION";

    /**
     * Activity Action: Show screen for controlling if the app specified in the data URI of the
     * intent can draw on top of other apps.
     * <p>
     * Unlike {@link #ACTION_MANAGE_OVERLAY_PERMISSION}, which in Android {@link
     * android.os.Build.VERSION_CODES#R} can't be used to show a GUI for a specific package,
     * permission {@code android.permission.INTERNAL_SYSTEM_WINDOW} is needed to start an activity
     * with this intent.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you
     * safeguard against this.
     * <p>
     * Input: The Intent's data URI MUST specify the application package name whose ability of
     * drawing on top of other apps you want to control.
     * For example "package:com.my.app".
     * <p>
     * Output: Nothing.
     *
     * @hide
     */
    @SystemApi
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_MANAGE_APP_OVERLAY_PERMISSION =
            "android.settings.MANAGE_APP_OVERLAY_PERMISSION";

    /**
     * Activity Action: Show screen for controlling which apps are allowed to write/modify
     * system settings.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you
     * safeguard against this.
     * <p>
     * Input: Optionally, the Intent's data URI can specify the application package name to
     * directly invoke the management GUI specific to the package name. For example
     * "package:com.my.app".
     * <p>
     * Output: Nothing.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_MANAGE_WRITE_SETTINGS =
            "android.settings.action.MANAGE_WRITE_SETTINGS";

    /**
     * Activity Action: Show screen for controlling app usage properties for an app.
     * Input: Intent's extra {@link android.content.Intent#EXTRA_PACKAGE_NAME} must specify the
     * application package name.
     * Output: Nothing.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_APP_USAGE_SETTINGS =
            "android.settings.action.APP_USAGE_SETTINGS";

    /**
     * Activity Action: Show screen of details about a particular application.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you
     * safeguard against this.
     * <p>
     * Input: The Intent's data URI specifies the application package name
     * to be shown, with the "package" scheme.  That is "package:com.my.app".
     * <p>
     * Output: Nothing.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_APPLICATION_DETAILS_SETTINGS =
            "android.settings.APPLICATION_DETAILS_SETTINGS";

    /**
     * Activity Action: Show list of applications that have been running
     * foreground services (to the user "running in the background").
     * <p>
     * Input: Extras "packages" is a string array of package names.
     * <p>
     * Output: Nothing.
     * @hide
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_FOREGROUND_SERVICES_SETTINGS =
            "android.settings.FOREGROUND_SERVICES_SETTINGS";

    /**
     * Activity Action: Show screen for controlling which apps can ignore battery optimizations.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     * <p>
     * You can use {@link android.os.PowerManager#isIgnoringBatteryOptimizations
     * PowerManager.isIgnoringBatteryOptimizations()} to determine if an application is
     * already ignoring optimizations.  You can use
     * {@link #ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS} to ask the user to put you
     * on this list.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS =
            "android.settings.IGNORE_BATTERY_OPTIMIZATION_SETTINGS";

    /**
     * Activity Action: Ask the user to allow an app to ignore battery optimizations (that is,
     * put them on the allowlist of apps shown by
     * {@link #ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS}).  For an app to use this, it also
     * must hold the {@link android.Manifest.permission#REQUEST_IGNORE_BATTERY_OPTIMIZATIONS}
     * permission.
     * <p><b>Note:</b> most applications should <em>not</em> use this; there are many facilities
     * provided by the platform for applications to operate correctly in the various power
     * saving modes.  This is only for unusual applications that need to deeply control their own
     * execution, at the potential expense of the user's battery life.  Note that these applications
     * greatly run the risk of showing to the user as high power consumers on their device.</p>
     * <p>
     * Input: The Intent's data URI must specify the application package name
     * to be shown, with the "package" scheme.  That is "package:com.my.app".
     * <p>
     * Output: Nothing.
     * <p>
     * You can use {@link android.os.PowerManager#isIgnoringBatteryOptimizations
     * PowerManager.isIgnoringBatteryOptimizations()} to determine if an application is
     * already ignoring optimizations.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS =
            "android.settings.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS";

    /**
     * Activity Action: Show screen for controlling any background restrictions imposed on
     * an app. If the system returns true for
     * {@link android.app.ActivityManager#isBackgroundRestricted()}, and the app is not able to
     * satisfy user requests due to being restricted in the background, then this intent can be
     * used to request the user to unrestrict the app.
     * <p>
     * Input: The Intent's data URI must specify the application package name
     *        to be shown, with the "package" scheme, such as "package:com.my.app".
     * <p>
     * Output: Nothing.
     */
    @FlaggedApi(android.app.Flags.FLAG_APP_RESTRICTIONS_API)
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_BACKGROUND_RESTRICTIONS_SETTINGS =
            "android.settings.BACKGROUND_RESTRICTIONS_SETTINGS";

    /**
     * Activity Action: Open the advanced power usage details page of an associated app.
     * <p>
     * Input: Intent's data URI set with an application name, using the
     * "package" schema (like "package:com.my.app")
     * <p>
     * Output: Nothing.
     *
     * @hide
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_VIEW_ADVANCED_POWER_USAGE_DETAIL =
            "android.settings.VIEW_ADVANCED_POWER_USAGE_DETAIL";

    /**
     * Activity Action: Show screen for controlling background data
     * restrictions for a particular application.
     * <p>
     * Input: Intent's data URI set with an application name, using the
     * "package" schema (like "package:com.my.app").
     *
     * <p>
     * Output: Nothing.
     * <p>
     * Applications can also use {@link android.net.ConnectivityManager#getRestrictBackgroundStatus
     * ConnectivityManager#getRestrictBackgroundStatus()} to determine the
     * status of the background data restrictions for them.
     *
     * <p class="note">
     * In some cases, a matching Activity may not exist, so ensure you
     * safeguard against this.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_IGNORE_BACKGROUND_DATA_RESTRICTIONS_SETTINGS =
            "android.settings.IGNORE_BACKGROUND_DATA_RESTRICTIONS_SETTINGS";

    /**
     * @hide
     * Activity Action: Show the "app ops" settings screen.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_APP_OPS_SETTINGS =
            "android.settings.APP_OPS_SETTINGS";

    /**
     * Activity Action: Show settings for system update functionality.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you
     * safeguard against this.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     *
     * @hide
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_SYSTEM_UPDATE_SETTINGS =
            "android.settings.SYSTEM_UPDATE_SETTINGS";

    /**
     * Activity Action: Show settings for managed profile settings.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you
     * safeguard against this.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     *
     * @hide
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_MANAGED_PROFILE_SETTINGS =
            "android.settings.MANAGED_PROFILE_SETTINGS";

    /**
     * Activity Action: Show settings to allow configuration of sync settings.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you
     * safeguard against this.
     * <p>
     * The account types available to add via the add account button may be restricted by adding an
     * {@link #EXTRA_AUTHORITIES} extra to this Intent with one or more syncable content provider's
     * authorities. Only account types which can sync with that content provider will be offered to
     * the user.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_SYNC_SETTINGS =
            "android.settings.SYNC_SETTINGS";

    /**
     * Activity Action: Show add account screen for creating a new account.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you
     * safeguard against this.
     * <p>
     * The account types available to add may be restricted by adding an {@link #EXTRA_AUTHORITIES}
     * extra to the Intent with one or more syncable content provider's authorities.  Only account
     * types which can sync with that content provider will be offered to the user.
     * <p>
     * Account types can also be filtered by adding an {@link #EXTRA_ACCOUNT_TYPES} extra to the
     * Intent with one or more account types.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_ADD_ACCOUNT =
            "android.settings.ADD_ACCOUNT_SETTINGS";

    /**
     * Activity Action: Show settings for enabling or disabling data saver
     * <p></p>
     * In some cases, a matching Activity may not exist, so ensure you
     * safeguard against this.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     *
     * @hide
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_DATA_SAVER_SETTINGS =
            "android.settings.DATA_SAVER_SETTINGS";

    /**
     * Activity Action: Show settings for selecting the network operator.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you
     * safeguard against this.
     * <p>
     * The subscription ID of the subscription for which available network operators should be
     * displayed may be optionally specified with {@link #EXTRA_SUB_ID}.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_NETWORK_OPERATOR_SETTINGS =
            "android.settings.NETWORK_OPERATOR_SETTINGS";

    /**
     * Activity Action: Show settings for selection of 2G/3G.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you
     * safeguard against this.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_DATA_ROAMING_SETTINGS =
            "android.settings.DATA_ROAMING_SETTINGS";

    /**
     * Activity Action: Show settings for internal storage.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you
     * safeguard against this.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_INTERNAL_STORAGE_SETTINGS =
            "android.settings.INTERNAL_STORAGE_SETTINGS";
    /**
     * Activity Action: Show settings for memory card storage.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you
     * safeguard against this.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_MEMORY_CARD_SETTINGS =
            "android.settings.MEMORY_CARD_SETTINGS";

    /**
     * Activity Action: Show settings for global search.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you
     * safeguard against this.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_SEARCH_SETTINGS =
        "android.search.action.SEARCH_SETTINGS";

    /**
     * Activity Action: Show general device information settings (serial
     * number, software version, phone number, etc.).
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you
     * safeguard against this.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_DEVICE_INFO_SETTINGS =
        "android.settings.DEVICE_INFO_SETTINGS";

    /**
     * Activity Action: Show NFC settings.
     * <p>
     * This shows UI that allows NFC to be turned on or off.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you
     * safeguard against this.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing
     * @see android.nfc.NfcAdapter#isEnabled()
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_NFC_SETTINGS = "android.settings.NFC_SETTINGS";

    /**
     * Activity Action: Show NFC Sharing settings.
     * <p>
     * This shows UI that allows NDEF Push (Android Beam) to be turned on or
     * off.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you
     * safeguard against this.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_NFCSHARING_SETTINGS =
        "android.settings.NFCSHARING_SETTINGS";

    /**
     * Activity Action: Show NFC Tap & Pay settings
     * <p>
     * This shows UI that allows the user to configure Tap&Pay
     * settings.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you
     * safeguard against this.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_NFC_PAYMENT_SETTINGS =
        "android.settings.NFC_PAYMENT_SETTINGS";

    /**
     * Activity Action: Show Daydream settings.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you
     * safeguard against this.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     * @see android.service.dreams.DreamService
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_DREAM_SETTINGS = "android.settings.DREAM_SETTINGS";

    /**
     * Activity Action: Show Communal settings.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you
     * safeguard against this.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     *
     * @hide
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_COMMUNAL_SETTING = "android.settings.COMMUNAL_SETTINGS";

    /**
     * Activity Action: Show Notification assistant settings.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you
     * safeguard against this.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_NOTIFICATION_ASSISTANT_SETTINGS =
            "android.settings.NOTIFICATION_ASSISTANT_SETTINGS";

    /**
     * Activity Action: Show Notification listener settings.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you
     * safeguard against this.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     * @see android.service.notification.NotificationListenerService
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_NOTIFICATION_LISTENER_SETTINGS
            = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";

    /**
     * Activity Action: Show notification listener permission settings page for app.
     * <p>
     * Users can grant and deny access to notifications for a {@link ComponentName} from here.
     * See
     * {@link android.app.NotificationManager#isNotificationListenerAccessGranted(ComponentName)}
     * for more details.
     * <p>
     * Input: The extra {@link #EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME} containing the name
     * of the component to grant or revoke notification listener access to.
     * <p>
     * Output: Nothing.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS =
            "android.settings.NOTIFICATION_LISTENER_DETAIL_SETTINGS";

    /**
     * Activity Extra: What component name to show the notification listener permission
     * page for.
     * <p>
     * A string extra containing a {@link ComponentName}. This must be passed as an extra field to
     * {@link #ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS}.
     */
    public static final String EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME =
            "android.provider.extra.NOTIFICATION_LISTENER_COMPONENT_NAME";

    /**
     * Activity Action: Show Do Not Disturb access settings.
     * <p>
     * Users can grant and deny access to Do Not Disturb configuration from here. Managed
     * profiles cannot grant Do Not Disturb access.
     * See {@link android.app.NotificationManager#isNotificationPolicyAccessGranted()} for more
     * details.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     *
     * <p class="note">
     * In some cases, a matching Activity may not exist, so ensure you
     * safeguard against this.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS
            = "android.settings.NOTIFICATION_POLICY_ACCESS_SETTINGS";

    /**
     * Activity Action: Show do not disturb setting page for app.
     * <p>
     * Users can grant and deny access to Do Not Disturb configuration for an app from here.
     * See {@link android.app.NotificationManager#isNotificationPolicyAccessGranted()} for more
     * details.
     * <p>
     * Input: Intent's data URI set with an application name, using the
     * "package" schema (like "package:com.my.app").
     * <p>
     * Output: Nothing.
     *
     * @hide
     */
    @SystemApi
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_NOTIFICATION_POLICY_ACCESS_DETAIL_SETTINGS =
            "android.settings.NOTIFICATION_POLICY_ACCESS_DETAIL_SETTINGS";

    /**
     * Activity Action: Show the automatic do not disturb rule listing page
     * <p>
     *     Users can add, enable, disable, and remove automatic do not disturb rules from this
     *     screen. See {@link NotificationManager#addAutomaticZenRule(AutomaticZenRule)} for more
     *     details.
     * </p>
     * <p>
     *     Input: Nothing
     *     Output: Nothing
     * </p>
     *
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_CONDITION_PROVIDER_SETTINGS
            = "android.settings.ACTION_CONDITION_PROVIDER_SETTINGS";

    /**
     * Activity Action: Shows the settings page for an {@link AutomaticZenRule} mode.
     * <p>
     * Users can change the behavior of the mode when it's activated and access the owning app's
     * additional configuration screen, where triggering criteria can be modified (see
     * {@link AutomaticZenRule#setConfigurationActivity(ComponentName)}).
     * <p>
     * A matching Activity will only be found if
     * {@link NotificationManager#areAutomaticZenRulesUserManaged()} is true.
     * <p>
     * Input: The id of the rule, provided in the {@link #EXTRA_AUTOMATIC_ZEN_RULE_ID} extra.
     * <p>
     * Output: Nothing.
     */
    @FlaggedApi(android.app.Flags.FLAG_MODES_API)
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_AUTOMATIC_ZEN_RULE_SETTINGS
            = "android.settings.AUTOMATIC_ZEN_RULE_SETTINGS";

    /**
     * Activity Extra: The String id of the {@link AutomaticZenRule mode} settings to display.
     * <p>
     * This must be passed as an extra field to the {@link #ACTION_AUTOMATIC_ZEN_RULE_SETTINGS}.
     */
    @FlaggedApi(android.app.Flags.FLAG_MODES_API)
    public static final String EXTRA_AUTOMATIC_ZEN_RULE_ID
            = "android.provider.extra.AUTOMATIC_ZEN_RULE_ID";

    /**
     * Activity Action: Show settings for video captioning.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you safeguard
     * against this.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_CAPTIONING_SETTINGS = "android.settings.CAPTIONING_SETTINGS";

    /**
     * Activity Action: Show the top level print settings.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you
     * safeguard against this.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_PRINT_SETTINGS =
            "android.settings.ACTION_PRINT_SETTINGS";

    /**
     * Activity Action: Show Zen Mode configuration settings.
     *
     * @hide
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_ZEN_MODE_SETTINGS = "android.settings.ZEN_MODE_SETTINGS";

    /**
     * Activity Action: Show Zen Mode visual effects configuration settings.
     *
     * @hide
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ZEN_MODE_BLOCKED_EFFECTS_SETTINGS =
            "android.settings.ZEN_MODE_BLOCKED_EFFECTS_SETTINGS";

    /**
     * Activity Action: Show Zen Mode onboarding activity.
     *
     * @hide
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ZEN_MODE_ONBOARDING = "android.settings.ZEN_MODE_ONBOARDING";

    /**
     * Activity Action: Show Zen Mode (aka Do Not Disturb) priority configuration settings.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_ZEN_MODE_PRIORITY_SETTINGS
            = "android.settings.ZEN_MODE_PRIORITY_SETTINGS";

    /**
     * Activity Action: Show Zen Mode automation configuration settings.
     *
     * @hide
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_ZEN_MODE_AUTOMATION_SETTINGS
            = "android.settings.ZEN_MODE_AUTOMATION_SETTINGS";

    /**
     * Activity Action: Modify do not disturb mode settings.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you safeguard against this.
     * <p>
     * This intent MUST be started using
     * {@link android.service.voice.VoiceInteractionSession#startVoiceActivity
     * startVoiceActivity}.
     * <p>
     * Note: The Activity implementing this intent MUST verify that
     * {@link android.app.Activity#isVoiceInteraction isVoiceInteraction}.
     * returns true before modifying the setting.
     * <p>
     * Input: The optional {@link #EXTRA_DO_NOT_DISTURB_MODE_MINUTES} extra can be used to indicate
     * how long the user wishes to avoid interruptions for. The optional
     * {@link #EXTRA_DO_NOT_DISTURB_MODE_ENABLED} extra can be to indicate if the user is
     * enabling or disabling do not disturb mode. If either extra is not included, the
     * user maybe asked to provide the value.
     * <p>
     * Output: Nothing.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_VOICE_CONTROL_DO_NOT_DISTURB_MODE =
            "android.settings.VOICE_CONTROL_DO_NOT_DISTURB_MODE";

    /**
     * Activity Action: Show Zen Mode schedule rule configuration settings.
     *
     * @hide
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_ZEN_MODE_SCHEDULE_RULE_SETTINGS
            = "android.settings.ZEN_MODE_SCHEDULE_RULE_SETTINGS";

    /**
     * Activity Action: Show Zen Mode event rule configuration settings.
     *
     * @hide
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_ZEN_MODE_EVENT_RULE_SETTINGS
            = "android.settings.ZEN_MODE_EVENT_RULE_SETTINGS";

    /**
     * Activity Action: Show Zen Mode external rule configuration settings.
     *
     * @hide
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_ZEN_MODE_EXTERNAL_RULE_SETTINGS
            = "android.settings.ZEN_MODE_EXTERNAL_RULE_SETTINGS";

    /**
     * Activity Action: Show the regulatory information screen for the device.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you safeguard
     * against this.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String
            ACTION_SHOW_REGULATORY_INFO = "android.settings.SHOW_REGULATORY_INFO";

    /**
     * Activity Action: Show Device Name Settings.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you safeguard
     * against this.
     *
     * @hide
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String DEVICE_NAME_SETTINGS = "android.settings.DEVICE_NAME";

    /**
     * Activity Action: Show pairing settings.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you safeguard
     * against this.
     *
     * @hide
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_PAIRING_SETTINGS = "android.settings.PAIRING_SETTINGS";

    /**
     * Activity Action: Show battery saver settings.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you safeguard
     * against this.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_BATTERY_SAVER_SETTINGS
            = "android.settings.BATTERY_SAVER_SETTINGS";

    /**
     * Activity Action: Modify Battery Saver mode setting using a voice command.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you safeguard against this.
     * <p>
     * This intent MUST be started using
     * {@link android.service.voice.VoiceInteractionSession#startVoiceActivity
     * startVoiceActivity}.
     * <p>
     * Note: The activity implementing this intent MUST verify that
     * {@link android.app.Activity#isVoiceInteraction isVoiceInteraction} returns true before
     * modifying the setting.
     * <p>
     * Input: To tell which state batter saver mode should be set to, add the
     * {@link #EXTRA_BATTERY_SAVER_MODE_ENABLED} extra to this Intent with the state specified.
     * If the extra is not included, no changes will be made.
     * <p>
     * Output: Nothing.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_VOICE_CONTROL_BATTERY_SAVER_MODE =
            "android.settings.VOICE_CONTROL_BATTERY_SAVER_MODE";

    /**
     * Activity Action: Show Home selection settings. If there are multiple activities
     * that can satisfy the {@link Intent#CATEGORY_HOME} intent, this screen allows you
     * to pick your preferred activity.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_HOME_SETTINGS
            = "android.settings.HOME_SETTINGS";

    /**
     * Activity Action: Show Default apps settings.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you
     * safeguard against this.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_MANAGE_DEFAULT_APPS_SETTINGS
            = "android.settings.MANAGE_DEFAULT_APPS_SETTINGS";

    /**
     * Activity Action: Show More default apps settings.
     * <p>
     * If a Settings activity handles this intent action, a "More defaults" entry will be shown in
     * the Default apps settings, and clicking it will launch that activity.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you safeguard against this.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     *
     * @hide
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    @SystemApi
    public static final String ACTION_MANAGE_MORE_DEFAULT_APPS_SETTINGS =
            "android.settings.MANAGE_MORE_DEFAULT_APPS_SETTINGS";

    /**
     * Activity Action: Show app screen size list settings for user to override app aspect
     * ratio.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you
     * safeguard against this.
     * <p>
     * Can include the following extra {@link android.content.Intent#EXTRA_PACKAGE_NAME} specifying
     * the name of the package to scroll to in the page.
     * @hide
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_MANAGE_USER_ASPECT_RATIO_SETTINGS =
            "android.settings.MANAGE_USER_ASPECT_RATIO_SETTINGS";

    /**
     * Activity Action: Show notification settings.
     *
     * @hide
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_NOTIFICATION_SETTINGS
            = "android.settings.NOTIFICATION_SETTINGS";

    /**
     * Activity Action: Show conversation settings.
     *
     * @hide
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_CONVERSATION_SETTINGS
            = "android.settings.CONVERSATION_SETTINGS";

    /**
     * Activity Action: Show notification history screen.
     *
     * @hide
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_NOTIFICATION_HISTORY
            = "android.settings.NOTIFICATION_HISTORY";

    /**
     * Activity Action: Show app listing settings, filtered by those that send notifications.
     *
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_ALL_APPS_NOTIFICATION_SETTINGS =
            "android.settings.ALL_APPS_NOTIFICATION_SETTINGS";

    /**
     * Activity Action: Show app settings specifically for sending notifications. Same as
     * ALL_APPS_NOTIFICATION_SETTINGS but meant for internal use.
     * @hide
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_ALL_APPS_NOTIFICATION_SETTINGS_FOR_REVIEW =
            "android.settings.ALL_APPS_NOTIFICATION_SETTINGS_FOR_REVIEW";

    /**
     * Activity Action: Show notification settings for a single app.
     * <p>
     *     Input: {@link #EXTRA_APP_PACKAGE}, the package to display.
     * <p>
     * Output: Nothing.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_APP_NOTIFICATION_SETTINGS
            = "android.settings.APP_NOTIFICATION_SETTINGS";

    /**
     * Activity Action: Show notification settings for a single {@link NotificationChannel}.
     * <p>
     *     Input: {@link #EXTRA_APP_PACKAGE}, the package containing the channel to display.
     *     Input: {@link #EXTRA_CHANNEL_ID}, the id of the channel to display.
     * <p>
     * Output: Nothing.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_CHANNEL_NOTIFICATION_SETTINGS
            = "android.settings.CHANNEL_NOTIFICATION_SETTINGS";

    /**
     * Activity Action: Show notification bubble settings for a single app.
     * See {@link NotificationManager#getBubblePreference()}.
     * <p>
     *     Input: {@link #EXTRA_APP_PACKAGE}, the package to display.
     * <p>
     * Output: Nothing.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_APP_NOTIFICATION_BUBBLE_SETTINGS
            = "android.settings.APP_NOTIFICATION_BUBBLE_SETTINGS";

    /**
     * Intent Extra: The value of {@link android.app.settings.SettingsEnums#EntryPointType} for
     * settings metrics that logs the entry point about physical keyboard settings.
     * <p>
     * This must be passed as an extra field to the {@link #ACTION_HARD_KEYBOARD_SETTINGS}.
     * @hide
     */
    public static final String EXTRA_ENTRYPOINT =
            "com.android.settings.inputmethod.EXTRA_ENTRYPOINT";

    /**
     * Activity Extra: The package owner of the notification channel settings to display.
     * <p>
     * This must be passed as an extra field to the {@link #ACTION_CHANNEL_NOTIFICATION_SETTINGS}.
     */
    public static final String EXTRA_APP_PACKAGE = "android.provider.extra.APP_PACKAGE";

    /**
     * Activity Extra: The {@link NotificationChannel#getId()} of the notification channel settings
     * to display.
     * <p>
     * This must be passed as an extra field to the {@link #ACTION_CHANNEL_NOTIFICATION_SETTINGS}.
     */
    public static final String EXTRA_CHANNEL_ID = "android.provider.extra.CHANNEL_ID";

    /**
     * Activity Extra: The {@link NotificationChannel#getConversationId()} of the notification
     * conversation settings to display.
     * <p>
     * This is an optional extra field to the {@link #ACTION_CHANNEL_NOTIFICATION_SETTINGS}. If
     * included the system will first look up notification settings by channel and conversation id,
     * and will fall back to channel id if a specialized channel for this conversation doesn't
     * exist, similar to {@link NotificationManager#getNotificationChannel(String, String)}.
     */
    public static final String EXTRA_CONVERSATION_ID = "android.provider.extra.CONVERSATION_ID";

    /**
     * Activity Extra: An {@code Arraylist<String>} of {@link NotificationChannel} field names to
     * show on the Settings UI.
     *
     * <p>
     * This is an optional extra field to the {@link #ACTION_CHANNEL_NOTIFICATION_SETTINGS}. If
     * included the system will filter out any Settings that doesn't appear in this list that
     * otherwise would display.
     */
    public static final String EXTRA_CHANNEL_FILTER_LIST
            = "android.provider.extra.CHANNEL_FILTER_LIST";

    /**
     * Activity Action: Show notification redaction settings.
     *
     * @hide
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_APP_NOTIFICATION_REDACTION
            = "android.settings.ACTION_APP_NOTIFICATION_REDACTION";

    /** @hide */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public static final String EXTRA_APP_UID = "app_uid";

    /**
     * Activity Action: Show power menu settings.
     *
     * @hide
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_POWER_MENU_SETTINGS =
            "android.settings.ACTION_POWER_MENU_SETTINGS";

    /**
     * Activity Action: Show controls settings.
     *
     * @hide
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_DEVICE_CONTROLS_SETTINGS =
            "android.settings.ACTION_DEVICE_CONTROLS_SETTINGS";

    /**
     * Activity Action: Show media control settings
     *
     * @hide
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_MEDIA_CONTROLS_SETTINGS =
            "android.settings.ACTION_MEDIA_CONTROLS_SETTINGS";

    /**
     * Activity Action: Show a dialog with disabled by policy message.
     * <p> If an user action is disabled by policy, this dialog can be triggered to let
     * the user know about this.
     * <p>
     * Input: {@link Intent#EXTRA_USER}: The user of the admin.
     * <p>
     * Output: Nothing.
     *
     * @hide
     */
    // Intent#EXTRA_USER_ID can also be used
    @SystemApi
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_SHOW_ADMIN_SUPPORT_DETAILS =
            "android.settings.SHOW_ADMIN_SUPPORT_DETAILS";

    /**
     * Intent extra: The id of a setting restricted by supervisors.
     * <p>
     * Type: Integer with a value from the one of the SUPERVISOR_VERIFICATION_* constants below.
     * <ul>
     * <li>{@see #SUPERVISOR_VERIFICATION_SETTING_UNKNOWN}
     * <li>{@see #SUPERVISOR_VERIFICATION_SETTING_BIOMETRICS}
     * </ul>
     * </p>
     */
    public static final String EXTRA_SUPERVISOR_RESTRICTED_SETTING_KEY =
            "android.provider.extra.SUPERVISOR_RESTRICTED_SETTING_KEY";

    /**
     * The unknown setting can usually be ignored and is used for compatibility with future
     * supervisor settings.
     */
    public static final int SUPERVISOR_VERIFICATION_SETTING_UNKNOWN = 0;

    /**
     * Settings for supervisors to control what kinds of biometric sensors, such a face and
     * fingerprint scanners, can be used on the device.
     */
    public static final int SUPERVISOR_VERIFICATION_SETTING_BIOMETRICS = 1;

    /**
     * Keys for {@link #EXTRA_SUPERVISOR_RESTRICTED_SETTING_KEY}.
     * @hide
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = { "SUPERVISOR_VERIFICATION_SETTING_" }, value = {
            SUPERVISOR_VERIFICATION_SETTING_UNKNOWN,
            SUPERVISOR_VERIFICATION_SETTING_BIOMETRICS,
    })
    public @interface SupervisorVerificationSetting {}

    /**
     * Activity action: Launch UI to manage a setting restricted by supervisors.
     * <p>
     * Input: {@link #EXTRA_SUPERVISOR_RESTRICTED_SETTING_KEY} specifies what setting to open.
     * </p>
     * <p>
     * Output: Nothing.
     * </p>
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_MANAGE_SUPERVISOR_RESTRICTED_SETTING =
            "android.settings.MANAGE_SUPERVISOR_RESTRICTED_SETTING";

    /**
     * Activity Action: Show a dialog for remote bugreport flow.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     *
     * @hide
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_SHOW_REMOTE_BUGREPORT_DIALOG
            = "android.settings.SHOW_REMOTE_BUGREPORT_DIALOG";

    /**
     * Activity Action: Show VR listener settings.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     *
     * @see android.service.vr.VrListenerService
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_VR_LISTENER_SETTINGS
            = "android.settings.VR_LISTENER_SETTINGS";

    /**
     * Activity Action: Show Picture-in-picture settings.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     *
     * @hide
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_PICTURE_IN_PICTURE_SETTINGS
            = "android.settings.PICTURE_IN_PICTURE_SETTINGS";

    /**
     * Activity Action: Show Storage Manager settings.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     *
     * @hide
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_STORAGE_MANAGER_SETTINGS
            = "android.settings.STORAGE_MANAGER_SETTINGS";

    /**
     * Activity Action: Allows user to select current webview implementation.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     * <p class="note">
     * In some cases, a matching Activity may not exist, so ensure you
     * safeguard against this.

     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_WEBVIEW_SETTINGS = "android.settings.WEBVIEW_SETTINGS";

    /**
     * Activity Action: Show enterprise privacy section.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     * @hide
     */
    @SystemApi
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_ENTERPRISE_PRIVACY_SETTINGS
            = "android.settings.ENTERPRISE_PRIVACY_SETTINGS";

    /**
     * Activity Action: Show Work Policy info.
     * DPC apps can implement an activity that handles this intent in order to show device policies
     * associated with the work profile or managed device.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     *
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_SHOW_WORK_POLICY_INFO =
            "android.settings.SHOW_WORK_POLICY_INFO";

    /**
     * Activity Action: Show screen that let user select its Autofill Service.
     * <p>
     * Input: Intent's data URI set with an application name, using the
     * "package" schema (like "package:com.my.app").
     *
     * <p>
     * Output: {@link android.app.Activity#RESULT_OK} if user selected an Autofill Service belonging
     * to the caller package.
     *
     * <p>
     * <b>NOTE: </b> Applications should call
     * {@link android.view.autofill.AutofillManager#hasEnabledAutofillServices()} and
     * {@link android.view.autofill.AutofillManager#isAutofillSupported()}, and only use this action
     * to start an activity if they return {@code false} and {@code true} respectively.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_REQUEST_SET_AUTOFILL_SERVICE =
            "android.settings.REQUEST_SET_AUTOFILL_SERVICE";

    /**
     * Activity Action: Show screen that let user enable a Credential Manager provider.
     * <p>
     * Input: Intent's data URI set with an application name, using the
     * "package" schema (like "package:com.my.app").
     *
     * <p>
     * Output: {@link android.app.Activity#RESULT_OK} if user selected a provider belonging
     * to the caller package.
     * <p>
     * <b>NOTE: </b> Applications should call
     * {@link android.credentials.CredentialManager#isEnabledCredentialProviderService(
     * ComponentName)} and only use this action to start an activity if they return {@code false}.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    @FlaggedApi(android.credentials.flags.Flags.FLAG_NEW_SETTINGS_INTENTS)
    public static final String ACTION_CREDENTIAL_PROVIDER =
            "android.settings.CREDENTIAL_PROVIDER";

    /**
     * Activity Action: Show screen for controlling the Quick Access Wallet.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you
     * safeguard against this.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_QUICK_ACCESS_WALLET_SETTINGS =
            "android.settings.QUICK_ACCESS_WALLET_SETTINGS";

    /**
     * Activity Action: Show screen for controlling which apps have access on volume directories.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing.
     * <p>
     * Applications typically use this action to ask the user to revert the "Do not ask again"
     * status of directory access requests made by
     * {@link android.os.storage.StorageVolume#createAccessIntent(String)}.
     * @deprecated use {@link #ACTION_APPLICATION_DETAILS_SETTINGS} to manage storage permissions
     *             for a specific application
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    @Deprecated
    public static final String ACTION_STORAGE_VOLUME_ACCESS_SETTINGS =
            "android.settings.STORAGE_VOLUME_ACCESS_SETTINGS";


    /**
     * Activity Action: Show screen that let user select enable (or disable) Content Capture.
     * <p>
     * Input: Nothing.
     *
     * <p>
     * Output: Nothing
     *
     * @hide
     */
    @SystemApi
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_REQUEST_ENABLE_CONTENT_CAPTURE =
            "android.settings.REQUEST_ENABLE_CONTENT_CAPTURE";

    /**
     * Activity Action: Show screen that let user manage how Android handles URL resolution.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing
     *
     * @hide
     */
    @SystemApi
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_MANAGE_DOMAIN_URLS = "android.settings.MANAGE_DOMAIN_URLS";

    /**
     * Activity Action: Show screen that let user select enable (or disable) tethering.
     * <p>
     * Input: Nothing.
     * <p>
     * Output: Nothing
     *
     * @hide
     */
    @SystemApi
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_TETHER_SETTINGS = "android.settings.TETHER_SETTINGS";

    /**
     * Activity Action: Show screen that lets user configure wifi tethering.
     * <p>
     * In some cases, a matching Activity may not exist, so ensure you safeguard against this.
     * <p>
     * Input: Nothing
     * <p>
     * Output: Nothing
     *
     * @hide
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_WIFI_TETHER_SETTING =
            "com.android.settings.WIFI_TETHER_SETTINGS";

    /**
     * Broadcast to trigger notification of asking user to enable MMS.
     * Need to specify {@link #EXTRA_ENABLE_MMS_DATA_REQUEST_REASON} and {@link #EXTRA_SUB_ID}.
     *
     * @hide
     */
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_ENABLE_MMS_DATA_REQUEST =
            "android.settings.ENABLE_MMS_DATA_REQUEST";

    /**
     * Shows restrict settings dialog when settings is blocked.
     *
     * @hide
     */
    @SystemApi
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_SHOW_RESTRICTED_SETTING_DIALOG =
            "android.settings.SHOW_RESTRICTED_SETTING_DIALOG";

    /**
     * Integer value that specifies the reason triggering enable MMS data notification.
     * This must be passed as an extra field to the {@link #ACTION_ENABLE_MMS_DATA_REQUEST}.
     * Extra with value of EnableMmsDataReason interface.
     * @hide
     */
    public static final String EXTRA_ENABLE_MMS_DATA_REQUEST_REASON =
            "android.settings.extra.ENABLE_MMS_DATA_REQUEST_REASON";

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = { "ENABLE_MMS_DATA_REQUEST_REASON_" }, value = {
            ENABLE_MMS_DATA_REQUEST_REASON_INCOMING_MMS,
            ENABLE_MMS_DATA_REQUEST_REASON_OUTGOING_MMS,
    })
    public @interface EnableMmsDataReason{}

    /**
     * Requesting to enable MMS data because there's an incoming MMS.
     * @hide
     */
    public static final int ENABLE_MMS_DATA_REQUEST_REASON_INCOMING_MMS = 0;

    /**
     * Requesting to enable MMS data because user is sending MMS.
     * @hide
     */
    public static final int ENABLE_MMS_DATA_REQUEST_REASON_OUTGOING_MMS = 1;

    /**
     * Activity Action: Show screen of a cellular subscription and highlight the
     * "enable MMS" toggle.
     * <p>
     * Input: {@link #EXTRA_SUB_ID}: Sub ID of the subscription.
     * <p>
     * Output: Nothing
     *
     * @hide
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_MMS_MESSAGE_SETTING = "android.settings.MMS_MESSAGE_SETTING";

    /**
     * Activity Action: Show a screen of bedtime settings, which is provided by the wellbeing app.
     * <p>
     * The handler of this intent action may not exist.
     * <p>
     * To start an activity with this intent, apps should set the wellbeing package explicitly in
     * the intent together with this action. The wellbeing package is defined in
     * {@code com.android.internal.R.string.config_systemWellbeing}.
     * <p>
     * Output: Nothing
     *
     * @hide
     */
    @SystemApi
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_BEDTIME_SETTINGS = "android.settings.BEDTIME_SETTINGS";

    /**
     * Activity action: Launch UI to manage the permissions of an app.
     * <p>
     * Input: {@link android.content.Intent#EXTRA_PACKAGE_NAME} specifies the package whose
     * permissions will be managed by the launched UI.
     * </p>
     * <p>
     * Output: Nothing.
     * </p>
     *
     * @see android.content.Intent#EXTRA_PACKAGE_NAME
     *
     * @hide
     */
    @SystemApi
    @RequiresPermission(android.Manifest.permission.LAUNCH_PERMISSION_SETTINGS)
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_APP_PERMISSIONS_SETTINGS =
            "android.settings.APP_PERMISSIONS_SETTINGS";

    // End of Intent actions for Settings

    /**
     * @hide - Private call() method on SettingsProvider to read from 'system' table.
     */
    public static final String CALL_METHOD_GET_SYSTEM = "GET_system";

    /**
     * @hide - Private call() method on SettingsProvider to read from 'secure' table.
     */
    public static final String CALL_METHOD_GET_SECURE = "GET_secure";

    /**
     * @hide - Private call() method on SettingsProvider to read from 'global' table.
     */
    public static final String CALL_METHOD_GET_GLOBAL = "GET_global";

    /**
     * @hide - Private call() method on SettingsProvider to read from 'config' table.
     */
    public static final String CALL_METHOD_GET_CONFIG = "GET_config";

    /**
     * @hide - Specifies that the caller of the fast-path call()-based flow tracks
     * the settings generation in order to cache values locally. If this key is
     * mapped to a <code>null</code> string extra in the request bundle, the response
     * bundle will contain the same key mapped to a parcelable extra which would be
     * an {@link android.util.MemoryIntArray}. The response will also contain an
     * integer mapped to the {@link #CALL_METHOD_GENERATION_INDEX_KEY} which is the
     * index in the array clients should use to lookup the generation. For efficiency
     * the caller should request the generation tracking memory array only if it
     * doesn't already have it.
     *
     * @see #CALL_METHOD_GENERATION_INDEX_KEY
     */
    public static final String CALL_METHOD_TRACK_GENERATION_KEY = "_track_generation";

    /**
     * @hide Key with the location in the {@link android.util.MemoryIntArray} where
     * to look up the generation id of the backing table. The value is an integer.
     *
     * @see #CALL_METHOD_TRACK_GENERATION_KEY
     */
    public static final String CALL_METHOD_GENERATION_INDEX_KEY = "_generation_index";

    /**
     * @hide Key with the settings table generation. The value is an integer.
     *
     * @see #CALL_METHOD_TRACK_GENERATION_KEY
     */
    public static final String CALL_METHOD_GENERATION_KEY = "_generation";

    /**
     * @hide - User handle argument extra to the fast-path call()-based requests
     */
    public static final String CALL_METHOD_USER_KEY = "_user";

    /**
     * @hide - Boolean argument extra to the fast-path call()-based requests
     */
    public static final String CALL_METHOD_MAKE_DEFAULT_KEY = "_make_default";

    /**
     * @hide - User handle argument extra to the fast-path call()-based requests
     */
    public static final String CALL_METHOD_RESET_MODE_KEY = "_reset_mode";

    /**
     * @hide - String argument extra to the fast-path call()-based requests
     */
    public static final String CALL_METHOD_TAG_KEY = "_tag";

    /**
     * @hide - String argument extra to the fast-path call()-based requests
     */
    public static final String CALL_METHOD_PREFIX_KEY = "_prefix";

    /**
     * @hide - String argument extra to the fast-path call()-based requests
     */
    public static final String CALL_METHOD_SYNC_DISABLED_MODE_KEY = "_disabled_mode";

    /**
     * @hide - RemoteCallback monitor callback argument extra to the fast-path call()-based requests
     */
    public static final String CALL_METHOD_MONITOR_CALLBACK_KEY = "_monitor_callback_key";

    /**
     * @hide - String argument extra to the fast-path call()-based requests
     */
    public static final String CALL_METHOD_FLAGS_KEY = "_flags";

    /**
     * @hide - String argument extra to the fast-path call()-based requests
     */
    public static final String CALL_METHOD_OVERRIDEABLE_BY_RESTORE_KEY = "_overrideable_by_restore";

    /** @hide - Private call() method to write to 'system' table */
    public static final String CALL_METHOD_PUT_SYSTEM = "PUT_system";

    /** @hide - Private call() method to write to 'secure' table */
    public static final String CALL_METHOD_PUT_SECURE = "PUT_secure";

    /** @hide - Private call() method to write to 'global' table */
    public static final String CALL_METHOD_PUT_GLOBAL= "PUT_global";

    /** @hide - Private call() method to write to 'configuration' table */
    public static final String CALL_METHOD_PUT_CONFIG = "PUT_config";

    /** @hide - Private call() method to write to and delete from the 'configuration' table */
    public static final String CALL_METHOD_SET_ALL_CONFIG = "SET_ALL_config";

    /** @hide - Private call() method to delete from the 'system' table */
    public static final String CALL_METHOD_DELETE_SYSTEM = "DELETE_system";

    /** @hide - Private call() method to delete from the 'secure' table */
    public static final String CALL_METHOD_DELETE_SECURE = "DELETE_secure";

    /** @hide - Private call() method to delete from the 'global' table */
    public static final String CALL_METHOD_DELETE_GLOBAL = "DELETE_global";

    /** @hide - Private call() method to reset to defaults the 'configuration' table */
    public static final String CALL_METHOD_DELETE_CONFIG = "DELETE_config";

    /** @hide - Private call() method to reset to defaults the 'system' table */
    public static final String CALL_METHOD_RESET_SYSTEM = "RESET_system";

    /** @hide - Private call() method to reset to defaults the 'secure' table */
    public static final String CALL_METHOD_RESET_SECURE = "RESET_secure";

    /** @hide - Private call() method to reset to defaults the 'global' table */
    public static final String CALL_METHOD_RESET_GLOBAL = "RESET_global";

    /** @hide - Private call() method to reset to defaults the 'configuration' table */
    public static final String CALL_METHOD_RESET_CONFIG = "RESET_config";

    /** @hide - Private call() method to query the 'system' table */
    public static final String CALL_METHOD_LIST_SYSTEM = "LIST_system";

    /** @hide - Private call() method to query the 'secure' table */
    public static final String CALL_METHOD_LIST_SECURE = "LIST_secure";

    /** @hide - Private call() method to query the 'global' table */
    public static final String CALL_METHOD_LIST_GLOBAL = "LIST_global";

    /** @hide - Private call() method to query the 'configuration' table */
    public static final String CALL_METHOD_LIST_CONFIG = "LIST_config";

    /** @hide - Private call() method to disable / re-enable syncs to the 'configuration' table */
    public static final String CALL_METHOD_SET_SYNC_DISABLED_MODE_CONFIG =
            "SET_SYNC_DISABLED_MODE_config";

    /**
     * @hide - Private call() method to return the current mode of sync disabling for the
     * 'configuration' table
     */
    public static final String CALL_METHOD_GET_SYNC_DISABLED_MODE_CONFIG =
            "GET_SYNC_DISABLED_MODE_config";

    /** @hide - Private call() method to register monitor callback for 'configuration' table */
    public static final String CALL_METHOD_REGISTER_MONITOR_CALLBACK_CONFIG =
            "REGISTER_MONITOR_CALLBACK_config";

    /** @hide - Private call() method to unregister monitor callback for 'configuration' table */
    public static final String CALL_METHOD_UNREGISTER_MONITOR_CALLBACK_CONFIG =
            "UNREGISTER_MONITOR_CALLBACK_config";

    /** @hide - String argument extra to the config monitor callback */
    public static final String EXTRA_MONITOR_CALLBACK_TYPE = "monitor_callback_type";

    /** @hide - String argument extra to the config monitor callback */
    public static final String EXTRA_ACCESS_CALLBACK = "access_callback";

    /** @hide - String argument extra to the config monitor callback */
    public static final String EXTRA_NAMESPACE_UPDATED_CALLBACK =
            "namespace_updated_callback";

    /** @hide - String argument extra to the config monitor callback */
    public static final String EXTRA_NAMESPACE = "namespace";

    /** @hide - String argument extra to the config monitor callback */
    public static final String EXTRA_CALLING_PACKAGE = "calling_package";

    /**
     * Activity Extra: Limit available options in launched activity based on the given authority.
     * <p>
     * This can be passed as an extra field in an Activity Intent with one or more syncable content
     * provider's authorities as a String[]. This field is used by some intents to alter the
     * behavior of the called activity.
     * <p>
     * Example: The {@link #ACTION_ADD_ACCOUNT} intent restricts the account types available based
     * on the authority given.
     */
    public static final String EXTRA_AUTHORITIES = "authorities";

    /**
     * Activity Extra: Limit available options in launched activity based on the given account
     * types.
     * <p>
     * This can be passed as an extra field in an Activity Intent with one or more account types
     * as a String[]. This field is used by some intents to alter the behavior of the called
     * activity.
     * <p>
     * Example: The {@link #ACTION_ADD_ACCOUNT} intent restricts the account types to the specified
     * list.
     */
    public static final String EXTRA_ACCOUNT_TYPES = "account_types";

    public static final String EXTRA_INPUT_METHOD_ID = "input_method_id";

    /**
     * Activity Extra: The device identifier to act upon.
     * <p>
     * This can be passed as an extra field in an Activity Intent with a single
     * InputDeviceIdentifier. This field is used by some activities to jump straight into the
     * settings for the given device.
     * <p>
     * Example: The {@link #ACTION_INPUT_METHOD_SETTINGS} intent opens the keyboard layout
     * dialog for the given device.
     * @hide
     */
    public static final String EXTRA_INPUT_DEVICE_IDENTIFIER = "input_device_identifier";

    /**
     * Activity Extra: Enable or disable Airplane Mode.
     * <p>
     * This can be passed as an extra field to the {@link #ACTION_VOICE_CONTROL_AIRPLANE_MODE}
     * intent as a boolean to indicate if it should be enabled.
     */
    public static final String EXTRA_AIRPLANE_MODE_ENABLED = "airplane_mode_enabled";

    /**
     * Activity Extra: Enable or disable Battery saver mode.
     * <p>
     * This can be passed as an extra field to the {@link #ACTION_VOICE_CONTROL_BATTERY_SAVER_MODE}
     * intent as a boolean to indicate if it should be enabled.
     */
    public static final String EXTRA_BATTERY_SAVER_MODE_ENABLED =
            "android.settings.extra.battery_saver_mode_enabled";

    /**
     * Activity Extra: Enable or disable Do Not Disturb mode.
     * <p>
     * This can be passed as an extra field to the {@link #ACTION_VOICE_CONTROL_DO_NOT_DISTURB_MODE}
     * intent as a boolean to indicate if it should be enabled.
     */
    public static final String EXTRA_DO_NOT_DISTURB_MODE_ENABLED =
            "android.settings.extra.do_not_disturb_mode_enabled";

    /**
     * Activity Extra: How many minutes to enable do not disturb mode for.
     * <p>
     * This can be passed as an extra field to the {@link #ACTION_VOICE_CONTROL_DO_NOT_DISTURB_MODE}
     * intent to indicate how long do not disturb mode should be enabled for.
     */
    public static final String EXTRA_DO_NOT_DISTURB_MODE_MINUTES =
            "android.settings.extra.do_not_disturb_mode_minutes";

    /**
     * Reset mode: reset to defaults only settings changed by the
     * calling package. If there is a default set the setting
     * will be set to it, otherwise the setting will be deleted.
     * This is the only type of reset available to non-system clients.
     * @hide
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @TestApi
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public static final int RESET_MODE_PACKAGE_DEFAULTS = 1;

    /**
     * Reset mode: reset all settings set by untrusted packages, which is
     * packages that aren't a part of the system, to the current defaults.
     * If there is a default set the setting will be set to it, otherwise
     * the setting will be deleted. This mode is only available to the system.
     * @hide
     */
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public static final int RESET_MODE_UNTRUSTED_DEFAULTS = 2;

    /**
     * Reset mode: delete all settings set by untrusted packages, which is
     * packages that aren't a part of the system. If a setting is set by an
     * untrusted package it will be deleted if its default is not provided
     * by the system, otherwise the setting will be set to its default.
     * This mode is only available to the system.
     * @hide
     */
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public static final int RESET_MODE_UNTRUSTED_CHANGES = 3;

    /**
     * Reset mode: reset all settings to defaults specified by trusted
     * packages, which is packages that are a part of the system, and
     * delete all settings set by untrusted packages. If a setting has
     * a default set by a system package it will be set to the default,
     * otherwise the setting will be deleted. This mode is only available
     * to the system.
     * @hide
     */
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public static final int RESET_MODE_TRUSTED_DEFAULTS = 4;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = { "RESET_MODE_" }, value = {
            RESET_MODE_PACKAGE_DEFAULTS,
            RESET_MODE_UNTRUSTED_DEFAULTS,
            RESET_MODE_UNTRUSTED_CHANGES,
            RESET_MODE_TRUSTED_DEFAULTS
    })
    public @interface ResetMode{}

    /**
     * Activity Extra: Number of certificates
     * <p>
     * This can be passed as an extra field to the {@link #ACTION_MONITORING_CERT_INFO}
     * intent to indicate the number of certificates
     * @hide
     */
    public static final String EXTRA_NUMBER_OF_CERTIFICATES =
            "android.settings.extra.number_of_certificates";

    private static final String SYSTEM_PACKAGE_NAME = "android";

    public static final String AUTHORITY = "settings";

    private static final String TAG = "Settings";
    private static final boolean LOCAL_LOGV = false;

    // Used in system server calling uid workaround in call()
    private static boolean sInSystemServer = false;
    private static final Object sInSystemServerLock = new Object();

    /** @hide */
    public static void setInSystemServer() {
        synchronized (sInSystemServerLock) {
            sInSystemServer = true;
        }
    }

    /** @hide */
    public static boolean isInSystemServer() {
        synchronized (sInSystemServerLock) {
            return sInSystemServer;
        }
    }

    public static class SettingNotFoundException extends AndroidException {
        public SettingNotFoundException(String msg) {
            super(msg);
        }
    }

    /**
     * Common base for tables of name/value settings.
     */
    public static class NameValueTable implements BaseColumns {
        public static final String NAME = "name";
        public static final String VALUE = "value";
        // A flag indicating whether the current value of a setting should be preserved during
        // restore.
        /** @hide */
        public static final String IS_PRESERVED_IN_RESTORE = "is_preserved_in_restore";

        protected static boolean putString(ContentResolver resolver, Uri uri,
                String name, String value) {
            // The database will take care of replacing duplicates.
            try {
                ContentValues values = new ContentValues();
                values.put(NAME, name);
                values.put(VALUE, value);
                resolver.insert(uri, values);
                return true;
            } catch (SQLException e) {
                Log.w(TAG, "Can't set key " + name + " in " + uri, e);
                return false;
            }
        }

        public static Uri getUriFor(Uri uri, String name) {
            return Uri.withAppendedPath(uri, name);
        }
    }

    private static final class GenerationTracker {
        @NonNull private final String mName;
        @NonNull private final MemoryIntArray mArray;
        @NonNull private final Consumer<String> mErrorHandler;
        private final int mIndex;
        private int mCurrentGeneration;

        GenerationTracker(@NonNull String name, @NonNull MemoryIntArray array, int index,
                int generation, Consumer<String> errorHandler) {
            mName = name;
            mArray = array;
            mIndex = index;
            mErrorHandler = errorHandler;
            mCurrentGeneration = generation;
        }

        // This method also updates the obsolete generation code stored locally
        public boolean isGenerationChanged() {
            final int currentGeneration = readCurrentGeneration();
            if (currentGeneration >= 0) {
                if (currentGeneration == mCurrentGeneration) {
                    return false;
                }
                mCurrentGeneration = currentGeneration;
            }
            return true;
        }

        public int getCurrentGeneration() {
            return mCurrentGeneration;
        }

        private int readCurrentGeneration() {
            try {
                return mArray.get(mIndex);
            } catch (IOException e) {
                Log.e(TAG, "Error getting current generation", e);
                mErrorHandler.accept(mName);
            }
            return -1;
        }

        public void destroy() {
            maybeCloseGenerationArray(mArray);
        }

        @Override
        protected void finalize() throws Throwable {
            try {
                destroy();
            } finally {
                super.finalize();
            }
        }
    }

    private static void maybeCloseGenerationArray(@Nullable MemoryIntArray array) {
        if (array == null) {
            return;
        }
        try {
            // If this process is the system server process, the MemoryIntArray received from Parcel
            // is the same object as the one kept inside SettingsProvider, so skipping the close().
            if (!Settings.isInSystemServer() && !array.isClosed()) {
                array.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error closing the generation tracking array", e);
        }
    }

    private static final class ContentProviderHolder {
        private final Object mLock = new Object();

        private final Uri mUri;
        @GuardedBy("mLock")
        @UnsupportedAppUsage
        private IContentProvider mContentProvider;

        public ContentProviderHolder(Uri uri) {
            mUri = uri;
        }

        public IContentProvider getProvider(ContentResolver contentResolver) {
            synchronized (mLock) {
                if (mContentProvider == null) {
                    mContentProvider = contentResolver
                            .acquireProvider(mUri.getAuthority());
                }
                return mContentProvider;
            }
        }

        public void clearProviderForTest() {
            synchronized (mLock) {
                mContentProvider = null;
            }
        }
    }

    // Thread-safe.
    private static class NameValueCache {
        private static final boolean DEBUG = false;

        private static final String[] SELECT_VALUE_PROJECTION = new String[] {
                Settings.NameValueTable.VALUE
        };

        private static final String NAME_EQ_PLACEHOLDER = "name=?";

        // Cached values of queried settings.
        // Key is the setting's name, value is the setting's value.
        // Must synchronize on 'this' to access mValues and mValuesVersion.
        private final ArrayMap<String, String> mValues = new ArrayMap<>();

        // Cached values for queried prefixes.
        // Key is the prefix, value is all of the settings under the prefix, mapped from a setting's
        // name to a setting's value. The name string doesn't include the prefix.
        // Must synchronize on 'this' to access.
        private final ArrayMap<String, ArrayMap<String, String>> mPrefixToValues = new ArrayMap<>();

        private final Uri mUri;
        @UnsupportedAppUsage
        private final ContentProviderHolder mProviderHolder;

        // The method we'll call (or null, to not use) on the provider
        // for the fast path of retrieving settings.
        private final String mCallGetCommand;
        private final String mCallSetCommand;
        private final String mCallDeleteCommand;
        private final String mCallListCommand;
        private final String mCallSetAllCommand;

        private final ArraySet<String> mReadableFields;
        private final ArraySet<String> mAllFields;
        private final ArrayMap<String, Integer> mReadableFieldsWithMaxTargetSdk;

        // Mapping from the name of a setting (or the prefix of a namespace) to a generation tracker
        @GuardedBy("this")
        private ArrayMap<String, GenerationTracker> mGenerationTrackers = new ArrayMap<>();

        private Consumer<String> mGenerationTrackerErrorHandler = (String name) -> {
            synchronized (NameValueCache.this) {
                Log.e(TAG, "Error accessing generation tracker - removing");
                final GenerationTracker tracker = mGenerationTrackers.get(name);
                if (tracker != null) {
                    tracker.destroy();
                    mGenerationTrackers.remove(name);
                }
                mValues.remove(name);
            }
        };

        <T extends NameValueTable> NameValueCache(Uri uri, String getCommand,
                String setCommand, String deleteCommand, ContentProviderHolder providerHolder,
                Class<T> callerClass) {
            this(uri, getCommand, setCommand, deleteCommand, null, null, providerHolder,
                    callerClass);
        }

        private <T extends NameValueTable> NameValueCache(Uri uri, String getCommand,
                String setCommand, String deleteCommand, String listCommand, String setAllCommand,
                ContentProviderHolder providerHolder, Class<T> callerClass) {
            mUri = uri;
            mCallGetCommand = getCommand;
            mCallSetCommand = setCommand;
            mCallDeleteCommand = deleteCommand;
            mCallListCommand = listCommand;
            mCallSetAllCommand = setAllCommand;
            mProviderHolder = providerHolder;
            mReadableFields = new ArraySet<>();
            mAllFields = new ArraySet<>();
            mReadableFieldsWithMaxTargetSdk = new ArrayMap<>();
            getPublicSettingsForClass(callerClass, mAllFields, mReadableFields,
                    mReadableFieldsWithMaxTargetSdk);
        }

        public boolean putStringForUser(ContentResolver cr, String name, String value,
                String tag, boolean makeDefault, final int userHandle,
                boolean overrideableByRestore) {
            try {
                Bundle arg = new Bundle();
                arg.putString(Settings.NameValueTable.VALUE, value);
                arg.putInt(CALL_METHOD_USER_KEY, userHandle);
                if (tag != null) {
                    arg.putString(CALL_METHOD_TAG_KEY, tag);
                }
                if (makeDefault) {
                    arg.putBoolean(CALL_METHOD_MAKE_DEFAULT_KEY, true);
                }
                if (overrideableByRestore) {
                    arg.putBoolean(CALL_METHOD_OVERRIDEABLE_BY_RESTORE_KEY, true);
                }
                IContentProvider cp = mProviderHolder.getProvider(cr);
                cp.call(cr.getAttributionSource(),
                        mProviderHolder.mUri.getAuthority(), mCallSetCommand, name, arg);
            } catch (RemoteException e) {
                Log.w(TAG, "Can't set key " + name + " in " + mUri, e);
                return false;
            }
            return true;
        }

        public @SetAllResult int setStringsForPrefix(ContentResolver cr, String prefix,
                HashMap<String, String> keyValues) {
            if (mCallSetAllCommand == null) {
                // This NameValueCache does not support atomically setting multiple flags
                return SET_ALL_RESULT_FAILURE;
            }
            try {
                Bundle args = new Bundle();
                args.putString(CALL_METHOD_PREFIX_KEY, prefix);
                args.putSerializable(CALL_METHOD_FLAGS_KEY, keyValues);
                IContentProvider cp = mProviderHolder.getProvider(cr);
                Bundle bundle = cp.call(cr.getAttributionSource(),
                        mProviderHolder.mUri.getAuthority(),
                        mCallSetAllCommand, null, args);
                return bundle.getInt(KEY_CONFIG_SET_ALL_RETURN);
            } catch (RemoteException e) {
                // Not supported by the remote side
                return SET_ALL_RESULT_FAILURE;
            }
        }

        public boolean deleteStringForUser(ContentResolver cr, String name, final int userHandle) {
            try {
                Bundle arg = new Bundle();
                arg.putInt(CALL_METHOD_USER_KEY, userHandle);
                IContentProvider cp = mProviderHolder.getProvider(cr);
                cp.call(cr.getAttributionSource(),
                        mProviderHolder.mUri.getAuthority(), mCallDeleteCommand, name, arg);
            } catch (RemoteException e) {
                Log.w(TAG, "Can't delete key " + name + " in " + mUri, e);
                return false;
            }
            return true;
        }

        @UnsupportedAppUsage
        public String getStringForUser(ContentResolver cr, String name, final int userHandle) {
            final boolean isSelf = (userHandle == UserHandle.myUserId());
            final boolean useCache = isSelf && !isInSystemServer();
            boolean needsGenerationTracker = false;
            if (useCache) {
                synchronized (NameValueCache.this) {
                    final GenerationTracker generationTracker = mGenerationTrackers.get(name);
                    if (generationTracker != null) {
                        if (generationTracker.isGenerationChanged()) {
                            if (DEBUG) {
                                Log.i(TAG, "Generation changed for setting:" + name
                                        + " type:" + mUri.getPath()
                                        + " in package:" + cr.getPackageName()
                                        + " and user:" + userHandle);
                            }
                            // When a generation number changes, remove cached value, remove the old
                            // generation tracker and request a new one
                            mValues.remove(name);
                            generationTracker.destroy();
                            mGenerationTrackers.remove(name);
                        } else if (mValues.containsKey(name)) {
                            if (DEBUG) {
                                Log.i(TAG, "Cache hit for setting:" + name);
                            }
                            return mValues.get(name);
                        }
                    }
                }
                if (DEBUG) {
                    Log.i(TAG, "Cache miss for setting:" + name + " for user:"
                            + userHandle);
                }
                // Generation tracker doesn't exist or the value isn't cached
                needsGenerationTracker = true;
            } else {
                if (DEBUG || LOCAL_LOGV) {
                    Log.v(TAG, "get setting for user " + userHandle
                            + " by user " + UserHandle.myUserId() + " so skipping cache");
                }
            }

            // Check if the target settings key is readable. Reject if the caller is not system and
            // is trying to access a settings key defined in the Settings.Secure, Settings.System or
            // Settings.Global and is not annotated as @Readable.
            // Notice that a key string that is not defined in any of the Settings.* classes will
            // still be regarded as readable.
            if (!isCallerExemptFromReadableRestriction() && mAllFields.contains(name)) {
                if (!mReadableFields.contains(name)) {
                    throw new SecurityException(
                            "Settings key: <" + name + "> is not readable. From S+, settings keys "
                                    + "annotated with @hide are restricted to system_server and "
                                    + "system apps only, unless they are annotated with @Readable."
                    );
                } else {
                    // When the target settings key has @Readable annotation, if the caller app's
                    // target sdk is higher than the maxTargetSdk of the annotation, reject access.
                    if (mReadableFieldsWithMaxTargetSdk.containsKey(name)) {
                        final int maxTargetSdk = mReadableFieldsWithMaxTargetSdk.get(name);
                        final Application application = ActivityThread.currentApplication();
                        final boolean targetSdkCheckOk = application != null
                                && application.getApplicationInfo() != null
                                && application.getApplicationInfo().targetSdkVersion
                                <= maxTargetSdk;
                        if (!targetSdkCheckOk) {
                            throw new SecurityException(
                                    "Settings key: <" + name + "> is only readable to apps with "
                                            + "targetSdkVersion lower than or equal to: "
                                            + maxTargetSdk
                            );
                        }
                    }
                }
            }

            IContentProvider cp = mProviderHolder.getProvider(cr);

            // Try the fast path first, not using query().  If this
            // fails (alternate Settings provider that doesn't support
            // this interface?) then we fall back to the query/table
            // interface.
            if (mCallGetCommand != null) {
                try {
                    Bundle args = new Bundle();
                    if (!isSelf) {
                        args.putInt(CALL_METHOD_USER_KEY, userHandle);
                    }
                    if (needsGenerationTracker) {
                        args.putString(CALL_METHOD_TRACK_GENERATION_KEY, null);
                        if (DEBUG) {
                            Log.i(TAG, "Requested generation tracker for setting:" + name
                                    + " type:" + mUri.getPath()
                                    + " in package:" + cr.getPackageName()
                                    + " and user:" + userHandle);
                        }
                    }
                    Bundle b;
                    // If we're in system server and in a binder transaction we need to clear the
                    // calling uid. This works around code in system server that did not call
                    // clearCallingIdentity, previously this wasn't needed because reading settings
                    // did not do permission checking but thats no longer the case.
                    // Long term this should be removed and callers should properly call
                    // clearCallingIdentity or use a ContentResolver from the caller as needed.
                    if (Settings.isInSystemServer() && Binder.getCallingUid() != Process.myUid()) {
                        final long token = Binder.clearCallingIdentity();
                        try {
                            b = cp.call(cr.getAttributionSource(),
                                    mProviderHolder.mUri.getAuthority(), mCallGetCommand, name,
                                    args);
                        } finally {
                            Binder.restoreCallingIdentity(token);
                        }
                    } else {
                        b = cp.call(cr.getAttributionSource(),
                                mProviderHolder.mUri.getAuthority(), mCallGetCommand, name, args);
                    }
                    if (b != null) {
                        String value = b.getString(Settings.NameValueTable.VALUE);
                        // Don't update our cache for reads of other users' data
                        if (isSelf) {
                            synchronized (NameValueCache.this) {
                                if (needsGenerationTracker) {
                                    MemoryIntArray array = b.getParcelable(
                                            CALL_METHOD_TRACK_GENERATION_KEY, android.util.MemoryIntArray.class);
                                    final int index = b.getInt(
                                            CALL_METHOD_GENERATION_INDEX_KEY, -1);
                                    if (array != null && index >= 0) {
                                        final int generation = b.getInt(
                                                CALL_METHOD_GENERATION_KEY, 0);
                                        if (DEBUG) {
                                            Log.i(TAG, "Received generation tracker for setting:"
                                                    + name
                                                    + " type:" + mUri.getPath()
                                                    + " in package:" + cr.getPackageName()
                                                    + " and user:" + userHandle
                                                    + " with index:" + index);
                                        }
                                        mGenerationTrackers.put(name, new GenerationTracker(name,
                                                array, index, generation,
                                                mGenerationTrackerErrorHandler));
                                    } else {
                                        maybeCloseGenerationArray(array);
                                    }
                                }
                                if (mGenerationTrackers.get(name) != null
                                        && !mGenerationTrackers.get(name).isGenerationChanged()) {
                                    if (DEBUG) {
                                        Log.i(TAG, "Updating cache for setting:" + name);
                                    }
                                    mValues.put(name, value);
                                }
                            }
                        } else {
                            if (DEBUG || LOCAL_LOGV) {
                                Log.i(TAG, "call-query of user " + userHandle
                                        + " by " + UserHandle.myUserId()
                                        + (isInSystemServer() ? " in system_server" : "")
                                        + " so not updating cache");
                            }
                        }
                        return value;
                    }
                    // If the response Bundle is null, we fall through
                    // to the query interface below.
                } catch (RemoteException e) {
                    // Not supported by the remote side?  Fall through
                    // to query().
                }
            }

            Cursor c = null;
            try {
                Bundle queryArgs = ContentResolver.createSqlQueryBundle(
                        NAME_EQ_PLACEHOLDER, new String[]{name}, null);
                // Same workaround as above.
                if (Settings.isInSystemServer() && Binder.getCallingUid() != Process.myUid()) {
                    final long token = Binder.clearCallingIdentity();
                    try {
                        c = cp.query(cr.getAttributionSource(), mUri,
                                SELECT_VALUE_PROJECTION, queryArgs, null);
                    } finally {
                        Binder.restoreCallingIdentity(token);
                    }
                } else {
                    c = cp.query(cr.getAttributionSource(), mUri,
                            SELECT_VALUE_PROJECTION, queryArgs, null);
                }
                if (c == null) {
                    Log.w(TAG, "Can't get key " + name + " from " + mUri);
                    return null;
                }

                String value = c.moveToNext() ? c.getString(0) : null;
                synchronized (NameValueCache.this) {
                    if (mGenerationTrackers.get(name) != null
                            && !mGenerationTrackers.get(name).isGenerationChanged()) {
                        if (DEBUG) {
                            Log.i(TAG, "Updating cache for setting:" + name + " using query");
                        }
                        mValues.put(name, value);
                    }
                }
                return value;
            } catch (RemoteException e) {
                Log.w(TAG, "Can't get key " + name + " from " + mUri, e);
                return null;  // Return null, but don't cache it.
            } finally {
                if (c != null) c.close();
            }
        }

        private static boolean isCallerExemptFromReadableRestriction() {
            if (Settings.isInSystemServer()) {
                return true;
            }
            if (UserHandle.getAppId(Binder.getCallingUid()) < Process.FIRST_APPLICATION_UID) {
                return true;
            }
            final Application application = ActivityThread.currentApplication();
            if (application == null || application.getApplicationInfo() == null) {
                return false;
            }
            final ApplicationInfo applicationInfo = application.getApplicationInfo();
            final boolean isTestOnly =
                    (applicationInfo.flags & ApplicationInfo.FLAG_TEST_ONLY) != 0;
            return isTestOnly || applicationInfo.isSystemApp() || applicationInfo.isPrivilegedApp()
                    || applicationInfo.isSignedWithPlatformKey();
        }

        private Map<String, String> getStringsForPrefixStripPrefix(
                ContentResolver cr, String prefix, List<String> names) {
            String namespace = prefix.substring(0, prefix.length() - 1);
            ArrayMap<String, String> keyValues = new ArrayMap<>();
            int substringLength = prefix.length();
            int currentGeneration = -1;
            boolean needsGenerationTracker = false;
            synchronized (NameValueCache.this) {
                final GenerationTracker generationTracker = mGenerationTrackers.get(prefix);
                if (generationTracker != null) {
                    if (generationTracker.isGenerationChanged()) {
                        if (DEBUG) {
                            Log.i(TAG, "Generation changed for prefix:" + prefix
                                    + " type:" + mUri.getPath()
                                    + " in package:" + cr.getPackageName());
                        }
                        // When a generation number changes, remove cached values, remove the old
                        // generation tracker and request a new one
                        generationTracker.destroy();
                        mGenerationTrackers.remove(prefix);
                        mPrefixToValues.remove(prefix);
                        needsGenerationTracker = true;
                    } else {
                        final ArrayMap<String, String> cachedSettings = mPrefixToValues.get(prefix);
                        if (cachedSettings != null) {
                            if (!names.isEmpty()) {
                                for (String name : names) {
                                    // The cache can contain "null" values, need to use containsKey.
                                    if (cachedSettings.containsKey(name)) {
                                        keyValues.put(
                                                name,
                                                cachedSettings.get(name));
                                    }
                                }
                            } else {
                                keyValues.putAll(cachedSettings);
                                // Remove the hack added for the legacy behavior.
                                keyValues.remove("");
                            }
                            return keyValues;
                        }
                    }
                    currentGeneration = generationTracker.getCurrentGeneration();
                } else {
                    needsGenerationTracker = true;
                }
            }
            if (mCallListCommand == null) {
                // No list command specified, return empty map
                return keyValues;
            }
            if (DEBUG) {
                Log.i(TAG, "Cache miss for prefix:" + prefix);
            }
            IContentProvider cp = mProviderHolder.getProvider(cr);

            try {
                Bundle args = new Bundle();
                args.putString(Settings.CALL_METHOD_PREFIX_KEY, prefix);
                if (needsGenerationTracker) {
                    args.putString(CALL_METHOD_TRACK_GENERATION_KEY, null);
                    if (DEBUG) {
                        Log.i(TAG, "Requested generation tracker for prefix:" + prefix
                                + " type: " + mUri.getPath()
                                + " in package:" + cr.getPackageName());
                    }
                }

                Bundle b;
                // If we're in system server and the caller did not call
                // clearCallingIdentity, the read would fail due to mismatched AttributionSources.
                if (Settings.isInSystemServer() && Binder.getCallingUid() != Process.myUid()) {
                    final long token = Binder.clearCallingIdentity();
                    try {
                        // Fetch all flags for the namespace at once for caching purposes
                        b = cp.call(cr.getAttributionSource(),
                                mProviderHolder.mUri.getAuthority(), mCallListCommand, null, args);
                    } finally {
                        Binder.restoreCallingIdentity(token);
                    }
                } else {
                    // Fetch all flags for the namespace at once for caching purposes
                    b = cp.call(cr.getAttributionSource(),
                            mProviderHolder.mUri.getAuthority(), mCallListCommand, null, args);
                }
                if (b == null) {
                    // Invalid response, return an empty map
                    return keyValues;
                }

                // All flags for the namespace
                HashMap<String, String> flagsToValues =
                        (HashMap) b.getSerializable(Settings.NameValueTable.VALUE, java.util.HashMap.class);
                if (flagsToValues == null) {
                    return keyValues;
                }
                // Only the flags requested by the caller
                if (!names.isEmpty()) {
                    for (String name : names) {
                        // flagsToValues can contain "null" values, need to use containsKey.
                        final String key = Config.createCompositeName(namespace, name);
                        if (flagsToValues.containsKey(key)) {
                            keyValues.put(
                                    name,
                                    flagsToValues.get(key));
                        }
                    }
                } else {
                    for (Map.Entry<String, String> flag : flagsToValues.entrySet()) {
                        keyValues.put(
                                flag.getKey().substring(substringLength),
                                flag.getValue());
                    }
                }

                synchronized (NameValueCache.this) {
                    if (needsGenerationTracker) {
                        MemoryIntArray array = b.getParcelable(
                                CALL_METHOD_TRACK_GENERATION_KEY, android.util.MemoryIntArray.class);
                        final int index = b.getInt(
                                CALL_METHOD_GENERATION_INDEX_KEY, -1);
                        if (array != null && index >= 0) {
                            final int generation = b.getInt(
                                    CALL_METHOD_GENERATION_KEY, 0);
                            if (DEBUG) {
                                Log.i(TAG, "Received generation tracker for prefix:" + prefix
                                        + " type:" + mUri.getPath()
                                        + " in package:" + cr.getPackageName()
                                        + " with index:" + index);
                            }
                            mGenerationTrackers.put(prefix,
                                    new GenerationTracker(prefix, array, index, generation,
                                            mGenerationTrackerErrorHandler));
                            currentGeneration = generation;
                        } else {
                            maybeCloseGenerationArray(array);
                        }
                    }
                    if (mGenerationTrackers.get(prefix) != null && currentGeneration
                            == mGenerationTrackers.get(prefix).getCurrentGeneration()) {
                        if (DEBUG) {
                            Log.i(TAG, "Updating cache for prefix:" + prefix);
                        }
                        // Cache the complete list of flags for the namespace for bulk queries.
                        // In this cached list, the setting's name doesn't include the prefix.
                        ArrayMap<String, String> namesToValues =
                                new ArrayMap<>(flagsToValues.size() + 1);
                        for (Map.Entry<String, String> flag : flagsToValues.entrySet()) {
                            namesToValues.put(
                                    flag.getKey().substring(substringLength),
                                    flag.getValue());
                        }
                        // Legacy behavior, we return <"", null> when queried with name = ""
                        namesToValues.put("", null);
                        mPrefixToValues.put(prefix, namesToValues);
                    }
                }
                return keyValues;
            } catch (RemoteException e) {
                // Not supported by the remote side, return an empty map
                return keyValues;
            }
        }

        public void clearGenerationTrackerForTest() {
            synchronized (NameValueCache.this) {
                for (int i = 0; i < mGenerationTrackers.size(); i++) {
                    mGenerationTrackers.valueAt(i).destroy();
                }
                mGenerationTrackers.clear();
                mValues.clear();
            }
        }
    }

    /**
     * Checks if the specified context can draw on top of other apps. As of API
     * level 23, an app cannot draw on top of other apps unless it declares the
     * {@link android.Manifest.permission#SYSTEM_ALERT_WINDOW} permission in its
     * manifest, <em>and</em> the user specifically grants the app this
     * capability. To prompt the user to grant this approval, the app must send an
     * intent with the action
     * {@link android.provider.Settings#ACTION_MANAGE_OVERLAY_PERMISSION}, which
     * causes the system to display a permission management screen.
     *
     * @param context App context.
     * @return true if the specified context can draw on top of other apps, false otherwise
     */
    public static boolean canDrawOverlays(Context context) {
        return Settings.isCallingPackageAllowedToDrawOverlays(context, Process.myUid(),
                context.getOpPackageName(), false) || context.checkSelfPermission(
                Manifest.permission.SYSTEM_APPLICATION_OVERLAY)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * This annotation indicates that the value of a setting is allowed to be read
     * with the get* methods. The following settings should be readable:
     * 1) all the public settings
     * 2) all the hidden settings added before S
     */
    @Target({ ElementType.FIELD })
    @Retention(RetentionPolicy.RUNTIME)
    private @interface Readable {
        int maxTargetSdk() default 0;
    }

    private static <T extends NameValueTable> void getPublicSettingsForClass(
            Class<T> callerClass, Set<String> allKeys, Set<String> readableKeys,
            ArrayMap<String, Integer> keysWithMaxTargetSdk) {
        final Field[] allFields = callerClass.getDeclaredFields();
        try {
            for (int i = 0; i < allFields.length; i++) {
                final Field field = allFields[i];
                if (!field.getType().equals(String.class)) {
                    continue;
                }
                final Object value = field.get(callerClass);
                if (!value.getClass().equals(String.class)) {
                    continue;
                }
                allKeys.add((String) value);
                final Readable annotation = field.getAnnotation(Readable.class);

                if (annotation != null) {
                    final String key = (String) value;
                    final int maxTargetSdk = annotation.maxTargetSdk();
                    readableKeys.add(key);
                    if (maxTargetSdk != 0) {
                        keysWithMaxTargetSdk.put(key, maxTargetSdk);
                    }
                }
            }
        } catch (IllegalAccessException ignored) {
        }
    }

    private static float parseFloatSetting(String settingValue, String settingName)
            throws SettingNotFoundException {
        if (settingValue == null) {
            throw new SettingNotFoundException(settingName);
        }
        try {
            return Float.parseFloat(settingValue);
        } catch (NumberFormatException e) {
            throw new SettingNotFoundException(settingName);
        }
    }

    private static float parseFloatSettingWithDefault(String settingValue, float defaultValue) {
        try {
            return settingValue != null ? Float.parseFloat(settingValue) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static int parseIntSetting(String settingValue, String settingName)
            throws SettingNotFoundException {
        if (settingValue == null) {
            throw new SettingNotFoundException(settingName);
        }
        try {
            return Integer.parseInt(settingValue);
        } catch (NumberFormatException e) {
            throw new SettingNotFoundException(settingName);
        }
    }

    private static int parseIntSettingWithDefault(String settingValue, int defaultValue) {
        try {
            return settingValue != null ? Integer.parseInt(settingValue) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static long parseLongSetting(String settingValue, String settingName)
            throws SettingNotFoundException {
        if (settingValue == null) {
            throw new SettingNotFoundException(settingName);
        }
        try {
            return Long.parseLong(settingValue);
        } catch (NumberFormatException e) {
            throw new SettingNotFoundException(settingName);
        }
    }

    private static long parseLongSettingWithDefault(String settingValue, long defaultValue) {
        try {
            return settingValue != null ? Long.parseLong(settingValue) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * System settings, containing miscellaneous system preferences.  This
     * table holds simple name/value pairs.  There are convenience
     * functions for accessing individual settings entries.
     */
    public static final class System extends NameValueTable {
        // NOTE: If you add new settings here, be sure to add them to
        // com.android.providers.settings.SettingsProtoDumpUtil#dumpProtoSystemSettingsLocked.

        private static final float DEFAULT_FONT_SCALE = 1.0f;
        private static final int DEFAULT_FONT_WEIGHT = 0;

        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI =
            Uri.parse("content://" + AUTHORITY + "/system");

        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        private static final ContentProviderHolder sProviderHolder =
                new ContentProviderHolder(CONTENT_URI);

        @UnsupportedAppUsage
        private static final NameValueCache sNameValueCache = new NameValueCache(
                CONTENT_URI,
                CALL_METHOD_GET_SYSTEM,
                CALL_METHOD_PUT_SYSTEM,
                CALL_METHOD_DELETE_SYSTEM,
                sProviderHolder,
                System.class);

        @UnsupportedAppUsage
        private static final HashSet<String> MOVED_TO_SECURE;
        static {
            MOVED_TO_SECURE = new HashSet<>(30);
            MOVED_TO_SECURE.add(Secure.ADAPTIVE_SLEEP);
            MOVED_TO_SECURE.add(Secure.ANDROID_ID);
            MOVED_TO_SECURE.add(Secure.HTTP_PROXY);
            MOVED_TO_SECURE.add(Secure.LOCATION_PROVIDERS_ALLOWED);
            MOVED_TO_SECURE.add(Secure.LOCK_BIOMETRIC_WEAK_FLAGS);
            MOVED_TO_SECURE.add(Secure.LOCK_PATTERN_ENABLED);
            MOVED_TO_SECURE.add(Secure.LOCK_PATTERN_VISIBLE);
            MOVED_TO_SECURE.add(Secure.LOCK_PATTERN_TACTILE_FEEDBACK_ENABLED);
            MOVED_TO_SECURE.add(Secure.LOGGING_ID);
            MOVED_TO_SECURE.add(Secure.PARENTAL_CONTROL_ENABLED);
            MOVED_TO_SECURE.add(Secure.PARENTAL_CONTROL_LAST_UPDATE);
            MOVED_TO_SECURE.add(Secure.PARENTAL_CONTROL_REDIRECT_URL);
            MOVED_TO_SECURE.add(Secure.SETTINGS_CLASSNAME);
            MOVED_TO_SECURE.add(Secure.USE_GOOGLE_MAIL);
            MOVED_TO_SECURE.add(Secure.WIFI_NETWORKS_AVAILABLE_NOTIFICATION_ON);
            MOVED_TO_SECURE.add(Secure.WIFI_NETWORKS_AVAILABLE_REPEAT_DELAY);
            MOVED_TO_SECURE.add(Secure.WIFI_NUM_OPEN_NETWORKS_KEPT);
            MOVED_TO_SECURE.add(Secure.WIFI_ON);
            MOVED_TO_SECURE.add(Secure.WIFI_WATCHDOG_ACCEPTABLE_PACKET_LOSS_PERCENTAGE);
            MOVED_TO_SECURE.add(Secure.WIFI_WATCHDOG_AP_COUNT);
            MOVED_TO_SECURE.add(Secure.WIFI_WATCHDOG_BACKGROUND_CHECK_DELAY_MS);
            MOVED_TO_SECURE.add(Secure.WIFI_WATCHDOG_BACKGROUND_CHECK_ENABLED);
            MOVED_TO_SECURE.add(Secure.WIFI_WATCHDOG_BACKGROUND_CHECK_TIMEOUT_MS);
            MOVED_TO_SECURE.add(Secure.WIFI_WATCHDOG_INITIAL_IGNORED_PING_COUNT);
            MOVED_TO_SECURE.add(Secure.WIFI_WATCHDOG_MAX_AP_CHECKS);
            MOVED_TO_SECURE.add(Secure.WIFI_WATCHDOG_ON);
            MOVED_TO_SECURE.add(Secure.WIFI_WATCHDOG_PING_COUNT);
            MOVED_TO_SECURE.add(Secure.WIFI_WATCHDOG_PING_DELAY_MS);
            MOVED_TO_SECURE.add(Secure.WIFI_WATCHDOG_PING_TIMEOUT_MS);

            // At one time in System, then Global, but now back in Secure
            MOVED_TO_SECURE.add(Secure.INSTALL_NON_MARKET_APPS);
        }

        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        private static final HashSet<String> MOVED_TO_GLOBAL;
        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        private static final HashSet<String> MOVED_TO_SECURE_THEN_GLOBAL;
        static {
            MOVED_TO_GLOBAL = new HashSet<>();
            MOVED_TO_SECURE_THEN_GLOBAL = new HashSet<>();

            // these were originally in system but migrated to secure in the past,
            // so are duplicated in the Secure.* namespace
            MOVED_TO_SECURE_THEN_GLOBAL.add(Global.ADB_ENABLED);
            MOVED_TO_SECURE_THEN_GLOBAL.add(Global.BLUETOOTH_ON);
            MOVED_TO_SECURE_THEN_GLOBAL.add(Global.DATA_ROAMING);
            MOVED_TO_SECURE_THEN_GLOBAL.add(Global.DEVICE_PROVISIONED);
            MOVED_TO_SECURE_THEN_GLOBAL.add(Global.HTTP_PROXY);
            MOVED_TO_SECURE_THEN_GLOBAL.add(Global.NETWORK_PREFERENCE);
            MOVED_TO_SECURE_THEN_GLOBAL.add(Global.USB_MASS_STORAGE_ENABLED);
            MOVED_TO_SECURE_THEN_GLOBAL.add(Global.WIFI_MOBILE_DATA_TRANSITION_WAKELOCK_TIMEOUT_MS);
            MOVED_TO_SECURE_THEN_GLOBAL.add(Global.WIFI_MAX_DHCP_RETRY_COUNT);

            // these are moving directly from system to global
            MOVED_TO_GLOBAL.add(Settings.Global.AIRPLANE_MODE_ON);
            MOVED_TO_GLOBAL.add(Settings.Global.AIRPLANE_MODE_RADIOS);
            MOVED_TO_GLOBAL.add(Settings.Global.AIRPLANE_MODE_TOGGLEABLE_RADIOS);
            MOVED_TO_GLOBAL.add(Settings.Global.AUTO_TIME);
            MOVED_TO_GLOBAL.add(Settings.Global.AUTO_TIME_ZONE);
            MOVED_TO_GLOBAL.add(Settings.Global.CAR_DOCK_SOUND);
            MOVED_TO_GLOBAL.add(Settings.Global.CAR_UNDOCK_SOUND);
            MOVED_TO_GLOBAL.add(Settings.Global.DESK_DOCK_SOUND);
            MOVED_TO_GLOBAL.add(Settings.Global.DESK_UNDOCK_SOUND);
            MOVED_TO_GLOBAL.add(Settings.Global.DOCK_SOUNDS_ENABLED);
            MOVED_TO_GLOBAL.add(Settings.Global.LOCK_SOUND);
            MOVED_TO_GLOBAL.add(Settings.Global.UNLOCK_SOUND);
            MOVED_TO_GLOBAL.add(Settings.Global.LOW_BATTERY_SOUND);
            MOVED_TO_GLOBAL.add(Settings.Global.POWER_SOUNDS_ENABLED);
            MOVED_TO_GLOBAL.add(Settings.Global.STAY_ON_WHILE_PLUGGED_IN);
            MOVED_TO_GLOBAL.add(Settings.Global.WIFI_SLEEP_POLICY);
            MOVED_TO_GLOBAL.add(Settings.Global.MODE_RINGER);
            MOVED_TO_GLOBAL.add(Settings.Global.WINDOW_ANIMATION_SCALE);
            MOVED_TO_GLOBAL.add(Settings.Global.TRANSITION_ANIMATION_SCALE);
            MOVED_TO_GLOBAL.add(Settings.Global.ANIMATOR_DURATION_SCALE);
            MOVED_TO_GLOBAL.add(Settings.Global.FANCY_IME_ANIMATIONS);
            MOVED_TO_GLOBAL.add(Settings.Global.COMPATIBILITY_MODE);
            MOVED_TO_GLOBAL.add(Settings.Global.EMERGENCY_TONE);
            MOVED_TO_GLOBAL.add(Settings.Global.CALL_AUTO_RETRY);
            MOVED_TO_GLOBAL.add(Settings.Global.DEBUG_APP);
            MOVED_TO_GLOBAL.add(Settings.Global.WAIT_FOR_DEBUGGER);
            MOVED_TO_GLOBAL.add(Settings.Global.ALWAYS_FINISH_ACTIVITIES);
            MOVED_TO_GLOBAL.add(Settings.Global.TZINFO_UPDATE_CONTENT_URL);
            MOVED_TO_GLOBAL.add(Settings.Global.TZINFO_UPDATE_METADATA_URL);
            MOVED_TO_GLOBAL.add(Settings.Global.SELINUX_UPDATE_CONTENT_URL);
            MOVED_TO_GLOBAL.add(Settings.Global.SELINUX_UPDATE_METADATA_URL);
            MOVED_TO_GLOBAL.add(Settings.Global.SMS_SHORT_CODES_UPDATE_CONTENT_URL);
            MOVED_TO_GLOBAL.add(Settings.Global.SMS_SHORT_CODES_UPDATE_METADATA_URL);
            MOVED_TO_GLOBAL.add(Settings.Global.CERT_PIN_UPDATE_CONTENT_URL);
            MOVED_TO_GLOBAL.add(Settings.Global.CERT_PIN_UPDATE_METADATA_URL);
            MOVED_TO_GLOBAL.add(Settings.Global.RADIO_NFC);
            MOVED_TO_GLOBAL.add(Settings.Global.RADIO_CELL);
            MOVED_TO_GLOBAL.add(Settings.Global.RADIO_WIFI);
            MOVED_TO_GLOBAL.add(Settings.Global.RADIO_BLUETOOTH);
            MOVED_TO_GLOBAL.add(Settings.Global.RADIO_WIMAX);
            MOVED_TO_GLOBAL.add(Settings.Global.SHOW_PROCESSES);
        }

        /** @hide */
        public static void getMovedToGlobalSettings(Set<String> outKeySet) {
            outKeySet.addAll(MOVED_TO_GLOBAL);
            outKeySet.addAll(MOVED_TO_SECURE_THEN_GLOBAL);
        }

        /** @hide */
        public static void getMovedToSecureSettings(Set<String> outKeySet) {
            outKeySet.addAll(MOVED_TO_SECURE);
        }

        /** @hide */
        public static void getNonLegacyMovedKeys(HashSet<String> outKeySet) {
            outKeySet.addAll(MOVED_TO_GLOBAL);
        }

        /** @hide */
        public static void clearProviderForTest() {
            sProviderHolder.clearProviderForTest();
            sNameValueCache.clearGenerationTrackerForTest();
        }

        /** @hide */
        public static void getPublicSettings(Set<String> allKeys, Set<String> readableKeys,
                ArrayMap<String, Integer> readableKeysWithMaxTargetSdk) {
            getPublicSettingsForClass(System.class, allKeys, readableKeys,
                    readableKeysWithMaxTargetSdk);
        }

        /**
         * Look up a name in the database.
         * @param resolver to access the database with
         * @param name to look up in the table
         * @return the corresponding value, or null if not present
         */
        public static String getString(ContentResolver resolver, String name) {
            return getStringForUser(resolver, name, resolver.getUserId());
        }

        /** @hide */
        @UnsupportedAppUsage
        public static String getStringForUser(ContentResolver resolver, String name,
                int userHandle) {
            if (MOVED_TO_SECURE.contains(name)) {
                Log.w(TAG, "Setting " + name + " has moved from android.provider.Settings.System"
                        + " to android.provider.Settings.Secure, returning read-only value.");
                return Secure.getStringForUser(resolver, name, userHandle);
            }
            if (MOVED_TO_GLOBAL.contains(name) || MOVED_TO_SECURE_THEN_GLOBAL.contains(name)) {
                Log.w(TAG, "Setting " + name + " has moved from android.provider.Settings.System"
                        + " to android.provider.Settings.Global, returning read-only value.");
                return Global.getStringForUser(resolver, name, userHandle);
            }

            return sNameValueCache.getStringForUser(resolver, name, userHandle);
        }

        /**
         * Store a name/value pair into the database.
         * @param resolver to access the database with
         * @param name to store
         * @param value to associate with the name
         * @return true if the value was set, false on database errors
         */
        public static boolean putString(ContentResolver resolver, String name, String value) {
            return putStringForUser(resolver, name, value, resolver.getUserId());
        }

        /**
         * Store a name/value pair into the database. Values written by this method will be
         * overridden if a restore happens in the future.
         *
         * @param resolver to access the database with
         * @param name to store
         * @param value to associate with the name
         *
         * @return true if the value was set, false on database errors
         *
         * @hide
         */
        @RequiresPermission(Manifest.permission.MODIFY_SETTINGS_OVERRIDEABLE_BY_RESTORE)
        @SystemApi
        public static boolean putString(@NonNull ContentResolver resolver,
                @NonNull String name, @Nullable String value, boolean overrideableByRestore) {
            return putStringForUser(resolver, name, value, resolver.getUserId(),
                   overrideableByRestore);
        }

        /**
         * Store a name/value pair into the database.
         *
         * @param resolver to access the database with
         * @param name to store
         * @param value to associate with the name
         * @param makeDefault whether to make the value the default one
         * @param overrideableByRestore whether restore can override this value
         * @return true if the value was set, false on database errors
         *
         * @hide
         */
        @RequiresPermission(Manifest.permission.MODIFY_SETTINGS_OVERRIDEABLE_BY_RESTORE)
        @SystemApi
        @FlaggedApi(Flags.FLAG_SYSTEM_SETTINGS_DEFAULT)
        public static boolean putString(@NonNull ContentResolver resolver, @NonNull String name,
                @Nullable String value, boolean makeDefault, boolean overrideableByRestore) {
            return putStringForUser(resolver, name, value, /* tag= */ null,
                    makeDefault, resolver.getUserId(), overrideableByRestore);
        }

        /** @hide */
        @UnsupportedAppUsage
        public static boolean putStringForUser(ContentResolver resolver, String name, String value,
                int userHandle) {
            return putStringForUser(resolver, name, value, userHandle,
                    DEFAULT_OVERRIDEABLE_BY_RESTORE);
        }

        private static boolean putStringForUser(ContentResolver resolver, String name, String value,
                int userHandle, boolean overrideableByRestore) {
            return putStringForUser(resolver, name, value, /* tag= */ null,
                    /* makeDefault= */ false, userHandle, overrideableByRestore);
        }

        private static boolean putStringForUser(ContentResolver resolver, String name, String value,
                String tag, boolean makeDefault, int userHandle, boolean overrideableByRestore) {
            if (LOCAL_LOGV) {
                Log.v(TAG, "System.putString(name=" + name + ", value=" + value + ") for "
                        + userHandle);
            }
            if (MOVED_TO_SECURE.contains(name)) {
                Log.w(TAG, "Setting " + name + " has moved from android.provider.Settings.System"
                        + " to android.provider.Settings.Secure, value is unchanged.");
                return false;
            }
            if (MOVED_TO_GLOBAL.contains(name) || MOVED_TO_SECURE_THEN_GLOBAL.contains(name)) {
                Log.w(TAG, "Setting " + name + " has moved from android.provider.Settings.System"
                        + " to android.provider.Settings.Global, value is unchanged.");
                return false;
            }
            return sNameValueCache.putStringForUser(resolver, name, value, tag, makeDefault,
                    userHandle, overrideableByRestore);
        }

        /**
         * Reset the settings to their defaults. This would reset <strong>only</strong>
         * settings set by the caller's package. Think of it of a way to undo your own
         * changes to the system settings. Passing in the optional tag will reset only
         * settings changed by your package and associated with this tag.
         *
         * @param resolver Handle to the content resolver.
         * @param tag Optional tag which should be associated with the settings to reset.
         *
         * @see #putString(ContentResolver, String, String, boolean, boolean)
         *
         * @hide
         */
        @SystemApi
        @FlaggedApi(Flags.FLAG_SYSTEM_SETTINGS_DEFAULT)
        public static void resetToDefaults(@NonNull ContentResolver resolver,
                @Nullable String tag) {
            resetToDefaultsAsUser(resolver, tag, RESET_MODE_PACKAGE_DEFAULTS,
                    resolver.getUserId());
        }

        /**
         * Reset the settings to their defaults for a given user with a specific mode. The
         * optional tag argument is valid only for {@link #RESET_MODE_PACKAGE_DEFAULTS}
         * allowing resetting the settings made by a package and associated with the tag.
         *
         * @param resolver Handle to the content resolver.
         * @param tag Optional tag which should be associated with the settings to reset.
         * @param mode The reset mode.
         * @param userHandle The user for which to reset to defaults.
         *
         * @see #RESET_MODE_PACKAGE_DEFAULTS
         * @see #RESET_MODE_UNTRUSTED_DEFAULTS
         * @see #RESET_MODE_UNTRUSTED_CHANGES
         * @see #RESET_MODE_TRUSTED_DEFAULTS
         *
         * @hide
         */
        public static void resetToDefaultsAsUser(@NonNull ContentResolver resolver,
                @Nullable String tag, @ResetMode int mode, @IntRange(from = 0) int userHandle) {
            try {
                Bundle arg = new Bundle();
                arg.putInt(CALL_METHOD_USER_KEY, userHandle);
                if (tag != null) {
                    arg.putString(CALL_METHOD_TAG_KEY, tag);
                }
                arg.putInt(CALL_METHOD_RESET_MODE_KEY, mode);
                IContentProvider cp = sProviderHolder.getProvider(resolver);
                cp.call(resolver.getAttributionSource(),
                        sProviderHolder.mUri.getAuthority(), CALL_METHOD_RESET_SYSTEM, null, arg);
            } catch (RemoteException e) {
                Log.w(TAG, "Can't reset do defaults for " + CONTENT_URI, e);
            }
        }

        /**
         * Construct the content URI for a particular name/value pair,
         * useful for monitoring changes with a ContentObserver.
         * @param name to look up in the table
         * @return the corresponding content URI, or null if not present
         */
        public static Uri getUriFor(String name) {
            if (MOVED_TO_SECURE.contains(name)) {
                Log.w(TAG, "Setting " + name + " has moved from android.provider.Settings.System"
                    + " to android.provider.Settings.Secure, returning Secure URI.");
                return Secure.getUriFor(Secure.CONTENT_URI, name);
            }
            if (MOVED_TO_GLOBAL.contains(name) || MOVED_TO_SECURE_THEN_GLOBAL.contains(name)) {
                Log.w(TAG, "Setting " + name + " has moved from android.provider.Settings.System"
                        + " to android.provider.Settings.Global, returning read-only global URI.");
                return Global.getUriFor(Global.CONTENT_URI, name);
            }
            return getUriFor(CONTENT_URI, name);
        }

        /**
         * Convenience function for retrieving a single system settings value
         * as an integer.  Note that internally setting values are always
         * stored as strings; this function converts the string to an integer
         * for you.  The default value will be returned if the setting is
         * not defined or not an integer.
         *
         * @param cr The ContentResolver to access.
         * @param name The name of the setting to retrieve.
         * @param def Value to return if the setting is not defined.
         *
         * @return The setting's current value, or 'def' if it is not defined
         * or not a valid integer.
         */
        public static int getInt(ContentResolver cr, String name, int def) {
            return getIntForUser(cr, name, def, cr.getUserId());
        }

        /** @hide */
        @UnsupportedAppUsage
        public static int getIntForUser(ContentResolver cr, String name, int def, int userHandle) {
            String v = getStringForUser(cr, name, userHandle);
            return parseIntSettingWithDefault(v, def);
        }

        /**
         * Convenience function for retrieving a single system settings value
         * as an integer.  Note that internally setting values are always
         * stored as strings; this function converts the string to an integer
         * for you.
         * <p>
         * This version does not take a default value.  If the setting has not
         * been set, or the string value is not a number,
         * it throws {@link SettingNotFoundException}.
         *
         * @param cr The ContentResolver to access.
         * @param name The name of the setting to retrieve.
         *
         * @throws SettingNotFoundException Thrown if a setting by the given
         * name can't be found or the setting value is not an integer.
         *
         * @return The setting's current value.
         */
        public static int getInt(ContentResolver cr, String name)
                throws SettingNotFoundException {
            return getIntForUser(cr, name, cr.getUserId());
        }

        /** @hide */
        @UnsupportedAppUsage
        public static int getIntForUser(ContentResolver cr, String name, int userHandle)
                throws SettingNotFoundException {
            String v = getStringForUser(cr, name, userHandle);
            return parseIntSetting(v, name);
        }

        /**
         * Convenience function for updating a single settings value as an
         * integer. This will either create a new entry in the table if the
         * given name does not exist, or modify the value of the existing row
         * with that name.  Note that internally setting values are always
         * stored as strings, so this function converts the given value to a
         * string before storing it.
         *
         * @param cr The ContentResolver to access.
         * @param name The name of the setting to modify.
         * @param value The new value for the setting.
         * @return true if the value was set, false on database errors
         */
        public static boolean putInt(ContentResolver cr, String name, int value) {
            return putIntForUser(cr, name, value, cr.getUserId());
        }

        /** @hide */
        @UnsupportedAppUsage
        public static boolean putIntForUser(ContentResolver cr, String name, int value,
                int userHandle) {
            return putStringForUser(cr, name, Integer.toString(value), userHandle);
        }

        /**
         * Convenience function for retrieving a single system settings value
         * as a {@code long}.  Note that internally setting values are always
         * stored as strings; this function converts the string to a {@code long}
         * for you.  The default value will be returned if the setting is
         * not defined or not a {@code long}.
         *
         * @param cr The ContentResolver to access.
         * @param name The name of the setting to retrieve.
         * @param def Value to return if the setting is not defined.
         *
         * @return The setting's current value, or 'def' if it is not defined
         * or not a valid {@code long}.
         */
        public static long getLong(ContentResolver cr, String name, long def) {
            return getLongForUser(cr, name, def, cr.getUserId());
        }

        /** @hide */
        public static long getLongForUser(ContentResolver cr, String name, long def,
                int userHandle) {
            String v = getStringForUser(cr, name, userHandle);
            return parseLongSettingWithDefault(v, def);
        }

        /**
         * Convenience function for retrieving a single system settings value
         * as a {@code long}.  Note that internally setting values are always
         * stored as strings; this function converts the string to a {@code long}
         * for you.
         * <p>
         * This version does not take a default value.  If the setting has not
         * been set, or the string value is not a number,
         * it throws {@link SettingNotFoundException}.
         *
         * @param cr The ContentResolver to access.
         * @param name The name of the setting to retrieve.
         *
         * @return The setting's current value.
         * @throws SettingNotFoundException Thrown if a setting by the given
         * name can't be found or the setting value is not an integer.
         */
        public static long getLong(ContentResolver cr, String name)
                throws SettingNotFoundException {
            return getLongForUser(cr, name, cr.getUserId());
        }

        /** @hide */
        public static long getLongForUser(ContentResolver cr, String name, int userHandle)
                throws SettingNotFoundException {
            String v = getStringForUser(cr, name, userHandle);
            return parseLongSetting(v, name);
        }

        /**
         * Convenience function for updating a single settings value as a long
         * integer. This will either create a new entry in the table if the
         * given name does not exist, or modify the value of the existing row
         * with that name.  Note that internally setting values are always
         * stored as strings, so this function converts the given value to a
         * string before storing it.
         *
         * @param cr The ContentResolver to access.
         * @param name The name of the setting to modify.
         * @param value The new value for the setting.
         * @return true if the value was set, false on database errors
         */
        public static boolean putLong(ContentResolver cr, String name, long value) {
            return putLongForUser(cr, name, value, cr.getUserId());
        }

        /** @hide */
        public static boolean putLongForUser(ContentResolver cr, String name, long value,
                int userHandle) {
            return putStringForUser(cr, name, Long.toString(value), userHandle);
        }

        /**
         * Convenience function for retrieving a single system settings value
         * as a floating point number.  Note that internally setting values are
         * always stored as strings; this function converts the string to an
         * float for you. The default value will be returned if the setting
         * is not defined or not a valid float.
         *
         * @param cr The ContentResolver to access.
         * @param name The name of the setting to retrieve.
         * @param def Value to return if the setting is not defined.
         *
         * @return The setting's current value, or 'def' if it is not defined
         * or not a valid float.
         */
        public static float getFloat(ContentResolver cr, String name, float def) {
            return getFloatForUser(cr, name, def, cr.getUserId());
        }

        /** @hide */
        public static float getFloatForUser(ContentResolver cr, String name, float def,
                int userHandle) {
            String v = getStringForUser(cr, name, userHandle);
            return parseFloatSettingWithDefault(v, def);
        }

        /**
         * Convenience function for retrieving a single system settings value
         * as a float.  Note that internally setting values are always
         * stored as strings; this function converts the string to a float
         * for you.
         * <p>
         * This version does not take a default value.  If the setting has not
         * been set, or the string value is not a number,
         * it throws {@link SettingNotFoundException}.
         *
         * @param cr The ContentResolver to access.
         * @param name The name of the setting to retrieve.
         *
         * @throws SettingNotFoundException Thrown if a setting by the given
         * name can't be found or the setting value is not a float.
         *
         * @return The setting's current value.
         */
        public static float getFloat(ContentResolver cr, String name)
                throws SettingNotFoundException {
            return getFloatForUser(cr, name, cr.getUserId());
        }

        /** @hide */
        public static float getFloatForUser(ContentResolver cr, String name, int userHandle)
                throws SettingNotFoundException {
            String v = getStringForUser(cr, name, userHandle);
            return parseFloatSetting(v, name);
        }

        /**
         * Convenience function for updating a single settings value as a
         * floating point number. This will either create a new entry in the
         * table if the given name does not exist, or modify the value of the
         * existing row with that name.  Note that internally setting values
         * are always stored as strings, so this function converts the given
         * value to a string before storing it.
         *
         * @param cr The ContentResolver to access.
         * @param name The name of the setting to modify.
         * @param value The new value for the setting.
         * @return true if the value was set, false on database errors
         */
        public static boolean putFloat(ContentResolver cr, String name, float value) {
            return putFloatForUser(cr, name, value, cr.getUserId());
        }

        /** @hide */
        public static boolean putFloatForUser(ContentResolver cr, String name, float value,
                int userHandle) {
            return putStringForUser(cr, name, Float.toString(value), userHandle);
        }

        /**
         * Convenience function to read all of the current
         * configuration-related settings into a
         * {@link Configuration} object.
         *
         * @param cr The ContentResolver to access.
         * @param outConfig Where to place the configuration settings.
         */
        public static void getConfiguration(ContentResolver cr, Configuration outConfig) {
            adjustConfigurationForUser(cr, outConfig, cr.getUserId(),
                    false /* updateSettingsIfEmpty */);
        }

        /** @hide */
        public static void adjustConfigurationForUser(ContentResolver cr, Configuration outConfig,
                int userHandle, boolean updateSettingsIfEmpty) {
            final float defaultFontScale = getDefaultFontScale(cr, userHandle);
            outConfig.fontScale = Settings.System.getFloatForUser(
                    cr, FONT_SCALE, defaultFontScale, userHandle);
            if (outConfig.fontScale < 0) {
                outConfig.fontScale = defaultFontScale;
            }
            outConfig.fontWeightAdjustment = Settings.Secure.getIntForUser(
                    cr, Settings.Secure.FONT_WEIGHT_ADJUSTMENT, DEFAULT_FONT_WEIGHT, userHandle);

            final String localeValue =
                    Settings.System.getStringForUser(cr, SYSTEM_LOCALES, userHandle);
            if (localeValue != null) {
                outConfig.setLocales(LocaleList.forLanguageTags(localeValue));
            } else {
                // Do not update configuration with emtpy settings since we need to take over the
                // locale list of previous user if the settings value is empty. This happens when a
                // new user is created.

                if (updateSettingsIfEmpty) {
                    // Make current configuration persistent. This is necessary the first time a
                    // user log in. At the first login, the configuration settings are empty, so we
                    // need to store the adjusted configuration as the initial settings.
                    Settings.System.putStringForUser(
                            cr, SYSTEM_LOCALES, outConfig.getLocales().toLanguageTags(),
                            userHandle, DEFAULT_OVERRIDEABLE_BY_RESTORE);
                }
            }
        }

        private static float getDefaultFontScale(ContentResolver cr, int userHandle) {
            return com.android.window.flags.Flags.configurableFontScaleDefault()
                    ? Settings.System.getFloatForUser(cr, DEFAULT_DEVICE_FONT_SCALE,
                    DEFAULT_FONT_SCALE, userHandle) : DEFAULT_FONT_SCALE;
        }

        /**
         * @hide Erase the fields in the Configuration that should be applied
         * by the settings.
         */
        public static void clearConfiguration(Configuration inoutConfig) {
            inoutConfig.fontScale = 0;
            if (!inoutConfig.userSetLocale && !inoutConfig.getLocales().isEmpty()) {
                inoutConfig.clearLocales();
            }
            inoutConfig.fontWeightAdjustment = Configuration.FONT_WEIGHT_ADJUSTMENT_UNDEFINED;
        }

        /**
         * Convenience function to write a batch of configuration-related
         * settings from a {@link Configuration} object.
         *
         * @param cr The ContentResolver to access.
         * @param config The settings to write.
         * @return true if the values were set, false on database errors
         */
        public static boolean putConfiguration(ContentResolver cr, Configuration config) {
            return putConfigurationForUser(cr, config, cr.getUserId());
        }

        /** @hide */
        public static boolean putConfigurationForUser(ContentResolver cr, Configuration config,
                int userHandle) {
            return Settings.System.putFloatForUser(cr, FONT_SCALE, config.fontScale, userHandle) &&
                    Settings.System.putStringForUser(
                            cr, SYSTEM_LOCALES, config.getLocales().toLanguageTags(), userHandle,
                            DEFAULT_OVERRIDEABLE_BY_RESTORE);
        }

        /**
         * Convenience function for checking if settings should be overwritten with config changes.
         * @see #putConfigurationForUser(ContentResolver, Configuration, int)
         * @hide
         */
        public static boolean hasInterestingConfigurationChanges(int changes) {
            return (changes & ActivityInfo.CONFIG_FONT_SCALE) != 0 ||
                    (changes & ActivityInfo.CONFIG_LOCALE) != 0;
        }

        /** @deprecated - Do not use */
        @Deprecated
        public static boolean getShowGTalkServiceStatus(ContentResolver cr) {
            return getShowGTalkServiceStatusForUser(cr, cr.getUserId());
        }

        /**
         * @hide
         * @deprecated - Do not use
         */
        @Deprecated
        public static boolean getShowGTalkServiceStatusForUser(ContentResolver cr,
                int userHandle) {
            return getIntForUser(cr, SHOW_GTALK_SERVICE_STATUS, 0, userHandle) != 0;
        }

        /** @deprecated - Do not use */
        @Deprecated
        public static void setShowGTalkServiceStatus(ContentResolver cr, boolean flag) {
            setShowGTalkServiceStatusForUser(cr, flag, cr.getUserId());
        }

        /**
         * @hide
         * @deprecated - Do not use
         */
        @Deprecated
        public static void setShowGTalkServiceStatusForUser(ContentResolver cr, boolean flag,
                int userHandle) {
            putIntForUser(cr, SHOW_GTALK_SERVICE_STATUS, flag ? 1 : 0, userHandle);
        }

        /**
         * @deprecated Use {@link android.provider.Settings.Global#STAY_ON_WHILE_PLUGGED_IN} instead
         */
        @Deprecated
        public static final String STAY_ON_WHILE_PLUGGED_IN = Global.STAY_ON_WHILE_PLUGGED_IN;

        /**
         * What happens when the user presses the end call button if they're not
         * on a call.<br/>
         * <b>Values:</b><br/>
         * 0 - The end button does nothing.<br/>
         * 1 - The end button goes to the home screen.<br/>
         * 2 - The end button puts the device to sleep and locks the keyguard.<br/>
         * 3 - The end button goes to the home screen.  If the user is already on the
         * home screen, it puts the device to sleep.
         */
        @Readable
        public static final String END_BUTTON_BEHAVIOR = "end_button_behavior";

        /**
         * END_BUTTON_BEHAVIOR value for "go home".
         * @hide
         */
        public static final int END_BUTTON_BEHAVIOR_HOME = 0x1;

        /**
         * END_BUTTON_BEHAVIOR value for "go to sleep".
         * @hide
         */
        public static final int END_BUTTON_BEHAVIOR_SLEEP = 0x2;

        /**
         * END_BUTTON_BEHAVIOR default value.
         * @hide
         */
        public static final int END_BUTTON_BEHAVIOR_DEFAULT = END_BUTTON_BEHAVIOR_SLEEP;

        /**
         * Is advanced settings mode turned on. 0 == no, 1 == yes
         * @hide
         */
        @Readable
        public static final String ADVANCED_SETTINGS = "advanced_settings";

        /**
         * ADVANCED_SETTINGS default value.
         * @hide
         */
        public static final int ADVANCED_SETTINGS_DEFAULT = 0;

        /**
         * If the triple press gesture for toggling accessibility is enabled.
         * Set to 1 for true and 0 for false.
         *
         * This setting is used only internally.
         * @hide
         */
        public static final String WEAR_ACCESSIBILITY_GESTURE_ENABLED
                = "wear_accessibility_gesture_enabled";

        /**
         * If the triple press gesture for toggling accessibility is enabled during OOBE.
         * Set to 1 for true and 0 for false.
         *
         * This setting is used only internally.
         * @hide
         */
        public static final String WEAR_ACCESSIBILITY_GESTURE_ENABLED_DURING_OOBE =
                "wear_accessibility_gesture_enabled_during_oobe";


        /**
         * If the text-to-speech pre-warm is enabled.
         * Set to 1 for true and 0 for false.
         *
         * This setting is used only internally.
         * @hide
         */
        public static final String WEAR_TTS_PREWARM_ENABLED = "wear_tts_prewarm_enabled";

        /**
         * @deprecated Use {@link android.provider.Settings.Global#AIRPLANE_MODE_ON} instead
         */
        @Deprecated
        public static final String AIRPLANE_MODE_ON = Global.AIRPLANE_MODE_ON;

        /**
         * @deprecated Use {@link android.provider.Settings.Global#RADIO_BLUETOOTH} instead
         */
        @Deprecated
        public static final String RADIO_BLUETOOTH = Global.RADIO_BLUETOOTH;

        /**
         * @deprecated Use {@link android.provider.Settings.Global#RADIO_WIFI} instead
         */
        @Deprecated
        public static final String RADIO_WIFI = Global.RADIO_WIFI;

        /**
         * @deprecated Use {@link android.provider.Settings.Global#RADIO_WIMAX} instead
         * {@hide}
         */
        @Deprecated
        public static final String RADIO_WIMAX = Global.RADIO_WIMAX;

        /**
         * @deprecated Use {@link android.provider.Settings.Global#RADIO_CELL} instead
         */
        @Deprecated
        public static final String RADIO_CELL = Global.RADIO_CELL;

        /**
         * @deprecated Use {@link android.provider.Settings.Global#RADIO_NFC} instead
         */
        @Deprecated
        public static final String RADIO_NFC = Global.RADIO_NFC;

        /**
         * @deprecated Use {@link android.provider.Settings.Global#AIRPLANE_MODE_RADIOS} instead
         */
        @Deprecated
        public static final String AIRPLANE_MODE_RADIOS = Global.AIRPLANE_MODE_RADIOS;

        /**
         * @deprecated Use {@link android.provider.Settings.Global#AIRPLANE_MODE_TOGGLEABLE_RADIOS} instead
         *
         * {@hide}
         */
        @Deprecated
        @UnsupportedAppUsage
        public static final String AIRPLANE_MODE_TOGGLEABLE_RADIOS =
                Global.AIRPLANE_MODE_TOGGLEABLE_RADIOS;

        /**
         * @deprecated Use {@link android.provider.Settings.Global#WIFI_SLEEP_POLICY} instead
         */
        @Deprecated
        public static final String WIFI_SLEEP_POLICY = Global.WIFI_SLEEP_POLICY;

        /**
         * @deprecated Use {@link android.provider.Settings.Global#WIFI_SLEEP_POLICY_DEFAULT} instead
         */
        @Deprecated
        public static final int WIFI_SLEEP_POLICY_DEFAULT = Global.WIFI_SLEEP_POLICY_DEFAULT;

        /**
         * @deprecated Use {@link android.provider.Settings.Global#WIFI_SLEEP_POLICY_NEVER_WHILE_PLUGGED} instead
         */
        @Deprecated
        public static final int WIFI_SLEEP_POLICY_NEVER_WHILE_PLUGGED =
                Global.WIFI_SLEEP_POLICY_NEVER_WHILE_PLUGGED;

        /**
         * @deprecated Use {@link android.provider.Settings.Global#WIFI_SLEEP_POLICY_NEVER} instead
         */
        @Deprecated
        public static final int WIFI_SLEEP_POLICY_NEVER = Global.WIFI_SLEEP_POLICY_NEVER;

        /**
         * @deprecated Use {@link android.provider.Settings.Global#MODE_RINGER} instead
         */
        @Deprecated
        public static final String MODE_RINGER = Global.MODE_RINGER;

        /**
         * Whether to use static IP and other static network attributes.
         * <p>
         * Set to 1 for true and 0 for false.
         *
         * @deprecated Use {@link WifiManager} instead
         */
        @Deprecated
        @Readable
        public static final String WIFI_USE_STATIC_IP = "wifi_use_static_ip";

        /**
         * The static IP address.
         * <p>
         * Example: "192.168.1.51"
         *
         * @deprecated Use {@link WifiManager} instead
         */
        @Deprecated
        @Readable
        public static final String WIFI_STATIC_IP = "wifi_static_ip";

        /**
         * If using static IP, the gateway's IP address.
         * <p>
         * Example: "192.168.1.1"
         *
         * @deprecated Use {@link WifiManager} instead
         */
        @Deprecated
        @Readable
        public static final String WIFI_STATIC_GATEWAY = "wifi_static_gateway";

        /**
         * If using static IP, the net mask.
         * <p>
         * Example: "255.255.255.0"
         *
         * @deprecated Use {@link WifiManager} instead
         */
        @Deprecated
        @Readable
        public static final String WIFI_STATIC_NETMASK = "wifi_static_netmask";

        /**
         * If using static IP, the primary DNS's IP address.
         * <p>
         * Example: "192.168.1.1"
         *
         * @deprecated Use {@link WifiManager} instead
         */
        @Deprecated
        @Readable
        public static final String WIFI_STATIC_DNS1 = "wifi_static_dns1";

        /**
         * If using static IP, the secondary DNS's IP address.
         * <p>
         * Example: "192.168.1.2"
         *
         * @deprecated Use {@link WifiManager} instead
         */
        @Deprecated
        @Readable
        public static final String WIFI_STATIC_DNS2 = "wifi_static_dns2";

        /**
         * Determines whether remote devices may discover and/or connect to
         * this device.
         * <P>Type: INT</P>
         * 2 -- discoverable and connectable
         * 1 -- connectable but not discoverable
         * 0 -- neither connectable nor discoverable
         */
        @Readable
        public static final String BLUETOOTH_DISCOVERABILITY =
            "bluetooth_discoverability";

        /**
         * Bluetooth discoverability timeout.  If this value is nonzero, then
         * Bluetooth becomes discoverable for a certain number of seconds,
         * after which is becomes simply connectable.  The value is in seconds.
         */
        @Readable
        public static final String BLUETOOTH_DISCOVERABILITY_TIMEOUT =
            "bluetooth_discoverability_timeout";

        /**
         * @deprecated Use {@link android.provider.Settings.Secure#LOCK_PATTERN_ENABLED}
         * instead
         */
        @Deprecated
        public static final String LOCK_PATTERN_ENABLED = Secure.LOCK_PATTERN_ENABLED;

        /**
         * @deprecated Use {@link android.provider.Settings.Secure#LOCK_PATTERN_VISIBLE}
         * instead
         */
        @Deprecated
        public static final String LOCK_PATTERN_VISIBLE = "lock_pattern_visible_pattern";

        /**
         * @deprecated Use
         * {@link android.provider.Settings.Secure#LOCK_PATTERN_TACTILE_FEEDBACK_ENABLED}
         * instead
         */
        @Deprecated
        public static final String LOCK_PATTERN_TACTILE_FEEDBACK_ENABLED =
            "lock_pattern_tactile_feedback_enabled";

        /**
         * A formatted string of the next alarm that is set, or the empty string
         * if there is no alarm set.
         *
         * @deprecated Use {@link android.app.AlarmManager#getNextAlarmClock()}.
         */
        @Deprecated
        @Readable
        public static final String NEXT_ALARM_FORMATTED = "next_alarm_formatted";

        /**
         * Scaling factor for fonts, float.
         */
        @Readable
        public static final String FONT_SCALE = "font_scale";

        /**
         * Default scaling factor for fonts for the specific device, float.
         * The value is read from the {@link R.dimen.def_device_font_scale}
         * configuration property.
         *
         * @hide
         */
        @Readable
        public static final String DEFAULT_DEVICE_FONT_SCALE = "device_font_scale";

        /**
         * The serialized system locale value.
         *
         * Do not use this value directory.
         * To get system locale, use {@link LocaleList#getDefault} instead.
         * To update system locale, use {@link com.android.internal.app.LocalePicker#updateLocales}
         * instead.
         * @hide
         */
        @Readable
        public static final String SYSTEM_LOCALES = "system_locales";


        /**
         * Name of an application package to be debugged.
         *
         * @deprecated Use {@link Global#DEBUG_APP} instead
         */
        @Deprecated
        public static final String DEBUG_APP = Global.DEBUG_APP;

        /**
         * If 1, when launching DEBUG_APP it will wait for the debugger before
         * starting user code.  If 0, it will run normally.
         *
         * @deprecated Use {@link Global#WAIT_FOR_DEBUGGER} instead
         */
        @Deprecated
        public static final String WAIT_FOR_DEBUGGER = Global.WAIT_FOR_DEBUGGER;

        /**
         * Whether or not to dim the screen. 0=no  1=yes
         * @deprecated This setting is no longer used.
         */
        @Deprecated
        @Readable
        public static final String DIM_SCREEN = "dim_screen";

        /**
         * The display color mode.
         * @hide
         */
        @Readable
        public static final String DISPLAY_COLOR_MODE = "display_color_mode";

        /**
         * Hint to decide whether restored vendor color modes are compatible with the new device. If
         * unset or a match is not made, only the standard color modes will be restored.
         * @hide
         */
        public static final String DISPLAY_COLOR_MODE_VENDOR_HINT =
                "display_color_mode_vendor_hint";

        /**
         * The user selected min refresh rate in frames per second. If infinite, the user wants
         * the highest possible refresh rate.
         *
         * If this isn't set, 0 will be used.
         * @hide
         */
        @Readable
        public static final String MIN_REFRESH_RATE = "min_refresh_rate";

        /**
         * The user selected peak refresh rate in frames per second. If infinite, the user wants
         * the highest possible refresh rate.
         *
         * If this isn't set, the system falls back to a device specific default.
         * @hide
         */
        @Readable
        public static final String PEAK_REFRESH_RATE = "peak_refresh_rate";

        /**
         * Control lock behavior on fold
         *
         * If this isn't set, the system falls back to a device specific default.
         * @hide
         */
        @Readable
        public static final String FOLD_LOCK_BEHAVIOR = "fold_lock_behavior_setting";

        /**
         * The amount of time in milliseconds before the device goes to sleep or begins
         * to dream after a period of inactivity.  This value is also known as the
         * user activity timeout period since the screen isn't necessarily turned off
         * when it expires.
         *
         * <p>
         * This value is bounded by maximum timeout set by
         * {@link android.app.admin.DevicePolicyManager#setMaximumTimeToLock(ComponentName, long)}.
         */
        @Readable
        public static final String SCREEN_OFF_TIMEOUT = "screen_off_timeout";

        /**
         * The screen backlight brightness between 0 and 255.
         */
        @Readable
        public static final String SCREEN_BRIGHTNESS = "screen_brightness";

        /**
         * The screen backlight brightness between 0.0f and 1.0f.
         * @hide
         */
        @Readable
        public static final String SCREEN_BRIGHTNESS_FLOAT = "screen_brightness_float";

        /**
         * Control whether to enable automatic brightness mode.
         */
        @Readable
        public static final String SCREEN_BRIGHTNESS_MODE = "screen_brightness_mode";

        /**
         * Adjustment to auto-brightness to make it generally more (>0.0 <1.0)
         * or less (<0.0 >-1.0) bright.
         * @hide
         */
        @UnsupportedAppUsage
        @Readable
        public static final String SCREEN_AUTO_BRIGHTNESS_ADJ = "screen_auto_brightness_adj";

        /**
         * SCREEN_BRIGHTNESS_MODE value for manual mode.
         */
        public static final int SCREEN_BRIGHTNESS_MODE_MANUAL = 0;

        /**
         * SCREEN_BRIGHTNESS_MODE value for automatic mode.
         */
        public static final int SCREEN_BRIGHTNESS_MODE_AUTOMATIC = 1;

        /**
         * Control whether to enable adaptive sleep mode.
         * @deprecated Use {@link android.provider.Settings.Secure#ADAPTIVE_SLEEP} instead.
         * @hide
         */
        @Deprecated
        @Readable
        public static final String ADAPTIVE_SLEEP = "adaptive_sleep";

        /**
         * Control whether the process CPU usage meter should be shown.
         *
         * @deprecated This functionality is no longer available as of
         * {@link android.os.Build.VERSION_CODES#N_MR1}.
         */
        @Deprecated
        public static final String SHOW_PROCESSES = Global.SHOW_PROCESSES;

        /**
         * If 1, the activity manager will aggressively finish activities and
         * processes as soon as they are no longer needed.  If 0, the normal
         * extended lifetime is used.
         *
         * @deprecated Use {@link Global#ALWAYS_FINISH_ACTIVITIES} instead
         */
        @Deprecated
        public static final String ALWAYS_FINISH_ACTIVITIES = Global.ALWAYS_FINISH_ACTIVITIES;

        /**
         * Determines which streams are affected by ringer and zen mode changes. The
         * stream type's bit should be set to 1 if it should be muted when going
         * into an inaudible ringer mode.
         */
        @Readable
        public static final String MODE_RINGER_STREAMS_AFFECTED = "mode_ringer_streams_affected";

        /**
          * Determines which streams are affected by mute. The
          * stream type's bit should be set to 1 if it should be muted when a mute request
          * is received.
          */
        @Readable
        public static final String MUTE_STREAMS_AFFECTED = "mute_streams_affected";

        /**
         * Whether vibrate is on for different events. This is used internally,
         * changing this value will not change the vibrate. See AudioManager.
         */
        @Readable
        public static final String VIBRATE_ON = "vibrate_on";

        /**
         * Whether applying ramping ringer on incoming phone call ringtone.
         * <p>1 = apply ramping ringer
         * <p>0 = do not apply ramping ringer
         * @hide
         */
        @Readable
        public static final String APPLY_RAMPING_RINGER = "apply_ramping_ringer";

        /**
         * If 1, redirects the system vibrator to all currently attached input devices
         * that support vibration.  If there are no such input devices, then the system
         * vibrator is used instead.
         * If 0, does not register the system vibrator.
         *
         * This setting is mainly intended to provide a compatibility mechanism for
         * applications that only know about the system vibrator and do not use the
         * input device vibrator API.
         *
         * @hide
         */
        @Readable
        public static final String VIBRATE_INPUT_DEVICES = "vibrate_input_devices";

        /**
         * The intensity of alarm vibrations, if configurable.
         *
         * Not all devices are capable of changing their vibration intensity; on these devices
         * there will likely be no difference between the various vibration intensities except for
         * intensity 0 (off) and the rest.
         *
         * <b>Values:</b><br/>
         * 0 - Vibration is disabled<br/>
         * 1 - Weak vibrations<br/>
         * 2 - Medium vibrations<br/>
         * 3 - Strong vibrations
         * @hide
         */
        public static final String ALARM_VIBRATION_INTENSITY =
                "alarm_vibration_intensity";

        /**
         * The intensity of media vibrations, if configurable.
         *
         * This includes any vibration that is part of media, such as music, movie, soundtrack,
         * game or animations.
         *
         * Not all devices are capable of changing their vibration intensity; on these devices
         * there will likely be no difference between the various vibration intensities except for
         * intensity 0 (off) and the rest.
         *
         * <b>Values:</b><br/>
         * 0 - Vibration is disabled<br/>
         * 1 - Weak vibrations<br/>
         * 2 - Medium vibrations<br/>
         * 3 - Strong vibrations
         * @hide
         */
        public static final String MEDIA_VIBRATION_INTENSITY =
                "media_vibration_intensity";

        /**
         * The intensity of notification vibrations, if configurable.
         *
         * Not all devices are capable of changing their vibration intensity; on these devices
         * there will likely be no difference between the various vibration intensities except for
         * intensity 0 (off) and the rest.
         *
         * <b>Values:</b><br/>
         * 0 - Vibration is disabled<br/>
         * 1 - Weak vibrations<br/>
         * 2 - Medium vibrations<br/>
         * 3 - Strong vibrations
         * @hide
         */
        @Readable
        public static final String NOTIFICATION_VIBRATION_INTENSITY =
                "notification_vibration_intensity";

        /**
         * The intensity of ringtone vibrations, if configurable.
         *
         * Not all devices are capable of changing their vibration intensity; on these devices
         * there will likely be no difference between the various vibration intensities except for
         * intensity 0 (off) and the rest.
         *
         * <b>Values:</b><br/>
         * 0 - Vibration is disabled<br/>
         * 1 - Weak vibrations<br/>
         * 2 - Medium vibrations<br/>
         * 3 - Strong vibrations
         * @hide
         */
        @Readable
        public static final String RING_VIBRATION_INTENSITY =
                "ring_vibration_intensity";

        /**
         * The intensity of haptic feedback vibrations, if configurable.
         *
         * Not all devices are capable of changing their feedback intensity; on these devices
         * there will likely be no difference between the various vibration intensities except for
         * intensity 0 (off) and the rest.
         *
         * <b>Values:</b><br/>
         * 0 - Vibration is disabled<br/>
         * 1 - Weak vibrations<br/>
         * 2 - Medium vibrations<br/>
         * 3 - Strong vibrations
         * @hide
         */
        @Readable
        public static final String HAPTIC_FEEDBACK_INTENSITY =
                "haptic_feedback_intensity";

        /**
         * The intensity of haptic feedback vibrations for interaction with hardware components from
         * the device, like buttons and sensors, if configurable.
         *
         * Not all devices are capable of changing their feedback intensity; on these devices
         * there will likely be no difference between the various vibration intensities except for
         * intensity 0 (off) and the rest.
         *
         * <b>Values:</b><br/>
         * 0 - Vibration is disabled<br/>
         * 1 - Weak vibrations<br/>
         * 2 - Medium vibrations<br/>
         * 3 - Strong vibrations
         * @hide
         */
        public static final String HARDWARE_HAPTIC_FEEDBACK_INTENSITY =
                "hardware_haptic_feedback_intensity";

        /**
         * Whether keyboard vibration feedback is enabled. The value is boolean (1 or 0).
         *
         * @hide
         */
        @Readable
        public static final String KEYBOARD_VIBRATION_ENABLED = "keyboard_vibration_enabled";

        /**
         * Ringer volume. This is used internally, changing this value will not
         * change the volume. See AudioManager.
         *
         * @removed Not used by anything since API 2.
         */
        @Readable
        public static final String VOLUME_RING = "volume_ring";

        /**
         * System/notifications volume. This is used internally, changing this
         * value will not change the volume. See AudioManager.
         *
         * @removed Not used by anything since API 2.
         */
        @Readable
        public static final String VOLUME_SYSTEM = "volume_system";

        /**
         * Voice call volume. This is used internally, changing this value will
         * not change the volume. See AudioManager.
         *
         * @removed Not used by anything since API 2.
         */
        @Readable
        public static final String VOLUME_VOICE = "volume_voice";

        /**
         * Music/media/gaming volume. This is used internally, changing this
         * value will not change the volume. See AudioManager.
         *
         * @removed Not used by anything since API 2.
         */
        @Readable
        public static final String VOLUME_MUSIC = "volume_music";

        /**
         * Alarm volume. This is used internally, changing this
         * value will not change the volume. See AudioManager.
         *
         * @removed Not used by anything since API 2.
         */
        @Readable
        public static final String VOLUME_ALARM = "volume_alarm";

        /**
         * Notification volume. This is used internally, changing this
         * value will not change the volume. See AudioManager.
         *
         * @removed Not used by anything since API 2.
         */
        @Readable
        public static final String VOLUME_NOTIFICATION = "volume_notification";

        /**
         * Bluetooth Headset volume. This is used internally, changing this value will
         * not change the volume. See AudioManager.
         *
         * @removed Not used by anything since API 2.
         */
        @Readable
        public static final String VOLUME_BLUETOOTH_SCO = "volume_bluetooth_sco";

        /**
         * @hide
         * Acessibility volume. This is used internally, changing this
         * value will not change the volume.
         */
        @Readable
        public static final String VOLUME_ACCESSIBILITY = "volume_a11y";

        /**
         * @hide
         * Volume index for virtual assistant.
         */
        @Readable
        public static final String VOLUME_ASSISTANT = "volume_assistant";

        /**
         * Master volume (float in the range 0.0f to 1.0f).
         *
         * @hide
         */
        @Readable
        public static final String VOLUME_MASTER = "volume_master";

        /**
         * Master mono (int 1 = mono, 0 = normal).
         *
         * @hide
         */
        @UnsupportedAppUsage
        @Readable
        public static final String MASTER_MONO = "master_mono";

        /**
         * Master balance (float -1.f = 100% left, 0.f = dead center, 1.f = 100% right).
         *
         * @hide
         */
        @Readable
        public static final String MASTER_BALANCE = "master_balance";

        /**
         * Whether the notifications should use the ring volume (value of 1) or
         * a separate notification volume (value of 0). In most cases, users
         * will have this enabled so the notification and ringer volumes will be
         * the same. However, power users can disable this and use the separate
         * notification volume control.
         * <p>
         * Note: This is a one-off setting that will be removed in the future
         * when there is profile support. For this reason, it is kept hidden
         * from the public APIs.
         *
         * @hide
         * @deprecated
         */
        @Deprecated
        @Readable
        public static final String NOTIFICATIONS_USE_RING_VOLUME =
            "notifications_use_ring_volume";

        /**
         * Whether silent mode should allow vibration feedback. This is used
         * internally in AudioService and the Sound settings activity to
         * coordinate decoupling of vibrate and silent modes. This setting
         * will likely be removed in a future release with support for
         * audio/vibe feedback profiles.
         *
         * Not used anymore. On devices with vibrator, the user explicitly selects
         * silent or vibrate mode.
         * Kept for use by legacy database upgrade code in DatabaseHelper.
         * @hide
         */
        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        @Readable
        public static final String VIBRATE_IN_SILENT = "vibrate_in_silent";

        /**
         * The mapping of stream type (integer) to its setting.
         *
         * @removed  Not used by anything since API 2.
         */
        public static final String[] VOLUME_SETTINGS = {
            VOLUME_VOICE, VOLUME_SYSTEM, VOLUME_RING, VOLUME_MUSIC,
            VOLUME_ALARM, VOLUME_NOTIFICATION, VOLUME_BLUETOOTH_SCO
        };

        /**
         * @hide
         * The mapping of stream type (integer) to its setting.
         * Unlike the VOLUME_SETTINGS array, this one contains as many entries as
         * AudioSystem.NUM_STREAM_TYPES, and has empty strings for stream types whose volumes
         * are never persisted.
         */
        public static final String[] VOLUME_SETTINGS_INT = {
                VOLUME_VOICE, VOLUME_SYSTEM, VOLUME_RING, VOLUME_MUSIC,
                VOLUME_ALARM, VOLUME_NOTIFICATION, VOLUME_BLUETOOTH_SCO,
                "" /*STREAM_SYSTEM_ENFORCED, no setting for this stream*/,
                "" /*STREAM_DTMF, no setting for this stream*/,
                "" /*STREAM_TTS, no setting for this stream*/,
                VOLUME_ACCESSIBILITY, VOLUME_ASSISTANT
            };

        /**
         * Appended to various volume related settings to record the previous
         * values before they the settings were affected by a silent/vibrate
         * ringer mode change.
         *
         * @removed  Not used by anything since API 2.
         */
        @Readable
        public static final String APPEND_FOR_LAST_AUDIBLE = "_last_audible";

        /**
         * Persistent store for the system-wide default ringtone URI.
         * <p>
         * If you need to play the default ringtone at any given time, it is recommended
         * you give {@link #DEFAULT_RINGTONE_URI} to the media player.  It will resolve
         * to the set default ringtone at the time of playing.
         *
         * @see #DEFAULT_RINGTONE_URI
         */
        @Readable
        public static final String RINGTONE = "ringtone";

        /**
         * Persistent store for the system-wide default ringtone for Slot2 URI.
         *
         * @see #RINGTONE
         * @see #DEFAULT_RINGTONE2_URI
         *
         */
        /** {@hide} */
        public static final String RINGTONE2 = "ringtone2";

        /**
         * A {@link Uri} that will point to the current default ringtone at any
         * given time.
         * <p>
         * If the current default ringtone is in the DRM provider and the caller
         * does not have permission, the exception will be a
         * FileNotFoundException.
         */
        public static final Uri DEFAULT_RINGTONE_URI = getUriFor(RINGTONE);

        /**
         * A {@link Uri} that will point to the current default ringtone for Slot2
         * at any given time.
         *
         * @see #DEFAULT_RINGTONE_URI
         *
         */
        /** {@hide} */
        public static final Uri DEFAULT_RINGTONE2_URI = getUriFor(RINGTONE2);

        /** {@hide} */
        public static final String RINGTONE_CACHE = "ringtone_cache";
        /** {@hide} */
        public static final Uri RINGTONE_CACHE_URI = getUriFor(RINGTONE_CACHE);

        /** {@hide} */
        public static final String RINGTONE2_CACHE = "ringtone2_cache";
        /** {@hide} */
        public static final Uri RINGTONE2_CACHE_URI = getUriFor(RINGTONE2_CACHE);

        /**
         * Persistent store for the system-wide default notification sound.
         *
         * @see #RINGTONE
         * @see #DEFAULT_NOTIFICATION_URI
         */
        @Readable
        public static final String NOTIFICATION_SOUND = "notification_sound";

        /**
         * A {@link Uri} that will point to the current default notification
         * sound at any given time.
         *
         * @see #DEFAULT_RINGTONE_URI
         */
        public static final Uri DEFAULT_NOTIFICATION_URI = getUriFor(NOTIFICATION_SOUND);

        /** {@hide} */
        @Readable
        public static final String NOTIFICATION_SOUND_CACHE = "notification_sound_cache";
        /** {@hide} */
        public static final Uri NOTIFICATION_SOUND_CACHE_URI = getUriFor(NOTIFICATION_SOUND_CACHE);

        /**
         * When enabled, notifications attention effects: sound, vibration, flashing
         * will have a cooldown timer.
         *
         * The value 1 - enable, 0 - disable
         * @hide
         */
        public static final String NOTIFICATION_COOLDOWN_ENABLED =
            "notification_cooldown_enabled";

        /**
         * When enabled, notification cooldown will apply to all notifications.
         * Otherwise cooldown will only apply to conversations.
         *
         * The value 1 - enable, 0 - disable
         * Only valid if {@code NOTIFICATION_COOLDOWN_ENABLED} is enabled.
         * @hide
         */
        public static final String NOTIFICATION_COOLDOWN_ALL =
            "notification_cooldown_all";

        /**
         * When enabled, notification attention effects will be restricted to vibration only
         * as long as the screen is unlocked.
         *
         * The value 1 - enable, 0 - disable
         * @hide
         */
        public static final String NOTIFICATION_COOLDOWN_VIBRATE_UNLOCKED =
            "notification_cooldown_vibrate_unlocked";

        /**
         * Persistent store for the system-wide default alarm alert.
         *
         * @see #RINGTONE
         * @see #DEFAULT_ALARM_ALERT_URI
         */
        @Readable
        public static final String ALARM_ALERT = "alarm_alert";

        /**
         * A {@link Uri} that will point to the current default alarm alert at
         * any given time.
         *
         * @see #DEFAULT_ALARM_ALERT_URI
         */
        public static final Uri DEFAULT_ALARM_ALERT_URI = getUriFor(ALARM_ALERT);

        /** {@hide} */
        @Readable
        public static final String ALARM_ALERT_CACHE = "alarm_alert_cache";
        /** {@hide} */
        public static final Uri ALARM_ALERT_CACHE_URI = getUriFor(ALARM_ALERT_CACHE);

        /**
         * Persistent store for the system default media button event receiver.
         *
         * @hide
         */
        @Readable(maxTargetSdk = Build.VERSION_CODES.R)
        public static final String MEDIA_BUTTON_RECEIVER = "media_button_receiver";

        /**
         * Setting to enable Auto Replace (AutoText) in text editors. 1 = On, 0 = Off
         */
        @Readable
        public static final String TEXT_AUTO_REPLACE = "auto_replace";

        /**
         * Setting to enable Auto Caps in text editors. 1 = On, 0 = Off
         */
        @Readable
        public static final String TEXT_AUTO_CAPS = "auto_caps";

        /**
         * Setting to enable Auto Punctuate in text editors. 1 = On, 0 = Off. This
         * feature converts two spaces to a "." and space.
         */
        @Readable
        public static final String TEXT_AUTO_PUNCTUATE = "auto_punctuate";

        /**
         * Setting to showing password characters in text editors. 1 = On, 0 = Off
         */
        @Readable
        public static final String TEXT_SHOW_PASSWORD = "show_password";

        @Readable
        public static final String SHOW_GTALK_SERVICE_STATUS =
                "SHOW_GTALK_SERVICE_STATUS";

        /**
         * Name of activity to use for wallpaper on the home screen.
         *
         * @deprecated Use {@link WallpaperManager} instead.
         */
        @Deprecated
        @Readable
        public static final String WALLPAPER_ACTIVITY = "wallpaper_activity";

        /**
         * @deprecated Use {@link android.provider.Settings.Global#AUTO_TIME}
         * instead
         */
        @Deprecated
        public static final String AUTO_TIME = Global.AUTO_TIME;

        /**
         * @deprecated Use {@link android.provider.Settings.Global#AUTO_TIME_ZONE}
         * instead
         */
        @Deprecated
        public static final String AUTO_TIME_ZONE = Global.AUTO_TIME_ZONE;

        /**
         * Display the user's times, e.g. in the status bar, as 12 or 24 hours.
         * <ul>
         *    <li>24 = 24 hour</li>
         *    <li>12 = 12 hour</li>
         *    <li>[unset] = use the device locale's default</li>
         * </ul>
         */
        @Readable
        public static final String TIME_12_24 = "time_12_24";

        /**
         * @deprecated No longer used. Use {@link #TIME_12_24} instead.
         */
        @Deprecated
        @Readable
        public static final String DATE_FORMAT = "date_format";

        /**
         * Whether the setup wizard has been run before (on first boot), or if
         * it still needs to be run.
         *
         * nonzero = it has been run in the past
         * 0 = it has not been run in the past
         */
        @Readable
        public static final String SETUP_WIZARD_HAS_RUN = "setup_wizard_has_run";

        /**
         * Scaling factor for normal window animations. Setting to 0 will disable window
         * animations.
         *
         * @deprecated Use {@link Global#WINDOW_ANIMATION_SCALE} instead
         */
        @Deprecated
        public static final String WINDOW_ANIMATION_SCALE = Global.WINDOW_ANIMATION_SCALE;

        /**
         * Scaling factor for activity transition animations. Setting to 0 will disable window
         * animations.
         *
         * @deprecated Use {@link Global#TRANSITION_ANIMATION_SCALE} instead
         */
        @Deprecated
        public static final String TRANSITION_ANIMATION_SCALE = Global.TRANSITION_ANIMATION_SCALE;

        /**
         * Scaling factor for Animator-based animations. This affects both the start delay and
         * duration of all such animations. Setting to 0 will cause animations to end immediately.
         * The default value is 1.
         *
         * @deprecated Use {@link Global#ANIMATOR_DURATION_SCALE} instead
         */
        @Deprecated
        public static final String ANIMATOR_DURATION_SCALE = Global.ANIMATOR_DURATION_SCALE;

        /**
         * Control whether the accelerometer will be used to change screen
         * orientation.  If 0, it will not be used unless explicitly requested
         * by the application; if 1, it will be used by default unless explicitly
         * disabled by the application.
         */
        @Readable
        public static final String ACCELEROMETER_ROTATION = "accelerometer_rotation";

        /**
         * Control the type of rotation which can be performed using the accelerometer
         * if ACCELEROMETER_ROTATION is enabled.
         * Value is a bitwise combination of
         * 1 = 0 degrees (portrait)
         * 2 = 90 degrees (left)
         * 4 = 180 degrees (inverted portrait)
         * 8 = 270 degrees (right)
         * Setting to 0 is effectively orientation lock
         * @hide
         */
        public static final String ACCELEROMETER_ROTATION_ANGLES = "accelerometer_rotation_angles";

        /**
         * Default screen rotation when no other policy applies.
         * When {@link #ACCELEROMETER_ROTATION} is zero and no on-screen Activity expresses a
         * preference, this rotation value will be used. Must be one of the
         * {@link android.view.Surface#ROTATION_0 Surface rotation constants}.
         *
         * @see Display#getRotation
         */
        @Readable
        public static final String USER_ROTATION = "user_rotation";

        /**
         * Control whether the rotation lock toggle in the System UI should be hidden.
         * Typically this is done for accessibility purposes to make it harder for
         * the user to accidentally toggle the rotation lock while the display rotation
         * has been locked for accessibility.
         *
         * If 0, then rotation lock toggle is not hidden for accessibility (although it may be
         * unavailable for other reasons).  If 1, then the rotation lock toggle is hidden.
         *
         * @hide
         */
        @UnsupportedAppUsage
        @Readable
        public static final String HIDE_ROTATION_LOCK_TOGGLE_FOR_ACCESSIBILITY =
                "hide_rotation_lock_toggle_for_accessibility";

        /**
         * Whether the phone vibrates when it is ringing due to an incoming call. This will
         * be used by Phone and Setting apps; it shouldn't affect other apps.
         * The value is boolean (1 or 0).
         *
         * Note: this is not same as "vibrate on ring", which had been available until ICS.
         * It was about AudioManager's setting and thus affected all the applications which
         * relied on the setting, while this is purely about the vibration setting for incoming
         * calls.
         *
         * @deprecated Replaced by using {@link android.os.VibrationAttributes#USAGE_RINGTONE} on
         * vibrations for incoming calls. User settings are applied automatically by the service and
         * should not be applied by individual apps.
         */
        @Deprecated
        @Readable
        public static final String VIBRATE_WHEN_RINGING = "vibrate_when_ringing";

        /**
         * When {@code 1}, Telecom enhanced call blocking functionality is enabled.  When
         * {@code 0}, enhanced call blocking functionality is disabled.
         * @hide
         */
        @Readable
        public static final String DEBUG_ENABLE_ENHANCED_CALL_BLOCKING =
                "debug.enable_enhanced_calling";

        /**
         * Whether the audible DTMF tones are played by the dialer when dialing. The value is
         * boolean (1 or 0).
         */
        @Readable
        public static final String DTMF_TONE_WHEN_DIALING = "dtmf_tone";

        /**
         * CDMA only settings
         * DTMF tone type played by the dialer when dialing.
         *                 0 = Normal
         *                 1 = Long
         */
        @Readable
        public static final String DTMF_TONE_TYPE_WHEN_DIALING = "dtmf_tone_type";

        /**
         * Whether the hearing aid is enabled. The value is
         * boolean (1 or 0).
         * @hide
         */
        @UnsupportedAppUsage
        @Readable
        public static final String HEARING_AID = "hearing_aid";

        /**
         * CDMA only settings
         * TTY Mode
         * 0 = OFF
         * 1 = FULL
         * 2 = VCO
         * 3 = HCO
         * @hide
         */
        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        @Readable
        public static final String TTY_MODE = "tty_mode";

        /**
         * Whether the sounds effects (key clicks, lid open ...) are enabled. The value is
         * boolean (1 or 0).
         */
        @Readable
        public static final String SOUND_EFFECTS_ENABLED = "sound_effects_enabled";

        /**
         * Whether haptic feedback (Vibrate on tap) is enabled. The value is
         * boolean (1 or 0).
         *
         * @deprecated Replaced by using {@link android.os.VibrationAttributes#USAGE_TOUCH} on
         * vibrations. User settings are applied automatically by the service and should not be
         * applied by individual apps.
         */
        @Deprecated
        @Readable
        public static final String HAPTIC_FEEDBACK_ENABLED = "haptic_feedback_enabled";

        /**
         * @deprecated Each application that shows web suggestions should have its own
         * setting for this.
         */
        @Deprecated
        @Readable
        public static final String SHOW_WEB_SUGGESTIONS = "show_web_suggestions";

        /**
         * Whether the notification LED should repeatedly flash when a notification is
         * pending. The value is boolean (1 or 0).
         * @hide
         */
        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        @Readable
        public static final String NOTIFICATION_LIGHT_PULSE = "notification_light_pulse";

        /**
         * Show pointer location on screen?
         * 0 = no
         * 1 = yes
         * @hide
         */
        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        @Readable
        public static final String POINTER_LOCATION = "pointer_location";

        /**
         * Show touch positions on screen?
         * 0 = no
         * 1 = yes
         * @hide
         */
        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        @Readable
        public static final String SHOW_TOUCHES = "show_touches";

        /**
         * Show key presses dispatched to focused windows on the screen.
         * 0 = no
         * 1 = yes
         * @hide
         */
        public static final String SHOW_KEY_PRESSES = "show_key_presses";

        /**
         * Show rotary input dispatched to focused windows on the screen.
         * 0 = no
         * 1 = yes
         * @hide
         */
        public static final String SHOW_ROTARY_INPUT = "show_rotary_input";

        /**
         * The screen backlight brightness for automatic mode.
         *
         * <p>Value should be one of:
         *      <ul>
         *        <li>SCREEN_BRIGHTNESS_AUTOMATIC_BRIGHT
         *        <li>SCREEN_BRIGHTNESS_AUTOMATIC_NORMAL
         *        <li>SCREEN_BRIGHTNESS_AUTOMATIC_DIM
         *      </ul>
         * @hide
         */
        public static final String SCREEN_BRIGHTNESS_FOR_ALS = "screen_brightness_for_als";

        /**
         * SCREEN_BRIGHTNESS_FOR_ALS value for automatic bright.
         * @hide
         */
        public static final int SCREEN_BRIGHTNESS_AUTOMATIC_BRIGHT = 1;

        /**
         * SCREEN_BRIGHTNESS_FOR_ALS value for automatic normal.
         * @hide
         */
        public static final int SCREEN_BRIGHTNESS_AUTOMATIC_NORMAL = 2;

        /**
         * SCREEN_BRIGHTNESS_FOR_ALS value for automatic dim.
         * @hide
         */
        public static final int SCREEN_BRIGHTNESS_AUTOMATIC_DIM = 3;

        /**
         * Log raw orientation data from
         * {@link com.android.server.policy.WindowOrientationListener} for use with the
         * orientationplot.py tool.
         * 0 = no
         * 1 = yes
         * @hide
         */
        @Readable
        public static final String WINDOW_ORIENTATION_LISTENER_LOG =
                "window_orientation_listener_log";

        /**
         * @deprecated Use {@link android.provider.Settings.Global#POWER_SOUNDS_ENABLED}
         * instead
         * @hide
         */
        @Deprecated
        public static final String POWER_SOUNDS_ENABLED = Global.POWER_SOUNDS_ENABLED;

        /**
         * @deprecated Use {@link android.provider.Settings.Global#DOCK_SOUNDS_ENABLED}
         * instead
         * @hide
         */
        @Deprecated
        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        public static final String DOCK_SOUNDS_ENABLED = Global.DOCK_SOUNDS_ENABLED;

        /**
         * Whether to play sounds when the keyguard is shown and dismissed.
         * @hide
         */
        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        @Readable
        public static final String LOCKSCREEN_SOUNDS_ENABLED = "lockscreen_sounds_enabled";

        /**
         * Whether the lockscreen should be completely disabled.
         * @hide
         */
        @Readable
        public static final String LOCKSCREEN_DISABLED = "lockscreen.disabled";

        /**
         * @deprecated Use {@link android.provider.Settings.Global#LOW_BATTERY_SOUND}
         * instead
         * @hide
         */
        @Deprecated
        public static final String LOW_BATTERY_SOUND = Global.LOW_BATTERY_SOUND;

        /**
         * @deprecated Use {@link android.provider.Settings.Global#DESK_DOCK_SOUND}
         * instead
         * @hide
         */
        @Deprecated
        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        public static final String DESK_DOCK_SOUND = Global.DESK_DOCK_SOUND;

        /**
         * @deprecated Use {@link android.provider.Settings.Global#DESK_UNDOCK_SOUND}
         * instead
         * @hide
         */
        @Deprecated
        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        public static final String DESK_UNDOCK_SOUND = Global.DESK_UNDOCK_SOUND;

        /**
         * @deprecated Use {@link android.provider.Settings.Global#CAR_DOCK_SOUND}
         * instead
         * @hide
         */
        @Deprecated
        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        public static final String CAR_DOCK_SOUND = Global.CAR_DOCK_SOUND;

        /**
         * @deprecated Use {@link android.provider.Settings.Global#CAR_UNDOCK_SOUND}
         * instead
         * @hide
         */
        @Deprecated
        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        public static final String CAR_UNDOCK_SOUND = Global.CAR_UNDOCK_SOUND;

        /**
         * @deprecated Use {@link android.provider.Settings.Global#LOCK_SOUND}
         * instead
         * @hide
         */
        @Deprecated
        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        public static final String LOCK_SOUND = Global.LOCK_SOUND;

        /**
         * @deprecated Use {@link android.provider.Settings.Global#UNLOCK_SOUND}
         * instead
         * @hide
         */
        @Deprecated
        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        public static final String UNLOCK_SOUND = Global.UNLOCK_SOUND;

        /**
         * Receive incoming SIP calls?
         * 0 = no
         * 1 = yes
         * @hide
         */
        @Readable
        public static final String SIP_RECEIVE_CALLS = "sip_receive_calls";

        /**
         * Call Preference String.
         * "SIP_ALWAYS" : Always use SIP with network access
         * "SIP_ADDRESS_ONLY" : Only if destination is a SIP address
         * @hide
         */
        @Readable
        public static final String SIP_CALL_OPTIONS = "sip_call_options";

        /**
         * One of the sip call options: Always use SIP with network access.
         * @hide
         */
        @Readable
        public static final String SIP_ALWAYS = "SIP_ALWAYS";

        /**
         * One of the sip call options: Only if destination is a SIP address.
         * @hide
         */
        @Readable
        public static final String SIP_ADDRESS_ONLY = "SIP_ADDRESS_ONLY";

        /**
         * @deprecated Use SIP_ALWAYS or SIP_ADDRESS_ONLY instead.  Formerly used to indicate that
         * the user should be prompted each time a call is made whether it should be placed using
         * SIP.  The {@link com.android.providers.settings.DatabaseHelper} replaces this with
         * SIP_ADDRESS_ONLY.
         * @hide
         */
        @Deprecated
        @Readable
        public static final String SIP_ASK_ME_EACH_TIME = "SIP_ASK_ME_EACH_TIME";

        /**
         * Pointer speed setting.
         * This is an integer value in a range between -7 and +7, so there are 15 possible values.
         *   -7 = slowest
         *    0 = default speed
         *   +7 = fastest
         * @hide
         */
        @SuppressLint({"NoSettingsProvider", "UnflaggedApi"}) // TestApi without associated feature.
        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        @Readable
        @TestApi
        public static final String POINTER_SPEED = "pointer_speed";

        /**
         * Touchpad pointer speed setting.
         * This is an integer value in a range between -7 and +7, so there are 15 possible values.
         *   -7 = slowest
         *    0 = default speed
         *   +7 = fastest
         * @hide
         */
        public static final String TOUCHPAD_POINTER_SPEED = "touchpad_pointer_speed";

        /**
         * Whether to invert the touchpad scrolling direction.
         *
         * If set to 1 (the default), moving two fingers downwards on the touchpad will scroll
         * upwards, consistent with normal touchscreen scrolling. If set to 0, moving two fingers
         * downwards will scroll downwards.
         *
         * @hide
         */
        @Readable
        public static final String TOUCHPAD_NATURAL_SCROLLING = "touchpad_natural_scrolling";

        /**
         * Whether to enable tap-to-click on touchpads.
         *
         * @hide
         */
        public static final String TOUCHPAD_TAP_TO_CLICK = "touchpad_tap_to_click";

        /**
         * Whether to enable tap dragging on touchpads.
         *
         * @hide
         */
        public static final String TOUCHPAD_TAP_DRAGGING = "touchpad_tap_dragging";

        /**
         * Whether to enable a right-click zone on touchpads.
         *
         * When set to 1, pressing to click in a section on the right-hand side of the touchpad will
         * result in a context click (a.k.a. right click).
         *
         * @hide
         */
        public static final String TOUCHPAD_RIGHT_CLICK_ZONE = "touchpad_right_click_zone";

        /**
         * Whether lock-to-app will be triggered by long-press on recents.
         * @hide
         */
        @Readable
        public static final String LOCK_TO_APP_ENABLED = "lock_to_app_enabled";

        /**
         * I am the lolrus.
         * <p>
         * Nonzero values indicate that the user has a bukkit.
         * Backward-compatible with <code>PrefGetPreference(prefAllowEasterEggs)</code>.
         * @hide
         */
        @Readable
        public static final String EGG_MODE = "egg_mode";

        /**
         * Setting to determine whether or not to show the battery percentage in the status bar.
         *    0 - Don't show percentage
         *    1 - Show percentage
         * @hide
         */
        @Readable
        public static final String SHOW_BATTERY_PERCENT = "dummy_show_battery_percent";

        /**
         * Whether or not to enable multiple audio focus.
         * When enabled, requires more management by user over application playback activity,
         * for instance pausing media apps when another starts.
         * @hide
         */
        @Readable
        public static final String MULTI_AUDIO_FOCUS_ENABLED = "multi_audio_focus_enabled";

        /**
         * The information of locale preference. This records user's preference to avoid
         * unsynchronized and existing locale preference in
         * {@link Locale#getDefault(Locale.Category)}.
         *
         * <p><b>Note:</b> The format follow the
         * <a href="https://www.rfc-editor.org/rfc/bcp/bcp47.txt">IETF BCP47 expression</a>
         *
         * E.g. : und-u-ca-gregorian-hc-h23
         * @hide
         */
        public static final String LOCALE_PREFERENCES = "locale_preferences";

        /**
         * Setting to enable camera flash notification feature.
         * <ul>
         *     <li> 0 = Off
         *     <li> 1 = On
         * </ul>
         * @hide
         */
        public static final String CAMERA_FLASH_NOTIFICATION = "camera_flash_notification";

        /**
         * Setting to enable screen flash notification feature.
         * <ul>
         *     <li> 0 = Off
         *     <li> 1 = On
         * </ul>
         *  @hide
         */
        public static final String SCREEN_FLASH_NOTIFICATION = "screen_flash_notification";

        /**
         * Integer property that specifes the color for screen flash notification as a
         * packed 32-bit color.
         *
         * @see android.graphics.Color#argb
         * @hide
         */
        public static final String SCREEN_FLASH_NOTIFICATION_COLOR =
                "screen_flash_notification_color_global";

        /**
         * Volume keys control cursor in text fields (default is 0)
         * 0 - Disabled
         * 1 - Volume up/down moves cursor left/right
         * 2 - Volume up/down moves cursor right/left
         * @hide
         */
        @Readable
        public static final String VOLUME_KEY_CURSOR_CONTROL = "volume_key_cursor_control";

        /**
         * IMPORTANT: If you add a new public settings you also have to add it to
         * PUBLIC_SETTINGS below. If the new setting is hidden you have to add
         * it to PRIVATE_SETTINGS below. Also add a validator that can validate
         * the setting value. See an example above.
         */

        /**
         * Whether to show seconds next to clock in status bar
         * 0 - hide (default)
         * 1 - show
         * @hide
         */
        public static final String STATUS_BAR_CLOCK_SECONDS = "status_bar_clock_seconds";

        /**
         * Shows custom date before clock time
         * 0 - No Date
         * 1 - Small Date
         * 2 - Normal Date
         * @hide
         */
        public static final String STATUS_BAR_CLOCK_DATE_DISPLAY = "status_bar_clock_date_display";

        /**
         * Sets the date string style
         * 0 - Regular style
         * 1 - Lowercase
         * 2 - Uppercase
         * @hide
         */
        public static final String STATUS_BAR_CLOCK_DATE_STYLE = "status_bar_clock_date_style";

        /**
         * Position of date
         * 0 - Left of clock
         * 1 - Right of clock
         * @hide
         */
        public static final String STATUS_BAR_CLOCK_DATE_POSITION = "status_bar_clock_date_position";

        /**
         * Stores the java DateFormat string for the date
         * @hide
         */
        public static final String STATUS_BAR_CLOCK_DATE_FORMAT = "status_bar_clock_date_format";

        /**
         * Whether to auto hide clock
         * @hide
         */
        public static final String STATUS_BAR_CLOCK_AUTO_HIDE = "status_bar_clock_auto_hide";

        /** @hide */
        public static final String STATUS_BAR_CLOCK_AUTO_HIDE_HDURATION = "status_bar_clock_auto_hide_hduration";

        /** @hide */
        public static final String STATUS_BAR_CLOCK_AUTO_HIDE_SDURATION = "status_bar_clock_auto_hide_sduration";

        /**
         * Statusbar clock background
         * 0 - hide accented chip  (default)
         * 1 - show accented chip
         * @hide
         */
        public static final String STATUSBAR_CLOCK_CHIP = "statusbar_clock_chip";

        /**
         * Battery style
         * @hide
         */
        public static final String STATUS_BAR_BATTERY_STYLE = "status_bar_battery_style";

        /**
          * Statusbar Battery %
          * 0: Hide the battery percentage
          * 1: Display the battery percentage inside the icon
          * 2: Display the battery percentage next to Icon
          * @hide
          */
        public static final String STATUS_BAR_SHOW_BATTERY_PERCENT = "status_bar_show_battery_percent";

        /**
         * Show battery percentage when charging
         * @hide
         */
        public static final String STATUS_BAR_BATTERY_TEXT_CHARGING = "status_bar_battery_text_charging";

        /**
         * Whether to show the battery bar
         * @hide
         */
        public static final String STATUSBAR_BATTERY_BAR = "statusbar_battery_bar";

        /**
         * @hide
         */
        public static final String STATUSBAR_BATTERY_BAR_COLOR = "statusbar_battery_bar_color";

        /**
         * @hide
         */
        public static final String STATUSBAR_BATTERY_BAR_THICKNESS =
                "statusbar_battery_bar_thickness";

        /**
         * @hide
         */
        public static final String STATUSBAR_BATTERY_BAR_STYLE = "statusbar_battery_bar_style";

        /**
         * @hide
         */
        public static final String STATUSBAR_BATTERY_BAR_ANIMATE = "statusbar_battery_bar_animate";

        /**
         * @hide
         */
        public static final String STATUSBAR_BATTERY_BAR_CHARGING_COLOR =
                "statusbar_battery_bar_charging_color";
        /**
         * @hide
         */
        public static final String STATUSBAR_BATTERY_BAR_BATTERY_LOW_COLOR =
                "statusbar_battery_bar_battery_low_color";

        /**
         * @hide
         */
        public static final String STATUSBAR_BATTERY_BAR_ENABLE_CHARGING_COLOR =
                "statusbar_battery_bar_enable_charging_color";

        /**
         * @hide
         */
        public static final String STATUSBAR_BATTERY_BAR_BLEND_COLOR = "statusbar_battery_bar_blend_color";

        /**
         * @hide
         */
        public static final String STATUSBAR_BATTERY_BAR_BLEND_COLOR_REVERSE =
                "statusbar_battery_bar_blend_color_reverse";

        /**
         * @hide
         */
        public static final String QS_BATTERY_STYLE = "qs_battery_style";

        /**
         * @hide
         */
        public static final String QS_SHOW_BATTERY_PERCENT = "qs_show_battery_percent";

        /**
         * Double tap on lockscreen to sleep
         * @hide
         */
        public static final String DOUBLE_TAP_SLEEP_LOCKSCREEN = "double_tap_sleep_lockscreen";

        /**
         * Whether StatusBar icons should use app icon
         * @hide
         */
        public static final String STATUSBAR_COLORED_ICONS = "statusbar_colored_icons";

        /**
         * Show the pending notification counts as overlays on the status bar
         * @hide
         */
        public static final String STATUSBAR_NOTIF_COUNT = "statusbar_notif_count";

        /**
         * Enable/disable Bluetooth Battery bar
         * @hide
         */
        public static final String BLUETOOTH_SHOW_BATTERY = "bluetooth_show_battery";

        /**
         * Statusbar logo
         * @hide
         */
        public static final String STATUS_BAR_LOGO = "status_bar_logo";

        /**
         * Position of Status bar logo
         * 0 - Left (default)
         * 1 - Right
         * @hide
         */
        public static final String STATUS_BAR_LOGO_POSITION = "status_bar_logo_position";

        /**
         * Statusbar logo custom style
         * @hide
         */
        public static final String STATUS_BAR_LOGO_STYLE = "status_bar_logo_style";

        /**
         * Whether to display cross sign for a data disabled connection
         * @hide
         */
        public static final String DATA_DISABLED_ICON = "data_disabled_icon";

        /**
         * Whether to display 4G icon instead LTE
         * @hide
         */
        public static final String SHOW_FOURG_ICON = "show_fourg_icon";

        /**
         * Whether to control brightness from status bar
         * 0 = 0ff, 1 = on
         * @hide
         */
        public static final String STATUS_BAR_BRIGHTNESS_CONTROL = "status_bar_brightness_control";

        /**
         * @hide
         */
        public static final String WIFI_STANDARD_ICON = "wifi_standard_icon";

        /**
         * Haptic feedback on brightness slider
         * @hide
         */
        public static final String QS_BRIGHTNESS_SLIDER_HAPTIC = "qs_brightness_slider_haptic";

        /**
         * Whether to disable qs on secure lockscreen.
         * @hide
         */
        public static final String SECURE_LOCKSCREEN_QS_DISABLED = "secure_lockscreen_qs_disabled";

        /**
         * @hide
         */
        public static final String QS_TRANSPARENCY = "qs_transparency";

        /**
         * Whether to show daily data usage in the QS footer.
         * @hide
         */
        public static final String QS_SHOW_DATA_USAGE = "qs_show_data_usage";

        /**
         * Change quick settings tiles animation style
         * @hide
         */
        public static final String QS_TILE_ANIMATION_STYLE = "qs_tile_animation_style";

        /**
         * Change quick settings tiles animation duration
         * @hide
         */
        public static final String QS_TILE_ANIMATION_DURATION = "qs_tile_animation_duration";

        /**
         * Change quick settings tiles interpolator
         * @hide
         */
        public static final String QS_TILE_ANIMATION_INTERPOLATOR = "qs_tile_animation_interpolator";

        /**
         * @hide
         */
        public static final String QS_TILE_VERTICAL_LAYOUT = "qs_tile_vertical_layout";

        /**
         * @hide
         */
        public static final String QS_TILE_LABEL_HIDE = "qs_tile_label_hide";

        /**
         * @hide
         */
        public static final String QS_TILE_LABEL_SIZE = "qs_tile_label_size";

        /**
         * @hide
         */
        public static final String QS_TILE_UI_STYLE = "qs_tile_ui_style";

        /**
         * @hide
         */
        public static final String QS_PANEL_STYLE = "qs_panel_style";

        /**
         * @hide
         */
        public static final String QS_LAYOUT_COLUMNS_LANDSCAPE = "qs_layout_columns_landscape";

        /**
         * @hide
         */
        public static final String QS_LAYOUT_COLUMNS = "qs_layout_columns";

        /**
         * @hide
         */
        public static final String QS_LAYOUT_ROWS_LANDSCAPE = "qs_layout_rows_landscape";

        /**
         * @hide
         */
        public static final String QS_LAYOUT_ROWS = "qs_layout_rows";

        /**
         * @hide
         */
        public static final String QQS_LAYOUT_ROWS_LANDSCAPE = "qqs_layout_rows_landscape";

        /**
         * @hide
         */
        public static final String QQS_LAYOUT_ROWS = "qqs_layout_rows";

        /**
         * Enable and Disable Dual Tone Colors QsPanel
         * @hide
         */
        public static final String QS_DUAL_TONE = "qs_dual_tone";

        /**
         * Whether to use the custom status bar header or not
         * @hide
         */
        public static final String STATUS_BAR_CUSTOM_HEADER = "status_bar_custom_header";

        /**
         * Whether to apply a shadow on top of the header image
         * value is the alpha value of the shadow image is 0 -> no shadow -> 255 black
         * @hide
         */
        public static final String STATUS_BAR_CUSTOM_HEADER_SHADOW = "status_bar_custom_header_shadow";

        /**
         * header image package to use for daylight header - package name - null if default
         * @hide
         */
        public static final String STATUS_BAR_DAYLIGHT_HEADER_PACK = "status_bar_daylight_header_pack";

        /**
         * Current active provider - available currently "static" "daylight"
         * @hide
         */
        public static final String STATUS_BAR_CUSTOM_HEADER_PROVIDER = "status_bar_custom_header_provider";

        /**
         * Manual override picture to use
         * @hide
         */
        public static final String STATUS_BAR_CUSTOM_HEADER_IMAGE = "status_bar_custom_header_image";

        /**
         * @hide
         */
        public static final String STATUS_BAR_FILE_HEADER_IMAGE = "status_bar_file_header_image";

        /**
         * Header height
         * @hide
         */
        public static final String STATUS_BAR_CUSTOM_HEADER_HEIGHT = "status_bar_custom_header_height";

        /**
         * Whether to show material Dismiss All Button for notifications
         * @hide
         */
        public static final String NOTIFICATION_MATERIAL_DISMISS = "notification_material_dismiss";

        /**
         * Whether to turn on Bluetooth automatically when showing the Bluetooth dialog
         * @hide
         */
        public static final String QS_BT_AUTO_ON = "qs_bt_auto_on";

        /**
         * @hide
         */
        public static final String LOCKSCREEN_WEATHER_ENABLED = "lockscreen_weather_enabled";

        /**
         * @hide
         */
        public static final String LOCKSCREEN_WEATHER_LOCATION = "lockscreen_weather_location";

        /**
         * @hide
         */
        public static final String LOCKSCREEN_WEATHER_TEXT = "lockscreen_weather_text";

        /**
         * Whether to show the battery info on the lockscreen while charging
         * @hide
         */
        public static final String LOCKSCREEN_BATTERY_INFO = "lockscreen_battery_info";

        /**
         * Whether to enable the ripple animation on fingerprint unlock
         * @hide
         */
        public static final String ENABLE_RIPPLE_EFFECT = "enable_ripple_effect";

        /**
         * Whether to show power menu on LockScreen
         * @hide
         */
        public static final String LOCKSCREEN_ENABLE_POWER_MENU = "lockscreen_enable_power_menu";

        /**
         * Whether to vibrate on succesful fingerprint authentication
         * @hide
         */
        public static final String FP_SUCCESS_VIBRATE = "fp_success_vibrate";

        /**
         * Whether to vibrate on unsuccesful fingerprint authentication
         * @hide
         */
        public static final String FP_ERROR_VIBRATE = "fp_error_vibrate";

        /**
         * @hide
         */
        public static final String UDFPS_ANIM_STYLE = "udfps_anim_style";

        /**
         * @hide
         */
        public static final String UDFPS_ICON = "udfps_icon";

        /**
         * GameSpace: List of added games by user
         * @hide
         */
        @Readable
        public static final String GAMESPACE_GAME_LIST = "gamespace_game_list";

        /**
         * GameSpace: Whether fullscreen intent will be suppressed while in game session
         * @hide
         */
        @Readable
        public static final String GAMESPACE_SUPPRESS_FULLSCREEN_INTENT = "gamespace_suppress_fullscreen_intent";

        /**
         * Sensor block per-package
         * @hide
         */
        @Readable
        public static final String SENSOR_BLOCK = "sensor_block";

        /**
         * Sensor blocked packages
         * @hide
         */
        @Readable
        public static final String SENSOR_BLOCKED_APP = "sensor_blocked_app";

         /** @hide */
        @Readable
        public static final String SENSOR_BLOCKED_APP_DUMMY = "sensor_blocked_app_dummy";

        /**
         * Whether allowing pocket service to register sensors and dispatch informations.
         *   0 = disabled
         *   1 = enabled
         * @hide
         */
        public static final String POCKET_JUDGE = "pocket_judge";

        /**
         * Whether to enable the pixel navbar animation
         * @hide
         */
        public static final String PIXEL_NAV_ANIMATION = "pixel_nav_animation";

        /**
         * Gesture navbar length mode.
         * Supported modes: 0 for short length, 1 for normal and 2 for long.
         * @hide
         */
        public static final String GESTURE_NAVBAR_LENGTH_MODE = "gesture_navbar_length_mode";

        /** @hide */
        public static final String BACK_GESTURE_HEIGHT = "back_gesture_height";

        /**
         * Size of gesture bar radius.
         * @hide
         */
        public static final String GESTURE_NAVBAR_RADIUS = "gesture_navbar_radius";

        /**
         * @hide
         */
        public static final String SCREENSHOT_SHUTTER_SOUND = "screenshot_shutter_sound";

        /**
         * Disable hw buttons
         * @hide
         */
        public static final String HARDWARE_KEYS_DISABLE = "hardware_keys_disable";

        /**
         * Swap capacitive keys
         * @hide
         */
        public static final String SWAP_CAPACITIVE_KEYS = "swap_capacitive_keys";

        /**
         * Indicates whether ANBI (Accidental navigation button interaction) is enabled.
         * @hide
         */
        public static final String ANBI_ENABLED = "anbi_enabled";

        /**
         * If On-The-Go should be displayed at the power menu.
         * @hide
         */
        public static final String GLOBAL_ACTIONS_ONTHEGO = "global_actions_onthego";

        /**
         * The alpha value of the On-The-Go overlay.
         * @hide
         */
        public static final String ON_THE_GO_ALPHA = "on_the_go_alpha";

        /**
         * Whether the service should restart itself or not.
         * @hide
         */
        public static final String ON_THE_GO_SERVICE_RESTART = "on_the_go_service_restart";

        /**
         * The camera instance to use.
         * 0 = Rear Camera
         * 1 = Front Camera
         * @hide
         */
        public static final String ON_THE_GO_CAMERA = "on_the_go_camera";

        /**
         * Navbar style
         * @hide
         */
        public static final String NAVBAR_STYLE = "navbar_style";

        /**
         * Whether to show or hide alert slider notifications on supported devices
         * @hide
         */
        public static final String ALERT_SLIDER_NOTIFICATIONS = "alert_slider_notifications";

        /**
         * Whether to show heads up only for dialer and sms apps
         * @hide
         */
        public static final String LESS_BORING_HEADS_UP = "less_boring_heads_up";

        /**
         * Heads up timeout configuration
         * @hide
         */
        public static final String HEADS_UP_TIMEOUT = "heads_up_timeout";

        /**
         * Whether to show the kill app button in notification guts
         * @hide
         */
        public static final String NOTIFICATION_GUTS_KILL_APP_BUTTON =
                "notification_guts_kill_app_button";

        /**
         * Whether to show charging animation
         * @hide
         */
        public static final String CHARGING_ANIMATION = "charging_animation";

        /**
         * Whether edge light is enabled.
         * Default 0
         * @hide
         */
        public static final String EDGE_LIGHT_ENABLED = "edge_light_enabled";

        /**
         * Whether to show edge light for all pulse events and not just for notifications.
         * Default 0
         * @hide
         */
        public static final String EDGE_LIGHT_ALWAYS_TRIGGER_ON_PULSE = "edge_light_always_trigger_on_pulse";

        /**
         * Whether to repeat edge light animation until pulse timeout.
         * Default 0
         * @hide
         */
        public static final String EDGE_LIGHT_REPEAT_ANIMATION = "edge_light_repeat_animation";

        /**
         * Color mode of edge light.
         * 0: Accent
         * 1: Notification
         * 2: Wallpaper
         * 3: Custom
         * Default 0
         * @hide
         */
        public static final String EDGE_LIGHT_COLOR_MODE = "edge_light_color_mode";

        /**
         * Custom color (hex value) for edge light.
         * Default #FFFFFF
         * @hide
         */
        public static final String EDGE_LIGHT_CUSTOM_COLOR = "edge_light_custom_color";

        /**
         * Force full screen for devices with cutout
         * @hide
         */
        public static final String FORCE_FULLSCREEN_CUTOUT_APPS = "force_full_screen_cutout_apps";

        /**
         * Whether to enable Smart Pixels
         * @hide
         */
        public static final String SMART_PIXELS_ENABLE = "smart_pixels_enable";

        /**
         * Smart Pixels pattern
         * @hide
         */
        public static final String SMART_PIXELS_PATTERN = "smart_pixels_pattern";

        /**
         * Smart Pixels Shift Timeout
         * @hide
         */
        public static final String SMART_PIXELS_SHIFT_TIMEOUT = "smart_pixels_shift_timeout";

        /**
         * Whether Smart Pixels should enable on power saver mode
         * @hide
         */
        public static final String SMART_PIXELS_ON_POWER_SAVE = "smart_pixels_on_power_save";

        /**
         * Defines the screen-off animation to display
         * @hide
         */
        public static final String SCREEN_OFF_ANIMATION = "screen_off_animation";

        /**
         * Whether to show rotation suggestion
         * @hide
         */
        @Readable
        public static final String ENABLE_ROTATION_BUTTON = "enable_rotation_button";

        /**
         * Adaptive playback
         * Automatically pause media when the volume is muted and
         * will resume automatically when volume is restored.
         *   0 = disabled
         *   1 = enabled
         * @hide
         */
        public static final String ADAPTIVE_PLAYBACK_ENABLED = "adaptive_playback_enabled";

        /**
         * Adaptive playback's timeout in ms
         * @hide
         */
        public static final String ADAPTIVE_PLAYBACK_TIMEOUT = "adaptive_playback_timeout";

        /**
         * Show app volume rows in volume panel
         * @hide
         */
        public static final String SHOW_APP_VOLUME = "show_app_volume";

        /**
         * @hide
         */
        public static final String ISLAND_NOTIFICATION = "island_notification";

        /**
         * @hide
         */
        public static final String ISLAND_NOTIFICATION_NOW_PLAYING = "island_notification_now_playing";

        /**
         * Override max volume for {@link android.media.AudioSystem.STREAM_VOICE_CALL}
         * see {@link com.android.server.audio.AudioService} for defaults in priority
         * -1 = disabled
         * @hide
         */
        @Readable
        public static final String MAX_CALL_VOLUME = "max_call_volume";

        /**
         * Stores the default max volume for {@link android.media.AudioSystem.STREAM_VOICE_CALL}
         * see {@link com.android.server.audio.AudioService} for defaults in priority
         * Used as a fallback for the user settings
         * @hide
         */
        @Readable
        public static final String DEFAULT_MAX_CALL_VOLUME = "default_max_call_volume";

        /**
         * Override max volume for {@link android.media.AudioSystem.STREAM_MUSIC}
         * see {@link com.android.server.audio.AudioService} for defaults in priority
         * -1 = disabled
         * @hide
         */
        @Readable
        public static final String MAX_MUSIC_VOLUME = "max_music_volume";

        /**
         * Stores the default max volume for {@link android.media.AudioSystem.STREAM_MUSIC}
         * see {@link com.android.server.audio.AudioService} for defaults in priority
         * Used as a fallback for the user settings
         * @hide
         */
        @Readable
        public static final String DEFAULT_MAX_MUSIC_VOLUME = "default_max_music_volume";

        /**
         * Override max volume for {@link android.media.AudioSystem.STREAM_ALARM}
         * see {@link com.android.server.audio.AudioService} for defaults in priority
         * -1 = disabled
         * @hide
         */
        @Readable
        public static final String MAX_ALARM_VOLUME = "max_alarm_volume";

        /**
         * Stores the default max volume for {@link android.media.AudioSystem.STREAM_ALARM}
         * see {@link com.android.server.audio.AudioService} for defaults in priority
         * Used as a fallback for the user settings
         * @hide
         */
        @Readable
        public static final String DEFAULT_MAX_ALARM_VOLUME = "default_max_alarm_volume";

        /**
         * Which Vibration Pattern to use
         * 0: dzzz-dzzz
         * 1: dzzz-da
         * 2: mm-mm-mm
         * 3: da-da-dzzz
         * 4: da-dzzz-da
         * 5: custom
         * @hide
         */
        @Readable
        public static final String RINGTONE_VIBRATION_PATTERN = "ringtone_vibration_pattern";

        /**
         * Custom vibration pattern
         * format: ms,ms,ms each a range from 0 to 1000 ms
         * @hide
         */
        @Readable
        public static final String CUSTOM_RINGTONE_VIBRATION_PATTERN = "custom_ringtone_vibration_pattern";

        /**
         * Whether the phone vibrates on call connect
         * @hide
         */
        @Readable
        public static final String VIBRATE_ON_CONNECT = "vibrate_on_connect";

        /**
         * Whether the phone vibrates on call waiting
         * @hide
         */
        @Readable
        public static final String VIBRATE_ON_CALLWAITING = "vibrate_on_callwaiting";

        /**
         * Whether the phone vibrates on disconnect
         * @hide
         */
        @Readable
        public static final String VIBRATE_ON_DISCONNECT = "vibrate_on_disconnect";

        /**
         * Whether to blink flashlight for incoming calls
         * 0 = Disabled (Default)
         * 1 = Blink flashlight only in Ringer mode
         * 2 = Blink flashlight only when ringer is not audible
         * 3 = Blink flahslight only when entirely silent
         * 4 = Blink flashlight always regardless of ringer mode
         * @hide
         */
        @Readable
        public static final String FLASHLIGHT_ON_CALL = "flashlight_on_call";

        /**
         * Whether flashlight_on_call ignores DND (Zen Mode)
         * @hide
         */
        @Readable
        public static final String FLASHLIGHT_ON_CALL_IGNORE_DND = "flashlight_on_call_ignore_dnd";

        /**
         * Rate in Hz in which to blink flashlight_on_call
         * @hide
         */
        @Readable
        public static final String FLASHLIGHT_ON_CALL_RATE = "flashlight_on_call_rate";

        /**
         * @hide
         */
        @Readable
        public static final String NAVIGATION_BAR_IME_SPACE = "navigation_bar_ime_space";

        /**
         * @hide
         */
        public static final String RECENTS_LOCKED_TASKS = "recents_locked_tasks";

        /**
         * Arcane Idle Manager
         * @hide
         */
        @Readable
        public static final String ARCANE_IDLE_MANAGER = "arcane_idle_manager";

        /**
         * Whether three fingers swipe is active
         * 0 = Inactive, 1 = Active
         * @hide
         */
        @Readable
        public static final String THREE_FINGER_GESTURE_ACTIVE = "three_fingers_swipe_active";

        /**
         * Clock font size
         * @hide
         */
        public static final String STATUS_BAR_CLOCK_SIZE = "status_bar_clock_size";

        /**
         * Clock font size QS
         * @hide
         */
        public static final String QS_HEADER_CLOCK_SIZE = "qs_header_clock_size";

        /**
         * Clock font color
         * @hide
         */
        public static final String STATUS_BAR_CLOCK_COLOR = "status_bar_clock_color";

        /**
         * Clock font style
         * @hide
         */
        public static final String STATUS_BAR_CLOCK_FONT_STYLE = "status_bar_clock_font_style";

        /**
         * @hide
         */
        public static final String QS_PANEL_TILE_HAPTIC = "qs_panel_tile_haptic";

        /**
         * @hide
         */
        @Readable
        public static final String EDGE_SCROLLING_HAPTICS_INTENSITY = "edge_scrolling_haptics_intensity";

        /**
         * @hide
         */
        public static final String VOLUME_SLIDER_HAPTICS_INTENSITY = "volume_slider_haptics_intensity";

        /**
         * Whether to show IME space
         * @hide
         */
        @Readable
        public static final String HIDE_IME_SPACE_ENABLE = "hide_ime_space_enable";

        /**
         * Whether to play notification sound and vibration if screen is ON
         * 0 - never
         * 1 - always
         * @hide
         */
        public static final String NOTIFICATION_SOUND_VIB_SCREEN_ON = "notification_sound_vib_screen_on";

        /**
         * Keys we no longer back up under the current schema, but want to continue to
         * process when restoring historical backup datasets.
         *
         * All settings in {@link LEGACY_RESTORE_SETTINGS} array *must* have a non-null validator,
         * otherwise they won't be restored.
         *
         * @hide
         */
        public static final String[] LEGACY_RESTORE_SETTINGS = {
        };

        /**
         * Whether to enable smart 5G mode
         * @hide
         */
        public static final String SMART_5G = "smart_5g";

        /**
         * These are all public system settings
         *
         * @hide
         */
        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        public static final Set<String> PUBLIC_SETTINGS = new ArraySet<>();
        static {
            PUBLIC_SETTINGS.add(END_BUTTON_BEHAVIOR);
            PUBLIC_SETTINGS.add(WIFI_USE_STATIC_IP);
            PUBLIC_SETTINGS.add(WIFI_STATIC_IP);
            PUBLIC_SETTINGS.add(WIFI_STATIC_GATEWAY);
            PUBLIC_SETTINGS.add(WIFI_STATIC_NETMASK);
            PUBLIC_SETTINGS.add(WIFI_STATIC_DNS1);
            PUBLIC_SETTINGS.add(WIFI_STATIC_DNS2);
            PUBLIC_SETTINGS.add(BLUETOOTH_DISCOVERABILITY);
            PUBLIC_SETTINGS.add(BLUETOOTH_DISCOVERABILITY_TIMEOUT);
            PUBLIC_SETTINGS.add(NEXT_ALARM_FORMATTED);
            PUBLIC_SETTINGS.add(FONT_SCALE);
            PUBLIC_SETTINGS.add(SYSTEM_LOCALES);
            PUBLIC_SETTINGS.add(DIM_SCREEN);
            PUBLIC_SETTINGS.add(SCREEN_OFF_TIMEOUT);
            PUBLIC_SETTINGS.add(SCREEN_BRIGHTNESS);
            PUBLIC_SETTINGS.add(SCREEN_BRIGHTNESS_FLOAT);
            PUBLIC_SETTINGS.add(SCREEN_BRIGHTNESS_MODE);
            PUBLIC_SETTINGS.add(MODE_RINGER_STREAMS_AFFECTED);
            PUBLIC_SETTINGS.add(MUTE_STREAMS_AFFECTED);
            PUBLIC_SETTINGS.add(VIBRATE_ON);
            PUBLIC_SETTINGS.add(VOLUME_RING);
            PUBLIC_SETTINGS.add(VOLUME_SYSTEM);
            PUBLIC_SETTINGS.add(VOLUME_VOICE);
            PUBLIC_SETTINGS.add(VOLUME_MUSIC);
            PUBLIC_SETTINGS.add(VOLUME_ALARM);
            PUBLIC_SETTINGS.add(VOLUME_NOTIFICATION);
            PUBLIC_SETTINGS.add(VOLUME_BLUETOOTH_SCO);
            PUBLIC_SETTINGS.add(VOLUME_ASSISTANT);
            PUBLIC_SETTINGS.add(RINGTONE);
            PUBLIC_SETTINGS.add(RINGTONE2);
            PUBLIC_SETTINGS.add(NOTIFICATION_SOUND);
            PUBLIC_SETTINGS.add(ALARM_ALERT);
            PUBLIC_SETTINGS.add(TEXT_AUTO_REPLACE);
            PUBLIC_SETTINGS.add(TEXT_AUTO_CAPS);
            PUBLIC_SETTINGS.add(TEXT_AUTO_PUNCTUATE);
            PUBLIC_SETTINGS.add(TEXT_SHOW_PASSWORD);
            PUBLIC_SETTINGS.add(SHOW_GTALK_SERVICE_STATUS);
            PUBLIC_SETTINGS.add(WALLPAPER_ACTIVITY);
            PUBLIC_SETTINGS.add(TIME_12_24);
            PUBLIC_SETTINGS.add(DATE_FORMAT);
            PUBLIC_SETTINGS.add(SETUP_WIZARD_HAS_RUN);
            PUBLIC_SETTINGS.add(ACCELEROMETER_ROTATION);
            PUBLIC_SETTINGS.add(USER_ROTATION);
            PUBLIC_SETTINGS.add(DTMF_TONE_WHEN_DIALING);
            PUBLIC_SETTINGS.add(SOUND_EFFECTS_ENABLED);
            PUBLIC_SETTINGS.add(HAPTIC_FEEDBACK_ENABLED);
            PUBLIC_SETTINGS.add(SHOW_WEB_SUGGESTIONS);
            PUBLIC_SETTINGS.add(VIBRATE_WHEN_RINGING);
            PUBLIC_SETTINGS.add(APPLY_RAMPING_RINGER);
        }

        /**
         * These are all hidden system settings.
         *
         * @hide
         */
        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        public static final Set<String> PRIVATE_SETTINGS = new ArraySet<>();
        static {
            PRIVATE_SETTINGS.add(WIFI_USE_STATIC_IP);
            PRIVATE_SETTINGS.add(END_BUTTON_BEHAVIOR);
            PRIVATE_SETTINGS.add(ADVANCED_SETTINGS);
            PRIVATE_SETTINGS.add(WEAR_ACCESSIBILITY_GESTURE_ENABLED);
            PRIVATE_SETTINGS.add(WEAR_ACCESSIBILITY_GESTURE_ENABLED_DURING_OOBE);
            PRIVATE_SETTINGS.add(WEAR_TTS_PREWARM_ENABLED);
            PRIVATE_SETTINGS.add(SCREEN_AUTO_BRIGHTNESS_ADJ);
            PRIVATE_SETTINGS.add(VIBRATE_INPUT_DEVICES);
            PRIVATE_SETTINGS.add(VOLUME_MASTER);
            PRIVATE_SETTINGS.add(MASTER_MONO);
            PRIVATE_SETTINGS.add(MASTER_BALANCE);
            PRIVATE_SETTINGS.add(NOTIFICATIONS_USE_RING_VOLUME);
            PRIVATE_SETTINGS.add(VIBRATE_IN_SILENT);
            PRIVATE_SETTINGS.add(MEDIA_BUTTON_RECEIVER);
            PRIVATE_SETTINGS.add(HIDE_ROTATION_LOCK_TOGGLE_FOR_ACCESSIBILITY);
            PRIVATE_SETTINGS.add(DTMF_TONE_TYPE_WHEN_DIALING);
            PRIVATE_SETTINGS.add(HEARING_AID);
            PRIVATE_SETTINGS.add(TTY_MODE);
            PRIVATE_SETTINGS.add(NOTIFICATION_LIGHT_PULSE);
            PRIVATE_SETTINGS.add(POINTER_LOCATION);
            PRIVATE_SETTINGS.add(SHOW_TOUCHES);
            PRIVATE_SETTINGS.add(SHOW_KEY_PRESSES);
            PRIVATE_SETTINGS.add(WINDOW_ORIENTATION_LISTENER_LOG);
            PRIVATE_SETTINGS.add(POWER_SOUNDS_ENABLED);
            PRIVATE_SETTINGS.add(DOCK_SOUNDS_ENABLED);
            PRIVATE_SETTINGS.add(LOCKSCREEN_SOUNDS_ENABLED);
            PRIVATE_SETTINGS.add(LOCKSCREEN_DISABLED);
            PRIVATE_SETTINGS.add(LOW_BATTERY_SOUND);
            PRIVATE_SETTINGS.add(DESK_DOCK_SOUND);
            PRIVATE_SETTINGS.add(DESK_UNDOCK_SOUND);
            PRIVATE_SETTINGS.add(CAR_DOCK_SOUND);
            PRIVATE_SETTINGS.add(CAR_UNDOCK_SOUND);
            PRIVATE_SETTINGS.add(LOCK_SOUND);
            PRIVATE_SETTINGS.add(UNLOCK_SOUND);
            PRIVATE_SETTINGS.add(SIP_RECEIVE_CALLS);
            PRIVATE_SETTINGS.add(SIP_CALL_OPTIONS);
            PRIVATE_SETTINGS.add(SIP_ALWAYS);
            PRIVATE_SETTINGS.add(SIP_ADDRESS_ONLY);
            PRIVATE_SETTINGS.add(SIP_ASK_ME_EACH_TIME);
            PRIVATE_SETTINGS.add(POINTER_SPEED);
            PRIVATE_SETTINGS.add(LOCK_TO_APP_ENABLED);
            PRIVATE_SETTINGS.add(EGG_MODE);
            PRIVATE_SETTINGS.add(DISPLAY_COLOR_MODE);
            PRIVATE_SETTINGS.add(DISPLAY_COLOR_MODE_VENDOR_HINT);
            PRIVATE_SETTINGS.add(LOCALE_PREFERENCES);
            PRIVATE_SETTINGS.add(TOUCHPAD_POINTER_SPEED);
            PRIVATE_SETTINGS.add(TOUCHPAD_NATURAL_SCROLLING);
            PRIVATE_SETTINGS.add(TOUCHPAD_TAP_TO_CLICK);
            PRIVATE_SETTINGS.add(TOUCHPAD_TAP_DRAGGING);
            PRIVATE_SETTINGS.add(TOUCHPAD_RIGHT_CLICK_ZONE);
            PRIVATE_SETTINGS.add(CAMERA_FLASH_NOTIFICATION);
            PRIVATE_SETTINGS.add(SCREEN_FLASH_NOTIFICATION);
            PRIVATE_SETTINGS.add(SCREEN_FLASH_NOTIFICATION_COLOR);
            PRIVATE_SETTINGS.add(DEFAULT_DEVICE_FONT_SCALE);
        }

        /**
         * These entries are considered common between the personal and the managed profile,
         * since the managed profile doesn't get to change them.
         */
        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        private static final Set<String> CLONE_TO_MANAGED_PROFILE = new ArraySet<>();
        static {
            CLONE_TO_MANAGED_PROFILE.add(DATE_FORMAT);
            CLONE_TO_MANAGED_PROFILE.add(HAPTIC_FEEDBACK_ENABLED);
            CLONE_TO_MANAGED_PROFILE.add(SOUND_EFFECTS_ENABLED);
            CLONE_TO_MANAGED_PROFILE.add(TEXT_SHOW_PASSWORD);
            CLONE_TO_MANAGED_PROFILE.add(TIME_12_24);
        }

        /** @hide */
        public static void getCloneToManagedProfileSettings(Set<String> outKeySet) {
            outKeySet.addAll(CLONE_TO_MANAGED_PROFILE);
        }

        /**
         * These entries should be cloned from this profile's parent only if the dependency's
         * value is true ("1")
         *
         * Note: the dependencies must be Secure settings
         *
         * @hide
         */
        public static final Map<String, String> CLONE_FROM_PARENT_ON_VALUE = new ArrayMap<>();
        static {
            CLONE_FROM_PARENT_ON_VALUE.put(RINGTONE, Secure.SYNC_PARENT_SOUNDS);
            CLONE_FROM_PARENT_ON_VALUE.put(RINGTONE2, Secure.SYNC_PARENT_SOUNDS);
            CLONE_FROM_PARENT_ON_VALUE.put(NOTIFICATION_SOUND, Secure.SYNC_PARENT_SOUNDS);
            CLONE_FROM_PARENT_ON_VALUE.put(ALARM_ALERT, Secure.SYNC_PARENT_SOUNDS);
        }

        /** @hide */
        public static void getCloneFromParentOnValueSettings(Map<String, String> outMap) {
            outMap.putAll(CLONE_FROM_PARENT_ON_VALUE);
        }

        /**
         * System settings which can be accessed by instant apps.
         * @hide
         */
        public static final Set<String> INSTANT_APP_SETTINGS = new ArraySet<>();
        static {
            INSTANT_APP_SETTINGS.add(TEXT_AUTO_REPLACE);
            INSTANT_APP_SETTINGS.add(TEXT_AUTO_CAPS);
            INSTANT_APP_SETTINGS.add(TEXT_AUTO_PUNCTUATE);
            INSTANT_APP_SETTINGS.add(TEXT_SHOW_PASSWORD);
            INSTANT_APP_SETTINGS.add(DATE_FORMAT);
            INSTANT_APP_SETTINGS.add(FONT_SCALE);
            INSTANT_APP_SETTINGS.add(HAPTIC_FEEDBACK_ENABLED);
            INSTANT_APP_SETTINGS.add(TIME_12_24);
            INSTANT_APP_SETTINGS.add(SOUND_EFFECTS_ENABLED);
            INSTANT_APP_SETTINGS.add(ACCELEROMETER_ROTATION);
        }

        /**
         * When to use Wi-Fi calling
         *
         * @see android.telephony.TelephonyManager.WifiCallingChoices
         * @hide
         */
        @Readable
        public static final String WHEN_TO_MAKE_WIFI_CALLS = "when_to_make_wifi_calls";

        /** Controls whether bluetooth is on or off on wearable devices.
         *
         * <p>The valid values for this key are: 0 (disabled) or 1 (enabled).
         *
         * @hide
         */
        public static final String CLOCKWORK_BLUETOOTH_SETTINGS_PREF = "cw_bt_settings_pref";

        /**
         * Controls whether the unread notification dot indicator is shown on wearable devices.
         *
         * <p>The valid values for this key are: 0 (disabled) or 1 (enabled).
         *
         * @hide
         */
        public static final String UNREAD_NOTIFICATION_DOT_INDICATOR =
                "unread_notification_dot_indicator";

        /**
         * Controls whether auto-launching media controls is enabled on wearable devices.
         *
         * <p>The valid values for this key are: 0 (disabled) or 1 (enabled).
         *
         * @hide
         */
        public static final String AUTO_LAUNCH_MEDIA_CONTROLS = "auto_launch_media_controls";

        // Settings moved to Settings.Secure

        /**
         * @deprecated Use {@link android.provider.Settings.Global#ADB_ENABLED}
         * instead
         */
        @Deprecated
        public static final String ADB_ENABLED = Global.ADB_ENABLED;

        /**
         * @deprecated Use {@link android.provider.Settings.Secure#ANDROID_ID} instead
         */
        @Deprecated
        public static final String ANDROID_ID = Secure.ANDROID_ID;

        /**
         * @deprecated Use {@link android.provider.Settings.Global#BLUETOOTH_ON} instead
         */
        @Deprecated
        public static final String BLUETOOTH_ON = Global.BLUETOOTH_ON;

        /**
         * @deprecated Use {@link android.provider.Settings.Global#DATA_ROAMING} instead
         */
        @Deprecated
        public static final String DATA_ROAMING = Global.DATA_ROAMING;

        /**
         * @deprecated Use {@link android.provider.Settings.Global#DEVICE_PROVISIONED} instead
         */
        @Deprecated
        public static final String DEVICE_PROVISIONED = Global.DEVICE_PROVISIONED;

        /**
         * @deprecated Use {@link android.provider.Settings.Global#HTTP_PROXY} instead
         */
        @Deprecated
        public static final String HTTP_PROXY = Global.HTTP_PROXY;

        /**
         * @deprecated Use {@link android.provider.Settings.Secure#INSTALL_NON_MARKET_APPS} instead
         */
        @Deprecated
        public static final String INSTALL_NON_MARKET_APPS = Secure.INSTALL_NON_MARKET_APPS;

        /**
         * @deprecated Use {@link android.provider.Settings.Secure#LOCATION_PROVIDERS_ALLOWED}
         * instead
         */
        @Deprecated
        public static final String LOCATION_PROVIDERS_ALLOWED = Secure.LOCATION_PROVIDERS_ALLOWED;

        /**
         * @deprecated Use {@link android.provider.Settings.Secure#LOGGING_ID} instead
         */
        @Deprecated
        public static final String LOGGING_ID = Secure.LOGGING_ID;

        /**
         * @deprecated Use {@link android.provider.Settings.Global#NETWORK_PREFERENCE} instead
         */
        @Deprecated
        public static final String NETWORK_PREFERENCE = Global.NETWORK_PREFERENCE;

        /**
         * @deprecated Use {@link android.provider.Settings.Secure#PARENTAL_CONTROL_ENABLED}
         * instead
         */
        @Deprecated
        public static final String PARENTAL_CONTROL_ENABLED = Secure.PARENTAL_CONTROL_ENABLED;

        /**
         * @deprecated Use {@link android.provider.Settings.Secure#PARENTAL_CONTROL_LAST_UPDATE}
         * instead
         */
        @Deprecated
        public static final String PARENTAL_CONTROL_LAST_UPDATE = Secure.PARENTAL_CONTROL_LAST_UPDATE;

        /**
         * @deprecated Use {@link android.provider.Settings.Secure#PARENTAL_CONTROL_REDIRECT_URL}
         * instead
         */
        @Deprecated
        public static final String PARENTAL_CONTROL_REDIRECT_URL =
            Secure.PARENTAL_CONTROL_REDIRECT_URL;

        /**
         * @deprecated Use {@link android.provider.Settings.Secure#SETTINGS_CLASSNAME} instead
         */
        @Deprecated
        public static final String SETTINGS_CLASSNAME = Secure.SETTINGS_CLASSNAME;

        /**
         * @deprecated Use {@link android.provider.Settings.Global#USB_MASS_STORAGE_ENABLED} instead
         */
        @Deprecated
        public static final String USB_MASS_STORAGE_ENABLED = Global.USB_MASS_STORAGE_ENABLED;

        /**
         * @deprecated Use {@link android.provider.Settings.Global#USE_GOOGLE_MAIL} instead
         */
        @Deprecated
        public static final String USE_GOOGLE_MAIL = Global.USE_GOOGLE_MAIL;

       /**
         * @deprecated Use
         * {@link android.provider.Settings.Global#WIFI_MAX_DHCP_RETRY_COUNT} instead
         */
        @Deprecated
        public static final String WIFI_MAX_DHCP_RETRY_COUNT = Global.WIFI_MAX_DHCP_RETRY_COUNT;

        /**
         * @deprecated Use
         * {@link android.provider.Settings.Global#WIFI_MOBILE_DATA_TRANSITION_WAKELOCK_TIMEOUT_MS} instead
         */
        @Deprecated
        public static final String WIFI_MOBILE_DATA_TRANSITION_WAKELOCK_TIMEOUT_MS =
                Global.WIFI_MOBILE_DATA_TRANSITION_WAKELOCK_TIMEOUT_MS;

        /**
         * @deprecated Use
         * {@link android.provider.Settings.Global#WIFI_NETWORKS_AVAILABLE_NOTIFICATION_ON} instead
         */
        @Deprecated
        public static final String WIFI_NETWORKS_AVAILABLE_NOTIFICATION_ON =
                Global.WIFI_NETWORKS_AVAILABLE_NOTIFICATION_ON;

        /**
         * @deprecated Use
         * {@link android.provider.Settings.Global#WIFI_NETWORKS_AVAILABLE_REPEAT_DELAY} instead
         */
        @Deprecated
        public static final String WIFI_NETWORKS_AVAILABLE_REPEAT_DELAY =
                Global.WIFI_NETWORKS_AVAILABLE_REPEAT_DELAY;

        /**
         * @deprecated Use {@link android.provider.Settings.Global#WIFI_NUM_OPEN_NETWORKS_KEPT}
         * instead
         */
        @Deprecated
        public static final String WIFI_NUM_OPEN_NETWORKS_KEPT = Global.WIFI_NUM_OPEN_NETWORKS_KEPT;

        /**
         * @deprecated Use {@link android.provider.Settings.Global#WIFI_ON} instead
         */
        @Deprecated
        public static final String WIFI_ON = Global.WIFI_ON;

        /**
         * @deprecated Use
         * {@link android.provider.Settings.Secure#WIFI_WATCHDOG_ACCEPTABLE_PACKET_LOSS_PERCENTAGE}
         * instead
         */
        @Deprecated
        @Readable
        public static final String WIFI_WATCHDOG_ACCEPTABLE_PACKET_LOSS_PERCENTAGE =
                Secure.WIFI_WATCHDOG_ACCEPTABLE_PACKET_LOSS_PERCENTAGE;

        /**
         * @deprecated Use {@link android.provider.Settings.Secure#WIFI_WATCHDOG_AP_COUNT} instead
         */
        @Deprecated
        public static final String WIFI_WATCHDOG_AP_COUNT = Secure.WIFI_WATCHDOG_AP_COUNT;

        /**
         * @deprecated Use
         * {@link android.provider.Settings.Secure#WIFI_WATCHDOG_BACKGROUND_CHECK_DELAY_MS} instead
         */
        @Deprecated
        @Readable
        public static final String WIFI_WATCHDOG_BACKGROUND_CHECK_DELAY_MS =
                Secure.WIFI_WATCHDOG_BACKGROUND_CHECK_DELAY_MS;

        /**
         * @deprecated Use
         * {@link android.provider.Settings.Secure#WIFI_WATCHDOG_BACKGROUND_CHECK_ENABLED} instead
         */
        @Deprecated
        @Readable
        public static final String WIFI_WATCHDOG_BACKGROUND_CHECK_ENABLED =
                Secure.WIFI_WATCHDOG_BACKGROUND_CHECK_ENABLED;

        /**
         * @deprecated Use
         * {@link android.provider.Settings.Secure#WIFI_WATCHDOG_BACKGROUND_CHECK_TIMEOUT_MS}
         * instead
         */
        @Deprecated
        @Readable
        public static final String WIFI_WATCHDOG_BACKGROUND_CHECK_TIMEOUT_MS =
                Secure.WIFI_WATCHDOG_BACKGROUND_CHECK_TIMEOUT_MS;

        /**
         * @deprecated Use
         * {@link android.provider.Settings.Secure#WIFI_WATCHDOG_INITIAL_IGNORED_PING_COUNT} instead
         */
        @Deprecated
        @Readable
        public static final String WIFI_WATCHDOG_INITIAL_IGNORED_PING_COUNT =
            Secure.WIFI_WATCHDOG_INITIAL_IGNORED_PING_COUNT;

        /**
         * @deprecated Use {@link android.provider.Settings.Secure#WIFI_WATCHDOG_MAX_AP_CHECKS}
         * instead
         */
        @Deprecated
        public static final String WIFI_WATCHDOG_MAX_AP_CHECKS = Secure.WIFI_WATCHDOG_MAX_AP_CHECKS;

        /**
         * @deprecated Use {@link android.provider.Settings.Global#WIFI_WATCHDOG_ON} instead
         */
        @Deprecated
        public static final String WIFI_WATCHDOG_ON = Global.WIFI_WATCHDOG_ON;

        /**
         * @deprecated Use {@link android.provider.Settings.Secure#WIFI_WATCHDOG_PING_COUNT} instead
         */
        @Deprecated
        public static final String WIFI_WATCHDOG_PING_COUNT = Secure.WIFI_WATCHDOG_PING_COUNT;

        /**
         * @deprecated Use {@link android.provider.Settings.Secure#WIFI_WATCHDOG_PING_DELAY_MS}
         * instead
         */
        @Deprecated
        public static final String WIFI_WATCHDOG_PING_DELAY_MS = Secure.WIFI_WATCHDOG_PING_DELAY_MS;

        /**
         * @deprecated Use {@link android.provider.Settings.Secure#WIFI_WATCHDOG_PING_TIMEOUT_MS}
         * instead
         */
        @Deprecated
        @Readable
        public static final String WIFI_WATCHDOG_PING_TIMEOUT_MS =
            Secure.WIFI_WATCHDOG_PING_TIMEOUT_MS;

        /**
         * Checks if the specified app can modify system settings. As of API
         * level 23, an app cannot modify system settings unless it declares the
         * {@link android.Manifest.permission#WRITE_SETTINGS}
         * permission in its manifest, <em>and</em> the user specifically grants
         * the app this capability. To prompt the user to grant this approval,
         * the app must send an intent with the action {@link
         * android.provider.Settings#ACTION_MANAGE_WRITE_SETTINGS}, which causes
         * the system to display a permission management screen.
         *
         * @param context App context.
         * @return true if the calling app can write to system settings, false otherwise
         */
        public static boolean canWrite(Context context) {
            return isCallingPackageAllowedToWriteSettings(context, Process.myUid(),
                    context.getOpPackageName(), false);
        }
    }

    /**
     * Secure system settings, containing system preferences that applications
     * can read but are not allowed to write.  These are for preferences that
     * the user must explicitly modify through the UI of a system app. Normal
     * applications cannot modify the secure settings database, either directly
     * or by calling the "put" methods that this class contains.
     */
    public static final class Secure extends NameValueTable {
        // NOTE: If you add new settings here, be sure to add them to
        // com.android.providers.settings.SettingsProtoDumpUtil#dumpProtoSecureSettingsLocked.

        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI =
            Uri.parse("content://" + AUTHORITY + "/secure");

        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        private static final ContentProviderHolder sProviderHolder =
                new ContentProviderHolder(CONTENT_URI);

        // Populated lazily, guarded by class object:
        @UnsupportedAppUsage
        private static final NameValueCache sNameValueCache = new NameValueCache(
                CONTENT_URI,
                CALL_METHOD_GET_SECURE,
                CALL_METHOD_PUT_SECURE,
                CALL_METHOD_DELETE_SECURE,
                sProviderHolder,
                Secure.class);

        @UnsupportedAppUsage
        private static final HashSet<String> MOVED_TO_LOCK_SETTINGS;
        @UnsupportedAppUsage
        private static final HashSet<String> MOVED_TO_GLOBAL;
        static {
            MOVED_TO_LOCK_SETTINGS = new HashSet<>(6);
            MOVED_TO_LOCK_SETTINGS.add(Secure.LOCK_PATTERN_ENABLED);
            MOVED_TO_LOCK_SETTINGS.add(Secure.LOCK_PATTERN_VISIBLE);
            MOVED_TO_LOCK_SETTINGS.add(Secure.LOCK_PATTERN_TACTILE_FEEDBACK_ENABLED);
            MOVED_TO_LOCK_SETTINGS.add(Secure.LOCK_PATTERN_SIZE);
            MOVED_TO_LOCK_SETTINGS.add(Secure.LOCK_DOTS_VISIBLE);
            MOVED_TO_LOCK_SETTINGS.add(Secure.LOCK_SHOW_ERROR_PATH);

            MOVED_TO_GLOBAL = new HashSet<>();
            MOVED_TO_GLOBAL.add(Settings.Global.ADB_ENABLED);
            MOVED_TO_GLOBAL.add(Settings.Global.ASSISTED_GPS_ENABLED);
            MOVED_TO_GLOBAL.add(Settings.Global.BLUETOOTH_ON);
            MOVED_TO_GLOBAL.add(Settings.Global.CDMA_CELL_BROADCAST_SMS);
            MOVED_TO_GLOBAL.add(Settings.Global.CDMA_ROAMING_MODE);
            MOVED_TO_GLOBAL.add(Settings.Global.CDMA_SUBSCRIPTION_MODE);
            MOVED_TO_GLOBAL.add(Settings.Global.DATA_ACTIVITY_TIMEOUT_MOBILE);
            MOVED_TO_GLOBAL.add(Settings.Global.DATA_ACTIVITY_TIMEOUT_WIFI);
            MOVED_TO_GLOBAL.add(Settings.Global.DATA_ROAMING);
            MOVED_TO_GLOBAL.add(Settings.Global.DEVELOPMENT_SETTINGS_ENABLED);
            MOVED_TO_GLOBAL.add(Settings.Global.DEVICE_PROVISIONED);
            MOVED_TO_GLOBAL.add(Settings.Global.DISPLAY_SIZE_FORCED);
            MOVED_TO_GLOBAL.add(Settings.Global.DOWNLOAD_MAX_BYTES_OVER_MOBILE);
            MOVED_TO_GLOBAL.add(Settings.Global.DOWNLOAD_RECOMMENDED_MAX_BYTES_OVER_MOBILE);
            MOVED_TO_GLOBAL.add(Settings.Global.MOBILE_DATA);
            MOVED_TO_GLOBAL.add(Settings.Global.NETSTATS_DEV_BUCKET_DURATION);
            MOVED_TO_GLOBAL.add(Settings.Global.NETSTATS_DEV_DELETE_AGE);
            MOVED_TO_GLOBAL.add(Settings.Global.NETSTATS_DEV_PERSIST_BYTES);
            MOVED_TO_GLOBAL.add(Settings.Global.NETSTATS_DEV_ROTATE_AGE);
            MOVED_TO_GLOBAL.add(Settings.Global.NETSTATS_ENABLED);
            MOVED_TO_GLOBAL.add(Settings.Global.NETSTATS_GLOBAL_ALERT_BYTES);
            MOVED_TO_GLOBAL.add(Settings.Global.NETSTATS_POLL_INTERVAL);
            MOVED_TO_GLOBAL.add(Settings.Global.NETSTATS_SAMPLE_ENABLED);
            MOVED_TO_GLOBAL.add(Settings.Global.NETSTATS_TIME_CACHE_MAX_AGE);
            MOVED_TO_GLOBAL.add(Settings.Global.NETSTATS_UID_BUCKET_DURATION);
            MOVED_TO_GLOBAL.add(Settings.Global.NETSTATS_UID_DELETE_AGE);
            MOVED_TO_GLOBAL.add(Settings.Global.NETSTATS_UID_PERSIST_BYTES);
            MOVED_TO_GLOBAL.add(Settings.Global.NETSTATS_UID_ROTATE_AGE);
            MOVED_TO_GLOBAL.add(Settings.Global.NETSTATS_UID_TAG_BUCKET_DURATION);
            MOVED_TO_GLOBAL.add(Settings.Global.NETSTATS_UID_TAG_DELETE_AGE);
            MOVED_TO_GLOBAL.add(Settings.Global.NETSTATS_UID_TAG_PERSIST_BYTES);
            MOVED_TO_GLOBAL.add(Settings.Global.NETSTATS_UID_TAG_ROTATE_AGE);
            MOVED_TO_GLOBAL.add(Settings.Global.NETWORK_PREFERENCE);
            MOVED_TO_GLOBAL.add(Settings.Global.NITZ_UPDATE_DIFF);
            MOVED_TO_GLOBAL.add(Settings.Global.NITZ_UPDATE_SPACING);
            MOVED_TO_GLOBAL.add(Settings.Global.NTP_SERVER);
            MOVED_TO_GLOBAL.add(Settings.Global.NTP_TIMEOUT);
            MOVED_TO_GLOBAL.add(Settings.Global.PDP_WATCHDOG_ERROR_POLL_COUNT);
            MOVED_TO_GLOBAL.add(Settings.Global.PDP_WATCHDOG_LONG_POLL_INTERVAL_MS);
            MOVED_TO_GLOBAL.add(Settings.Global.PDP_WATCHDOG_MAX_PDP_RESET_FAIL_COUNT);
            MOVED_TO_GLOBAL.add(Settings.Global.PDP_WATCHDOG_POLL_INTERVAL_MS);
            MOVED_TO_GLOBAL.add(Settings.Global.PDP_WATCHDOG_TRIGGER_PACKET_COUNT);
            MOVED_TO_GLOBAL.add(Settings.Global.SETUP_PREPAID_DATA_SERVICE_URL);
            MOVED_TO_GLOBAL.add(Settings.Global.SETUP_PREPAID_DETECTION_REDIR_HOST);
            MOVED_TO_GLOBAL.add(Settings.Global.SETUP_PREPAID_DETECTION_TARGET_URL);
            MOVED_TO_GLOBAL.add(Settings.Global.TETHER_DUN_APN);
            MOVED_TO_GLOBAL.add(Settings.Global.TETHER_DUN_REQUIRED);
            MOVED_TO_GLOBAL.add(Settings.Global.TETHER_SUPPORTED);
            MOVED_TO_GLOBAL.add(Settings.Global.USB_MASS_STORAGE_ENABLED);
            MOVED_TO_GLOBAL.add(Settings.Global.USE_GOOGLE_MAIL);
            MOVED_TO_GLOBAL.add(Settings.Global.WIFI_COUNTRY_CODE);
            MOVED_TO_GLOBAL.add(Settings.Global.WIFI_FRAMEWORK_SCAN_INTERVAL_MS);
            MOVED_TO_GLOBAL.add(Settings.Global.WIFI_FREQUENCY_BAND);
            MOVED_TO_GLOBAL.add(Settings.Global.WIFI_IDLE_MS);
            MOVED_TO_GLOBAL.add(Settings.Global.WIFI_MAX_DHCP_RETRY_COUNT);
            MOVED_TO_GLOBAL.add(Settings.Global.WIFI_MOBILE_DATA_TRANSITION_WAKELOCK_TIMEOUT_MS);
            MOVED_TO_GLOBAL.add(Settings.Global.WIFI_NETWORKS_AVAILABLE_NOTIFICATION_ON);
            MOVED_TO_GLOBAL.add(Settings.Global.WIFI_NETWORKS_AVAILABLE_REPEAT_DELAY);
            MOVED_TO_GLOBAL.add(Settings.Global.WIFI_NUM_OPEN_NETWORKS_KEPT);
            MOVED_TO_GLOBAL.add(Settings.Global.WIFI_ON);
            MOVED_TO_GLOBAL.add(Settings.Global.WIFI_P2P_DEVICE_NAME);
            MOVED_TO_GLOBAL.add(Settings.Global.WIFI_SUPPLICANT_SCAN_INTERVAL_MS);
            MOVED_TO_GLOBAL.add(Settings.Global.WIFI_VERBOSE_LOGGING_ENABLED);
            MOVED_TO_GLOBAL.add(Settings.Global.WIFI_ENHANCED_AUTO_JOIN);
            MOVED_TO_GLOBAL.add(Settings.Global.WIFI_NETWORK_SHOW_RSSI);
            MOVED_TO_GLOBAL.add(Settings.Global.WIFI_WATCHDOG_ON);
            MOVED_TO_GLOBAL.add(Settings.Global.WIFI_WATCHDOG_POOR_NETWORK_TEST_ENABLED);
            MOVED_TO_GLOBAL.add(Settings.Global.WIFI_P2P_PENDING_FACTORY_RESET);
            MOVED_TO_GLOBAL.add(Settings.Global.WIMAX_NETWORKS_AVAILABLE_NOTIFICATION_ON);
            MOVED_TO_GLOBAL.add(Settings.Global.PACKAGE_VERIFIER_TIMEOUT);
            MOVED_TO_GLOBAL.add(Settings.Global.PACKAGE_VERIFIER_DEFAULT_RESPONSE);
            MOVED_TO_GLOBAL.add(Settings.Global.DATA_STALL_ALARM_NON_AGGRESSIVE_DELAY_IN_MS);
            MOVED_TO_GLOBAL.add(Settings.Global.DATA_STALL_ALARM_AGGRESSIVE_DELAY_IN_MS);
            MOVED_TO_GLOBAL.add(Settings.Global.GPRS_REGISTER_CHECK_PERIOD_MS);
            MOVED_TO_GLOBAL.add(Settings.Global.WTF_IS_FATAL);
            MOVED_TO_GLOBAL.add(Settings.Global.BATTERY_DISCHARGE_DURATION_THRESHOLD);
            MOVED_TO_GLOBAL.add(Settings.Global.BATTERY_DISCHARGE_THRESHOLD);
            MOVED_TO_GLOBAL.add(Settings.Global.SEND_ACTION_APP_ERROR);
            MOVED_TO_GLOBAL.add(Settings.Global.DROPBOX_AGE_SECONDS);
            MOVED_TO_GLOBAL.add(Settings.Global.DROPBOX_MAX_FILES);
            MOVED_TO_GLOBAL.add(Settings.Global.DROPBOX_QUOTA_KB);
            MOVED_TO_GLOBAL.add(Settings.Global.DROPBOX_QUOTA_PERCENT);
            MOVED_TO_GLOBAL.add(Settings.Global.DROPBOX_RESERVE_PERCENT);
            MOVED_TO_GLOBAL.add(Settings.Global.DROPBOX_TAG_PREFIX);
            MOVED_TO_GLOBAL.add(Settings.Global.ERROR_LOGCAT_PREFIX);
            MOVED_TO_GLOBAL.add(Settings.Global.SYS_FREE_STORAGE_LOG_INTERVAL);
            MOVED_TO_GLOBAL.add(Settings.Global.DISK_FREE_CHANGE_REPORTING_THRESHOLD);
            MOVED_TO_GLOBAL.add(Settings.Global.SYS_STORAGE_THRESHOLD_PERCENTAGE);
            MOVED_TO_GLOBAL.add(Settings.Global.SYS_STORAGE_THRESHOLD_MAX_BYTES);
            MOVED_TO_GLOBAL.add(Settings.Global.SYS_STORAGE_FULL_THRESHOLD_BYTES);
            MOVED_TO_GLOBAL.add(Settings.Global.SYNC_MAX_RETRY_DELAY_IN_SECONDS);
            MOVED_TO_GLOBAL.add(Settings.Global.CONNECTIVITY_CHANGE_DELAY);
            MOVED_TO_GLOBAL.add(Settings.Global.CAPTIVE_PORTAL_DETECTION_ENABLED);
            MOVED_TO_GLOBAL.add(Settings.Global.CAPTIVE_PORTAL_SERVER);
            MOVED_TO_GLOBAL.add(Settings.Global.SET_INSTALL_LOCATION);
            MOVED_TO_GLOBAL.add(Settings.Global.DEFAULT_INSTALL_LOCATION);
            MOVED_TO_GLOBAL.add(Settings.Global.INET_CONDITION_DEBOUNCE_UP_DELAY);
            MOVED_TO_GLOBAL.add(Settings.Global.INET_CONDITION_DEBOUNCE_DOWN_DELAY);
            MOVED_TO_GLOBAL.add(Settings.Global.READ_EXTERNAL_STORAGE_ENFORCED_DEFAULT);
            MOVED_TO_GLOBAL.add(Settings.Global.HTTP_PROXY);
            MOVED_TO_GLOBAL.add(Settings.Global.GLOBAL_HTTP_PROXY_HOST);
            MOVED_TO_GLOBAL.add(Settings.Global.GLOBAL_HTTP_PROXY_PORT);
            MOVED_TO_GLOBAL.add(Settings.Global.GLOBAL_HTTP_PROXY_EXCLUSION_LIST);
            MOVED_TO_GLOBAL.add(Settings.Global.SET_GLOBAL_HTTP_PROXY);
            MOVED_TO_GLOBAL.add(Settings.Global.DEFAULT_DNS_SERVER);
            MOVED_TO_GLOBAL.add(Settings.Global.PREFERRED_NETWORK_MODE);
            MOVED_TO_GLOBAL.add(Settings.Global.WEBVIEW_DATA_REDUCTION_PROXY_KEY);
            MOVED_TO_GLOBAL.add(Settings.Global.SECURE_FRP_MODE);
        }

        /** @hide */
        public static void getMovedToGlobalSettings(Set<String> outKeySet) {
            outKeySet.addAll(MOVED_TO_GLOBAL);
        }

        /** @hide */
        public static void getMovedToSystemSettings(Set<String> outKeySet) {
        }

        /** @hide */
        public static void clearProviderForTest() {
            sProviderHolder.clearProviderForTest();
            sNameValueCache.clearGenerationTrackerForTest();
        }

        /** @hide */
        public static void getPublicSettings(Set<String> allKeys, Set<String> readableKeys,
                ArrayMap<String, Integer> readableKeysWithMaxTargetSdk) {
            getPublicSettingsForClass(Secure.class, allKeys, readableKeys,
                    readableKeysWithMaxTargetSdk);
        }

        /**
         * Look up a name in the database.
         * @param resolver to access the database with
         * @param name to look up in the table
         * @return the corresponding value, or null if not present
         */
        public static String getString(ContentResolver resolver, String name) {
            return getStringForUser(resolver, name, resolver.getUserId());
        }

        /** @hide */
        @UnsupportedAppUsage
        public static String getStringForUser(ContentResolver resolver, String name,
                int userHandle) {
            if (MOVED_TO_GLOBAL.contains(name)) {
                Log.w(TAG, "Setting " + name + " has moved from android.provider.Settings.Secure"
                        + " to android.provider.Settings.Global.");
                return Global.getStringForUser(resolver, name, userHandle);
            }

            if (MOVED_TO_LOCK_SETTINGS.contains(name) && Process.myUid() != Process.SYSTEM_UID) {
                // No context; use the ActivityThread's context as an approximation for
                // determining the target API level.
                Application application = ActivityThread.currentApplication();

                boolean isPreMnc = application != null
                    && application.getApplicationInfo() != null
                    && application.getApplicationInfo().targetSdkVersion
                    <= VERSION_CODES.LOLLIPOP_MR1;
                if (isPreMnc) {
                    // Old apps used to get the three deprecated LOCK_PATTERN_* settings from
                    // ILockSettings.getString().  For security reasons, we now just return a
                    // stubbed-out value.  Note: the only one of these three settings actually known
                    // to have been used was LOCK_PATTERN_ENABLED, and ILockSettings.getString()
                    // already always returned "0" for that starting in Android 11.
                    return "0";
                }
                throw new SecurityException("Settings.Secure." + name + " is deprecated and no" +
                        " longer accessible. See API documentation for potential replacements.");
            }

            return sNameValueCache.getStringForUser(resolver, name, userHandle);
        }

        /**
         * Store a name/value pair into the database. Values written by this method will be
         * overridden if a restore happens in the future.
         *
         * @param resolver to access the database with
         * @param name to store
         * @param value to associate with the name
         * @return true if the value was set, false on database errors
         *
         * @hide
         */
        @RequiresPermission(Manifest.permission.MODIFY_SETTINGS_OVERRIDEABLE_BY_RESTORE)
        public static boolean putString(ContentResolver resolver, String name,
                String value, boolean overrideableByRestore) {
            return putStringForUser(resolver, name, value, /* tag */ null, /* makeDefault */ false,
                    resolver.getUserId(), overrideableByRestore);
        }

        /**
         * Store a name/value pair into the database.
         * @param resolver to access the database with
         * @param name to store
         * @param value to associate with the name
         * @return true if the value was set, false on database errors
         */
        public static boolean putString(ContentResolver resolver, String name, String value) {
            return putStringForUser(resolver, name, value, resolver.getUserId());
        }

        /** @hide */
        @UnsupportedAppUsage
        public static boolean putStringForUser(ContentResolver resolver, String name, String value,
                int userHandle) {
            return putStringForUser(resolver, name, value, null, false, userHandle,
                    DEFAULT_OVERRIDEABLE_BY_RESTORE);
        }

        /** @hide */
        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        public static boolean putStringForUser(@NonNull ContentResolver resolver,
                @NonNull String name, @Nullable String value, @Nullable String tag,
                boolean makeDefault, @UserIdInt int userHandle, boolean overrideableByRestore) {
            if (LOCAL_LOGV) {
                Log.v(TAG, "Secure.putString(name=" + name + ", value=" + value + ") for "
                        + userHandle);
            }
            if (MOVED_TO_GLOBAL.contains(name)) {
                Log.w(TAG, "Setting " + name + " has moved from android.provider.Settings.Secure"
                        + " to android.provider.Settings.Global");
                return Global.putStringForUser(resolver, name, value,
                        tag, makeDefault, userHandle, DEFAULT_OVERRIDEABLE_BY_RESTORE);
            }
            return sNameValueCache.putStringForUser(resolver, name, value, tag,
                    makeDefault, userHandle, overrideableByRestore);
        }

        /**
         * Store a name/value pair into the database.
         * <p>
         * The method takes an optional tag to associate with the setting
         * which can be used to clear only settings made by your package and
         * associated with this tag by passing the tag to {@link
         * #resetToDefaults(ContentResolver, String)}. Anyone can override
         * the current tag. Also if another package changes the setting
         * then the tag will be set to the one specified in the set call
         * which can be null. Also any of the settings setters that do not
         * take a tag as an argument effectively clears the tag.
         * </p><p>
         * For example, if you set settings A and B with tags T1 and T2 and
         * another app changes setting A (potentially to the same value), it
         * can assign to it a tag T3 (note that now the package that changed
         * the setting is not yours). Now if you reset your changes for T1 and
         * T2 only setting B will be reset and A not (as it was changed by
         * another package) but since A did not change you are in the desired
         * initial state. Now if the other app changes the value of A (assuming
         * you registered an observer in the beginning) you would detect that
         * the setting was changed by another app and handle this appropriately
         * (ignore, set back to some value, etc).
         * </p><p>
         * Also the method takes an argument whether to make the value the
         * default for this setting. If the system already specified a default
         * value, then the one passed in here will <strong>not</strong>
         * be set as the default.
         * </p>
         *
         * @param resolver to access the database with.
         * @param name to store.
         * @param value to associate with the name.
         * @param tag to associate with the setting.
         * @param makeDefault whether to make the value the default one.
         * @return true if the value was set, false on database errors.
         *
         * @see #resetToDefaults(ContentResolver, String)
         *
         * @hide
         */
        @SystemApi
        @RequiresPermission(Manifest.permission.WRITE_SECURE_SETTINGS)
        public static boolean putString(@NonNull ContentResolver resolver,
                @NonNull String name, @Nullable String value, @Nullable String tag,
                boolean makeDefault) {
            return putStringForUser(resolver, name, value, tag, makeDefault,
                    resolver.getUserId(), DEFAULT_OVERRIDEABLE_BY_RESTORE);
        }

        /**
         * Reset the settings to their defaults. This would reset <strong>only</strong>
         * settings set by the caller's package. Think of it of a way to undo your own
         * changes to the global settings. Passing in the optional tag will reset only
         * settings changed by your package and associated with this tag.
         *
         * @param resolver Handle to the content resolver.
         * @param tag Optional tag which should be associated with the settings to reset.
         *
         * @see #putString(ContentResolver, String, String, String, boolean)
         *
         * @hide
         */
        @SystemApi
        @RequiresPermission(Manifest.permission.WRITE_SECURE_SETTINGS)
        public static void resetToDefaults(@NonNull ContentResolver resolver,
                @Nullable String tag) {
            resetToDefaultsAsUser(resolver, tag, RESET_MODE_PACKAGE_DEFAULTS,
                    resolver.getUserId());
        }

        /**
         *
         * Reset the settings to their defaults for a given user with a specific mode. The
         * optional tag argument is valid only for {@link #RESET_MODE_PACKAGE_DEFAULTS}
         * allowing resetting the settings made by a package and associated with the tag.
         *
         * @param resolver Handle to the content resolver.
         * @param tag Optional tag which should be associated with the settings to reset.
         * @param mode The reset mode.
         * @param userHandle The user for which to reset to defaults.
         *
         * @see #RESET_MODE_PACKAGE_DEFAULTS
         * @see #RESET_MODE_UNTRUSTED_DEFAULTS
         * @see #RESET_MODE_UNTRUSTED_CHANGES
         * @see #RESET_MODE_TRUSTED_DEFAULTS
         *
         * @hide
         */
        public static void resetToDefaultsAsUser(@NonNull ContentResolver resolver,
                @Nullable String tag, @ResetMode int mode, @IntRange(from = 0) int userHandle) {
            try {
                Bundle arg = new Bundle();
                arg.putInt(CALL_METHOD_USER_KEY, userHandle);
                if (tag != null) {
                    arg.putString(CALL_METHOD_TAG_KEY, tag);
                }
                arg.putInt(CALL_METHOD_RESET_MODE_KEY, mode);
                IContentProvider cp = sProviderHolder.getProvider(resolver);
                cp.call(resolver.getAttributionSource(),
                        sProviderHolder.mUri.getAuthority(), CALL_METHOD_RESET_SECURE, null, arg);
            } catch (RemoteException e) {
                Log.w(TAG, "Can't reset do defaults for " + CONTENT_URI, e);
            }
        }

        /**
         * Construct the content URI for a particular name/value pair,
         * useful for monitoring changes with a ContentObserver.
         * @param name to look up in the table
         * @return the corresponding content URI, or null if not present
         */
        public static Uri getUriFor(String name) {
            if (MOVED_TO_GLOBAL.contains(name)) {
                Log.w(TAG, "Setting " + name + " has moved from android.provider.Settings.Secure"
                        + " to android.provider.Settings.Global, returning global URI.");
                return Global.getUriFor(Global.CONTENT_URI, name);
            }
            return getUriFor(CONTENT_URI, name);
        }

        /**
         * Convenience function for retrieving a single secure settings value
         * as an integer.  Note that internally setting values are always
         * stored as strings; this function converts the string to an integer
         * for you.  The default value will be returned if the setting is
         * not defined or not an integer.
         *
         * @param cr The ContentResolver to access.
         * @param name The name of the setting to retrieve.
         * @param def Value to return if the setting is not defined.
         *
         * @return The setting's current value, or 'def' if it is not defined
         * or not a valid integer.
         */
        public static int getInt(ContentResolver cr, String name, int def) {
            return getIntForUser(cr, name, def, cr.getUserId());
        }

        /** @hide */
        @UnsupportedAppUsage
        public static int getIntForUser(ContentResolver cr, String name, int def, int userHandle) {
            String v = getStringForUser(cr, name, userHandle);
            return parseIntSettingWithDefault(v, def);
        }

        /**
         * Convenience function for retrieving a single secure settings value
         * as an integer.  Note that internally setting values are always
         * stored as strings; this function converts the string to an integer
         * for you.
         * <p>
         * This version does not take a default value.  If the setting has not
         * been set, or the string value is not a number,
         * it throws {@link SettingNotFoundException}.
         *
         * @param cr The ContentResolver to access.
         * @param name The name of the setting to retrieve.
         *
         * @throws SettingNotFoundException Thrown if a setting by the given
         * name can't be found or the setting value is not an integer.
         *
         * @return The setting's current value.
         */
        public static int getInt(ContentResolver cr, String name)
                throws SettingNotFoundException {
            return getIntForUser(cr, name, cr.getUserId());
        }

        /** @hide */
        public static int getIntForUser(ContentResolver cr, String name, int userHandle)
                throws SettingNotFoundException {
            String v = getStringForUser(cr, name, userHandle);
            return parseIntSetting(v, name);
        }

        /**
         * Convenience function for updating a single settings value as an
         * integer. This will either create a new entry in the table if the
         * given name does not exist, or modify the value of the existing row
         * with that name.  Note that internally setting values are always
         * stored as strings, so this function converts the given value to a
         * string before storing it.
         *
         * @param cr The ContentResolver to access.
         * @param name The name of the setting to modify.
         * @param value The new value for the setting.
         * @return true if the value was set, false on database errors
         */
        public static boolean putInt(ContentResolver cr, String name, int value) {
            return putIntForUser(cr, name, value, cr.getUserId());
        }

        /** @hide */
        @UnsupportedAppUsage
        public static boolean putIntForUser(ContentResolver cr, String name, int value,
                int userHandle) {
            return putStringForUser(cr, name, Integer.toString(value), userHandle);
        }

        /**
         * Convenience function for retrieving a single secure settings value
         * as a {@code long}.  Note that internally setting values are always
         * stored as strings; this function converts the string to a {@code long}
         * for you.  The default value will be returned if the setting is
         * not defined or not a {@code long}.
         *
         * @param cr The ContentResolver to access.
         * @param name The name of the setting to retrieve.
         * @param def Value to return if the setting is not defined.
         *
         * @return The setting's current value, or 'def' if it is not defined
         * or not a valid {@code long}.
         */
        public static long getLong(ContentResolver cr, String name, long def) {
            return getLongForUser(cr, name, def, cr.getUserId());
        }

        /** @hide */
        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        public static long getLongForUser(ContentResolver cr, String name, long def,
                int userHandle) {
            String v = getStringForUser(cr, name, userHandle);
            return parseLongSettingWithDefault(v, def);
        }

        /**
         * Convenience function for retrieving a single secure settings value
         * as a {@code long}.  Note that internally setting values are always
         * stored as strings; this function converts the string to a {@code long}
         * for you.
         * <p>
         * This version does not take a default value.  If the setting has not
         * been set, or the string value is not a number,
         * it throws {@link SettingNotFoundException}.
         *
         * @param cr The ContentResolver to access.
         * @param name The name of the setting to retrieve.
         *
         * @return The setting's current value.
         * @throws SettingNotFoundException Thrown if a setting by the given
         * name can't be found or the setting value is not an integer.
         */
        public static long getLong(ContentResolver cr, String name)
                throws SettingNotFoundException {
            return getLongForUser(cr, name, cr.getUserId());
        }

        /** @hide */
        public static long getLongForUser(ContentResolver cr, String name, int userHandle)
                throws SettingNotFoundException {
            String v = getStringForUser(cr, name, userHandle);
            return parseLongSetting(v, name);
        }

        /**
         * Convenience function for updating a secure settings value as a long
         * integer. This will either create a new entry in the table if the
         * given name does not exist, or modify the value of the existing row
         * with that name.  Note that internally setting values are always
         * stored as strings, so this function converts the given value to a
         * string before storing it.
         *
         * @param cr The ContentResolver to access.
         * @param name The name of the setting to modify.
         * @param value The new value for the setting.
         * @return true if the value was set, false on database errors
         */
        public static boolean putLong(ContentResolver cr, String name, long value) {
            return putLongForUser(cr, name, value, cr.getUserId());
        }

        /** @hide */
        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        public static boolean putLongForUser(ContentResolver cr, String name, long value,
                int userHandle) {
            return putStringForUser(cr, name, Long.toString(value), userHandle);
        }

        /**
         * Convenience function for retrieving a single secure settings value
         * as a floating point number.  Note that internally setting values are
         * always stored as strings; this function converts the string to an
         * float for you. The default value will be returned if the setting
         * is not defined or not a valid float.
         *
         * @param cr The ContentResolver to access.
         * @param name The name of the setting to retrieve.
         * @param def Value to return if the setting is not defined.
         *
         * @return The setting's current value, or 'def' if it is not defined
         * or not a valid float.
         */
        public static float getFloat(ContentResolver cr, String name, float def) {
            return getFloatForUser(cr, name, def, cr.getUserId());
        }

        /** @hide */
        public static float getFloatForUser(ContentResolver cr, String name, float def,
                int userHandle) {
            String v = getStringForUser(cr, name, userHandle);
            return parseFloatSettingWithDefault(v, def);
        }

        /**
         * Convenience function for retrieving a single secure settings value
         * as a float.  Note that internally setting values are always
         * stored as strings; this function converts the string to a float
         * for you.
         * <p>
         * This version does not take a default value.  If the setting has not
         * been set, or the string value is not a number,
         * it throws {@link SettingNotFoundException}.
         *
         * @param cr The ContentResolver to access.
         * @param name The name of the setting to retrieve.
         *
         * @throws SettingNotFoundException Thrown if a setting by the given
         * name can't be found or the setting value is not a float.
         *
         * @return The setting's current value.
         */
        public static float getFloat(ContentResolver cr, String name)
                throws SettingNotFoundException {
            return getFloatForUser(cr, name, cr.getUserId());
        }

        /** @hide */
        public static float getFloatForUser(ContentResolver cr, String name, int userHandle)
                throws SettingNotFoundException {
            String v = getStringForUser(cr, name, userHandle);
            return parseFloatSetting(v, name);
        }

        /**
         * Convenience function for updating a single settings value as a
         * floating point number. This will either create a new entry in the
         * table if the given name does not exist, or modify the value of the
         * existing row with that name.  Note that internally setting values
         * are always stored as strings, so this function converts the given
         * value to a string before storing it.
         *
         * @param cr The ContentResolver to access.
         * @param name The name of the setting to modify.
         * @param value The new value for the setting.
         * @return true if the value was set, false on database errors
         */
        public static boolean putFloat(ContentResolver cr, String name, float value) {
            return putFloatForUser(cr, name, value, cr.getUserId());
        }

        /** @hide */
        public static boolean putFloatForUser(ContentResolver cr, String name, float value,
                int userHandle) {
            return putStringForUser(cr, name, Float.toString(value), userHandle);
        }

        /**
         * Control whether to enable adaptive sleep mode.
         * @hide
         */
        @Readable
        public static final String ADAPTIVE_SLEEP = "adaptive_sleep";

        /**
         * Setting key to indicate whether camera-based autorotate is enabled.
         *
         * @hide
         */
        public static final String CAMERA_AUTOROTATE = "camera_autorotate";

        /**
         * @deprecated Use {@link android.provider.Settings.Global#DEVELOPMENT_SETTINGS_ENABLED}
         * instead
         */
        @Deprecated
        public static final String DEVELOPMENT_SETTINGS_ENABLED =
                Global.DEVELOPMENT_SETTINGS_ENABLED;

        /**
         * When the user has enable the option to have a "bug report" command
         * in the power menu.
         * @hide
         */
        @Readable
        public static final String BUGREPORT_IN_POWER_MENU = "bugreport_in_power_menu";

        /**
         * The package name for the custom bugreport handler app. This app must be bugreport
         * allow-listed. This is currently used only by Power Menu short press.
         * @hide
         */
        public static final String CUSTOM_BUGREPORT_HANDLER_APP = "custom_bugreport_handler_app";

        /**
         * The user id for the custom bugreport handler app. This is currently used only by Power
         * Menu short press.
         * @hide
         */
        public static final String CUSTOM_BUGREPORT_HANDLER_USER = "custom_bugreport_handler_user";

        /**
         * @deprecated Use {@link android.provider.Settings.Global#ADB_ENABLED} instead
         */
        @Deprecated
        public static final String ADB_ENABLED = Global.ADB_ENABLED;

        /**
         * Setting to allow mock locations and location provider status to be injected into the
         * LocationManager service for testing purposes during application development.  These
         * locations and status values  override actual location and status information generated
         * by network, gps, or other location providers.
         *
         * @deprecated This settings is not used anymore.
         */
        @Deprecated
        @Readable
        public static final String ALLOW_MOCK_LOCATION = "mock_location";

        /**
         * This is used by Bluetooth Manager to store adapter name
         * @hide
         */
        @Readable(maxTargetSdk = Build.VERSION_CODES.S)
        @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
        @SuppressLint("NoSettingsProvider")
        public static final String BLUETOOTH_NAME = "bluetooth_name";

        /**
         * This is used by Bluetooth Manager to store adapter address
         * @hide
         */
        @Readable(maxTargetSdk = Build.VERSION_CODES.S)
        @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
        @SuppressLint("NoSettingsProvider")
        public static final String BLUETOOTH_ADDRESS = "bluetooth_address";

        /**
         * This is used by Bluetooth Manager to store whether adapter address is valid
         * @hide
         */
        @Readable(maxTargetSdk = Build.VERSION_CODES.S)
        @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
        @SuppressLint("NoSettingsProvider")
        public static final String BLUETOOTH_ADDR_VALID = "bluetooth_addr_valid";

        /**
         * This is used by LocalBluetoothLeBroadcast to store the broadcast program info.
         * @hide
         */
        public static final String BLUETOOTH_LE_BROADCAST_PROGRAM_INFO =
                "bluetooth_le_broadcast_program_info";

        /**
         * This is used by LocalBluetoothLeBroadcast to store the broadcast code.
         * @hide
         */
        public static final String BLUETOOTH_LE_BROADCAST_CODE = "bluetooth_le_broadcast_code";

        /**
         * This is used by LocalBluetoothLeBroadcast to store the app source name.
         * @hide
         */
        public static final String BLUETOOTH_LE_BROADCAST_APP_SOURCE_NAME =
                "bluetooth_le_broadcast_app_source_name";

        /**
         * This is used by LocalBluetoothLeBroadcast to downgrade the broadcast quality to improve
         * compatibility.
         *
         * <ul>
         *   <li>0 = false
         *   <li>1 = true
         * </ul>
         *
         * @hide
         */
        public static final String BLUETOOTH_LE_BROADCAST_IMPROVE_COMPATIBILITY =
                "bluetooth_le_broadcast_improve_compatibility";

        /**
         * This is used by LocalBluetoothLeBroadcast to store the fallback active device address.
         *
         * @hide
         */
        public static final String BLUETOOTH_LE_BROADCAST_FALLBACK_ACTIVE_DEVICE_ADDRESS =
                "bluetooth_le_broadcast_fallback_active_device_address";

        /**
         * Ringtone routing value for hearing aid. It routes ringtone to hearing aid or device
         * speaker.
         * <ul>
         *     <li> 0 = Default
         *     <li> 1 = Route to hearing aid
         *     <li> 2 = Route to device speaker
         * </ul>
         * @hide
         */
        public static final String HEARING_AID_RINGTONE_ROUTING =
                "hearing_aid_ringtone_routing";

        /**
         * Phone call routing value for hearing aid. It routes phone call to hearing aid or
         * device speaker.
         * <ul>
         *     <li> 0 = Default
         *     <li> 1 = Route to hearing aid
         *     <li> 2 = Route to device speaker
         * </ul>
         * @hide
         */
        public static final String HEARING_AID_CALL_ROUTING =
                "hearing_aid_call_routing";

        /**
         * Media routing value for hearing aid. It routes media to hearing aid or device
         * speaker.
         * <ul>
         *     <li> 0 = Default
         *     <li> 1 = Route to hearing aid
         *     <li> 2 = Route to device speaker
         * </ul>
         * @hide
         */
        public static final String HEARING_AID_MEDIA_ROUTING =
                "hearing_aid_media_routing";

        /**
         * Notification routing value for hearing aid. It routes notification sounds to hearing aid
         * or device speaker.
         * <ul>
         *     <li> 0 = Default
         *     <li> 1 = Route to hearing aid
         *     <li> 2 = Route to device speaker
         * </ul>
         * @hide
         */
        public static final String HEARING_AID_NOTIFICATION_ROUTING =
                "hearing_aid_notification_routing";

        /**
         * Setting to indicate that on device captions are enabled.
         *
         * @hide
         */
        @SystemApi
        @Readable
        public static final String ODI_CAPTIONS_ENABLED = "odi_captions_enabled";


        /**
         * Setting to indicate live caption button show or hide in the volume
         * rocker.
         *
         * @hide
         */
        public static final String ODI_CAPTIONS_VOLUME_UI_ENABLED =
                "odi_captions_volume_ui_enabled";

        /**
         * On Android 8.0 (API level 26) and higher versions of the platform,
         * a 64-bit number (expressed as a hexadecimal string), unique to
         * each combination of app-signing key, user, and device.
         * Values of {@code ANDROID_ID} are scoped by signing key and user.
         * The value may change if a factory reset is performed on the
         * device or if an APK signing key changes.
         *
         * For more information about how the platform handles {@code ANDROID_ID}
         * in Android 8.0 (API level 26) and higher, see <a
         * href="{@docRoot}about/versions/oreo/android-8.0-changes.html#privacy-all">
         * Android 8.0 Behavior Changes</a>.
         *
         * <p class="note"><strong>Note:</strong> For apps that were installed
         * prior to updating the device to a version of Android 8.0
         * (API level 26) or higher, the value of {@code ANDROID_ID} changes
         * if the app is uninstalled and then reinstalled after the OTA.
         * To preserve values across uninstalls after an OTA to Android 8.0
         * or higher, developers can use
         * <a href="{@docRoot}guide/topics/data/keyvaluebackup.html">
         * Key/Value Backup</a>.</p>
         *
         * <p>In versions of the platform lower than Android 8.0 (API level 26),
         * a 64-bit number (expressed as a hexadecimal string) that is randomly
         * generated when the user first sets up the device and should remain
         * constant for the lifetime of the user's device.
         *
         * On devices that have
         * <a href="{@docRoot}about/versions/android-4.2.html#MultipleUsers">
         * multiple users</a>, each user appears as a
         * completely separate device, so the {@code ANDROID_ID} value is
         * unique to each user.</p>
         *
         * <p class="note"><strong>Note:</strong> If the caller is an Instant App the ID is scoped
         * to the Instant App, it is generated when the Instant App is first installed and reset if
         * the user clears the Instant App.
         */
        @Readable
        public static final String ANDROID_ID = "android_id";

        /**
         * @deprecated Use {@link android.provider.Settings.Global#BLUETOOTH_ON} instead
         */
        @Deprecated
        public static final String BLUETOOTH_ON = Global.BLUETOOTH_ON;

        /**
         * @deprecated Use {@link android.provider.Settings.Global#DATA_ROAMING} instead
         */
        @Deprecated
        public static final String DATA_ROAMING = Global.DATA_ROAMING;

        /**
         * Stores {@link android.view.inputmethod.InputMethodInfo#getId()} of the input method
         * service that is currently selected.
         *
         * <p>Although the name {@link #DEFAULT_INPUT_METHOD} implies that there is a concept of
         * <i>default</i> input method, in reality this setting is no more or less than the
         * <strong>currently selected</strong> input method. This setting can be updated at any
         * time as a result of user-initiated and system-initiated input method switching.</p>
         *
         * <p>Use {@link ComponentName#unflattenFromString(String)} to parse the stored value.</p>
         */
        @Readable
        public static final String DEFAULT_INPUT_METHOD = "default_input_method";

        /**
         * Used only by {@link com.android.server.inputmethod.InputMethodManagerService} as a
         * temporary data store of {@link #DEFAULT_INPUT_METHOD} while a virtual-device-specific
         * input method is set as default.</p>
         *
         * <p>This should be considered to be an implementation detail of
         * {@link com.android.server.inputmethod.InputMethodManagerService}.  Other system
         * components should never rely on this value.</p>
         *
         * @see #DEFAULT_INPUT_METHOD
         * @hide
         */
        public static final String DEFAULT_DEVICE_INPUT_METHOD = "default_device_input_method";

        /**
         * Setting to record the input method subtype used by default, holding the ID
         * of the desired method.
         */
        @Readable
        public static final String SELECTED_INPUT_METHOD_SUBTYPE =
                "selected_input_method_subtype";

        /**
         * The {@link android.view.inputmethod.InputMethodInfo.InputMethodInfo#getId() ID} of the
         * default voice input method.
         * <p>
         * This stores the last known default voice IME. If the related system config value changes,
         * this is reset by InputMethodManagerService.
         * <p>
         * This IME is not necessarily in the enabled IME list. That state is still stored in
         * {@link #ENABLED_INPUT_METHODS}.
         *
         * @hide
         */
        public static final String DEFAULT_VOICE_INPUT_METHOD = "default_voice_input_method";

        /**
         * Setting to record the history of input method subtype, holding the pair of ID of IME
         * and its last used subtype.
         * @hide
         */
        @Readable
        public static final String INPUT_METHODS_SUBTYPE_HISTORY =
                "input_methods_subtype_history";

        /**
         * Setting to record the visibility of input method selector
         */
        @Readable
        public static final String INPUT_METHOD_SELECTOR_VISIBILITY =
                "input_method_selector_visibility";

        /**
         * Toggle for enabling stylus handwriting. When enabled, current Input method receives
         * stylus {@link MotionEvent}s if an {@link Editor} is focused.
         *
         * @see #STYLUS_HANDWRITING_DEFAULT_VALUE
         * @hide
         */
        @TestApi
        @Readable
        @SuppressLint("NoSettingsProvider")
        public static final String STYLUS_HANDWRITING_ENABLED = "stylus_handwriting_enabled";

        /**
         * Default value for {@link #STYLUS_HANDWRITING_ENABLED}.
         *
         * @hide
         */
        @TestApi
        @Readable
        @SuppressLint("NoSettingsProvider")
        public static final int STYLUS_HANDWRITING_DEFAULT_VALUE = 1;

        /**
         * The currently selected voice interaction service flattened ComponentName.
         * @hide
         */
        @TestApi
        @Readable
        public static final String VOICE_INTERACTION_SERVICE = "voice_interaction_service";


        /**
         * The currently selected credential service(s) flattened ComponentName.
         *
         * @hide
         */
        public static final String CREDENTIAL_SERVICE = "credential_service";

        /**
         * The currently selected primary credential service flattened ComponentName.
         *
         * @hide
         */
        public static final String CREDENTIAL_SERVICE_PRIMARY = "credential_service_primary";

        /**
         * The currently selected autofill service flattened ComponentName.
         * @hide
         */
        @TestApi
        @Readable
        public static final String AUTOFILL_SERVICE = "autofill_service";

        /**
         * Boolean indicating if Autofill supports field classification.
         *
         * @see android.service.autofill.AutofillService
         *
         * @hide
         */
        @SystemApi
        @Readable
        public static final String AUTOFILL_FEATURE_FIELD_CLASSIFICATION =
                "autofill_field_classification";

        /**
         * Boolean indicating if the dark mode dialog shown on first toggle has been seen.
         *
         * @hide
         */
        @Readable
        public static final String DARK_MODE_DIALOG_SEEN =
                "dark_mode_dialog_seen";

        /**
         * Custom time when Dark theme is scheduled to activate.
         * Represented as milliseconds from midnight (e.g. 79200000 == 10pm).
         * @hide
         */
        @Readable
        public static final String DARK_THEME_CUSTOM_START_TIME =
                "dark_theme_custom_start_time";

        /**
         * Custom time when Dark theme is scheduled to deactivate.
         * Represented as milliseconds from midnight (e.g. 79200000 == 10pm).
         * @hide
         */
        @Readable
        public static final String DARK_THEME_CUSTOM_END_TIME =
                "dark_theme_custom_end_time";

        /**
         * Defines value returned by {@link android.service.autofill.UserData#getMaxUserDataSize()}.
         *
         * @hide
         */
        @SystemApi
        @Readable
        public static final String AUTOFILL_USER_DATA_MAX_USER_DATA_SIZE =
                "autofill_user_data_max_user_data_size";

        /**
         * Defines value returned by
         * {@link android.service.autofill.UserData#getMaxFieldClassificationIdsSize()}.
         *
         * @hide
         */
        @SystemApi
        @Readable
        public static final String AUTOFILL_USER_DATA_MAX_FIELD_CLASSIFICATION_IDS_SIZE =
                "autofill_user_data_max_field_classification_size";

        /**
         * Defines value returned by
         * {@link android.service.autofill.UserData#getMaxCategoryCount()}.
         *
         * @hide
         */
        @SystemApi
        @Readable
        public static final String AUTOFILL_USER_DATA_MAX_CATEGORY_COUNT =
                "autofill_user_data_max_category_count";

        /**
         * Defines value returned by {@link android.service.autofill.UserData#getMaxValueLength()}.
         *
         * @hide
         */
        @SystemApi
        @Readable
        public static final String AUTOFILL_USER_DATA_MAX_VALUE_LENGTH =
                "autofill_user_data_max_value_length";

        /**
         * Defines value returned by {@link android.service.autofill.UserData#getMinValueLength()}.
         *
         * @hide
         */
        @SystemApi
        @Readable
        public static final String AUTOFILL_USER_DATA_MIN_VALUE_LENGTH =
                "autofill_user_data_min_value_length";

        /**
         * Defines whether Content Capture is enabled for the user.
         *
         * <p>Type: {@code int} ({@code 0} for disabled, {@code 1} for enabled).
         * <p>Default: enabled
         *
         * @hide
         */
        @TestApi
        @Readable
        public static final String CONTENT_CAPTURE_ENABLED = "content_capture_enabled";

        /**
         * @deprecated Use {@link android.provider.Settings.Global#DEVICE_PROVISIONED} instead
         */
        @Deprecated
        public static final String DEVICE_PROVISIONED = Global.DEVICE_PROVISIONED;

        /**
         * Indicates whether a DPC has been downloaded during provisioning.
         *
         * <p>Type: int (0 for false, 1 for true)
         *
         * <p>If this is true, then any attempts to begin setup again should result in factory reset
         *
         * @hide
         */
        @Readable
        public static final String MANAGED_PROVISIONING_DPC_DOWNLOADED =
                "managed_provisioning_dpc_downloaded";

        /**
         * Indicates whether the device is under restricted secure FRP mode.
         * Secure FRP mode is enabled when the device is under FRP. On solving of FRP challenge,
         * device is removed from this mode.
         * <p>
         * Type: int (0 for false, 1 for true)
         *
         * @deprecated Use Global.SECURE_FRP_MODE
         */
        @Deprecated
        @Readable
        public static final String SECURE_FRP_MODE = "secure_frp_mode";

        /**
         * Indicates whether the current user has completed setup via the setup wizard.
         * <p>
         * Type: int (0 for false, 1 for true)
         *
         * @hide
         */
        @SystemApi
        @Readable
        public static final String USER_SETUP_COMPLETE = "user_setup_complete";

        /**
         * Indicates that the user has not started setup personalization.
         * One of the possible states for {@link #USER_SETUP_PERSONALIZATION_STATE}.
         *
         * @hide
         */
        @SystemApi
        public static final int USER_SETUP_PERSONALIZATION_NOT_STARTED = 0;

        /**
         * Indicates that the user has not yet completed setup personalization.
         * One of the possible states for {@link #USER_SETUP_PERSONALIZATION_STATE}.
         *
         * @hide
         */
        @SystemApi
        public static final int USER_SETUP_PERSONALIZATION_STARTED = 1;

        /**
         * Indicates that the user has snoozed personalization and will complete it later.
         * One of the possible states for {@link #USER_SETUP_PERSONALIZATION_STATE}.
         *
         * @hide
         */
        @SystemApi
        public static final int USER_SETUP_PERSONALIZATION_PAUSED = 2;

        /**
         * Indicates that the user has completed setup personalization.
         * One of the possible states for {@link #USER_SETUP_PERSONALIZATION_STATE}.
         *
         * @hide
         */
        @SystemApi
        public static final int USER_SETUP_PERSONALIZATION_COMPLETE = 10;

        /** @hide */
        @Retention(RetentionPolicy.SOURCE)
        @IntDef({
                USER_SETUP_PERSONALIZATION_NOT_STARTED,
                USER_SETUP_PERSONALIZATION_STARTED,
                USER_SETUP_PERSONALIZATION_PAUSED,
                USER_SETUP_PERSONALIZATION_COMPLETE
        })
        public @interface UserSetupPersonalization {}

        /**
         * Defines the user's current state of device personalization.
         * The possible states are defined in {@link UserSetupPersonalization}.
         *
         * @hide
         */
        @SystemApi
        @Readable
        public static final String USER_SETUP_PERSONALIZATION_STATE =
                "user_setup_personalization_state";

        /**
         * Whether the current user has been set up via setup wizard (0 = false, 1 = true)
         * This value differs from USER_SETUP_COMPLETE in that it can be reset back to 0
         * in case SetupWizard has been re-enabled on TV devices.
         *
         * @hide
         */
        @Readable
        public static final String TV_USER_SETUP_COMPLETE = "tv_user_setup_complete";

        /**
         * The prefix for a category name that indicates whether a suggested action from that
         * category was marked as completed.
         * <p>
         * Type: int (0 for false, 1 for true)
         *
         * @hide
         */
        @SystemApi
        @Readable
        public static final String COMPLETED_CATEGORY_PREFIX = "suggested.completed_category.";

        /**
         * Whether or not compress blocks should be released on install.
         * <p>The setting only determines if the platform will attempt to release
         * compress blocks; it does not guarantee that the files will have their
         * compress blocks released. Compression is currently only supported on
         * some f2fs filesystems.
         * <p>
         * Type: int (0 for false, 1 for true)
         *
         * @hide
         */
        public static final String RELEASE_COMPRESS_BLOCKS_ON_INSTALL =
                "release_compress_blocks_on_install";

        /**
         * List of input methods that are currently enabled.  This is a string
         * containing the IDs of all enabled input methods, each ID separated
         * by ':'.
         *
         * Format like "ime0;subtype0;subtype1;subtype2:ime1:ime2;subtype0"
         * where imeId is ComponentName and subtype is int32.
         *
         * <p>Note: This setting is not readable to the app targeting API level 34 or higher. use
         * {@link android.view.inputmethod.InputMethodManager#getEnabledInputMethodList()} instead.
         */
        @Readable(maxTargetSdk = Build.VERSION_CODES.TIRAMISU)
        public static final String ENABLED_INPUT_METHODS = "enabled_input_methods";

        /**
         * List of system input methods that are currently disabled.  This is a string
         * containing the IDs of all disabled input methods, each ID separated
         * by ':'.
         * @hide
         */
        @Readable(maxTargetSdk = Build.VERSION_CODES.TIRAMISU)
        public static final String DISABLED_SYSTEM_INPUT_METHODS = "disabled_system_input_methods";

        /**
         * Whether to show the IME when a hard keyboard is connected. This is a boolean that
         * determines if the IME should be shown when a hard keyboard is attached.
         * @hide
         */
        @TestApi
        @Readable
        @SuppressLint("NoSettingsProvider")
        public static final String SHOW_IME_WITH_HARD_KEYBOARD = "show_ime_with_hard_keyboard";

        /**
         * Whether to enable bounce keys for Physical Keyboard accessibility.
         *
         * If set to non-zero value, any key press on physical keyboard within the provided
         * threshold duration (in milliseconds) of the same key, will be ignored.
         *
         * @hide
         */
        public static final String ACCESSIBILITY_BOUNCE_KEYS = "accessibility_bounce_keys";

        /**
         * Whether to enable slow keys for Physical Keyboard accessibility.
         *
         * If set to non-zero value, any key press on physical keyboard needs to be pressed and
         * held for the provided threshold duration (in milliseconds) to be registered in the
         * system.
         *
         * @hide
         */
        public static final String ACCESSIBILITY_SLOW_KEYS = "accessibility_slow_keys";

        /**
         * Whether to enable sticky keys for Physical Keyboard accessibility.
         *
         * This is a boolean value that determines if Sticky keys feature is enabled.
         *
         * @hide
         */
        public static final String ACCESSIBILITY_STICKY_KEYS = "accessibility_sticky_keys";

        /**
         * Whether stylus button presses are disabled. This is a boolean that
         * determines if stylus buttons are ignored.
         *
         * @hide
         */
        @TestApi
        @Readable
        @SuppressLint("NoSettingsProvider")
        public static final String STYLUS_BUTTONS_ENABLED = "stylus_buttons_enabled";

        /**
         * Preferred default user profile to use with the notes task button shortcut.
         *
         * @hide
         */
        @SuppressLint("NoSettingsProvider")
        public static final String DEFAULT_NOTE_TASK_PROFILE = "default_note_task_profile";

        /**
         * Host name and port for global http proxy. Uses ':' seperator for
         * between host and port.
         *
         * @deprecated Use {@link Global#HTTP_PROXY}
         */
        @Deprecated
        public static final String HTTP_PROXY = Global.HTTP_PROXY;

        /**
         * Package designated as always-on VPN provider.
         *
         * @hide
         */
        public static final String ALWAYS_ON_VPN_APP = "always_on_vpn_app";

        /**
         * Whether to block networking outside of VPN connections while always-on is set.
         * @see #ALWAYS_ON_VPN_APP
         *
         * @hide
         */
        @Readable
        public static final String ALWAYS_ON_VPN_LOCKDOWN = "always_on_vpn_lockdown";

        /**
         * Comma separated list of packages that are allowed to access the network when VPN is in
         * lockdown mode but not running.
         * @see #ALWAYS_ON_VPN_LOCKDOWN
         *
         * @hide
         */
        @Readable(maxTargetSdk = Build.VERSION_CODES.S)
        public static final String ALWAYS_ON_VPN_LOCKDOWN_WHITELIST =
                "always_on_vpn_lockdown_whitelist";

        /**
         * Whether applications can be installed for this user via the system's
         * {@link Intent#ACTION_INSTALL_PACKAGE} mechanism.
         *
         * <p>1 = permit app installation via the system package installer intent
         * <p>0 = do not allow use of the package installer
         * @deprecated Starting from {@link android.os.Build.VERSION_CODES#O}, apps should use
         * {@link PackageManager#canRequestPackageInstalls()}
         * @see PackageManager#canRequestPackageInstalls()
         */
        @Deprecated
        @Readable
        public static final String INSTALL_NON_MARKET_APPS = "install_non_market_apps";

        /**
         * A flag to tell {@link com.android.server.devicepolicy.DevicePolicyManagerService} that
         * the default for {@link #INSTALL_NON_MARKET_APPS} is reversed for this user on OTA. So it
         * can set the restriction {@link android.os.UserManager#DISALLOW_INSTALL_UNKNOWN_SOURCES}
         * on behalf of the profile owner if needed to make the change transparent for profile
         * owners.
         *
         * @hide
         */
        @Readable
        public static final String UNKNOWN_SOURCES_DEFAULT_REVERSED =
                "unknown_sources_default_reversed";

        /**
         * Comma-separated list of location providers that are enabled. Do not rely on this value
         * being present or correct, or on ContentObserver notifications on the corresponding Uri.
         *
         * @deprecated This setting no longer exists from Android S onwards as it no longer is
         * capable of realistically reflecting location settings. Use {@link
         * LocationManager#isProviderEnabled(String)} or {@link LocationManager#isLocationEnabled()}
         * instead.
         */
        @Deprecated
        @Readable
        public static final String LOCATION_PROVIDERS_ALLOWED = "location_providers_allowed";

        /**
         * The current location mode of the device. Do not rely on this value being present or on
         * ContentObserver notifications on the corresponding Uri.
         *
         * @deprecated The preferred methods for checking location mode and listening for changes
         * are via {@link LocationManager#isLocationEnabled()} and
         * {@link LocationManager#MODE_CHANGED_ACTION}.
         */
        @Deprecated
        @Readable
        public static final String LOCATION_MODE = "location_mode";

        /**
         * The App or module that changes the location mode.
         * @hide
         */
        @Readable
        public static final String LOCATION_CHANGER = "location_changer";

        /**
         * The location changer is unknown or unable to detect.
         * @hide
         */
        public static final int LOCATION_CHANGER_UNKNOWN = 0;

        /**
         * Location settings in system settings.
         * @hide
         */
        public static final int LOCATION_CHANGER_SYSTEM_SETTINGS = 1;

        /**
         * The location icon in drop down notification drawer.
         * @hide
         */
        public static final int LOCATION_CHANGER_QUICK_SETTINGS = 2;

        /**
         * Location mode is off.
         */
        public static final int LOCATION_MODE_OFF = 0;

        /**
         * This mode no longer has any distinct meaning, but is interpreted as the location mode is
         * on.
         *
         * @deprecated See {@link #LOCATION_MODE}.
         */
        @Deprecated
        public static final int LOCATION_MODE_SENSORS_ONLY = 1;

        /**
         * This mode no longer has any distinct meaning, but is interpreted as the location mode is
         * on.
         *
         * @deprecated See {@link #LOCATION_MODE}.
         */
        @Deprecated
        public static final int LOCATION_MODE_BATTERY_SAVING = 2;

        /**
         * This mode no longer has any distinct meaning, but is interpreted as the location mode is
         * on.
         *
         * @deprecated See {@link #LOCATION_MODE}.
         */
        @Deprecated
        public static final int LOCATION_MODE_HIGH_ACCURACY = 3;

        /**
         * Location mode is on.
         *
         * @hide
         */
        @SystemApi
        public static final int LOCATION_MODE_ON = LOCATION_MODE_HIGH_ACCURACY;

        /**
         * The current location time zone detection enabled state for the user.
         *
         * See {@link android.app.time.TimeManager#getTimeZoneCapabilitiesAndConfig} for access.
         * See {@link android.app.time.TimeManager#updateTimeZoneConfiguration} to update.
         * @hide
         */
        public static final String LOCATION_TIME_ZONE_DETECTION_ENABLED =
                "location_time_zone_detection_enabled";

        /**
         * The accuracy in meters used for coarsening location for clients with only the coarse
         * location permission.
         *
         * @hide
         */
        @Readable
        public static final String LOCATION_COARSE_ACCURACY_M = "locationCoarseAccuracy";

        /**
         * Whether or not to show display system location accesses.
         * @hide
         */
        public static final String LOCATION_SHOW_SYSTEM_OPS = "locationShowSystemOps";

        /**
         * A flag containing settings used for biometric weak
         * @hide
         */
        @Deprecated
        @Readable
        public static final String LOCK_BIOMETRIC_WEAK_FLAGS =
                "lock_biometric_weak_flags";

        /**
         * Whether lock-to-app will lock the keyguard when exiting.
         * @hide
         */
        @Readable
        public static final String LOCK_TO_APP_EXIT_LOCKED = "lock_to_app_exit_locked";

        /**
         * Whether autolock is enabled (0 = false, 1 = true)
         *
         * @deprecated Use {@link android.app.KeyguardManager} to determine the state and security
         *             level of the keyguard. Accessing this setting from an app that is targeting
         *             {@link VERSION_CODES#M} or later throws a {@code SecurityException}.
         */
        @Deprecated
        @Readable
        public static final String LOCK_PATTERN_ENABLED = "lock_pattern_autolock";

        /**
         * Whether lock pattern is visible as user enters (0 = false, 1 = true)
         *
         * @deprecated Accessing this setting from an app that is targeting
         *             {@link VERSION_CODES#M} or later throws a {@code SecurityException}.
         */
        @Deprecated
        @Readable
        public static final String LOCK_PATTERN_VISIBLE = "lock_pattern_visible_pattern";

        /**
         * Whether lock pattern will vibrate as user enters (0 = false, 1 =
         * true)
         *
         * @deprecated Starting in {@link VERSION_CODES#JELLY_BEAN_MR1} the
         *             lockscreen uses
         *             {@link Settings.System#HAPTIC_FEEDBACK_ENABLED}.
         *             Accessing this setting from an app that is targeting
         *             {@link VERSION_CODES#M} or later throws a {@code SecurityException}.
         */
        @Deprecated
        @Readable
        public static final String
                LOCK_PATTERN_TACTILE_FEEDBACK_ENABLED = "lock_pattern_tactile_feedback_enabled";

        /**
         * Determines the width and height of the LockPatternView widget
         * @hide
         */
        public static final String LOCK_PATTERN_SIZE = "lock_pattern_size";

        /**
         * Whether lock pattern will show dots (0 = false, 1 = true)
         * @hide
         */
        public static final String LOCK_DOTS_VISIBLE = "lock_pattern_dotsvisible";

        /**
         * Whether lockscreen error pattern is visible (0 = false, 1 = true)
         * @hide
         */
        public static final String LOCK_SHOW_ERROR_PATH = "lock_pattern_show_error_path";

        /**
         * This preference allows the device to be locked given time after screen goes off,
         * subject to current DeviceAdmin policy limits.
         * @hide
         */
        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        @Readable
        public static final String LOCK_SCREEN_LOCK_AFTER_TIMEOUT = "lock_screen_lock_after_timeout";


        /**
         * This preference contains the string that shows for owner info on LockScreen.
         * @hide
         * @deprecated
         */
        @Deprecated
        @Readable
        public static final String LOCK_SCREEN_OWNER_INFO = "lock_screen_owner_info";

        /**
         * Ids of the user-selected appwidgets on the lockscreen (comma-delimited).
         * @hide
         */
        @Deprecated
        @Readable
        public static final String LOCK_SCREEN_APPWIDGET_IDS =
            "lock_screen_appwidget_ids";

        /**
         * Id of the appwidget shown on the lock screen when appwidgets are disabled.
         * @hide
         */
        @Deprecated
        @Readable
        public static final String LOCK_SCREEN_FALLBACK_APPWIDGET_ID =
            "lock_screen_fallback_appwidget_id";

        /**
         * Index of the lockscreen appwidget to restore, -1 if none.
         * @hide
         */
        @Deprecated
        @Readable
        public static final String LOCK_SCREEN_STICKY_APPWIDGET =
            "lock_screen_sticky_appwidget";

        /**
         * This preference enables showing the owner info on LockScreen.
         * @hide
         * @deprecated
         */
        @Deprecated
        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        @Readable
        public static final String LOCK_SCREEN_OWNER_INFO_ENABLED =
            "lock_screen_owner_info_enabled";

        /**
         * Indicates whether the user has allowed notifications to be shown atop a securely locked
         * screen in their full "private" form (same as when the device is unlocked).
         * <p>
         * Type: int (0 for false, 1 for true)
         *
         * @hide
         */
        @SystemApi
        @Readable
        public static final String LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS =
                "lock_screen_allow_private_notifications";

        /**
         * When set by a user, allows notification remote input atop a securely locked screen
         * without having to unlock
         * @hide
         */
        @Readable
        public static final String LOCK_SCREEN_ALLOW_REMOTE_INPUT =
                "lock_screen_allow_remote_input";

        /**
         * Indicates which clock face to show on lock screen and AOD formatted as a serialized
         * {@link org.json.JSONObject} with the format:
         *     {"clock": id, "_applied_timestamp": timestamp}
         * @hide
         */
        @Readable
        public static final String LOCK_SCREEN_CUSTOM_CLOCK_FACE = "lock_screen_custom_clock_face";

        /**
         * Indicates which clock face to show on lock screen and AOD while docked.
         * @hide
         */
        @Readable
        public static final String DOCKED_CLOCK_FACE = "docked_clock_face";

        /**
         * Set by the system to track if the user needs to see the call to action for
         * the lockscreen notification policy.
         * @hide
         */
        @Readable
        public static final String SHOW_NOTE_ABOUT_NOTIFICATION_HIDING =
                "show_note_about_notification_hiding";

        /**
         * Set to 1 by the system after trust agents have been initialized.
         * @hide
         */
        @Readable
        public static final String TRUST_AGENTS_INITIALIZED =
                "trust_agents_initialized";

        /**
         * Set to 1 by the system after the list of known trust agents have been initialized.
         * @hide
         */
        public static final String KNOWN_TRUST_AGENTS_INITIALIZED =
                "known_trust_agents_initialized";

        /**
         * The Logging ID (a unique 64-bit value) as a hex string.
         * Used as a pseudonymous identifier for logging.
         * @deprecated This identifier is poorly initialized and has
         * many collisions.  It should not be used.
         */
        @Deprecated
        @Readable
        public static final String LOGGING_ID = "logging_id";

        /**
         * @deprecated Use {@link android.provider.Settings.Global#NETWORK_PREFERENCE} instead
         */
        @Deprecated
        public static final String NETWORK_PREFERENCE = Global.NETWORK_PREFERENCE;

        /**
         * No longer supported.
         */
        @Readable
        public static final String PARENTAL_CONTROL_ENABLED = "parental_control_enabled";

        /**
         * No longer supported.
         */
        @Readable
        public static final String PARENTAL_CONTROL_LAST_UPDATE = "parental_control_last_update";

        /**
         * No longer supported.
         */
        @Readable
        public static final String PARENTAL_CONTROL_REDIRECT_URL = "parental_control_redirect_url";

        /**
         * Settings classname to launch when Settings is clicked from All
         * Applications.  Needed because of user testing between the old
         * and new Settings apps.
         */
        // TODO: 881807
        @Readable
        public static final String SETTINGS_CLASSNAME = "settings_classname";

        /**
         * @deprecated Use {@link android.provider.Settings.Global#USB_MASS_STORAGE_ENABLED} instead
         */
        @Deprecated
        public static final String USB_MASS_STORAGE_ENABLED = Global.USB_MASS_STORAGE_ENABLED;

        /**
         * @deprecated Use {@link android.provider.Settings.Global#USE_GOOGLE_MAIL} instead
         */
        @Deprecated
        public static final String USE_GOOGLE_MAIL = Global.USE_GOOGLE_MAIL;

        /**
         * If accessibility is enabled.
         */
        @Readable
        public static final String ACCESSIBILITY_ENABLED = "accessibility_enabled";

        /**
         * Whether select sound track with audio description by default.
         * @hide
         */
        public static final String ENABLED_ACCESSIBILITY_AUDIO_DESCRIPTION_BY_DEFAULT =
                "enabled_accessibility_audio_description_by_default";

        /**
         * Setting specifying if the accessibility shortcut is enabled.
         * @hide
         */
        @Readable
        public static final String ACCESSIBILITY_SHORTCUT_ON_LOCK_SCREEN =
                "accessibility_shortcut_on_lock_screen";

        /**
         * Setting specifying if the accessibility shortcut dialog has been shown to this user.
         * @hide
         */
        @Readable
        public static final String ACCESSIBILITY_SHORTCUT_DIALOG_SHOWN =
                "accessibility_shortcut_dialog_shown";

        /**
         * Setting specifying if the timeout restriction
         * {@link ViewConfiguration#getAccessibilityShortcutKeyTimeout()}
         * of the accessibility shortcut dialog is skipped.
         *
         * @hide
         */
        public static final String SKIP_ACCESSIBILITY_SHORTCUT_DIALOG_TIMEOUT_RESTRICTION =
                "skip_accessibility_shortcut_dialog_timeout_restriction";

        /**
         * Setting specifying the accessibility services, accessibility shortcut targets,
         * or features to be toggled via the accessibility shortcut.
         *
         * <p> This is a colon-separated string list which contains the flattened
         * {@link ComponentName} and the class name of a system class implementing a supported
         * accessibility feature.
         * @hide
         */
        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        @TestApi
        @Readable
        public static final String ACCESSIBILITY_SHORTCUT_TARGET_SERVICE =
                "accessibility_shortcut_target_service";

        /**
         * Setting specifying the accessibility service or feature to be toggled via the
         * accessibility button in the navigation bar. This is either a flattened
         * {@link ComponentName} or the class name of a system class implementing a supported
         * accessibility feature.
         * @hide
         */
        @Readable
        public static final String ACCESSIBILITY_BUTTON_TARGET_COMPONENT =
                "accessibility_button_target_component";

        /**
         * Setting specifying the accessibility services, accessibility shortcut targets,
         * or features to be toggled via the accessibility button in the navigation bar.
         *
         * <p> This is a colon-separated string list which contains the flattened
         * {@link ComponentName} and the class name of a system class implementing a supported
         * accessibility feature.
         * @hide
         */
        @Readable
        public static final String ACCESSIBILITY_BUTTON_TARGETS = "accessibility_button_targets";

        /**
         * Setting specifying the accessibility services, accessibility shortcut targets,
         * or features to be toggled via a tile in the quick settings panel.
         *
         * <p> This is a colon-separated string list which contains the flattened
         * {@link ComponentName} and the class name of a system class implementing a supported
         * accessibility feature.
         * @hide
         */
        public static final String ACCESSIBILITY_QS_TARGETS = "accessibility_qs_targets";

        /**
         * The system class name of magnification controller which is a target to be toggled via
         * accessibility shortcut or accessibility button.
         *
         * @hide
         */
        @Readable
        public static final String ACCESSIBILITY_SHORTCUT_TARGET_MAGNIFICATION_CONTROLLER =
                "com.android.server.accessibility.MagnificationController";

        /**
         * If touch exploration is enabled.
         */
        @Readable
        public static final String TOUCH_EXPLORATION_ENABLED = "touch_exploration_enabled";

        /**
         * List of the enabled accessibility providers.
         */
        @Readable
        public static final String ENABLED_ACCESSIBILITY_SERVICES =
            "enabled_accessibility_services";

        /**
         * List of the notified non-accessibility category accessibility services.
         *
         * @hide
         */
        @Readable
        public static final String NOTIFIED_NON_ACCESSIBILITY_CATEGORY_SERVICES =
                "notified_non_accessibility_category_services";

        /**
         * List of the accessibility services to which the user has granted
         * permission to put the device into touch exploration mode.
         *
         * @hide
         */
        @Readable
        public static final String TOUCH_EXPLORATION_GRANTED_ACCESSIBILITY_SERVICES =
            "touch_exploration_granted_accessibility_services";

        /**
         * Is talkback service enabled or not. 0 == no, 1 == yes
         *
         * @hide
         */
        public static final String WEAR_TALKBACK_ENABLED = "wear_talkback_enabled";

        /**
         * Whether the Global Actions Panel is enabled.
         * @hide
         */
        @Readable
        public static final String GLOBAL_ACTIONS_PANEL_ENABLED = "global_actions_panel_enabled";

        /**
         * Whether the Global Actions Panel can be toggled on or off in Settings.
         * @hide
         */
        @Readable
        public static final String GLOBAL_ACTIONS_PANEL_AVAILABLE =
                "global_actions_panel_available";

        /**
         * Enables debug mode for the Global Actions Panel.
         * @hide
         */
        @Readable
        public static final String GLOBAL_ACTIONS_PANEL_DEBUG_ENABLED =
                "global_actions_panel_debug_enabled";

        /**
         * Whether the hush gesture has ever been used
         * @hide
         */
        @SystemApi
        @Readable
        public static final String HUSH_GESTURE_USED = "hush_gesture_used";

        /**
         * Number of times the user has manually clicked the ringer toggle
         * @hide
         */
        @Readable
        public static final String MANUAL_RINGER_TOGGLE_COUNT = "manual_ringer_toggle_count";

        /**
         * Whether to play a sound for charging events.
         * @hide
         */
        @Readable
        public static final String CHARGING_SOUNDS_ENABLED = "charging_sounds_enabled";

        /**
         * Whether to vibrate for charging events.
         * @hide
         */
        @Readable
        public static final String CHARGING_VIBRATION_ENABLED = "charging_vibration_enabled";

        /**
         * If 0, turning on dnd manually will last indefinitely.
         * Else if non-negative, turning on dnd manually will last for this many minutes.
         * Else (if negative), turning on dnd manually will surface a dialog that prompts
         * user to specify a duration.
         * @hide
         */
        @Readable
        public static final String ZEN_DURATION = "zen_duration";

        /** @hide */ public static final int ZEN_DURATION_PROMPT = -1;
        /** @hide */ public static final int ZEN_DURATION_FOREVER = 0;

        /**
         * If nonzero, will show the zen upgrade notification when the user toggles DND on/off.
         * @hide
         */
        @Readable
        public static final String SHOW_ZEN_UPGRADE_NOTIFICATION = "show_zen_upgrade_notification";

        /**
         * If nonzero, will show the zen update settings suggestion.
         * @hide
         */
        @Readable
        public static final String SHOW_ZEN_SETTINGS_SUGGESTION = "show_zen_settings_suggestion";

        /**
         * If nonzero, zen has not been updated to reflect new changes.
         * @hide
         */
        @Readable
        public static final String ZEN_SETTINGS_UPDATED = "zen_settings_updated";

        /**
         * If nonzero, zen setting suggestion has been viewed by user
         * @hide
         */
        @Readable
        public static final String ZEN_SETTINGS_SUGGESTION_VIEWED =
                "zen_settings_suggestion_viewed";

        /**
         * Whether the in call notification is enabled to play sound during calls.  The value is
         * boolean (1 or 0).
         * @hide
         */
        @Readable
        public static final String IN_CALL_NOTIFICATION_ENABLED = "in_call_notification_enabled";

        /**
         * Uri of the slice that's presented on the keyguard.
         * Defaults to a slice with the date and next alarm.
         *
         * @hide
         */
        @Readable
        public static final String KEYGUARD_SLICE_URI = "keyguard_slice_uri";

        /**
         * The adjustment in font weight. This is used to draw text in bold.
         *
         * <p> This value can be negative. To display bolded text, the adjustment used is 300,
         * which is the difference between
         * {@link android.graphics.fonts.FontStyle#FONT_WEIGHT_NORMAL} and
         * {@link android.graphics.fonts.FontStyle#FONT_WEIGHT_BOLD}.
         *
         * @hide
         */
        @Readable
        public static final String FONT_WEIGHT_ADJUSTMENT = "font_weight_adjustment";

        /**
         * Whether to speak passwords while in accessibility mode.
         *
         * @deprecated The speaking of passwords is controlled by individual accessibility services.
         * Apps should ignore this setting and provide complete information to accessibility
         * at all times, which was the behavior when this value was {@code true}.
         */
        @Deprecated
        @Readable
        public static final String ACCESSIBILITY_SPEAK_PASSWORD = "speak_password";

        /**
         * Whether to draw text with high contrast while in accessibility mode.
         *
         * @hide
         */
        @Readable
        public static final String ACCESSIBILITY_HIGH_TEXT_CONTRAST_ENABLED =
                "high_text_contrast_enabled";

        /**
         * The color contrast, float in [-1, 1], 1 being the highest contrast.
         *
         * @hide
         */
        public static final String CONTRAST_LEVEL = "contrast_level";

        /**
         * Setting that specifies whether the display magnification is enabled via a system-wide
         * triple tap gesture. Display magnifications allows the user to zoom in the display content
         * and is targeted to low vision users. The current magnification scale is controlled by
         * {@link #ACCESSIBILITY_DISPLAY_MAGNIFICATION_SCALE}.
         *
         * @hide
         */
        @UnsupportedAppUsage
        @TestApi
        @Readable
        public static final String ACCESSIBILITY_DISPLAY_MAGNIFICATION_ENABLED =
                "accessibility_display_magnification_enabled";

        /**
         * Setting that specifies whether the display magnification is enabled via a shortcut
         * affordance within the system's navigation area. Display magnifications allows the user to
         * zoom in the display content and is targeted to low vision users. The current
         * magnification scale is controlled by {@link #ACCESSIBILITY_DISPLAY_MAGNIFICATION_SCALE}.
         *
         * @deprecated Use {@link #ACCESSIBILITY_BUTTON_TARGETS} instead.
         * {@link #ACCESSIBILITY_BUTTON_TARGETS} holds the magnification system class name
         * when navigation bar magnification is enabled.
         * @hide
         */
        @SystemApi
        @Readable
        public static final String ACCESSIBILITY_DISPLAY_MAGNIFICATION_NAVBAR_ENABLED =
                "accessibility_display_magnification_navbar_enabled";

        /**
         * Setting that specifies what the display magnification scale is.
         * Display magnifications allows the user to zoom in the display
         * content and is targeted to low vision users. Whether a display
         * magnification is performed is controlled by
         * {@link #ACCESSIBILITY_DISPLAY_MAGNIFICATION_ENABLED} and
         * {@link #ACCESSIBILITY_DISPLAY_MAGNIFICATION_NAVBAR_ENABLED}
         *
         * @hide
         */
        @Readable
        public static final String ACCESSIBILITY_DISPLAY_MAGNIFICATION_SCALE =
                "accessibility_display_magnification_scale";

        /**
         * Unused mangnification setting
         *
         * @hide
         * @deprecated
         */
        @Deprecated
        @Readable
        public static final String ACCESSIBILITY_DISPLAY_MAGNIFICATION_AUTO_UPDATE =
                "accessibility_display_magnification_auto_update";

        /**
         * Accessibility Window Magnification Allow diagonal scrolling value. The value is boolean.
         * 1 : on, 0 : off
         *
         * @hide
         */
        public static final String ACCESSIBILITY_ALLOW_DIAGONAL_SCROLLING =
                "accessibility_allow_diagonal_scrolling";


        /**
         * Setting that specifies what mode the soft keyboard is in (default or hidden). Can be
         * modified from an AccessibilityService using the SoftKeyboardController.
         *
         * @hide
         */
        @Readable
        public static final String ACCESSIBILITY_SOFT_KEYBOARD_MODE =
                "accessibility_soft_keyboard_mode";

        /**
         * Default soft keyboard behavior.
         *
         * @hide
         */
        public static final int SHOW_MODE_AUTO = 0;

        /**
         * Soft keyboard is never shown.
         *
         * @hide
         */
        public static final int SHOW_MODE_HIDDEN = 1;

        /**
         * Setting that specifies whether timed text (captions) should be
         * displayed in video content. Text display properties are controlled by
         * the following settings:
         * <ul>
         * <li>{@link #ACCESSIBILITY_CAPTIONING_LOCALE}
         * <li>{@link #ACCESSIBILITY_CAPTIONING_BACKGROUND_COLOR}
         * <li>{@link #ACCESSIBILITY_CAPTIONING_FOREGROUND_COLOR}
         * <li>{@link #ACCESSIBILITY_CAPTIONING_EDGE_COLOR}
         * <li>{@link #ACCESSIBILITY_CAPTIONING_EDGE_TYPE}
         * <li>{@link #ACCESSIBILITY_CAPTIONING_TYPEFACE}
         * <li>{@link #ACCESSIBILITY_CAPTIONING_FONT_SCALE}
         * </ul>
         *
         * @hide
         */
        @Readable
        public static final String ACCESSIBILITY_CAPTIONING_ENABLED =
                "accessibility_captioning_enabled";

        /**
         * Setting that specifies the language for captions as a locale string,
         * e.g. en_US.
         *
         * @see java.util.Locale#toString
         * @hide
         */
        @Readable
        public static final String ACCESSIBILITY_CAPTIONING_LOCALE =
                "accessibility_captioning_locale";

        /**
         * Integer property that specifies the preset style for captions, one
         * of:
         * <ul>
         * <li>{@link android.view.accessibility.CaptioningManager.CaptionStyle#PRESET_CUSTOM}
         * <li>a valid index of {@link android.view.accessibility.CaptioningManager.CaptionStyle#PRESETS}
         * </ul>
         *
         * @see java.util.Locale#toString
         * @hide
         */
        @Readable
        public static final String ACCESSIBILITY_CAPTIONING_PRESET =
                "accessibility_captioning_preset";

        /**
         * Integer property that specifes the background color for captions as a
         * packed 32-bit color.
         *
         * @see android.graphics.Color#argb
         * @hide
         */
        @Readable
        public static final String ACCESSIBILITY_CAPTIONING_BACKGROUND_COLOR =
                "accessibility_captioning_background_color";

        /**
         * Integer property that specifes the foreground color for captions as a
         * packed 32-bit color.
         *
         * @see android.graphics.Color#argb
         * @hide
         */
        @Readable
        public static final String ACCESSIBILITY_CAPTIONING_FOREGROUND_COLOR =
                "accessibility_captioning_foreground_color";

        /**
         * Integer property that specifes the edge type for captions, one of:
         * <ul>
         * <li>{@link android.view.accessibility.CaptioningManager.CaptionStyle#EDGE_TYPE_NONE}
         * <li>{@link android.view.accessibility.CaptioningManager.CaptionStyle#EDGE_TYPE_OUTLINE}
         * <li>{@link android.view.accessibility.CaptioningManager.CaptionStyle#EDGE_TYPE_DROP_SHADOW}
         * </ul>
         *
         * @see #ACCESSIBILITY_CAPTIONING_EDGE_COLOR
         * @hide
         */
        @Readable
        public static final String ACCESSIBILITY_CAPTIONING_EDGE_TYPE =
                "accessibility_captioning_edge_type";

        /**
         * Integer property that specifes the edge color for captions as a
         * packed 32-bit color.
         *
         * @see #ACCESSIBILITY_CAPTIONING_EDGE_TYPE
         * @see android.graphics.Color#argb
         * @hide
         */
        @Readable
        public static final String ACCESSIBILITY_CAPTIONING_EDGE_COLOR =
                "accessibility_captioning_edge_color";

        /**
         * Integer property that specifes the window color for captions as a
         * packed 32-bit color.
         *
         * @see android.graphics.Color#argb
         * @hide
         */
        @Readable
        public static final String ACCESSIBILITY_CAPTIONING_WINDOW_COLOR =
                "accessibility_captioning_window_color";

        /**
         * String property that specifies the typeface for captions, one of:
         * <ul>
         * <li>DEFAULT
         * <li>MONOSPACE
         * <li>SANS_SERIF
         * <li>SERIF
         * </ul>
         *
         * @see android.graphics.Typeface
         * @hide
         */
        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        @Readable
        public static final String ACCESSIBILITY_CAPTIONING_TYPEFACE =
                "accessibility_captioning_typeface";

        /**
         * Floating point property that specifies font scaling for captions.
         *
         * @hide
         */
        @Readable
        public static final String ACCESSIBILITY_CAPTIONING_FONT_SCALE =
                "accessibility_captioning_font_scale";

        /**
         * Setting that specifies whether display color inversion is enabled.
         */
        @Readable
        public static final String ACCESSIBILITY_DISPLAY_INVERSION_ENABLED =
                "accessibility_display_inversion_enabled";

        /**
         * Flag that specifies whether font size has been changed. The flag will
         * be set when users change the scaled value of font size for the first time.
         * @hide
         */
        @Readable
        public static final String ACCESSIBILITY_FONT_SCALING_HAS_BEEN_CHANGED =
                "accessibility_font_scaling_has_been_changed";

        /**
         * Setting that specifies whether display color space adjustment is
         * enabled.
         *
         * @hide
         */
        @UnsupportedAppUsage
        @Readable
        public static final String ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED =
                "accessibility_display_daltonizer_enabled";

        /**
         * Integer property that specifies the type of color space adjustment to
         * perform. Valid values are defined in AccessibilityManager and Settings arrays.xml:
         * - AccessibilityManager.DALTONIZER_DISABLED = -1
         * - AccessibilityManager.DALTONIZER_SIMULATE_MONOCHROMACY = 0
         * - <item>@string/daltonizer_mode_protanomaly</item> = 11
         * - AccessibilityManager.DALTONIZER_CORRECT_DEUTERANOMALY and
         *       <item>@string/daltonizer_mode_deuteranomaly</item> = 12
         * - <item>@string/daltonizer_mode_tritanomaly</item> = 13
         *
         * @hide
         */
        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        @Readable
        public static final String ACCESSIBILITY_DISPLAY_DALTONIZER =
                "accessibility_display_daltonizer";

        /**
         * Setting that specifies whether automatic click when the mouse pointer stops moving is
         * enabled.
         *
         * @hide
         */
        @UnsupportedAppUsage
        @Readable
        public static final String ACCESSIBILITY_AUTOCLICK_ENABLED =
                "accessibility_autoclick_enabled";

        /**
         * Integer setting specifying amount of time in ms the mouse pointer has to stay still
         * before performing click when {@link #ACCESSIBILITY_AUTOCLICK_ENABLED} is set.
         *
         * @see #ACCESSIBILITY_AUTOCLICK_ENABLED
         * @hide
         */
        @Readable
        public static final String ACCESSIBILITY_AUTOCLICK_DELAY =
                "accessibility_autoclick_delay";

        /**
         * Whether or not larger size icons are used for the pointer of mouse/trackpad for
         * accessibility.
         * (0 = false, 1 = true)
         * @hide
         */
        @UnsupportedAppUsage
        @Readable
        public static final String ACCESSIBILITY_LARGE_POINTER_ICON =
                "accessibility_large_pointer_icon";

        /**
         * The timeout for considering a press to be a long press in milliseconds.
         * @hide
         */
        @UnsupportedAppUsage
        @Readable
        public static final String LONG_PRESS_TIMEOUT = "long_press_timeout";

        /**
         * The duration in milliseconds between the first tap's up event and the second tap's
         * down event for an interaction to be considered part of the same multi-press.
         * @hide
         */
        @Readable
        public static final String MULTI_PRESS_TIMEOUT = "multi_press_timeout";

        /**
         * The duration before a key repeat begins in milliseconds.
         * @hide
         */
        public static final String KEY_REPEAT_TIMEOUT_MS = "key_repeat_timeout";

        /**
         * The duration between successive key repeats in milliseconds.
         * @hide
         */
        public static final String KEY_REPEAT_DELAY_MS = "key_repeat_delay";

        /**
         * Setting that specifies recommended timeout in milliseconds for controls
         * which don't need user's interactions.
         *
         * @hide
         */
        @Readable
        public static final String ACCESSIBILITY_NON_INTERACTIVE_UI_TIMEOUT_MS =
                "accessibility_non_interactive_ui_timeout_ms";

        /**
         * Setting that specifies recommended timeout in milliseconds for controls
         * which need user's interactions.
         *
         * @hide
         */
        @Readable
        public static final String ACCESSIBILITY_INTERACTIVE_UI_TIMEOUT_MS =
                "accessibility_interactive_ui_timeout_ms";


        /**
         * Setting that specifies whether Reduce Bright Colors, or brightness dimming by color
         * adjustment, is enabled.
         *
         * @hide
         */
        public static final String REDUCE_BRIGHT_COLORS_ACTIVATED =
                "reduce_bright_colors_activated";

        /**
         * Setting that specifies the level of Reduce Bright Colors in intensity. The range is
         * [0, 100].
         *
         * @hide
         */
        public static final String REDUCE_BRIGHT_COLORS_LEVEL =
                "reduce_bright_colors_level";

        /**
         * Setting that specifies whether Reduce Bright Colors should persist across reboots.
         *
         * @hide
         */
        public static final String REDUCE_BRIGHT_COLORS_PERSIST_ACROSS_REBOOTS =
                "reduce_bright_colors_persist_across_reboots";

        /**
         * Setting that specifies whether Even Dimmer - a feature that allows the brightness
         * slider to go below what the display can conventionally do, should be enabled.
         *
         * @hide
         */
        public static final String EVEN_DIMMER_ACTIVATED =
                "even_dimmer_activated";

        /**
         * Setting that specifies which nits level Even Dimmer should allow the screen brightness
         * to go down to.
         *
         * @hide
         */
        public static final String EVEN_DIMMER_MIN_NITS =
                "even_dimmer_min_nits";

        /**
         * List of the enabled print services.
         *
         * N and beyond uses {@link #DISABLED_PRINT_SERVICES}. But this might be used in an upgrade
         * from pre-N.
         *
         * @hide
         */
        @UnsupportedAppUsage
        @Readable
        public static final String ENABLED_PRINT_SERVICES =
            "enabled_print_services";

        /**
         * List of the disabled print services.
         *
         * @hide
         */
        @TestApi
        @Readable
        public static final String DISABLED_PRINT_SERVICES =
            "disabled_print_services";

        /**
         * The saved value for WindowManagerService.setForcedDisplayDensity()
         * formatted as a single integer representing DPI. If unset, then use
         * the real display density.
         *
         * @hide
         */
        @Readable
        public static final String DISPLAY_DENSITY_FORCED = "display_density_forced";

        /**
         * Setting to always use the default text-to-speech settings regardless
         * of the application settings.
         * 1 = override application settings,
         * 0 = use application settings (if specified).
         *
         * @deprecated  The value of this setting is no longer respected by
         * the framework text to speech APIs as of the Ice Cream Sandwich release.
         */
        @Deprecated
        @Readable
        public static final String TTS_USE_DEFAULTS = "tts_use_defaults";

        /**
         * Default text-to-speech engine speech rate. 100 = 1x
         */
        @Readable
        public static final String TTS_DEFAULT_RATE = "tts_default_rate";

        /**
         * Default text-to-speech engine pitch. 100 = 1x
         */
        @Readable
        public static final String TTS_DEFAULT_PITCH = "tts_default_pitch";

        /**
         * Default text-to-speech engine.
         */
        @Readable
        public static final String TTS_DEFAULT_SYNTH = "tts_default_synth";

        /**
         * Default text-to-speech language.
         *
         * @deprecated this setting is no longer in use, as of the Ice Cream
         * Sandwich release. Apps should never need to read this setting directly,
         * instead can query the TextToSpeech framework classes for the default
         * locale. {@link TextToSpeech#getLanguage()}.
         */
        @Deprecated
        @Readable
        public static final String TTS_DEFAULT_LANG = "tts_default_lang";

        /**
         * Default text-to-speech country.
         *
         * @deprecated this setting is no longer in use, as of the Ice Cream
         * Sandwich release. Apps should never need to read this setting directly,
         * instead can query the TextToSpeech framework classes for the default
         * locale. {@link TextToSpeech#getLanguage()}.
         */
        @Deprecated
        @Readable
        public static final String TTS_DEFAULT_COUNTRY = "tts_default_country";

        /**
         * Default text-to-speech locale variant.
         *
         * @deprecated this setting is no longer in use, as of the Ice Cream
         * Sandwich release. Apps should never need to read this setting directly,
         * instead can query the TextToSpeech framework classes for the
         * locale that is in use {@link TextToSpeech#getLanguage()}.
         */
        @Deprecated
        @Readable
        public static final String TTS_DEFAULT_VARIANT = "tts_default_variant";

        /**
         * Stores the default tts locales on a per engine basis. Stored as
         * a comma seperated list of values, each value being of the form
         * {@code engine_name:locale} for example,
         * {@code com.foo.ttsengine:eng-USA,com.bar.ttsengine:esp-ESP}. This
         * supersedes {@link #TTS_DEFAULT_LANG}, {@link #TTS_DEFAULT_COUNTRY} and
         * {@link #TTS_DEFAULT_VARIANT}. Apps should never need to read this
         * setting directly, and can query the TextToSpeech framework classes
         * for the locale that is in use.
         *
         * @hide
         */
        @Readable
        public static final String TTS_DEFAULT_LOCALE = "tts_default_locale";

        /**
         * Space delimited list of plugin packages that are enabled.
         */
        @Readable
        public static final String TTS_ENABLED_PLUGINS = "tts_enabled_plugins";

        /**
         * @deprecated Use {@link android.provider.Settings.Global#WIFI_NETWORKS_AVAILABLE_NOTIFICATION_ON}
         * instead.
         */
        @Deprecated
        public static final String WIFI_NETWORKS_AVAILABLE_NOTIFICATION_ON =
                Global.WIFI_NETWORKS_AVAILABLE_NOTIFICATION_ON;

        /**
         * @deprecated Use {@link android.provider.Settings.Global#WIFI_NETWORKS_AVAILABLE_REPEAT_DELAY}
         * instead.
         */
        @Deprecated
        public static final String WIFI_NETWORKS_AVAILABLE_REPEAT_DELAY =
                Global.WIFI_NETWORKS_AVAILABLE_REPEAT_DELAY;

        /**
         * @deprecated Use {@link android.provider.Settings.Global#WIFI_NUM_OPEN_NETWORKS_KEPT}
         * instead.
         */
        @Deprecated
        public static final String WIFI_NUM_OPEN_NETWORKS_KEPT =
                Global.WIFI_NUM_OPEN_NETWORKS_KEPT;

        /**
         * @deprecated Use {@link android.provider.Settings.Global#WIFI_ON}
         * instead.
         */
        @Deprecated
        public static final String WIFI_ON = Global.WIFI_ON;

        /**
         * The acceptable packet loss percentage (range 0 - 100) before trying
         * another AP on the same network.
         * @deprecated This setting is not used.
         */
        @Deprecated
        @Readable
        public static final String WIFI_WATCHDOG_ACCEPTABLE_PACKET_LOSS_PERCENTAGE =
                "wifi_watchdog_acceptable_packet_loss_percentage";

        /**
         * The number of access points required for a network in order for the
         * watchdog to monitor it.
         * @deprecated This setting is not used.
         */
        @Deprecated
        @Readable
        public static final String WIFI_WATCHDOG_AP_COUNT = "wifi_watchdog_ap_count";

        /**
         * The delay between background checks.
         * @deprecated This setting is not used.
         */
        @Deprecated
        @Readable
        public static final String WIFI_WATCHDOG_BACKGROUND_CHECK_DELAY_MS =
                "wifi_watchdog_background_check_delay_ms";

        /**
         * Whether the Wi-Fi watchdog is enabled for background checking even
         * after it thinks the user has connected to a good access point.
         * @deprecated This setting is not used.
         */
        @Deprecated
        @Readable
        public static final String WIFI_WATCHDOG_BACKGROUND_CHECK_ENABLED =
                "wifi_watchdog_background_check_enabled";

        /**
         * The timeout for a background ping
         * @deprecated This setting is not used.
         */
        @Deprecated
        @Readable
        public static final String WIFI_WATCHDOG_BACKGROUND_CHECK_TIMEOUT_MS =
                "wifi_watchdog_background_check_timeout_ms";

        /**
         * The number of initial pings to perform that *may* be ignored if they
         * fail. Again, if these fail, they will *not* be used in packet loss
         * calculation. For example, one network always seemed to time out for
         * the first couple pings, so this is set to 3 by default.
         * @deprecated This setting is not used.
         */
        @Deprecated
        @Readable
        public static final String WIFI_WATCHDOG_INITIAL_IGNORED_PING_COUNT =
            "wifi_watchdog_initial_ignored_ping_count";

        /**
         * The maximum number of access points (per network) to attempt to test.
         * If this number is reached, the watchdog will no longer monitor the
         * initial connection state for the network. This is a safeguard for
         * networks containing multiple APs whose DNS does not respond to pings.
         * @deprecated This setting is not used.
         */
        @Deprecated
        @Readable
        public static final String WIFI_WATCHDOG_MAX_AP_CHECKS = "wifi_watchdog_max_ap_checks";

        /**
         * @deprecated Use {@link android.provider.Settings.Global#WIFI_WATCHDOG_ON} instead
         */
        @Deprecated
        @Readable
        public static final String WIFI_WATCHDOG_ON = "wifi_watchdog_on";

        /**
         * A comma-separated list of SSIDs for which the Wi-Fi watchdog should be enabled.
         * @deprecated This setting is not used.
         */
        @Deprecated
        @Readable
        public static final String WIFI_WATCHDOG_WATCH_LIST = "wifi_watchdog_watch_list";

        /**
         * The number of pings to test if an access point is a good connection.
         * @deprecated This setting is not used.
         */
        @Deprecated
        @Readable
        public static final String WIFI_WATCHDOG_PING_COUNT = "wifi_watchdog_ping_count";

        /**
         * The delay between pings.
         * @deprecated This setting is not used.
         */
        @Deprecated
        @Readable
        public static final String WIFI_WATCHDOG_PING_DELAY_MS = "wifi_watchdog_ping_delay_ms";

        /**
         * The timeout per ping.
         * @deprecated This setting is not used.
         */
        @Deprecated
        @Readable
        public static final String WIFI_WATCHDOG_PING_TIMEOUT_MS = "wifi_watchdog_ping_timeout_ms";

        /**
         * @deprecated Use
         * {@link android.provider.Settings.Global#WIFI_MAX_DHCP_RETRY_COUNT} instead
         */
        @Deprecated
        public static final String WIFI_MAX_DHCP_RETRY_COUNT = Global.WIFI_MAX_DHCP_RETRY_COUNT;

        /**
         * @deprecated Use
         * {@link android.provider.Settings.Global#WIFI_MOBILE_DATA_TRANSITION_WAKELOCK_TIMEOUT_MS} instead
         */
        @Deprecated
        public static final String WIFI_MOBILE_DATA_TRANSITION_WAKELOCK_TIMEOUT_MS =
                Global.WIFI_MOBILE_DATA_TRANSITION_WAKELOCK_TIMEOUT_MS;

        /**
         * The number of milliseconds to hold on to a PendingIntent based request. This delay gives
         * the receivers of the PendingIntent an opportunity to make a new network request before
         * the Network satisfying the request is potentially removed.
         *
         * @hide
         */
        @Readable
        public static final String CONNECTIVITY_RELEASE_PENDING_INTENT_DELAY_MS =
                "connectivity_release_pending_intent_delay_ms";

        /**
         * Whether background data usage is allowed.
         *
         * @deprecated As of {@link VERSION_CODES#ICE_CREAM_SANDWICH},
         *             availability of background data depends on several
         *             combined factors. When background data is unavailable,
         *             {@link ConnectivityManager#getActiveNetworkInfo()} will
         *             now appear disconnected.
         */
        @Deprecated
        @Readable
        public static final String BACKGROUND_DATA = "background_data";

        /**
         * Origins for which browsers should allow geolocation by default.
         * The value is a space-separated list of origins.
         */
        @Readable
        public static final String ALLOWED_GEOLOCATION_ORIGINS
                = "allowed_geolocation_origins";

        /**
         * The preferred TTY mode     0 = TTy Off, CDMA default
         *                            1 = TTY Full
         *                            2 = TTY HCO
         *                            3 = TTY VCO
         * @hide
         */
        @Readable
        public static final String PREFERRED_TTY_MODE =
                "preferred_tty_mode";

        /**
         * Whether the enhanced voice privacy mode is enabled.
         * 0 = normal voice privacy
         * 1 = enhanced voice privacy
         * @hide
         */
        @Readable
        public static final String ENHANCED_VOICE_PRIVACY_ENABLED = "enhanced_voice_privacy_enabled";

        /**
         * Whether the TTY mode mode is enabled.
         * 0 = disabled
         * 1 = enabled
         * @hide
         */
        @Readable
        public static final String TTY_MODE_ENABLED = "tty_mode_enabled";

        /**
         * User-selected RTT mode. When on, outgoing and incoming calls will be answered as RTT
         * calls when supported by the device and carrier. Boolean value.
         * 0 = OFF
         * 1 = ON
         */
        @Readable
        public static final String RTT_CALLING_MODE = "rtt_calling_mode";

        /**
        /**
         * Controls whether settings backup is enabled.
         * Type: int ( 0 = disabled, 1 = enabled )
         * @hide
         */
        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        @Readable
        public static final String BACKUP_ENABLED = "backup_enabled";

        /**
         * Controls whether application data is automatically restored from backup
         * at install time.
         * Type: int ( 0 = disabled, 1 = enabled )
         * @hide
         */
        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        @Readable
        public static final String BACKUP_AUTO_RESTORE = "backup_auto_restore";

        /**
         * Controls whether framework backup scheduling is enabled.
         * @hide
         */
        public static final String BACKUP_SCHEDULING_ENABLED = "backup_scheduling_enabled";

        /**
         * Indicates whether settings backup has been fully provisioned.
         * Type: int ( 0 = unprovisioned, 1 = fully provisioned )
         * @hide
         */
        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        @Readable
        public static final String BACKUP_PROVISIONED = "backup_provisioned";

        /**
         * Component of the transport to use for backup/restore.
         * @hide
         */
        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        @Readable
        public static final String BACKUP_TRANSPORT = "backup_transport";

        /**
         * Indicates the version for which the setup wizard was last shown. The version gets
         * bumped for each release when there is new setup information to show.
         *
         * @hide
         */
        @SystemApi
        @Readable
        public static final String LAST_SETUP_SHOWN = "last_setup_shown";

        /**
         * The interval in milliseconds after which Wi-Fi is considered idle.
         * When idle, it is possible for the device to be switched from Wi-Fi to
         * the mobile data network.
         * @hide
         * @deprecated Use {@link android.provider.Settings.Global#WIFI_IDLE_MS}
         * instead.
         */
        @Deprecated
        public static final String WIFI_IDLE_MS = Global.WIFI_IDLE_MS;

        /**
         * The global search provider chosen by the user (if multiple global
         * search providers are installed). This will be the provider returned
         * by {@link SearchManager#getGlobalSearchActivity()} if it's still
         * installed. This setting is stored as a flattened component name as
         * per {@link ComponentName#flattenToString()}.
         *
         * @hide
         */
        @Readable
        public static final String SEARCH_GLOBAL_SEARCH_ACTIVITY =
                "search_global_search_activity";

        /**
         * The number of promoted sources in GlobalSearch.
         * @hide
         */
        @Readable
        public static final String SEARCH_NUM_PROMOTED_SOURCES = "search_num_promoted_sources";
        /**
         * The maximum number of suggestions returned by GlobalSearch.
         * @hide
         */
        @Readable
        public static final String SEARCH_MAX_RESULTS_TO_DISPLAY = "search_max_results_to_display";
        /**
         * The number of suggestions GlobalSearch will ask each non-web search source for.
         * @hide
         */
        @Readable
        public static final String SEARCH_MAX_RESULTS_PER_SOURCE = "search_max_results_per_source";
        /**
         * The number of suggestions the GlobalSearch will ask the web search source for.
         * @hide
         */
        @Readable
        public static final String SEARCH_WEB_RESULTS_OVERRIDE_LIMIT =
                "search_web_results_override_limit";
        /**
         * The number of milliseconds that GlobalSearch will wait for suggestions from
         * promoted sources before continuing with all other sources.
         * @hide
         */
        @Readable
        public static final String SEARCH_PROMOTED_SOURCE_DEADLINE_MILLIS =
                "search_promoted_source_deadline_millis";
        /**
         * The number of milliseconds before GlobalSearch aborts search suggesiton queries.
         * @hide
         */
        @Readable
        public static final String SEARCH_SOURCE_TIMEOUT_MILLIS = "search_source_timeout_millis";
        /**
         * The maximum number of milliseconds that GlobalSearch shows the previous results
         * after receiving a new query.
         * @hide
         */
        @Readable
        public static final String SEARCH_PREFILL_MILLIS = "search_prefill_millis";
        /**
         * The maximum age of log data used for shortcuts in GlobalSearch.
         * @hide
         */
        @Readable
        public static final String SEARCH_MAX_STAT_AGE_MILLIS = "search_max_stat_age_millis";
        /**
         * The maximum age of log data used for source ranking in GlobalSearch.
         * @hide
         */
        @Readable
        public static final String SEARCH_MAX_SOURCE_EVENT_AGE_MILLIS =
                "search_max_source_event_age_millis";
        /**
         * The minimum number of impressions needed to rank a source in GlobalSearch.
         * @hide
         */
        @Readable
        public static final String SEARCH_MIN_IMPRESSIONS_FOR_SOURCE_RANKING =
                "search_min_impressions_for_source_ranking";
        /**
         * The minimum number of clicks needed to rank a source in GlobalSearch.
         * @hide
         */
        @Readable
        public static final String SEARCH_MIN_CLICKS_FOR_SOURCE_RANKING =
                "search_min_clicks_for_source_ranking";
        /**
         * The maximum number of shortcuts shown by GlobalSearch.
         * @hide
         */
        @Readable
        public static final String SEARCH_MAX_SHORTCUTS_RETURNED = "search_max_shortcuts_returned";
        /**
         * The size of the core thread pool for suggestion queries in GlobalSearch.
         * @hide
         */
        @Readable
        public static final String SEARCH_QUERY_THREAD_CORE_POOL_SIZE =
                "search_query_thread_core_pool_size";
        /**
         * The maximum size of the thread pool for suggestion queries in GlobalSearch.
         * @hide
         */
        @Readable
        public static final String SEARCH_QUERY_THREAD_MAX_POOL_SIZE =
                "search_query_thread_max_pool_size";
        /**
         * The size of the core thread pool for shortcut refreshing in GlobalSearch.
         * @hide
         */
        @Readable
        public static final String SEARCH_SHORTCUT_REFRESH_CORE_POOL_SIZE =
                "search_shortcut_refresh_core_pool_size";
        /**
         * The maximum size of the thread pool for shortcut refreshing in GlobalSearch.
         * @hide
         */
        @Readable
        public static final String SEARCH_SHORTCUT_REFRESH_MAX_POOL_SIZE =
                "search_shortcut_refresh_max_pool_size";
        /**
         * The maximun time that excess threads in the GlobalSeach thread pools will
         * wait before terminating.
         * @hide
         */
        @Readable
        public static final String SEARCH_THREAD_KEEPALIVE_SECONDS =
                "search_thread_keepalive_seconds";
        /**
         * The maximum number of concurrent suggestion queries to each source.
         * @hide
         */
        @Readable
        public static final String SEARCH_PER_SOURCE_CONCURRENT_QUERY_LIMIT =
                "search_per_source_concurrent_query_limit";

        /**
         * Whether or not alert sounds are played on StorageManagerService events.
         * (0 = false, 1 = true)
         * @hide
         */
        @Readable
        public static final String MOUNT_PLAY_NOTIFICATION_SND = "mount_play_not_snd";

        /**
         * Whether or not UMS auto-starts on UMS host detection. (0 = false, 1 = true)
         * @hide
         */
        @Readable
        public static final String MOUNT_UMS_AUTOSTART = "mount_ums_autostart";

        /**
         * Whether or not a notification is displayed on UMS host detection. (0 = false, 1 = true)
         * @hide
         */
        @Readable
        public static final String MOUNT_UMS_PROMPT = "mount_ums_prompt";

        /**
         * Whether or not a notification is displayed while UMS is enabled. (0 = false, 1 = true)
         * @hide
         */
        @Readable
        public static final String MOUNT_UMS_NOTIFY_ENABLED = "mount_ums_notify_enabled";

        /**
         * If nonzero, ANRs in invisible background processes bring up a dialog.
         * Otherwise, the process will be silently killed.
         *
         * Also prevents ANRs and crash dialogs from being suppressed.
         * @hide
         */
        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        @TestApi
        @Readable
        @SuppressLint("NoSettingsProvider")
        public static final String ANR_SHOW_BACKGROUND = "anr_show_background";

        /**
         * If nonzero, crashes in foreground processes will bring up a dialog.
         * Otherwise, the process will be silently killed.
         * @hide
         */
        @TestApi
        @Readable
        @SuppressLint("NoSettingsProvider")
        public static final String SHOW_FIRST_CRASH_DIALOG_DEV_OPTION =
                "show_first_crash_dialog_dev_option";

        /**
         * The {@link ComponentName} string of the service to be used as the voice recognition
         * service.
         *
         * @hide
         */
        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        @Readable
        public static final String VOICE_RECOGNITION_SERVICE = "voice_recognition_service";

        /**
         * The {@link ComponentName} string of the selected spell checker service which is
         * one of the services managed by the text service manager.
         *
         * @hide
         */
        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        @TestApi
        @Readable
        @SuppressLint("NoSettingsProvider")
        public static final String SELECTED_SPELL_CHECKER = "selected_spell_checker";

        /**
         * {@link android.view.textservice.SpellCheckerSubtype#hashCode()} of the selected subtype
         * of the selected spell checker service which is one of the services managed by the text
         * service manager.
         *
         * @hide
         */
        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        @TestApi
        @Readable
        @SuppressLint("NoSettingsProvider")
        public static final String SELECTED_SPELL_CHECKER_SUBTYPE =
                "selected_spell_checker_subtype";

        /**
         * Whether spell checker is enabled or not.
         *
         * @hide
         */
        @Readable
        public static final String SPELL_CHECKER_ENABLED = "spell_checker_enabled";

        /**
         * What happens when the user presses the Power button while in-call
         * and the screen is on.<br/>
         * <b>Values:</b><br/>
         * 1 - The Power button turns off the screen and locks the device. (Default behavior)<br/>
         * 2 - The Power button hangs up the current call.<br/>
         *
         * @hide
         */
        @UnsupportedAppUsage
        @Readable
        public static final String INCALL_POWER_BUTTON_BEHAVIOR = "incall_power_button_behavior";

        /**
         * Whether the user allows minimal post processing or not.
         *
         * <p>Values:
         * 0 - Not allowed. Any preferences set through the Window.setPreferMinimalPostProcessing
         *     API will be ignored.
         * 1 - Allowed. Any preferences set through the Window.setPreferMinimalPostProcessing API
         *     will be respected and the appropriate signals will be sent to display.
         *     (Default behaviour)
         *
         * @hide
         */
        @Readable
        public static final String MINIMAL_POST_PROCESSING_ALLOWED =
                "minimal_post_processing_allowed";

        /**
         * No mode switching will happen.
         *
         * @see #MATCH_CONTENT_FRAME_RATE
         * @hide
         */
        public static final int MATCH_CONTENT_FRAMERATE_NEVER = 0;

        /**
         * Allow only refresh rate switching between modes in the same configuration group.
         * This way only switches without visual interruptions for the user will be allowed.
         *
         * @see #MATCH_CONTENT_FRAME_RATE
         * @hide
         */
        public static final int MATCH_CONTENT_FRAMERATE_SEAMLESSS_ONLY = 1;

        /**
         * Allow refresh rate switching between all refresh rates even if the switch will have
         * visual interruptions for the user.
         *
         * @see #MATCH_CONTENT_FRAME_RATE
         * @hide
         */
        public static final int MATCH_CONTENT_FRAMERATE_ALWAYS = 2;

        /**
         * User's preference for refresh rate switching.
         *
         * <p>Values:
         * 0 - Never switch refresh rates.
         * 1 - Switch refresh rates only when it can be done seamlessly. (Default behaviour)
         * 2 - Always prefer refresh rate switching even if it's going to have visual interruptions
         *     for the user.
         *
         * @see android.view.Surface#setFrameRate
         * @see #MATCH_CONTENT_FRAMERATE_NEVER
         * @see #MATCH_CONTENT_FRAMERATE_SEAMLESSS_ONLY
         * @see #MATCH_CONTENT_FRAMERATE_ALWAYS
         * @hide
         */
        public static final String MATCH_CONTENT_FRAME_RATE =
                "match_content_frame_rate";

        /**
         * INCALL_POWER_BUTTON_BEHAVIOR value for "turn off screen".
         * @hide
         */
        public static final int INCALL_POWER_BUTTON_BEHAVIOR_SCREEN_OFF = 0x1;

        /**
         * INCALL_POWER_BUTTON_BEHAVIOR value for "hang up".
         * @hide
         */
        public static final int INCALL_POWER_BUTTON_BEHAVIOR_HANGUP = 0x2;

        /**
         * INCALL_POWER_BUTTON_BEHAVIOR default value.
         * @hide
         */
        public static final int INCALL_POWER_BUTTON_BEHAVIOR_DEFAULT =
                INCALL_POWER_BUTTON_BEHAVIOR_SCREEN_OFF;

        /**
         * What happens when the user presses the Back button while in-call
         * and the screen is on.<br/>
         * <b>Values:</b><br/>
         * 0 - The Back buttons does nothing different.<br/>
         * 1 - The Back button hangs up the current call.<br/>
         *
         * @hide
         */
        @Readable
        public static final String INCALL_BACK_BUTTON_BEHAVIOR = "incall_back_button_behavior";

        /**
         * INCALL_BACK_BUTTON_BEHAVIOR value for no action.
         * @hide
         */
        public static final int INCALL_BACK_BUTTON_BEHAVIOR_NONE = 0x0;

        /**
         * INCALL_BACK_BUTTON_BEHAVIOR value for "hang up".
         * @hide
         */
        public static final int INCALL_BACK_BUTTON_BEHAVIOR_HANGUP = 0x1;

        /**
         * INCALL_POWER_BUTTON_BEHAVIOR default value.
         * @hide
         */
        public static final int INCALL_BACK_BUTTON_BEHAVIOR_DEFAULT =
                INCALL_BACK_BUTTON_BEHAVIOR_NONE;

        /**
         * Whether the device should wake when the wake gesture sensor detects motion.
         * @hide
         */
        @Readable
        public static final String WAKE_GESTURE_ENABLED = "wake_gesture_enabled";

        /**
         * Whether the device should doze if configured.
         * @hide
         */
        @UnsupportedAppUsage
        @Readable
        public static final String DOZE_ENABLED = "doze_enabled";

        /**
         * Indicates whether doze should be always on.
         * <p>
         * Type: int (0 for false, 1 for true)
         *
         * @hide
         */
        @SystemApi
        @Readable
        public static final String DOZE_ALWAYS_ON = "doze_always_on";

        /**
         * Indicates whether doze turns on automatically
         * 0 = disabled (default)
         * 1 = from sunset to sunrise
         * 2 = custom time
         * 3 = from sunset till a time
         * 4 = from a time till sunrise
         * @hide
         */
        @Readable
        public static final String DOZE_ALWAYS_ON_AUTO_MODE = "doze_always_on_auto_mode";

        /**
         * The custom time {@link DOZE_ALWAYS_ON} should be on at
         * Only relevant when {@link DOZE_ALWAYS_ON_AUTO_MODE} is set to 2 and above
         * 0 = Disabled (default)
         * format: HH:mm,HH:mm (since,till)
         * @hide
         */
        @Readable
        public static final String DOZE_ALWAYS_ON_AUTO_TIME = "doze_always_on_auto_time";

        /**
         * Whether the device should pulse on pick up gesture.
         * @hide
         */
        @Readable
        public static final String DOZE_PICK_UP_GESTURE = "doze_pick_up_gesture";

        /**
         * Whether the device should pulse on long press gesture.
         * @hide
         */
        @Readable
        public static final String DOZE_PULSE_ON_LONG_PRESS = "doze_pulse_on_long_press";

        /**
         * Whether the device should pulse on double tap gesture.
         * @hide
         */
        @Readable
        public static final String DOZE_DOUBLE_TAP_GESTURE = "doze_pulse_on_double_tap";

        /**
         * Whether the device should respond to the SLPI tap gesture.
         * @hide
         */
        @Readable
        public static final String DOZE_TAP_SCREEN_GESTURE = "doze_tap_gesture";

        /**
         * Gesture that wakes up the display, showing some version of the lock screen.
         * @hide
         */
        @Readable
        public static final String DOZE_WAKE_LOCK_SCREEN_GESTURE = "doze_wake_screen_gesture";

        /**
         * Gesture that wakes up the display, toggling between {@link Display.STATE_OFF} and
         * {@link Display.STATE_DOZE}.
         * @hide
         */
        @Readable
        public static final String DOZE_WAKE_DISPLAY_GESTURE = "doze_wake_display_gesture";

        /**
         * Gesture that wakes up the display on quick pickup, toggling between
         * {@link Display.STATE_OFF} and {@link Display.STATE_DOZE}.
         * @hide
         */
        public static final String DOZE_QUICK_PICKUP_GESTURE = "doze_quick_pickup_gesture";

        /**
         * Whether the device should suppress the current doze configuration and disable dozing.
         * @hide
         */
        @Readable
        public static final String SUPPRESS_DOZE = "suppress_doze";

        /**
         * Pulse notifications on tilt
         * @hide
         */
        public static final String DOZE_TILT_GESTURE = "doze_tilt_gesture";

        /**
         * Pulse notifications on hand wave
         * @hide
         */
        public static final String DOZE_HANDWAVE_GESTURE = "doze_handwave_gesture";

        /**
         * Pulse notifications on removal from pocket
         * @hide
         */
        public static final String DOZE_POCKET_GESTURE = "doze_pocket_gesture";

        /**
         * Wake up instead of pulsing notifications
         * @hide
         */
        public static final String RAISE_TO_WAKE_GESTURE = "raise_to_wake_gesture";

        /**
         * Vibrate when pulsing notifications on gesture
         * @hide
         */
        public static final String DOZE_GESTURE_VIBRATE = "doze_gesture_vibrate";

        /**
         * Gesture that skips media.
         * @hide
         */
        @Readable
        public static final String SKIP_GESTURE = "skip_gesture";

        /**
         * Count of successful gestures.
         * @hide
         */
        @Readable
        public static final String SKIP_GESTURE_COUNT = "skip_gesture_count";

        /**
         * Count of non-gesture interaction.
         * @hide
         */
        @Readable
        public static final String SKIP_TOUCH_COUNT = "skip_touch_count";

        /**
         * Direction to advance media for skip gesture
         * @hide
         */
        @Readable
        public static final String SKIP_DIRECTION = "skip_gesture_direction";

        /**
         * Gesture that silences sound (alarms, notification, calls).
         * @hide
         */
        @Readable
        public static final String SILENCE_GESTURE = "silence_gesture";

        /**
         * Count of successful silence alarms gestures.
         * @hide
         */
        @Readable
        public static final String SILENCE_ALARMS_GESTURE_COUNT = "silence_alarms_gesture_count";

        /**
         * Count of successful silence timer gestures.
         * @hide
         */
        @Readable
        public static final String SILENCE_TIMER_GESTURE_COUNT = "silence_timer_gesture_count";

        /**
         * Count of successful silence call gestures.
         * @hide
         */
        @Readable
        public static final String SILENCE_CALL_GESTURE_COUNT = "silence_call_gesture_count";

        /**
         * Count of non-gesture interaction.
         * @hide
         */
        @Readable
        public static final String SILENCE_ALARMS_TOUCH_COUNT = "silence_alarms_touch_count";

        /**
         * Count of non-gesture interaction.
         * @hide
         */
        @Readable
        public static final String SILENCE_TIMER_TOUCH_COUNT = "silence_timer_touch_count";

        /**
         * Count of non-gesture interaction.
         * @hide
         */
        @Readable
        public static final String SILENCE_CALL_TOUCH_COUNT = "silence_call_touch_count";

        /**
         * Number of successful "Motion Sense" tap gestures to pause media.
         * @hide
         */
        @Readable
        public static final String AWARE_TAP_PAUSE_GESTURE_COUNT = "aware_tap_pause_gesture_count";

        /**
         * Number of touch interactions to pause media when a "Motion Sense" gesture could
         * have been used.
         * @hide
         */
        @Readable
        public static final String AWARE_TAP_PAUSE_TOUCH_COUNT = "aware_tap_pause_touch_count";

        /**
         * For user preference if swipe bottom to expand notification gesture enabled.
         * @hide
         */
        public static final String SWIPE_BOTTOM_TO_NOTIFICATION_ENABLED =
                "swipe_bottom_to_notification_enabled";

        /**
         * Controls whether One-Handed mode is currently activated.
         * @hide
         */
        public static final String ONE_HANDED_MODE_ACTIVATED = "one_handed_mode_activated";

        /**
         * For user preference if One-Handed Mode enabled.
         * @hide
         */
        public static final String ONE_HANDED_MODE_ENABLED = "one_handed_mode_enabled";

        /**
         * For user preference if One-Handed Mode timeout.
         * @hide
         */
        public static final String ONE_HANDED_MODE_TIMEOUT = "one_handed_mode_timeout";

        /**
         * For user taps app to exit One-Handed Mode.
         * @hide
         */
        public static final String TAPS_APP_TO_EXIT = "taps_app_to_exit";

        /**
         * Internal use, one handed mode tutorial showed times.
         * @hide
         */
        public static final String ONE_HANDED_TUTORIAL_SHOW_COUNT =
                "one_handed_tutorial_show_count";

        /**
         * Toggle to enable/disable for the apps to use the Ui translation for Views. The value
         * indicates whether the Ui translation is enabled by the user.
         * <p>
         * Type: {@code int} ({@code 0} for disabled, {@code 1} for enabled)
         *
         * @hide
         */
        @SystemApi
        @Readable
        @SuppressLint("NoSettingsProvider")
        public static final String UI_TRANSLATION_ENABLED = "ui_translation_enabled";

        /**
         * The current night mode that has been selected by the user.  Owned
         * and controlled by UiModeManagerService.  Constants are as per
         * UiModeManager.
         * @hide
         */
        @Readable
        public static final String UI_NIGHT_MODE = "ui_night_mode";

        /**
         * The current night mode custom type that has been selected by the user.  Owned
         * and controlled by UiModeManagerService. Constants are as per UiModeManager.
         * @hide
         */
        @Readable
        @SuppressLint("NoSettingsProvider")
        public static final String UI_NIGHT_MODE_CUSTOM_TYPE = "ui_night_mode_custom_type";

        /**
         * The current night mode that has been overridden to turn on by the system.  Owned
         * and controlled by UiModeManagerService.  Constants are as per
         * UiModeManager.
         * @hide
         */
        @Readable
        public static final String UI_NIGHT_MODE_OVERRIDE_ON = "ui_night_mode_override_on";

        /**
         * The last computed night mode bool the last time the phone was on
         * @hide
         */
        public static final String UI_NIGHT_MODE_LAST_COMPUTED = "ui_night_mode_last_computed";

        /**
         * The current night mode that has been overridden to turn off by the system.  Owned
         * and controlled by UiModeManagerService.  Constants are as per
         * UiModeManager.
         * @hide
         */
        @Readable
        public static final String UI_NIGHT_MODE_OVERRIDE_OFF = "ui_night_mode_override_off";

        /**
         * Whether screensavers are enabled.
         * @hide
         */
        @Readable
        public static final String SCREENSAVER_ENABLED = "screensaver_enabled";

        /**
         * The user's chosen screensaver components.
         *
         * These will be launched by the PhoneWindowManager after a timeout when not on
         * battery, or upon dock insertion (if SCREENSAVER_ACTIVATE_ON_DOCK is set to 1).
         * @hide
         */
        @Readable
        public static final String SCREENSAVER_COMPONENTS = "screensaver_components";

        /**
         * If screensavers are enabled, whether the screensaver should be automatically launched
         * when the device is inserted into a (desk) dock.
         * @hide
         */
        @Readable
        public static final String SCREENSAVER_ACTIVATE_ON_DOCK = "screensaver_activate_on_dock";

        /**
         * If screensavers are enabled, whether the screensaver should be automatically launched
         * when the screen times out when not on battery.
         * @hide
         */
        @Readable
        public static final String SCREENSAVER_ACTIVATE_ON_SLEEP = "screensaver_activate_on_sleep";

        /**
         * If screensavers are enabled, the default screensaver component.
         * @hide
         */
        @Readable
        public static final String SCREENSAVER_DEFAULT_COMPONENT = "screensaver_default_component";

        /**
         * Whether complications are enabled to be shown over the screensaver by the user.
         *
         * @hide
         */
        public static final String SCREENSAVER_COMPLICATIONS_ENABLED =
                "screensaver_complications_enabled";

        /**
         * Whether home controls are enabled to be shown over the screensaver by the user.
         *
         * @hide
         */
        public static final String SCREENSAVER_HOME_CONTROLS_ENABLED =
                "screensaver_home_controls_enabled";


        /**
         * Default, indicates that the user has not yet started the dock setup flow.
         *
         * @hide
         */
        public static final int DOCK_SETUP_NOT_STARTED = 0;

        /**
         * Indicates that the user has started but not yet completed dock setup.
         * One of the possible states for {@link #DOCK_SETUP_STATE}.
         *
         * @hide
         */
        public static final int DOCK_SETUP_STARTED = 1;

        /**
         * Indicates that the user has snoozed dock setup and will complete it later.
         * One of the possible states for {@link #DOCK_SETUP_STATE}.
         *
         * @hide
         */
        public static final int DOCK_SETUP_PAUSED = 2;

        /**
         * Indicates that the user has been prompted to start dock setup.
         * One of the possible states for {@link #DOCK_SETUP_STATE}.
         *
         * @hide
         */
        public static final int DOCK_SETUP_PROMPTED = 3;

        /**
         * Indicates that the user has started dock setup but never finished it.
         * One of the possible states for {@link #DOCK_SETUP_STATE}.
         *
         * @hide
         */
        public static final int DOCK_SETUP_INCOMPLETE = 4;

        /**
         * Indicates that the user has completed dock setup.
         * One of the possible states for {@link #DOCK_SETUP_STATE}.
         *
         * @hide
         */
        public static final int DOCK_SETUP_COMPLETED = 10;

        /**
         * Indicates that dock setup timed out before the user could complete it.
         * One of the possible states for {@link #DOCK_SETUP_STATE}.
         *
         * @hide
         */
        public static final int DOCK_SETUP_TIMED_OUT = 11;

        /** @hide */
        @Retention(RetentionPolicy.SOURCE)
        @IntDef({
                DOCK_SETUP_NOT_STARTED,
                DOCK_SETUP_STARTED,
                DOCK_SETUP_PAUSED,
                DOCK_SETUP_PROMPTED,
                DOCK_SETUP_INCOMPLETE,
                DOCK_SETUP_COMPLETED,
                DOCK_SETUP_TIMED_OUT
        })
        public @interface DockSetupState {
        }

        /**
         * Defines the user's current state of dock setup.
         * The possible states are defined in {@link DockSetupState}.
         *
         * @hide
         */
        public static final String DOCK_SETUP_STATE = "dock_setup_state";


        /**
         * Default, indicates that the user has not yet started the hub mode tutorial.
         *
         * @hide
         */
        public static final int HUB_MODE_TUTORIAL_NOT_STARTED = 0;

        /**
         * Indicates that the user has started but not yet completed the hub mode tutorial.
         * One of the possible states for {@link #HUB_MODE_TUTORIAL_STATE}.
         *
         * @hide
         */
        public static final int HUB_MODE_TUTORIAL_STARTED = 1;

        /**
         * Any value greater than or equal to this value is considered that the user has
         * completed the hub mode tutorial.
         *
         * One of the possible states for {@link #HUB_MODE_TUTORIAL_STATE}.
         *
         * @hide
         */
        public static final int HUB_MODE_TUTORIAL_COMPLETED = 10;

        /** @hide */
        @Retention(RetentionPolicy.SOURCE)
        @IntDef({
                HUB_MODE_TUTORIAL_NOT_STARTED,
                HUB_MODE_TUTORIAL_STARTED,
                HUB_MODE_TUTORIAL_COMPLETED
        })
        public @interface HubModeTutorialState {
        }

        /**
         * Defines the user's current state of navigating through the hub mode tutorial.
         * Some possible states are defined in {@link HubModeTutorialState}.
         *
         * Any value greater than or equal to {@link HUB_MODE_TUTORIAL_COMPLETED} indicates that
         * the user has completed that version of the hub mode tutorial. And tutorial may be
         * shown again when a new version becomes available.
         * @hide
         */
        public static final String HUB_MODE_TUTORIAL_STATE = "hub_mode_tutorial_state";

        /**
         * The default NFC payment component
         *
         * @deprecated please use {@link android.app.role.RoleManager#getRoleHolders(String)}
         * with {@link android.app.role.RoleManager#ROLE_WALLET} parameter.
         * @hide
         */
        @Deprecated
        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        public static final String NFC_PAYMENT_DEFAULT_COMPONENT = "nfc_payment_default_component";

        /**
         * Whether NFC payment is handled by the foreground application or a default.
         * @hide
         */
        @Readable
        public static final String NFC_PAYMENT_FOREGROUND = "nfc_payment_foreground";

        /**
         * Specifies the package name currently configured to be the primary sms application
         * @hide
         */
        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        @Readable
        public static final String SMS_DEFAULT_APPLICATION = "sms_default_application";

        /**
         * Specifies the package name currently configured to be the default dialer application
         * @hide
         */
        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        @Readable
        public static final String DIALER_DEFAULT_APPLICATION = "dialer_default_application";

        /**
         * Specifies the component name currently configured to be the default call screening
         * application
         * @hide
         */
        @Readable
        public static final String CALL_SCREENING_DEFAULT_COMPONENT =
                "call_screening_default_component";

        /**
         * Specifies the package name currently configured to be the emergency assistance application
         *
         * @see android.telephony.TelephonyManager#ACTION_EMERGENCY_ASSISTANCE
         *
         * @hide
         */
        @Readable
        public static final String EMERGENCY_ASSISTANCE_APPLICATION = "emergency_assistance_application";

        /**
         * Specifies whether the current app context on scren (assist data) will be sent to the
         * assist application (active voice interaction service).
         *
         * @hide
         */
        @Readable
        public static final String ASSIST_STRUCTURE_ENABLED = "assist_structure_enabled";

        /**
         * Specifies whether a screenshot of the screen contents will be sent to the assist
         * application (active voice interaction service).
         *
         * @hide
         */
        @Readable
        public static final String ASSIST_SCREENSHOT_ENABLED = "assist_screenshot_enabled";

        /**
         * Specifies whether the screen will show an animation if screen contents are sent to the
         * assist application (active voice interaction service).
         *
         * Note that the disclosure will be forced for third-party assistants or if the device
         * does not support disabling it.
         *
         * @hide
         */
        @Readable
        public static final String ASSIST_DISCLOSURE_ENABLED = "assist_disclosure_enabled";

        /**
         * Control if rotation suggestions are sent to System UI when in rotation locked mode.
         * Done to enable screen rotation while the screen rotation is locked. Enabling will
         * poll the accelerometer in rotation locked mode.
         *
         * If 0, then rotation suggestions are not sent to System UI. If 1, suggestions are sent.
         *
         * @hide
         */
        @Readable
        public static final String SHOW_ROTATION_SUGGESTIONS = "show_rotation_suggestions";

        /**
         * The disabled state of SHOW_ROTATION_SUGGESTIONS.
         * @hide
         */
        public static final int SHOW_ROTATION_SUGGESTIONS_DISABLED = 0x0;

        /**
         * The enabled state of SHOW_ROTATION_SUGGESTIONS.
         * @hide
         */
        public static final int SHOW_ROTATION_SUGGESTIONS_ENABLED = 0x1;

        /**
         * The default state of SHOW_ROTATION_SUGGESTIONS.
         * @hide
         */
        public static final int SHOW_ROTATION_SUGGESTIONS_DEFAULT =
                SHOW_ROTATION_SUGGESTIONS_ENABLED;

        /**
         * The number of accepted rotation suggestions. Used to determine if the user has been
         * introduced to rotation suggestions.
         * @hide
         */
        @Readable
        public static final String NUM_ROTATION_SUGGESTIONS_ACCEPTED =
                "num_rotation_suggestions_accepted";

        /**
         * Read only list of the service components that the current user has explicitly allowed to
         * see and assist with all of the user's notifications.
         *
         * @deprecated Use
         * {@link NotificationManager#isNotificationAssistantAccessGranted(ComponentName)}.
         * @hide
         */
        @Deprecated
        @Readable
        public static final String ENABLED_NOTIFICATION_ASSISTANT =
                "enabled_notification_assistant";

        /**
         * Read only list of the service components that the current user has explicitly allowed to
         * see all of the user's notifications, separated by ':'.
         *
         * @hide
         * @deprecated Use
         * {@link NotificationManager#isNotificationListenerAccessGranted(ComponentName)}.
         */
        @Deprecated
        @UnsupportedAppUsage
        @Readable
        public static final String ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners";

        /**
         * Read only list of the packages that the current user has explicitly allowed to
         * manage do not disturb, separated by ':'.
         *
         * @deprecated Use {@link NotificationManager#isNotificationPolicyAccessGranted()}.
         * @hide
         */
        @Deprecated
        @TestApi
        @Readable
        public static final String ENABLED_NOTIFICATION_POLICY_ACCESS_PACKAGES =
                "enabled_notification_policy_access_packages";

        /**
         * Defines whether managed profile ringtones should be synced from it's parent profile
         * <p>
         * 0 = ringtones are not synced
         * 1 = ringtones are synced from the profile's parent (default)
         * <p>
         * This value is only used for managed profiles.
         * @hide
         */
        @TestApi
        @Readable
        @RequiresPermission(Manifest.permission.WRITE_SECURE_SETTINGS)
        public static final String SYNC_PARENT_SOUNDS = "sync_parent_sounds";

        /**
         * @hide
         */
        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        @TestApi
        @Readable
        public static final String IMMERSIVE_MODE_CONFIRMATIONS = "immersive_mode_confirmations";

        /**
         * This is the query URI for finding a print service to install.
         *
         * @hide
         */
        @Readable
        public static final String PRINT_SERVICE_SEARCH_URI = "print_service_search_uri";

        /**
         * This is the query URI for finding a NFC payment service to install.
         *
         * @hide
         */
        @Readable
        public static final String PAYMENT_SERVICE_SEARCH_URI = "payment_service_search_uri";

        /**
         * This is the query URI for finding a auto fill service to install.
         *
         * @hide
         */
        @Readable
        public static final String AUTOFILL_SERVICE_SEARCH_URI = "autofill_service_search_uri";

        /**
         * If enabled, apps should try to skip any introductory hints on first launch. This might
         * apply to users that are already familiar with the environment or temporary users.
         * <p>
         * Type : int (0 to show hints, 1 to skip showing hints)
         */
        @Readable
        public static final String SKIP_FIRST_USE_HINTS = "skip_first_use_hints";

        /**
         * Persisted playback time after a user confirmation of an unsafe volume level.
         *
         * @hide
         */
        @Readable
        public static final String UNSAFE_VOLUME_MUSIC_ACTIVE_MS = "unsafe_volume_music_active_ms";

        /**
         * Indicates whether the spatial audio feature was enabled for this user.
         *
         * Type : int (0 disabled, 1 enabled)
         *
         * @hide
         */
        public static final String SPATIAL_AUDIO_ENABLED = "spatial_audio_enabled";

        /**
         * Internal collection of audio device inventory items
         * The device item stored are {@link com.android.server.audio.AdiDeviceState}
         * @hide
         */
        public static final String AUDIO_DEVICE_INVENTORY = "audio_device_inventory";

        /**
         * Stores a boolean that defines whether the CSD as a feature is enabled or not.
         * @hide
         */
        public static final String AUDIO_SAFE_CSD_AS_A_FEATURE_ENABLED =
                "audio_safe_csd_as_a_feature_enabled";

        /**
         * Indicates whether notification display on the lock screen is enabled.
         * <p>
         * Type: int (0 for false, 1 for true)
         *
         * @hide
         */
        @SystemApi
        @Readable
        public static final String LOCK_SCREEN_SHOW_NOTIFICATIONS =
                "lock_screen_show_notifications";

        /**
         * Indicates whether the lock screen should display silent notifications.
         * <p>
         * Type: int (0 for false, 1 for true)
         *
         * @hide
         */
        @Readable
        public static final String LOCK_SCREEN_SHOW_SILENT_NOTIFICATIONS =
                "lock_screen_show_silent_notifications";

        /**
         * Indicates whether "seen" notifications should be suppressed from the lockscreen.
         * <p>
         * Type: int (0 for unset, 1 for true, 2 for false)
         *
         * @hide
         */
        public static final String LOCK_SCREEN_SHOW_ONLY_UNSEEN_NOTIFICATIONS =
                "lock_screen_show_only_unseen_notifications";

        /**
         * Indicates whether snooze options should be shown on notifications
         * <p>
         * Type: int (0 for false, 1 for true)
         *
         * @hide
         */
        @Readable
        public static final String SHOW_NOTIFICATION_SNOOZE = "show_notification_snooze";

       /**
         * 1 if it is allowed to remove the primary GAIA account. 0 by default.
         * @hide
         */
        public static final String ALLOW_PRIMARY_GAIA_ACCOUNT_REMOVAL_FOR_TESTS =
                "allow_primary_gaia_account_removal_for_tests";

        /**
         * List of TV inputs that are currently hidden. This is a string
         * containing the IDs of all hidden TV inputs. Each ID is encoded by
         * {@link android.net.Uri#encode(String)} and separated by ':'.
         * @hide
         */
        @Readable
        public static final String TV_INPUT_HIDDEN_INPUTS = "tv_input_hidden_inputs";

        /**
         * List of custom TV input labels. This is a string containing <TV input id, custom name>
         * pairs. TV input id and custom name are encoded by {@link android.net.Uri#encode(String)}
         * and separated by ','. Each pair is separated by ':'.
         * @hide
         */
        @Readable
        public static final String TV_INPUT_CUSTOM_LABELS = "tv_input_custom_labels";

        /**
         * Whether TV app uses non-system inputs.
         *
         * <p>
         * The value is boolean (1 or 0), where 1 means non-system TV inputs are allowed,
         * and 0 means non-system TV inputs are not allowed.
         *
         * <p>
         * Devices such as sound bars may have changed the system property allow_third_party_inputs
         * to false so the TV Application only uses HDMI and other built in inputs. This setting
         * allows user to override the default and have the TV Application use third party TV inputs
         * available on play store.
         *
         * @hide
         */
        @Readable
        public static final String TV_APP_USES_NON_SYSTEM_INPUTS = "tv_app_uses_non_system_inputs";

        /**
         * Whether automatic routing of system audio to USB audio peripheral is disabled.
         * The value is boolean (1 or 0), where 1 means automatic routing is disabled,
         * and 0 means automatic routing is enabled.
         *
         * @hide
         */
        @Readable
        public static final String USB_AUDIO_AUTOMATIC_ROUTING_DISABLED =
                "usb_audio_automatic_routing_disabled";

        /**
         * The timeout in milliseconds before the device fully goes to sleep after
         * a period of inactivity.  This value sets an upper bound on how long the device
         * will stay awake or dreaming without user activity.  It should generally
         * be longer than {@link Settings.System#SCREEN_OFF_TIMEOUT} as otherwise the device
         * will sleep before it ever has a chance to dream.
         * <p>
         * Use -1 to disable this timeout.
         * </p>
         *
         * @hide
         */
        @Readable
        public static final String SLEEP_TIMEOUT = "sleep_timeout";

        /**
         * The timeout in milliseconds before the device goes to sleep due to user inattentiveness,
         * even if the system is holding wakelocks. It should generally be longer than {@code
         * config_attentiveWarningDuration}, as otherwise the device will show the attentive
         * warning constantly. Small timeouts are discouraged, as they will cause the device to
         * go to sleep quickly after waking up.
         * <p>
         * Use -1 to disable this timeout.
         * </p>
         *
         * @hide
         */
        @Readable
        public static final String ATTENTIVE_TIMEOUT = "attentive_timeout";

        /**
         * Controls whether double tap to wake is enabled.
         * @hide
         */
        @Readable
        public static final String DOUBLE_TAP_TO_WAKE = "double_tap_to_wake";

        /**
         * The current assistant component. It could be a voice interaction service,
         * or an activity that handles ACTION_ASSIST, or empty which means using the default
         * handling.
         *
         * <p>This should be set indirectly by setting the {@link
         * android.app.role.RoleManager#ROLE_ASSISTANT assistant role}.
         *
         * @hide
         */
        @UnsupportedAppUsage
        @Readable
        public static final String ASSISTANT = "assistant";

        /**
         * Whether the camera launch gesture should be disabled.
         *
         * @hide
         */
        @Readable
        public static final String CAMERA_GESTURE_DISABLED = "camera_gesture_disabled";

        /**
         * Whether the emergency gesture should be enabled.
         *
         * @hide
         */
        public static final String EMERGENCY_GESTURE_ENABLED = "emergency_gesture_enabled";

        /**
         * Whether the emergency gesture sound should be enabled.
         *
         * @hide
         */
        public static final String EMERGENCY_GESTURE_SOUND_ENABLED =
                "emergency_gesture_sound_enabled";

        /**
         * Whether the emergency gesture UI is currently showing.
         *
         * @hide
         */
        public static final String EMERGENCY_GESTURE_UI_SHOWING = "emergency_gesture_ui_showing";

        /**
         * The last time the emergency gesture UI was started.
         *
         * @hide
         */
        public static final String EMERGENCY_GESTURE_UI_LAST_STARTED_MILLIS =
                "emergency_gesture_ui_last_started_millis";

        /**
         * Whether the camera launch gesture to double tap the power button when the screen is off
         * should be disabled.
         *
         * @hide
         */
        @Readable
        public static final String CAMERA_DOUBLE_TAP_POWER_GESTURE_DISABLED =
                "camera_double_tap_power_gesture_disabled";

        /**
         * Whether the camera double twist gesture to flip between front and back mode should be
         * enabled.
         *
         * @hide
         */
        @Readable
        public static final String CAMERA_DOUBLE_TWIST_TO_FLIP_ENABLED =
                "camera_double_twist_to_flip_enabled";

        /**
         * Whether or not the smart camera lift trigger that launches the camera when the user moves
         * the phone into a position for taking photos should be enabled.
         *
         * @hide
         */
        @Readable
        public static final String CAMERA_LIFT_TRIGGER_ENABLED = "camera_lift_trigger_enabled";

        /**
         * The default enable state of the camera lift trigger.
         *
         * @hide
         */
        public static final int CAMERA_LIFT_TRIGGER_ENABLED_DEFAULT = 1;

        /**
         * Whether or not the flashlight (camera torch mode) is available required to turn
         * on flashlight.
         *
         * @hide
         */
        @Readable
        public static final String FLASHLIGHT_AVAILABLE = "flashlight_available";

        /**
         * Whether or not flashlight is enabled.
         *
         * @hide
         */
        @Readable
        public static final String FLASHLIGHT_ENABLED = "flashlight_enabled";

        /**
         * Whether or not face unlock is allowed on Keyguard.
         * @hide
         */
        @Readable
        public static final String FACE_UNLOCK_KEYGUARD_ENABLED = "face_unlock_keyguard_enabled";

        /**
         * Whether or not face unlock dismisses the keyguard.
         * @hide
         */
        @Readable
        public static final String FACE_UNLOCK_DISMISSES_KEYGUARD =
                "face_unlock_dismisses_keyguard";

        /**
         * Whether or not media is shown automatically when bypassing as a heads up.
         * @hide
         */
        @Readable
        public static final String SHOW_MEDIA_WHEN_BYPASSING =
                "show_media_when_bypassing";

        /**
         * Whether or not face unlock requires attention. This is a cached value, the source of
         * truth is obtained through the HAL.
         * @hide
         */
        @Readable
        public static final String FACE_UNLOCK_ATTENTION_REQUIRED =
                "face_unlock_attention_required";

        /**
         * Whether or not face unlock requires a diverse set of poses during enrollment. This is a
         * cached value, the source of truth is obtained through the HAL.
         * @hide
         */
        @Readable
        public static final String FACE_UNLOCK_DIVERSITY_REQUIRED =
                "face_unlock_diversity_required";


        /**
         * Whether or not face unlock is allowed for apps (through BiometricPrompt).
         * @hide
         */
        @Readable
        public static final String FACE_UNLOCK_APP_ENABLED = "face_unlock_app_enabled";

        /**
         * Whether or not face unlock always requires user confirmation, meaning {@link
         * android.hardware.biometrics.BiometricPrompt.Builder#setConfirmationRequired(boolean)}
         * is always 'true'. This overrides the behavior that apps choose in the
         * setConfirmationRequired API.
         * @hide
         */
        @Readable
        public static final String FACE_UNLOCK_ALWAYS_REQUIRE_CONFIRMATION =
                "face_unlock_always_require_confirmation";

        /**
         * Whether or not a user should re enroll their face.
         *
         * Face unlock re enroll.
         *  0 = No re enrollment.
         *  1 = Re enrollment is required.
         *
         * @hide
         */
        @Readable
        public static final String FACE_UNLOCK_RE_ENROLL = "face_unlock_re_enroll";

        /**
         * The time (in millis) to wait for a power button before sending a
         * successful auth in to keyguard(for side fingerprint)
         * @hide
         */
        @Readable
        public static final String FINGERPRINT_SIDE_FPS_KG_POWER_WINDOW =
                "fingerprint_side_fps_kg_power_window";

        /**
         * The time (in millis) to wait for a power button before sending
         * a successful auth in biometric prompt(for side fingerprint)
         * @hide
         */
        @Readable
        public static final String FINGERPRINT_SIDE_FPS_BP_POWER_WINDOW =
                "fingerprint_side_fps_bp_power_window";

        /**
         * The time (in millis) that a finger tap will wait for a power button
         * before dismissing the power dialog during enrollment(for side
         * fingerprint)
         * @hide
         */
        @Readable
        public static final String FINGERPRINT_SIDE_FPS_ENROLL_TAP_WINDOW =
                "fingerprint_side_fps_enroll_tap_window";

        /**
         * The time (in millis) that a power event will ignore future authentications
         * (for side fingerprint)
         * @hide
         */
        @Readable
        public static final String FINGERPRINT_SIDE_FPS_AUTH_DOWNTIME =
                "fingerprint_side_fps_auth_downtime";

        /**
         * Whether or not a SFPS device is enabling the performant auth setting.
         * The "_V2" suffix was added to re-introduce the default behavior for
         * users. See b/265264294 fore more details.
         * @hide
         */
        public static final String SFPS_PERFORMANT_AUTH_ENABLED = "sfps_performant_auth_enabled_v2";

        /**
         * Whether or not debugging is enabled.
         * @hide
         */
        @Readable
        public static final String BIOMETRIC_DEBUG_ENABLED =
                "biometric_debug_enabled";

        /**
         * Whether or not virtual sensors are enabled.
         * @hide
         */
        @TestApi
        @Readable
        public static final String BIOMETRIC_VIRTUAL_ENABLED = "biometric_virtual_enabled";

        /**
         * Whether or not biometric is allowed on Keyguard.
         * @hide
         */
        @Readable
        public static final String BIOMETRIC_KEYGUARD_ENABLED = "biometric_keyguard_enabled";

        /**
         * Whether or not biometric is allowed for apps (through BiometricPrompt).
         * @hide
         */
        @Readable
        public static final String BIOMETRIC_APP_ENABLED = "biometric_app_enabled";

        /**
         * Whether or not active unlock triggers on wake.
         * @hide
         */
        public static final String ACTIVE_UNLOCK_ON_WAKE = "active_unlock_on_wake";

        /**
         * Whether or not active unlock triggers on unlock intent.
         * @hide
         */
        public static final String ACTIVE_UNLOCK_ON_UNLOCK_INTENT =
                "active_unlock_on_unlock_intent";

        /**
         * Whether or not active unlock triggers on biometric failure.
         * @hide
         */
        public static final String ACTIVE_UNLOCK_ON_BIOMETRIC_FAIL =
                "active_unlock_on_biometric_fail";

        /**
         * If active unlock triggers on biometric failures, include the following error codes
         * as a biometric failure. See {@link android.hardware.biometrics.BiometricFaceConstants}.
         * Error codes should be separated by a pipe. For example: "1|4|5". If active unlock
         * should never trigger on any face errors, this should be set to an empty string.
         * A null value will use the system default value (TIMEOUT).
         * @hide
         */
        public static final String ACTIVE_UNLOCK_ON_FACE_ERRORS =
                "active_unlock_on_face_errors";

        /**
         * If active unlock triggers on biometric failures, include the following acquired info
         * as a "biometric failure". See {@link android.hardware.biometrics.BiometricFaceConstants}.
         * Acquired codes should be separated by a pipe. For example: "1|4|5". If active unlock
         * should never on trigger on any acquired info messages, this should be
         * set to an empty string. A null value will use the system default value (none).
         * @hide
         */
        public static final String ACTIVE_UNLOCK_ON_FACE_ACQUIRE_INFO =
                "active_unlock_on_face_acquire_info";

        /**
         * If active unlock triggers on biometric failures, then also request active unlock on
         * unlock intent when each setting (BiometricType) is the only biometric type enrolled.
         * Biometric types should be separated by a pipe. For example: "0|3" or "0". If this
         * setting should be disabled, then this should be set to an empty string. A null value
         * will use the system default value (0 / None).
         *   0 = None, 1 = Any face, 2 = Any fingerprint, 3 = Under display fingerprint
         * @hide
         */
        public static final String ACTIVE_UNLOCK_ON_UNLOCK_INTENT_WHEN_BIOMETRIC_ENROLLED =
                "active_unlock_on_unlock_intent_when_biometric_enrolled";

        /**
         * If active unlock triggers on unlock intents, then also request active unlock on
         * these wake-up reasons. See {@link PowerManager.WakeReason} for value mappings.
         * WakeReasons should be separated by a pipe. For example: "0|3" or "0". If this
         * setting should be disabled, then this should be set to an empty string. A null value
         * will use the system default value (WAKE_REASON_UNFOLD_DEVICE).
         * @hide
         */
        public static final String ACTIVE_UNLOCK_WAKEUPS_CONSIDERED_UNLOCK_INTENTS =
                "active_unlock_wakeups_considered_unlock_intents";

        /**
         * If active unlock triggers and succeeds on these wakeups, force dismiss keyguard on
         * these wake reasons. See {@link PowerManager#WakeReason} for value mappings.
         * WakeReasons should be separated by a pipe. For example: "0|3" or "0". If this
         * setting should be disabled, then this should be set to an empty string. A null value
         * will use the system default value (WAKE_REASON_UNFOLD_DEVICE).
         * @hide
         */
        public static final String ACTIVE_UNLOCK_WAKEUPS_TO_FORCE_DISMISS_KEYGUARD =
                "active_unlock_wakeups_to_force_dismiss_keyguard";

        /**
         * Whether the assist gesture should be enabled.
         *
         * @hide
         */
        @Readable
        public static final String ASSIST_GESTURE_ENABLED = "assist_gesture_enabled";

        /**
         * Sensitivity control for the assist gesture.
         *
         * @hide
         */
        @Readable
        public static final String ASSIST_GESTURE_SENSITIVITY = "assist_gesture_sensitivity";

        /**
         * Whether the assist gesture should silence alerts.
         *
         * @hide
         */
        @Readable
        public static final String ASSIST_GESTURE_SILENCE_ALERTS_ENABLED =
                "assist_gesture_silence_alerts_enabled";

        /**
         * Whether the assist gesture should wake the phone.
         *
         * @hide
         */
        @Readable
        public static final String ASSIST_GESTURE_WAKE_ENABLED =
                "assist_gesture_wake_enabled";

        /**
         * Indicates whether the Assist Gesture Deferred Setup has been completed.
         * <p>
         * Type: int (0 for false, 1 for true)
         *
         * @hide
         */
        @SystemApi
        @Readable
        public static final String ASSIST_GESTURE_SETUP_COMPLETE = "assist_gesture_setup_complete";

        /**
         * Whether the assistant can be triggered by a touch gesture.
         *
         * @hide
         */
        public static final String ASSIST_TOUCH_GESTURE_ENABLED =
                "assist_touch_gesture_enabled";

        /**
         * Whether the assistant can be triggered by long-pressing the home button
         *
         * @hide
         */
        public static final String ASSIST_LONG_PRESS_HOME_ENABLED =
                "assist_long_press_home_enabled";

        /**
         * Whether press and hold on nav handle can trigger search.
         *
         * @hide
         */
        public static final String SEARCH_PRESS_HOLD_NAV_HANDLE_ENABLED =
                "search_press_hold_nav_handle_enabled";

        /**
         * Whether long-pressing on the home button can trigger search.
         *
         * @hide
         */
        public static final String SEARCH_LONG_PRESS_HOME_ENABLED =
                "search_long_press_home_enabled";


        /**
         * Whether or not the accessibility data streaming is enbled for the
         * {@link VisualQueryDetectedResult#setAccessibilityDetectionData}.
         * @hide
         */
        public static final String VISUAL_QUERY_ACCESSIBILITY_DETECTION_ENABLED =
                "visual_query_accessibility_detection_enabled";

        /**
         * Control whether Night display is currently activated.
         * @hide
         */
        @Readable
        public static final String NIGHT_DISPLAY_ACTIVATED = "night_display_activated";

        /**
         * Control whether Night display will automatically activate/deactivate.
         * @hide
         */
        @Readable
        public static final String NIGHT_DISPLAY_AUTO_MODE = "night_display_auto_mode";

        /**
         * Control the color temperature of Night Display, represented in Kelvin.
         * @hide
         */
        @Readable
        public static final String NIGHT_DISPLAY_COLOR_TEMPERATURE =
                "night_display_color_temperature";

        /**
         * Custom time when Night display is scheduled to activate.
         * Represented as milliseconds from midnight (e.g. 79200000 == 10pm).
         * @hide
         */
        @Readable
        public static final String NIGHT_DISPLAY_CUSTOM_START_TIME =
                "night_display_custom_start_time";

        /**
         * Custom time when Night display is scheduled to deactivate.
         * Represented as milliseconds from midnight (e.g. 21600000 == 6am).
         * @hide
         */
        @Readable
        public static final String NIGHT_DISPLAY_CUSTOM_END_TIME = "night_display_custom_end_time";

        /**
         * A String representing the LocalDateTime when Night display was last activated. Use to
         * decide whether to apply the current activated state after a reboot or user change. In
         * legacy cases, this is represented by the time in milliseconds (since epoch).
         * @hide
         */
        @Readable
        public static final String NIGHT_DISPLAY_LAST_ACTIVATED_TIME =
                "night_display_last_activated_time";

        /**
         * Display color balance for the red channel, from 0 to 255.
         * @hide
         */
        public static final String DISPLAY_COLOR_BALANCE_RED = "display_color_balance_red";

        /**
         * Display color balance for the green channel, from 0 to 255.
         * @hide
         */
        public static final String DISPLAY_COLOR_BALANCE_GREEN = "display_color_balance_green";

        /**
         * Display color balance for the blue channel, from 0 to 255.
         * @hide
         */
        public static final String DISPLAY_COLOR_BALANCE_BLUE = "display_color_balance_blue";

        /**
         * Control whether display white balance is currently enabled.
         * @hide
         */
        @Readable
        public static final String DISPLAY_WHITE_BALANCE_ENABLED = "display_white_balance_enabled";

        /**
         * Names of the service components that the current user has explicitly allowed to
         * be a VR mode listener, separated by ':'.
         *
         * @hide
         */
        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        @TestApi
        @Readable
        public static final String ENABLED_VR_LISTENERS = "enabled_vr_listeners";

        /**
         * Behavior of the display while in VR mode.
         *
         * One of {@link #VR_DISPLAY_MODE_LOW_PERSISTENCE} or {@link #VR_DISPLAY_MODE_OFF}.
         *
         * @hide
         */
        @Readable
        public static final String VR_DISPLAY_MODE = "vr_display_mode";

        /**
         * Lower the display persistence while the system is in VR mode.
         *
         * @see PackageManager#FEATURE_VR_MODE_HIGH_PERFORMANCE
         *
         * @hide.
         */
        public static final int VR_DISPLAY_MODE_LOW_PERSISTENCE = 0;

        /**
         * Do not alter the display persistence while the system is in VR mode.
         *
         * @see PackageManager#FEATURE_VR_MODE_HIGH_PERFORMANCE
         *
         * @hide.
         */
        public static final int VR_DISPLAY_MODE_OFF = 1;

        /**
         * The latest SDK version that CarrierAppUtils#disableCarrierAppsUntilPrivileged has been
         * executed for.
         *
         * <p>This is used to ensure that we only take one pass which will disable apps that are not
         * privileged (if any). From then on, we only want to enable apps (when a matching SIM is
         * inserted), to avoid disabling an app that the user might actively be using.
         *
         * <p>Will be set to {@link android.os.Build.VERSION#SDK_INT} once executed. Note that older
         * SDK versions prior to R set 1 for this value.
         *
         * @hide
         */
        @Readable
        public static final String CARRIER_APPS_HANDLED = "carrier_apps_handled";

        /**
         * Whether parent user can access remote contact in managed profile.
         *
         * @hide
         */
        @Readable
        public static final String MANAGED_PROFILE_CONTACT_REMOTE_SEARCH =
                "managed_profile_contact_remote_search";

        /**
         * Whether parent profile can access remote calendar data in managed profile.
         *
         * @hide
         */
        @Readable
        public static final String CROSS_PROFILE_CALENDAR_ENABLED =
                "cross_profile_calendar_enabled";

        /**
         * Whether or not the automatic storage manager is enabled and should run on the device.
         *
         * @hide
         */
        @Readable
        public static final String AUTOMATIC_STORAGE_MANAGER_ENABLED =
                "automatic_storage_manager_enabled";

        /**
         * How many days of information for the automatic storage manager to retain on the device.
         *
         * @hide
         */
        @Readable
        public static final String AUTOMATIC_STORAGE_MANAGER_DAYS_TO_RETAIN =
                "automatic_storage_manager_days_to_retain";

        /**
         * Default number of days of information for the automatic storage manager to retain.
         *
         * @hide
         */
        public static final int AUTOMATIC_STORAGE_MANAGER_DAYS_TO_RETAIN_DEFAULT = 90;

        /**
         * How many bytes the automatic storage manager has cleared out.
         *
         * @hide
         */
        @Readable
        public static final String AUTOMATIC_STORAGE_MANAGER_BYTES_CLEARED =
                "automatic_storage_manager_bytes_cleared";

        /**
         * Last run time for the automatic storage manager.
         *
         * @hide
         */
        @Readable
        public static final String AUTOMATIC_STORAGE_MANAGER_LAST_RUN =
                "automatic_storage_manager_last_run";
        /**
         * If the automatic storage manager has been disabled by policy. Note that this doesn't
         * mean that the automatic storage manager is prevented from being re-enabled -- this only
         * means that it was turned off by policy at least once.
         *
         * @hide
         */
        @Readable
        public static final String AUTOMATIC_STORAGE_MANAGER_TURNED_OFF_BY_POLICY =
                "automatic_storage_manager_turned_off_by_policy";

        /**
         * Whether SystemUI navigation keys is enabled.
         * @hide
         */
        @Readable
        public static final String SYSTEM_NAVIGATION_KEYS_ENABLED =
                "system_navigation_keys_enabled";

        /**
         * Holds comma separated list of ordering of QS tiles.
         *
         * @hide
         */
        @Readable(maxTargetSdk = VERSION_CODES.TIRAMISU)
        public static final String QS_TILES = "sysui_qs_tiles";

        /**
         * Whether this user has enabled Quick controls.
         *
         * 0 indicates disabled and 1 indicates enabled. A non existent value should be treated as
         * enabled.
         *
         * @deprecated Controls are migrated to Quick Settings, rendering this unnecessary and will
         *             be removed in a future release.
         * @hide
         */
        @Readable
        @Deprecated
        public static final String CONTROLS_ENABLED = "controls_enabled";

        /**
         * Whether power menu content (cards, passes, controls) will be shown when device is locked.
         *
         * 0 indicates hide and 1 indicates show. A non existent value will be treated as hide.
         * @hide
         */
        @TestApi
        @Readable
        public static final String POWER_MENU_LOCKED_SHOW_CONTENT =
                "power_menu_locked_show_content";

        /**
         * Whether home controls should be accessible from the lockscreen
         *
         * @hide
         */
        public static final String LOCKSCREEN_SHOW_CONTROLS = "lockscreen_show_controls";

        /**
         * Whether trivial home controls can be used without authentication
         *
         * @hide
         */
        public static final String LOCKSCREEN_ALLOW_TRIVIAL_CONTROLS =
                "lockscreen_allow_trivial_controls";

        /**
         * Whether wallet should be accessible from the lockscreen
         *
         * @hide
         */
        public static final String LOCKSCREEN_SHOW_WALLET = "lockscreen_show_wallet";

        /**
         * Whether to use the lockscreen double-line clock
         *
         * @hide
         */
        public static final String LOCKSCREEN_USE_DOUBLE_LINE_CLOCK =
                "lockscreen_use_double_line_clock";        
        
        /**
         * Whether to use the lockscreen custom clock
         *
         * @hide
         */
        public static final String CLOCK_LS =
                "clock_ls";

        /**
         * Whether to show the vibrate icon in the Status Bar (default off)
         *
         * @hide
         */
        public static final String STATUS_BAR_SHOW_VIBRATE_ICON = "status_bar_show_vibrate_icon";

        /**
         * Specifies whether the web action API is enabled.
         *
         * @hide
         */
        @SystemApi
        @Readable
        public static final String INSTANT_APPS_ENABLED = "instant_apps_enabled";

        /**
         * Whether qr code scanner should be accessible from the lockscreen
         *
         * @hide
         */
        public static final String LOCK_SCREEN_SHOW_QR_CODE_SCANNER =
                "lock_screen_show_qr_code_scanner";

        /**
         * Whether or not to enable qr code code scanner setting to enable/disable lockscreen
         * entry point. Any value apart from null means setting needs to be enabled
         *
         * @hide
         */
        public static final String SHOW_QR_CODE_SCANNER_SETTING =
                "show_qr_code_scanner_setting";

        /**
         * Has this pairable device been paired or upgraded from a previously paired system.
         * @hide
         */
        @Readable
        public static final String DEVICE_PAIRED = "device_paired";

        /**
         * Specifies additional package name for broadcasting the CMAS messages.
         * @hide
         */
        @Readable
        public static final String CMAS_ADDITIONAL_BROADCAST_PKG = "cmas_additional_broadcast_pkg";

        /**
         * Whether the launcher should show any notification badges.
         * The value is boolean (1 or 0).
         * @hide
         */
        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        @TestApi
        @Readable
        public static final String NOTIFICATION_BADGING = "notification_badging";

        /**
         * When enabled the system will maintain a rolling history of received notifications. When
         * disabled the history will be disabled and deleted.
         *
         * The value 1 - enable, 0 - disable
         * @hide
         */
        @Readable
        public static final String NOTIFICATION_HISTORY_ENABLED = "notification_history_enabled";

        /**
         * When enabled conversations marked as favorites will be set to bubble.
         *
         * The value 1 - enable, 0 - disable
         * @hide
         */
        @Readable
        public static final String BUBBLE_IMPORTANT_CONVERSATIONS
                = "bubble_important_conversations";

        /**
         * When enabled, notifications able to bubble will display an affordance allowing the user
         * to bubble them.
         * The value is boolean (1 to enable or 0 to disable).
         *
         * @hide
         */
        @TestApi
        @SuppressLint("NoSettingsProvider")
        @Readable
        public static final String NOTIFICATION_BUBBLES = "notification_bubbles";

        /**
         * Whether notifications are dismissed by a right-to-left swipe (instead of a left-to-right
         * swipe).
         *
         * @hide
         */
        @Readable
        public static final String NOTIFICATION_DISMISS_RTL = "notification_dismiss_rtl";

        /**
         * Comma separated list of QS tiles that have been auto-added already.
         * @hide
         */
        @Readable
        public static final String QS_AUTO_ADDED_TILES = "qs_auto_tiles";

        /**
         * The duration of timeout, in milliseconds, to switch from a non-Dock User to the
         * Dock User when the device is docked.
         * @hide
         */
        public static final String TIMEOUT_TO_DOCK_USER = "timeout_to_dock_user";

        /**
         * Backup manager behavioral parameters.
         * This is encoded as a key=value list, separated by commas. Ex:
         *
         * "key_value_backup_interval_milliseconds=14400000,key_value_backup_require_charging=true"
         *
         * The following keys are supported:
         *
         * <pre>
         * key_value_backup_interval_milliseconds  (long)
         * key_value_backup_fuzz_milliseconds      (long)
         * key_value_backup_require_charging       (boolean)
         * key_value_backup_required_network_type  (int)
         * full_backup_interval_milliseconds       (long)
         * full_backup_require_charging            (boolean)
         * full_backup_required_network_type       (int)
         * backup_finished_notification_receivers  (String[])
         * </pre>
         *
         * backup_finished_notification_receivers uses ":" as delimeter for values.
         *
         * <p>
         * Type: string
         * @hide
         */
        @Readable
        public static final String BACKUP_MANAGER_CONSTANTS = "backup_manager_constants";


        /**
         * Local transport parameters so we can configure it for tests.
         * This is encoded as a key=value list, separated by commas.
         *
         * The following keys are supported:
         *
         * <pre>
         * fake_encryption_flag  (boolean)
         * </pre>
         *
         * <p>
         * Type: string
         * @hide
         */
        @Readable
        public static final String BACKUP_LOCAL_TRANSPORT_PARAMETERS =
                "backup_local_transport_parameters";

        /**
         * Flag to set if the system should predictively attempt to re-enable Bluetooth while
         * the user is driving.
         * @hide
         */
        @Readable
        public static final String BLUETOOTH_ON_WHILE_DRIVING = "bluetooth_on_while_driving";

        /**
         * Volume dialog timeout in ms.
         * @hide
         */
        public static final String VOLUME_DIALOG_DISMISS_TIMEOUT = "volume_dialog_dismiss_timeout";

        /**
         * What behavior should be invoked when the volume hush gesture is triggered
         * One of VOLUME_HUSH_OFF, VOLUME_HUSH_VIBRATE, VOLUME_HUSH_MUTE.
         *
         * @hide
         */
        @SystemApi
        @Readable
        public static final String VOLUME_HUSH_GESTURE = "volume_hush_gesture";

        /** @hide */
        @SystemApi
        public static final int VOLUME_HUSH_OFF = 0;
        /** @hide */
        @SystemApi
        public static final int VOLUME_HUSH_VIBRATE = 1;
        /** @hide */
        @SystemApi
        public static final int VOLUME_HUSH_MUTE = 2;

        /**
         * The number of times (integer) the user has manually enabled battery saver.
         * @hide
         */
        @Readable
        public static final String LOW_POWER_MANUAL_ACTIVATION_COUNT =
                "low_power_manual_activation_count";

        /**
         * Whether the "first time battery saver warning" dialog needs to be shown (0: default)
         * or not (1).
         *
         * @hide
         */
        @Readable
        public static final String LOW_POWER_WARNING_ACKNOWLEDGED =
                "low_power_warning_acknowledged";

        /**
         * Whether the "first time extra battery saver warning" dialog needs to be shown
         * (0: default) or not (1).
         *
         * @hide
         */
        public static final String EXTRA_LOW_POWER_WARNING_ACKNOWLEDGED =
                "extra_low_power_warning_acknowledged";

        /**
         * 0 (default) Auto battery saver suggestion has not been suppressed. 1) it has been
         * suppressed.
         * @hide
         */
        @Readable
        public static final String SUPPRESS_AUTO_BATTERY_SAVER_SUGGESTION =
                "suppress_auto_battery_saver_suggestion";

        /**
         * List of packages, which data need to be unconditionally cleared before full restore.
         * Type: string
         * @hide
         */
        @Readable
        public static final String PACKAGES_TO_CLEAR_DATA_BEFORE_FULL_RESTORE =
                "packages_to_clear_data_before_full_restore";

        /**
         * How often to check for location access.
         * @hide
         *
         * @deprecated This has been moved to DeviceConfig property
         * {@link LocationAccessCheck#PROPERTY_LOCATION_ACCESS_PERIODIC_INTERVAL_MILLIS} in a T
         * module update
         *
         * Before Android T set this property to control the interval for the check
         * On Android T set this and the DeviceConfig property
         * After Android T set the DeviceConfig property
         */
        @SystemApi
        @Deprecated
        @Readable
        public static final String LOCATION_ACCESS_CHECK_INTERVAL_MILLIS =
                "location_access_check_interval_millis";

        /**
         * Delay between granting location access and checking it.
         * @hide
         *
         * @deprecated This has been moved to DeviceConfig property
         * {@link LocationAccessCheck#PROPERTY_LOCATION_ACCESS_CHECK_DELAY_MILLIS} in a T module
         * update
         *
         * Before Android T set this property to control the delay for the check
         * On Android T set this and the DeviceConfig property
         * After Android T set the DeviceConfig property
         */
        @SystemApi
        @Deprecated
        @Readable
        public static final String LOCATION_ACCESS_CHECK_DELAY_MILLIS =
                "location_access_check_delay_millis";

        /**
         * @deprecated This setting does not have any effect anymore
         * @hide
         */
        @SystemApi
        @Deprecated
        @Readable
        public static final String LOCATION_PERMISSIONS_UPGRADE_TO_Q_MODE =
                "location_permissions_upgrade_to_q_mode";

        /**
         * Whether or not the system Auto Revoke feature is disabled.
         * @hide
         */
        @SystemApi
        @Readable
        public static final String AUTO_REVOKE_DISABLED = "auto_revoke_disabled";

        /**
         * Map of android.theme.customization.* categories to the enabled overlay package for that
         * category, formatted as a serialized {@link org.json.JSONObject}. If there is no
         * corresponding package included for a category, then all overlay packages in that
         * category must be disabled.
         *
         * A few category keys have special meaning and are used for Material You theming.
         *
         * A {@code FabricatedOverlay} containing Material You tonal palettes will be generated
         * in case {@code android.theme.customization.system_palette} contains a
         * {@link android.annotation.ColorInt}.
         *
         * The strategy used for generating the tonal palettes can be defined with the
         * {@code android.theme.customization.theme_style} key, with one of the following options:
         * <ul>
         *   <li> {@code TONAL_SPOT} is a mid vibrancy palette that uses an accent 3 analogous to
         *   accent 1.</li>
         *   <li> {@code VIBRANT} is a high vibrancy palette that harmoniously blends subtle shifts
         *   between colors.</li>
         *   <li> {@code EXPRESSIVE} is a high vibrancy palette that pairs unexpected and unique
         *   accents colors together.</li>
         *   <li> {@code SPRITZ} is a low vibrancy palette that creates a soft wash between
         *   colors.</li>
         *   <li> {@code RAINBOW} uses both chromatic accents and neutral surfaces to create a more
         *   subtle color experience for users.</li>
         *   <li> {@code FRUIT_SALAD} experiments with the concept of "two tone colors" to give
         *   users more expression.</li>
         * </ul>
         *
         * Example of valid fabricated theme specification:
         * <pre>
         * {
         *     "android.theme.customization.system_palette":"B1611C",
         *     "android.theme.customization.theme_style":"EXPRESSIVE"
         * }
         * </pre>
         * @hide
         */
        @SystemApi
        @Readable
        public static final String THEME_CUSTOMIZATION_OVERLAY_PACKAGES =
                "theme_customization_overlay_packages";

        /**
         * Indicates whether the nav bar is forced to always be visible, even in immersive mode.
         * <p>Type: int (0 for false, 1 for true)
         *
         * @hide
         */
        public static final String NAV_BAR_FORCE_VISIBLE = "nav_bar_force_visible";

        /**
         * Indicates whether the device is in kids nav mode.
         * <p>Type: int (0 for false, 1 for true)
         *
         * @hide
         */
        public static final String NAV_BAR_KIDS_MODE = "nav_bar_kids_mode";

        /**
         * Navigation bar mode.
         *  0 = 3 button
         *  1 = 2 button
         *  2 = fully gestural
         * @hide
         */
        @Readable
        public static final String NAVIGATION_MODE =
                "navigation_mode";

        /**
         * The value is from another(source) device's {@link #NAVIGATION_MODE} during restore.
         * It's supposed to be written only by
         * {@link com.android.providers.settings.SettingsHelper}.
         * This setting should not be added into backup array.
         * <p>Value: -1 = Can't get value from restore(default),
         *  0 = 3 button,
         *  1 = 2 button,
         *  2 = fully gestural.
         * @hide
         */
        public static final String NAVIGATION_MODE_RESTORE = "navigation_mode_restore";

        /**
         * Scale factor for the back gesture inset size on the left side of the screen.
         * @hide
         */
        @Readable
        public static final String BACK_GESTURE_INSET_SCALE_LEFT =
                "back_gesture_inset_scale_left";

        /**
         * Scale factor for the back gesture inset size on the right side of the screen.
         * @hide
         */
        @Readable
        public static final String BACK_GESTURE_INSET_SCALE_RIGHT =
                "back_gesture_inset_scale_right";

        /**
         * Indicates whether the trackpad back gesture is enabled.
         * <p>Type: int (0 for false, 1 for true)
         *
         * @hide
         */
        public static final String TRACKPAD_GESTURE_BACK_ENABLED = "trackpad_gesture_back_enabled";

        /**
         * Indicates whether the trackpad home gesture is enabled.
         * <p>Type: int (0 for false, 1 for true)
         *
         * @hide
         */
        public static final String TRACKPAD_GESTURE_HOME_ENABLED = "trackpad_gesture_home_enabled";

        /**
         * Indicates whether the trackpad overview gesture is enabled.
         * <p>Type: int (0 for false, 1 for true)
         *
         * @hide
         */
        public static final String TRACKPAD_GESTURE_OVERVIEW_ENABLED =
                "trackpad_gesture_overview_enabled";

        /**
         * Indicates whether the trackpad notification gesture is enabled.
         * <p>Type: int (0 for false, 1 for true)
         *
         * @hide
         */
        public static final String TRACKPAD_GESTURE_NOTIFICATION_ENABLED =
                "trackpad_gesture_notification_enabled";

        /**
         * Indicates whether the trackpad quick switch gesture is enabled.
         * <p>Type: int (0 for false, 1 for true)
         *
         * @hide
         */
        public static final String TRACKPAD_GESTURE_QUICK_SWITCH_ENABLED =
                "trackpad_gesture_quick_switch_enabled";

        /**
         * Current provider of proximity-based sharing services.
         * Default value in @string/config_defaultNearbySharingComponent.
         * No VALIDATOR as this setting will not be backed up.
         * @hide
         */
        @Readable
        public static final String NEARBY_SHARING_COMPONENT = "nearby_sharing_component";

        /**
         * Nearby Sharing Slice URI for the SliceProvider to
         * read Nearby Sharing scan results and then draw the UI.
         * @hide
         */
        public static final String NEARBY_SHARING_SLICE_URI = "nearby_sharing_slice_uri";

        /**
         * Current provider of Fast Pair saved devices page.
         * Default value in @string/config_defaultNearbyFastPairSettingsDevicesComponent.
         * No VALIDATOR as this setting will not be backed up.
         * @hide
         */
        public static final String NEARBY_FAST_PAIR_SETTINGS_DEVICES_COMPONENT =
                "nearby_fast_pair_settings_devices_component";

        /**
         * Current provider of the component for requesting ambient context consent.
         * Default value in @string/config_defaultAmbientContextConsentComponent.
         * No VALIDATOR as this setting will not be backed up.
         * @hide
         */
        public static final String AMBIENT_CONTEXT_CONSENT_COMPONENT =
                "ambient_context_consent_component";

        /**
         * Current provider of the intent extra key for the caller's package name while
         * requesting ambient context consent.
         * No VALIDATOR as this setting will not be backed up.
         * @hide
         */
        public static final String AMBIENT_CONTEXT_PACKAGE_NAME_EXTRA_KEY =
                "ambient_context_package_name_key";

        /**
         * Current provider of the intent extra key for the event code int array while
         * requesting ambient context consent.
         * Default value in @string/config_ambientContextEventArrayExtraKey.
         * No VALIDATOR as this setting will not be backed up.
         * @hide
         */
        public static final String AMBIENT_CONTEXT_EVENT_ARRAY_EXTRA_KEY =
                "ambient_context_event_array_key";

        /**
         * Controls whether aware is enabled.
         * @hide
         */
        @Readable
        public static final String AWARE_ENABLED = "aware_enabled";

        /**
         * Controls whether aware_lock is enabled.
         * @hide
         */
        @Readable
        public static final String AWARE_LOCK_ENABLED = "aware_lock_enabled";

        /**
         * Controls whether tap gesture is enabled.
         * @hide
         */
        @Readable
        public static final String TAP_GESTURE = "tap_gesture";

        /**
         * Controls whether the people strip is enabled.
         * @hide
         */
        @Readable
        public static final String PEOPLE_STRIP = "people_strip";

        /**
         * Whether or not to enable media resumption
         * When enabled, media controls in quick settings will populate on boot and persist if
         * resumable via a MediaBrowserService.
         * @see Settings.Global#SHOW_MEDIA_ON_QUICK_SETTINGS
         * @hide
         */
        @Readable
        public static final String MEDIA_CONTROLS_RESUME = "qs_media_resumption";

        /**
         * Whether to enable media controls on lock screen.
         * When enabled, media controls will appear on lock screen.
         * @hide
         */
        public static final String MEDIA_CONTROLS_LOCK_SCREEN = "media_controls_lock_screen";

        /**
         * Whether to enable camera extensions software fallback.
         * @hide
         */
        @Readable
        public static final String CAMERA_EXTENSIONS_FALLBACK = "camera_extensions_fallback";

        /**
         * Controls whether contextual suggestions can be shown in the media controls.
         * @hide
         */
        public static final String MEDIA_CONTROLS_RECOMMENDATION = "qs_media_recommend";

        /**
         * Controls magnification mode when magnification is enabled via a system-wide triple tap
         * gesture or the accessibility shortcut.
         *
         * @see #ACCESSIBILITY_MAGNIFICATION_MODE_FULLSCREEN
         * @see #ACCESSIBILITY_MAGNIFICATION_MODE_WINDOW
         * @hide
         */
        @TestApi
        @Readable
        public static final String ACCESSIBILITY_MAGNIFICATION_MODE =
                "accessibility_magnification_mode";

        /**
         * Magnification mode value that is a default value for the magnification logging feature.
         * @hide
         */
        public static final int ACCESSIBILITY_MAGNIFICATION_MODE_NONE = 0x0;

        /**
         * Magnification mode value that magnifies whole display.
         * @hide
         */
        @TestApi
        public static final int ACCESSIBILITY_MAGNIFICATION_MODE_FULLSCREEN = 0x1;

        /**
         * Magnification mode value that magnifies magnify particular region in a window
         * @hide
         */
        @TestApi
        public static final int ACCESSIBILITY_MAGNIFICATION_MODE_WINDOW = 0x2;

        /**
         * Magnification mode value that is capable of magnifying whole display and particular
         * region in a window.
         * @hide
         */
        @TestApi
        public static final int ACCESSIBILITY_MAGNIFICATION_MODE_ALL = 0x3;

        /**
         * Whether the magnification always on feature is enabled. If true, the magnifier will not
         * deactivate on Activity transitions; it will only zoom out to 100%.
         *
         * @hide
         */
        public static final String ACCESSIBILITY_MAGNIFICATION_ALWAYS_ON_ENABLED =
                "accessibility_magnification_always_on_enabled";

        /**
         * Whether the following typing focus feature for magnification is enabled.
         * @hide
         */
        public static final String ACCESSIBILITY_MAGNIFICATION_FOLLOW_TYPING_ENABLED =
                "accessibility_magnification_follow_typing_enabled";

        /**
         * Whether the magnification joystick controller feature is enabled.
         * @hide
         */
        public static final String ACCESSIBILITY_MAGNIFICATION_JOYSTICK_ENABLED =
                "accessibility_magnification_joystick_enabled";

        /**
         * Setting that specifies whether the display magnification is enabled via a system-wide
         * two fingers triple tap gesture.
         *
         * @hide
         */
        public static final String ACCESSIBILITY_MAGNIFICATION_TWO_FINGER_TRIPLE_TAP_ENABLED =
                "accessibility_magnification_two_finger_triple_tap_enabled";

        /**
         * For pinch to zoom anywhere feature.
         *
         * If true, you should be able to pinch to magnify the window anywhere.
         *
         * @hide
         */
        public static final String ACCESSIBILITY_PINCH_TO_ZOOM_ANYWHERE_ENABLED =
                "accessibility_pinch_to_zoom_anywhere_enabled";

        /**
         * For magnification feature where panning can be controlled with a single finger.
         *
         * If true, you can pan using a single finger gesture.
         *
         * @hide
         */
        public static final String ACCESSIBILITY_SINGLE_FINGER_PANNING_ENABLED =
                "accessibility_single_finger_panning_enabled";

        /**
         * Controls magnification capability. Accessibility magnification is capable of at least one
         * of the magnification modes.
         *
         * @see #ACCESSIBILITY_MAGNIFICATION_MODE_FULLSCREEN
         * @see #ACCESSIBILITY_MAGNIFICATION_MODE_WINDOW
         * @see #ACCESSIBILITY_MAGNIFICATION_MODE_ALL
         * @hide
         */
        @TestApi
        @Readable
        public static final String ACCESSIBILITY_MAGNIFICATION_CAPABILITY =
                "accessibility_magnification_capability";

        /**
         *  Whether to show the window magnification prompt dialog when the user uses full-screen
         *  magnification first time after database is upgraded.
         *
         * @hide
         */
        public static final String ACCESSIBILITY_SHOW_WINDOW_MAGNIFICATION_PROMPT =
                "accessibility_show_window_magnification_prompt";

        /**
         * Controls the accessibility button mode. System will force-set the value to {@link
         * #ACCESSIBILITY_BUTTON_MODE_GESTURE} if {@link #NAVIGATION_MODE} is button; force-set the
         * value to {@link ACCESSIBILITY_BUTTON_MODE_NAVIGATION_BAR} if {@link #NAVIGATION_MODE} is
         * gestural; otherwise, remain the option.
         * <ul>
         *    <li> 0 = button in navigation bar </li>
         *    <li> 1 = button floating on the display </li>
         *    <li> 2 = button using gesture to trigger </li>
         * </ul>
         *
         * @see #ACCESSIBILITY_BUTTON_MODE_NAVIGATION_BAR
         * @see #ACCESSIBILITY_BUTTON_MODE_FLOATING_MENU
         * @see #ACCESSIBILITY_BUTTON_MODE_GESTURE
         * @hide
         */
        public static final String ACCESSIBILITY_BUTTON_MODE =
                "accessibility_button_mode";

        /**
         * Accessibility button mode value that specifying the accessibility service or feature to
         * be toggled via the button in the navigation bar.
         *
         * @hide
         */
        public static final int ACCESSIBILITY_BUTTON_MODE_NAVIGATION_BAR = 0x0;

        /**
         * Accessibility button mode value that specifying the accessibility service or feature to
         * be toggled via the button floating on the display.
         *
         * @hide
         */
        public static final int ACCESSIBILITY_BUTTON_MODE_FLOATING_MENU = 0x1;

        /**
         * Accessibility button mode value that specifying the accessibility service or feature to
         * be toggled via the gesture.
         *
         * @hide
         */
        public static final int ACCESSIBILITY_BUTTON_MODE_GESTURE = 0x2;

        /**
         * The size of the accessibility floating menu.
         * <ul>
         *     <li> 0 = small size
         *     <li> 1 = large size
         * </ul>
         *
         * @hide
         */
        public static final String ACCESSIBILITY_FLOATING_MENU_SIZE =
                "accessibility_floating_menu_size";

        /**
         * The icon type of the accessibility floating menu.
         * <ul>
         *     <li> 0 = full circle type
         *     <li> 1 = half circle type
         * </ul>
         *
         * @hide
         */
        public static final String ACCESSIBILITY_FLOATING_MENU_ICON_TYPE =
                "accessibility_floating_menu_icon_type";

        /**
         * Whether the fade effect for the accessibility floating menu is enabled.
         *
         * @hide
         */
        public static final String ACCESSIBILITY_FLOATING_MENU_FADE_ENABLED =
                "accessibility_floating_menu_fade_enabled";

        /**
         * The opacity value for the accessibility floating menu fade out effect, from 0.0
         * (transparent) to 1.0 (opaque).
         *
         * @hide
         */
        public static final String ACCESSIBILITY_FLOATING_MENU_OPACITY =
                "accessibility_floating_menu_opacity";

        /**
         * Prompts the user to the Accessibility button is replaced with the floating menu.
         * <ul>
         *    <li> 0 = disabled </li>
         *    <li> 1 = enabled </li>
         * </ul>
         *
         * @hide
         */
        public static final String ACCESSIBILITY_FLOATING_MENU_MIGRATION_TOOLTIP_PROMPT =
                "accessibility_floating_menu_migration_tooltip_prompt";

        /**
         * For the force dark theme feature which inverts any apps that don't already support dark
         * theme.
         *
         * If true, it will automatically invert any app that is mainly light.
         *
         * This is related to the force dark override setting, however it will always force the apps
         * colors and will ignore any developer hints or opt-out APIs.
         *
         * @hide
         */
        @Readable
        public static final String ACCESSIBILITY_FORCE_INVERT_COLOR_ENABLED =
                "accessibility_force_invert_color_enabled";

        /**
         * Whether the Adaptive connectivity option is enabled.
         *
         * @hide
         */
        public static final String ADAPTIVE_CONNECTIVITY_ENABLED = "adaptive_connectivity_enabled";

        /**
         * Controls the 'Sunlight boost' toggle in wearable devices (high brightness mode).
         *
         * Valid values for this key are: '0' (disabled) or '1' (enabled).
         *
         * @hide
         */
        public static final String HBM_SETTING_KEY =
                "com.android.server.display.HBM_SETTING_KEY";

        /**
         * Whether to show privacy indicator for location
         * @hide
         */
        public static final String ENABLE_LOCATION_PRIVACY_INDICATOR = "enable_location_privacy_indicator";

        /**
         * Whether to show privacy indicator for camera
         * @hide
         */
        public static final String ENABLE_CAMERA_PRIVACY_INDICATOR = "enable_camera_privacy_indicator";

        /**
         * Whether to show privacy indicator for media projection
         * @hide
         */
        public static final String ENABLE_PROJECTION_PRIVACY_INDICATOR = "enable_projection_privacy_indicator";

        /**
         * Enable udfps detection even when screen is off
         * Default value is 0
         * @hide
         */
        public static final String SCREEN_OFF_UDFPS_ENABLED = "screen_off_udfps_enabled";

        /**
         * Our GameSpace can't write to device_config directly [GTS]
         * Use this as intermediate to pass device_config property
         * from our GameSpace to com.android.server.app.GameManagerService
         * so we can set the device_config property from there.
         * @hide
         */
        public static final String GAME_OVERLAY = "game_overlay";

        /**
         * Select from different navigation bar layouts
         * @hide
         */
        public static final String NAVBAR_LAYOUT_VIEWS = "navbar_layout_views";

        /**
         * Inverse navigation bar layout
         * @hide
         */
        public static final String NAVBAR_INVERSE_LAYOUT = "navbar_inverse_layout";

        /**
         * Whether to show or hide the arrow for back gesture
         * @hide
         */
        public static final String BACK_GESTURE_ARROW = "back_gesture_arrow";

        /**
         * Whether or not to vibrate when back gesture is used
         * @hide
         */
        public static final String BACK_GESTURE_HAPTIC_INTENSITY = "back_gesture_haptic_intensity";

        /**
         * Whether to show an overlay in the bottom corner of the screen on copying stuff
         * into the clipboard.
         * @hide
         */
        public static final String SHOW_CLIPBOARD_OVERLAY = "show_clipboard_overlay";

        /**
         * Control whether the process CPU info meter should be shown.
         * @hide
         */
        public static final String SHOW_CPU_OVERLAY = "show_cpu_overlay";

        /**
         * Control whether the process FPS info meter should be shown.
         * @hide
         */
        public static final String SHOW_FPS_OVERLAY = "show_fps_overlay";

        /**
         * Whether to pulse ambient on new music tracks
         * @hide
         */
        public static final String PULSE_ON_NEW_TRACKS = "pulse_on_new_tracks";

        /**
         * Whether to enable DOZE only when charging
         * @hide
         */
        public static final String DOZE_ON_CHARGE = "doze_on_charge";

        /**
         * Pulse navbar music visualizer
         * @hide
         */
        public static final String NAVBAR_PULSE_ENABLED = "navbar_pulse_enabled";

        /**
         * Pulse ambient music visualizer
         * @hide
         */
        public static final String AMBIENT_PULSE_ENABLED = "ambient_pulse_enabled";

        /**
         * Pulse lockscreen music visualizer
         * @hide
         */
        public static final String LOCKSCREEN_PULSE_ENABLED = "lockscreen_pulse_enabled";

        /**
         * Pulse navbar music visualizer color type
         * @hide
         */
        public static final String PULSE_COLOR_MODE = "pulse_color_mode";

        /**
         * Pulse music visualizer user defined color
         * @hide
         */
        public static final String PULSE_COLOR_USER = "pulse_color_user";

        /**
         * Pulse lavalamp animation speed
         * @hide
         */
        public static final String PULSE_LAVALAMP_SPEED = "pulse_lavalamp_speed";

        /**
         * Pulse renderer implementation
         * @hide
         */
        public static final String PULSE_RENDER_STYLE = "pulse_render_style";

        /**
         * Custom Pulse Widths
         * @hide
         */
        public static final String PULSE_CUSTOM_DIMEN = "pulse_custom_dimen";

        /**
         * Custom Spacing Between Pulse Bars
         * @hide
         */
        public static final String PULSE_CUSTOM_DIV = "pulse_custom_div";

        /**
         * Custom Pulse Block Size
         * @hide
         */
        public static final String PULSE_FILLED_BLOCK_SIZE = "pulse_filled_block_size";

        /**
         * Custom Spacing Between Pulse Blocks
         * @hide
         */
        public static final String PULSE_EMPTY_BLOCK_SIZE = "pulse_empty_block_size";

        /**
         * Custom Pulse Sanity Levels
         * @hide
         */
        public static final String PULSE_CUSTOM_FUDGE_FACTOR = "pulse_custom_fudge_factor";

        /**
         * Pulse Fudge Factor
         * @hide
         */
        public static final String PULSE_SOLID_FUDGE_FACTOR = "pulse_solid_fudge_factor";

        /**
         * Pulse Solid units count
         * @hide
         */
        public static final String PULSE_SOLID_UNITS_COUNT = "pulse_solid_units_count";

        /**
         * Pulse Solid units opacity
         * @hide
         */
        public static final String PULSE_SOLID_UNITS_OPACITY = "pulse_solid_units_opacity";

        /**
         * Pulse Solid units rounded
         * @hide
         */
        public static final String PULSE_SOLID_UNITS_ROUNDED = "pulse_solid_units_rounded";

        /**
         * Pulse uses FFT averaging
         * @hide
         */
        public static final String PULSE_SMOOTHING_ENABLED = "pulse_smoothing_enabled";

	/**
         * Pulse gravity
         * @hide
         */
        public static final String PULSE_CUSTOM_GRAVITY = "pulse_custom_gravity";

        /**
         * Enable and disable QS Panel visualizer
         * @hide
         */
        public static final String VISUALIZER_CENTER_MIRRORED = "visualizer_center_mirrored";
        
        /**
         * Pulse vertical mirror
         * @hide
         */
        public static final String PULSE_VERTICAL_MIRROR = "pulse_vertical_mirror";    

        /**
         * Volume styles
         * @hide
         */
        public static final String CUSTOM_VOLUME_STYLES = "custom_volume_styles";

        /**
         * Keys we no longer back up under the current schema, but want to continue to
         * process when restoring historical backup datasets.
         *
         * All settings in {@link LEGACY_RESTORE_SETTINGS} array *must* have a non-null validator,
         * otherwise they won't be restored.
         *
         * @hide
         */
        @Readable
        public static final String[] LEGACY_RESTORE_SETTINGS = {
                ENABLED_NOTIFICATION_LISTENERS,
                ENABLED_NOTIFICATION_ASSISTANT,
                ENABLED_NOTIFICATION_POLICY_ACCESS_PACKAGES
        };

        /**
         * How long Assistant handles have enabled in milliseconds.
         *
         * @hide
         */
        public static final String ASSIST_HANDLES_LEARNING_TIME_ELAPSED_MILLIS =
                "reminder_exp_learning_time_elapsed";

        /**
         * How many times the Assistant has been triggered using the touch gesture.
         *
         * @hide
         */
        public static final String ASSIST_HANDLES_LEARNING_EVENT_COUNT =
                "reminder_exp_learning_event_count";

        /**
         * Whether to show clipboard access notifications.
         *
         * @hide
         */
        public static final String CLIPBOARD_SHOW_ACCESS_NOTIFICATIONS =
                "clipboard_show_access_notifications";

        /**
         * If nonzero, nas has not been updated to reflect new changes.
         * @hide
         */
        @Readable
        public static final String NAS_SETTINGS_UPDATED = "nas_settings_updated";

        /**
         * Control whether Game Dashboard shortcut is always on for all games.
         * @hide
         */
        @Readable
        public static final String GAME_DASHBOARD_ALWAYS_ON = "game_dashboard_always_on";


        /**
         * For this device state, no specific auto-rotation lock setting should be applied.
         * If the user toggles the auto-rotate lock in this state, the setting will apply to the
         * previously valid device state.
         * @hide
         */
        public static final int DEVICE_STATE_ROTATION_LOCK_IGNORED = 0;
        /**
         * For this device state, the setting for auto-rotation is locked.
         * @hide
         */
        public static final int DEVICE_STATE_ROTATION_LOCK_LOCKED = 1;
        /**
         * For this device state, the setting for auto-rotation is unlocked.
         * @hide
         */
        public static final int DEVICE_STATE_ROTATION_LOCK_UNLOCKED = 2;

        /**
         * The different settings that can be used as values with
         * {@link #DEVICE_STATE_ROTATION_LOCK}.
         * @hide
         */
        @IntDef(prefix = {"DEVICE_STATE_ROTATION_LOCK_"}, value = {
                DEVICE_STATE_ROTATION_LOCK_IGNORED,
                DEVICE_STATE_ROTATION_LOCK_LOCKED,
                DEVICE_STATE_ROTATION_LOCK_UNLOCKED,
        })
        @Retention(RetentionPolicy.SOURCE)
        public @interface DeviceStateRotationLockSetting {
        }

        /** @hide */
        public static final int DEVICE_STATE_ROTATION_KEY_UNKNOWN = -1;
        /** @hide */
        public static final int DEVICE_STATE_ROTATION_KEY_FOLDED = 0;
        /** @hide */
        public static final int DEVICE_STATE_ROTATION_KEY_HALF_FOLDED = 1;
        /** @hide */
        public static final int DEVICE_STATE_ROTATION_KEY_UNFOLDED = 2;
        /** @hide */
        public static final int DEVICE_STATE_ROTATION_KEY_REAR_DISPLAY = 3;

        /**
         * The different postures that can be used as keys with
         * {@link #DEVICE_STATE_ROTATION_LOCK}.
         * @hide
         */
        @IntDef(prefix = {"DEVICE_STATE_ROTATION_KEY_"}, value = {
                DEVICE_STATE_ROTATION_KEY_UNKNOWN,
                DEVICE_STATE_ROTATION_KEY_FOLDED,
                DEVICE_STATE_ROTATION_KEY_HALF_FOLDED,
                DEVICE_STATE_ROTATION_KEY_UNFOLDED,
                DEVICE_STATE_ROTATION_KEY_REAR_DISPLAY,
        })
        @Retention(RetentionPolicy.SOURCE)
        public @interface DeviceStateRotationLockKey {
        }

        /**
         * Rotation lock setting keyed on device state.
         *
         * This holds a serialized map using int keys that represent postures in
         * {@link DeviceStateRotationLockKey} and value of
         * {@link DeviceStateRotationLockSetting} representing the rotation lock setting for that
         * posture.
         *
         * Serialized as key0:value0:key1:value1:...:keyN:valueN.
         *
         * Example: "0:1:1:2:2:1"
         * This example represents a map of:
         * <ul>
         *     <li>DEVICE_STATE_ROTATION_KEY_FOLDED -> DEVICE_STATE_ROTATION_LOCK_LOCKED</li>
         *     <li>DEVICE_STATE_ROTATION_KEY_HALF_FOLDED -> DEVICE_STATE_ROTATION_LOCK_UNLOCKED</li>
         *     <li>DEVICE_STATE_ROTATION_KEY_UNFOLDED -> DEVICE_STATE_ROTATION_LOCK_IGNORED</li>
         * </ul>
         *
         * @hide
         */
        public static final String DEVICE_STATE_ROTATION_LOCK =
                "device_state_rotation_lock";

        /**
         * Control whether communal mode is allowed on this device.
         *
         * @hide
         */
        public static final String COMMUNAL_MODE_ENABLED = "communal_mode_enabled";

        /**
         * An array of SSIDs of Wi-Fi networks that, when connected, are considered safe to enable
         * the communal mode.
         *
         * @hide
         */
        public static final String COMMUNAL_MODE_TRUSTED_NETWORKS =
                "communal_mode_trusted_networks";

        /**
         * Setting to store denylisted system languages by the CEC {@code <Set Menu Language>}
         * confirmation dialog.
         *
         * @hide
         */
        public static final String HDMI_CEC_SET_MENU_LANGUAGE_DENYLIST =
                "hdmi_cec_set_menu_language_denylist";

        /**
         * Whether the Taskbar Education is about to be shown or is currently showing.
         *
         * <p>1 if true, 0 or unset otherwise.
         *
         * <p>This setting is used to inform other components that the Taskbar Education is
         * currently showing, which can prevent them from showing something else to the user.
         *
         * @hide
         */
        public static final String LAUNCHER_TASKBAR_EDUCATION_SHOWING =
                "launcher_taskbar_education_showing";

        /**
         * Whether or not adaptive charging feature is enabled by user.
         * Type: int (0 for false, 1 for true)
         * Default: 1
         *
         * @hide
         */
        public static final String ADAPTIVE_CHARGING_ENABLED = "adaptive_charging_enabled";

        /**
         * Whether battery saver is currently set to different schedule mode.
         *
         * @hide
         */
        public static final String EXTRA_AUTOMATIC_POWER_SAVE_MODE =
                "extra_automatic_power_save_mode";

        /**
         * Whether contextual screen timeout is enabled.
         *
         * @hide
         */
        public static final String CONTEXTUAL_SCREEN_TIMEOUT_ENABLED =
                "contextual_screen_timeout_enabled";

        /**
         * Whether lockscreen weather is enabled.
         *
         * @hide
         */
        public static final String LOCK_SCREEN_WEATHER_ENABLED = "lockscreen_weather_enabled";

        /**
         * Whether the feature that the device will fire a haptic when users scroll and hit
         * the edge of the screen is enabled.
         *
         * @hide
         */
        public static final String ACCESSIBILITY_DISPLAY_MAGNIFICATION_EDGE_HAPTIC_ENABLED =
                "accessibility_display_magnification_edge_haptic_enabled";

        /**
         * If 1, DND default allowed packages have been updated
         *
         *  @hide
         */
        public static final String DND_CONFIGS_MIGRATED = "dnd_settings_migrated";

        /**
         * Controls whether to hide private space entry point in All Apps
         *
         * @hide
         */
        public static final String HIDE_PRIVATESPACE_ENTRY_POINT = "hide_privatespace_entry_point";

        /** @hide */
        public static final int PRIVATE_SPACE_AUTO_LOCK_ON_DEVICE_LOCK = 0;
        /** @hide */
        public static final int PRIVATE_SPACE_AUTO_LOCK_AFTER_INACTIVITY = 1;
        /** @hide */
        public static final int PRIVATE_SPACE_AUTO_LOCK_NEVER = 2;

        /**
         * The different auto lock options for private space.
         *
         * @hide
         */
        @IntDef(prefix = {"PRIVATE_SPACE_AUTO_LOCK_"}, value = {
                PRIVATE_SPACE_AUTO_LOCK_ON_DEVICE_LOCK,
                PRIVATE_SPACE_AUTO_LOCK_AFTER_INACTIVITY,
                PRIVATE_SPACE_AUTO_LOCK_NEVER,
        })
        @Retention(RetentionPolicy.SOURCE)
        public @interface PrivateSpaceAutoLockOption {
        }


        /**
         *  Store auto lock value for private space.
         *  The possible values are defined in {@link PrivateSpaceAutoLockOption}.
         *
         * @hide
         */
        public static final String PRIVATE_SPACE_AUTO_LOCK = "private_space_auto_lock";

        /**
         * Toggle for enabling stylus pointer icon. Pointer icons for styluses will only be be shown
         * when this is enabled. Enabling this alone won't enable the stylus pointer;
         * config_enableStylusPointerIcon needs to be true as well.
         *
         * @hide
         */
        @Readable
        public static final String STYLUS_POINTER_ICON_ENABLED = "stylus_pointer_icon_enabled";


        /**
         * Whether to show ambient instead of waking for the dt2w gesture
         * @hide
         */
        public static final String DOZE_DOUBLE_TAP_GESTURE_AMBIENT = "doze_double_tap_gesture_ambient";

        /**
         * Whether to show ambient instead of waking for the pickup gesture
         * Do note quick pickup (device sensor) is already configured to do that
         * @hide
         */
        public static final String DOZE_PICK_UP_GESTURE_AMBIENT = "doze_pick_up_gesture_ambient";

        /**
         * Control whether FLAG_SECURE is ignored for all windows.
         * @hide
         */
        @Readable
        public static final String WINDOW_IGNORE_SECURE = "window_ignore_secure";
        
        /**
         * These entries are considered common between the personal and the managed profile,
         * since the managed profile doesn't get to change them.
         */
        private static final Set<String> CLONE_TO_MANAGED_PROFILE = new ArraySet<>();

        static {
            CLONE_TO_MANAGED_PROFILE.add(ACCESSIBILITY_ENABLED);
            CLONE_TO_MANAGED_PROFILE.add(ALLOW_MOCK_LOCATION);
            CLONE_TO_MANAGED_PROFILE.add(ALLOWED_GEOLOCATION_ORIGINS);
            CLONE_TO_MANAGED_PROFILE.add(CONTENT_CAPTURE_ENABLED);
            CLONE_TO_MANAGED_PROFILE.add(ENABLED_ACCESSIBILITY_SERVICES);
            CLONE_TO_MANAGED_PROFILE.add(LOCATION_CHANGER);
            CLONE_TO_MANAGED_PROFILE.add(LOCATION_MODE);
            CLONE_TO_MANAGED_PROFILE.add(SHOW_IME_WITH_HARD_KEYBOARD);
            CLONE_TO_MANAGED_PROFILE.add(ACCESSIBILITY_BOUNCE_KEYS);
            CLONE_TO_MANAGED_PROFILE.add(ACCESSIBILITY_SLOW_KEYS);
            CLONE_TO_MANAGED_PROFILE.add(ACCESSIBILITY_STICKY_KEYS);
            CLONE_TO_MANAGED_PROFILE.add(NOTIFICATION_BUBBLES);
            CLONE_TO_MANAGED_PROFILE.add(NOTIFICATION_HISTORY_ENABLED);
        }

        /** @hide */
        public static void getCloneToManagedProfileSettings(Set<String> outKeySet) {
            outKeySet.addAll(CLONE_TO_MANAGED_PROFILE);
        }

        /**
         * Secure settings which can be accessed by instant apps.
         * @hide
         */
        public static final Set<String> INSTANT_APP_SETTINGS = new ArraySet<>();
        static {
            INSTANT_APP_SETTINGS.add(ENABLED_ACCESSIBILITY_SERVICES);
            INSTANT_APP_SETTINGS.add(ACCESSIBILITY_SPEAK_PASSWORD);
            INSTANT_APP_SETTINGS.add(ACCESSIBILITY_DISPLAY_INVERSION_ENABLED);
            INSTANT_APP_SETTINGS.add(ACCESSIBILITY_CAPTIONING_ENABLED);
            INSTANT_APP_SETTINGS.add(ACCESSIBILITY_CAPTIONING_PRESET);
            INSTANT_APP_SETTINGS.add(ACCESSIBILITY_CAPTIONING_EDGE_TYPE);
            INSTANT_APP_SETTINGS.add(ACCESSIBILITY_CAPTIONING_EDGE_COLOR);
            INSTANT_APP_SETTINGS.add(ACCESSIBILITY_CAPTIONING_LOCALE);
            INSTANT_APP_SETTINGS.add(ACCESSIBILITY_CAPTIONING_BACKGROUND_COLOR);
            INSTANT_APP_SETTINGS.add(ACCESSIBILITY_CAPTIONING_FOREGROUND_COLOR);
            INSTANT_APP_SETTINGS.add(ACCESSIBILITY_CAPTIONING_TYPEFACE);
            INSTANT_APP_SETTINGS.add(ACCESSIBILITY_CAPTIONING_FONT_SCALE);
            INSTANT_APP_SETTINGS.add(ACCESSIBILITY_CAPTIONING_WINDOW_COLOR);
            INSTANT_APP_SETTINGS.add(ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED);
            INSTANT_APP_SETTINGS.add(ACCESSIBILITY_DISPLAY_DALTONIZER);
            INSTANT_APP_SETTINGS.add(ACCESSIBILITY_AUTOCLICK_DELAY);
            INSTANT_APP_SETTINGS.add(ACCESSIBILITY_AUTOCLICK_ENABLED);
            INSTANT_APP_SETTINGS.add(ACCESSIBILITY_LARGE_POINTER_ICON);

            INSTANT_APP_SETTINGS.add(DEFAULT_INPUT_METHOD);
            INSTANT_APP_SETTINGS.add(ENABLED_INPUT_METHODS);

            INSTANT_APP_SETTINGS.add(ANDROID_ID);

            INSTANT_APP_SETTINGS.add(ALLOW_MOCK_LOCATION);
        }

        /**
         * Helper method for determining if a location provider is enabled.
         *
         * @param cr the content resolver to use
         * @param provider the location provider to query
         * @return true if the provider is enabled
         *
         * @deprecated use {@link LocationManager#isProviderEnabled(String)}
         */
        @Deprecated
        public static boolean isLocationProviderEnabled(ContentResolver cr, String provider) {
            IBinder binder = ServiceManager.getService(Context.LOCATION_SERVICE);
            ILocationManager lm = Objects.requireNonNull(ILocationManager.Stub.asInterface(binder));
            try {
                return lm.isProviderEnabledForUser(provider, cr.getUserId());
         