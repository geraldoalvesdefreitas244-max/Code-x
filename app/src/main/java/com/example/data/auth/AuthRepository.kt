package com.example.data.auth

import android.app.Activity
import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.data.local.SecureKeyStore
import com.example.data.local.UserDao
import com.example.data.local.UserEntity
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class AuthRepository(
    private val context: Context,
    private val userDao: UserDao,
    private val secureKeyStore: SecureKeyStore
) {

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val currentUser: Flow<UserEntity?> = secureKeyStore.activeUserId.flatMapLatest { uid ->
        if (uid.isNullOrBlank()) flowOf(null) else userDao.observeById(uid)
    }

    fun isFirebaseGoogleAuthConfigured(): Boolean {
        val resId = context.resources.getIdentifier(
            "default_web_client_id",
            "string",
            context.packageName
        )
        return resId != 0 && FirebaseApp.getApps(context).isNotEmpty()
    }

    suspend fun registerLocalAccount(name: String, email: String, password: String): Result<UserEntity> {
        return runCatching {
            val cleanName = name.trim()
            val cleanEmail = email.trim().lowercase()
            require(cleanName.length >= 2) { "Informe um nome com pelo menos 2 caracteres." }
            require(cleanEmail.contains("@") && cleanEmail.contains(".")) { "E-mail inválido." }
            require(password.length >= 6) { "A senha deve ter no mínimo 6 caracteres." }

            val existing = userDao.getByEmail(cleanEmail)
            require(existing == null) { "Já existe uma conta cadastrada com este e-mail." }

            val salt = generateSalt()
            val hash = hashPassword(password, salt)
            val verificationCode = (100000..999999).random().toString()
            val user = UserEntity(
                id = "usr_${UUID.randomUUID().toString().take(12)}",
                name = cleanName,
                email = cleanEmail,
                passwordHash = hash,
                passwordSalt = salt,
                isEmailVerified = false,
                verificationCode = verificationCode,
                authProvider = "LOCAL_EMAIL",
                cloudSyncEnabled = false,
                createdAt = System.currentTimeMillis()
            )
            userDao.upsert(user)
            secureKeyStore.setActiveUserId(user.id)
            user
        }
    }

    suspend fun loginWithEmailAndPassword(email: String, password: String): Result<UserEntity> {
        return runCatching {
            val cleanEmail = email.trim().lowercase()
            require(cleanEmail.isNotBlank() && password.isNotBlank()) { "Preencha e-mail e senha." }
            val user = userDao.getByEmail(cleanEmail)
                ?: error("Nenhuma conta encontrada para este e-mail.")
            require(user.authProvider == "LOCAL_EMAIL" && user.passwordSalt.isNotBlank()) {
                "Esta conta utiliza login com Google."
            }
            val candidateHash = hashPassword(password, user.passwordSalt)
            require(candidateHash == user.passwordHash) { "Senha incorreta." }
            secureKeyStore.setActiveUserId(user.id)
            user
        }
    }

    suspend fun requestPasswordRecoveryCode(email: String): Result<String> {
        return runCatching {
            val cleanEmail = email.trim().lowercase()
            val user = userDao.getByEmail(cleanEmail)
                ?: error("E-mail não cadastrado neste dispositivo.")
            val code = (100000..999999).random().toString()
            userDao.update(user.copy(verificationCode = code))
            code
        }
    }

    suspend fun resetPasswordWithCode(email: String, code: String, newPassword: String): Result<Unit> {
        return runCatching {
            val cleanEmail = email.trim().lowercase()
            require(newPassword.length >= 6) { "A nova senha deve ter no mínimo 6 caracteres." }
            val user = userDao.getByEmail(cleanEmail) ?: error("Conta não encontrada.")
            require(user.verificationCode.isNotBlank() && user.verificationCode == code.trim()) {
                "Código de verificação inválido."
            }
            val newSalt = generateSalt()
            val newHash = hashPassword(newPassword, newSalt)
            userDao.update(
                user.copy(
                    passwordHash = newHash,
                    passwordSalt = newSalt,
                    verificationCode = ""
                )
            )
        }
    }

    suspend fun verifyEmailCode(userId: String, code: String): Result<UserEntity> {
        return runCatching {
            val user = userDao.getById(userId) ?: error("Usuário não encontrado.")
            require(user.verificationCode == code.trim()) { "Código de verificação incorreto." }
            val updated = user.copy(isEmailVerified = true, verificationCode = "")
            userDao.update(updated)
            updated
        }
    }

    suspend fun generateNewEmailVerificationCode(userId: String): Result<String> {
        return runCatching {
            val user = userDao.getById(userId) ?: error("Usuário não encontrado.")
            val code = (100000..999999).random().toString()
            userDao.update(user.copy(verificationCode = code))
            code
        }
    }

    suspend fun signInWithGoogle(activity: Activity): Result<UserEntity> {
        return runCatching {
            val resId = context.resources.getIdentifier(
                "default_web_client_id",
                "string",
                context.packageName
            )
            if (resId == 0 || FirebaseApp.getApps(context).isEmpty()) {
                error(
                    "Google Sign-In / Firebase não provisionado neste ambiente. " +
                        "Adicione o arquivo google-services.json ao projeto ou entre com E-mail e Senha."
                )
            }
            val clientId = context.getString(resId)
            val credentialManager = CredentialManager.create(activity)
            val googleOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
            val request = GetCredentialRequest.Builder().addCredentialOption(googleOption).build()

            val result = try {
                credentialManager.getCredential(activity, request)
            } catch (e: GetCredentialCancellationException) {
                Log.w("Auth", "Google Sign-In cancelado pelo usuário: ${e.message}", e)
                error("Login com Google cancelado.")
            }

            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleCred = GoogleIdTokenCredential.createFrom(credential.data)
                val firebaseCred = GoogleAuthProvider.getCredential(googleCred.idToken, null)
                val authResult = FirebaseAuth.getInstance().signInWithCredential(firebaseCred).await()
                val fbUser = authResult.user ?: error("Usuário Firebase ausente.")
                val user = UserEntity(
                    id = fbUser.uid,
                    name = fbUser.displayName ?: googleCred.displayName ?: "Desenvolvedor",
                    email = fbUser.email ?: googleCred.id,
                    passwordHash = "",
                    passwordSalt = "",
                    isEmailVerified = fbUser.isEmailVerified,
                    verificationCode = "",
                    authProvider = "GOOGLE",
                    cloudSyncEnabled = true,
                    createdAt = System.currentTimeMillis()
                )
                userDao.upsert(user)
                secureKeyStore.setActiveUserId(user.id)
                user
            } else {
                error("Tipo de credencial inesperado.")
            }
        }
    }

    suspend fun toggleCloudSync(userId: String, enabled: Boolean): Result<UserEntity> {
        return runCatching {
            val user = userDao.getById(userId) ?: error("Usuário não encontrado.")
            val updated = user.copy(cloudSyncEnabled = enabled)
            userDao.update(updated)
            updated
        }
    }

    suspend fun updateProfileName(userId: String, newName: String): Result<UserEntity> {
        return runCatching {
            val clean = newName.trim()
            require(clean.length >= 2) { "O nome deve ter pelo menos 2 caracteres." }
            val user = userDao.getById(userId) ?: error("Usuário não encontrado.")
            val updated = user.copy(name = clean)
            userDao.update(updated)
            updated
        }
    }

    suspend fun signOut() {
        withContext(NonCancellable) {
            try {
                CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
            } catch (_: Exception) {
            }
            try {
                if (FirebaseApp.getApps(context).isNotEmpty()) {
                    FirebaseAuth.getInstance().signOut()
                }
            } catch (_: Exception) {
            }
            secureKeyStore.setActiveUserId(null)
        }
    }

    private fun generateSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun hashPassword(password: String, saltBase64: String): String {
        val salt = Base64.decode(saltBase64, Base64.NO_WRAP)
        val spec = PBEKeySpec(password.toCharArray(), salt, 12_000, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val encoded = factory.generateSecret(spec).encoded
        return Base64.encodeToString(encoded, Base64.NO_WRAP)
    }
}
