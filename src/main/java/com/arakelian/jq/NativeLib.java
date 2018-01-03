package com.arakelian.jq;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.List;

import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Platform;

@Value.Immutable(copy = false)
public abstract class NativeLib {
    private static final Logger LOGGER = LoggerFactory.getLogger(NativeLib.class);

    @Value.Derived
    @Value.Auxiliary
    public String getArchitecture() {
        return getOsName() + ":" + getOsArch();
    }

    @Value.Default
    @Value.Auxiliary
    public List<String> getDependencies() {
        return ImmutableList.of();
    }

    @Value.Derived
    public List<String> getFilenames() {
        final ImmutableList.Builder<String> builder = ImmutableList.builder();

        final String name = getName();
        if (Platform.isWindows()) {
            builder.add(name + ".dll");
        } else if (Platform.isLinux()) {
            builder.add("lib" + name + ".so");
        } else if (Platform.isMac()) {
            builder.add("lib" + name + ".dylib");
        } else {
            throw new IllegalStateException("Unsupported architecture: " + getArchitecture());
        }

        for (final String dependency : getDependencies()) {
            builder.add(dependency);
        }

        return builder.build();
    }

    @Value.Default
    @Value.Auxiliary
    public File getTemporaryFolder() throws UncheckedIOException {
        // must create a temp directory, required for NativeLibrary.addSearchPath
        final File tmpdir = Files.createTempDir();
        // tmpdir.deleteOnExit();
        return tmpdir;
    }

    @Value.Lazy
    @Value.Auxiliary
    public File getLocalCopy() throws UncheckedIOException {
        final File tmpdir = getTemporaryFolder();

        for (final String filename : getFilenames()) {
            try {
                final File local = new File(tmpdir, filename);
                // local.deleteOnExit();

                final String resource = "lib/" + getPath() + filename;
                LOGGER.info("Copying resource {} to: {}", resource, local);
                try (InputStream in = this.getClass().getClassLoader().getResourceAsStream(resource);
                        OutputStream out = new FileOutputStream(local)) {
                    Preconditions.checkState(in != null, "Cannot find resource %s", resource);
                    final byte buf[] = new byte[1024 * 1024];
                    int n;
                    while (-1 != (n = in.read(buf))) {
                        out.write(buf, 0, n);
                    }
                }
            } catch (final IOException e) {
                throw new UncheckedIOException(
                        "Unable to copy library " + filename + " to temporary folder " + tmpdir, e);
            }
        }

        return tmpdir;
    }

    public abstract String getName();

    @Value.Lazy
    @Value.Auxiliary
    public NativeLibrary getNativeLibrary() throws UncheckedIOException {
        final String name = getName();

        final File libPath = getLocalCopy();
        LOGGER.info("{} library path: {}", name, libPath);
        NativeLibrary.addSearchPath(name, libPath.getAbsolutePath());

        final NativeLibrary instance = NativeLibrary.getInstance(name);
        LOGGER.info("{} loaded from path: {} ", name, libPath);
        return instance;
    }

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
        if (Platform.isWindows()) {
            if ("x86".equalsIgnoreCase(osArch)) {
                return "win-x86/";
            }
        } else if (Platform.isLinux()) {
            if ("amd64".equalsIgnoreCase(osArch)) {
                return "linux-x86_64/";
            } else if ("ia64".equalsIgnoreCase(osArch)) {
                return "linux-ia64/";
            } else if ("i386".equalsIgnoreCase(osArch)) {
                return "linux-x86/";
            }
        } else if (Platform.isMac()) {
            if ("x86_64".equalsIgnoreCase("x86_64")) {
                return "darwin-x86_64/";
            }
        }
        throw new IllegalStateException("Unsupported architecture: " + getArchitecture());
    }
}
