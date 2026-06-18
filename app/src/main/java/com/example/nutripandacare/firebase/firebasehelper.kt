package com.example.nutripandacare.firebase

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.example.nutripandacare.firebase.FirebaseHelper.uid
import com.example.nutripandacare.firebase.FirebaseHelper.db


object FirebaseHelper {

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val uid get() = auth.currentUser?.uid ?: ""

    // ════════════════════════════════════════════════════════════
    // [A] AUTH & USER MANAGEMENT
    // ════════════════════════════════════════════════════════════

    fun login(
        email: String,
        password: String,
        onSuccess: (uid: String) -> Unit,
        onError: (String) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val user = result.user ?: return@addOnSuccessListener
                checkStatusAkun(user.uid, { onSuccess(user.uid) }, {
                    auth.signOut()
                    onError("Akun kamu dinonaktifkan oleh pengelola. Hubungi admin.")
                }, {
                    auth.signOut()
                    onError("Pendaftaran akun kamu ditolak oleh pengelola.")
                }, { onError(it) })
            }
            .addOnFailureListener { onError(it.message ?: "Login gagal") }
    }

    fun register(
        nama: String,
        email: String,
        noHp: String,
        password: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: return@addOnSuccessListener
                val data = hashMapOf<String, Any>(
                    "nama" to nama,
                    "email" to email,
                    "no_hp" to noHp,
                    "role" to "",
                    "is_verified" to false,
                    "status_akun" to "aktif",
                    "created_at" to Timestamp.now(),
                    "updated_at" to Timestamp.now()
                )
                db.collection("users").document(uid).set(data)
                    .addOnSuccessListener { onSuccess(uid) }
                    .addOnFailureListener { onError(it.message ?: "Gagal simpan data user") }
            }
            .addOnFailureListener { onError(it.message ?: "Registrasi gagal") }
    }

    fun checkStatusAkun(
        uid: String,
        onAktif: () -> Unit,
        onNonAktif: () -> Unit,
        onDitolak: () -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val status = doc.getString("status_akun") ?: "aktif"
                when (status) {
                    "nonaktif" -> onNonAktif()
                    "ditolak"  -> onDitolak()
                    else       -> onAktif()
                }
            }
            .addOnFailureListener { onError(it.message ?: "Gagal cek status akun") }
    }

    fun logout() = auth.signOut()

    fun resetPassword(email: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (email.isBlank()) { onError("Email tidak boleh kosong"); return }
        auth.sendPasswordResetEmail(email.trim())
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Gagal kirim email reset") }
    }

    fun getDataUser(
        uid: String,
        onSuccess: (Map<String, Any?>) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { onSuccess(it.data ?: emptyMap()) }
            .addOnFailureListener { onError(it.message ?: "Gagal ambil data user") }
    }

    fun updateProfileUser(
        updates: Map<String, Any>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (uid.isEmpty()) { onError("User belum login"); return }
        db.collection("users").document(uid)
            .update(updates + mapOf("updated_at" to Timestamp.now()))
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Gagal update profil") }
    }

    fun updateDataUser(
        uid: String,
        dataUpdate: Map<String, Any>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (uid.isEmpty()) { onError("UID tidak valid"); return }
        db.collection("users").document(uid)
            .update(dataUpdate + mapOf("updated_at" to Timestamp.now()))
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Gagal update profil") }
    }

    fun simpanRole(
        uid: String,
        role: String,
        isVerified: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection("users").document(uid).update(
            "role", role,
            "is_verified", isVerified,
            "updated_at", Timestamp.now()
        ).addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Gagal simpan role") }
    }

    fun getRole(uid: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { onSuccess(it.getString("role") ?: "") }
            .addOnFailureListener { onError(it.message ?: "Gagal ambil role") }
    }

    // ════════════════════════════════════════════════════════════
    // [B] ARTIKEL / KONTEN EDUKASI
    // ════════════════════════════════════════════════════════════

    val KATEGORI_ARTIKEL = listOf("Stunting", "Resep Sehat", "Gizi", "Tumbuh Kembang")

    fun tambahArtikel(
        judul: String,
        deskripsi: String,
        isiKonten: String,
        kategori: String,
        thumbnailUrl: String = "",
        menitBaca: Int,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val data = hashMapOf<String, Any>(
            "judul"         to judul,
            "deskripsi"     to deskripsi,
            "isi_konten"    to isiKonten,
            "kategori"      to kategori,
            "thumbnail_url" to thumbnailUrl,
            "menit_baca"    to menitBaca,
            "penulis"       to uid,
            "dibuat_oleh"   to uid,
            "is_published"  to true,
            "waktu_publish" to Timestamp.now()
        )
        db.collection("artikel").add(data)
            .addOnSuccessListener { onSuccess(it.id) }
            .addOnFailureListener { onError(it.message ?: "Gagal publish artikel") }
    }

    fun getArtikelById(
        id: String,
        onSuccess: (Map<String, Any?>) -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection("artikel").document(id).get()
            .addOnSuccessListener {
                if (it.exists()) onSuccess(it.data ?: emptyMap())
                else onError("Data tidak ditemukan")
            }
            .addOnFailureListener { onError(it.message ?: "Error") }
    }

    fun getArtikel(
        kategori: String = "Semua",
        onSuccess: (List<Pair<String, Map<String, Any?>>>) -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection("artikel").whereEqualTo("is_published", true).get()
            .addOnSuccessListener { snap ->
                val list = snap.documents
                    .map { Pair(it.id, it.data ?: emptyMap()) }
                    .filter { kategori == "Semua" || it.second["kategori"] == kategori }
                    .sortedByDescending {
                        it.second["waktu_publish"] as? Timestamp ?: Timestamp.now()
                    }
                onSuccess(list)
            }
            .addOnFailureListener { onError(it.message ?: "Gagal muat artikel") }
    }

    fun getArtikelSaya(
        onSuccess: (List<Pair<String, Map<String, Any?>>>) -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection("artikel").whereEqualTo("dibuat_oleh", uid).get()
            .addOnSuccessListener { snap ->
                val list = snap.documents
                    .map { Pair(it.id, it.data ?: emptyMap()) }
                    .sortedByDescending {
                        it.second["waktu_publish"] as? Timestamp ?: Timestamp.now()
                    }
                onSuccess(list)
            }
            .addOnFailureListener { onError(it.message ?: "Gagal muat artikel saya") }
    }

    fun updateArtikel(
        artikelId: String,
        dataUpdate: Map<String, Any>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection("artikel").document(artikelId).update(dataUpdate)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Gagal update artikel") }
    }

    fun hapusArtikel(artikelId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        db.collection("artikel").document(artikelId).delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Gagal hapus artikel") }
    }

    // ════════════════════════════════════════════════════════════
    // [C] PENGUMUMAN & NOTIFIKASI
    // ════════════════════════════════════════════════════════════

    fun kirimPengumuman(
        judul: String,
        isi: String,
        targetRole: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val data = hashMapOf<String, Any>(
            "judul_pengumuman"  to judul,
            "isi_pengumuman"    to isi,
            "target_role"       to targetRole,
            "is_published"      to true,
            "waktu_pengumuman"  to Timestamp.now(),
            "dibuat_oleh"       to uid
        )
        db.collection("pengumuman").add(data).addOnSuccessListener {
            blastNotifikasi(judul, isi, "pengumuman", targetRole, onSuccess, onError)
        }.addOnFailureListener { onError(it.message ?: "Gagal kirim pesan") }
    }

    fun getPengumumanTerbaru(
        limit: Int = 20,
        onSuccess: (List<Map<String, Any?>>) -> Unit,
        onError: (String) -> Unit
    ) {
        // [FIX] Hapus orderBy untuk hindari composite index requirement.
        // Sort & limit dilakukan di client side.
        db.collection("pengumuman").whereEqualTo("is_published", true)
            .get()
            .addOnSuccessListener { snap ->
                val list = snap.documents
                    .map { it.data ?: emptyMap<String, Any?>() }
                    .sortedByDescending { it["waktu_pengumuman"] as? Timestamp ?: Timestamp.now() }
                    .take(limit)
                onSuccess(list)
            }
            .addOnFailureListener { onError(it.message ?: "Gagal muat pengumuman") }
    }

    fun blastNotifikasi(
        judul: String,
        isi: String,
        tipe: String,
        targetRole: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val query = if (targetRole == "semua") {
            db.collection("users")
        } else {
            db.collection("users").whereEqualTo("role", targetRole)
        }

        query.get().addOnSuccessListener { snap ->
            if (snap.isEmpty) { onSuccess(); return@addOnSuccessListener }

            val aktiveDocs = snap.documents.filter { doc ->
                (doc.getString("status_akun") ?: "aktif") == "aktif"
            }

            if (aktiveDocs.isEmpty()) { onSuccess(); return@addOnSuccessListener }

            val batch = db.batch()
            aktiveDocs.forEach { doc ->
                val ref = db.collection("notifikasi").document()
                batch.set(
                    ref, hashMapOf<String, Any>(
                        "user_id" to doc.id,
                        "judul"   to judul,
                        "isi"     to isi,
                        "tipe"    to tipe,
                        "is_read" to false,
                        "waktu"   to Timestamp.now()
                    )
                )
            }
            batch.commit()
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { onError("Gagal kirim notif: ${it.message}") }

        }.addOnFailureListener { onError("Gagal ambil daftar user: ${it.message}") }
    }

    fun listenNotifikasi(
        onUpdate: (List<Pair<String, Map<String, Any?>>>) -> Unit,
        onError: (String) -> Unit
    ) = db.collection("notifikasi").whereEqualTo("user_id", uid)
        .addSnapshotListener { snap, err ->
            if (err != null) { onError(err.message ?: "Error"); return@addSnapshotListener }
            val list = snap?.documents
                ?.map { Pair(it.id, it.data ?: emptyMap()) }
                ?.sortedByDescending { it.second["waktu"] as? Timestamp ?: Timestamp.now() }
                ?: emptyList()
            onUpdate(list)
        }

    fun tandaiDibaca(id: String) {
        db.collection("notifikasi").document(id).update("is_read", true)
    }

    fun tandaiSemuaDibaca(onSuccess: () -> Unit) {
        db.collection("notifikasi").whereEqualTo("user_id", uid)
            .whereEqualTo("is_read", false)
            .get().addOnSuccessListener { snap ->
                val batch = db.batch()
                snap.documents.forEach { batch.update(it.reference, "is_read", true) }
                batch.commit().addOnSuccessListener { onSuccess() }
            }
    }

    fun getJumlahNotifBelumDibaca(onResult: (Int) -> Unit) {
        db.collection("notifikasi").whereEqualTo("user_id", uid)
            .whereEqualTo("is_read", false)
            .get()
            .addOnSuccessListener { onResult(it.size()) }
            .addOnFailureListener { onResult(0) }
    }

    // ════════════════════════════════════════════════════════════
    // [D] REKAP GIZI & SISWA (GURU)
    // ════════════════════════════════════════════════════════════

    fun simpanRekapGizi(
        sekolah: String, periode: String, totalSiswa: Int, normal: Int,
        giziKurang: Int, giziBuruk: Int, giziLebih: Int, obesitas: Int,
        stunting: Int = 0,
        onSuccess: (String) -> Unit, onError: (String) -> Unit
    ) {
        val data = hashMapOf<String, Any>(
            "sekolah"     to sekolah,
            "guru_uid"    to uid,
            "periode"     to periode,
            "total_siswa" to totalSiswa,
            "normal"      to normal,
            "gizi_kurang" to giziKurang,
            "gizi_buruk"  to giziBuruk,
            "gizi_lebih"  to giziLebih,
            "obesitas"    to obesitas,
            "stunting"    to stunting,
            "created_at"  to Timestamp.now(),
            "updated_at"  to Timestamp.now()
        )
        db.collection("rekap_gizi").add(data)
            .addOnSuccessListener { onSuccess(it.id) }
            .addOnFailureListener { onError(it.message ?: "Gagal simpan rekap") }
    }

    fun tambahDetailSiswa(
        rekapId: String,
        namaSiswa: String,
        kelas: String,
        berat: Double,
        tinggi: Double,
        usiaBulan: Int,
        zScore: Double,
        statusGizi: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val data = hashMapOf<String, Any>(
            "nama_siswa"   to namaSiswa,
            "kelas"        to kelas,
            "berat_badan"  to berat,
            "tinggi_badan" to tinggi,
            "usia_bulan"   to usiaBulan,
            "z_score"      to zScore,
            "status_gizi"  to statusGizi,
            "tanggal_ukur" to Timestamp.now()
        )
        db.collection("rekap_gizi").document(rekapId)
            .collection("detail_siswa")
            .add(data)
            .addOnSuccessListener {
                android.util.Log.d("FirebaseHelper", "detail_siswa berhasil ditambah id=${it.id}")
                onSuccess()
                updateRekapSummary(
                    rekapId   = rekapId,
                    onSuccess = { android.util.Log.d("FirebaseHelper", "updateRekapSummary selesai") },
                    onError   = { errMsg -> android.util.Log.e("FirebaseHelper", "updateRekapSummary gagal: $errMsg") }
                )
            }
            .addOnFailureListener { e ->
                android.util.Log.e("FirebaseHelper", "Gagal add detail_siswa: ${e.message}", e)
                onError(e.message ?: "Gagal simpan siswa")
            }
    }

    fun updateRekapSummary(
        rekapId: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        db.collection("rekap_gizi").document(rekapId)
            .collection("detail_siswa").get()
            .addOnSuccessListener { snap ->
                var total = 0; var normal = 0; var kurang = 0
                var buruk = 0; var lebih  = 0; var obs    = 0; var stunting = 0

                snap.documents.forEach { doc ->
                    total++
                    val status = (doc.getString("status_gizi") ?: "").lowercase().trim()
                    if (status.contains("stunting")) stunting++
                    when {
                        status == "normal"        -> normal++
                        status.contains("kurang") -> kurang++
                        status.contains("buruk")  -> buruk++
                        status.contains("lebih")  -> lebih++
                        status == "obesitas"      -> obs++
                    }
                }

                db.collection("rekap_gizi").document(rekapId).update(
                    mapOf(
                        "total_siswa" to total,
                        "normal"      to normal,
                        "gizi_kurang" to kurang,
                        "gizi_buruk"  to buruk,
                        "gizi_lebih"  to lebih,
                        "obesitas"    to obs,
                        "stunting"    to stunting,
                        "updated_at"  to Timestamp.now()
                    )
                )
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { e -> onError(e.message ?: "Gagal update summary") }
            }
            .addOnFailureListener { e -> onError(e.message ?: "Gagal ambil detail siswa") }
    }

    fun getRekapGizi(
        onSuccess: (List<Pair<String, Map<String, Any?>>>) -> Unit,
        onError: (String) -> Unit
    ) {
        // [FIX] Hapus orderBy untuk hindari kebutuhan composite index.
        // Sort dilakukan di client side.
        db.collection("rekap_gizi").whereEqualTo("guru_uid", uid)
            .get()
            .addOnSuccessListener { snap ->
                val list = snap.documents
                    .map { Pair(it.id, it.data ?: emptyMap()) }
                    .sortedByDescending { it.second["created_at"] as? Timestamp ?: Timestamp.now() }
                onSuccess(list)
            }
            .addOnFailureListener { onError(it.message ?: "Gagal muat rekap") }
    }

    fun getDetailSiswa(
        rekapId: String,
        onSuccess: (List<Map<String, Any?>>) -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection("rekap_gizi").document(rekapId)
            .collection("detail_siswa")
            .orderBy("nama_siswa")
            .get()
            .addOnSuccessListener { snap ->
                onSuccess(snap.documents.map { it.data ?: emptyMap() })
            }
            .addOnFailureListener { onError(it.message ?: "Gagal muat detail siswa") }
    }

    // ════════════════════════════════════════════════════════════
    // [E] KALKULASI GIZI WHO
    // ════════════════════════════════════════════════════════════

    fun hitungZScore(berat: Double, usiaBulan: Int): Double {
        val (median, sd) = when {
            usiaBulan <= 1  -> Pair(4.5,  0.60)
            usiaBulan <= 6  -> Pair(7.9,  0.88)
            usiaBulan <= 12 -> Pair(9.6,  1.03)
            usiaBulan <= 24 -> Pair(12.2, 1.25)
            usiaBulan <= 36 -> Pair(14.3, 1.43)
            usiaBulan <= 48 -> Pair(16.3, 1.63)
            usiaBulan <= 60 -> Pair(18.3, 1.83)
            else            -> Pair(20.0, 2.00)
        }
        return (berat - median) / sd
    }

    fun hitungZScoreTbu(tinggi: Double, usiaBulan: Int): Double {
        val (median, sd) = when {
            usiaBulan <= 6  -> Pair(67.6,  2.4)
            usiaBulan <= 12 -> Pair(75.7,  2.7)
            usiaBulan <= 24 -> Pair(87.8,  3.1)
            usiaBulan <= 36 -> Pair(96.1,  3.5)
            usiaBulan <= 48 -> Pair(103.3, 3.8)
            usiaBulan <= 60 -> Pair(110.0, 4.0)
            else            -> Pair(116.0, 4.2)
        }
        return (tinggi - median) / sd
    }

    fun tentukanStatusGizi(zBbu: Double, zTbu: Double): String {
        val isStunting     = zTbu < -2.0
        val isSevereStunt  = zTbu < -3.0
        return when {
            isSevereStunt && zBbu < -3.0 -> "Stunting - Gizi Buruk"
            isSevereStunt                -> "Stunting Berat"
            isStunting && zBbu < -2.0   -> "Stunting - Gizi Kurang"
            isStunting                   -> "Stunting"
            zBbu < -3.0                  -> "Gizi Buruk"
            zBbu < -2.0                  -> "Gizi Kurang"
            zBbu <= 1.0                  -> "Normal"
            zBbu <= 2.0                  -> "Berisiko Gizi Lebih"
            zBbu <= 3.0                  -> "Gizi Lebih"
            else                         -> "Obesitas"
        }
    }

    fun rekomendasiMakanan(statusGizi: String, usiaBulan: Int): List<String> {
        val s = statusGizi.lowercase()
        return when {
            s.contains("buruk") || s.contains("berat") -> listOf(
                "🥛 Susu formula/ASI intensif",
                "🥚 Telur rebus setiap hari",
                "⚠️ Konsultasi Dokter"
            )
            s.contains("kurang") || s.contains("stunting") -> listOf(
                "🍗 Protein hewani (ikan/ayam)",
                "🥦 Sayuran hijau",
                "🥛 Susu setiap hari"
            )
            s.contains("lebih") || s == "obesitas" -> listOf(
                "🍎 Perbanyak buah & sayur",
                "🏃 Aktivitas fisik rutin",
                "🍚 Kurangi porsi karbo"
            )
            else -> listOf(
                "🍚 Nasi + Lauk bergizi",
                "🥦 Sayuran berwarna",
                "✅ Pertahankan pola makan sehat"
            )
        }
    }

    fun hitungPersentaseNutrisi(zScore: Double): Int {
        return when {
            zScore < -3.0 -> 40
            zScore < -2.0 -> 60
            zScore < 1.0  -> 95
            zScore < 2.0  -> 85
            else          -> 70
        }
    }

    // ════════════════════════════════════════════════════════════
    // [F] MANAJEMEN PENGGUNA (UNTUK PENGELOLA)
    // ════════════════════════════════════════════════════════════

    fun getAllPengguna(
        onSuccess: (List<Pair<String, Map<String, Any?>>>) -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection("users").get()
            .addOnSuccessListener { snap ->
                val list = snap.documents
                    .filter { (it.getString("role") ?: "") != "pengelola" }
                    .map { Pair(it.id, it.data ?: emptyMap()) }
                    .sortedByDescending { it.second["created_at"] as? Timestamp ?: Timestamp.now() }
                onSuccess(list)
            }.addOnFailureListener { onError("Gagal ambil data") }
    }

    fun getPendaftarBaru(
        onSuccess: (List<Pair<String, Map<String, Any?>>>) -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection("users").whereEqualTo("is_verified", false)
            .whereEqualTo("status_akun", "aktif").get()
            .addOnSuccessListener { snap ->
                val list = snap.documents
                    .filter { (it.getString("role") ?: "") != "pengelola" }
                    .map { Pair(it.id, it.data ?: emptyMap()) }
                onSuccess(list)
            }.addOnFailureListener { onError("Gagal ambil data") }
    }

    fun verifikasiAkun(uid: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        db.collection("users").document(uid)
            .update("is_verified", true, "status_akun", "aktif", "updated_at", Timestamp.now())
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError("Gagal verifikasi") }
    }

    fun nonaktifkanAkun(uid: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        db.collection("users").document(uid)
            .update("status_akun", "nonaktif", "updated_at", Timestamp.now())
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError("Gagal nonaktifkan") }
    }

    fun aktifkanAkun(uid: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        db.collection("users").document(uid)
            .update("status_akun", "aktif", "is_verified", true, "updated_at", Timestamp.now())
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError("Gagal aktifkan") }
    }

    fun tolakAkun(uid: String, alasan: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        db.collection("users").document(uid).update(
            "status_akun", "ditolak",
            "alasan_tolak", alasan,
            "is_verified", false,
            "updated_at", Timestamp.now()
        )
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError("Gagal tolak") }
    }

    // ════════════════════════════════════════════════════════════
    // [G] MENU MBG
    // ════════════════════════════════════════════════════════════

    fun simpanMenuMbg(
        tanggal: String,
        tanggalLabel: String,
        namaMenu: String,
        deskripsi: String,
        kalori: Int,
        protein: Int,
        karbo: Int,
        lemak: Int,
        serat: Int,
        persentaseNutrisi: Int,
        fotoMenu: String = "",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val data = hashMapOf<String, Any>(
            "tanggal"             to tanggal,
            "tanggal_label"       to tanggalLabel,
            "nama_menu"           to namaMenu,
            "deskripsi"           to deskripsi,
            "kalori"              to kalori,
            "protein"             to protein,
            "karbo"               to karbo,
            "lemak"               to lemak,
            "serat"               to serat,
            "persentase_nutrisi"  to persentaseNutrisi,
            "foto_menu"           to fotoMenu,
            "dibuat_oleh"         to uid,
            "created_at"          to Timestamp.now()
        )
        db.collection("menu_mbg").document(tanggal).set(data)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError("Gagal simpan menu") }
    }

    fun getMenuHariIni(
        tanggal: String,
        onSuccess: (Map<String, Any?>?) -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection("menu_mbg").document(tanggal).get()
            .addOnSuccessListener { onSuccess(if (it.exists()) it.data else null) }
            .addOnFailureListener { onError("Gagal ambil menu") }
    }

    fun getDaftarMenu(
        start: String,
        end: String,
        onSuccess: (List<Map<String, Any?>>) -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection("menu_mbg")
            .whereGreaterThanOrEqualTo("tanggal", start)
            .whereLessThanOrEqualTo("tanggal", end)
            .get()
            .addOnSuccessListener { snap ->
                onSuccess(snap.documents.map { it.data ?: emptyMap() })
            }
            .addOnFailureListener { onError(it.message ?: "Gagal ambil daftar menu") }
    }

    fun hapusMenuMbg(tanggal: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        db.collection("menu_mbg").document(tanggal).delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Gagal hapus menu") }
    }

    fun updateMenuMbg(
        tanggal: String,
        dataUpdate: Map<String, Any>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection("menu_mbg").document(tanggal).update(dataUpdate)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Gagal update menu") }
    }

    // ════════════════════════════════════════════════════════════
    // [H] ADUAN
    // ════════════════════════════════════════════════════════════

    fun kirimAduan(
        judul: String,
        isi: String,
        kategori: String,
        pengirimNama: String,
        pengirimRole: String,
        fotoAduan: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val data = hashMapOf<String, Any>(
            "judul_aduan"    to judul,
            "isi_aduan"      to isi,
            "kategori_aduan" to kategori,
            "pengirim_nama"  to pengirimNama,
            "pengirim_role"  to pengirimRole,
            "pengirim_uid"   to uid,
            "foto_aduan"     to fotoAduan,
            "status_aduan"   to "menunggu",
            "balasan"        to "",
            "created_at"     to Timestamp.now()
        )
        db.collection("aduan").add(data)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError("Gagal kirim aduan") }
    }

    // [FIX] Hapus orderBy("created_at") untuk hindari kebutuhan composite index
    // (pengirim_uid + created_at). Sort dilakukan di client side.
    fun getAduanSaya(
        onSuccess: (List<Pair<String, Map<String, Any?>>>) -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection("aduan").whereEqualTo("pengirim_uid", uid)
            .get()
            .addOnSuccessListener { snap ->
                val list = snap.documents
                    .map { Pair(it.id, it.data ?: emptyMap()) }
                    .sortedByDescending { it.second["created_at"] as? Timestamp ?: Timestamp.now() }
                onSuccess(list)
            }.addOnFailureListener { onError(it.message ?: "Gagal muat aduan") }
    }

    fun getAllAduan(
        onSuccess: (List<Pair<String, Map<String, Any?>>>) -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection("aduan").orderBy("created_at", Query.Direction.DESCENDING).get()
            .addOnSuccessListener { snap ->
                onSuccess(snap.documents.map { Pair(it.id, it.data ?: emptyMap()) })
            }
            .addOnFailureListener { onError("Gagal muat aduan") }
    }

    fun balasAduan(
        aduanId: String,
        balasan: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection("aduan").document(aduanId)
            .update("balasan", balasan, "status_aduan", "selesai", "dibalas_pada", Timestamp.now())
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError("Gagal balas aduan") }
    }

    // ════════════════════════════════════════════════════════════
    // [I] DATA ANAK & GIZI (ORANG TUA)
    // ════════════════════════════════════════════════════════════

    fun getDataAnak(onSuccess: (String, Map<String, Any?>) -> Unit, onError: (String) -> Unit) {
        if (uid.isEmpty()) { onError("User belum login"); return }
        db.collection("anak").whereEqualTo("orang_tua_uid", uid).get()
            .addOnSuccessListener { snap ->
                if (snap.isEmpty) onError("Data anak belum ada")
                else {
                    val doc = snap.documents[0]
                    onSuccess(doc.id, doc.data ?: emptyMap())
                }
            }.addOnFailureListener { onError(it.message ?: "Gagal ambil data anak") }
    }

    fun getRiwayatGizi(
        anakId: String,
        limit: Int = 20,
        onSuccess: (List<Map<String, Any?>>) -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection("anak").document(anakId).collection("riwayat_gizi")
            .orderBy("tanggal_ukur", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get()
            .addOnSuccessListener { snap ->
                onSuccess(snap.documents.map { it.data ?: emptyMap() })
            }
            .addOnFailureListener { onError(it.message ?: "Gagal ambil riwayat gizi") }
    }

    fun simpanHasilGizi(
        anakId: String,
        berat: Double,
        tinggi: Double,
        usiaBulan: Int,
        zScore: Double,
        statusGizi: String,
        persentaseNutrisi: Int,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val data = hashMapOf<String, Any>(
            "berat_badan"         to berat,
            "tinggi_badan"        to tinggi,
            "usia_bulan"          to usiaBulan,
            "z_score"             to zScore,
            "status_gizi"         to statusGizi,
            "persentase_nutrisi"  to persentaseNutrisi,
            "tanggal_ukur"        to Timestamp.now()
        )
        db.collection("anak").document(anakId).update(data)
            .addOnSuccessListener {
                db.collection("anak").document(anakId).collection("riwayat_gizi").add(data)
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { onError(it.message ?: "Gagal simpan riwayat gizi") }
            }
            .addOnFailureListener { onError(it.message ?: "Gagal update data anak") }
    }

    fun tambahDataAnak(
        namaAnak: String,
        usiaAnak: String,
        usiaBulan: Int,
        jenisKelamin: String,
        sekolahAnak: String,
        kelas: String,
        fotoAnak: String = "",
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (uid.isEmpty()) { onError("User belum login"); return }
        val data = hashMapOf<String, Any>(
            "nama_anak"    to namaAnak,
            "usia_anak"    to usiaAnak,
            "usia_bulan"   to usiaBulan,
            "jenis_kelamin" to jenisKelamin,
            "sekolah_anak" to sekolahAnak,
            "kelas"        to kelas,
            "foto_anak"    to fotoAnak,
            "orang_tua_uid" to uid,
            "status_gizi"  to "Belum dicek",
            "created_at"   to Timestamp.now(),
            "updated_at"   to Timestamp.now()
        )
        db.collection("anak").add(data)
            .addOnSuccessListener { onSuccess(it.id) }
            .addOnFailureListener { onError(it.message ?: "Gagal simpan data anak") }
    }

    fun updateDataAnak(
        anakId: String,
        updates: Map<String, Any>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (anakId.isEmpty()) { onError("ID anak tidak valid"); return }
        db.collection("anak").document(anakId)
            .update(updates + mapOf("updated_at" to Timestamp.now()))
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Gagal update data anak") }
    }
}
