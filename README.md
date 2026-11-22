# Jash AI Study Assistant

A permission-based Android study helper for **practice questions, mock tests, and authorized classroom demonstrations**.

## Current Android features
- Import an image from the device's photo picker.
- On-device ML Kit OCR for printed English text.
- Review and edit extracted text before sending it.
- Send a practice question to the backend and display answer, confidence, and verification note.
- Configure the backend URL in the app.
- English/Telugu text entry; Telugu can be pasted manually.

## Not yet implemented
- Telugu-script OCR (the current bundled ML Kit recognizer is Latin-script only).
- Floating overlay or MediaProjection screen capture.
- Windows desktop client.
- Production backend deployment and API-key configuration.
- Signed release APK and physical-device QA.

## Safe-use scope
This app is for self-study, practice/mock tests, and demonstrations explicitly authorized by the institution. It is not designed for covert assistance during active graded exams, automatic exam-option selection, or bypassing proctoring.

## Backend setup
```bash
cd backend
npm install
cp .env.example .env
# Set OPENAI_API_KEY locally; never commit secrets
npm start
```
The API listens on port 8787 by default. For Android Emulator, the default app backend URL `http://10.0.2.2:8787` points to the host machine. For a physical phone, use the computer's reachable LAN address or a deployed HTTPS backend.

## Build Android debug APK
Open `android/` in Android Studio with JDK 17 and Android SDK 35, or use the GitHub Actions workflow under Actions → Android APK → Run workflow. The artifact is named `jash-ai-screen-assistant-debug-apk`.

## Release signing
A production APK must be signed with a keystore controlled by the project owner. Do not commit keystores, passwords, or API keys. Add signing secrets only through a secure CI secret store.
