# Placeholder character asset

Stage 1 does not bundle a 3D model file, on purpose (see ModelLoader.kt's
kdoc for the full explanation) - a real rigged "Bubu the bear" asset can't
be produced inside this code scaffold, and shipping a fake/broken one would
just break later.

`ModelLoader` looks for a glTF 2.0 binary file at:

    app/src/main/assets/models/bear_placeholder.glb

If it's missing, the app still builds and runs correctly - you'll just see
the background and camera with no character on screen, and a logcat
warning tagged "AILiveBear".

## Fastest way to get something on screen

Any valid, reasonably small `.glb` works as a stand-in while Stage 1's
animation rig (breathing/blink/head-sway) is being tested - it does not
need to look like a bear yet:

1. Grab a simple sample model, e.g. `Box.glb` or `BoxAnimated.glb` from the
   official Khronos glTF-Sample-Assets repository
   (https://github.com/KhronosGroup/glTF-Sample-Assets), which are CC0/
   permissively licensed.
2. Rename it to `bear_placeholder.glb` and drop it in this folder.
3. Rebuild and run. You should see it gently bob/scale (breathing) and sway
   (idle head movement).

## Stage 2

Stage 2 replaces this placeholder with the real rigged bear (skeleton +
morph targets for the facial rig / mouth shapes / eyes) at the same path,
and teaches ModelLoader/CharacterManager to target specific bones and
morph targets instead of the whole root node.
