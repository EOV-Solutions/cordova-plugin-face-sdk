//
//  CDVFaceSDK.swift
//  cordova-plugin-face-sdk
//
//  Cordova bridge for FaceSDK
//  Wraps FMFaceSDK from FaceSDK.xcframework
//
//  Copyright © 2024 EOV Solutions. All rights reserved.
//

import Foundation
import UIKit
import FaceSDK
import AVFoundation

@objc(CDVFaceSDK)
class CDVFaceSDK: CDVPlugin {

    // MARK: - Properties

    private let sdk = FMFaceSDK.shared
    private let licenseManager = FMLicenseManager.shared

    // MARK: - Initialize

    /// Initialize the SDK with license key
    /// Two-step process:
    /// 1. Activate license
    /// 2. Initialize engine
    @objc func initialize(_ command: CDVInvokedUrlCommand) {
        guard let options = command.arguments.first as? [String: Any],
              let licenseKey = options["licenseKey"] as? String else {
            let result = CDVPluginResult(status: CDVCommandStatus.error,
                                         messageAs: ["code": "E_INVALID_PARAMS", "message": "licenseKey is required"])
            commandDelegate.send(result, callbackId: command.callbackId)
            return
        }

        let faceId = options["faceId"] as? String
        let userName = options["userName"] as? String
        let orgId = options["orgId"] as? String

        // Set userName to LicenseManager before license activation
        if let name = userName {
            licenseManager.setUserName(name)
        }

        // Step 1: Activate license
        licenseManager.activate(licenseKey: licenseKey, faceId: faceId) { [weak self] licenseResult in
            guard let self = self else { return }

            switch licenseResult {
            case .success:
                // Step 2: Initialize engine
                self.sdk.initializeEngine { engineResult in
                    switch engineResult {
                    case .success:
                        // Set active organization from license
                        if let licenseOrgId = self.licenseManager.currentOrgId {
                            self.sdk.setOrganization(licenseOrgId) { orgResult in
                                if let org = orgId {
                                    self.sdk.setOrganization(org) { _ in
                                        if let fId = faceId {
                                            self.sdk.autoSyncFaceData(faceId: fId)
                                        }
                                        let result = CDVPluginResult(status: CDVCommandStatus.ok,
                                                                      messageAs: ["success": true, "message": "SDK initialized successfully"])
                                        self.commandDelegate.send(result, callbackId: command.callbackId)
                                    }
                                } else {
                                    if let fId = faceId {
                                        self.sdk.autoSyncFaceData(faceId: fId)
                                    }
                                    let result = CDVPluginResult(status: CDVCommandStatus.ok,
                                                                  messageAs: ["success": true, "message": "SDK initialized successfully"])
                                    self.commandDelegate.send(result, callbackId: command.callbackId)
                                }
                            }
                        } else if let org = orgId {
                            self.sdk.setOrganization(org) { _ in
                                if let fId = faceId {
                                    self.sdk.autoSyncFaceData(faceId: fId)
                                }
                                let result = CDVPluginResult(status: CDVCommandStatus.ok,
                                                              messageAs: ["success": true, "message": "SDK initialized successfully"])
                                self.commandDelegate.send(result, callbackId: command.callbackId)
                            }
                        } else {
                            if let fId = faceId {
                                self.sdk.autoSyncFaceData(faceId: fId)
                            }
                            let result = CDVPluginResult(status: CDVCommandStatus.ok,
                                                          messageAs: ["success": true, "message": "SDK initialized successfully"])
                            self.commandDelegate.send(result, callbackId: command.callbackId)
                        }
                    case .failure(let error):
                        let result = CDVPluginResult(status: CDVCommandStatus.error,
                                                      messageAs: ["code": "E_INIT_FAILED", "message": error.localizedDescription])
                        self.commandDelegate.send(result, callbackId: command.callbackId)
                    }
                }
            case .failure(let error):
                let result = CDVPluginResult(status: CDVCommandStatus.error,
                                              messageAs: ["code": "E_LICENSE_FAILED", "message": error.localizedDescription])
                self.commandDelegate.send(result, callbackId: command.callbackId)
            }
        }
    }

    // MARK: - License

    /// Check if license is valid
    @objc func isLicenseValid(_ command: CDVInvokedUrlCommand) {
        let valid = sdk.isLicenseValid
        let status = sdk.licenseStatus
        let message = getLicenseStatusMessage(status)

        let result = CDVPluginResult(status: CDVCommandStatus.ok,
                                      messageAs: ["valid": valid, "status": status.rawValue, "message": message])
        commandDelegate.send(result, callbackId: command.callbackId)
    }

    /// Get license info
    @objc func getLicenseInfo(_ command: CDVInvokedUrlCommand) {
        let status = sdk.licenseStatus
        let result = CDVPluginResult(status: CDVCommandStatus.ok,
                                      messageAs: [
                                          "isValid": sdk.isLicenseValid,
                                          "status": status.rawValue,
                                          "message": getLicenseStatusMessage(status)
                                      ] as [String : Any])
        commandDelegate.send(result, callbackId: command.callbackId)
    }

    /// Get license status code
    @objc func getLicenseStatus(_ command: CDVInvokedUrlCommand) {
        let result = CDVPluginResult(status: CDVCommandStatus.ok,
                                      messageAs: sdk.licenseStatus.rawValue)
        commandDelegate.send(result, callbackId: command.callbackId)
    }

    // MARK: - Terminate & Cleanup

    /// Terminate the SDK and release all resources
    @objc func terminate(_ command: CDVInvokedUrlCommand) {
        sdk.terminate()
        let result = CDVPluginResult(status: CDVCommandStatus.ok,
                                      messageAs: ["success": true, "message": "SDK terminated successfully"])
        commandDelegate.send(result, callbackId: command.callbackId)
    }

    /// Check if SDK is initialized
    @objc func isInitialized(_ command: CDVInvokedUrlCommand) {
        let result = CDVPluginResult(status: CDVCommandStatus.ok,
                                      messageAs: sdk.isInitialized)
        commandDelegate.send(result, callbackId: command.callbackId)
    }

    private func getLicenseStatusMessage(_ status: FMLicenseStatus) -> String {
        switch status {
        case .notInitialized:
            return "License not initialized"
        case .valid:
            return "License is valid"
        case .expired:
            return "License has expired"
        case .gracePeriod:
            return "License in grace period"
        case .invalid:
            return "Invalid license"
        case .blocked:
            return "License blocked"
        case .quotaExceeded:
            return "Quota exceeded"
        @unknown default:
            return "Unknown license status"
        }
    }

    // MARK: - Registration

    /// Start face registration flow
    @objc func startRegistration(_ command: CDVInvokedUrlCommand) {
        guard sdk.isInitialized else {
            let result = CDVPluginResult(status: CDVCommandStatus.error,
                                          messageAs: ["code": "E_NOT_INITIALIZED", "message": "SDK not initialized"])
            commandDelegate.send(result, callbackId: command.callbackId)
            return
        }

        let options = command.arguments.first as? [String: Any] ?? [:]
        let userId = options["userId"] as? String
        let userName = options["userName"] as? String
        let orgId = options["orgId"] as? String

        // Check camera permission before opening camera
        let cameraStatus = AVCaptureDevice.authorizationStatus(for: .video)
        switch cameraStatus {
        case .denied, .restricted:
            let result = CDVPluginResult(status: CDVCommandStatus.error,
                                          messageAs: ["code": "E_PERMISSION", "message": "Camera permission not granted"])
            commandDelegate.send(result, callbackId: command.callbackId)
            return
        case .notDetermined:
            AVCaptureDevice.requestAccess(for: .video) { [weak self] granted in
                if granted {
                    self?.startRegistration(command)
                } else {
                    DispatchQueue.main.async {
                        let result = CDVPluginResult(status: CDVCommandStatus.error,
                                                      messageAs: ["code": "E_PERMISSION", "message": "Camera permission denied by user"])
                        self?.commandDelegate.send(result, callbackId: command.callbackId)
                    }
                }
            }
            return
        default:
            break
        }

        // Set user info to LicenseManager
        if let name = userName, !name.isEmpty {
            FMLicenseManager.shared.setUserName(name)
        }

        // Set organization if provided
        if let org = orgId {
            sdk.setOrganization(org) { _ in }
        }

        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }

            guard let topVC = self.getTopViewController() else {
                let result = CDVPluginResult(status: CDVCommandStatus.error,
                                              messageAs: ["code": "E_NO_ACTIVITY", "message": "No view controller available"])
                self.commandDelegate.send(result, callbackId: command.callbackId)
                return
            }

            let registerVC = FMMultiStepRegisterViewController()
            registerVC.userName = userName
            registerVC.userId = userId
            if let mode = options["mode"] as? String {
                registerVC.registrationMode = mode
            }

            registerVC.onComplete = { embeddings in
                let result = CDVPluginResult(status: CDVCommandStatus.ok,
                                              messageAs: [
                                                  "success": true,
                                                  "userId": userId ?? "",
                                                  "userName": userName ?? "",
                                                  "orgId": orgId ?? "",
                                                  "featureCount": embeddings.count,
                                                  "serverSynced": true
                                              ] as [String : Any])
                self.commandDelegate.send(result, callbackId: command.callbackId)
            }

            registerVC.onDismiss = {
                let result = CDVPluginResult(status: CDVCommandStatus.ok,
                                              messageAs: [
                                                  "success": false,
                                                  "error": "Registration cancelled"
                                              ])
                self.commandDelegate.send(result, callbackId: command.callbackId)
            }

            let nav = UINavigationController(rootViewController: registerVC)
            nav.modalPresentationStyle = .fullScreen
            topVC.present(nav, animated: true)
        }
    }

    /// Check if user is enrolled
    @objc func isUserEnrolled(_ command: CDVInvokedUrlCommand) {
        guard let userId = command.arguments.first as? String else {
            let result = CDVPluginResult(status: CDVCommandStatus.error,
                                          messageAs: ["code": "E_INVALID_PARAMS", "message": "userId is required"])
            commandDelegate.send(result, callbackId: command.callbackId)
            return
        }

        let result = CDVPluginResult(status: CDVCommandStatus.ok,
                                      messageAs: sdk.isUserEnrolled(userId))
        commandDelegate.send(result, callbackId: command.callbackId)
    }

    /// Delete user
    @objc func deleteUser(_ command: CDVInvokedUrlCommand) {
        guard let userId = command.arguments.first as? String else {
            let result = CDVPluginResult(status: CDVCommandStatus.error,
                                          messageAs: ["code": "E_INVALID_PARAMS", "message": "userId is required"])
            commandDelegate.send(result, callbackId: command.callbackId)
            return
        }

        let result = CDVPluginResult(status: CDVCommandStatus.ok,
                                      messageAs: sdk.deleteUser(userId))
        commandDelegate.send(result, callbackId: command.callbackId)
    }

    // MARK: - Refresh Embeddings

    /// Refresh embeddings: Delete local data and re-download from server
    @objc func refreshEmbeddings(_ command: CDVInvokedUrlCommand) {
        guard sdk.isInitialized else {
            let result = CDVPluginResult(status: CDVCommandStatus.error,
                                          messageAs: ["code": "E_NOT_INITIALIZED", "message": "SDK not initialized"])
            commandDelegate.send(result, callbackId: command.callbackId)
            return
        }

        guard let faceId = command.arguments.first as? String else {
            let result = CDVPluginResult(status: CDVCommandStatus.error,
                                          messageAs: ["code": "E_INVALID_PARAMS", "message": "faceId is required"])
            commandDelegate.send(result, callbackId: command.callbackId)
            return
        }

        sdk.refreshEmbeddings(faceId: faceId) { [weak self] refreshResult in
            guard let self = self else { return }
            DispatchQueue.main.async {
                switch refreshResult {
                case .success(let data):
                    let result = CDVPluginResult(status: CDVCommandStatus.ok,
                                                  messageAs: [
                                                      "success": true,
                                                      "deletedCount": data.deletedCount,
                                                      "userId": data.userId,
                                                      "userName": data.userName,
                                                      "message": "Embeddings refreshed successfully"
                                                  ] as [String : Any])
                    self.commandDelegate.send(result, callbackId: command.callbackId)
                case .failure(let error):
                    let result = CDVPluginResult(status: CDVCommandStatus.error,
                                                  messageAs: ["code": "E_REFRESH_FAILED", "message": error.localizedDescription])
                    self.commandDelegate.send(result, callbackId: command.callbackId)
                }
            }
        }
    }

    // MARK: - Recognition

    /// Start face recognition flow
    @objc func startRecognition(_ command: CDVInvokedUrlCommand) {
        guard sdk.isInitialized else {
            let result = CDVPluginResult(status: CDVCommandStatus.error,
                                          messageAs: ["code": "E_NOT_INITIALIZED", "message": "SDK not initialized"])
            commandDelegate.send(result, callbackId: command.callbackId)
            return
        }

        // Check camera permission before opening camera
        let cameraStatus = AVCaptureDevice.authorizationStatus(for: .video)
        switch cameraStatus {
        case .denied, .restricted:
            let result = CDVPluginResult(status: CDVCommandStatus.error,
                                          messageAs: ["code": "E_PERMISSION", "message": "Camera permission not granted"])
            commandDelegate.send(result, callbackId: command.callbackId)
            return
        case .notDetermined:
            AVCaptureDevice.requestAccess(for: .video) { [weak self] granted in
                if granted {
                    self?.startRecognition(command)
                } else {
                    DispatchQueue.main.async {
                        let result = CDVPluginResult(status: CDVCommandStatus.error,
                                                      messageAs: ["code": "E_PERMISSION", "message": "Camera permission denied by user"])
                        self?.commandDelegate.send(result, callbackId: command.callbackId)
                    }
                }
            }
            return
        default:
            break
        }

        let options = command.arguments.first as? [String: Any] ?? [:]
        let timeoutSeconds = options["timeoutSeconds"] as? Int ?? 30
        let orgId = options["orgId"] as? String

        // Set organization if provided
        if let org = orgId {
            sdk.setOrganization(org) { _ in }
        }

        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }

            guard let topVC = self.getTopViewController() else {
                let result = CDVPluginResult(status: CDVCommandStatus.error,
                                              messageAs: ["code": "E_NO_ACTIVITY", "message": "No view controller available"])
                self.commandDelegate.send(result, callbackId: command.callbackId)
                return
            }

            let recognitionVC = FMLiveRecognitionViewController()
            recognitionVC.timeoutSeconds = timeoutSeconds
            recognitionVC.recognitionMode = options["mode"] as? String ?? "verify"
            recognitionVC.verifyUserId = options["userId"] as? String

            recognitionVC.onResult = { recResult in
                var imageBase64 = ""
                if let path = recResult.imagePath,
                   let data = try? Data(contentsOf: URL(fileURLWithPath: path)) {
                    imageBase64 = "data:image/jpeg;base64," + data.base64EncodedString()
                }

                let result = CDVPluginResult(status: CDVCommandStatus.ok,
                                              messageAs: [
                                                  "success": true,
                                                  "isLive": recResult.isLive,
                                                  "isRecognized": recResult.isRecognized,
                                                  "userId": recResult.userId ?? "",
                                                  "userName": recResult.userName,
                                                  "confidence": recResult.confidence,
                                                  "imagePath": recResult.imagePath ?? "",
                                                  "imageBase64": imageBase64
                                              ] as [String : Any])
                self.commandDelegate.send(result, callbackId: command.callbackId)
            }

            recognitionVC.onDismiss = {
                let result = CDVPluginResult(status: CDVCommandStatus.ok,
                                              messageAs: [
                                                  "success": false,
                                                  "isLive": false,
                                                  "isRecognized": false,
                                                  "error": "Recognition cancelled or timeout"
                                              ])
                self.commandDelegate.send(result, callbackId: command.callbackId)
            }

            let nav = UINavigationController(rootViewController: recognitionVC)
            nav.modalPresentationStyle = .fullScreen
            topVC.present(nav, animated: true)
        }
    }

    // MARK: - Permissions

    /// Check camera permission
    @objc func checkPermission(_ command: CDVInvokedUrlCommand) {
        let status = AVCaptureDevice.authorizationStatus(for: .video)
        var statusString: String
        var granted: Bool

        switch status {
        case .authorized:
            statusString = "granted"
            granted = true
        case .denied:
            statusString = "denied"
            granted = false
        case .restricted:
            statusString = "restricted"
            granted = false
        case .notDetermined:
            statusString = "undetermined"
            granted = false
        @unknown default:
            statusString = "undetermined"
            granted = false
        }

        let result = CDVPluginResult(status: CDVCommandStatus.ok,
                                      messageAs: ["granted": granted, "status": statusString])
        commandDelegate.send(result, callbackId: command.callbackId)
    }

    /// Request camera permission
    @objc func requestPermission(_ command: CDVInvokedUrlCommand) {
        AVCaptureDevice.requestAccess(for: .video) { [weak self] granted in
            DispatchQueue.main.async {
                let result = CDVPluginResult(status: CDVCommandStatus.ok,
                                              messageAs: [
                                                  "granted": granted,
                                                  "status": granted ? "granted" : "denied"
                                              ])
                self?.commandDelegate.send(result, callbackId: command.callbackId)
            }
        }
    }

    // MARK: - Device Compatibility

    /// Check if the current device is compatible with Face SDK
    @objc func checkDeviceCompatibility(_ command: CDVInvokedUrlCommand) {
        var unsupportedReasons: [String] = []

        // 1. Check front camera
        let hasFrontCamera = checkFrontCamera()
        if !hasFrontCamera {
            unsupportedReasons.append("No front-facing camera detected")
        }

        // 2. Check OS version (iOS 12+)
        let osVersion = UIDevice.current.systemVersion
        let versionComponents = osVersion.split(separator: ".").compactMap { Int($0) }
        let majorVersion = versionComponents.first ?? 0
        let osVersionSupported = majorVersion >= 12
        if !osVersionSupported {
            unsupportedReasons.append("iOS \(osVersion) is below minimum (iOS 12.0)")
        }

        // 3. Check CPU architecture (arm64 required for CoreML)
        var cpuArchSupported = false
        #if arch(arm64)
        cpuArchSupported = true
        #else
        unsupportedReasons.append("CPU architecture is not arm64")
        #endif

        // 4. Check RAM (>= 2GB)
        let totalRAMBytes = ProcessInfo.processInfo.physicalMemory
        let totalRAMMB = Int(totalRAMBytes / (1024 * 1024))
        let hasEnoughRAM = totalRAMMB >= 2048
        if !hasEnoughRAM {
            unsupportedReasons.append("Insufficient RAM: \(totalRAMMB)MB (minimum 2048MB)")
        }

        // 5. Check available storage (>= 100MB)
        let availableStorageMB = getAvailableStorageMB()
        let hasEnoughStorage = availableStorageMB >= 100
        if !hasEnoughStorage {
            unsupportedReasons.append("Insufficient storage: \(availableStorageMB)MB available (minimum 100MB)")
        }

        // Device model
        let deviceModel = getDeviceModel()

        // Overall compatibility
        let compatible = hasFrontCamera && osVersionSupported && cpuArchSupported && hasEnoughRAM && hasEnoughStorage

        let message = compatible
            ? "Device is compatible with Face SDK"
            : "Device is NOT compatible: \(unsupportedReasons.joined(separator: "; "))"

        let resultData: [String: Any] = [
            "compatible": compatible,
            "message": message,
            "platform": "ios",
            "osVersion": osVersion,
            "deviceModel": deviceModel,
            "checks": [
                "hasFrontCamera": hasFrontCamera,
                "osVersionSupported": osVersionSupported,
                "cpuArchSupported": cpuArchSupported,
                "hasEnoughRAM": hasEnoughRAM,
                "hasEnoughStorage": hasEnoughStorage
            ],
            "totalRAM": totalRAMMB,
            "availableStorage": availableStorageMB,
            "unsupportedReasons": unsupportedReasons
        ]

        let result = CDVPluginResult(status: CDVCommandStatus.ok, messageAs: resultData)
        commandDelegate.send(result, callbackId: command.callbackId)
    }

    private func checkFrontCamera() -> Bool {
        let discoverySession = AVCaptureDevice.DiscoverySession(
            deviceTypes: [.builtInWideAngleCamera],
            mediaType: .video,
            position: .front
        )
        return !discoverySession.devices.isEmpty
    }

    private func getAvailableStorageMB() -> Int {
        do {
            let attrs = try FileManager.default.attributesOfFileSystem(forPath: NSHomeDirectory())
            if let freeSize = attrs[.systemFreeSize] as? Int64 {
                return Int(freeSize / (1024 * 1024))
            }
        } catch {
            // Fall back to 0 if check fails
        }
        return 0
    }

    private func getDeviceModel() -> String {
        var systemInfo = utsname()
        uname(&systemInfo)
        let machineMirror = Mirror(reflecting: systemInfo.machine)
        let identifier = machineMirror.children.reduce("") { id, element in
            guard let value = element.value as? Int8, value != 0 else { return id }
            return id + String(UnicodeScalar(UInt8(value)))
        }
        return identifier
    }

    // MARK: - Helper

    /// Get the topmost presented view controller (matches RN getTopViewController behavior)
    private func getTopViewController() -> UIViewController? {
        var topVC: UIViewController? = self.viewController
        while let presented = topVC?.presentedViewController {
            topVC = presented
        }
        return topVC
    }
}
