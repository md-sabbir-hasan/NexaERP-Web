/**
 * Triggers a browser download for a blob response (used for PDF/Excel exports).
 */
export function triggerBlobDownload(blob: Blob, filename: string, mimeType: string): void {
  const objectUrl = URL.createObjectURL(new Blob([blob], { type: mimeType }));

  const anchor = document.createElement('a');
  anchor.href = objectUrl;
  anchor.download = filename;

  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();

  setTimeout(() => URL.revokeObjectURL(objectUrl), 1000);
}

export const EXCEL_MIME_TYPE = 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet';

/**
 * Extracts a readable error message from an HttpErrorResponse whose body is a Blob
 * (happens when responseType: 'blob' is used and the server returns a JSON error).
 */
export async function extractBlobErrorMessage(error: any, fallback: string): Promise<string> {
  if (error?.error instanceof Blob) {
    try {
      const text = await error.error.text();
      const body = JSON.parse(text);
      return body?.message ?? fallback;
    } catch {
      return fallback;
    }
  }

  return error?.error?.message ?? fallback;
}

/**
 * Sanitizes a string for safe use in a downloaded filename.
 */
export function toSafeFilenamePart(value: string): string {
  return value
    .trim()
    .replace(/[^a-zA-Z0-9-_]+/g, '-')
    .replace(/^-+|-+$/g, '');
}
