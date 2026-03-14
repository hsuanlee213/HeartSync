# Music Playback Setup – 讓 App 播放免費音樂

## 現況

專案已有三套音樂來源：

| 來源 | 用途 | 狀態 |
|------|------|------|
| **Firebase Storage `essentials/`** | ZEN/SYNC/OVERDRIVE 模式離線音檔 | 需執行上傳腳本，App 會快取至本地 |
| **Firestore `songs`** | Music Library 播放 | 需執行種子腳本寫入資料 |
| **Jamendo API** | MusicDiscoveryPlayer（BPM 推薦） | 目前為 Mock，URL 為假資料 |

主畫面的 Play 按鈕與 Terminal 動畫使用的是 **PlayerHolder**，音樂來自 **Music Library**（Firestore）或 **Essential Audio**（Firebase Storage + 本地快取）。

---

## Essential Audio（Firebase Storage + 本地快取）

三個模式（ZEN、SYNC、OVERDRIVE）的測試音檔存放在 Firebase Storage，App 首次播放時下載並快取至本地，之後離線也可播放。

### 上傳 Essential 音檔到 Firebase Storage

1. 建立 `scripts/essentials/`，將 `zen.mp3`、`sync.mp3`、`overdrive.mp3` 放入
2. 執行上傳腳本：

```bash
cd scripts
npm install firebase-admin   # 若尚未安裝
node upload-essentials-to-storage.js
```

3. 在 Firebase Console → Storage 確認 `essentials/` 資料夾下有 3 個 mp3 檔
4. 設定 Storage 規則允許讀取：

```
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /essentials/{allPaths=**} {
      allow read: if true;
    }
  }
}
```

### 播放流程

- **有網路**：從 Firebase Storage 下載 → 快取至 `files/essentials/` → 播放
- **無網路**：若已快取則直接播放；否則無法播放（需先連線下載）

---

## 實作步驟（使用 SoundHelix 免費音樂）

### 1. 準備 Firebase 服務帳戶金鑰

1. 開啟 [Firebase Console](https://console.firebase.google.com/) → 選擇專案
2. 專案設定 → 服務帳戶 → 產生新的私密金鑰
3. 將下載的 JSON 檔命名為 `serviceAccountKey.json`
4. 放到 `scripts/` 目錄

### 2. 執行種子腳本

```bash
cd scripts
npm install firebase-admin   # 若尚未安裝
node seed-firestore.js
```

腳本會寫入兩首 SoundHelix 免費音樂到 Firestore：

- `songs/sample1`: Calm Track (72 BPM)  
  `https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3`
- `songs/sample2`: Exercise Track (128 BPM)  
  `https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3`

### 3. 設定 Firestore 安全規則

在 Firebase Console → Firestore → 規則，確保可讀取 `songs`：

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /songs/{songId} {
      allow read: if true;   // 或依需求限制為已登入使用者
    }
  }
}
```

### 4. 在 App 中播放

1. 登入 App
2. 主畫面 Toolbar → 點選 **Library**（或對應的音樂庫入口）
3. 在 Music Library 中點選任一曲目
4. 音樂會開始播放，並同步到主畫面的 PlayerHolder
5. 回到主畫面後，Play/Pause 會控制同一首音樂，Terminal 動畫會依 `isMusicPlaying` 切換

---

## 若想改用 Jamendo API（真實 API）

1. 到 [Jamendo API](https://developer.jamendo.com/) 註冊並取得 `client_id`
2. 修改 `JamendoApiService.kt`，將 `fetchTracks()` 改為實際呼叫：

   ```
   https://api.jamendo.com/v3.0/tracks/?client_id=YOUR_CLIENT_ID&format=json&limit=200
   ```

3. 解析 JSON，將 `audio` 欄位對應到 `Song.audioUrl`
4. 注意：Jamendo 不提供 BPM，需自行估算或使用其他欄位

---

## 快速檢查清單

- [ ] `scripts/serviceAccountKey.json` 已存在
- [ ] 已執行 `node upload-essentials-to-storage.js`（Essential 音檔）
- [ ] 已執行 `node seed-firestore.js`（Music Library）
- [ ] Firebase Storage 規則允許讀取 `essentials/`
- [ ] Firestore 規則允許讀取 `songs`
- [ ] 已登入 App
- [ ] 已從 Music Library 選擇並播放一首歌
