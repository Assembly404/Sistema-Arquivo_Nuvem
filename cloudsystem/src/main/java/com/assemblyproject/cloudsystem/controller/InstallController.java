package com.assemblyproject.cloudsystem.controller;

import java.io.IOException;
import java.io.InputStream;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serve os scripts de instalação do agente (nó pessoal) para download.
 * Permite que o frontend mostre links directos sem precisar de alojar
 * os ficheiros noutro lado.
 */
@RestController
@RequestMapping("/api/install")
public class InstallController {

    private static final String SH_SCRIPT = "install/install-node.sh";
    private static final String BAT_SCRIPT = "install/install-node.bat";

    @GetMapping(value = "/install-node.sh", produces = "application/x-sh")
    public ResponseEntity<byte[]> downloadSh() throws IOException {
        return serve(SH_SCRIPT, "install-node.sh");
    }

    @GetMapping(value = "/install-node.bat", produces = "application/x-msdos-program")
    public ResponseEntity<byte[]> downloadBat() throws IOException {
        return serve(BAT_SCRIPT, "install-node.bat");
    }

    private ResponseEntity<byte[]> serve(String classpathLocation, String filename) throws IOException {
        ClassPathResource resource = new ClassPathResource(classpathLocation);
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }
        try (InputStream in = resource.getInputStream()) {
            byte[] bytes = in.readAllBytes();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(bytes.length)
                    .body(bytes);
        }
    }
}
