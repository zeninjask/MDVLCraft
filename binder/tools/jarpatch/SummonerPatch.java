import java.io.*;
import java.util.zip.*;
import org.objectweb.asm.*;

/**
 * Patches Traveler's Optics (traveloptics-5.5.0) for Iron's Spells 3.16, whose IMagicSummon.getSummoner() now returns Entity
 * instead of LivingEntity. Calls to the old getSummoner()LivingEntity on Iron's interfaces threw NoSuchMethodError (Vigor
 * Siphon crashed the server when a summoned Vesper hit a player). Those calls now call getSummoner()Entity and cast it to
 * LivingEntity, and every class with its own getSummoner()LivingEntity gets a getSummoner()Entity that returns it, so Iron's
 * own owner checks see the summon's summoner. Run after OwnerGetterPatch.
 * Usage: java -cp asm.jar SummonerPatch.java <in.jar> <out.jar>
 */
public class SummonerPatch {
    static final String NAME = "getSummoner";
    static final String OLD = "()Lnet/minecraft/world/entity/LivingEntity;";
    static final String NEW = "()Lnet/minecraft/world/entity/Entity;";
    static final String LIVING = "net/minecraft/world/entity/LivingEntity";

    public static void main(String[] args) throws IOException {
        int patched = 0;
        try (ZipInputStream in = new ZipInputStream(new FileInputStream(args[0]));
             ZipOutputStream out = new ZipOutputStream(new FileOutputStream(args[1]))) {
            for (ZipEntry entry; (entry = in.getNextEntry()) != null; ) {
                byte[] bytes = in.readAllBytes();
                if (entry.getName().endsWith(".class") && new String(bytes, "ISO-8859-1").contains(NAME)) {
                    bytes = patch(bytes);
                    patched++;
                }
                out.putNextEntry(new ZipEntry(entry.getName()));
                out.write(bytes);
                out.closeEntry();
            }
        }
        System.out.println(patched + " classes patched");
    }

    static boolean ironsSummon(String owner) {
        return owner.equals("io/redspace/ironsspellbooks/entity/mobs/MagicSummon")
            || owner.equals("io/redspace/ironsspellbooks/entity/mobs/IMagicSummon");
    }

    static byte[] patch(byte[] bytes) {
        ClassReader reader = new ClassReader(bytes);
        ClassWriter writer = new ClassWriter(0);
        reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
            String name;
            boolean isInterface;
            boolean hasOld;
            boolean hasNew;

            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                this.name = name;
                this.isInterface = (access & Opcodes.ACC_INTERFACE) != 0;
                super.visit(version, access, name, signature, superName, interfaces);
            }

            @Override
            public MethodVisitor visitMethod(int access, String method, String desc, String signature, String[] exceptions) {
                if (method.equals(NAME) && desc.equals(OLD) && (access & Opcodes.ACC_ABSTRACT) == 0) {
                    hasOld = true;
                }
                if (method.equals(NAME) && desc.equals(NEW)) {
                    hasNew = true;
                }
                return new MethodVisitor(Opcodes.ASM9, super.visitMethod(access, method, desc, signature, exceptions)) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String called, String calledDesc, boolean itf) {
                        if (called.equals(NAME) && calledDesc.equals(OLD) && ironsSummon(owner)) {
                            super.visitMethodInsn(opcode, owner, called, NEW, itf);
                            super.visitTypeInsn(Opcodes.CHECKCAST, LIVING);
                            return;
                        }
                        super.visitMethodInsn(opcode, owner, called, calledDesc, itf);
                    }
                };
            }

            @Override
            public void visitEnd() {
                if (hasOld && !hasNew && !isInterface) {
                    MethodVisitor bridge = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC | Opcodes.ACC_BRIDGE, NAME, NEW, null, null);
                    bridge.visitCode();
                    bridge.visitVarInsn(Opcodes.ALOAD, 0);
                    bridge.visitMethodInsn(Opcodes.INVOKEVIRTUAL, name, NAME, OLD, false);
                    bridge.visitInsn(Opcodes.ARETURN);
                    bridge.visitMaxs(1, 1);
                    bridge.visitEnd();
                }
                super.visitEnd();
            }
        }, 0);
        return writer.toByteArray();
    }
}
