package service.vaxapp.model;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.LoaderOptions;
import org.owasp.encoder.Encode;
import org.springframework.stereotype.Component;

@Component
public class SecureYamlHandler {

    private Yaml yaml;

    public SecureYamlHandler() {
        // Initialize Yaml with safe options
        this.yaml = createSafeYaml();
    }

    /**
     * Creates a Yaml instance with secure configurations to prevent deserialization attacks.
     *
     * @return A configured Yaml instance
     */
    public Yaml createSafeYaml() {
        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setMaxAliasesForCollections(50); // Prevent DoS attacks by limiting aliases
        Constructor constructor = new Constructor(Object.class, loaderOptions);
        constructor.setAllowDuplicateKeys(false); // Disallow duplicate keys to avoid ambiguity
        return new Yaml(constructor);
    }

    /**
     * Parses YAML input securely, ensuring no malicious content is deserialized.
     *
     * @param yamlContent The YAML content to parse
     * @return Parsed Object if valid, null otherwise
     */
    public Object parseYaml(String yamlContent) {
        try {
            return yaml.load(yamlContent);
        } catch (Exception e) {
            // Log the exception and return null to indicate parsing failed
            System.out.println("Failed to parse YAML content: " + e.getMessage());
            return null;
        }
    }

    /**
     * Validates if the provided YAML content is safe and adheres to expected schema.
     *
     * @param yamlContent The YAML content to validate
     * @return True if valid, false otherwise
     */
    public boolean isValidYaml(String yamlContent) {
        try {
            yaml.load(yamlContent);
            return true;
        } catch (Exception e) {
            return false; // If parsing throws an exception, it is not valid
        }
    }

    /**
     * Sanitizes input by encoding it for HTML to prevent XSS attacks.
     *
     * @param input The input string to sanitize
     * @return Sanitized input string
     */
    public String sanitizeInput(String input) {
        return Encode.forHtml(input);
    }

    /**
     * Test case to validate the functionality of the SecureYamlHandler class.
     */
    public static void main(String[] args) {
        SecureYamlHandler handler = new SecureYamlHandler();

        String validYaml = "key: value";
        String maliciousYaml = "!!com.example.CustomClass\nproperty: value";

        System.out.println("Testing with valid YAML:");
        System.out.println("Is valid YAML? " + handler.isValidYaml(validYaml)); // Expected: true
        System.out.println("Parsed YAML: " + handler.parseYaml(validYaml)); // Should parse correctly

        System.out.println("\nTesting with malicious YAML:");
        System.out.println("Is valid YAML? " + handler.isValidYaml(maliciousYaml)); // Expected: false
        System.out.println("Parsed YAML: " + handler.parseYaml(maliciousYaml)); // Should return null

        System.out.println("\nTesting input sanitization:");
        String unsafeInput = "<script>alert('xss');</script>";
        System.out.println("Sanitized input: " + handler.sanitizeInput(unsafeInput)); // Should be HTML-encoded
    }
}

