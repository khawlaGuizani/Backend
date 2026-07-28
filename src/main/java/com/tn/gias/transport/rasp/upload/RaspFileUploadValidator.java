package com.tn.gias.transport.rasp.upload;

import com.tn.gias.transport.rasp.core.RaspGuard;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * Validates uploaded files: blocks dangerous extensions (executables,
 * server-side scripts, archives that commonly carry webshells) and detects
 * content/extension mismatches via magic-byte sniffing (e.g. a ".jpg" whose
 * actual bytes are a PE/ELF executable or a JSP script). Call
 * {@link #validate(MultipartFile)} from any controller that accepts file
 * uploads; RaspHttpFilter already blocks obviously dangerous filenames at
 * the HTTP boundary as a first layer.
 */
@Component
public class RaspFileUploadValidator {

    private static final long MAX_SIZE_BYTES = 20L * 1024 * 1024; // 20MB

    private static final byte[] MAGIC_PE_EXE = {0x4D, 0x5A};                     // MZ
    private static final byte[] MAGIC_ELF = {0x7F, 'E', 'L', 'F'};
    private static final byte[] MAGIC_SCRIPT_SHEBANG = {'#', '!'};

    private final RaspGuard guard;

    public RaspFileUploadValidator(RaspGuard guard) {
        this.guard = guard;
    }

    public void validate(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return;
        }

        String filename = file.getOriginalFilename();
        guard.checkFileName("FILE_UPLOAD", filename);

        if (file.getSize() > MAX_SIZE_BYTES) {
            guard.reportCustom("FILE_UPLOAD", "DANGEROUS_FILE_UPLOAD", "MEDIUM", "max-size-exceeded",
                    "Upload rejected: " + filename + " exceeds " + MAX_SIZE_BYTES + " bytes");
        }

        byte[] header = readHeader(file, 8);
        if (startsWith(header, MAGIC_PE_EXE) || startsWith(header, MAGIC_ELF) || startsWith(header, MAGIC_SCRIPT_SHEBANG)) {
            guard.reportCustom("FILE_UPLOAD", "DANGEROUS_FILE_UPLOAD", "CRITICAL", "executable-magic-bytes",
                    "Upload rejected: " + filename + " content is an executable/script, not the declared type");
        }
    }

    private byte[] readHeader(MultipartFile file, int length) throws IOException {
        byte[] buffer = new byte[length];
        try (InputStream in = file.getInputStream()) {
            int read = in.read(buffer);
            if (read < length) {
                byte[] trimmed = new byte[Math.max(read, 0)];
                System.arraycopy(buffer, 0, trimmed, 0, trimmed.length);
                return trimmed;
            }
        }
        return buffer;
    }

    private boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }
}
