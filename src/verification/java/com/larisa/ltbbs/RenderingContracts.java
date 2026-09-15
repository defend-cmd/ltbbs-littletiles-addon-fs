package com.larisa.ltbbs;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.MethodRemapper;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.tree.*;

/** Offline contract/coordinate checks, not a substitute for an in-game GPU test. */
public final class RenderingContracts implements Opcodes {
    private static final String BBS = "mchorse/bbs_mod/forms/renderers/BlockFormRenderer";
    private static final String MIXIN = "com/larisa/ltbbs/mixin/client/BlockFormRendererMixin";
    private static final String GRID = "team/creative/littletiles/common/grid/LittleGrid";
    private static final String BOX = "team/creative/littletiles/common/math/box/LittleBox";
    private static final String RENDER_BOX = "team/creative/littletiles/client/render/tile/LittleRenderBox";
    private static final String GROUP = "team/creative/littletiles/common/block/little/tile/group/LittleGroup";
    private static final String FACING = "team/creative/creativecore/common/util/math/base/Facing";
    private static final String COMPILER = "team/creative/creativecore/client/render/model/CreativeBakedBoxModel";

    public static void main(String[] args) throws Exception {
        Path bbs = Path.of(args[0]), lt = Path.of(args[1]), cc = Path.of(args[2]), addon = Path.of(args[3]);
        ClassNode renderer = read(bbs, BBS);
        MethodNode target = method(renderer, "renderBlock", "(Lnet/minecraft/class_4587;Lnet/minecraft/class_4597;IIZ)V");
        ClassNode mixin = read(addon, MIXIN);
        MethodNode hook = method(mixin, "ltbbs$renderStructure",
                target.desc.replace(")V", "Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V"));
        AnnotationNode inject = annotation(hook.visibleAnnotations,
                "Lorg/spongepowered/asm/mixin/injection/Inject;");
        require(value(inject, "method").equals(List.of("renderBlock")), "Wrong injection target");
        require(Boolean.TRUE.equals(value(inject, "cancellable")), "Hook must cancel the original block draw");
        @SuppressWarnings("unchecked")
        List<AnnotationNode> ats = (List<AnnotationNode>) value(inject, "at");
        require(value(ats.get(0), "value").equals("HEAD"), "Hook must run at block-space entry");

        MethodNode world = method(renderer, "render3D", null), ui = method(renderer, "renderInUI", null);
        require(calls(world, BBS, "renderBlock") && calls(ui, BBS, "renderBlock"), "Both wrappers must reach the hook");
        List<float[]> worldOffsets = translations(world), uiOffsets = translations(ui);
        require(worldOffsets.size() == 2 && uiOffsets.size() == 1, "BBS block-space wrappers changed");
        for (float[] xyz : worldOffsets) close(xyz, new float[]{-0.5F, 0, -0.5F});
        close(uiOffsets.get(0), worldOffsets.get(0));
        ClassNode modelBlock = read(bbs, "mchorse/bbs_mod/client/renderer/ModelBlockEntityRenderer");
        float[] blockOrigin = translations(method(modelBlock, "render", null)).get(0);
        close(blockOrigin, new float[]{0.5F, 0, 0.5F});
        float[] netOffset = new float[3];
        for (int axis = 0; axis < 3; axis++) netOffset[axis] = blockOrigin[axis] + worldOffsets.get(0)[axis];
        close(netOffset, new float[]{0, 0, 0});
        noTransforms(mixin);
        ClassNode mesh = read(addon, "com/larisa/ltbbs/client/LittleTilesMesh");
        noTransforms(mesh);
        require(constants(mesh).containsAll(List.of("getTiles", "getRenderingBoxes", "compileBoxes")),
                "Mesh must get raw group boxes and compile them directly");
        require(!constants(mesh).contains("shrinkCubesToOneBlock"), "Inventory normalization reintroduced");
        System.out.println("PASS: remapped BBS hook signature; shared world/UI block space; no addon offsets");

        ClassNode invoker = read(Path.of(args[4]), "com/larisa/ltbbs/mixin/client/ItemRendererInvoker");
        MethodNode invoke = method(invoker, "ltbbs$renderQuads", null);
        ClassNode itemRenderer = findClass(args[5], "net/minecraft/client/render/item/ItemRenderer");
        MethodNode quads = method(itemRenderer, "renderBakedItemQuads", invoke.desc);
        noTransforms(quads);
        String refmap = resource(addon, "ltbbs-littletiles-addon-fs-refmap.json");
        require(refmap.contains("renderBakedItemQuads") && refmap.contains("method_23180"), "Missing invoker remapping");
        System.out.println("PASS: Minecraft quad-only invoker signature and production refmap");

        method(read(lt, "team/creative/littletiles/api/common/tool/ILittlePlacer"), "getTiles",
                "(Lnet/minecraft/world/item/ItemStack;)L" + GROUP + ";");
        method(read(lt, GROUP), "getRenderingBoxes", "(Z)Ljava/util/List;");
        MethodNode compiler = method(read(cc, COMPILER), "compileBoxes", "(Ljava/util/List;L" + FACING
                + ";Lnet/minecraft/client/renderer/RenderType;Lnet/minecraft/util/RandomSource;ZLjava/util/List;)Ljava/util/List;");
        require((compiler.access & (ACC_PUBLIC | ACC_STATIC)) == (ACC_PUBLIC | ACC_STATIC), "Compiler is not public static");
        require(read(cc, FACING).fields.stream().anyMatch(f -> f.name.equals("VALUES") && f.desc.equals("[L" + FACING + ";")),
                "Missing CreativeCore faces");
        ClassNode itemModel = read(lt, "team/creative/littletiles/client/render/item/LittleModelItemTilesBig");
        require(calls(method(itemModel, "getBoxes", null), GROUP, "shrinkCubesToOneBlock"), "Inventory model assumption changed");
        System.out.println("PASS: installed LittleTiles/CreativeCore APIs and inventory normalization diagnosis");
        checkGeometry(lt, netOffset);
        System.out.println("All offline rendering contracts passed. GPU output and a full Connector launch are not covered.");
    }

    private static void checkGeometry(Path lt, float[] offset) throws Exception {
        // Execute the installed LittleGrid conversion and LittleRenderBox constructor
        // bytecode. Only surrounding game types are replaced by data-only stubs.
        String testGrid = "ltbbs/verification/InstalledGrid", testBox = "ltbbs/verification/InstalledBox";
        SimpleRemapper names = new SimpleRemapper(Map.of(GRID, testGrid, BOX, Type.getInternalName(SourceBox.class),
                RENDER_BOX, testBox, "team/creative/creativecore/client/render/box/RenderBox", Type.getInternalName(RawBox.class),
                "net/minecraft/world/level/block/state/BlockState", "java/lang/Object"));
        ClassWriter gridWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        gridWriter.visit(V17, ACC_PUBLIC, testGrid, null, "java/lang/Object", null);
        gridWriter.visitField(ACC_PUBLIC, "pixelLengthF", "F", null, null).visitEnd();
        MethodVisitor init = gridWriter.visitMethod(ACC_PUBLIC, "<init>", "(F)V", null, null);
        init.visitCode(); init.visitVarInsn(ALOAD, 0); init.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        init.visitVarInsn(ALOAD, 0); init.visitVarInsn(FLOAD, 1); init.visitFieldInsn(PUTFIELD, testGrid, "pixelLengthF", "F");
        init.visitInsn(RETURN); init.visitMaxs(0, 0); init.visitEnd();
        copyMethod(method(read(lt, GRID), "toVanillaGridF", "(I)F"), gridWriter, names);
        gridWriter.visitEnd();
        ClassWriter boxWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        boxWriter.visit(V17, ACC_PUBLIC, testBox, null, Type.getInternalName(RawBox.class), null);
        copyMethod(method(read(lt, RENDER_BOX), "<init>", "(L" + GRID + ";L" + BOX + ";)V"), boxWriter, names);
        boxWriter.visitEnd();
        Loader loader = new Loader();
        Class<?> gridClass = loader.define(testGrid, gridWriter.toByteArray());
        Class<?> boxClass = loader.define(testBox, boxWriter.toByteArray());
        var makeGrid = gridClass.getConstructor(float.class);
        var makeBox = boxClass.getConstructor(gridClass, SourceBox.class);
        Properties fixture = new Properties();
        try (InputStream in = RenderingContracts.class.getResourceAsStream("/stone-steps.properties")) { fixture.load(in); }
        int count = Integer.parseInt(fixture.getProperty("grid"));
        Object grid = makeGrid.newInstance(1F / count);
        for (int i = 0; i < 3; i++) {
            int[] item = ints(fixture.getProperty("item." + i));
            int[] placed = ints(fixture.getProperty("world." + i));
            require(Arrays.equals(item, Arrays.copyOfRange(placed, 1, 7)), "Copied item differs from original placed box");
            RawBox result = (RawBox) makeBox.newInstance(grid, new SourceBox(item));
            float[] expected = floats(fixture.getProperty("expected." + i));
            close(result.bounds, expected);
            for (int n = 0; n < 6; n++) result.bounds[n] += offset[n % 3];
            close(result.bounds, expected);
        }
        for (int resolution : new int[]{16, 32, 64}) {
            Object otherGrid = makeGrid.newInstance(1F / resolution);
            for (int[] coords : new int[][]{{0, 0, 0, resolution, resolution, resolution},
                    {-7, 4, 11, 41, 20, 70}, {2, 5, 7, 3, 6, 8}}) {
                RawBox result = (RawBox) makeBox.newInstance(otherGrid, new SourceBox(coords));
                for (int n = 0; n < 6; n++) close(result.bounds[n], coords[n] / (float) resolution);
            }
        }
        System.out.println("PASS: installed geometry bytecode preserves all 3 saved boxes; full cubes, off-center, negative and multi-block bounds on grids 16/32/64");
    }

    public static final class SourceBox {
        public int minX, minY, minZ, maxX, maxY, maxZ;
        public SourceBox(int[] n) { minX=n[0]; minY=n[1]; minZ=n[2]; maxX=n[3]; maxY=n[4]; maxZ=n[5]; }
    }
    public static class RawBox {
        public float[] bounds;
        public int color;
        public SourceBox box;
        public RawBox(float a, float b, float c, float d, float e, float f, Object state) { bounds = new float[]{a,b,c,d,e,f}; }
    }
    private static final class Loader extends ClassLoader {
        Class<?> define(String name, byte[] code) { return defineClass(name.replace('/', '.'), code, 0, code.length); }
    }
    private static void copyMethod(MethodNode original, ClassWriter writer, SimpleRemapper names) {
        MethodVisitor out = writer.visitMethod(ACC_PUBLIC, original.name, names.mapMethodDesc(original.desc), null, null);
        original.accept(new MethodRemapper(out, names));
    }
    private static List<float[]> translations(MethodNode method) {
        List<float[]> result = new ArrayList<>();
        for (AbstractInsnNode op : method.instructions) {
            if (op instanceof MethodInsnNode call && call.owner.equals("net/minecraft/class_4587")
                    && call.name.equals("method_46416")) {
                AbstractInsnNode z = previous(op), y = previous(z), x = previous(y);
                if (number(x) != null && number(y) != null && number(z) != null)
                    result.add(new float[]{number(x), number(y), number(z)});
            }
        }
        return result;
    }
    private static AbstractInsnNode previous(AbstractInsnNode op) {
        do { op = op.getPrevious(); } while (op != null && op.getOpcode() < 0);
        return op;
    }
    private static Float number(AbstractInsnNode op) {
        if (op instanceof LdcInsnNode ldc && ldc.cst instanceof Number n) return n.floatValue();
        if (op != null && op.getOpcode() >= FCONST_0 && op.getOpcode() <= FCONST_2) return (float)(op.getOpcode() - FCONST_0);
        return null;
    }
    private static void noTransforms(ClassNode type) { type.methods.forEach(RenderingContracts::noTransforms); }
    private static void noTransforms(MethodNode method) {
        for (AbstractInsnNode op : method.instructions) if (op instanceof MethodInsnNode call) {
            if (call.owner.equals("net/minecraft/class_4587") || call.owner.equals("net/minecraft/client/util/math/MatrixStack"))
                require(call.name.equals("peek") || call.name.equals("method_23760"), "Matrix mutation in " + method.name);
            require(!call.owner.startsWith("org/joml/"), "Unexpected matrix math in " + method.name);
            require(!call.owner.equals("net/minecraft/class_918") && !call.owner.equals("net/minecraft/client/render/item/ItemRenderer"),
                    "Item model renderer reintroduced in " + method.name);
        }
    }
    private static List<Object> constants(ClassNode node) {
        List<Object> out = new ArrayList<>();
        for (MethodNode m : node.methods) for (AbstractInsnNode op : m.instructions) if (op instanceof LdcInsnNode ldc) out.add(ldc.cst);
        return out;
    }
    private static boolean calls(MethodNode method, String owner, String name) {
        for (AbstractInsnNode op : method.instructions)
            if (op instanceof MethodInsnNode call && call.owner.equals(owner) && call.name.equals(name)) return true;
        return false;
    }
    private static ClassNode findClass(String classpath, String name) throws Exception {
        for (String entry : classpath.split(java.util.regex.Pattern.quote(File.pathSeparator))) {
            Path path = Path.of(entry);
            if (Files.isRegularFile(path) && entry.endsWith(".jar")) {
                try (JarFile jar = new JarFile(path.toFile())) { if (jar.getEntry(name + ".class") != null) return read(path, name); }
            }
        }
        throw new AssertionError("Class not on compile classpath: " + name);
    }
    private static ClassNode read(Path path, String name) throws Exception {
        ClassNode node = new ClassNode();
        if (Files.isDirectory(path)) new ClassReader(Files.readAllBytes(path.resolve(name + ".class"))).accept(node, 0);
        else try (JarFile jar = new JarFile(path.toFile())) {
            require(jar.getEntry(name + ".class") != null, "Missing " + name + " in " + path.getFileName());
            try (InputStream in = jar.getInputStream(jar.getEntry(name + ".class"))) { new ClassReader(in).accept(node, 0); }
        }
        return node;
    }
    private static String resource(Path path, String name) throws Exception {
        try (JarFile jar = new JarFile(path.toFile())) {
            require(jar.getEntry(name) != null, "Missing " + name);
            try (InputStream in = jar.getInputStream(jar.getEntry(name))) { return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8); }
        }
    }
    private static MethodNode method(ClassNode type, String name, String desc) {
        return type.methods.stream().filter(m -> m.name.equals(name) && (desc == null || desc.equals(m.desc)))
                .findFirst().orElseThrow(() -> new AssertionError("Missing " + type.name + "." + name + " " + desc));
    }
    private static AnnotationNode annotation(List<AnnotationNode> nodes, String desc) {
        require(nodes != null, "Missing annotation " + desc);
        return nodes.stream().filter(a -> a.desc.equals(desc)).findFirst().orElseThrow();
    }
    private static Object value(AnnotationNode annotation, String key) {
        for (int i = 0; i < annotation.values.size(); i += 2) if (annotation.values.get(i).equals(key)) return annotation.values.get(i + 1);
        throw new AssertionError("Missing annotation value " + key);
    }
    private static int[] ints(String text) { return Arrays.stream(text.split(",")).mapToInt(Integer::parseInt).toArray(); }
    private static float[] floats(String text) {
        String[] parts = text.split(","); float[] out = new float[parts.length];
        for (int i=0; i<out.length; i++) out[i] = Float.parseFloat(parts[i]); return out;
    }
    private static void close(float[] actual, float[] expected) {
        require(actual.length == expected.length, "Different coordinate count");
        for (int i=0; i<actual.length; i++) close(actual[i], expected[i]);
    }
    private static void close(float actual, float expected) {
        require(Math.abs(actual - expected) < 0.000001F, "Expected " + expected + ", got " + actual);
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
