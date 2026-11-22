# Jash AI Screen Assistant

An Android live screen-reading companion for accessibility, reading, and study support.

## Live screen reader (new)
- Starts Android MediaProjection after the user approves the system screen-sharing prompt.
- Runs a visible foreground service and persistent Android notification while sharing.
- Samples the shared display and performs on-device ML Kit OCR for printed English text.
- Shows the latest recognized text in the app.
- Two-finger tap on the in-app reader panel reads the latest recognized text aloud using Android Text-to-Speech.
- Stop sharing from the app; no video file is created or saved.

## Other features
- Import a screenshot from the device photo picker.
- Review and edit extracted text.
- Share selected text or an image from another app.
- English/Telugu manual text entry; Telugu-script image OCR is not yet supported.

## Limitations
- The live reader currently extracts printed English text only; it does not yet describe images or recognize Telugu script.
- The two-finger read-aloud gesture currently works inside the app's reader panel, not as a global gesture over other apps.
- Live mode is an OCR/text-reading feature. It does not provide automatic answers to questions detected across other apps.
- The AI question-solving backend still requires deployment and a privately configured OPENAI_API_KEY.
- A Windows companion is not implemented yet.

## Privacy and permissions
Screen capture starts only after the Android system permission prompt is accepted. Android displays its screen-sharing indicator and the app maintains an ongoing foreground notification. The service processes frames temporarily on-device for OCR; it does not save a screen recording. Stop sharing at any time from the app or Android system controls.

## Backend setup
```bash
cd backend
npm install
cp .env.example .env
# Set OPENAI_API_KEY locally; never commit secrets
npm start
```
The API listens on port 8787 by default. For Android Emulator, `http://10.0.2.2:8787` points to the host machine. For a physical phone, use a reachable LAN address or a deployed HTTPS backend.

## Build Android debug APK
Open `android/` in Android Studio with JDK 17 and Android SDK 35, or use GitHub Actions → Android debug APK. The artifact is named `jash-ai-screen-assistant-debug-apk`.
