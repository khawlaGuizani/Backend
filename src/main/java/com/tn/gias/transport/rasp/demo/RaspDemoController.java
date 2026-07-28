package com.tn.gias.transport.rasp.demo;

import com.tn.gias.transport.rasp.RaspProperties;
import com.tn.gias.transport.rasp.core.RaspGuard;
import com.tn.gias.transport.rasp.ssrf.RaspSsrfGuard;
import com.tn.gias.transport.rasp.upload.RaspFileUploadValidator;
import com.tn.gias.transport.rasp.xxe.RaspXmlUtils;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Deliberately vulnerable endpoints used ONLY to demonstrate that RASP
 * intercepts real exploit payloads before they reach this business logic.
 * Disabled by default ({@code rasp.demo.enabled=false}); enable it only in
 * an isolated local/demo environment — see README-DEVSECOPS.md >
 * "Runtime Validation". Every sink below is guarded by an explicit RaspGuard
 * check in addition to whatever RaspHttpFilter already caught upstream, so
 * the demo also proves the defense-in-depth (multi-layer) design.
 */
@RestController
@RequestMapping("/api/rasp-demo")
@ConditionalOnProperty(prefix = "rasp.demo", name = "enabled", havingValue = "true")
public class RaspDemoController {

    private final RaspGuard guard;
    private final RaspProperties properties;
    private final RaspSsrfGuard ssrfGuard;
    private final RaspFileUploadValidator fileUploadValidator;
    private final RestTemplate raspRestTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    private Path sandboxDir;

    public RaspDemoController(RaspGuard guard,
                               RaspProperties properties,
                               RaspSsrfGuard ssrfGuard,
                               RaspFileUploadValidator fileUploadValidator,
                               RestTemplate raspRestTemplate) {
        this.guard = guard;
        this.properties = properties;
        this.ssrfGuard = ssrfGuard;
        this.fileUploadValidator = fileUploadValidator;
        this.raspRestTemplate = raspRestTemplate;
    }

    @PostConstruct
    void initSandbox() throws IOException {
        sandboxDir = Files.createTempDirectory("rasp-demo-sandbox");
        Files.writeString(sandboxDir.resolve("welcome.txt"),
                "This is a normal, non-sensitive demo file served from the RASP path-traversal sandbox.\n");
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of(
                "raspEnabled", properties.isEnabled(),
                "raspMode", properties.getMode(),
                "demoEnabled", properties.getDemo().isEnabled()
        );
    }

    /** Try: ?input=hello then ?input=' UNION SELECT 1-- */
    @GetMapping("/sql")
    public ResponseEntity<?> sqlDemo(@RequestParam String input) {
        // Deliberately vulnerable pattern (string concatenation into a native query),
        // kept ONLY to prove RASP's JDBC-layer guard catches it independently of the
        // HTTP-layer filter. Never write real application code like this.
        String sql = "SELECT '" + input + "' AS demo_echo";
        Object result = entityManager.createNativeQuery(sql).getSingleResult();
        return ResponseEntity.ok(Map.of("result", String.valueOf(result)));
    }

    /** Try: ?filename=welcome.txt then ?filename=../../../../etc/passwd */
    @GetMapping("/path")
    public ResponseEntity<?> pathDemo(@RequestParam String filename) throws IOException {
        guard.checkPathTraversal("DEMO_FILE_READ", filename);
        File target = new File(sandboxDir.toFile(), filename);
        String content = Files.readString(target.toPath(), StandardCharsets.UTF_8);
        return ResponseEntity.ok(Map.of("content", content));
    }

    /** Try: ?host=localhost then ?host=localhost; cat /etc/passwd */
    @GetMapping("/exec")
    public ResponseEntity<?> execDemo(@RequestParam String host) throws IOException, InterruptedException {
        guard.checkCommand("DEMO_EXEC", host);
        ProcessBuilder pb = new ProcessBuilder("sh", "-c", "ping -c 1 -W 1 " + host);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        String output;
        try (InputStream in = process.getInputStream()) {
            output = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        boolean finished = process.waitFor(3, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
        }
        return ResponseEntity.ok(Map.of("output", output));
    }

    /** Try: url=https://example.com then url=http://169.254.169.254/latest/meta-data/ */
    @GetMapping("/ssrf")
    public ResponseEntity<?> ssrfDemo(@RequestParam String url) {
        ssrfGuard.checkTarget(java.net.URI.create(url));
        String body = raspRestTemplate.getForObject(url, String.class);
        return ResponseEntity.ok(Map.of("fetchedLength", body == null ? 0 : body.length()));
    }

    /** multipart/form-data upload — try a .txt then a .jsp/.exe */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadDemo(@RequestParam("file") MultipartFile file) throws IOException {
        fileUploadValidator.validate(file);
        return ResponseEntity.ok(Map.of("accepted", file.getOriginalFilename()));
    }

    /** POST raw XML body — try a normal <root>ok</root> then a DOCTYPE/ENTITY payload */
    @PostMapping(value = "/xxe", consumes = "application/xml")
    public ResponseEntity<?> xxeDemo(@RequestBody String xmlBody) throws Exception {
        guard.checkXxe("DEMO_XXE", xmlBody);
        DocumentBuilder builder = RaspXmlUtils.safeDocumentBuilderFactory().newDocumentBuilder();
        var doc = builder.parse(new InputSource(new StringReader(xmlBody)));
        return ResponseEntity.ok(Map.of("rootElement", doc.getDocumentElement().getNodeName()));
    }

    /** POST base64-encoded serialized Java object bytes — protected by the global ObjectInputFilter */
    @PostMapping("/deserialize")
    public ResponseEntity<?> deserializeDemo(@RequestBody String base64Payload) {
        byte[] bytes = Base64.getDecoder().decode(base64Payload.trim());
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            Object obj = ois.readObject();
            return ResponseEntity.ok(Map.of("deserialized", String.valueOf(obj)));
        } catch (InvalidClassException e) {
            return ResponseEntity.status(403).body(Map.of(
                    "blocked", true,
                    "reason", "RASP global ObjectInputFilter rejected this class",
                    "detail", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** Try: ?filter=(uid=demo) then ?filter=*)(uid=*))(|(uid=* */
    @GetMapping("/ldap")
    public ResponseEntity<?> ldapDemo(@RequestParam String filter) {
        guard.checkLdap("DEMO_LDAP", filter);
        return ResponseEntity.ok(Map.of("filterAccepted", filter));
    }

    /** Try: ?expression=hello then ?expression=T(java.lang.Runtime).getRuntime().exec('id') */
    @GetMapping("/expr")
    public ResponseEntity<?> expressionDemo(@RequestParam String expression) {
        guard.checkExpression("DEMO_EXPRESSION", expression);
        return ResponseEntity.ok(Map.of("expressionAccepted", expression));
    }
}
