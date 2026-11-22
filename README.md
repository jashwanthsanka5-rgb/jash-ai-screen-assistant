# Jash AI Screen Assistant

A permission-based study and practice-question assistant for Android and Windows.

## Scope
- Practice/mock questions and college-authorized demonstrations only.
- User-initiated screenshot import or screen capture with visible Android permission.
- OCR question text, including English and Telugu as language packs are configured.
- Answer-only output for practice questions (MCQ letter or numerical value).
- Backend answer verification; uncertain answers must be marked as uncertain.
- No covert live-exam answer assistance, proctoring bypass, or automatic exam-option selection.

## Project layout
- `android/` Android app scaffold (Kotlin + Jetpack Compose)
- `backend/` Node.js API scaffold
- `windows/` Windows desktop scaffold (planned)

## Backend setup
```bash
cd backend
npm install
cp .env.example .env
# Set OPENAI_API_KEY in .env; never commit secrets
npm start
```

## Android
Open the `android` folder in Android Studio, install the Android SDK, then build the debug APK. The current Android project is a starter and must be tested on a physical device before use.

## Important
This repository is a development scaffold, not a signed production APK. Do not commit signing keys, API keys, or passwords. A signed release requires a keystore owned and secured by the project owner.
