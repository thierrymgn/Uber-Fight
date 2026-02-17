package com.example.mobile_uber_fight.repositories

import android.util.Log
import com.example.mobile_uber_fight.logger.GrafanaLogger
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test

class FightRepositoryTest {

    private lateinit var repository: FightRepository
    private val mockDb = mockk<FirebaseFirestore>()
    private val mockAuth = mockk<FirebaseAuth>()
    private val mockUser = mockk<FirebaseUser>()
    private val mockCollection = mockk<CollectionReference>()
    private val mockDocRef = mockk<DocumentReference>()
    private val mockTaskDoc = mockk<Task<DocumentReference>>()
    private val mockTaskVoid = mockk<Task<Void>>()

    @Before
    fun setup() {
        mockkStatic(FirebaseFirestore::class)
        mockkStatic(FirebaseAuth::class)
        mockkStatic(FieldValue::class)
        mockkStatic(Log::class)
        mockkObject(GrafanaLogger)

        every { Log.i(any(), any()) } returns 0
        every { Log.d(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0

        every { FirebaseFirestore.getInstance() } returns mockDb
        every { FirebaseAuth.getInstance() } returns mockAuth
        every { FieldValue.serverTimestamp() } returns mockk()

        every { mockAuth.currentUser } returns mockUser
        every { mockUser.uid } returns "user123"
        every { mockDb.collection("fights") } returns mockCollection

        repository = FightRepository()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `createFightRequest success calls onSuccess callback`() {
        val address = "28 Av. de la République"
        val lat = 48.8
        val lng = 2.5
        val type = "Duel"
        val onSuccess = mockk<(String) -> Unit>(relaxed = true)
        val onFailure = mockk<(Exception) -> Unit>(relaxed = true)

        val capturedData = slot<HashMap<String, Any>>()
        every { mockCollection.add(capture(capturedData)) } returns mockTaskDoc
        every { mockDocRef.id } returns "new_fight_id"

        val slotSuccess = slot<OnSuccessListener<DocumentReference>>()
        every { mockTaskDoc.addOnSuccessListener(capture(slotSuccess)) } answers {
            slotSuccess.captured.onSuccess(mockDocRef)
            mockTaskDoc
        }
        every { mockTaskDoc.addOnFailureListener(any()) } returns mockTaskDoc

        repository.createFightRequest(address, lat, lng, type, onSuccess, onFailure)

        verify { mockCollection.add(any()) }
        assert(capturedData.captured["address"] == address)
        assert(capturedData.captured["fightType"] == type)
        assert(capturedData.captured["requesterId"] == "user123")
        verify { onSuccess("new_fight_id") }
    }

    @Test
    fun `createFightRequest failure calls onFailure callback`() {
        val exception = Exception("Firestore error")
        val onSuccess = mockk<(String) -> Unit>(relaxed = true)
        val onFailure = mockk<(Exception) -> Unit>(relaxed = true)

        every { mockCollection.add(any()) } returns mockTaskDoc

        every { mockTaskDoc.addOnSuccessListener(any()) } returns mockTaskDoc
        every { mockTaskDoc.addOnFailureListener(any()) } answers {
            val listener = firstArg<OnFailureListener>()
            listener.onFailure(exception)
            mockTaskDoc
        }

        repository.createFightRequest("addr", 0.0, 0.0, "Duel", onSuccess, onFailure)

        verify { onFailure(exception) }
        verify(exactly = 0) { onSuccess(any()) }
    }

    @Test
    fun `createFightRequest calls onFailure when user is not logged in`() {
        every { mockAuth.currentUser } returns null

        val onSuccess = mockk<(String) -> Unit>(relaxed = true)
        val onFailure = mockk<(Exception) -> Unit>(relaxed = true)

        repository.createFightRequest("addr", 0.0, 0.0, "Duel", onSuccess, onFailure)

        verify { onFailure(match { it.message == "Utilisateur non connecté" }) }
        verify(exactly = 0) { onSuccess(any()) }
    }


    @Test
    fun `updateFightStatus success calls onSuccess callback`() {
        val fightId = "fight_abc"
        val newStatus = "COMPLETED"
        val onSuccess = mockk<() -> Unit>(relaxed = true)

        every { mockCollection.document(fightId) } returns mockDocRef
        every { mockDocRef.update("status", newStatus) } returns mockTaskVoid

        val slotSuccess = slot<OnSuccessListener<Void>>()
        every { mockTaskVoid.addOnSuccessListener(capture(slotSuccess)) } answers {
            slotSuccess.captured.onSuccess(null)
            mockTaskVoid
        }
        every { mockTaskVoid.addOnFailureListener(any()) } returns mockTaskVoid

        repository.updateFightStatus(fightId, newStatus, onSuccess)

        verify { mockDocRef.update("status", newStatus) }
        verify { onSuccess() }
    }

    @Test
    fun `updateFightStatus failure calls onFailure callback`() {
        val fightId = "fight_abc"
        val newStatus = "COMPLETED"
        val exception = Exception("Update failed")
        val onSuccess = mockk<() -> Unit>(relaxed = true)
        val onFailure = mockk<(Exception) -> Unit>(relaxed = true)

        every { mockCollection.document(fightId) } returns mockDocRef
        every { mockDocRef.update("status", newStatus) } returns mockTaskVoid

        every { mockTaskVoid.addOnSuccessListener(any()) } returns mockTaskVoid
        every { mockTaskVoid.addOnFailureListener(any()) } answers {
            val listener = firstArg<OnFailureListener>()
            listener.onFailure(exception)
            mockTaskVoid
        }

        repository.updateFightStatus(fightId, newStatus, onSuccess, onFailure)

        verify { onFailure(exception) }
        verify(exactly = 0) { onSuccess() }
    }

    @Test
    fun `cancelFight success calls onSuccess callback`() {
        val fightId = "fight_123"
        val onSuccess = mockk<() -> Unit>(relaxed = true)
        val onFailure = mockk<(Exception) -> Unit>(relaxed = true)

        every { mockCollection.document(fightId) } returns mockDocRef
        every { mockDocRef.update("status", "CANCELLED") } returns mockTaskVoid

        every { mockTaskVoid.addOnSuccessListener(any()) } answers {
            val listener = firstArg<OnSuccessListener<Void>>()
            listener.onSuccess(null)
            mockTaskVoid
        }
        every { mockTaskVoid.addOnFailureListener(any()) } returns mockTaskVoid

        repository.cancelFight(fightId, onSuccess, onFailure)

        verify { mockDocRef.update("status", "CANCELLED") }
        verify { onSuccess() }
        verify(exactly = 0) { onFailure(any()) }
    }

    @Test
    fun `cancelFight failure calls onFailure callback`() {
        val fightId = "fight_123"
        val exception = Exception("Cancel failed")
        val onSuccess = mockk<() -> Unit>(relaxed = true)
        val onFailure = mockk<(Exception) -> Unit>(relaxed = true)

        every { mockCollection.document(fightId) } returns mockDocRef
        every { mockDocRef.update("status", "CANCELLED") } returns mockTaskVoid

        every { mockTaskVoid.addOnSuccessListener(any()) } returns mockTaskVoid
        every { mockTaskVoid.addOnFailureListener(any()) } answers {
            val listener = firstArg<OnFailureListener>()
            listener.onFailure(exception)
            mockTaskVoid
        }

        repository.cancelFight(fightId, onSuccess, onFailure)

        verify { onFailure(exception) }
        verify(exactly = 0) { onSuccess() }
    }

    @Test
    fun `acceptFight success calls onSuccess callback`() {
        val fightId = "fight_456"
        val onSuccess = mockk<() -> Unit>(relaxed = true)
        val onFailure = mockk<(Exception) -> Unit>(relaxed = true)

        every { mockCollection.document(fightId) } returns mockDocRef
        every { mockDocRef.update(any<Map<String, Any>>()) } returns mockTaskVoid

        every { mockTaskVoid.addOnSuccessListener(any()) } answers {
            val listener = firstArg<OnSuccessListener<Void>>()
            listener.onSuccess(null)
            mockTaskVoid
        }
        every { mockTaskVoid.addOnFailureListener(any()) } returns mockTaskVoid

        repository.acceptFight(fightId, onSuccess, onFailure)

        verify { mockDocRef.update(mapOf("status" to "ACCEPTED", "fighterId" to "user123")) }
        verify { onSuccess() }
        verify(exactly = 0) { onFailure(any()) }
    }

    @Test
    fun `acceptFight failure calls onFailure callback`() {
        val fightId = "fight_456"
        val exception = Exception("Accept failed")
        val onSuccess = mockk<() -> Unit>(relaxed = true)
        val onFailure = mockk<(Exception) -> Unit>(relaxed = true)

        every { mockCollection.document(fightId) } returns mockDocRef
        every { mockDocRef.update(any<Map<String, Any>>()) } returns mockTaskVoid

        every { mockTaskVoid.addOnSuccessListener(any()) } returns mockTaskVoid
        every { mockTaskVoid.addOnFailureListener(any()) } answers {
            val listener = firstArg<OnFailureListener>()
            listener.onFailure(exception)
            mockTaskVoid
        }

        repository.acceptFight(fightId, onSuccess, onFailure)

        verify { onFailure(exception) }
        verify(exactly = 0) { onSuccess() }
    }

    @Test
    fun `acceptFight calls onFailure when user is not logged in`() {
        every { mockAuth.currentUser } returns null

        val onSuccess = mockk<() -> Unit>(relaxed = true)
        val onFailure = mockk<(Exception) -> Unit>(relaxed = true)

        repository.acceptFight("fight_456", onSuccess, onFailure)

        verify { onFailure(match { it.message == "Impossible d'accepter: utilisateur non connecté." }) }
        verify(exactly = 0) { onSuccess() }
    }
}
