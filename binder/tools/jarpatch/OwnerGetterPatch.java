import java.io.*;
import java.nio.file.*;
import java.util.zip.*;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.*;

/**
 * Patches Traveler's Optics (traveloptics-5.5.0) for Iron's Spells 3.16: Iron's dropped its OwnerGetter interface, and its
 * owner goals now take a Supplier<Entity>, so every summon of Traveler's Optics failed to make its goals (NoClassDefFoundError
 * on the first summon, e.g. Primal Pack's raptors). Every reference to OwnerGetter becomes Supplier, and the lambdas that
 * made an OwnerGetter (its get() returned the summoner) are made with Supplier.get's erased type.
 * Usage: java -cp asm.jar:asm-commons.jar OwnerGetterPatch.java <in.jar> <out.jar>
 */
public class OwnerGetterPatch {
    static final String OWNER_GETTER = "io/redspace/ironsspellbooks/entity/mobs/goals/OwnerGetter";
    static final String SUPPLIER = "java/util/function/Supplier";

    public static void main(String[] args) throws IOException {
        int patched = 0;
        try (ZipInputStream in = new ZipInputStream(new FileInputStream(args[0]));
             ZipOutputStream out = new ZipOutputStream(new FileOutputStream(args[1]))) {
            for (ZipEntry entry; (entry = in.getNextEntry()) != null; ) {
                byte[] bytes = in.readAllBytes();
                if (entry.getName().endsWith(".class") && new String(bytes, "ISO-8859-1").contains(OWNER_GETTER)) {
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

    static byte[] patch(byte[] bytes) {
        ClassReader reader = new ClassReader(bytes);
        ClassWriter writer = new ClassWriter(0);
        ClassVisitor fixLambdas = new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                return new MethodVisitor(Opcodes.ASM9, super.visitMethod(access, name, desc, signature, exceptions)) {
                    @Override
                    public void visitInvokeDynamicInsn(String indyName, String indyDesc, Handle bsm, Object... bsmArgs) {
                        if (Type.getReturnType(indyDesc).getInternalName().equals(SUPPLIER) && bsmArgs.length == 3 && bsmArgs[0] instanceof Type) {
                            bsmArgs[0] = Type.getMethodType("()Ljava/lang/Object;");
                        }
                        super.visitInvokeDynamicInsn(indyName, indyDesc, bsm, bsmArgs);
                    }
                };
            }
        };
        reader.accept(new ClassRemapper(fixLambdas, new SimpleRemapper(OWNER_GETTER, SUPPLIER)), 0);
        return writer.toByteArray();
    }
}
