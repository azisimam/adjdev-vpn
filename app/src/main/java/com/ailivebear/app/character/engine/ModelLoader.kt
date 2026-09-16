package com.ailivebear.app.character.engine

import android.content.Context
import com.ailivebear.app.core.util.AppLog
import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import com.google.android.filament.Scene
import com.google.android.filament.gltfio.AssetLoader
import com.google.android.filament.gltfio.FilamentAsset
import com.google.android.filament.gltfio.ResourceLoader
import com.google.android.filament.gltfio.UbershaderProvider
import java.io.FileNotFoundException
import java.nio.ByteBuffer

/**
 * Loads a glTF/.glb character model into the scene via Filament's gltfio.
 *
 * PLACEHOLDER ASSET NOTE: Stage 1 ships NO bundled 3D model - a real
 * rigged "cute stylized bear" can't be produced inside a code scaffold, so
 * rather than fake one, this loader is simply fully functional against any
 * valid glTF 2.0 / .glb file placed at
 * `app/src/main/assets/models/bear_placeholder.glb` (see the README in
 * that folder for exact steps). If the file is missing, this loader logs a
 * warning and returns null; the app keeps running normally with just the
 * background and camera, and does not crash. A later stage replaces the
 * placeholder with the real rigged asset (skeleton + morph targets for the
 * facial rig).
 */
class ModelLoader(private val engine: Engine, private val scene: Scene) {

    private val materialProvider = UbershaderProvider(engine)
    private val assetLoader = AssetLoader(engine, materialProvider, EntityManager.get())
    private val resourceLoader = ResourceLoader(engine)

    private var currentAsset: FilamentAsset? = null

    companion object {
        const val PLACEHOLDER_ASSET_PATH = "models/bear_placeholder.glb"
    }

    /** Returns the loaded [FilamentAsset], or null if no placeholder asset was found/valid. */
    fun loadCharacter(context: Context, assetPath: String = PLACEHOLDER_ASSET_PATH): FilamentAsset? {
        unloadCurrent()

        val bytes = try {
            context.assets.open(assetPath).use { it.readBytes() }
        } catch (e: FileNotFoundException) {
            AppLog.w(
                message = "No character model at assets/$assetPath yet - showing background only. " +
                    "See assets/models/README.md for how to add a placeholder glTF."
            )
            return null
        } catch (e: Exception) {
            AppLog.e(message = "Failed to read character model at assets/$assetPath", throwable = e)
            return null
        }

        val buffer = ByteBuffer.allocateDirect(bytes.size).apply {
            put(bytes)
            rewind()
        }

        val asset = assetLoader.createAsset(buffer) ?: run {
            AppLog.e(message = "assets/$assetPath was not a valid glTF/.glb file")
            return null
        }

        resourceLoader.loadResources(asset)
        asset.releaseSourceData()

        scene.addEntities(asset.entities)
        currentAsset = asset
        return asset
    }

    fun unloadCurrent() {
        currentAsset?.let { asset ->
            scene.removeEntities(asset.entities)
            assetLoader.destroyAsset(asset)
        }
        currentAsset = null
    }

    fun destroy() {
        unloadCurrent()
        resourceLoader.destroy()
        assetLoader.destroy()
        materialProvider.destroyMaterials()
    }
}
