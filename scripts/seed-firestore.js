/**
 * Heart Sync - Firestore 種子資料腳本
 * 使用方式：在 scripts/ 目錄下執行 node seed-firestore.js
 * 需先將服務帳戶金鑰存成 serviceAccountKey.json（見 docs/FIREBASE_SCRIPTS_STEPS.md）
 */
const admin = require('firebase-admin');
const path = require('path');
const fs = require('fs');

const keyPath = path.join(__dirname, 'serviceAccountKey.json');
if (!fs.existsSync(keyPath)) {
  console.error('找不到 serviceAccountKey.json，請從 Firebase Console 下載並放到 scripts/ 目錄。');
  process.exit(1);
}

admin.initializeApp({ credential: admin.credential.cert(keyPath) });
const db = admin.firestore();

async function seedActivityModes() {
  const modes = [
    { id: 'exercise', name: 'Exercise', description: '120+ BPM', minBpm: 120, maxBpm: 200, baseGenres: [], createdAt: Date.now() },
    { id: 'driving', name: 'Driving', description: '80-120 BPM', minBpm: 80, maxBpm: 120, baseGenres: [], createdAt: Date.now() },
    { id: 'calm', name: 'Calm', description: '50-80 BPM', minBpm: 50, maxBpm: 80, baseGenres: [], createdAt: Date.now() },
  ];
  for (const m of modes) {
    const { id, ...data } = m;
    await db.collection('activityModes').doc(id).set(data);
    console.log('  activityModes:', id);
  }
}

async function seedSongs() {
  const songs = [
    {
      docId: 'sample1',
      title: 'Sample Calm Track',
      artist: 'Heart Sync',
      bpm: 72,
      durationSec: 120,
      genre: 'Ambient',
      tags: ['calm'],
      audioSourceType: 'url',
      audioUrl: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3',
      coverUrl: '',
    },
    {
      docId: 'sample2',
      title: 'Sample Exercise Track',
      artist: 'Heart Sync',
      bpm: 128,
      durationSec: 180,
      genre: 'Pop',
      tags: ['exercise'],
      audioSourceType: 'url',
      audioUrl: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3',
      coverUrl: '',
    },
  ];
  for (const s of songs) {
    const docId = s.docId;
    const { docId: _, ...data } = s;
    await db.collection('songs').doc(docId).set({ ...data, id: docId });
    console.log('  songs:', docId);
  }
}

async function main() {
  console.log('開始寫入 Firestore 種子資料...\n');
  await seedActivityModes();
  console.log('');
  await seedSongs();
  console.log('\n完成。');
  process.exit(0);
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
