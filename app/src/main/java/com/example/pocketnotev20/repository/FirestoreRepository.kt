package com.example.pocketnotev20.repository

import android.net.Uri
import com.example.pocketnotev20.model.AdminAccessRequestItem
import com.example.pocketnotev20.model.CalendarItem
import com.example.pocketnotev20.model.ContentApprovalRequestItem
import com.example.pocketnotev20.model.CtSlotItem
import com.example.pocketnotev20.model.NoteItem
import com.example.pocketnotev20.model.QuestionItem
import com.example.pocketnotev20.model.RoutineItem
import com.example.pocketnotev20.model.UserProfileItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class FirestoreRepository {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()

    fun signup(
        email: String,
        pass: String,
        name: String,
        role: String = "user",
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        if (email.isBlank() || pass.length < 6) {
            onFailure(Exception("Email cannot be empty and password must be at least 6 characters"))
            return
        }
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: ""
                val requestedRole = if (role == "admin") "sub_admin" else "user"
                val userData = hashMapOf(
                    "uid" to uid,
                    "name" to name,
                    "email" to email,
                    "role" to if (role == "admin") "user" else role,
                    "requestedRole" to requestedRole,
                    "approvalStatus" to if (role == "admin") "pending" else "approved",
                    "reviewedBy" to "",
                    "accountStatus" to "active",
                    "removedBy" to ""
                )
                db.collection("users").document(uid).set(userData)
                    .addOnSuccessListener {
                        auth.signOut()
                        onSuccess(
                            if (role == "admin") {
                                "Sub admin request submitted. A main admin can approve or reject it from notifications."
                            } else {
                                "Account created successfully. Please sign in."
                            }
                        )
                    }
                    .addOnFailureListener { onFailure(it) }
            }
            .addOnFailureListener { e ->
                if (e is FirebaseAuthException && e.errorCode == "ERROR_CONFIGURATION_NOT_FOUND") {
                    onFailure(Exception("Please enable Email/Password login in Firebase Console (Authentication > Sign-in method)"))
                } else {
                    onFailure(e)
                }
            }
    }

    fun login(email: String, pass: String, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        auth.signInWithEmailAndPassword(email, pass)
            .addOnSuccessListener { result ->
                resolveUserRoleAndStatus(result.user?.uid ?: "", onSuccess, onFailure)
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    fun signInWithGoogle(idToken: String, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnSuccessListener { result ->
                val user = result.user
                val uid = user?.uid ?: ""
                
                db.collection("users").document(uid).get()
                    .addOnSuccessListener { doc ->
                        if (!doc.exists()) {
                            val userData = hashMapOf(
                                "uid" to uid,
                                "name" to (user?.displayName ?: ""),
                                "email" to (user?.email ?: ""),
                                "role" to "user",
                                "requestedRole" to "",
                                "approvalStatus" to "approved",
                                "reviewedBy" to "",
                                "accountStatus" to "active",
                                "removedBy" to ""
                            )
                            db.collection("users").document(uid).set(userData)
                                .addOnSuccessListener {
                                    onSuccess("user")
                                }
                                .addOnFailureListener { onFailure(it) }
                        } else {
                            resolveUserRoleAndStatus(uid, onSuccess, onFailure)
                        }
                    }
                    .addOnFailureListener { onFailure(it) }
            }
            .addOnFailureListener { onFailure(it) }
    }

    private fun resolveUserRoleAndStatus(uid: String, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val accountStatus = doc.getString("accountStatus").orEmpty()
                if (accountStatus.equals("removed", ignoreCase = true)) {
                    auth.signOut()
                    onFailure(Exception("This account was removed by the main admin."))
                    return@addOnSuccessListener
                }

                val role = doc.getString("role") ?: "user"
                val requestedRole = doc.getString("requestedRole").orEmpty()
                val approvalStatus = doc.getString("approvalStatus").orEmpty()

                val resolvedRole = when {
                    role == "admin" -> "admin"
                    role == "sub_admin" -> "sub_admin_approved"
                    requestedRole == "sub_admin" && approvalStatus == "rejected" -> "sub_admin_rejected"
                    requestedRole == "sub_admin" && approvalStatus == "pending" -> "sub_admin_pending"
                    requestedRole == "sub_admin" && approvalStatus == "approved" -> "sub_admin_approved"
                    else -> role
                }

                onSuccess(resolvedRole)
            }
            .addOnFailureListener { onFailure(it) }
    }

    fun getPendingSubAdminRequests(
        onSuccess: (List<AdminAccessRequestItem>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("users")
            .whereEqualTo("requestedRole", "sub_admin")
            .whereEqualTo("approvalStatus", "pending")
            .get()
            .addOnSuccessListener { result ->
                val items = result.documents.mapNotNull { document ->
                    document.toObject(AdminAccessRequestItem::class.java)
                }.sortedBy { it.name.lowercase() }
                onSuccess(items)
            }
            .addOnFailureListener { onFailure(it) }
    }

    fun getCurrentUserAdminAccessStatus(
        onSuccess: (AdminAccessRequestItem) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val uid = auth.currentUser?.uid.orEmpty()
        if (uid.isBlank()) {
            onFailure(Exception("No signed-in user found."))
            return
        }

        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                        onSuccess(
                            document.toObject(AdminAccessRequestItem::class.java)
                                ?.takeUnless {
                                    document.getString("accountStatus").orEmpty().equals("removed", ignoreCase = true)
                                }
                                ?: AdminAccessRequestItem(uid = uid)
                        )
                    }
            .addOnFailureListener { onFailure(it) }
    }

    fun updateSubAdminRequest(
        uid: String,
        approve: Boolean,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val updates = hashMapOf<String, Any>(
            "approvalStatus" to if (approve) "approved" else "rejected",
            "role" to if (approve) "sub_admin" else "user",
            "reviewedBy" to (auth.currentUser?.uid ?: "")
        )

        db.collection("users").document(uid)
            .set(updates, SetOptions.merge())
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun getCurrentUserProfile(
        onSuccess: (UserProfileItem) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val currentUser = auth.currentUser
        val uid = currentUser?.uid.orEmpty()
        if (uid.isBlank()) {
            onFailure(Exception("No signed-in user found."))
            return
        }

        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val profile = mapUserProfile(doc.id, doc.data.orEmpty())
                    .copy(email = doc.getString("email").orEmpty().ifBlank { currentUser?.email.orEmpty() })

                if (profile.accountStatus.equals("removed", ignoreCase = true)) {
                    auth.signOut()
                    onFailure(Exception("This account was removed by the main admin."))
                } else {
                    onSuccess(profile)
                }
            }
            .addOnFailureListener { onFailure(it) }
    }

    fun getManageableAccounts(
        onSuccess: (List<UserProfileItem>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("users").get()
            .addOnSuccessListener { result ->
                val items = result.documents.map { document ->
                    mapUserProfile(document.id, document.data.orEmpty())
                }
                    .filterNot { it.role.equals("admin", ignoreCase = true) }
                    .filterNot { it.accountStatus.equals("removed", ignoreCase = true) }
                    .sortedWith(
                        compareBy<UserProfileItem>(
                            { accountTypeSort(it) },
                            { it.name.ifBlank { it.email }.lowercase() }
                        )
                    )
                onSuccess(items)
            }
            .addOnFailureListener { onFailure(it) }
    }

    fun removeManagedAccount(
        uid: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val reviewerUid = auth.currentUser?.uid.orEmpty()
        if (uid.isBlank()) {
            onFailure(Exception("Missing account id."))
            return
        }
        if (reviewerUid.isBlank()) {
            onFailure(Exception("No signed-in admin found."))
            return
        }
        if (uid == reviewerUid) {
            onFailure(Exception("You cannot remove the current main admin account."))
            return
        }

        val userRef = db.collection("users").document(uid)
        userRef.get()
            .addOnSuccessListener { document ->
                val role = document.getString("role").orEmpty()
                if (role.equals("admin", ignoreCase = true)) {
                    onFailure(Exception("Main admin accounts cannot be removed."))
                    return@addOnSuccessListener
                }

                userRef.set(
                    mapOf(
                        "accountStatus" to "removed",
                        "role" to "removed",
                        "requestedRole" to "",
                        "approvalStatus" to "removed",
                        "reviewedBy" to reviewerUid,
                        "removedBy" to reviewerUid
                    ),
                    SetOptions.merge()
                )
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { onFailure(it) }
            }
            .addOnFailureListener { onFailure(it) }
    }

    fun updateCurrentUserProfile(
        profile: UserProfileItem,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val currentUser = auth.currentUser
        val uid = currentUser?.uid.orEmpty()
        if (uid.isBlank()) {
            onFailure(Exception("No signed-in user found."))
            return
        }

        val trimmedName = profile.name.trim()
        val trimmedEmail = profile.email.trim()
        if (trimmedName.isBlank() || trimmedEmail.isBlank()) {
            onFailure(Exception("Name and email cannot be empty."))
            return
        }

        val updates = hashMapOf<String, Any>(
            "uid" to uid,
            "name" to trimmedName,
            "email" to trimmedEmail,
            "idNumber" to profile.idNumber.trim(),
            "phone" to profile.phone.trim(),
            "department" to profile.department.trim(),
            "session" to profile.session.trim()
        )

        val saveToFirestore = {
            db.collection("users").document(uid)
                .set(updates, SetOptions.merge())
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { onFailure(it) }
        }

        val currentEmail = currentUser?.email.orEmpty()
        if (trimmedEmail.equals(currentEmail, ignoreCase = true) || currentEmail.isBlank()) {
            saveToFirestore()
        } else {
            currentUser?.updateEmail(trimmedEmail)
                ?.addOnSuccessListener { saveToFirestore() }
                ?.addOnFailureListener {
                    onFailure(
                        Exception(
                            it.message ?: "Email update failed. Please sign in again and try once more."
                        )
                    )
                }
        }
    }

    fun getPendingContentRequests(
        onSuccess: (List<ContentApprovalRequestItem>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("content_requests")
            .whereEqualTo("approvalStatus", "pending")
            .get()
            .addOnSuccessListener { result ->
                val items = result.documents.map { document ->
                    mapContentRequest(document.id, document.data.orEmpty())
                }.sortedWith(
                    compareBy<ContentApprovalRequestItem>({ it.requestType }, { it.submittedByName.lowercase() }, { it.summary.lowercase() })
                )
                onSuccess(items)
            }
            .addOnFailureListener { onFailure(it) }
    }

    fun getCurrentUserContentRequests(
        onSuccess: (List<ContentApprovalRequestItem>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val uid = auth.currentUser?.uid.orEmpty()
        if (uid.isBlank()) {
            onFailure(Exception("No signed-in user found."))
            return
        }

        db.collection("content_requests")
            .whereEqualTo("submittedByUid", uid)
            .get()
            .addOnSuccessListener { result ->
                val items = result.documents.map { document ->
                    mapContentRequest(document.id, document.data.orEmpty())
                }.sortedWith(
                    compareByDescending<ContentApprovalRequestItem> {
                        statusPriority(it.approvalStatus)
                    }.thenBy { it.requestType.lowercase() }
                        .thenBy { it.summary.lowercase() }
                )
                onSuccess(items)
            }
            .addOnFailureListener { onFailure(it) }
    }

    fun updateContentRequest(
        requestId: String,
        approve: Boolean,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val requestRef = db.collection("content_requests").document(requestId)
        if (!approve) {
            requestRef.set(
                mapOf(
                    "approvalStatus" to "rejected",
                    "reviewedBy" to (auth.currentUser?.uid ?: "")
                ),
                SetOptions.merge()
            )
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { onFailure(it) }
            return
        }

        requestRef.get()
            .addOnSuccessListener { document ->
                val requestType = document.getString("requestType").orEmpty()
                val payload = (document.get("payload") as? Map<*, *>)?.entries?.associate { entry ->
                    entry.key.toString() to entry.value
                } ?: emptyMap()

                applyApprovedContentRequest(
                    requestType = requestType,
                    payload = payload,
                    onSuccess = {
                        requestRef.set(
                            mapOf(
                                "approvalStatus" to "approved",
                                "reviewedBy" to (auth.currentUser?.uid ?: "")
                            ),
                            SetOptions.merge()
                        )
                            .addOnSuccessListener { onSuccess() }
                            .addOnFailureListener { onFailure(it) }
                    },
                    onFailure = onFailure
                )
            }
            .addOnFailureListener { onFailure(it) }
    }

    fun submitNoteForReview(
        note: NoteItem,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        handleContentSubmission(
            requestType = "note",
            summary = "${note.subject.ifBlank { "Unknown Subject" }} • ${note.noteTitle.ifBlank { "Untitled Note" }}",
            payload = mapOf(
                "subject" to note.subject,
                "level" to note.level,
                "term" to note.term,
                "noteTitle" to note.noteTitle,
                "description" to note.description
            ),
            onApproveDirectly = { saveNoteDirect(note, { onSuccess("Note published successfully.") }, onFailure) },
            onQueued = onSuccess,
            onFailure = onFailure
        )
    }

    fun submitQuestionForReview(
        question: QuestionItem,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        handleContentSubmission(
            requestType = "question",
            summary = "${question.subject.ifBlank { "Unknown Subject" }} • ${question.level.ifBlank { "Level" }} • ${question.term.ifBlank { "Term" }}",
            payload = mapOf(
                "subject" to question.subject,
                "level" to question.level,
                "term" to question.term,
                "session" to question.session,
                "years" to question.years
            ),
            onApproveDirectly = { saveQuestionDirect(question, { onSuccess("Question data published successfully.") }, onFailure) },
            onQueued = onSuccess,
            onFailure = onFailure
        )
    }

    fun submitRoutineForReview(
        routine: RoutineItem,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        handleContentSubmission(
            requestType = "routine",
            summary = "${routine.level.ifBlank { "Level" }} • ${routine.term.ifBlank { "Term" }} • ${routine.section.ifBlank { "Section" }}",
            payload = mapOf(
                "day" to routine.day,
                "level" to routine.level,
                "term" to routine.term,
                "section" to routine.section,
                "classes" to routine.classes,
                "uploadName" to routine.uploadName,
                "fileUrl" to routine.fileUrl,
                "storagePath" to routine.storagePath,
                "fileType" to routine.fileType
            ),
            onApproveDirectly = { saveRoutineDirect(routine, { onSuccess("Routine published successfully.") }, onFailure) },
            onQueued = onSuccess,
            onFailure = onFailure
        )
    }

    fun submitRoutineUploadForReview(
        routine: RoutineItem,
        fileUri: Uri,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val uid = auth.currentUser?.uid.orEmpty()
        if (uid.isBlank()) {
            onFailure(Exception("No signed-in user found."))
            return
        }

        val safeName = routine.uploadName.ifBlank { "routine_upload" }
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
        val fileId = "${UUID.randomUUID()}_$safeName"
        val ref = storage.reference.child("routine_uploads/$uid/$fileId")

        ref.putFile(fileUri)
            .addOnSuccessListener {
                val routineWithStoragePath = routine.copy(storagePath = ref.path)
                ref.downloadUrl.addOnSuccessListener { downloadUri ->
                    submitRoutineForReview(
                        routine = routineWithStoragePath.copy(fileUrl = downloadUri.toString()),
                        onSuccess = onSuccess,
                        onFailure = onFailure
                    )
                }.addOnFailureListener {
                    submitRoutineForReview(
                        routine = routineWithStoragePath,
                        onSuccess = onSuccess,
                        onFailure = onFailure
                    )
                }
            }
            .addOnFailureListener { onFailure(it) }
    }

    fun downloadRoutineFile(
        routine: RoutineItem,
        destinationFile: java.io.File,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val storagePath = routine.storagePath
        if (storagePath.isBlank()) {
            onFailure(Exception("Routine file path is missing."))
            return
        }

        storage.reference.child(storagePath).getFile(destinationFile)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun getRoutineDownloadUrl(
        routine: RoutineItem,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        if (routine.fileUrl.isNotBlank()) {
            onSuccess(routine.fileUrl)
            return
        }

        val storagePath = routine.storagePath
        if (storagePath.isBlank()) {
            onFailure(Exception("Routine file path is missing."))
            return
        }

        storage.reference.child(storagePath).downloadUrl
            .addOnSuccessListener { onSuccess(it.toString()) }
            .addOnFailureListener { onFailure(it) }
    }

    fun submitCalendarForReview(
        calendar: CalendarItem,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        handleContentSubmission(
            requestType = "calendar",
            summary = "${calendar.title.ifBlank { "Calendar Event" }} • ${calendar.date.ifBlank { "Date" }}",
            payload = mapOf(
                "title" to calendar.title,
                "date" to calendar.date,
                "type" to calendar.type
            ),
            onApproveDirectly = { saveCalendarDirect(calendar, { onSuccess("Calendar event published successfully.") }, onFailure) },
            onQueued = onSuccess,
            onFailure = onFailure
        )
    }

    fun submitCtSlotForReview(
        ctSlot: CtSlotItem,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        handleContentSubmission(
            requestType = "ct_slot",
            summary = "${ctSlot.date.ifBlank { "Date" }} • ${ctSlot.room.ifBlank { "Room" }} • ${ctSlot.courseLabel.ifBlank { "Label" }}",
            payload = mapOf(
                "id" to ctSlot.id,
                "weekNo" to ctSlot.weekNo,
                "date" to ctSlot.date,
                "room" to ctSlot.room,
                "courseLabel" to ctSlot.courseLabel
            ),
            onApproveDirectly = { saveCtSlotDirect(ctSlot, { onSuccess("CT slot published successfully.") }, onFailure) },
            onQueued = onSuccess,
            onFailure = onFailure
        )
    }

    fun logout() {
        auth.signOut()
    }

    // --- Data Retrieval Functions ---

    fun getQuestions(onSuccess: (List<QuestionItem>) -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("question_bank").get()
            .addOnSuccessListener { result ->
                val list = result.documents.mapNotNull { it.toObject(QuestionItem::class.java) }
                onSuccess(list)
            }
            .addOnFailureListener { onFailure(it) }
    }

    fun getRoutine(onSuccess: (List<RoutineItem>) -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("routine").get()
            .addOnSuccessListener { result ->
                val list = result.documents.mapNotNull { it.toObject(RoutineItem::class.java) }
                onSuccess(list)
            }
            .addOnFailureListener { onFailure(it) }
    }

    fun getCalendar(onSuccess: (List<CalendarItem>) -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("calendar").get()
            .addOnSuccessListener { result ->
                val list = result.documents.mapNotNull { it.toObject(CalendarItem::class.java) }
                onSuccess(list)
            }
            .addOnFailureListener { onFailure(it) }
    }

    fun getNotes(onSuccess: (List<NoteItem>) -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("notes").get()
            .addOnSuccessListener { result ->
                val list = result.documents.mapNotNull { it.toObject(NoteItem::class.java) }
                onSuccess(list)
            }
            .addOnFailureListener { onFailure(it) }
    }

    fun getCtSlots(onSuccess: (List<CtSlotItem>) -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("ct_slots").get()
            .addOnSuccessListener { result ->
                val list = result.documents.mapNotNull { it.toObject(CtSlotItem::class.java) }
                onSuccess(list)
            }
            .addOnFailureListener { onFailure(it) }
    }

    // --- Data Addition Functions ---

    fun addNote(note: NoteItem, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        saveNoteDirect(note, onSuccess, onFailure)
    }

    fun addQuestion(question: QuestionItem, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        saveQuestionDirect(question, onSuccess, onFailure)
    }

    fun addRoutine(routine: RoutineItem, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        saveRoutineDirect(routine, onSuccess, onFailure)
    }

    fun addCalendar(calendar: CalendarItem, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        saveCalendarDirect(calendar, onSuccess, onFailure)
    }

    fun addCtSlot(ctSlot: CtSlotItem, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        saveCtSlotDirect(ctSlot, onSuccess, onFailure)
    }

    private fun handleContentSubmission(
        requestType: String,
        summary: String,
        payload: Map<String, Any>,
        onApproveDirectly: () -> Unit,
        onQueued: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        getCurrentUserMetadata(
            onSuccess = { uid, name, email, role ->
                if (role == "admin") {
                    onApproveDirectly()
                } else {
                    val requestData = hashMapOf(
                        "requestType" to requestType,
                        "summary" to summary,
                        "submittedByUid" to uid,
                        "submittedByName" to name,
                        "submittedByEmail" to email,
                        "approvalStatus" to "pending",
                        "payload" to payload
                    )
                    db.collection("content_requests").add(requestData)
                        .addOnSuccessListener {
                            onQueued("Submission sent to the main admin for approval.")
                        }
                        .addOnFailureListener { onFailure(it) }
                }
            },
            onFailure = onFailure
        )
    }

    private fun getCurrentUserMetadata(
        onSuccess: (uid: String, name: String, email: String, role: String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val currentUser = auth.currentUser
        val uid = currentUser?.uid.orEmpty()
        if (uid.isBlank()) {
            onFailure(Exception("No signed-in user found."))
            return
        }

        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.getString("accountStatus").orEmpty().equals("removed", ignoreCase = true)) {
                    auth.signOut()
                    onFailure(Exception("This account was removed by the main admin."))
                    return@addOnSuccessListener
                }

                onSuccess(
                    uid,
                    doc.getString("name").orEmpty(),
                    doc.getString("email").orEmpty().ifBlank { currentUser?.email.orEmpty() },
                    doc.getString("role") ?: "user"
                )
            }
            .addOnFailureListener { onFailure(it) }
    }

    private fun applyApprovedContentRequest(
        requestType: String,
        payload: Map<String, Any?>,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        when (requestType) {
            "note" -> saveNoteDirect(
                note = NoteItem(
                    subject = payload["subject"]?.toString().orEmpty(),
                    level = payload["level"]?.toString().orEmpty(),
                    term = payload["term"]?.toString().orEmpty(),
                    noteTitle = payload["noteTitle"]?.toString().orEmpty(),
                    description = payload["description"]?.toString().orEmpty()
                ),
                onSuccess = onSuccess,
                onFailure = onFailure
            )

            "question" -> saveQuestionDirect(
                question = QuestionItem(
                    subject = payload["subject"]?.toString().orEmpty(),
                    level = payload["level"]?.toString().orEmpty(),
                    term = payload["term"]?.toString().orEmpty(),
                    session = payload["session"]?.toString().orEmpty(),
                    years = (payload["years"] as? List<*>)?.map { it.toString() } ?: emptyList()
                ),
                onSuccess = onSuccess,
                onFailure = onFailure
            )

            "routine" -> saveRoutineDirect(
                routine = RoutineItem(
                    day = payload["day"]?.toString().orEmpty(),
                    level = payload["level"]?.toString().orEmpty(),
                    term = payload["term"]?.toString().orEmpty(),
                    section = payload["section"]?.toString().orEmpty(),
                    classes = (payload["classes"] as? List<*>)?.map { it.toString() } ?: emptyList(),
                    uploadName = payload["uploadName"]?.toString().orEmpty(),
                    fileUrl = payload["fileUrl"]?.toString().orEmpty(),
                    storagePath = payload["storagePath"]?.toString().orEmpty(),
                    fileType = payload["fileType"]?.toString().orEmpty()
                ),
                onSuccess = onSuccess,
                onFailure = onFailure
            )

            "calendar" -> saveCalendarDirect(
                calendar = CalendarItem(
                    title = payload["title"]?.toString().orEmpty(),
                    date = payload["date"]?.toString().orEmpty(),
                    type = payload["type"]?.toString().orEmpty()
                ),
                onSuccess = onSuccess,
                onFailure = onFailure
            )

            "ct_slot" -> saveCtSlotDirect(
                ctSlot = CtSlotItem(
                    id = payload["id"]?.toString().orEmpty(),
                    weekNo = (payload["weekNo"] as? Number)?.toInt() ?: 0,
                    date = payload["date"]?.toString().orEmpty(),
                    room = payload["room"]?.toString().orEmpty(),
                    courseLabel = payload["courseLabel"]?.toString().orEmpty()
                ),
                onSuccess = onSuccess,
                onFailure = onFailure
            )

            else -> onFailure(Exception("Unknown request type: $requestType"))
        }
    }

    private fun mapContentRequest(
        id: String,
        data: Map<String, Any>
    ): ContentApprovalRequestItem {
        val payload = (data["payload"] as? Map<*, *>)?.entries?.associate { entry ->
            entry.key.toString() to entry.value
        } ?: emptyMap()

        return ContentApprovalRequestItem(
            id = id,
            requestType = data["requestType"]?.toString().orEmpty(),
            summary = data["summary"]?.toString().orEmpty(),
            submittedByUid = data["submittedByUid"]?.toString().orEmpty(),
            submittedByName = data["submittedByName"]?.toString().orEmpty(),
            submittedByEmail = data["submittedByEmail"]?.toString().orEmpty(),
            approvalStatus = data["approvalStatus"]?.toString().orEmpty(),
            reviewedBy = data["reviewedBy"]?.toString().orEmpty(),
            payload = payload
        )
    }

    private fun statusPriority(status: String): Int {
        return when (status.lowercase()) {
            "pending" -> 3
            "rejected" -> 2
            "approved" -> 1
            else -> 0
        }
    }

    private fun mapUserProfile(documentId: String, data: Map<String, Any?>): UserProfileItem {
        return UserProfileItem(
            uid = data["uid"]?.toString().orEmpty().ifBlank { documentId },
            name = data["name"]?.toString().orEmpty(),
            email = data["email"]?.toString().orEmpty(),
            idNumber = data["idNumber"]?.toString().orEmpty(),
            phone = data["phone"]?.toString().orEmpty(),
            department = data["department"]?.toString().orEmpty(),
            session = data["session"]?.toString().orEmpty(),
            role = data["role"]?.toString().orEmpty().ifBlank { "user" },
            requestedRole = data["requestedRole"]?.toString().orEmpty(),
            approvalStatus = data["approvalStatus"]?.toString().orEmpty(),
            reviewedBy = data["reviewedBy"]?.toString().orEmpty(),
            accountStatus = data["accountStatus"]?.toString().orEmpty().ifBlank { "active" },
            removedBy = data["removedBy"]?.toString().orEmpty()
        )
    }

    private fun accountTypeSort(profile: UserProfileItem): Int {
        return when {
            profile.requestedRole.equals("sub_admin", ignoreCase = true) ||
                profile.role.equals("sub_admin", ignoreCase = true) -> 0
            else -> 1
        }
    }

    private fun saveNoteDirect(note: NoteItem, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val id = note.id.ifEmpty { UUID.randomUUID().toString() }
        db.collection("notes").document(id).set(note.copy(id = id))
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    private fun saveQuestionDirect(question: QuestionItem, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val id = question.id.ifEmpty { UUID.randomUUID().toString() }
        db.collection("question_bank").document(id).set(question.copy(id = id))
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    private fun saveRoutineDirect(routine: RoutineItem, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val id = routine.id.ifEmpty { UUID.randomUUID().toString() }
        db.collection("routine").document(id).set(routine.copy(id = id))
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    private fun saveCalendarDirect(calendar: CalendarItem, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val id = calendar.id.ifEmpty { UUID.randomUUID().toString() }
        db.collection("calendar").document(id).set(calendar.copy(id = id))
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    private fun saveCtSlotDirect(ctSlot: CtSlotItem, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val id = ctSlot.id.ifEmpty { UUID.randomUUID().toString() }
        db.collection("ct_slots").document(id).set(ctSlot.copy(id = id))
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun deleteCtSlot(id: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("ct_slots").document(id).delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    // --- Seeding Function ---

    fun seedAllData(onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) {
        val batch = db.batch()

        val notes = listOf(
            NoteItem(id = "n1", subject = "Structured Programming", level = "Level 1", term = "Term 1", noteTitle = "Basics", description = "Intro to C")
        )
        val ctSlots = listOf(
            CtSlotItem(id = "ct_22_feb_307", weekNo = 4, date = "22-Feb", room = "307 (35)", courseLabel = "3-I"),
            CtSlotItem(id = "ct_22_feb_407", weekNo = 4, date = "22-Feb", room = "407 (40)", courseLabel = "3-I"),
            CtSlotItem(id = "ct_26_feb_307", weekNo = 4, date = "26-Feb", room = "307 (35)", courseLabel = "3-I"),
            CtSlotItem(id = "ct_26_feb_407", weekNo = 4, date = "26-Feb", room = "407 (40)", courseLabel = "3-I"),
            CtSlotItem(id = "ct_3_mar_307", weekNo = 5, date = "3-Mar", room = "307 (35)", courseLabel = "3-I"),
            CtSlotItem(id = "ct_3_mar_407", weekNo = 5, date = "3-Mar", room = "407 (40)", courseLabel = "3-I"),
            CtSlotItem(id = "ct_5_mar_302", weekNo = 5, date = "5-Mar", room = "302 (45)", courseLabel = "3-I"),
            CtSlotItem(id = "ct_5_mar_311", weekNo = 5, date = "5-Mar", room = "311 (45)", courseLabel = "3-I"),
            CtSlotItem(id = "ct_10_mar_307", weekNo = 6, date = "10-Mar", room = "307 (35)", courseLabel = "3-I"),
            CtSlotItem(id = "ct_10_mar_407", weekNo = 6, date = "10-Mar", room = "407 (40)", courseLabel = "3-I"),
            CtSlotItem(id = "ct_29_mar_307", weekNo = 7, date = "29-Mar", room = "307 (35)", courseLabel = "3-I"),
            CtSlotItem(id = "ct_29_mar_407", weekNo = 7, date = "29-Mar", room = "407 (40)", courseLabel = "3-I"),
            CtSlotItem(id = "ct_30_mar_302", weekNo = 7, date = "30-Mar", room = "302 (45)", courseLabel = "3-I"),
            CtSlotItem(id = "ct_30_mar_311", weekNo = 7, date = "30-Mar", room = "311 (45)", courseLabel = "3-I"),
            CtSlotItem(id = "ct_2_apr_307", weekNo = 7, date = "2-Apr", room = "307 (35)", courseLabel = "3-I"),
            CtSlotItem(id = "ct_2_apr_407", weekNo = 7, date = "2-Apr", room = "407 (40)", courseLabel = "3-I"),
            CtSlotItem(id = "ct_15_apr_307", weekNo = 8, date = "15-Apr", room = "307 (35)", courseLabel = "3-I"),
            CtSlotItem(id = "ct_15_apr_407", weekNo = 8, date = "15-Apr", room = "407 (40)", courseLabel = "3-I"),
            CtSlotItem(id = "ct_19_apr_307", weekNo = 9, date = "19-Apr", room = "307 (35)", courseLabel = "3-I"),
            CtSlotItem(id = "ct_19_apr_407", weekNo = 9, date = "19-Apr", room = "407 (40)", courseLabel = "3-I"),
            CtSlotItem(id = "ct_23_apr_307", weekNo = 9, date = "23-Apr", room = "307 (35)", courseLabel = "3-I"),
            CtSlotItem(id = "ct_23_apr_407", weekNo = 9, date = "23-Apr", room = "407 (40)", courseLabel = "3-I"),
            CtSlotItem(id = "ct_26_apr_302", weekNo = 10, date = "26-Apr", room = "302 (45)", courseLabel = "3-I"),
            CtSlotItem(id = "ct_26_apr_311", weekNo = 10, date = "26-Apr", room = "311 (45)", courseLabel = "3-I"),
            CtSlotItem(id = "ct_28_apr_307", weekNo = 10, date = "28-Apr", room = "307 (35)", courseLabel = "3-I"),
            CtSlotItem(id = "ct_28_apr_407", weekNo = 10, date = "28-Apr", room = "407 (40)", courseLabel = "3-I"),
            CtSlotItem(id = "ct_3_may_307", weekNo = 11, date = "3-May", room = "307 (35)", courseLabel = "3-I"),
            CtSlotItem(id = "ct_3_may_407", weekNo = 11, date = "3-May", room = "407 (40)", courseLabel = "3-I"),
            CtSlotItem(id = "ct_5_may_302", weekNo = 11, date = "5-May", room = "302 (45)", courseLabel = "3-I"),
            CtSlotItem(id = "ct_5_may_311", weekNo = 11, date = "5-May", room = "311 (45)", courseLabel = "3-I"),
            CtSlotItem(id = "ct_7_may_307", weekNo = 11, date = "7-May", room = "307 (35)", courseLabel = "3-I"),
            CtSlotItem(id = "ct_7_may_407", weekNo = 11, date = "7-May", room = "407 (40)", courseLabel = "3-I"),
            CtSlotItem(id = "ct_12_may_307", weekNo = 12, date = "12-May", room = "307 (35)", courseLabel = "3-I"),
            CtSlotItem(id = "ct_12_may_407", weekNo = 12, date = "12-May", room = "407 (40)", courseLabel = "3-I"),
            CtSlotItem(id = "ct_14_may_302", weekNo = 12, date = "14-May", room = "302 (45)", courseLabel = "3-I"),
            CtSlotItem(id = "ct_14_may_311", weekNo = 12, date = "14-May", room = "311 (45)", courseLabel = "3-I")
        )

        notes.forEach { item ->
            val ref = db.collection("notes").document(item.id)
            batch.set(ref, item, SetOptions.merge())
        }

        ctSlots.forEach { item ->
            val ref = db.collection("ct_slots").document(item.id)
            batch.set(ref, item, SetOptions.merge())
        }

        batch.commit()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }
}
