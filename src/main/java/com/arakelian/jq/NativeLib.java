package com.arakelian.jq;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;

import org.immutables.value.Value;

import com.sun.jna.NativeLibrary;

@Value.Immutable
public abstract class NativeLib {
    @Value.Derived
    @Value.Auxiliary
    public String getArchitecture() {
        return getOsName() + ":" + getOsArch();
    }

    @Value.Lazy
    @Value.Auxiliary
    public File getFile() throws UncheckedIOException {
        final File tmpDir = new File(System.getProperty("java.io.tmpdir"));
        if (!tmpDir.exists()) {
            tmpDir.mkdir();
        }

        try {
            final File tmp = File.createTempFile(getFilename() + "-", ".tmp", tmpDir);
            tmp.deleteOnExit();

            final String resource = getResourceName();
            try (InputStream in = this.getClass().getClassLoader().getResourceAsStream(resource);
                    OutputStream out = new FileOutputStream(tmp)) {
                final byte buf[] = new byte[1024 * 1024];
                int n;
                while (-1 != (n = in.read(buf))) {
                    out.write(buf, 0, n);
                }
                return tmp;
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to copy library " + //
                    getFilename() + " to temporary folder", e);
        }
    }

    @Value.Lazy
    @Value.Auxiliary
    public NativeLibrary getNativeLibrary() throws UncheckedIOException {
        final File file = getFile();
        return NativeLibrary.getInstance(file.getAbsolutePath());
    }

    @Value.Derived
    public String getFilename() {
        final String name = getName();

        if (isWindows()) {
            return name + ".dll";
        } else if (isLinux()) {
            return "lib" + name + ".so";
        } else if (isMac()) {
            return "lib" + name + ".dylib";
        }

        throw new IllegalStateException("Unsupported architecture: " + getArchitecture());
    }

    public abstract String getName();

    @Value.Derived
    public String getOsArch() {
        final String arch = System.getProperty("os.arch");
        return arch != null ? arch.toLowerCase() : "";
    }

    @Value.Derived
    public String getOsName() {
        final String name = System.getProperty("os.name");
        return name != null ? name.toLowerCase() : "";
    }

    @Value.Derived
    public String getPath() {
        final String osArch = getOsArch();
        if (isWindows()) {
            if ("x86".equalsIgnoreCase(osArch)) {
                return "win-x86/";
            }
        } else if (isLinux()) {
            if ("amd64".equalsIgnoreCase(osArch)) {
                return "linux-x86_64/";
            } else if ("ia64".equalsIgnoreCase(osArch)) {
                return "linux-ia64/";
            } else if ("i386".equalsIgnoreCase(osArch)) {
                return "linux-x86/";
            }
        } else if (isMac()) {
            if ("x86_64".equalsIgnoreCase("x86_64")) {
                return "darwin-x86_64/";
            }
        }
        throw new IllegalStateException("Unsupported architecture: " + getArchitecture());
    }

    @Value.Derived
    public String getResourceName() {
        return "lib/" + getPath() + getFilename();
    }

    @Value.Derived
    @Value.Auxiliary
    public boolean isLinux() {
        return getOsName().startsWith("linux");
    }

    @Value.Derived
    @Value.Auxiliary
    public boolean isMac() {
        return getOsName().startsWith("mac os x");
    }

    @Value.Derived
    @Value.Auxiliary
    public boolean isWindows() {
        return getOsName().startsWith("win");
    }
}
