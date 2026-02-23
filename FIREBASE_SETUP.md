# Firebase 獨立專案設定說明

專案已改為新包名 `com.heartbeatmusic`，建議使用**新的 Firebase 專案**，與原 group project 完全分離（權限與資料獨立）。

## 步驟

### 1. 建立新 Firebase 專案

1. 前往 [Firebase Console](https://console.firebase.google.com/)
2. 點「新增專案」，命名（例如 `heartbeat-music`）
3. 依需要啟用 Google Analytics（可選）

### 2. 在專案中新增 Android 應用

1. 在該 Firebase 專案中點「新增應用」→ 選 Android
2. **Android 套件名稱**請填：`com.heartbeatmusic`（必須與本專案一致）
3. 其餘欄位（App 暱稱、Debug signing certificate）可選填
4. 下載產生的 `google-services.json`

### 3. 替換專案內的設定檔

- 將下載的 `google-services.json` **覆蓋**專案中的：
  - `app/google-services.json`

### 4. 在 Firebase 中啟用服務

依你目前使用的功能，在 Firebase 專案中開啟：

- **Authentication**：Email/Password（登入、註冊）
- **Firestore Database**：建立與舊專案相同的 collections 結構（例如 `users`, `songs`, `history`, `activityModes` 等）及安全規則
- **Storage**：若有用到上傳/下載檔案

### 5. 資料遷移（可選）

若需要把舊專案的部分資料搬到新專案，可用 Firestore 匯出/匯入或自行寫腳本複製；多數情況獨立出來會選擇**從空資料開始**。

---

完成以上步驟後，Build 並執行 app，即會連到新的 Firebase 專案。
