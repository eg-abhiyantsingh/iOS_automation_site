//
//  SpeechRecognitionManager.swift
//  Egalvanic PZ
//
//  Wraps SFSpeechRecognizer + AVAudioEngine for voice-to-text input
//

import Foundation
import Speech
import AVFoundation

@Observable
class SpeechRecognitionManager {
    var isRecording = false
    var transcribedText = ""
    var isAuthorized = false
    var isAvailable = false

    private var speechRecognizer: SFSpeechRecognizer?
    private var recognitionRequest: SFSpeechAudioBufferRecognitionRequest?
    private var recognitionTask: SFSpeechRecognitionTask?
    private let audioEngine = AVAudioEngine()
    private var isStopping = false

    init() {
        let localeId = LanguageManager.shared.currentLanguage == .french ? "fr-FR" : "en-US"
        speechRecognizer = SFSpeechRecognizer(locale: Locale(identifier: localeId))
        isAvailable = speechRecognizer?.isAvailable ?? false
    }

    func requestAuthorization() {
        SFSpeechRecognizer.requestAuthorization { [weak self] status in
            DispatchQueue.main.async {
                switch status {
                case .authorized:
                    self?.isAuthorized = true
                default:
                    self?.isAuthorized = false
                }
            }
        }

        AVAudioApplication.requestRecordPermission { [weak self] granted in
            DispatchQueue.main.async {
                if !granted {
                    self?.isAuthorized = false
                }
            }
        }
    }

    func toggleRecording() {
        if isRecording {
            stopRecording()
        } else {
            startRecording()
        }
    }

    func startRecording() {
        // Stop any existing session first
        if isRecording || audioEngine.isRunning {
            stopRecording()
        }

        guard let speechRecognizer, speechRecognizer.isAvailable else { return }

        // Clear previous transcription
        transcribedText = ""
        isStopping = false

        let request = SFSpeechAudioBufferRecognitionRequest()
        request.shouldReportPartialResults = true
        recognitionRequest = request

        do {
            let audioSession = AVAudioSession.sharedInstance()
            try audioSession.setCategory(.record, mode: .measurement, options: .duckOthers)
            try audioSession.setActive(true, options: .notifyOthersOnDeactivation)
        } catch {
            AppLogger.log(.error, "Audio session setup failed: \(error)", category: .general)
            return
        }

        let inputNode = audioEngine.inputNode
        let recordingFormat = inputNode.outputFormat(forBus: 0)

        inputNode.installTap(onBus: 0, bufferSize: 1024, format: recordingFormat) { buffer, _ in
            request.append(buffer)
        }

        audioEngine.prepare()
        do {
            try audioEngine.start()
        } catch {
            AppLogger.log(.error, "Audio engine failed to start: \(error)", category: .general)
            return
        }

        recognitionTask = speechRecognizer.recognitionTask(with: request) { [weak self] result, error in
            guard let self, !self.isStopping else { return }

            if let result {
                DispatchQueue.main.async {
                    self.transcribedText = result.bestTranscription.formattedString
                }
            }

            if error != nil || (result?.isFinal ?? false) {
                DispatchQueue.main.async {
                    self.stopRecording()
                }
            }
        }

        isRecording = true
    }

    func stopRecording() {
        guard !isStopping else { return }
        isStopping = true

        if audioEngine.isRunning {
            audioEngine.stop()
            audioEngine.inputNode.removeTap(onBus: 0)
        }
        recognitionRequest?.endAudio()
        recognitionRequest = nil
        recognitionTask?.cancel()
        recognitionTask = nil
        isRecording = false
    }
}
