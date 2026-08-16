package com.projecteternal.app

import android.content.Context
import androidx.room.Room
import com.projecteternal.app.controller.GameController
import com.projecteternal.persist.SaveManager
import com.projecteternal.persist.db.EternalDatabase
import com.projecteternal.persist.db.RoomSaveStore

/**
 * Manual DI root. Holds the single save store + manager + game controller for
 * the whole process; accessed from the Application.
 */
class AppContainer(context: Context) {

    private val database: EternalDatabase =
        Room.databaseBuilder(context, EternalDatabase::class.java, "eternal.db").build()

    private val saveStore = RoomSaveStore(database.saveSlotDao())
    private val saveManager = SaveManager(saveStore)

    val gameController = GameController(saveManager)
}
