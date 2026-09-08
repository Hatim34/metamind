package be.icc.metamind.document;

public record DocumentImportedEvent(long documentId, String filePath) {
}
