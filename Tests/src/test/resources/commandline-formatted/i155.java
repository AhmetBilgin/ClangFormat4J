package src.test.resources;
class I155 {
    void walkAndClose(Stream<?> stream) {
        try (stream) {
        }
    }
}
