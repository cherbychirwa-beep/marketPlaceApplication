package com.shopizzo.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.shopizzo.data.model.Notification
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class NotificationRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val notificationsCol = firestore.collection("notifications")

    fun getNotificationsFlow(): Flow<List<Notification>> = callbackFlow {
        val listener = notificationsCol
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    val notifications = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Notification::class.java)?.copy(id = doc.id)
                    }
                    trySend(notifications)
                }
            }
        awaitClose { listener.remove() }
    }
}
