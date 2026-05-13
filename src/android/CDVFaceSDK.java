package com.eov.cordova.facesdk;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

import androidx.core.content.ContextCompat;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.eov.facesdk.FaceSDK;
import com.eov.facesdk.SDKException;
import com.eov.facekit.ui.MultiStepRegisterActivity;
import com.eov.facekit.ui.LiveRecognitionActivity;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.util.Base64;

/**
 * Cordova Plugin for Face SDK
 * Bridges JavaScript calls to Android Face SDK
 *
 * Copyright (c) 2024 EOV Solutions. All rights reserved.
 */
public class CDVFaceSDK extends CordovaPlugin {

    private static final String TAG = "CDVFaceSDK";
    private static final int REQUEST_REGISTRATION = 3001;
    private static final int REQUEST_RECOGNITION = 3002;
    private static final int REQUEST_CAMERA_PERMISSION = 3003;
    private static final int REQUEST_CAMERA_FOR_REGISTRATION = 3004;
    private static final int REQUEST_CAMERA_FOR_RECOGNITION = 3005;

    private CallbackContext registrationCallback;
    private CallbackContext recognitionCallback;
    private CallbackContext permissionCallback;

    // Pending args for auto-permission-request flow
    private JSONArray pendingRegistrationArgs;
    private CallbackContext pendingRegistrationCallback;
    private JSONArray pendingRecognitionArgs;
    private CallbackContext pendingRecognitionCallback;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        switch (action) {
            case "initialize":
                initialize(args, callbackContext);
                return true;
            case "isLicenseValid":
                isLicenseValid(callbackContext);
                return true;
            case "getLicenseInfo":
                getLicenseInfo(callbackContext);
                return true;
            case "getLicenseStatus":
                getLicenseStatus(callbackContext);
                return true;
            case "startRegistration":
                startRegistration(args, callbackContext);
                return true;
            case "isUserEnrolled":
                isUserEnrolled(args, callbackContext);
                return true;
            case "deleteUser":
                deleteUser(args, callbackContext);
                return true;
            case "refreshEmbeddings":
                refreshEmbeddings(args, callbackContext);
                return true;
            case "startRecognition":
                startRecognition(args, callbackContext);
                return true;
            case "checkPermission":
                checkPermission(callbackContext);
                return true;
            case "requestPermission":
                requestPermission(callbackContext);
                return true;
            case "terminate":
                terminate(callbackContext);
                return true;
            case "isInitialized":
                isInitialized(callbackContext);
                return true;
            case "checkDeviceCompatibility":
                checkDeviceCompatibility(callbackContext);
                return true;
            default:
                return false;
        }
    }

    // ============ License & Initialization ============

    private void initialize(JSONArray args, CallbackContext callbackContext) {
        JSONObject options = args.optJSONObject(0);
        if (options == null) {
            try {
                JSONObject err = new JSONObject();
                err.put("code", "E_INVALID_PARAMS");
                err.put("message", "options object is required");
                callbackContext.error(err);
            } catch (JSONException e) {
                callbackContext.error("options object is required");
            }
            return;
        }

        String licenseKey = options.optString("licenseKey", "");
        if (licenseKey.isEmpty()) {
            try {
                JSONObject err = new JSONObject();
                err.put("code", "E_INVALID_PARAMS");
                err.put("message", "licenseKey is required");
                callbackContext.error(err);
            } catch (JSONException e) {
                callbackContext.error("licenseKey is required");
            }
            return;
        }

        String faceId = options.optString("faceId", null);
        String userName = options.optString("userName", null);
        String orgId = options.optString("orgId", null);
        String onPremiseServerUrl = options.optString("onPremiseServerUrl", null);
        String tenantId = options.optString("tenant_id", options.optString("tenantId", null));

        if (userName != null && !userName.isEmpty()) {
            FaceSDK.setUserName(userName);
        }

        try {
            FaceSDK.initializeLicense(
                cordova.getActivity().getApplicationContext(),
                licenseKey,
                faceId,
                onPremiseServerUrl,
                tenantId,
                new FaceSDK.LicenseCallback() {
                    @Override
                    public void onSuccess() {
                        FaceSDK.initialize(cordova.getActivity().getApplicationContext(), new FaceSDK.InitCallback() {
                            @Override
                            public void onSuccess() {
                                if (orgId != null && !orgId.isEmpty()) {
                                    FaceSDK.getInstance().setOrganization(orgId);
                                }
                                try {
                                    JSONObject result = new JSONObject();
                                    result.put("success", true);
                                    result.put("message", "SDK initialized successfully");
                                    callbackContext.success(result);
                                } catch (JSONException e) {
                                    callbackContext.success("SDK initialized successfully");
                                }
                            }

                            @Override
                            public void onError(SDKException error) {
                                try {
                                    JSONObject err = new JSONObject();
                                    err.put("code", "E_INIT_FAILED");
                                    err.put("message", error.getMessage() != null ? error.getMessage() : "SDK initialization failed");
                                    callbackContext.error(err);
                                } catch (JSONException e) {
                                    callbackContext.error("SDK initialization failed");
                                }
                            }
                        });
                    }

                    @Override
                    public void onError(String errorMessage) {
                        try {
                            JSONObject err = new JSONObject();
                            err.put("code", "E_LICENSE_FAILED");
                            err.put("message", errorMessage);
                            callbackContext.error(err);
                        } catch (JSONException e) {
                            callbackContext.error(errorMessage);
                        }
                    }
                }
            );
        } catch (Exception e) {
            Log.e(TAG, "Initialize failed", e);
            try {
                JSONObject err = new JSONObject();
                err.put("code", "E_INIT_FAILED");
                err.put("message", e.getMessage() != null ? e.getMessage() : "Unknown error");
                callbackContext.error(err);
            } catch (JSONException je) {
                callbackContext.error("Initialize failed");
            }
        }
    }

    private void isLicenseValid(CallbackContext callbackContext) {
        try {
            boolean valid = FaceSDK.isLicenseValid();
            int status = FaceSDK.getLicenseStatus();
            String message = FaceSDK.getStatusMessage(status);

            JSONObject result = new JSONObject();
            result.put("valid", valid);
            result.put("status", status);
            result.put("message", message);
            callbackContext.success(result);
        } catch (Exception e) {
            try {
                JSONObject err = new JSONObject();
                err.put("code", "E_LICENSE_CHECK_FAILED");
                err.put("message", e.getMessage() != null ? e.getMessage() : "Unknown error");
                callbackContext.error(err);
            } catch (JSONException je) {
                callbackContext.error("License check failed");
            }
        }
    }

    private void getLicenseInfo(CallbackContext callbackContext) {
        try {
            int status = FaceSDK.getLicenseStatus();
            JSONObject result = new JSONObject();
            result.put("isValid", FaceSDK.isLicenseValid());
            result.put("status", status);
            result.put("message", FaceSDK.getStatusMessage(status));
            callbackContext.success(result);
        } catch (Exception e) {
            try {
                JSONObject err = new JSONObject();
                err.put("code", "E_LICENSE_INFO_FAILED");
                err.put("message", e.getMessage() != null ? e.getMessage() : "Unknown error");
                callbackContext.error(err);
            } catch (JSONException je) {
                callbackContext.error("License info failed");
            }
        }
    }

    private void getLicenseStatus(CallbackContext callbackContext) {
        try {
            int status = FaceSDK.getLicenseStatus();
            callbackContext.success(status);
        } catch (Exception e) {
            try {
                JSONObject err = new JSONObject();
                err.put("code", "E_LICENSE_STATUS_FAILED");
                err.put("message", e.getMessage() != null ? e.getMessage() : "Unknown error");
                callbackContext.error(err);
            } catch (JSONException je) {
                callbackContext.error("License status failed");
            }
        }
    }

    // ============ Terminate & Cleanup ============

    private void terminate(CallbackContext callbackContext) {
        try {
            FaceSDK.getInstance().terminate();
            JSONObject result = new JSONObject();
            result.put("success", true);
            result.put("message", "SDK terminated successfully");
            callbackContext.success(result);
        } catch (Exception e) {
            Log.e(TAG, "Terminate failed", e);
            try {
                JSONObject err = new JSONObject();
                err.put("code", "E_TERMINATE_FAILED");
                err.put("message", e.getMessage() != null ? e.getMessage() : "Unknown error");
                callbackContext.error(err);
            } catch (JSONException je) {
                callbackContext.error("Terminate failed");
            }
        }
    }

    private void isInitialized(CallbackContext callbackContext) {
        try {
            boolean initialized = FaceSDK.getInstance().isInitialized();
            PluginResult result = new PluginResult(PluginResult.Status.OK, initialized);
            callbackContext.sendPluginResult(result);
        } catch (Exception e) {
            try {
                JSONObject err = new JSONObject();
                err.put("code", "E_CHECK_FAILED");
                err.put("message", e.getMessage() != null ? e.getMessage() : "Unknown error");
                callbackContext.error(err);
            } catch (JSONException je) {
                callbackContext.error("Check failed");
            }
        }
    }

    // ============ Registration ============

    private void startRegistration(JSONArray args, CallbackContext callbackContext) {
        Activity activity = cordova.getActivity();
        if (activity == null) {
            sendError(callbackContext, "E_NO_ACTIVITY", "Activity is null");
            return;
        }

        if (!FaceSDK.getInstance().isInitialized()) {
            sendError(callbackContext, "E_NOT_INITIALIZED", "SDK not initialized");
            return;
        }

        if (!hasPermission()) {
            // Auto-request permission, then retry
            pendingRegistrationArgs = args;
            pendingRegistrationCallback = callbackContext;
            cordova.requestPermission(this, REQUEST_CAMERA_FOR_REGISTRATION, Manifest.permission.CAMERA);
            return;
        }

        launchRegistration(args, callbackContext);
    }

    private void launchRegistration(JSONArray args, CallbackContext callbackContext) {
        Activity activity = cordova.getActivity();
        registrationCallback = callbackContext;

        try {
            JSONObject options = args.optJSONObject(0);
            if (options == null) {
                options = new JSONObject();
            }
            Intent intent = new Intent(activity, MultiStepRegisterActivity.class);

            if (options.has("userName")) {
                intent.putExtra(MultiStepRegisterActivity.EXTRA_USER_NAME, options.getString("userName"));
            }
            if (options.has("orgId")) {
                intent.putExtra(MultiStepRegisterActivity.EXTRA_ORG_ID, options.getString("orgId"));
            }
            if (options.has("skipNameDialog")) {
                intent.putExtra(MultiStepRegisterActivity.EXTRA_SKIP_NAME_DIALOG, options.getBoolean("skipNameDialog"));
            }
            if (options.has("mode")) {
                intent.putExtra(MultiStepRegisterActivity.EXTRA_REGISTRATION_MODE, options.getString("mode"));
            }

            cordova.startActivityForResult(this, intent, REQUEST_REGISTRATION);
        } catch (Exception e) {
            Log.e(TAG, "Error launching registration", e);
            registrationCallback = null;
            sendError(callbackContext, "E_LAUNCH_FAILED", "Failed to launch registration: " + e.getMessage());
        }
    }

    private void isUserEnrolled(JSONArray args, CallbackContext callbackContext) {
        String userId = args.optString(0, "");
        if (userId.isEmpty()) {
            sendError(callbackContext, "E_INVALID_PARAMS", "userId is required");
            return;
        }

        try {
            boolean enrolled = FaceSDK.getInstance().isUserEnrolled(userId);
            PluginResult result = new PluginResult(PluginResult.Status.OK, enrolled);
            callbackContext.sendPluginResult(result);
        } catch (Exception e) {
            sendError(callbackContext, "E_CHECK_FAILED", e.getMessage() != null ? e.getMessage() : "Unknown error");
        }
    }

    private void deleteUser(JSONArray args, CallbackContext callbackContext) {
        String userId = args.optString(0, "");
        if (userId.isEmpty()) {
            sendError(callbackContext, "E_INVALID_PARAMS", "userId is required");
            return;
        }

        try {
            boolean deleted = FaceSDK.getInstance().deleteUser(userId);
            PluginResult result = new PluginResult(PluginResult.Status.OK, deleted);
            callbackContext.sendPluginResult(result);
        } catch (Exception e) {
            sendError(callbackContext, "E_DELETE_FAILED", e.getMessage() != null ? e.getMessage() : "Unknown error");
        }
    }

    // ============ Refresh Embeddings ============

    private void refreshEmbeddings(JSONArray args, CallbackContext callbackContext) {
        if (!FaceSDK.getInstance().isInitialized()) {
            sendError(callbackContext, "E_NOT_INITIALIZED", "SDK not initialized");
            return;
        }

        String faceId = args.optString(0, "");
        if (faceId.isEmpty()) {
            sendError(callbackContext, "E_INVALID_PARAMS", "faceId is required");
            return;
        }

        try {
            FaceSDK.refreshEmbeddings(
                cordova.getActivity().getApplicationContext(),
                faceId,
                new FaceSDK.RefreshCallback() {
                    @Override
                    public void onSuccess(int deletedCount, String userId, String userName) {
                        try {
                            JSONObject result = new JSONObject();
                            result.put("success", true);
                            result.put("deletedCount", deletedCount);
                            result.put("userId", userId);
                            result.put("userName", userName);
                            result.put("message", "Embeddings refreshed successfully");
                            callbackContext.success(result);
                        } catch (JSONException e) {
                            callbackContext.success("Embeddings refreshed successfully");
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {
                        sendError(callbackContext, "E_REFRESH_FAILED", errorMessage);
                    }
                }
            );
        } catch (Exception e) {
            Log.e(TAG, "refreshEmbeddings failed", e);
            sendError(callbackContext, "E_REFRESH_FAILED", e.getMessage() != null ? e.getMessage() : "Unknown error");
        }
    }

    // ============ Recognition ============

    private void startRecognition(JSONArray args, CallbackContext callbackContext) {
        Activity activity = cordova.getActivity();
        if (activity == null) {
            sendError(callbackContext, "E_NO_ACTIVITY", "Activity is null");
            return;
        }

        if (!FaceSDK.getInstance().isInitialized()) {
            sendError(callbackContext, "E_NOT_INITIALIZED", "SDK not initialized");
            return;
        }

        if (!hasPermission()) {
            // Auto-request permission, then retry
            pendingRecognitionArgs = args;
            pendingRecognitionCallback = callbackContext;
            cordova.requestPermission(this, REQUEST_CAMERA_FOR_RECOGNITION, Manifest.permission.CAMERA);
            return;
        }

        launchRecognition(args, callbackContext);
    }

    private void launchRecognition(JSONArray args, CallbackContext callbackContext) {
        Activity activity = cordova.getActivity();
        recognitionCallback = callbackContext;

        try {
            JSONObject options = args.optJSONObject(0);
            if (options == null) {
                options = new JSONObject();
            }
            Intent intent = new Intent(activity, LiveRecognitionActivity.class);

            if (options.has("orgId")) {
                intent.putExtra(LiveRecognitionActivity.EXTRA_ORG_ID, options.getString("orgId"));
            }
            if (options.has("timeoutSeconds")) {
                intent.putExtra(LiveRecognitionActivity.EXTRA_TIMEOUT_SECONDS, options.getInt("timeoutSeconds"));
            }
            if (options.has("mode")) {
                intent.putExtra(LiveRecognitionActivity.EXTRA_RECOGNITION_MODE, options.getString("mode"));
            }
            if (options.has("userId")) {
                intent.putExtra(LiveRecognitionActivity.EXTRA_VERIFY_USER_ID, options.getString("userId"));
            }

            cordova.startActivityForResult(this, intent, REQUEST_RECOGNITION);
        } catch (Exception e) {
            Log.e(TAG, "Error launching recognition", e);
            recognitionCallback = null;
            sendError(callbackContext, "E_LAUNCH_FAILED", "Failed to launch recognition: " + e.getMessage());
        }
    }

    // ============ Permissions ============

    private void checkPermission(CallbackContext callbackContext) {
        boolean granted = hasPermission();
        try {
            JSONObject result = new JSONObject();
            result.put("granted", granted);
            result.put("status", granted ? "granted" : "denied");
            callbackContext.success(result);
        } catch (JSONException e) {
            callbackContext.success(granted ? "granted" : "denied");
        }
    }

    private void requestPermission(CallbackContext callbackContext) {
        if (hasPermission()) {
            try {
                JSONObject result = new JSONObject();
                result.put("granted", true);
                result.put("status", "granted");
                callbackContext.success(result);
            } catch (JSONException e) {
                callbackContext.success("granted");
            }
            return;
        }

        Activity activity = cordova.getActivity();
        if (activity == null) {
            sendError(callbackContext, "E_NO_ACTIVITY", "Activity is null");
            return;
        }

        permissionCallback = callbackContext;
        cordova.requestPermission(this, REQUEST_CAMERA_PERMISSION, Manifest.permission.CAMERA);
    }

    // ============ Device Compatibility ============

    private void checkDeviceCompatibility(CallbackContext callbackContext) {
        try {
            Context context = cordova.getActivity().getApplicationContext();
            List<String> unsupportedReasons = new ArrayList<>();

            // 1. Check front camera
            boolean hasFrontCamera = checkFrontCamera(context);
            if (!hasFrontCamera) unsupportedReasons.add("No front-facing camera detected");

            // 2. Check OS version (Android 7.0 / API 24+)
            String osVersion = String.valueOf(Build.VERSION.SDK_INT);
            boolean osVersionSupported = Build.VERSION.SDK_INT >= 24;
            if (!osVersionSupported) unsupportedReasons.add("Android API " + Build.VERSION.SDK_INT + " is below minimum (24 / Android 7.0)");

            // 3. Check CPU architecture (arm64-v8a or armeabi-v7a required for AI models)
            String[] abis = Build.SUPPORTED_ABIS;
            List<String> supportedAbis = abis != null ? Arrays.asList(abis) : new ArrayList<>();
            boolean cpuArchSupported = false;
            for (String abi : supportedAbis) {
                if ("arm64-v8a".equals(abi) || "armeabi-v7a".equals(abi)) {
                    cpuArchSupported = true;
                    break;
                }
            }
            if (!cpuArchSupported) unsupportedReasons.add("CPU architecture does not include arm64-v8a or armeabi-v7a");

            // 4. Check RAM (>= 2GB)
            ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
            activityManager.getMemoryInfo(memInfo);
            int totalRAMMB = (int) (memInfo.totalMem / (1024 * 1024));
            boolean hasEnoughRAM = totalRAMMB >= 2048;
            if (!hasEnoughRAM) unsupportedReasons.add("Insufficient RAM: " + totalRAMMB + "MB (minimum 2048MB)");

            // 5. Check available storage (>= 100MB)
            StatFs stat = new StatFs(Environment.getDataDirectory().getPath());
            int availableStorageMB = (int) (stat.getAvailableBlocksLong() * stat.getBlockSizeLong() / (1024 * 1024));
            boolean hasEnoughStorage = availableStorageMB >= 100;
            if (!hasEnoughStorage) unsupportedReasons.add("Insufficient storage: " + availableStorageMB + "MB available (minimum 100MB)");

            // Device model
            String deviceModel = Build.MANUFACTURER + " " + Build.MODEL;

            // Overall compatibility
            boolean compatible = hasFrontCamera && osVersionSupported && cpuArchSupported && hasEnoughRAM && hasEnoughStorage;

            JSONObject checks = new JSONObject();
            checks.put("hasFrontCamera", hasFrontCamera);
            checks.put("osVersionSupported", osVersionSupported);
            checks.put("cpuArchSupported", cpuArchSupported);
            checks.put("hasEnoughRAM", hasEnoughRAM);
            checks.put("hasEnoughStorage", hasEnoughStorage);

            JSONArray reasons = new JSONArray();
            for (String reason : unsupportedReasons) {
                reasons.put(reason);
            }

            String message;
            if (compatible) {
                message = "Device is compatible with Face SDK";
            } else {
                StringBuilder sb = new StringBuilder("Device is NOT compatible: ");
                for (int i = 0; i < unsupportedReasons.size(); i++) {
                    if (i > 0) sb.append("; ");
                    sb.append(unsupportedReasons.get(i));
                }
                message = sb.toString();
            }

            JSONObject result = new JSONObject();
            result.put("compatible", compatible);
            result.put("message", message);
            result.put("platform", "android");
            result.put("osVersion", osVersion);
            result.put("deviceModel", deviceModel);
            result.put("checks", checks);
            result.put("totalRAM", totalRAMMB);
            result.put("availableStorage", availableStorageMB);
            result.put("unsupportedReasons", reasons);
            callbackContext.success(result);
        } catch (Exception e) {
            Log.e(TAG, "checkDeviceCompatibility failed", e);
            sendError(callbackContext, "E_COMPATIBILITY_CHECK_FAILED", e.getMessage() != null ? e.getMessage() : "Unknown error");
        }
    }

    private boolean checkFrontCamera(Context context) {
        try {
            CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            for (String cameraId : cameraManager.getCameraIdList()) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            Log.w(TAG, "Error checking front camera", e);
            return false;
        }
    }

    // ============ Internal Helpers ============

    private boolean hasPermission() {
        return ContextCompat.checkSelfPermission(
            cordova.getActivity(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) throws JSONException {
        boolean granted = grantResults.length > 0 &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED;

        switch (requestCode) {
            case REQUEST_CAMERA_PERMISSION:
                if (permissionCallback != null) {
                    JSONObject result = new JSONObject();
                    result.put("granted", granted);
                    result.put("status", granted ? "granted" : "denied");
                    permissionCallback.success(result);
                    permissionCallback = null;
                }
                break;

            case REQUEST_CAMERA_FOR_REGISTRATION:
                if (granted && pendingRegistrationCallback != null) {
                    launchRegistration(pendingRegistrationArgs, pendingRegistrationCallback);
                } else if (pendingRegistrationCallback != null) {
                    sendError(pendingRegistrationCallback, "E_PERMISSION", "Camera permission denied by user");
                }
                pendingRegistrationArgs = null;
                pendingRegistrationCallback = null;
                break;

            case REQUEST_CAMERA_FOR_RECOGNITION:
                if (granted && pendingRecognitionCallback != null) {
                    launchRecognition(pendingRecognitionArgs, pendingRecognitionCallback);
                } else if (pendingRecognitionCallback != null) {
                    sendError(pendingRecognitionCallback, "E_PERMISSION", "Camera permission denied by user");
                }
                pendingRecognitionArgs = null;
                pendingRecognitionCallback = null;
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_REGISTRATION:
                handleRegistrationResult(resultCode, data);
                break;
            case REQUEST_RECOGNITION:
                handleRecognitionResult(resultCode, data);
                break;
        }
    }

    private void handleRegistrationResult(int resultCode, Intent data) {
        if (registrationCallback == null) return;
        CallbackContext callback = registrationCallback;
        registrationCallback = null;

        try {
            if (resultCode == Activity.RESULT_OK && data != null) {
                boolean success = data.getBooleanExtra(MultiStepRegisterActivity.EXTRA_RESULT_SUCCESS, false);
                String userId = data.getStringExtra(MultiStepRegisterActivity.EXTRA_RESULT_USER_ID);
                String userName = data.getStringExtra(MultiStepRegisterActivity.EXTRA_RESULT_USER_NAME);
                String orgId = data.getStringExtra(MultiStepRegisterActivity.EXTRA_RESULT_ORG_ID);
                int featureCount = data.getIntExtra(MultiStepRegisterActivity.EXTRA_RESULT_FEATURE_COUNT, 0);

                JSONObject result = new JSONObject();
                result.put("success", success);
                result.put("userId", userId != null ? userId : "");
                result.put("userName", userName != null ? userName : "");
                result.put("orgId", orgId != null ? orgId : "");
                result.put("featureCount", featureCount);
                callback.success(result);
            } else {
                JSONObject result = new JSONObject();
                result.put("success", false);
                result.put("error", "Registration cancelled");
                callback.success(result);
            }
        } catch (JSONException e) {
            callback.error("Registration result processing failed");
        }
    }

    private void handleRecognitionResult(int resultCode, Intent data) {
        if (recognitionCallback == null) return;
        CallbackContext callback = recognitionCallback;
        recognitionCallback = null;

        try {
            if (resultCode == Activity.RESULT_OK && data != null) {
                String userId = data.getStringExtra(LiveRecognitionActivity.EXTRA_USER_ID);
                String userName = data.getStringExtra(LiveRecognitionActivity.EXTRA_USER_NAME);
                float confidence = data.getFloatExtra(LiveRecognitionActivity.EXTRA_CONFIDENCE, 0f);
                boolean isLive = data.getBooleanExtra(LiveRecognitionActivity.EXTRA_IS_LIVE, false);
                boolean isRecognized = data.getBooleanExtra(LiveRecognitionActivity.EXTRA_IS_RECOGNIZED, false);
                String imagePath = data.getStringExtra(LiveRecognitionActivity.EXTRA_IMAGE_PATH);

                JSONObject result = new JSONObject();
                result.put("success", true);
                result.put("isLive", isLive);
                result.put("isRecognized", isRecognized);
                result.put("userId", userId != null ? userId : "");
                result.put("userName", userName != null ? userName : "");
                result.put("confidence", (double) confidence);
                result.put("imagePath", imagePath != null ? imagePath : "");

                String imageBase64 = "";
                if (imagePath != null && !imagePath.isEmpty()) {
                    try {
                        File imgFile = new File(imagePath);
                        if (imgFile.exists()) {
                            byte[] bytes = new byte[(int) imgFile.length()];
                            FileInputStream fis = new FileInputStream(imgFile);
                            fis.read(bytes);
                            fis.close();
                            imageBase64 = "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP);
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Failed to encode image to base64", e);
                    }
                }
                result.put("imageBase64", imageBase64);
                callback.success(result);
            } else {
                String errorMessage = "Recognition cancelled";

                if (data != null) {
                    String intentMessage = data.getStringExtra(LiveRecognitionActivity.EXTRA_ERROR_MESSAGE);
                    String legacyError = data.getStringExtra("error");

                    if (intentMessage != null && !intentMessage.isEmpty()) {
                        errorMessage = intentMessage;
                    } else if (legacyError != null && !legacyError.isEmpty()) {
                        errorMessage = legacyError;
                    }
                }

                JSONObject result = new JSONObject();
                result.put("success", false);
                result.put("isLive", false);
                result.put("isRecognized", false);
                result.put("error", errorMessage);
                callback.success(result);
            }
        } catch (JSONException e) {
            callback.error("Recognition result processing failed");
        }
    }

    private void sendError(CallbackContext callbackContext, String code, String message) {
        try {
            JSONObject err = new JSONObject();
            err.put("code", code);
            err.put("message", message);
            callbackContext.error(err);
        } catch (JSONException e) {
            callbackContext.error(message);
        }
    }
}
