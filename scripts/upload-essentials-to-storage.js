/**
 * Upload essential audio files (zen.mp3, sync.mp3, overdrive.mp3) to Firebase Storage.
 * Run from scripts/: node upload-essentials-to-storage.js
 * Requires: serviceAccountKey.json in scripts/
 */
const admin = require('firebase-admin');
const path = require('path');
const fs = require('fs');

const keyPath = path.join(__dirname, 'serviceAccountKey.json');
if (!fs.existsSync(keyPath)) {
  console.error('Missing serviceAccountKey.json. Place it in scripts/ directory.');
  process.exit(1);
}

const projectId = require(keyPath).project_id;
const storageBucket = `${projectId}.appspot.com`;

admin.initializeApp({
  credential: admin.credential.cert(keyPath),
  storageBucket,
});

const bucket = admin.storage().bucket();

const ESSENTIALS = [
  { local: 'zen.mp3', remote: 'essentials/zen.mp3' },
  { local: 'sync.mp3', remote: 'essentials/sync.mp3' },
  { local: 'overdrive.mp3', remote: 'essentials/overdrive.mp3' },
];

async function uploadEssentials() {
  const essentialsDir = path.join(__dirname, 'essentials');

  for (const { local, remote } of ESSENTIALS) {
    const sourcePath = path.join(essentialsDir, local);

    if (!fs.existsSync(sourcePath)) {
      console.warn(`  Skip ${remote}: file not found at ${sourcePath}`);
      console.warn(`  Place zen.mp3, sync.mp3, overdrive.mp3 in scripts/essentials/ before running.`);
      continue;
    }

    try {
      await bucket.upload(sourcePath, {
        destination: remote,
        metadata: {
          contentType: 'audio/mpeg',
          cacheControl: 'public, max-age=31536000',
        },
      });
      console.log(`  Uploaded: ${remote}`);
    } catch (err) {
      console.error(`  Failed ${remote}:`, err.message);
    }
  }

  console.log('\nDone. Essential files are in Firebase Storage at essentials/');
  process.exit(0);
}

uploadEssentials().catch((err) => {
  console.error(err);
  process.exit(1);
});
