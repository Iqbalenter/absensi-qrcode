// Seed script: membuat akun test (Admin, HRD, Magang) di Firebase Authentication
// beserta dokumen profilnya di koleksi Firestore "users".
//
// Cara pakai:
//   1. Firebase Console > Project settings > Service accounts > Generate new private key
//      Simpan file yang terdownload sebagai serviceAccountKey.json di folder ini (scripts/).
//      JANGAN commit/upload file ini ke mana pun, isinya kredensial admin penuh ke project.
//   2. cd scripts
//   3. npm install
//   4. node seedFirestore.js

const admin = require("firebase-admin");
const serviceAccount = require("./serviceAccountKey.json");

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
});

const auth = admin.auth();
const db = admin.firestore();

const testUsers = [
  { email: "admin@sistemabsensi.test", password: "Admin123!", name: "Admin Utama", role: "ADMIN", nim: "-" },
  { email: "hrd@sistemabsensi.test", password: "Hrd123!", name: "Staff HRD", role: "HRD", nim: "-" },
  { email: "magang1@sistemabsensi.test", password: "Magang123!", name: "Budi Santoso", role: "MAGANG", nim: "2024100012" },
  { email: "magang2@sistemabsensi.test", password: "Magang123!", name: "Siti Aminah", role: "MAGANG", nim: "2024100045" },
];

async function seed() {
  for (const u of testUsers) {
    let userRecord;
    try {
      userRecord = await auth.getUserByEmail(u.email);
      console.log(`Akun sudah ada, dipakai ulang: ${u.email}`);
    } catch (error) {
      userRecord = await auth.createUser({
        email: u.email,
        password: u.password,
        displayName: u.name,
      });
      console.log(`Akun dibuat: ${u.email} / ${u.password}`);
    }

    await db.collection("users").doc(userRecord.uid).set({
      uid: userRecord.uid,
      name: u.name,
      email: u.email,
      role: u.role,
      nim: u.nim,
      status: "aktif",
    });
  }

  console.log("\nSelesai seeding data. Kamu bisa login di app pakai akun-akun di atas.");
  process.exit(0);
}

seed().catch((error) => {
  console.error(error);
  process.exit(1);
});
