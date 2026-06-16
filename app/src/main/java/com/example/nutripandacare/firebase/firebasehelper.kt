package com.example.nutripandacare.firebase

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

// ════════════════════════════════════════════════════════════════
// OBJECT SINGLETON — akses dari mana saja di project
// ════════════════════════════════════════════════════════════════
object FirebaseHelper {

    val auth = FirebaseAuth.getInstance()
    val db   = FirebaseFirestore.getInstance()
    val uid  get() = auth.currentUser?.uid ?: ""

    // ════════════════════════════════════════════════════════════
    // [A] AUTH — Login, Register, Logout
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
                // Cek status akun setelah login berhasil (UAT: nonaktif tidak bisa login)
                checkStatusAkun(user.uid,
                    onAktif  = { onSuccess(user.uid) },
                    onNonAktif = {
                        auth.signOut()
                        onError("Akun kamu dinonaktifkan oleh pengelola. Hubungi admin.")
                    },
                    onDitolak = {
                        auth.signOut()
                        onError("Pendaftaran akun kamu ditolak oleh pengelola.")
                    },
                    onError = { onError(it) }
                )
            }
            .addOnFailureListener { onError(it.message ?: "Login gagal") }
    }

    /**
     * Cek status akun user dari Firestore.
     * Dipakai setelah login berhasil untuk cegah user nonaktif/ditolak masuk.
     * UAT D-3: "pengguna tidak dapat login" setelah dinonaktifkan.
     */
    fun checkStatusAkun(
        uid: String,
        onAktif: () -> Unit,
        onNonAktif: () -> Unit,
        onDitolak: () -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val statusAkun  = doc.getString("status_akun") ?: "aktif"
                val isVerified  = doc.getBoolean("is_verified") ?: false
                val role        = doc.getString("role") ?: ""

                when {
                    statusAkun == "nonaktif" -> onNonAktif()
                    statusAkun == "ditolak"  -> onDitolak()
                    else -> onAktif()
                }
            }
            .addOnFailureListener { onError(it.message ?: "Gagal cek status akun") }
    }

    fun register(
        nama: String,
        email: String,
        noHp: String,
        password: String,
        onSuccess: (uid: String) -> Unit,
        onError: (String) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid  = result.user?.uid ?: return@addOnSuccessListener
                val data = hashMapOf(
                    "nama"        to nama,
                    "email"       to email,
                    "no_hp"       to noHp,
                    "role"        to "",
                    "foto_url"    to "",
                    "is_verified" to false,
                    "status_akun" to "aktif",
                    "created_at"  to Timestamp.now(),
                    "updated_at"  to Timestamp.now()
                )
                db.collection("users").document(uid).set(data)
                    .addOnSuccessListener { onSuccess(uid) }
                    .addOnFailureListener { onError(it.message ?: "Gagal simpan data") }
            }
            .addOnFailureListener { onError(it.message ?: "Registrasi gagal") }
    }

    fun simpanRole(
        uid: String,
        role: String,
        isVerified: Boolean = false,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val updates = hashMapOf<String, Any>(
            "role"       to role,
            "updated_at" to Timestamp.now()
        )
        // Pengelola langsung verified; guru/ortu menunggu verifikasi pengelola
        if (isVerified || role == "pengelola") updates["is_verified"] = true
        else updates["is_verified"] = false

        db.collection("users").document(uid)
            .update(updates)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Gagal simpan role") }
    }

    fun getRole(
        uid: String,
        onSuccess: (role: String) -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc -> onSuccess(doc.getString("role") ?: "") }
            .addOnFailureListener { onError(it.message ?: "Gagal ambil role") }
    }

    fun logout() = auth.signOut()

    /**
     * Reset password via email — UAT A-9: email reset terkirim ke inbox.
     * UAT A-10: tanpa email → tidak ada email terkirim (handled di UI).
     */
    fun resetPassword(
        email: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (email.isBlank()) {
            onError("Email tidak boleh kosong")
            return
        }
        auth.sendPasswordResetEmail(email.trim())
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Gagal kirim reset password") }
    }

    // ════════════════════════════════════════════════════════════
    // LOGIN DENGAN GOOGLE
    // ════════════════════════════════════════════════════════════

    fun loginWithGoogle(
        idToken: String,
        onSuccess: (uid: String, isNewUser: Boolean) -> Unit,
        onError: (String) -> Unit
    ) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnSuccessListener { result ->
                val user      = result.user ?: return@addOnSuccessListener
                val isNewUser = result.additionalUserInfo?.isNewUser ?: false
                if (isNewUser) {
                    val data = hashMapOf(
                        "nama"        to (user.displayName ?: ""),
                        "email"       to (user.email ?: ""),
                        "no_hp"       to "",
                        "foto_url"    to (user.photoUrl?.toString() ?: ""),
                        "role"        to "",
                        "is_verified" to false,
                        "status_akun" to "aktif",
                        "created_at"  to Timestamp.now(),
                        "updated_at"  to Timestamp.now()
                    )
                    db.collection("users").document(user.uid).set(data)
                        .addOnSuccessListener { onSuccess(user.uid, true) }
                        .addOnFailureListener { onError(it.message ?: "Gagal simpan data Google") }
                } else {
                    // User lama — cek status akun dulu
                    checkStatusAkun(user.uid,
                        onAktif    = { onSuccess(user.uid, false) },
                        onNonAktif = {
                            auth.signOut()
                            onError("Akun kamu dinonaktifkan oleh pengelola. Hubungi admin.")
                        },
                        onDitolak  = {
                            auth.signOut()
                            onError("Pendaftaran akun kamu ditolak oleh pengelola.")
                        },
                        onError    = { onError(it) }
                    )
                }
            }
            .addOnFailureListener { onError(it.message ?: "Google Sign-In gagal") }
    }

    // ════════════════════════════════════════════════════════════
    // [B] USER — Data profil user
    // ════════════════════════════════════════════════════════════

    fun getDataUser(
        uid: String,
        onSuccess: (data: Map<String, Any?>) -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { onSuccess(it.data ?: emptyMap()) }
            .addOnFailureListener { onError(it.message ?: "Gagal ambil data user") }
    }

    fun updateDataUser(
        uid: String,
        dataUpdate: Map<String, Any>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val data = dataUpdate.toMutableMap()
        data["updated_at"] = Timestamp.now()
        db.collection("users").document(uid).update(data)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Gagal update data") }
    }

    // ════════════════════════════════════════════════════════════
    // [C] DATA ANAK — Khusus Orang Tua
    // ════════════════════════════════════════════════════════════

    fun getDataAnak(
        onSuccess: (anakId: String, data: Map<String, Any?>) -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection("users").document(uid)
            .collection("anak")
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    onError("Data anak belum ada")
                    return@addOnSuccessListener
                }
                val doc = snapshot.documents[0]
                onSuccess(doc.id, doc.data ?: emptyMap())
            }
            .addOnFailureListener { onError(it.message ?: "Gagal ambil data anak") }
    }

    fun tambahDataAnak(
        namaAnak: String,
        usiaAnak: String,
        usiaBulan: Int,
        jenisKelamin: String,
        tanggalLahir: Timestamp,
        sekolah: String,
        kelas: String,
        onSuccess: (anakId: String) -> Unit,
        onError: (String) -> Unit
    ) {
        val data = hashMapOf(
            "nama_anak"          to namaAnak,
            "usia_anak"          to usiaAnak,
            "usia_bulan"         to usiaBulan,
            "jenis_kelamin"      to jenisKelamin,
            "tanggal_lahir"      to tanggalLahir,
            "sekolah_anak"       to sekolah,
            "kelas"              to kelas,
            "foto_anak"          to "",
            "berat_badan"        to 0.0,
            "tinggi_badan"       to 0.0,
            "status_gizi"        to "Belum dicek",
            "z_score"            to 0.0,
            "persentase_nutrisi" to 0,
            "tanggal_cek"        to null,
            "updated_at"         to Timestamp.now()
        )
        db.collection("users").document(uid)
            .collection("anak").add(data)
            .addOnSuccessListener { onSuccess(it.id) }
            .addOnFailureListener { onError(it.message ?: "Gagal tambah data anak") }
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
        val anakRef = db.collection("users").document(uid)
            .collection("anak").document(anakId)

        val dataGizi = hashMapOf(
            "berat_badan"        to berat,
            "tinggi_badan"       to tinggi,
            "usia_bulan"         to usiaBulan,
            "z_score"            to zScore,
            "status_gizi"        to statusGizi,
            "persentase_nutrisi" to persentaseNutrisi,
            "tanggal_cek"        to Timestamp.now(),
            "updated_at"         to Timestamp.now()
        )

        val batch = db.batch()
        batch.update(anakRef, dataGizi as Map<String, Any>)
        batch.set(anakRef.collection("riwayat_gizi").document(), dataGizi)
        batch.commit()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Gagal simpan gizi") }
    }

    fun getRiwayatGizi(
        anakId: String,
        limit: Long = 6,
        onSuccess: (List<Map<String, Any?>>) -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection("users").document(uid)
            .collection("anak").document(anakId)
            .collection("riwayat_gizi")
            .orderBy("tanggal_cek", Query.Direction.DESCENDING)
            .limit(limit)
            .get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.map { it.data ?: emptyMap() }
                onSuccess(list)
            }
            .addOnFailureListener { onError(it.message ?: "Gagal ambil riwayat") }
    }

    // ════════════════════════════════════════════════════════════
    // [D] MENU MBG — Pengelola (CRUD) & Orang Tua (Read)
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
        val data = hashMapOf(
            "tanggal"            to tanggal,
            "tanggal_label"      to tanggalLabel,
            "nama_menu"          to namaMenu,
            "deskripsi"          to deskripsi,
            "kalori"             to kalori,
            "protein"            to protein,
            "karbo"              to karbo,
            "lemak"              to lemak,
            "serat"              to serat,
            "persentase_nutrisi" to persentaseNutrisi,
            "foto_menu"          to fotoMenu,
            "dibuat_oleh"        to uid,
            "created_at"         to Timestamp.now()
        )
        db.collection("menu_mbg").document(tanggal)
            .set(data)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Gagal simpan menu") }
    }

    fun getMenuHariIni(
        tanggal: String,
        onSuccess: (data: Map<String, Any?>?) -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection("menu_mbg").document(tanggal).get()
            .addOnSuccessListener { doc -> onSuccess(if (doc.exists()) doc.data else null) }
            .addOnFailureListener { onError(it.message ?: "Gagal ambil menu") }
    }

    fun getDaftarMenu(
        tanggalMulai: String,
        tanggalAkhir: String,
        onSuccess: (List<Map<String, Any?>>) -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection("menu_mbg")
            .orderBy("tanggal")
            .startAt(tanggalMulai)
            .endAt(tanggalAkhir)
            .get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.map { it.data ?: emptyMap() }
                onSuccess(list)
            }
            .addOnFailureListener { onError(it.message ?: "Gagal ambil daftar menu") }
    }

    fun hapusMenuMbg(
        tanggal: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
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
    // [E] ARTIKEL EDUKASI — Guru (CRUD) & Orang Tua (Read)
    // ════════════════════════════════════════════════════════════

    val KATEGORI_ARTIKEL = listOf("Stunting", "Resep Sehat", "Gizi", "Tumbuh Kembang")

    fun tambahArtikel(
        judul: String,
        deskripsi: String,
        isiKonten: String,
        kategori: String,
        thumbnailUrl: String = "",
        menitBaca: Int,
        onSuccess: (artikelId: String) -> Unit,
        onError: (String) -> Unit
    ) {
        val data = hashMapOf(
            "judul"         to judul,
            "deskripsi"     to deskripsi,
            "isi_konten"    to isiKonten,
            "kategori"      to kategori,
            "thumbnail_url" to thumbnailUrl,
            "menit_baca"    to menitBaca,
            "penulis"       to uid,
            "is_published"  to true,
            "waktu_publish" to Timestamp.now(),
            "dibuat_oleh"   to uid
        )
        db.collection("artikel").add(data)
            .addOnSuccessListener { onSuccess(it.id) }
            .addOnFailureListener { onError(it.message ?: "Gagal tambah artikel") }
    }

    fun getArtikel(
        kategori: String = "Semua",
        onSuccess: (List<Pair<String, Map<String, Any?>>>) -> Unit,
        onError: (String) -> Unit
    ) {
        val query = if (kategori == "Semua") {
            db.collection("artikel")
                .whereEqualTo("is_published", true)
                .orderBy("waktu_publish", Query.Direction.DESCENDING)
        } else {
            db.collection("artikel")
                .whereEqualTo("is_published", true)
                .whereEqualTo("kategori", kategori)
                .orderBy("waktu_publish", Query.Direction.DESCENDING)
        }
        query.get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.map { Pair(it.id, it.data ?: emptyMap()) }
                onSuccess(list)
            }
            .addOnFailureListener { onError(it.message ?: "Gagal ambil artikel") }
    }

    fun getArtikelSaya(
        onSuccess: (List<Pair<String, Map<String, Any?>>>) -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection("artikel")
            .whereEqualTo("dibuat_oleh", uid)
            .orderBy("waktu_publish", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.map { Pair(it.id, it.data ?: emptyMap()) }
                onSuccess(list)
            }
            .addOnFailureListener { onError(it.message ?: "Gagal ambil artikel") }
    }

    fun updateArtikel(
        artikelId: String,
        dataUpdate: Map<String, Any>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val data = dataUpdate.toMutableMap()
        data["updated_at"] = Timestamp.now()
        db.collection("artikel").document(artikelId).update(data)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Gagal update artikel") }
    }

    fun hapusArtikel(
        artikelId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection("artikel").document(artikelId).delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Gagal hapus artikel") }
    }

    fun getArtikelById(
        artikelId: String,
        onSuccess: (Map<String, Any?>) -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection("artikel").document(artikelId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) onSuccess(doc.data ?: emptyMap())
                else onError("Artikel tidak ditemukan")
            }
            .addOnFailureListener { onError(it.message ?: "Gagal ambil artikel") }
    }

    // ════════════════════════════════════════════════════════════
    // [F] PENGUMUMAN
    // ════════════════════════════════════════════════════════════

    fun kirimPengumuman(
        judul: String,
        isi: String,
        targetRole: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val data = hashMapOf(
            "judul_pengumuman" to judul,
            "isi_pengumuman"   to isi,
            "target_role"      to targetRole,
            "is_published"     to true,
            "waktu_pengumuman" to Timestamp.now(),
            "dibuat_oleh"      to uid
        )
        db.collection("pengumuman").add(data)
            .addOnSuccessListener {
                blastNotifikasi(
                    judul      = judul,
                    isi        = isi,
                    tipe       = "pengumuman",
                    targetRole = targetRole,
                    onSuccess  = onSuccess,
                    onError    = onError
                )
            }
            .addOnFailureListener { onError(it.message ?: "Gagal kirim pengumuman") }
    }

    fun getPengumumanTerbaru(
        limit: Long = 5,
        onSuccess: (List<Map<String, Any?>>) -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection("pengumuman")
            .whereEqualTo("is_published", true)
            .orderBy("waktu_pengumuman", Query.Direction.DESCENDING)
            .limit(limit)
            .get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.map { it.data ?: emptyMap() }
                onSuccess(list)
            }
            .addOnFailureListener { onError(it.message ?: "Gagal ambil pengumuman") }
    }

    // ════════════════════════════════════════════════════════════
    // [G] NOTIFIKASI & STORAGE
    // ════════════════════════════════════════════════════════════

    val storage = com.google.firebase.storage.FirebaseStorage.getInstance()

    fun uploadImage(
        path: String,
        uri: android.net.Uri,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val ref = storage.reference.child(path)
        ref.putFile(uri)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { url -> onSuccess(url.toString()) }
            }
            .addOnFailureListener { onError(it.message ?: "Upload gagal") }
    }

    fun kirimNotifikasi(
        targetUid: String,
        judul: String,
        isi: String,
        tipe: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val data = hashMapOf(
            "user_id" to targetUid,
            "judul"   to judul,
            "isi"     to isi,
            "tipe"    to tipe,
            "is_read" to false,
            "waktu"   to Timestamp.now()
        )
        db.collection("notifikasi").add(data)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Gagal kirim notifikasi") }
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
            db.collection("users").whereEqualTo("status_akun", "aktif")
        } else {
            db.collection("users")
                .whereEqualTo("role", targetRole)
                .whereEqualTo("status_akun", "aktif")
        }
        query.get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) { onSuccess(); return@addOnSuccessListener }
                val batch = db.batch()
                snapshot.documents.forEach { userDoc ->
                    val notifRef = db.collection("notifikasi").document()
                    batch.set(notifRef, hashMapOf(
                        "user_id" to userDoc.id,
                        "judul"   to judul,
                        "isi"     to isi,
                        "tipe"    to tipe,
                        "is_read" to false,
                        "waktu"   to Timestamp.now()
                    ))
                }
                batch.commit()
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { onError(it.message ?: "Gagal blast notifikasi") }
            }
            .addOnFailureListener { onError(it.message ?: "Gagal ambil daftar user") }
    }

    fun listenNotifikasi(
        onUpdate: (List<Pair<String, Map<String, Any?>>>) -> Unit,
        onError: (String) -> Unit
    ) = db.collection("notifikasi")
        .whereEqualTo("user_id", uid)
        .orderBy("waktu", Query.Direction.DESCENDING)
        .addSnapshotListener { snapshot, error ->
            if (error != null) { onError(error.message ?: "Error"); return@addSnapshotListener }
            val list = snapshot?.documents?.map { Pair(it.id, it.data ?: emptyMap()) } ?: emptyList()
            onUpdate(list)
        }

    fun tandaiDibaca(notifId: String) {
        db.collection("notifikasi").document(notifId).update("is_read", true)
    }

    fun tandaiSemuaDibaca(onSuccess: () -> Unit = {}) {
        db.collection("notifikasi")
            .whereEqualTo("user_id", uid)
            .whereEqualTo("is_read", false)
            .get()
            .addOnSuccessListener { snapshot ->
                val batch = db.batch()
                snapshot.documents.forEach { batch.update(it.reference, "is_read", true) }
                batch.commit().addOnSuccessListener { onSuccess() }
            }
    }

    fun getJumlahNotifBelumDibaca(onResult: (Int) -> Unit) {
        db.collection("notifikasi")
            .whereEqualTo("user_id", uid)
            .whereEqualTo("is_read", false)
            .get()
            .addOnSuccessListener { onResult(it.size()) }
            .addOnFailureListener { onResult(0) }
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
        fotoAduan: String = "",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val data = hashMapOf(
            "judul_aduan"    to judul,
            "isi_aduan"      to isi,
            "kategori_aduan" to kategori,
            "status_aduan"   to "menunggu",
            "pengirim_uid"   to uid,
            "pengirim_nama"  to pengirimNama,
            "pengirim_role"  to pengirimRole,
            "foto_aduan"     to fotoAduan,
            "balasan"        to "",
            "created_at"     to Timestamp.now(),
            "updated_at"     to Timestamp.now()
        )
        db.collection("aduan").add(data)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Gagal kirim aduan") }
    }

    fun getAduanSaya(
        onSuccess: (List<Pair<String, Map<String, Any?>>>) -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection("aduan")
            .whereEqualTo("pengirim_uid", uid)
            .orderBy("created_at", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.map { Pair(it.id, it.data ?: emptyMap()) }
                onSuccess(list)
            }
            .addOnFailureListener { onError(it.message ?: "Gagal ambil aduan") }
    }

    fun getAllAduan(
        onSuccess: (List<Pair<String, Map<String, Any?>>>) -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection("aduan")
            .orderBy("created_at", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.map { Pair(it.id, it.data ?: emptyMap()) }
                onSuccess(list)
            }
            .addOnFailureListener { onError(it.message ?: "Gagal ambil aduan") }
    }

    fun balasAduan(
        aduanId: String,
        balasan: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection("aduan").document(aduanId)
            .update(
                "balasan",      balasan,
                "status_aduan", "selesai",
                "updated_at",   Timestamp.now()
            )
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Gagal balas aduan") }
    }

    // ════════════════════════════════════════════════════════════
    // [I] REKAP GIZI — Guru (CRUD)
    // ════════════════════════════════════════════════════════════

    fun simpanRekapGizi(
        sekolah: String,
        periode: String,
        totalSiswa: Int,
        normal: Int,
        giziKurang: Int,
        giziBuruk: Int,
        giziLebih: Int,
        obesitas: Int,
        onSuccess: (rekapId: String) -> Unit,
        onError: (String) -> Unit
    ) {
        val data = hashMapOf(
            "sekolah"      to sekolah,
            "guru_uid"     to uid,
            "periode"      to periode,
            "total_siswa"  to totalSiswa,
            "normal"       to normal,
            "gizi_kurang"  to giziKurang,
            "gizi_buruk"   to giziBuruk,
            "gizi_lebih"   to giziLebih,
            "obesitas"     to obesitas,
            "created_at"   to Timestamp.now(),
            "updated_at"   to Timestamp.now()
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
        val data = hashMapOf(
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
            .collection("detail_siswa").add(data)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Gagal tambah siswa") }
    }

    fun getRekapGizi(
        onSuccess: (List<Pair<String, Map<String, Any?>>>) -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection("rekap_gizi")
            .whereEqualTo("guru_uid", uid)
            .orderBy("created_at", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.map { Pair(it.id, it.data ?: emptyMap()) }
                onSuccess(list)
            }
            .addOnFailureListener { onError(it.message ?: "Gagal ambil rekap") }
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
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.map { it.data ?: emptyMap() }
                onSuccess(list)
            }
            .addOnFailureListener { onError(it.message ?: "Gagal ambil detail siswa") }
    }

    // ════════════════════════════════════════════════════════════
    // [J] DATA PENGGUNA — Khusus Pengelola
    // ════════════════════════════════════════════════════════════

    /**
     * Ambil SEMUA pengguna yang sudah terverifikasi (aktif DAN nonaktif).
     * Fix dari versi lama yang hanya ambil yg aktif — sehingga pengelola
     * bisa melihat & mengaktifkan kembali akun yang sudah dinonaktifkan (UAT D-3).
     */
    fun getAllPengguna(
        role: String = "semua",
        onSuccess: (List<Pair<String, Map<String, Any?>>>) -> Unit,
        onError: (String) -> Unit
    ) {
        // Ambil user verified, tanpa filter status_akun
        // agar pengelola bisa lihat nonaktif juga (untuk diaktifkan kembali)
        val query = if (role == "semua") {
            db.collection("users")
                .whereEqualTo("is_verified", true)
                .orderBy("created_at", Query.Direction.DESCENDING)
        } else {
            db.collection("users")
                .whereEqualTo("role", role)
                .whereEqualTo("is_verified", true)
                .orderBy("created_at", Query.Direction.DESCENDING)
        }
        query.get()
            .addOnSuccessListener { snapshot ->
                // Filter exclude pengelola dari list (tidak perlu ditampilkan)
                val list = snapshot.documents
                    .filter { (it.getString("role") ?: "") != "pengelola" }
                    .map { Pair(it.id, it.data ?: emptyMap()) }
                onSuccess(list)
            }
            .addOnFailureListener { onError(it.message ?: "Gagal ambil data pengguna") }
    }

    /**
     * Pendaftar baru = user dengan is_verified=false DAN status_akun = "aktif"
     * (bukan ditolak, bukan pengelola)
     */
    fun getPendaftarBaru(
        onSuccess: (List<Pair<String, Map<String, Any?>>>) -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection("users")
            .whereEqualTo("is_verified", false)
            .whereEqualTo("status_akun", "aktif")
            .get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents
                    .filter { doc ->
                        val role = doc.getString("role") ?: ""
                        role == "guru" || role == "orang_tua"
                    }
                    .map { Pair(it.id, it.data ?: emptyMap()) }
                    .sortedByDescending { (it.second["created_at"] as? Timestamp) ?: Timestamp.now() }
                onSuccess(list)
            }
            .addOnFailureListener { onError(it.message ?: "Gagal ambil pendaftar baru") }
    }

    /**
     * Verifikasi akun — UAT D-1.
     * Set is_verified=true, status_akun="aktif", kirim notifikasi ke user.
     */
    fun verifikasiAkun(
        targetUid: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection("users").document(targetUid)
            .update(
                "is_verified", true,
                "status_akun", "aktif",
                "updated_at",  Timestamp.now()
            )
            .addOnSuccessListener {
                kirimNotifikasi(
                    targetUid = targetUid,
                    judul     = "Akun Berhasil Diverifikasi 🎉",
                    isi       = "Selamat! Akun kamu sudah diverifikasi oleh pengelola. Silakan login sekarang.",
                    tipe      = "akun"
                )
                onSuccess()
            }
            .addOnFailureListener { onError(it.message ?: "Gagal verifikasi akun") }
    }

    /**
     * Tolak akun — UAT D-2.
     * Set status_akun="ditolak", kirim notifikasi beserta alasan.
     */
    fun tolakAkun(
        targetUid: String,
        alasan: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection("users").document(targetUid)
            .update(
                "status_akun",  "ditolak",
                "alasan_tolak", alasan,
                "is_verified",  false,
                "updated_at",   Timestamp.now()
            )
            .addOnSuccessListener {
                kirimNotifikasi(
                    targetUid = targetUid,
                    judul     = "Pendaftaran Ditolak",
                    isi       = "Maaf, pendaftaran akun kamu ditolak. Alasan: $alasan",
                    tipe      = "akun"
                )
                onSuccess()
            }
            .addOnFailureListener { onError(it.message ?: "Gagal tolak akun") }
    }

    /**
     * Nonaktifkan akun — UAT D-3.
     * Pengguna tidak bisa login setelah dinonaktifkan.
     * (login() di atas sudah cek status_akun dan block jika nonaktif)
     */
    fun nonaktifkanAkun(
        targetUid: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection("users").document(targetUid)
            .update(
                "status_akun", "nonaktif",
                "updated_at",  Timestamp.now()
            )
            .addOnSuccessListener {
                // Kirim notifikasi ke user yang dinonaktifkan
                kirimNotifikasi(
                    targetUid = targetUid,
                    judul     = "Akun Dinonaktifkan",
                    isi       = "Akun kamu telah dinonaktifkan oleh pengelola. Hubungi admin untuk informasi lebih lanjut.",
                    tipe      = "akun"
                )
                onSuccess()
            }
            .addOnFailureListener { onError(it.message ?: "Gagal nonaktifkan akun") }
    }

    /**
     * Aktifkan kembali akun — UAT D-3 (reverse).
     */
    fun aktifkanAkun(
        targetUid: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection("users").document(targetUid)
            .update(
                "status_akun", "aktif",
                "updated_at",  Timestamp.now()
            )
            .addOnSuccessListener {
                kirimNotifikasi(
                    targetUid = targetUid,
                    judul     = "Akun Diaktifkan Kembali ✅",
                    isi       = "Akun kamu telah diaktifkan kembali oleh pengelola. Silakan login sekarang.",
                    tipe      = "akun"
                )
                onSuccess()
            }
            .addOnFailureListener { onError(it.message ?: "Gagal aktifkan akun") }
    }

    // ════════════════════════════════════════════════════════════
    // [K] KALKULASI GIZI — WHO Standard
    // ════════════════════════════════════════════════════════════

    fun hitungZScore(berat: Double, usiaBulan: Int): Double {
        val (median, sd) = when {
            usiaBulan <= 1  -> Pair(4.5,  0.60)
            usiaBulan <= 2  -> Pair(5.6,  0.70)
            usiaBulan <= 3  -> Pair(6.4,  0.75)
            usiaBulan <= 4  -> Pair(7.0,  0.80)
            usiaBulan <= 5  -> Pair(7.5,  0.85)
            usiaBulan <= 6  -> Pair(7.9,  0.88)
            usiaBulan <= 7  -> Pair(8.3,  0.92)
            usiaBulan <= 8  -> Pair(8.6,  0.95)
            usiaBulan <= 9  -> Pair(8.9,  0.97)
            usiaBulan <= 10 -> Pair(9.2,  0.99)
            usiaBulan <= 11 -> Pair(9.4,  1.01)
            usiaBulan <= 12 -> Pair(9.6,  1.03)
            usiaBulan <= 15 -> Pair(10.2, 1.08)
            usiaBulan <= 18 -> Pair(10.8, 1.13)
            usiaBulan <= 21 -> Pair(11.3, 1.17)
            usiaBulan <= 24 -> Pair(12.2, 1.25)
            usiaBulan <= 27 -> Pair(12.6, 1.28)
            usiaBulan <= 30 -> Pair(13.0, 1.32)
            usiaBulan <= 33 -> Pair(13.4, 1.36)
            usiaBulan <= 36 -> Pair(14.3, 1.43)
            usiaBulan <= 42 -> Pair(15.2, 1.52)
            usiaBulan <= 48 -> Pair(16.3, 1.63)
            usiaBulan <= 54 -> Pair(17.1, 1.71)
            usiaBulan <= 60 -> Pair(18.3, 1.83)
            else            -> Pair(20.0, 2.00)
        }
        return (berat - median) / sd
    }

    fun hitungZScoreTbu(tinggi: Double, usiaBulan: Int): Double {
        val (median, sd) = when {
            usiaBulan <= 3  -> Pair(59.8,  2.2)
            usiaBulan <= 6  -> Pair(67.6,  2.4)
            usiaBulan <= 9  -> Pair(72.0,  2.6)
            usiaBulan <= 12 -> Pair(75.7,  2.7)
            usiaBulan <= 18 -> Pair(82.3,  2.9)
            usiaBulan <= 24 -> Pair(87.8,  3.1)
            usiaBulan <= 36 -> Pair(96.1,  3.5)
            usiaBulan <= 48 -> Pair(103.3, 3.8)
            usiaBulan <= 60 -> Pair(110.0, 4.0)
            else            -> Pair(116.0, 4.2)
        }
        return (tinggi - median) / sd
    }

    fun kategoriGizi(zScore: Double): String = when {
        zScore < -3.0 -> "Gizi Buruk"
        zScore < -2.0 -> "Gizi Kurang"
        zScore <= 1.0 -> "Normal"
        zScore <= 2.0 -> "Berisiko Gizi Lebih"
        zScore <= 3.0 -> "Gizi Lebih"
        else          -> "Obesitas"
    }

    fun hitungPersentaseNutrisi(zScore: Double): Int = when {
        zScore < -3.0 -> 30
        zScore < -2.0 -> 55
        zScore < -1.0 -> 75
        zScore <= 1.0 -> 100
        zScore <= 2.0 -> 85
        zScore <= 3.0 -> 70
        else          -> 60
    }

    fun rekomendasiMakanan(statusGizi: String, usiaBulan: Int): List<String> {
        val isBalita = usiaBulan <= 60
        return when (statusGizi.lowercase()) {
            "gizi buruk" -> listOf(
                "🥛 Susu formula/ASI intensif minimal 8x/hari",
                "🥚 Telur rebus 1–2 butir per hari (tinggi protein)",
                "🐟 Ikan kukus atau ayam tanpa lemak setiap makan",
                "🥜 Kacang-kacangan halus (kacang merah, tempe, tahu)",
                "🥣 Bubur nasi dengan kuah kaldu ayam/sapi",
                "🧀 Keju atau yoghurt sebagai camilan",
                "🫘 RUTF (Ready-to-Use Therapeutic Food) jika tersedia",
                "⚠️ Segera konsultasi ke dokter atau Puskesmas terdekat"
            )
            "gizi kurang" -> listOf(
                "🍗 Tambah porsi protein: ayam, ikan, telur, atau daging",
                "🥚 Telur 1 butir setiap hari wajib",
                "🥛 Susu atau produk susu (keju, yoghurt) setiap hari",
                "🥜 Kacang-kacangan: tempe, tahu, edamame",
                "🍚 Nasi atau kentang dengan porsi cukup setiap makan",
                "🥑 Tambahkan alpukat atau minyak zaitun untuk kalori sehat",
                "🍌 Buah-buahan manis: pisang, mangga, pepaya",
                "📅 Makan teratur 3x utama + 2x selingan sehat"
            )
            "berisiko gizi lebih" -> listOf(
                "🥦 Perbanyak sayuran hijau: bayam, brokoli, kangkung",
                "🍎 Pilih buah rendah gula: apel, pir, jeruk",
                "🐟 Protein rendah lemak: ikan, ayam tanpa kulit",
                "🌾 Ganti nasi putih dengan nasi merah atau ubi",
                "💧 Perbanyak minum air putih, kurangi jus buah",
                "🚫 Kurangi camilan manis dan gorengan",
                "🏃 Tingkatkan aktivitas fisik: bermain aktif 1 jam/hari"
            )
            "gizi lebih" -> listOf(
                "🥦 Sayuran harus isi setengah piring setiap makan",
                "🍎 Buah segar 2–3 porsi per hari sebagai camilan",
                "🐟 Protein tanpa lemak: ikan kukus/panggang, tahu, tempe",
                "🌾 Karbohidrat kompleks: ubi, jagung, nasi merah",
                "🚫 Hindari: gorengan, fast food, minuman bersoda",
                "💧 Minum air putih 6–8 gelas per hari",
                "🏃 Aktivitas fisik 60 menit per hari",
                "📋 Konsultasi ke ahli gizi untuk program diet anak"
            )
            "obesitas" -> listOf(
                "🥗 Perbanyak sayuran segar: salad, tumisan tanpa minyak",
                "🍎 Buah rendah kalori: semangka, melon, jeruk, apel",
                "🐟 Protein tanpa lemak: ikan rebus/kukus, putih telur",
                "🚫 Hindari: gula, tepung putih, makanan olahan, fast food",
                "🥛 Susu rendah lemak atau skim, batasi 2 gelas/hari",
                "💧 Air putih 8 gelas per hari, hindari minuman manis",
                "🏃 Olahraga teratur minimal 60 menit per hari",
                "⚠️ Wajib konsultasi dokter anak dan ahli gizi",
                "📊 Pantau berat badan setiap 2 minggu"
            )
            else -> listOf(
                "🍚 Nasi + lauk-pauk bergizi 3 porsi per hari",
                "🥩 Protein hewani: ayam, ikan, telur, daging sapi",
                "🌱 Protein nabati: tempe, tahu, kacang-kacangan",
                "🥦 Sayuran berwarna-warni minimal 2–3 porsi/hari",
                "🍎 Buah segar 2–3 porsi per hari",
                "🥛 Susu atau produk susu setiap hari",
                "💧 Air putih cukup sesuai usia",
                "✅ Status gizi baik! Pertahankan pola makan sehat"
            )
        }
    }
}