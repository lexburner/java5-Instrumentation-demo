package moe.cnkirito.agent;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

public class DogTransformer implements ClassFileTransformer {

    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        System.out.println("className: " + className);
        if (!className.equalsIgnoreCase("moe/cnkirito/agent/Dog")) {
            return null;
        }
        return getBytesFromFile("C:/Users/xujingfeng/Desktop/Dog.class");
//        return getBytesFromFile("app/target/classes/moe/cnkirito/agent/Dog.class");
    }

    public static byte[] getBytesFromFile(String fileName) {
        File file = new File(fileName);
        try (InputStream is = new FileInputStream(file)) {
            // precondition

            long length = file.length();
            byte[] bytes = new byte[(int) length];

            // Read in the bytes
            int offset = 0;
            int numRead = 0;
            while (offset <bytes.length
                    && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
                offset += numRead;
            }

            if (offset < bytes.length) {
                throw new IOException("Could not completely read file "
                        + file.getName());
            }
            is.close();
            return bytes;
        } catch (Exception e) {
            System.out.println("error occurs in _ClassTransformer!"
                    + e.getClass().getName());
            return null;
        }
    }

}
