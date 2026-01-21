@RestController
@RequestMapping("/api/profile/cv")
@RequiredArgsConstructor
public class ProfileCvController {

    private final UserService userService;
    private final String uploadDir = "uploads/cvs/";

    @PostMapping
    public ResponseEntity<?> uploadCv(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {

        String username = authentication.getName();

        String contentType = file.getContentType();
        if (!"application/pdf".equals(contentType) &&
            !"application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(contentType)) {
            return ResponseEntity.badRequest()
                    .body("Formato no permitido. Solo PDF o DOCX");
        }

        try {
            Files.createDirectories(Paths.get(uploadDir));

            String extension = contentType.equals("application/pdf") ? ".pdf" : ".docx";
            String fileName = username + extension;
            Path filePath = Paths.get(uploadDir).resolve(fileName);

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            userService.updateCv(username, filePath.toString());

            return ResponseEntity.ok("CV guardado correctamente");

        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body("Error al guardar el CV");
        }
    }

    
    @GetMapping
    public ResponseEntity<Resource> getCv(Authentication authentication) throws IOException {
        String username = authentication.getName();
        String cvPath = userService.getCvPath(username);

        if (cvPath == null) {
            return ResponseEntity.notFound().build();
        }

        Path path = Paths.get(cvPath);
        Resource resource = new UrlResource(path.toUri());

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }

    
    @DeleteMapping
    public ResponseEntity<?> deleteCv(Authentication authentication) throws IOException {
        String username = authentication.getName();
        String cvPath = userService.getCvPath(username);

        if (cvPath != null) {
            Files.deleteIfExists(Paths.get(cvPath));
            userService.removeCv(username);
        }

        return ResponseEntity.ok("CV eliminado correctamente");
    }
}
