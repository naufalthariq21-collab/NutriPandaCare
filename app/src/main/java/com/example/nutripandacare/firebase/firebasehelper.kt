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

    /** Login dengan email & password */
    fun login(
        email: String,
        password: String,
        onSuccess: (uid: String) -> Unit,
        onError: (String) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val user = result.user ?: return@addOnSuccessListener
                // Diperbarui: Langsung masuk tanpa wajib verifikasi email agar memudahkan user
                onSuccess(user.uid)
            }
            .addOnFailureListener { onError(it.message ?: "Login gagal") }
    }

    /** Register akun baru */
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
                val user = result.user

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
                    .addOnSuccessListener {
                        user?.sendEmailVerification()
                        onSuccess(uid)
                    }
                    .addOnFailureListener { onError(it.message ?: "Gagal simpan data") }
            }
            .addOnFailureListener { onError(it.message ?: "Registrasi gagal") }
    }

    /** Simpan role user setelah pilih role */
    fun simpanRole(
        uid: String,
        role: String,
        isVerified: Boolean = false,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val updates = hashMapOf<String, Any>(
            "role" to role,
            "updated_at" to Timestamp.now()
        )
        // Jika pengelola, otomatis dianggap verified
        if (isVerified || role == "pengelola") updates["is_verified"] = true

        db.collection("users").document(uid)
            .update(updates)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Gagal simpan role") }
    }

    /** Ambil role user dari Firestore */
    fun getRole(
        uid: String,
        onSuccess: (role: String) -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                onSuccess(doc.getString("role") ?: "")
            }
            .addOnFailureListener { onError(it.message ?: "Gagal ambil role") }
    }

    /** Logout */
    fun logout() = auth.signOut()

    /** Kirim ulang email verifikasi */
    fun kirimUlangVerifikasi(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        auth.currentUser?.sendEmailVerification()
            ?.addOnSuccessListener { onSuccess() }
            ?.addOnFailureListener { onError(it.message ?: "Gagal kirim email") }
    }

    /** Cek apakah email sudah diverifikasi */
    fun cekEmailVerified(onResult: (Boolean) -> Unit) {
        auth.currentUser?.reload()
            ?.addOnSuccessListener {
                val verified = auth.currentUser?.isEmailVerified ?: false
                if (verified) {
                    db.collection("users").document(uid)
                        .update("is_verified", true)
                }
                onResult(verified)
            }
    }

    /** Reset password */
    fun resetPassword(
        email: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Gagal kirim reset password") }
    }

    // ════════════════════════════════════════════════════════════
    // LOGIN DENGAN GOOGLE — Firebase Google Sign-In
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
                        "is_verified" to true,
                        "status_akun" to "aktif",
                        "created_at"  to Timestamp.now(),
                        "updated_at"  to Timestamp.now()
                    )
                    db.collection("users").document(user.uid).set(data)
                        .addOnSuccessListener { onSuccess(user.uid, true) }
                        .addOnFailureListener { onError(it.message ?: "Gagal simpan data Google") }
                } else {
                    onSuccess(user.uid, false)
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
        db.collection("users").document(uid)
            .get()
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

        db.collection("users").document(uid)
            .update(data)
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
            .collection("anak")
            .add(data)
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
    // [D] MENU MBG — Khusus Pengelola (CRUD) & Orang Tua (Read)
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
        db.collection("menu_mbg").document(tanggal)
            .get()
            .addOnSuccessListener { doc ->
                onSuccess(if (doc.exists()) doc.data else null)
            }
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
        db.collection("menu_mbg").document(tanggal)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Gagal hapus menu") }
    }

    fun updateMenuMbg(
        tanggal: String,
        dataUpdate: Map<String, Any>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection("menu_mbg").document(tanggal)
            .update(dataUpdate)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Gagal update menu") }
    }


    // ════════════════════════════════════════════════════════════
    // [E] ARTIKEL EDUKASI — Guru (CRUD) & Orang Tua (Read)
    // ════════════════════════════════════════════════════════════

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
        var query = db.collection("artikel")
            .whereEqualTo("is_published", true)
            .orderBy("waktu_publish", Query.Direction.DESCENDING)

        if (kategori != "Semua") {
            query = db.collection("artikel")
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

    fun updateArtikel(
        artikelId: String,
        dataUpdate: Map<String, Any>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection("artikel").document(artikelId)
            .update(dataUpdate)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Gagal update artikel") }
    }

    fun hapusArtikel(
        artikelId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection("artikel").document(artikelId)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Gagal hapus artikel") }
    }


    // ════════════════════════════════════════════════════════════
    // [F] PENGUMUMAN — Pengelola (CRUD)
    // ════════════════════════════════════════════════════════════

    fun kirimPengumuman(
        judul: String,
        isi: String,
        targetRole: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val data = hashMapOf(
            "judul_pengumuman"  to judul,
            "isi_pengumuman"    to isi,
            "target_role"       to targetRole,
            "is_published"      to true,
            "waktu_pengumuman"  to Timestamp.now(),
            "dibuat_oleh"       to uid
        )

        db.collection("pengumuman").add(data)
            .addOnSuccessListener {
                blastNotifikasi(
                    judul    = judul,
                    isi      = isi,
                    tipe     = "pengumuman",
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
    // [G] NOTIFIKASI — Semua Role
    // ════════════════════════════════════════════════════════════

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
        db.collection("notifikasi").document(notifId)
            .update("is_read", true)
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


    // ════════════════════════════════════════════════════════════
    // [H] ADUAN — Orang Tua & Guru (Create + Read)
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
            .collection("detail_siswa")
            .add(data)
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

    fun getAllPengguna(
        role: String = "semua",
        onSuccess: (List<Pair<String, Map<String, Any?>>>) -> Unit,
        onError: (String) -> Unit
    ) {
        val query = if (role == "semua") {
            db.collection("users").orderBy("created_at", Query.Direction.DESCENDING)
        } else {
            db.collection("users")
                .whereEqualTo("role", role)
                .orderBy("created_at", Query.Direction.DESCENDING)
        }

        query.get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.map { Pair(it.id, it.data ?: emptyMap()) }
                onSuccess(list)
            }
            .addOnFailureListener { onError(it.message ?: "Gagal ambil data pengguna") }
    }

    fun getPendaftarBaru(
        onSuccess: (List<Pair<String, Map<String, Any?>>>) -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection("users")
            .whereEqualTo("is_verified", false)
            .orderBy("created_at", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.map { Pair(it.id, it.data ?: emptyMap()) }
                onSuccess(list)
            }
            .addOnFailureListener { onError(it.message ?: "Gagal ambil pendaftar baru") }
    }

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
                    judul     = "Akun Berhasil Diverifikasi",
                    isi       = "Selamat! Akun kamu sudah aktif. Silakan login sekarang.",
                    tipe      = "akun"
                )
                onSuccess()
            }
            .addOnFailureListener { onError(it.message ?: "Gagal verifikasi akun") }
    }

    fun nonaktifkanAkun(
        targetUid: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection("users").document(targetUid)
            .update("status_akun", "nonaktif", "updated_at", Timestamp.now())
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Gagal nonaktifkan akun") }
    }

    fun hapusAkunPengguna(
        targetUid: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection("users").document(targetUid)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Gagal hapus akun") }
    }


    // ════════════════════════════════════════════════════════════
    // [K] HELPER UMUM
    // ════════════════════════════════════════════════════════════

    fun hitungZScore(berat: Double, usiaBulan: Int): Double {
        val median = when {
            usiaBulan <= 6  -> 6.4
            usiaBulan <= 12 -> 9.6
            usiaBulan <= 24 -> 12.2
            usiaBulan <= 36 -> 14.3
            usiaBulan <= 48 -> 16.3
            usiaBulan <= 60 -> 18.3
            else            -> 20.0
        }
        return (berat - median) / (median * 0.12)
    }

    fun kategoriGizi(zScore: Double): String = when {
        zScore < -3.0 -> "Gizi Buruk"
        zScore < -2.0 -> "Gizi Kurang"
        zScore <= 2.0 -> "Normal"
        zScore <= 3.0 -> "Gizi Lebih"
        else          -> "Obesitas"
    }
}
