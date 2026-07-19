package com.shopizzo.data.repository

import android.app.Activity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore
import com.shopizzo.data.model.UserProfile
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class AuthRepository {

    private val auth      : FirebaseAuth       = FirebaseAuth.getInstance()
    private val firestore : FirebaseFirestore  = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")

    val currentUser: FirebaseUser? get() = auth.currentUser
    val isLoggedIn: Boolean get() = auth.currentUser != null

    suspend fun register(
        email       : String,
        password    : String,
        fullName    : String,
        phoneNumber : String,
        country     : String
    ): Result<UserProfile> = try {
        val authResult = auth.createUserWithEmailAndPassword(email, password).await()
        val uid = authResult.user?.uid ?: throw Exception("Registration failed")

        val profile = UserProfile(
            uid           = uid,
            fullName      = fullName,
            email         = email,
            phoneNumber   = phoneNumber,
            country       = country,
            notificationsEnabled = true,
            createdAt     = com.google.firebase.Timestamp.now()
        )
        usersCollection.document(uid).set(profile).await()
        Result.success(profile)
    } catch (e: Exception) {
        Result.failure(mapFirebaseException(e))
    }

    suspend fun login(email: String, password: String): Result<FirebaseUser> = try {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        Result.success(result.user!!)
    } catch (e: Exception) {
        Result.failure(mapFirebaseException(e))
    }

    suspend fun updateProfile(profile: UserProfile): Result<Unit> = try {
        usersCollection.document(profile.uid).set(profile).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(mapFirebaseException(e))
    }

    suspend fun updateNotificationSetting(enabled: Boolean): Result<Unit> = try {
        val uid = auth.currentUser?.uid ?: throw Exception("Not logged in")
        usersCollection.document(uid).update("notificationsEnabled", enabled).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(mapFirebaseException(e))
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> = try {
        auth.sendPasswordResetEmail(email).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(mapFirebaseException(e))
    }

    suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser> = try {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(credential).await()
        val user = result.user!!
        
        val snapshot = usersCollection.document(user.uid).get().await()
        if (!snapshot.exists()) {
            val profile = UserProfile(
                uid = user.uid,
                fullName = user.displayName ?: "Shopizzo User",
                email = user.email ?: "",
                notificationsEnabled = true
            )
            usersCollection.document(user.uid).set(profile).await()
        }
        Result.success(user)
    } catch (e: Exception) {
        Result.failure(mapFirebaseException(e))
    }

    fun logout() = auth.signOut()

    fun getUserProfileFlow(uid: String): Flow<UserProfile?> = callbackFlow {
        val listener = usersCollection.document(uid).addSnapshotListener { snapshot, e ->
            if (e != null) {
                trySend(null)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                trySend(snapshot.toObject(UserProfile::class.java))
            } else {
                trySend(null)
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun getUserProfile(): UserProfile? {
        val uid = auth.currentUser?.uid ?: return null
        return try {
            val snapshot = usersCollection.document(uid).get().await()
            snapshot.toObject(UserProfile::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun toggleFavourite(productId: String, currentFavourites: List<String>): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
        return try {
            val isAdding = !currentFavourites.contains(productId)
            val updated = if (isAdding) {
                currentFavourites + productId
            } else {
                currentFavourites - productId
            }
            usersCollection.document(uid).update("favouriteIds", updated).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapFirebaseException(e))
        }
    }

    suspend fun setPaymentPin(uid: String, pin: String): Result<Unit> = try {
        val hashedPin = hashPin(pin)
        usersCollection.document(uid).update("paymentPin", hashedPin).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun validatePaymentPin(uid: String, pin: String): Result<Boolean> = try {
        val snapshot = usersCollection.document(uid).get().await()
        val storedPin = snapshot.getString("paymentPin")
        val hashedInput = hashPin(pin)
        Result.success(storedPin == hashedInput)
    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun hashPin(pin: String): String {
        val bytes = pin.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

    suspend fun updatePreferredAuthMethod(uid: String, method: String): Result<Unit> = try {
        usersCollection.document(uid).update("preferredAuthMethod", method).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // --- Phone Auth ---
    fun startPhoneVerification(
        phoneNumber: String,
        activity: Activity,
        callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    ) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    suspend fun signInWithPhone(verificationId: String, code: String): Result<FirebaseUser> = try {
        val credential = PhoneAuthProvider.getCredential(verificationId, code)
        signInWithCredential(credential)
    } catch (e: Exception) {
        Result.failure(mapFirebaseException(e))
    }

    suspend fun signInWithCredential(credential: AuthCredential): Result<FirebaseUser> = try {
        val result = auth.signInWithCredential(credential).await()
        val user = result.user!!
        
        val snapshot = usersCollection.document(user.uid).get().await()
        if (!snapshot.exists()) {
            val profile = UserProfile(
                uid = user.uid,
                fullName = user.displayName ?: "Phone User",
                phoneNumber = user.phoneNumber ?: "",
                notificationsEnabled = true
            )
            usersCollection.document(user.uid).set(profile).await()
        }
        Result.success(user)
    } catch (e: Exception) {
        Result.failure(mapFirebaseException(e))
    }

    private fun mapFirebaseException(e: Exception): Exception {
        return when (e) {
            is FirebaseAuthInvalidCredentialsException -> Exception("Invalid credentials. Please check your email/password.")
            is FirebaseAuthUserCollisionException -> Exception("An account with this email already exists.")
            is FirebaseAuthInvalidUserException -> Exception("No account found with this email.")
            is FirebaseException -> Exception(e.localizedMessage ?: "Authentication failed.")
            else -> e
        }
    }
}
