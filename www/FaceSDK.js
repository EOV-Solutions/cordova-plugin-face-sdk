/**
 * Cordova Face SDK Plugin
 *
 * Face recognition module for Cordova/Ionic applications
 * Wrapper for FaceSDK.xcframework (iOS) and FaceKit AAR (Android)
 *
 * Copyright © 2024 EOV Solutions. All rights reserved.
 */

var exec = require('cordova/exec');

var SERVICE_NAME = 'FaceSDK';

// Store initialization data for use in registration
var _initData = {};

/**
 * @typedef {Object} InitializeOptions
 * @property {string} licenseKey - License key from server
 * @property {string} [faceId] - Optional face ID (used as userId in registration)
 * @property {string} [userName] - Optional user name (used in registration)
 */

/**
 * @typedef {Object} InitializeResult
 * @property {boolean} success
 * @property {string} [message]
 * @property {string} [orgId]
 * @property {string} [error]
 */

/**
 * @typedef {Object} LicenseResult
 * @property {boolean} valid
 * @property {number} [status]
 * @property {string} [message]
 */

/**
 * @typedef {Object} LicenseInfo
 * @property {boolean} isValid
 * @property {number} status
 * @property {string} [message]
 * @property {string} [expiryDate]
 * @property {number} [maxUsers]
 * @property {number} [currentUsers]
 * @property {string[]} [features]
 */

/**
 * @typedef {Object} PermissionResult
 * @property {boolean} granted
 * @property {string} status - 'granted' | 'denied' | 'restricted' | 'undetermined' | 'unknown'
 */

/**
 * @typedef {Object} RegistrationOptions
 * @property {string} [userId] - Optional user ID (uses faceId from initialize if not provided)
 * @property {string} [userName] - Optional user display name
 * @property {boolean} [skipNameDialog] - Skip name dialog and use provided userName directly
 * @property {string} [mode] - Registration mode: 'create_only' (fail if exists), 'upsert' (update if exists, default), 'overwrite' (delete + insert)
 */

/**
 * @typedef {Object} RegistrationResult
 * @property {boolean} success
 * @property {string} [userId]
 * @property {string} [userName]
 * @property {string} [orgId]
 * @property {number} [featureCount]
 * @property {boolean} [serverSynced]
 * @property {string} [error]
 */

/**
 * @typedef {Object} RecognitionOptions
 * @property {number} [timeoutSeconds=30] - Recognition timeout in seconds
 */

/**
 * @typedef {Object} RecognitionResult
 * @property {boolean} success
 * @property {boolean} isLive - Whether the face is live (not a photo/video)
 * @property {boolean} isRecognized - Whether a user was recognized
 * @property {string} [userId] - User ID if recognized
 * @property {string} [userName] - User name if recognized
 * @property {number} [confidence] - Recognition confidence score (0-1)
 * @property {string} [imagePath] - Path to captured face image
 * @property {string} [error]
 */

/**
 * @typedef {Object} RefreshResult
 * @property {boolean} success
 * @property {number} deletedCount - Number of users deleted from local DB
 * @property {string} userId - User ID of re-imported user
 * @property {string} userName - User name of re-imported user
 * @property {string} [message]
 */

var FaceSDK = {

    /**
     * Initialize the Face SDK with license key
     * @param {InitializeOptions} options
     * @param {function} successCallback
     * @param {function} errorCallback
     */
    initialize: function (options, successCallback, errorCallback) {
        if (!options || !options.licenseKey) {
            if (errorCallback) {
                errorCallback({ code: 'E_INVALID_PARAMS', message: 'licenseKey is required' });
            }
            return;
        }

        // Store init data for use in registration
        _initData = {
            faceId: options.faceId,
            userName: options.userName
        };

        exec(successCallback, errorCallback, SERVICE_NAME, 'initialize', [options]);
    },

    /**
     * Check if license is valid and SDK is initialized
     * @param {function} successCallback
     * @param {function} errorCallback
     */
    isLicenseValid: function (successCallback, errorCallback) {
        exec(successCallback, errorCallback, SERVICE_NAME, 'isLicenseValid', []);
    },

    /**
     * Get detailed license information
     * @param {function} successCallback
     * @param {function} errorCallback
     */
    getLicenseInfo: function (successCallback, errorCallback) {
        exec(successCallback, errorCallback, SERVICE_NAME, 'getLicenseInfo', []);
    },

    /**
     * Get license status code
     * @param {function} successCallback
     * @param {function} errorCallback
     */
    getLicenseStatus: function (successCallback, errorCallback) {
        exec(successCallback, errorCallback, SERVICE_NAME, 'getLicenseStatus', []);
    },

    /**
     * Start face registration flow (5 poses: front, left, right, up, down)
     * Uses faceId as userId and userName from initialize() if not explicitly provided
     * @param {RegistrationOptions} [options={}]
     * @param {function} successCallback
     * @param {function} errorCallback
     */
    startRegistration: function (options, successCallback, errorCallback) {
        // Allow calling without options: startRegistration(success, error)
        if (typeof options === 'function') {
            errorCallback = successCallback;
            successCallback = options;
            options = {};
        }
        options = options || {};

        // Merge with init data - init data as defaults, options can override
        // Use explicit null/undefined check (not ||) to match RN's ?? behavior
        // so that empty string "" is preserved rather than falling back
        var mergedOptions = {
            userId: (options.userId != null && options.userId !== undefined) ? options.userId : _initData.faceId,
            userName: (options.userName != null && options.userName !== undefined) ? options.userName : _initData.userName,
            skipNameDialog: options.skipNameDialog,
            mode: options.mode
        };

        exec(successCallback, errorCallback, SERVICE_NAME, 'startRegistration', [mergedOptions]);
    },

    /**
     * Check if user is enrolled
     * @param {string} userId
     * @param {function} successCallback
     * @param {function} errorCallback
     */
    isUserEnrolled: function (userId, successCallback, errorCallback) {
        exec(successCallback, errorCallback, SERVICE_NAME, 'isUserEnrolled', [userId]);
    },

    /**
     * Delete a user from the database
     * @param {string} userId
     * @param {function} successCallback
     * @param {function} errorCallback
     */
    deleteUser: function (userId, successCallback, errorCallback) {
        exec(successCallback, errorCallback, SERVICE_NAME, 'deleteUser', [userId]);
    },

    /**
     * Refresh embeddings: Delete all local embeddings and re-download from server.
     * @param {string} [faceId] - Face ID to re-download embeddings for (defaults to faceId from initialize)
     * @param {function} successCallback
     * @param {function} errorCallback
     */
    refreshEmbeddings: function (faceId, successCallback, errorCallback) {
        // Allow calling with just callbacks: refreshEmbeddings(success, error)
        if (typeof faceId === 'function') {
            errorCallback = successCallback;
            successCallback = faceId;
            faceId = _initData.faceId;
        }

        if (!faceId) {
            if (errorCallback) {
                errorCallback({ code: 'E_INVALID_PARAMS', message: 'faceId is required. Pass it as parameter or set it in initialize()' });
            }
            return;
        }

        exec(successCallback, errorCallback, SERVICE_NAME, 'refreshEmbeddings', [faceId]);
    },

    /**
     * Start face recognition flow with liveness detection (3 random poses)
     * @param {RecognitionOptions} [options={}]
     * @param {function} successCallback
     * @param {function} errorCallback
     */
    startRecognition: function (options, successCallback, errorCallback) {
        // Allow calling without options: startRecognition(success, error)
        if (typeof options === 'function') {
            errorCallback = successCallback;
            successCallback = options;
            options = {};
        }
        options = options || {};

        exec(successCallback, errorCallback, SERVICE_NAME, 'startRecognition', [options]);
    },

    /**
     * Check camera permission status
     * @param {function} successCallback
     * @param {function} errorCallback
     */
    checkPermission: function (successCallback, errorCallback) {
        exec(successCallback, errorCallback, SERVICE_NAME, 'checkPermission', []);
    },

    /**
     * Request camera permission
     * @param {function} successCallback
     * @param {function} errorCallback
     */
    requestPermission: function (successCallback, errorCallback) {
        exec(successCallback, errorCallback, SERVICE_NAME, 'requestPermission', []);
    },

    /**
     * Check camera permission status (alias)
     * @param {function} successCallback
     * @param {function} errorCallback
     */
    checkCameraPermission: function (successCallback, errorCallback) {
        exec(successCallback, errorCallback, SERVICE_NAME, 'checkPermission', []);
    },

    /**
     * Request camera permission (alias)
     * @param {function} successCallback
     * @param {function} errorCallback
     */
    requestCameraPermission: function (successCallback, errorCallback) {
        exec(successCallback, errorCallback, SERVICE_NAME, 'requestPermission', []);
    },

    /**
     * Terminate the SDK and release all resources (AI models, sessions, etc.)
     * After calling terminate(), you must call initialize() again before using other SDK functions.
     * @param {function} successCallback
     * @param {function} errorCallback
     */
    terminate: function (successCallback, errorCallback) {
        _initData = {};
        exec(successCallback, errorCallback, SERVICE_NAME, 'terminate', []);
    },

    /**
     * Check if SDK is initialized
     * @param {function} successCallback
     * @param {function} errorCallback
     */
    isInitialized: function (successCallback, errorCallback) {
        exec(successCallback, errorCallback, SERVICE_NAME, 'isInitialized', []);
    },

    /**
     * Check if the current device is compatible with the Face SDK.
     * Verifies: front camera, OS version, CPU architecture (arm64), RAM, available storage.
     * This should be called BEFORE initialize() to give users early feedback
     * if their device cannot run face recognition.
     *
     * @param {function} successCallback - Called with DeviceCompatibilityResult
     * @param {function} errorCallback
     */
    checkDeviceCompatibility: function (successCallback, errorCallback) {
        exec(successCallback, errorCallback, SERVICE_NAME, 'checkDeviceCompatibility', []);
    }
};

// Also export a Promise-based wrapper for modern JS usage
FaceSDK.promise = {
    initialize: function (options) {
        return new Promise(function (resolve, reject) {
            FaceSDK.initialize(options, resolve, reject);
        });
    },
    isLicenseValid: function () {
        return new Promise(function (resolve, reject) {
            FaceSDK.isLicenseValid(resolve, reject);
        });
    },
    getLicenseInfo: function () {
        return new Promise(function (resolve, reject) {
            FaceSDK.getLicenseInfo(resolve, reject);
        });
    },
    getLicenseStatus: function () {
        return new Promise(function (resolve, reject) {
            FaceSDK.getLicenseStatus(resolve, reject);
        });
    },
    startRegistration: function (options) {
        return new Promise(function (resolve, reject) {
            FaceSDK.startRegistration(options || {}, resolve, reject);
        });
    },
    isUserEnrolled: function (userId) {
        return new Promise(function (resolve, reject) {
            FaceSDK.isUserEnrolled(userId, resolve, reject);
        });
    },
    deleteUser: function (userId) {
        return new Promise(function (resolve, reject) {
            FaceSDK.deleteUser(userId, resolve, reject);
        });
    },
    refreshEmbeddings: function (faceId) {
        return new Promise(function (resolve, reject) {
            FaceSDK.refreshEmbeddings(faceId, resolve, reject);
        });
    },
    startRecognition: function (options) {
        return new Promise(function (resolve, reject) {
            FaceSDK.startRecognition(options || {}, resolve, reject);
        });
    },
    checkPermission: function () {
        return new Promise(function (resolve, reject) {
            FaceSDK.checkPermission(resolve, reject);
        });
    },
    requestPermission: function () {
        return new Promise(function (resolve, reject) {
            FaceSDK.requestPermission(resolve, reject);
        });
    },
    terminate: function () {
        return new Promise(function (resolve, reject) {
            FaceSDK.terminate(resolve, reject);
        });
    },
    isInitialized: function () {
        return new Promise(function (resolve, reject) {
            FaceSDK.isInitialized(resolve, reject);
        });
    },
    checkDeviceCompatibility: function () {
        return new Promise(function (resolve, reject) {
            FaceSDK.checkDeviceCompatibility(resolve, reject);
        });
    }
};

module.exports = FaceSDK;
