package com.example.mobile_uber_fight.repositories

import android.util.Log
import com.example.mobile_uber_fight.models.User
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test

class UserRepositoryTest {
    private val mockDb = mockk<FirebaseFirestore>()
    private val mockAuth = mockk<FirebaseAuth>()
    private val mockUser = mockk<FirebaseUser>()
    private val mockCollection = mockk<CollectionReference>()
    private val mockDocRef = mockk<DocumentReference>()
    private val mockTask = mockk<Task<DocumentReference>>()
    private val mockTaskVoid = mockk<Task<Void>>()

    private lateinit var repository: UserRepository

    @Before
    fun setup() {
        mockkStatic(FirebaseAuth::class)
        mockkStatic(FirebaseFirestore::class)
        mockkStatic(FirebaseStorage::class)
        mockkStatic(FieldValue::class)
        mockkStatic(Log::class)

        every { Log.d(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>(), any()) } returns 0

        every { FirebaseAuth.getInstance() } returns mockAuth
        every { FirebaseFirestore.getInstance() } returns mockDb
        every { FirebaseStorage.getInstance() } returns mockk()
        every { FieldValue.serverTimestamp() } returns mockk()

        every { mockAuth.currentUser } returns mockUser
        every { mockUser.uid } returns "user123"

        repository = UserRepository()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `submitRating success calls onSuccess callback and saves correct data`() {
        val targetUserId = "fighter456"
        val fightId = "fight_789"
        val rating = 4.5f
        val comment = "Great fight!"

        val onSuccess = mockk<() -> Unit>(relaxed = true)
        val onFailure = mockk<(Exception) -> Unit>(relaxed = true)

        val capturedData = slot<Any>()

        every { mockDb.collection("reviews") } returns mockCollection
        every { mockCollection.add(capture(capturedData)) } returns mockTask

        every { mockTask.addOnSuccessListener(any()) } answers {
            val listener = firstArg<OnSuccessListener<DocumentReference>>()
            listener.onSuccess(mockDocRef)
            mockTask
        }
        every { mockTask.addOnFailureListener(any()) } returns mockTask

        repository.submitRating(targetUserId, fightId, rating, comment, onSuccess, onFailure)

        verify { mockCollection.add(any()) }

        val dataMap = capturedData.captured as HashMap<*, *>
        assert(dataMap["rating"] == 4.5)
        assert(dataMap["comment"] == comment)
        assert(dataMap["fightId"] == fightId)
        assert(dataMap["fromUserId"] == "user123")
        assert(dataMap["toUserId"] == targetUserId)

        verify { onSuccess() }
        verify(exactly = 0) { onFailure(any()) }
    }

    @Test
    fun `submitRating failure calls onFailure callback`() {
        val onSuccess = mockk<() -> Unit>(relaxed = true)
        val onFailure = mockk<(Exception) -> Unit>(relaxed = true)
        val exception = Exception("Firestore error")

        every { mockDb.collection("reviews") } returns mockCollection
        every { mockCollection.add(any()) } returns mockTask

        every { mockTask.addOnSuccessListener(any()) } returns mockTask
        every { mockTask.addOnFailureListener(any()) } answers {
            val listener = firstArg<OnFailureListener>()
            listener.onFailure(exception)
            mockTask
        }

        repository.submitRating("target", "fight", 3.0f, "comment", onSuccess, onFailure)

        verify { onFailure(exception) }
        verify(exactly = 0) { onSuccess() }
    }

    @Test
    fun `submitRating does nothing when user is not logged in`() {
        every { mockAuth.currentUser } returns null

        val onSuccess = mockk<() -> Unit>(relaxed = true)
        val onFailure = mockk<(Exception) -> Unit>(relaxed = true)

        repository.submitRating("target", "fight", 3.0f, "comment", onSuccess, onFailure)

        verify(exactly = 0) { mockDb.collection(any()) }
        verify(exactly = 0) { onSuccess() }
        verify(exactly = 0) { onFailure(any()) }
    }

    @Test
    fun `updateUserLocation success updates location in Firestore`() {
        every { mockDb.collection("users") } returns mockCollection
        every { mockCollection.document("user123") } returns mockDocRef
        every { mockDocRef.update(eq("location"), any()) } returns mockTaskVoid

        every { mockTaskVoid.addOnSuccessListener(any()) } answers {
            val listener = firstArg<OnSuccessListener<Void>>()
            listener.onSuccess(null)
            mockTaskVoid
        }
        every { mockTaskVoid.addOnFailureListener(any()) } returns mockTaskVoid

        repository.updateUserLocation(48.8, 2.5)

        verify { mockDocRef.update(eq("location"), any()) }
    }

    @Test
    fun `updateUserLocation does nothing when user is not logged in`() {
        every { mockAuth.currentUser } returns null

        repository.updateUserLocation(48.8, 2.5)

        verify(exactly = 0) { mockDb.collection(any()) }
    }

    @Test
    fun `getCurrentUser success returns user object`() {
        val mockDocSnapshot = mockk<DocumentSnapshot>()
        val mockTaskDoc = mockk<Task<DocumentSnapshot>>()
        val expectedUser = User(uid = "user123", username = "TestUser", email = "test@test.com")

        every { mockDb.collection("users") } returns mockCollection
        every { mockCollection.document("user123") } returns mockDocRef
        every { mockDocRef.get() } returns mockTaskDoc

        every { mockTaskDoc.addOnSuccessListener(any()) } answers {
            val listener = firstArg<OnSuccessListener<DocumentSnapshot>>()
            listener.onSuccess(mockDocSnapshot)
            mockTaskDoc
        }
        every { mockTaskDoc.addOnFailureListener(any()) } returns mockTaskDoc
        every { mockDocSnapshot.toObject(User::class.java) } returns expectedUser

        val onSuccess = mockk<(User?) -> Unit>(relaxed = true)
        val onFailure = mockk<(Exception) -> Unit>(relaxed = true)

        repository.getCurrentUser(onSuccess, onFailure)

        verify { onSuccess(expectedUser) }
        verify(exactly = 0) { onFailure(any()) }
    }

    @Test
    fun `getCurrentUser failure calls onFailure callback`() {
        val mockTaskDoc = mockk<Task<DocumentSnapshot>>()
        val exception = Exception("Firestore error")

        every { mockDb.collection("users") } returns mockCollection
        every { mockCollection.document("user123") } returns mockDocRef
        every { mockDocRef.get() } returns mockTaskDoc

        every { mockTaskDoc.addOnSuccessListener(any()) } returns mockTaskDoc
        every { mockTaskDoc.addOnFailureListener(any()) } answers {
            val listener = firstArg<OnFailureListener>()
            listener.onFailure(exception)
            mockTaskDoc
        }

        val onSuccess = mockk<(User?) -> Unit>(relaxed = true)
        val onFailure = mockk<(Exception) -> Unit>(relaxed = true)

        repository.getCurrentUser(onSuccess, onFailure)

        verify { onFailure(exception) }
        verify(exactly = 0) { onSuccess(any()) }
    }

    @Test
    fun `getCurrentUser calls onFailure when user is not logged in`() {
        every { mockAuth.currentUser } returns null

        val onSuccess = mockk<(User?) -> Unit>(relaxed = true)
        val onFailure = mockk<(Exception) -> Unit>(relaxed = true)

        repository.getCurrentUser(onSuccess, onFailure)

        verify { onFailure(match { it.message == "User not logged in" }) }
        verify(exactly = 0) { onSuccess(any()) }
    }

    @Test
    fun `updateUserProfile success calls onSuccess callback`() {
        every { mockDb.collection("users") } returns mockCollection
        every { mockCollection.document("user123") } returns mockDocRef
        every { mockDocRef.update(any<Map<String, Any>>()) } returns mockTaskVoid

        every { mockTaskVoid.addOnSuccessListener(any()) } answers {
            val listener = firstArg<OnSuccessListener<Void>>()
            listener.onSuccess(null)
            mockTaskVoid
        }
        every { mockTaskVoid.addOnFailureListener(any()) } returns mockTaskVoid

        val onSuccess = mockk<() -> Unit>(relaxed = true)
        val onFailure = mockk<(Exception) -> Unit>(relaxed = true)

        repository.updateUserProfile("NewName", "new@email.com", onSuccess, onFailure)

        verify { mockDocRef.update(mapOf("username" to "NewName", "email" to "new@email.com")) }
        verify { onSuccess() }
        verify(exactly = 0) { onFailure(any()) }
    }

    @Test
    fun `updateUserProfile failure calls onFailure callback`() {
        val exception = Exception("Update failed")

        every { mockDb.collection("users") } returns mockCollection
        every { mockCollection.document("user123") } returns mockDocRef
        every { mockDocRef.update(any<Map<String, Any>>()) } returns mockTaskVoid

        every { mockTaskVoid.addOnSuccessListener(any()) } returns mockTaskVoid
        every { mockTaskVoid.addOnFailureListener(any()) } answers {
            val listener = firstArg<OnFailureListener>()
            listener.onFailure(exception)
            mockTaskVoid
        }

        val onSuccess = mockk<() -> Unit>(relaxed = true)
        val onFailure = mockk<(Exception) -> Unit>(relaxed = true)

        repository.updateUserProfile("NewName", "new@email.com", onSuccess, onFailure)

        verify { onFailure(exception) }
        verify(exactly = 0) { onSuccess() }
    }

    @Test
    fun `updateUserProfile calls onFailure when user is not logged in`() {
        every { mockAuth.currentUser } returns null

        val onSuccess = mockk<() -> Unit>(relaxed = true)
        val onFailure = mockk<(Exception) -> Unit>(relaxed = true)

        repository.updateUserProfile("NewName", "new@email.com", onSuccess, onFailure)

        verify { onFailure(match { it.message == "User not logged in" }) }
        verify(exactly = 0) { onSuccess() }
    }
}
