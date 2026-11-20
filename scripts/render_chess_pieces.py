import bpy
import math
from pathlib import Path
from mathutils import Vector

ROOT = Path(__file__).resolve().parents[1]
MODEL_DIR = ROOT / "3D_Chess_Pieces_Pack_LowPoly" / "models"
OUTPUT_DIR = ROOT / "app" / "src" / "main" / "res" / "drawable-nodpi"
OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

def clear_scene():
    bpy.ops.object.select_all(action="SELECT")
    bpy.ops.object.delete(use_global=False)
    for datablocks in (bpy.data.meshes, bpy.data.materials, bpy.data.cameras, bpy.data.lights):
        for block in list(datablocks):
            if block.users == 0:
                datablocks.remove(block)

def make_material(is_light):
    mat = bpy.data.materials.new("CHESS Champagne Gold" if is_light else "CHESS Obsidian Gold")
    mat.diffuse_color = (0.78, 0.53, 0.20, 1.0) if is_light else (0.075, 0.065, 0.052, 1.0)
    mat.use_nodes = True
    bsdf = mat.node_tree.nodes.get("Principled BSDF")
    if bsdf:
        bsdf.inputs["Base Color"].default_value = mat.diffuse_color
        bsdf.inputs["Metallic"].default_value = 0.82 if is_light else 0.76
        bsdf.inputs["Roughness"].default_value = 0.23 if is_light else 0.27
        if "Coat Weight" in bsdf.inputs:
            bsdf.inputs["Coat Weight"].default_value = 0.28
            bsdf.inputs["Coat Roughness"].default_value = 0.18
    return mat

def aim(obj, point):
    obj.rotation_euler = (Vector(point) - obj.location).to_track_quat("-Z", "Y").to_euler()

def add_area(name, location, energy, color, size):
    data = bpy.data.lights.new(name, "AREA")
    data.energy = energy
    data.color = color
    data.shape = "DISK"
    data.size = size
    obj = bpy.data.objects.new(name, data)
    bpy.context.collection.objects.link(obj)
    obj.location = location
    aim(obj, (0, 0, 0.45))
    return obj

for model in sorted(MODEL_DIR.glob("*.glb")):
    clear_scene()
    bpy.ops.import_scene.gltf(filepath=str(model))
    is_light = model.stem.endswith("_light")
    material = make_material(is_light)
    meshes = [obj for obj in bpy.context.scene.objects if obj.type == "MESH"]
    if not meshes:
        raise RuntimeError("No mesh imported from " + model.name)
    for obj in meshes:
        obj.data.materials.clear()
        obj.data.materials.append(material)

    bpy.context.view_layer.update()
    corners = [obj.matrix_world @ Vector(corner) for obj in meshes for corner in obj.bound_box]
    minimum = Vector((min(v.x for v in corners), min(v.y for v in corners), min(v.z for v in corners)))
    maximum = Vector((max(v.x for v in corners), max(v.y for v in corners), max(v.z for v in corners)))
    center = (minimum + maximum) * 0.5
    dimensions = maximum - minimum
    height = max(dimensions.z, dimensions.x, dimensions.y, 0.5)
    target = Vector((center.x, center.y, minimum.z + dimensions.z * 0.48))

    scene = bpy.context.scene
    scene.render.engine = "BLENDER_EEVEE_NEXT" if bpy.app.version >= (4, 2, 0) else "BLENDER_EEVEE"
    scene.render.resolution_x = 640
    scene.render.resolution_y = 640
    scene.render.resolution_percentage = 100
    scene.render.image_settings.file_format = "PNG"
    scene.render.image_settings.color_mode = "RGBA"
    scene.render.film_transparent = True
    scene.render.image_settings.compression = 8
    scene.render.filepath = str(OUTPUT_DIR / ("piece_" + ("white" if is_light else "black") + "_" + model.stem.split("_")[1] + ".png"))

    world = bpy.data.worlds.new("CHESS Studio World") if not bpy.data.worlds else bpy.data.worlds[0]
    scene.world = world
    world.use_nodes = True
    world.node_tree.nodes["Background"].inputs["Color"].default_value = (0.12, 0.12, 0.12, 1)
    world.node_tree.nodes["Background"].inputs["Strength"].default_value = 0.32

    add_area("Key softbox", target + Vector((3.0, -4.0, 5.0)), 850, (1.0, 0.86, 0.66), 4.0)
    add_area("Cool fill", target + Vector((-4.0, -1.5, 2.8)), 620, (0.68, 0.79, 1.0), 3.2)
    add_area("Gold rim", target + Vector((1.5, 3.5, 4.2)), 1000, (1.0, 0.72, 0.36), 2.5)

    camera_data = bpy.data.cameras.new("Piece Camera")
    camera = bpy.data.objects.new("Piece Camera", camera_data)
    scene.collection.objects.link(camera)
    camera.location = target + Vector((height * 2.4, -height * 3.2, height * 2.1))
    aim(camera, target)
    camera_data.type = "ORTHO"
    camera_data.ortho_scale = 1.30  # Fixed across all twelve models to preserve real Staunton proportions
    scene.camera = camera
    scene.render.film_transparent = True
    scene.view_settings.view_transform = "Standard"
    scene.view_settings.look = "Medium High Contrast"
    bpy.ops.render.render(write_still=True)
    print("Rendered " + scene.render.filepath)
